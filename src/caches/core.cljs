
(ns caches.core
  (:require [clojure.string :as string]
            [lilac.core :refer [dev-check record+ number+ optional+]]))

(defn access-cache [*cache-states params]
  (let [caches (@*cache-states :caches), the-loop (@*cache-states :loop)]
    (if (contains? caches params)
      (do
       (swap!
        *cache-states
        (fn [cache-states]
          (-> cache-states
              (update-in
               [:caches params]
               (fn [info] (-> info (assoc :last-hit the-loop) (update :hit-times inc))))
              (update-in [:logs :this-loop :hit] inc)
              (update-in [:logs :all-loops :hit] inc))))
       (:value (get caches params)))
      (do
       (swap!
        *cache-states
        (fn [cache-states]
          (-> cache-states
              (update-in [:logs :this-loop :missed] inc)
              (update-in [:logs :all-loops :missed] inc))))
       nil))))

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
     (fn [caches]
       (->> caches
            (remove
             (fn [[params info]]
               (cond
                 (zero? (info :hit-times)) true
                 (> (- (states-0 :loop) (info :hit-loop)) (gc :elapse-loop))
                   (do (swap! *removed-used conj (info :hit-times)) true)
                 :else false)))
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
  (println (:logs @*cache-states))
  (doseq [[params info] (@*cache-states :caches)]
    (println "INFO:" (assoc info :value 'VALUE))))

(defn write-cache! [*cache-states params value]
  (let [the-loop (@*cache-states :loop)]
    (swap!
     *cache-states
     update
     :caches
     (fn [caches]
       (if (contains? caches params)
         (do
          (println "[Respo Caches] already exisits" params)
          (update
           caches
           params
           (fn [info] (-> info (assoc :last-hit the-loop) (update :hit-times inc)))))
         (assoc
          caches
          params
          {:value value, :initial-loop the-loop, :last-hit the-loop, :hit-times 0}))))))

(defn user-scripts [*caches]
  (def *caches (new-caches {:cold-duration 10, :trigger-loop 4, :elapse-loop 2}))
  (write-cache! *caches [1 2 3 4] 10)
  (write-cache! *caches [1 2 3] 6)
  (access-cache *caches [1 2 3 4])
  (access-cache *caches [1 2 3])
  (new-loop! *caches)
  (show-summary! *caches)
  (perform-gc! *caches))
