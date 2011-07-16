(ns tset.test.core
  (:use [tset.core])
  (:use [clojure.test]))

(use-fixtures :once (fn [f] (prn "once-fixture----------") (f)))
(use-fixtures :each (fn [f] (prn "each-fixture----------") (f)))

(deftest replace-me ;; FIXME: write
  (is true))
