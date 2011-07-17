(ns tset.test.core
  (:use [tset.core])
  (:use [clojure.test]))

(deftest test-ns-mantcher
  (are [arg name] ((:namespace-filter (namespaces arg)) (create-ns name))
    'my-ns 'my-ns
    :all 'my-ns
    :my-ns 'my-ns
    #".*my-.*" 'my-ns
    (constantly true) 'my-ns
    ['my-ns] 'my-ns
    ['my-ns 'other :foo] 'my-ns
    ['other 'my-ns 'foo] 'my-ns
    [:all :other 'foo] 'my-ns
    [:other :all :foo] 'my-ns
    [:my-ns :other 'foo] 'my-ns
    [:other :my-ns :foo] 'my-ns
    [(constantly true) :other 'foo] 'my-ns
    [:other (constantly true) :foo] 'my-ns)

  (are [arg name] (not ((:namespace-filter (namespaces arg)) (create-ns name)))
    'myxx-ns 'my-ns
    :myxx-ns 'my-ns
    #".*myxx-.*" 'my-ns
    (constantly false) 'my-ns
    ['myxx-ns] 'my-ns
    ['myxx-ns 'other :foo] 'my-ns
    ['other 'myxx-ns 'foo] 'my-ns
    [:other 'foo] 'my-ns
    [:other :foo] 'my-ns
    [:myxx-ns :other 'foo] 'my-ns
    [:other :myxx-ns :foo] 'my-ns
    [(constantly false) :other 'foo] 'my-ns
    [:other (constantly false) :foo] 'my-ns))

(deftest test-vars-mantcher
  (are [arg name] ((:test-filter (tests arg)) (intern (create-ns 'foo-ns) name))
    'my-var 'my-var
    :all 'my-var
    :my-var 'my-var
    #".*my-.*" 'my-var
    (constantly true) 'my-var
    ['my-var] 'my-var
    ['my-var 'other :foo] 'my-var
    ['other 'my-var 'foo] 'my-var
    [:all :other 'foo] 'my-var
    [:other :all :foo] 'my-var
    [:my-var :other 'foo] 'my-var
    [:other :my-var :foo] 'my-var
    [(constantly true) :other 'foo] 'my-var
    [:other (constantly true) :foo] 'my-var)

  (are [arg name] (not ((:test-filter (tests arg)) (intern (create-ns 'foo-ns) name)))
    'myxx-var 'my-var
    :myxx-var 'my-var
    #".*myxx-.*" 'my-var
    (constantly false) 'my-var
    ['myxx-var] 'my-var
    ['myxx-var 'other :foo] 'my-var
    ['other 'myxx-var 'foo] 'my-var
    [:other 'foo] 'my-var
    [:other :foo] 'my-var
    [:myxx-var :other 'foo] 'my-var
    [:other :myxx-var :foo] 'my-var
    [(constantly false) :other 'foo] 'my-var
    [:other (constantly false) :foo] 'my-var))
