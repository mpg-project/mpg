(ns jdbc-pg-sanity.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [jdbc-pg-sanity.core]))

(def pg {:subprotocol "postgresql"
         :subname "//127.0.0.1:5432/sanity_test"
         })



(deftest a-test
  (testing "Basic Math"
    (is (= 1 1))))



(deftest connection
  (testing "can connect to db"
    (is (= 1
           (->
            (sql/query pg ["select 1 as result"])
            first
            :result)))))


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
