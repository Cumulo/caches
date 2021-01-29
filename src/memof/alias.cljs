
(ns memof.alias (:require [memof.core :as memof]))

(defonce *memof-call-states (atom (memof/new-states {})))

(defn memof-call [f & args]
  (let [v (memof/access-record *memof-call-states f args)]
    (if (some? v)
      v
      (let [result (apply f args)]
        (memof/write-record! *memof-call-states f args result)
        result))))

(defn reset-calling-caches! [] (memof/reset-entries! *memof-call-states))

(defn tick-calling-loop! [] (memof/new-loop! *memof-call-states))
