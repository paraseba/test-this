(ns tset.test.core
  (:use [tset.core])
  (:use [clojure.test]))

(deftest test-ns-mantcher
  (are [args] ((:namespace-filter (apply namespaces args)) (create-ns 'my-ns))
    ['my-ns]
    [:all]
    [:my-ns]
    [#".*my-.*"]
    [(constantly true)]
    ['my-ns]
    ['my-ns 'other :foo]
    ['other 'my-ns 'foo]
    [:all :other 'foo]
    [:other :all :foo]
    [:my-ns :other 'foo]
    [:other :my-ns :foo]
    [(constantly true) :other 'foo]
    [:other (constantly true) :foo])

  (are [args] (not ((:namespace-filter (apply namespaces args)) (create-ns 'my-ns)))
    ['myxx-ns]
    [:myxx-ns]
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
    [:other (constantly false) :foo]))

(deftest test-vars-mantcher
  (are [args] ((:test-filter (apply tests args)) (intern (create-ns 'foo-ns) 'my-var))
    ['my-var]
    [:all]
    [:my-var]
    [#".*my-.*"]
    [(constantly true)]
    ['my-var]
    ['my-var 'other :foo]
    ['other 'my-var 'foo]
    [:all :other 'foo]
    [:other :all :foo]
    [:my-var :other 'foo]
    [:other :my-var :foo]
    [(constantly true) :other 'foo]
    [:other (constantly true) :foo])

  (are [args] (not ((:test-filter (apply tests args)) (intern (create-ns 'foo-ns) 'my-var)))
    ['myxx-var]
    [:myxx-var]
    [#".*myxx-.*"]
    [(constantly false)]
    ['myxx-var]
    ['myxx-var 'other :foo]
    ['other 'myxx-var 'foo]
    [:other 'foo]
    [:other :foo]
    [:myxx-var :other 'foo]
    [:other :myxx-var :foo]
    [(constantly false) :other 'foo]
    [:other (constantly false) :foo]))
