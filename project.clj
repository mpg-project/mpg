(defproject jdbc-pg-sanity "0.1.0"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [cheshire "5.5.0"]
                 ]
  :profiles
  {:dev {:dependencies [
                        [org.clojure/java.jdbc "0.4.2"]
                        [org.postgresql/postgresql "9.4-1206-jdbc4"]
                        ]}
   :test {:dependencies [
                         [org.clojure/java.jdbc "0.4.2"]
                         [org.postgresql/postgresql "9.4-1206-jdbc4"]
                         ]}

   })
