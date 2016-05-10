(ns mpg.util
  (:require [cheshire.core :as c])
  (:import [org.postgresql.util PGobject]
           [java.sql PreparedStatement]))

(defn pg-param-type
  [^PreparedStatement s ^long idx]
  (if-let [md (.getMetaData s)]
    (or (.getColumnTypeName md idx)
        (throw (ex-info "We could not obtain the column type name" {:got s :meta md})))
    (throw (ex-info "We could not obtain metadata from the prepared statement" {:got s}))))

(defn pg-json
  "Converts the given value to a PG JSON object"
  [value]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (c/generate-string value))))
