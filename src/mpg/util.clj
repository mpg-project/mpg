(ns mpg.util
  (:require [cheshire.core :as c])
  (:import [org.postgresql.util PGobject]
           [java.sql PreparedStatement]))

(defn pg-param-type
  [^PreparedStatement s ^long idx]
  (if-let [md (.getParameterMetaData s)]
    (or (.getParameterTypeName md idx)
        (throw (ex-info "We could not obtain the column type name" {:got s :meta md})))
    (throw (ex-info "We could not obtain metadata from the prepared statement" {:got s}))))

(defn pg-result-type
  [^PreparedStatement s ^long idx]
  (if-let [md (.getMetaData s)]
    (or (.getColumnTypeName md idx)
        (throw (ex-info "We could not obtain the column type name" {:got s :meta md})))
    (throw (ex-info "We could not obtain metadata from the prepared statement" {:got s}))))

(defn param-meta
  [s]
  (let [m (.getParameterMetaData s)
        c (.getParameterCount m)]
    (into [] (map (fn [i]
                    {:class (.getParameterClassName m i)
                     :type  (.getParameterTypeName m i)}))
          (range 1 (inc c)))))

(defn result-meta [s]
  (let [m (.getMetaData s)
        c (.getColumnCount m)]
    (into [] (map (fn [i]
                    {:class (.getColumnClassName m i)
                     :type  (.getColumnTypeName m i)
                     :label (.getColumnLabel m i)
                     :name  (.getColumnName m i)}))
          (range 1 (inc c)))))

(defn pg-json
  "Converts the given value to a PG JSON object"
  [value]
  (doto (PGobject.)
    (.setType "json")
    (.setValue (c/generate-string value))))

(defn fatal [msg map]
  (throw (ex-info (str msg ": " (pr-str map)) map)))
