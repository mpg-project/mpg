(ns mpg.data
  (:require [cheshire.core :as c]
            [clojure.java.jdbc :as j]
            [mpg.util :as u])
  (:import [org.postgresql.util PGobject]
           [org.postgresql.jdbc PgArray]
           [clojure.lang IPersistentMap IPersistentVector ExceptionInfo]
           [java.sql Date Timestamp PreparedStatement]
           [java.time Instant LocalDateTime]
           [java.util HashMap]))

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
        :json   (u/pg-json value))
        :hstore (HashMap. ^clojure.lang.PersistentHashMap value))
    IPersistentVector
    (sql-value [value]
      (u/pg-json value)))
  (extend-protocol j/ISQLParameter
    IPersistentMap
    (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
      (case (try (u/pg-param-type stmt idx) (catch ExceptionInfo e default-map))
        "json"   (.setObject stmt idx (u/pg-json v))
        "jsonb"  (.setObject stmt idx (u/pg-json v))
        "citext" (.setObject stmt idx (u/pg-json v))
        "hstore" (.setObject stmt idx (java.util.HashMap. ^clojure.lang.PersistentHashMap v))))
    IPersistentVector
    (set-parameter [v ^java.sql.PreparedStatement stmt ^long idx]
      (let [conn      (.getConnection stmt)
            meta      (.getParameterMetaData stmt)
            type-name (.getParameterTypeName meta idx)]
        (if-let [elem-type (when (= (first type-name) \_) (apply str (rest type-name)))]
          (.setObject stmt idx (.createArrayOf conn elem-type (to-array v)))
          (.setObject stmt idx (u/pg-json v)))))))

