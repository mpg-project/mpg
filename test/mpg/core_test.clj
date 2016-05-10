(ns mpg.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [environ.core :refer [env]]
            [mpg.core :as mpg])
  (:import [java.time LocalDate ZonedDateTime ZoneId]
           [org.postgresql.util PGobject]))

(mpg/patch)
;; Allow the user to specify connection parameters on the command line
(def pg
  (as-> {:subprotocol "postgresql"
         :subname (or (env :mpg-test-db-uri)
                      "//127.0.0.1:5432/sanity_test")} $
    (if-let [user (env :mpg-test-db-user)]
      (assoc $ :user user)
      $)
    (if-let [password (env :mpg-test-db-pass)]
      (assoc $ :password password)
      $)))

(def conn (sql/get-connection pg))

(defn roundtrip-prepared [type val]
  (-> (as-> (str "select (? :: " type ") as result") $
        (sql/prepare-statement conn $)
        (sql/query conn [$ val]))
      first :result))

(defn roundtrip-unprepared [type val]
  (-> (as-> (str "select (? :: " type ") as result") sql
        (sql/query pg [sql val]))
      first :result))

(deftest data
  (let [v1 {:a [{:b "c"}]}
        v2 [{:b [4 5 6]}]
        v3 {"a" "123" "b" "456"}] ; jdbc+hstore cannot has such rich delights as "numbers"
    (testing "roundtripping complex maps through json"
      (is (= v1
             (roundtrip-unprepared "json"  v1)
             (roundtrip-prepared   "json"  v1)
             (roundtrip-unprepared "jsonb" v1)
             (roundtrip-prepared   "jsonb" v1))))
    (testing "roundtripping complex vectors through json"
      (is (= v2
             (roundtrip-unprepared "json"  v2)
             (roundtrip-unprepared "jsonb" v2)
             (roundtrip-prepared   "json"  v2)
             (roundtrip-prepared   "jsonb" v2))))
    (testing "roundtripping simple maps through hstore"
      (is (= v3
             (roundtrip-unprepared "hstore"  v3)
             (roundtrip-prepared   "hstore"  v3))))))

(deftest arrays
  (testing "vector roundtrips through array"
    (let [v [2 4 8]]
      (is (= v
             (roundtrip-unprepared "int[]" v)
             (roundtrip-prepared   "int[]" v))))))

(deftest datetime
  (testing "LocalDate roundtrips through date"
    (let [v (LocalDate/now)]
      (is (= v
             (roundtrip-unprepared "date" v)
             (roundtrip-prepared   "date" v)))))
  (testing "ZonedDateTime roundtrips through timestamp/tz"
    (let [v (ZonedDateTime/now (ZoneId/of "UTC"))]
      (is (= v
             (roundtrip-unprepared "timestamp" v)
             (roundtrip-prepared   "timestamp" v)))
      (is (= v
             (roundtrip-unprepared "timestamptz" v)
             (roundtrip-prepared   "timestamptz" v))))))

(deftest citext
  (testing "citext should be treated as string type"
    (let [v "some example text"]
      (is (= v
             (roundtrip-prepared "citext" v)
             (roundtrip-unprepared "citext" v))))))
