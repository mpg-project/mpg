(ns mpg.core
  (:require [mpg.data :as data]
            [mpg.datetime :as datetime]))

(defn patch
  "Smooths integration with postgres such as by mapping between JSON and vectors/maps.
   Args: [& [opts]]
     opts is a map of options valid keys:
         :data     - boolean. auto-map maps and vectors?
         :datetime - boolean. auto-map java.time.{LocalDate, Instant, ZonedDateTime} ?
         :default-map - keyword. one of :json, :hstore. Controls the default
                        output format for maps in unprepared statements
     opts if provided are merged against the default settings:
       {:data true :datetime true :default-map :json}"
  ([& [{:keys [data datetime default-map]
     :or {data true datetime true
          default-map :json}}]]
   (when data
     (data/patch default-map))
   (when datetime
     (datetime/patch))))
