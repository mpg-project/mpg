(ns mpg.util
  (:require [cheshire.core :as c])
  (:import [org.postgresql.util PGobject]
           [java.sql PreparedStatement]))

(defn pg-param-type
  [^PreparedStatement s ^long idx]
  (-> s .getMetaData (.getColumnTypeName idx)))

(defn pg-json
  "Converts the given value to a PG JSON object"
  [value]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (c/generate-string value))))
