
(ns memof.main (:require [memof.core :as caches]))

(defonce *caches (caches/new-caches {}))

(defn main! [] (println "Started.") (caches/show-summary! *caches))

(defn reload! [] (.clear js/console) (println "Reloaded."))
