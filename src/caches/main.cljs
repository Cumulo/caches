
(ns caches.main (:require [caches.core :as caches]))

(defn main! [] (println "Started.") (caches/show-summary!))

(defn reload! [] (.clear js/console) (println "Reloaded."))
