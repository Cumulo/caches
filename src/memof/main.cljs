
(ns memof.main (:require [memof.core :as memof]))

(defonce *states (memof/new-states {}))

(defn main! [] (println "Started.") (memof/show-summary! *states))

(defn reload! [] (.clear js/console) (println "Reloaded."))
