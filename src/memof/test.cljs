
(ns memof.test (:require [cljs.test :refer [deftest testing is]] [memof.core :as memof]))

(deftest
 test-gc
 (let [*states (atom (memof/new-states {})), f1 (fn [] )]
   (memof/write-record! *states f1 [1 2 3] 6)
   (memof/write-record! *states f1 [1 2] 6)
   (testing "has entries" (is (some? (memof/access-record *states f1 [1 2]))))
   (memof/perform-gc! *states)
   (testing "should be empty after GC" (is (nil? (memof/access-record *states f1 [1 2 3]))))
   (testing
    "used record should kept after GC"
    (is (some? (memof/access-record *states f1 [1 2]))))))

(deftest
 test-reset
 (let [*states (atom (memof/new-states {})), f1 (fn [x] x)]
   (memof/write-record! *states f1 [1 2] 3)
   (testing "should have some entries" (is (pos? (count (:entries @*states)))))
   (memof/reset-entries! *states)
   (testing "should have two entries" (is (zero? (count (:entries @*states)))))))

(deftest
 test-write
 (let [*states (atom (memof/new-states {})), f1 (fn [x] x), f2 (fn [x] x)]
   (testing "gets nil before writing" (is (nil? (memof/access-record *states f1 [1 2]))))
   (memof/write-record! *states f1 [1 2] 3)
   (testing "access written record" (is (= 3 (memof/access-record *states f1 [1 2]))))
   (memof/write-record! *states f2 [1 2] 3)
   (testing "should have two entries" (is (= 2 (count (:entries @*states)))))
   (memof/write-record! *states f2 [1 2] 2)
   (testing "overwrites record" (is (= 2 (memof/access-record *states f2 [1 2]))))))
