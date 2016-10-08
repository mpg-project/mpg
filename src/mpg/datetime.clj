(ns mpg.datetime
  (:require [clojure.java.jdbc :as j]
            [mpg.util :as u :refer [pg-param-type]])
  (:import [java.time Instant LocalDate LocalDateTime ZonedDateTime ZoneId]
           [java.time.temporal ChronoField]
           [java.sql Date Timestamp PreparedStatement]
           [java.util Calendar]))

(defn get-zone
  "Returns the zone named, which may be a keyword or string, e.g. :UTC"
  [zone]
  (cond
    (string? zone) (ZoneId/of zone)
    (keyword? zone) (ZoneId/of (name zone))
    :else (throw (ex-info "Invalid timezone" {:got zone}))))

(def ^ZoneId utc (get-zone :UTC))
(def ^ZoneId local (ZoneId/systemDefault))

(defn zoneddatetime->timestamp [^ZonedDateTime zdt]
  (-> zdt
      (.withZoneSameInstant local)
      .toLocalDateTime
      Timestamp/valueOf))

(defn truncate-date [^java.util.Date d]
  (-> (doto (Calendar/getInstance)
        (.setTime d)
        (.set Calendar/HOUR_OF_DAY 0)
        (.set Calendar/MINUTE 0)
        (.set Calendar/SECOND 0)
        (.set Calendar/MILLISECOND 0))
      .getTimeInMillis
      Date.))
    

(defn localdate->date [^LocalDate ld]
  (-> ld
      (.atStartOfDay (ZoneId/systemDefault))
      .toInstant
      Date/from))

(defn patch
  "Installs conversion hooks for various java.time types
   args: []"
  []
  (extend-protocol j/IResultSetReadColumn
    Date
    (result-set-read-column [^Date v _ _]
      (.toLocalDate v))
    Timestamp
    (result-set-read-column [^Timestamp v _ _]
      (-> v .toInstant (.atZone utc))))
  (extend-protocol j/ISQLValue
    java.util.Date
    (sql-value [value]
      (Date. (.getTime value)))
    LocalDate
    (sql-value [value]
      (localdate->date value))
    ZonedDateTime
    (sql-value [value]
      (zoneddatetime->timestamp value)))
  (extend-protocol j/ISQLParameter
    java.util.Date
    (set-parameter [^java.util.Date v ^PreparedStatement stmt ^long idx]
      (.setObject stmt idx
        (case (pg-param-type stmt idx)
          "date" (Date. (.getTime v))
          "timestamp" (Timestamp. (.getTime v))
          "timestamptz" (Timestamp. (.getTime v)))))
    LocalDate
    (set-parameter [^LocalDate v ^PreparedStatement stmt ^long idx]
      (.setObject stmt idx
        (case (pg-param-type stmt idx)
          "date"        (Date/valueOf v)
          "timestamp"   (Date/valueOf v)
          "timestamptz" (Date/valueOf v))))
    ZonedDateTime
    (set-parameter [^ZonedDateTime v ^PreparedStatement stmt ^long idx]
      (let [t (pg-param-type stmt idx)]
        (if (#{"timestamp" "timestamptz"} t)
          (->> v zoneddatetime->timestamp (.setTimestamp stmt idx))
          (throw (ex-info (str "Invalid conversion from ZonedDateTime. expected " t) {})))))))
