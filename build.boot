(set-env!
  :version "1.2.0"
  :dependencies '[[org.clojure/clojure "1.9.0-alpha11" :scope "provided"]
                  [cheshire "5.6.1"]
                  [org.clojure/java.jdbc "0.6.0-alpha2"]
                  [org.postgresql/postgresql "9.4.1208"]
                  [adzerk/boot-test "1.1.0" :scope "test"]
                  [environ "1.0.2"          :scope "test"]]
  :global-vars {*warn-on-reflection* true}
  :resource-paths #{"src"}
  :source-paths #{"src"})

(require '[adzerk.boot-test :as t])

(task-options!
  pom {:project 'mpg
       :version (get-env :version)
       :description "More modern Postgres to the gallon. Transparently maps clojure <-> postgresql data"
       :url "https://github.com/mpg-project/mpg"
       :scm {:url "https://github.com/irresponsible/oolong.git"}
       :license {:name "MIT" :url "https://opensource.org/licenses/MIT"}
  target  {:dir #{"target"}}})

(deftask testing []
  (set-env! :source-paths #(conj % "test")
            :resource-paths #(conj % "test"))
  identity)

(deftask test []
  (comp (testing) (speak) (t/test)))

(deftask autotest []
  (comp (watch) (test)))

(deftask make-jar []
  (comp (pom) (jar)))

(deftask travis []
  (testing)
  (t/test))
