(ns mpg.data
  (:require [cheshire.core :as c]
            [clojure.java.jdbc :as j]
            [mpg.util :as u :refer [fatal pg-param-type pg-json]])
  (:import [org.postgresql.util PGobject]
           [org.postgresql.jdbc PgArray]
           [clojure.lang IPersistentMap IPersistentVector ExceptionInfo]
           [java.nio ByteBuffer]
           [java.sql Date Timestamp PreparedStatement]
           [java.time Instant LocalDateTime]
           [java.util HashMap]))

(defn bytebuf->array [^ByteBuffer buf]
  (let [a (byte-array (.remaining buf))]
    (.get buf a)
    a))

(defn patch
  "Installs conversion hooks:
     map <-> json/b OR hstore
     vector <-> json/b
   args: [default-map]
     default-map controls whether a map will be treated as hstore or json
     with unprepared statements (as we cannot just read the type).
     value is one of: :json, :hstore"
  [default-map]
  (extend-protocol j/IResultSetReadColumn
    java.util.HashMap ;; hstore
    (result-set-read-column [v _ _] (into {} v))
    PgArray
    (result-set-read-column [v _ _] (vec (.getArray v)))
    PGobject
    (result-set-read-column [pgobj _metadata _index]
      (let [type  (.getType pgobj)
            value (.getValue pgobj)]
        (case type
          "json"  (c/parse-string value true)
          "jsonb" (c/parse-string value true)
          "citext" (str value)
          value))))
  (extend-protocol j/ISQLValue
    IPersistentMap
    (sql-value [value]
      (case default-map
        :json   (pg-json value))
        :hstore (HashMap. ^clojure.lang.PersistentHashMap value))
    IPersistentVector
    (sql-value [value]
      (pg-json value))
    ByteBuffer
    (sql-value [value]
      (bytebuf->array value)))
  (extend-protocol j/ISQLParameter
    IPersistentMap
    (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
      (let [type (try (pg-param-type stmt idx) (catch ExceptionInfo e (name default-map)))]
        (case type
          "citext" (.setObject stmt idx (str v))
          "hstore" (.setObject stmt idx (java.util.HashMap. ^clojure.lang.PersistentHashMap v))
          (cond (#{"json" "jsonb"} type)
                (.setObject stmt idx (pg-json v))

                (#{"smallint" "integer" "int2" "int4" "serial" "serial4"} type)
                (if (integer? v)
                  (.setInt  stmt idx v)
                  (fatal "Expected integer" {:type type :got v :col-index idx}))

                (#{"bigint" "int8" "serial8" "bigserial"} type)
                (if (integer? v)
                  (.setLong stmt idx v)
                  (fatal "Expected integer" {:type type :got v :col-index idx}))

                (#{"decimal" "numeric" "real" "double precision"} type)
                (if (float? v)
                  (.setDouble stmt idx v)
                  (fatal "Expected float" {:type type :got v :col-index idx}))

                :else (fatal "Unknown data type in map" {:v v :idx idx :stmt stmt})))))
    IPersistentVector
    (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
      (let [conn      (.getConnection stmt)
            meta      (.getParameterMetaData stmt)
            type-name (.getParameterTypeName meta idx)]
        (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
          (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
          (if-let [array-type (-> (re-matches #"(.*)\[\]" type-name)
                                  (nth 1))]
            (.setObject stmt idx (.createArrayOf conn array-type (to-array v)))
            (.setObject stmt idx (pg-json v))))))
    ByteBuffer
    (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
      (.setBytes stmt idx (bytebuf->array v)))))
