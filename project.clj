(defproject mpg "0.2.0"
  :description "More modern Postgres to the gallon. Transparently maps clojure <-> postgresql data"
  :url "https://github.com/ShaneKilkelly/mpg"
  :license {:name "MIT"
            :url "https://opensource.org/licenses/MIT"}
  :dependencies [[org.clojure/clojure "1.8.0" :scope "provided"]
                 [cheshire "5.6.1"]
                 [org.clojure/java.jdbc "0.6.0-alpha2"]
                 [org.postgresql/postgresql "9.4.1208"]]
  :profiles {:dev {:dependencies [[environ "1.0.2"]]}}
  :global-vars {*warn-on-reflection* true})
