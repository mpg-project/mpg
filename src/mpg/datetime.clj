(ns mpg.datetime
  (:require [clojure.java.jdbc :as j]
            [mpg.util :as u])
  (:import [java.time Instant LocalDate LocalDateTime ZonedDateTime ZoneId]
           [java.time.temporal ChronoField]
           [java.sql Date Timestamp PreparedStatement]))

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
  (extend-protocol j/ISQLParameter
    LocalDate
    (set-parameter [^LocalDate v ^PreparedStatement stmt ^long idx]
      (.setObject stmt idx
        (case (u/pg-param-type stmt idx)
          "date" (Date/valueOf v))))
    ZonedDateTime
    (set-parameter [^ZonedDateTime v ^PreparedStatement stmt ^long idx]
      (let [t (u/pg-param-type stmt idx)]
        (if (#{"timestamp" "timestamptz"} t)
          (->> v zoneddatetime->timestamp (.setTimestamp stmt idx))
          (throw (ex-info (str "Invalid conversion from ZonedDateTime. expected " t) {})))))))
