(ns jdbc-pg-sanity.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [jdbc-pg-sanity.core]))

(def pg {:subprotocol "postgresql"
         :subname "//127.0.0.1:5432/sanity_test"})

(deftest connection
  (testing "can connect to db"
    (is (= 1
           (->
            (sql/query pg ["select 1 as result"])
            first
            :result)))))

;; Test Reads

(deftest json-and-maps
  (testing "json is read as a clojure map"
    (is (= {:a 1}
           (->
            (sql/query pg ["select '{\"a\": 1}'::json as result"])
            first
            :result))))
  (testing "jsonb is read as a clojure map"
    (is (= {:b 2}
           (->
            (sql/query pg ["select '{\"b\": 2}'::jsonb as result"])
            first
            :result)))))

(deftest vectors
  (testing "array is read as a clojure vector"
    (is (= [2 4 8]
           (->
            (sql/query pg ["select '{2,4,8}'::bigint[] as result"])
            first
            :result)))))

(deftest timestamp
  (testing "timestamp is read as java.util.Date"
    (is (= java.util.Date
           (->
            (sql/query pg ["select current_timestamp as result"])
            first
            :result
            type)))))
