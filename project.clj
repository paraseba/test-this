(defproject test-this "0.2.0"
  :description "Powerful clojure test runner"
  :dependencies [[org.clojure/clojure "1.3.0-beta1"]
                 [org.clojure/tools.namespace "0.1.1"]
                 [org.clojure/core.incubator "0.1.0"]
                 [com.stuartsierra/lazytest "1.2.3"]]
  :exclusions [org.clojure/clojure org.clojure/clojure-contrib]
  :repositories {"stuartsierra-releases" "http://stuartsierra.com/maven2"})
