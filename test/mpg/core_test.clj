(ns mpg.core-test
  (:require [clojure.test :refer :all]
            [clojure.java.jdbc :as sql]
            [environ.core :refer [env]]
            [mpg.core :as mpg]
            [mpg.data :as d]
            [mpg.datetime :as dt])
  (:import [java.nio ByteBuffer]
           [java.sql Timestamp]
           [java.time LocalDate ZonedDateTime ZoneId]
           [org.postgresql.util PGobject]))

(mpg/patch)

(defn to-byte-string [thing]
  (cond (instance? (Class/forName "[B") thing) (String. ^bytes thing)
        (instance? ByteBuffer thing) (String. ^bytes (d/bytebuf->array thing))
        :else (-> (str "WTF is this? " thing " " (type thing))
                  (ex-info {:got thing})
                  throw)))

(defn to-ts [thing]
  (cond (instance? Timestamp thing) thing
        (instance? java.util.Date thing) (Timestamp. (.getTime ^java.util.Date thing))
        (instance? ZonedDateTime thing) (dt/zoneddatetime->timestamp thing)
        :else (-> (str "WTF is this? " thing " " (type thing))
                  (ex-info {:got thing})
                  throw)))

(defn to-date [thing]
  (cond (instance? java.util.Date thing) (dt/truncate-date thing)
        (instance? LocalDate thing) (dt/localdate->date thing)
        :else (-> (str "WTF is this? " thing " " (type thing))
                  (ex-info {:got thing})
                  throw)))

;; Allow the user to specify connection parameters on the command line
(def pg
  (as-> {:subprotocol "postgresql"
         :subname (or (env :mpg-test-db-uri)
                      "//127.0.0.1:5432/mpg_test")} $
    (if-let [user (env :mpg-test-db-user)]
      (assoc $ :user user)
      $)
    (if-let [password (env :mpg-test-db-pass)]
      (assoc $ :password password)
      $)))

(def conn (sql/get-connection pg))

(defn prepare-db []
  (sql/execute! {:connection conn} "create extension if not exists hstore")
  (sql/execute! {:connection conn} "create extension if not exists citext")
  (sql/execute! {:connection conn} "drop table if exists insert_test")
  (sql/execute! {:connection conn}
    "create temporary table insert_test(
       a smallint, b integer, c bigint, d serial, e bigserial,
       f int2, g int4, h int8, i serial4, j serial8,
       k decimal, l numeric, m real, n double precision,
       o varchar, p citext, q jsonb, r json)"))

(defn random-byte-array [size]
  (let [a (byte-array size)]
    (->> a .nextBytes (doto (java.util.Random.)))
    a))

(defn select-roundtrip-prepared [type val]
  (-> (as-> (str "select (? :: " type ") as result") $
        (sql/prepare-statement conn $)
        (sql/query conn [$ val]))
      first :result))

(defn select-roundtrip-unprepared [type val]
  (-> (as-> (str "select (? :: " type ") as result") sql
        (sql/query {:connection conn} [sql val]))
      first :result))

(defn select-roundtrip-test
  ([title v types]
   (select-roundtrip-test title v types identity))
  ([title v types printer]
   (testing title
     (let [v2 (printer v)]
       (doseq [t types]
         (is (= v2
                (printer (select-roundtrip-unprepared t v))
                (printer (select-roundtrip-prepared   t v))
                ;; repeat because of issue #17
                (printer (select-roundtrip-unprepared t v))
                (printer (select-roundtrip-prepared   t v)))))))))

(defn keywordize [m]
  (into {} (map (fn [[k v]] [(keyword (str k)) v])) m))

(deftest insert-test
  (let [orig {:a 123 :b 123 :c 123 :d 123 :e 123 :f 123 :g 123 :h 123 :i 123 :j 123
              :k 1.23M :l 1.23M :m (float 1.23) :n 1.23 :o "foo" :p "foo"
              :q {123 123 1.23 1.23 "a" "b"} :r {123 123 1.23 1.23 "a" "b"}}
        one (-> orig (update :q keywordize) (update :r keywordize))
        two (first (sql/insert! {:connection conn} "insert_test" orig))
        three (first (sql/query {:connection conn} "select * from insert_test as result"))]
    (is (= one two three))))

(deftest select-data-bidirectional
  (let [v1 {:a [{:b "c"}]}
        v2 [{:b [4 5 6]}]
        v3 {"a" "123" "b" "456"} ; jdbc+hstore cannot has such rich delights as "numbers"
        v4 [2 4 8]
        v5 "example text"
        v6 (random-byte-array 32)]
    (select-roundtrip-test "maps <-> json"        v1 ["json" "jsonb"])
    (select-roundtrip-test "vector <-> json"      v2 ["json" "jsonb"])
    (select-roundtrip-test "map <-> hstore"       v3 ["hstore"])
    (select-roundtrip-test "vector <-> array"     v4 ["int[]"])
    (select-roundtrip-test "string <-> citext"    v5 ["citext"])
    (select-roundtrip-test "byte-array <-> bytea" v6 ["bytea"] #(String. ^bytes %))))

(deftest select-data-unidirectional
  (let [bytes (random-byte-array 32)
        bb (doto (ByteBuffer/allocate 32)
             (.put ^bytes bytes))]
    (select-roundtrip-test "Bytebuffer -> bytea" bb ["bytea"] to-byte-string)))

(deftest select-datetime-bidirectional
  (let [now-loc (LocalDate/now)
        now-utc (ZonedDateTime/now (ZoneId/of "UTC"))]
    (select-roundtrip-test "LocalDate <-> date"          now-loc ["date"])
    (select-roundtrip-test "ZonedDateTime <-> timestamp" now-utc ["timestamp" "timestamptz"])))

(deftest select-datetime-unidirectional
  (let [now-jud (java.util.Date.)
        now-jsts  (java.sql.Timestamp. (.getTime now-jud))]
    (select-roundtrip-test "j.u.Date -> date" now-jud ["date"] to-date)
    (select-roundtrip-test "j.u.Date -> timestamp" now-jud ["timestamp" "timestamptz"] to-ts)
    (select-roundtrip-test "j.s.Timestamp -> date" now-jsts ["date"] to-date)
    (select-roundtrip-test "j.s.Timestamp -> timestamp" now-jsts ["timestamp" "timestamptz"] to-ts)))
    
(defn create-test-table []
  (sql/execute! {:connection conn} "create table insert_test"))

(defn delete-test-table []
  (sql/execute! {:connection conn} "drop table insert_test"))

(prepare-db)
