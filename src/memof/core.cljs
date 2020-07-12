
(ns memof.core
  (:require [clojure.string :as string]
            [lilac.core :refer [dev-check record+ number+ optional+ boolean+]]))

(defn access-record [*states f params]
  (let [entries (@*states :entries), the-loop (@*states :loop)]
    (if (contains? entries f)
      (if (contains? (:records (get entries f)) params)
        (do
         (swap!
          *states
          update-in
          [:entries f]
          (fn [f-info]
            (-> f-info
                (update-in
                 [:records params]
                 (fn [record]
                   (-> record (assoc :last-hit-loop the-loop) (update :hit-times inc))))
                (update :hit-times inc))))
         (get-in entries [f :records params :value]))
        (do (swap! *states update-in [:entries f :missed-times] inc) nil))
      nil)))

(def lilac-gc-options
  (optional+
   (record+
    {:trigger-loop (number+), :elapse-loop (number+), :verbose? (boolean+)}
    {:check-keys? true, :all-optional? true})))

(defn modify-gc-options! [*states options]
  (dev-check options lilac-gc-options)
  (swap! *states update :gc (fn [x0] (merge x0 options))))

(defn perform-gc! [*states]
  (let [states-0 @*states, gc (states-0 :gc), *removed-used (atom [])]
    (swap!
     *states
     update
     :entries
     (fn [entries]
       (->> entries
            (map
             (fn [[f entry]]
               [f
                (update
                 entry
                 :records
                 (fn [records]
                   (->> records
                        (remove
                         (fn [[params record]]
                           (cond
                             (zero? (record :hit-times)) true
                             (>
                              (- (states-0 :loop) (record :last-hit-loop))
                              (gc :elapse-loop))
                               (do
                                (swap! *removed-used conj (record :hit-times))
                                (when (:verbose? gc)
                                  (println
                                   "[Memof verbose] removing at loop"
                                   (:loop states-0)
                                   "--"
                                   f
                                   params
                                   (assoc record :value "VALUE")))
                                true)
                             :else false)))
                        (into {}))))]))
            (remove (fn [[f entry]] (empty? (:records entry))))
            (into {}))))
    (println
     (str
      "[Memof GC] Performed GC, from "
      (count (states-0 :entries))
      " to "
      (count (@*states :entries))))
    (println "Removed counts" (frequencies @*removed-used))))

(defn new-loop! [*states]
  (swap! *states update :loop inc)
  (let [loop-count (@*states :loop), gc (@*states :gc)]
    (when (zero? (rem loop-count (gc :trigger-loop))) (perform-gc! *states))))

(defn new-states [gc-options]
  (dev-check gc-options lilac-gc-options)
  (let [options (merge {:trigger-loop 100, :elapse-loop 200, :verbose? false} gc-options)]
    (println "Initialized caches with options:" options)
    {:loop 0, :entries {}, :gc options}))

(defn reset-entries! [*states]
  (println "[Memof] reset.")
  (swap! *states assoc :loop 0 :caches {}))

(defn show-summary [states]
  (let [states (if (satisfies? IAtom states)
                 (do (println "[WARN] pass dereferenced value of show-summary!") @states)
                 states)]
    (println
     (str
      "\n"
      "[Meof Summary] of size "
      (count (states :entries))
      ". Currenly loop is "
      (:loop states)
      "."))
    (doseq [[f entry] (states :entries)]
      (println "FUNCTION.." f (dissoc entry :records))
      (doseq [[params record] (:records entry)]
        (println "INFO:" (assoc record :value 'VALUE))))))

(defn write-record! [*states f params value]
  (let [the-loop (@*states :loop)]
    (swap!
     *states
     update
     :entries
     (fn [entries]
       (let [entries (if (contains? entries f)
                       entries
                       (assoc
                        entries
                        f
                        {:records {}, :hit-times 0, :missed-times 0, :initial-loop the-loop}))]
         (update
          entries
          f
          (fn [entry]
            (if (and (contains? (:recods entry) params)
                     (= value (get-in entry [:records params :value])))
              (do
               (println "[Memof Record] already exisits" params "for" f)
               (-> entry
                   (update-in
                    [:records params]
                    (fn [info]
                      (-> info (assoc :last-hit-loop the-loop) (update :hit-times inc))))
                   (update :hit-times inc)))
              (assoc-in
               entry
               [:records params]
               {:value value, :initial-loop the-loop, :last-hit-loop the-loop, :hit-times 0})))))))))

(defn user-scripts [*states]
  (def *states (atom (new-states {:trigger-loop 4, :elapse-loop 2, :verbose? true})))
  (defn f1 [x] )
  (defn f2 [x y] )
  (write-record! *states f1 [1 2 3 4] 10)
  (write-record! *states f1 [1 2 3] 6)
  (write-record! *states f2 [1 2 3] 6)
  (access-record *states f1 [1 2 3 4])
  (access-record *states f1 [1 2 3])
  (access-record *states f1 [1 2 'x])
  (new-loop! *states)
  (show-summary @*states)
  (perform-gc! *states)
  (identity @*states))
