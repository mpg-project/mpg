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

(defn prepare-db []
  (sql/execute! {:connection conn} "create extension if not exists hstore")
  (sql/execute! {:connection conn} "create extension if not exists citext"))

(defn random-byte-array [size]
  (let [a (byte-array size)]
    (->> a .nextBytes (doto (java.util.Random.)))
    a))

(defn roundtrip-prepared [type val]
  (-> (as-> (str "select (? :: " type ") as result") $
        (sql/prepare-statement conn $)
        (sql/query conn [$ val]))
      first :result))

(defn roundtrip-unprepared [type val]
  (-> (as-> (str "select (? :: " type ") as result") sql
        (sql/query pg [sql val]))
      first :result))

(defn roundtrip-test
  ([title v types]
   (roundtrip-test title v types identity))
  ([title v types printer]
   (testing title
     (let [v2 (printer v)]
       (doseq [t types]
         (is (= v2
                (printer (roundtrip-unprepared t v))
                (printer (roundtrip-prepared   t v))
                ;; repeat because of issue #17
                (printer (roundtrip-unprepared t v))
                (printer (roundtrip-prepared   t v)))))))))

(deftest data
  (let [v1 {:a [{:b "c"}]}
        v2 [{:b [4 5 6]}]
        v3 {"a" "123" "b" "456"} ; jdbc+hstore cannot has such rich delights as "numbers"
        v4 [2 4 8]
        v5 "example text"
        v6 (random-byte-array 32)]
    (roundtrip-test "maps <-> json"        v1 ["json" "jsonb"])
    (roundtrip-test "vector <-> json"      v2 ["json" "jsonb"])
    (roundtrip-test "map <-> hstore"       v3 ["hstore"])
    (roundtrip-test "vector <-> array"     v4 ["int[]"])
    (roundtrip-test "string <-> citext"    v5 ["citext"])
    (roundtrip-test "byte-array <-> bytea" v6 ["bytea"] #(String. %))))

(deftest datetime
  (let [now-loc (LocalDate/now)
        now-utc (ZonedDateTime/now (ZoneId/of "UTC"))]
    (roundtrip-test "LocalDate <-> date"          now-loc ["date"])
    (roundtrip-test "ZonedDateTime <-> timestamp" now-utc ["timestamp" "timestamptz"])))

(prepare-db)
