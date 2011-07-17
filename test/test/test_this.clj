(ns test.test_this
  (:use [test-this])
  (:use [clojure.test :only (is are deftest)]))

(defn make-ns [name meta]
  (doto (create-ns name)
    (alter-meta! merge meta)))

(deftest test-ns-matcher
  (are [args] ((:namespace-filter (apply match-namespaces args)) (make-ns 'my-ns {:my-meta :meta}))
    ['my-ns]
    [:all]
    [:my-meta]
    [#".*my-.*"]
    [(constantly true)]
    ['my-ns]
    ['my-ns 'other :foo]
    ['other 'my-ns 'foo]
    [:all :other 'foo]
    [:other :all :foo]
    [:my-meta :other 'foo]
    [:other :my-meta :foo]
    [(constantly true) :other 'foo]
    [:other (constantly true) :foo]
    [[:all #".*my.*" 'my-ns :my-meta] :foo])

  (are [args] (not ((:namespace-filter (apply match-namespaces args))
                      (make-ns 'my-ns {:my-meta :meta :false false})))
    ['myxx-ns]
    [:myxx-ns]
    [:false]
    [#".*myxx-.*"]
    [(constantly false)]
    ['myxx-ns]
    ['myxx-ns 'other :foo]
    ['other 'myxx-ns 'foo]
    [:other 'foo]
    [:other :foo]
    [:myxx-ns :other 'foo]
    [:other :myxx-ns :foo]
    [(constantly false) :other 'foo]
    [:other (constantly false) :foo]
    [[:all #".*my.*" 'myxx-ns :my-ns] :foo]))

(defn make-var [name meta]
  (doto (intern (create-ns 'foo-ns) name)
    (alter-meta! merge meta)))

(deftest test-vars-matcher
  (are [args] ((:test-filter (apply match-tests args)) (make-var 'my-var {:my-meta :meta}))
    ['my-var]
    [:all]
    [:my-meta]
    [#".*my-.*"]
    [(constantly true)]
    ['my-var]
    ['my-var 'other :foo]
    ['other 'my-var 'foo]
    [:all :other 'foo]
    [:other :all :foo]
    [:my-meta :other 'foo]
    [:other :my-meta :foo]
    [(constantly true) :other 'foo]
    [:other (constantly true) :foo]
    [[:all #".*my.*" 'my-var :my-meta] :foo])

  (are [args] (not ((:test-filter (apply match-tests args)) (make-var 'my-var {:my-meta :meta :false false})))
    ['myxx-var]
    [:myxx-meta]
    [:false]
    [#".*myxx-.*"]
    [(constantly false)]
    ['myxx-var]
    ['myxx-var 'other :foo]
    ['other 'myxx-var 'foo]
    [:other 'foo]
    [:other :foo]
    [:myxx-meta :other 'foo]
    [:other :myxx-meta :foo]
    [(constantly false) :other 'foo]
    [:other (constantly false) :foo]
    [[:all #".*my.*" 'myxx-var :my-meta] :foo]))

