
(ns memof.core
  (:require [clojure.string :as string]
            [lilac.core :refer [dev-check record+ number+ optional+]]))

(defn access-cache [*cache-states f params]
  (let [caches (@*cache-states :caches), the-loop (@*cache-states :loop)]
    (if (contains? caches f)
      (if (contains? (:caches (get caches f)) params)
        (do
         (swap!
          *cache-states
          update-in
          [:caches f]
          (fn [f-info]
            (-> f-info
                (update-in
                 [:caches params]
                 (fn [info] (-> info (assoc :last-hit the-loop) (update :hit-times inc))))
                (update :hit-times inc))))
         (:value (get-in caches [f :caches params])))
        (do (swap! *cache-states update-in [:caches f :missed-times] inc) nil))
      nil)))

(def lilac-gc-configs
  (optional+
   (record+
    {:cold-duration (number+), :trigger-loop (number+), :elapse-loop (number+)}
    {:check-keys? true, :all-optional? true})))

(defn new-caches [gc-configs]
  (dev-check gc-configs lilac-gc-configs)
  (let [options (merge {:cold-duration 400, :trigger-loop 100, :elapse-loop 50} gc-configs)]
    (println "Initialized caches with options:" options)
    (atom
     {:loop 0,
      :caches {},
      :gc options,
      :logs {:this-loop {:hit 0, :missed 0}, :all-loops {:hit 0, :missed 0}}})))

(defn perform-gc! [*cache-states]
  (let [states-0 @*cache-states, gc (states-0 :gc), *removed-used (atom [])]
    (swap!
     *cache-states
     update
     :caches
     (fn [dict]
       (->> dict
            (map
             (fn [[f info]]
               [f
                (update
                 info
                 :caches
                 (fn [caches]
                   (->> caches
                        (remove
                         (fn [[params info]]
                           (cond
                             (zero? (info :hit-times)) true
                             (> (- (states-0 :loop) (info :hit-loop)) (gc :elapse-loop))
                               (do (swap! *removed-used conj (info :hit-times)) true)
                             :else false)))
                        (into {}))))]))
            (remove (fn [[f info]] (empty? (:caches info))))
            (into {}))))
    (println
     (str
      "[Caches GC] Performed GC, from "
      (count (states-0 :caches))
      " to "
      (count (@*cache-states :caches))))
    (println "Removed counts" (frequencies @*removed-used))))

(defn new-loop! [*cache-states]
  (swap! *cache-states update :loop inc)
  (swap! *cache-states assoc-in [:logs :this-loop] {:hit 0, :missed 0})
  (let [loop-count (@*cache-states :loop), gc (@*cache-states :gc)]
    (when (and (> loop-count (gc :cold-duration)) (zero? (rem loop-count (gc :trigger-loop))))
      (perform-gc! *cache-states))))

(defn reset-caches! [*cache-states]
  (println "[Caches] reset.")
  (swap! *cache-states assoc :loop 0 :caches {}))

(defn show-summary! [*cache-states]
  (println
   (str
    "\n"
    "[Caches Summary] of size "
    (count (@*cache-states :caches))
    ". Currenly loop is "
    (:loop @*cache-states)
    "."))
  (doseq [[f dict] (@*cache-states :caches)]
    (println "FUNCTION.." f (dissoc dict :caches))
    (doseq [[params info] (:caches dict)] (println "INFO:" (assoc info :value 'VALUE)))))

(defn write-cache! [*cache-states f params value]
  (let [the-loop (@*cache-states :loop)]
    (swap!
     *cache-states
     update
     :caches
     (fn [caches]
       (let [caches (if (contains? caches f)
                      caches
                      (assoc
                       caches
                       f
                       {:caches {}, :hit-times 0, :missed-times 0, :initial-loop the-loop}))]
         (update
          caches
          f
          (fn [caller-info]
            (if (contains? (:caches caller-info) params)
              (do
               (println "[Respo Caches] already exisits" params "for" f)
               (-> caller-info
                   (update-in
                    [:caches params]
                    (fn [info] (-> info (assoc :last-hit the-loop) (update :hit-times inc))))
                   (update :hit-times inc)))
              (assoc-in
               caller-info
               [:caches params]
               {:value value, :initial-loop the-loop, :last-hit the-loop, :hit-times 0})))))))))

(defn user-scripts [*caches]
  (def *caches (new-caches {:cold-duration 10, :trigger-loop 4, :elapse-loop 2}))
  (defn f1 [x] )
  (defn f2 [x y] )
  (write-cache! *caches f1 [1 2 3 4] 10)
  (write-cache! *caches f1 [1 2 3] 6)
  (write-cache! *caches f2 [1 2 3] 6)
  (access-cache *caches f1 [1 2 3 4])
  (access-cache *caches f1 [1 2 3])
  (access-cache *caches f1 [1 2 'x])
  (new-loop! *caches)
  (show-summary! *caches)
  (perform-gc! *caches)
  (identity @*caches))
