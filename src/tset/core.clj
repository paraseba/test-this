(ns tset.core
  (:require clojure.tools.namespace
            clojure.java.io
            [clojure.test :as cljtest]))

(defn get-test-namespaces [base-dir ns-filter]
  (filter ns-filter
          (clojure.tools.namespace/find-namespaces-in-dir base-dir)))

(defn my-test-all-vars
  [test-filter ns]
  (let [once-fixture-fn (cljtest/join-fixtures (:clojure.test/once-fixtures (meta ns)))
        each-fixture-fn (cljtest/join-fixtures (:clojure.test/each-fixtures (meta ns)))]
    (once-fixture-fn
     (fn []
       (doseq [v (vals (filter test-filter (ns-interns ns)))]
         (when (:test (meta v))
           (each-fixture-fn (fn [] (cljtest/test-var v)))))))))

(defn do-run-tests [namespaces test-filter]
  (doseq [n namespaces]
    (require n))
  (with-redefs [cljtest/test-all-vars (partial my-test-all-vars test-filter)]
    (apply cljtest/run-tests namespaces)))

(defn run [{test-filter :filter ns-filter :namespace-filter
            before :before after :after dir :test-dir
            :or {test-filter (constantly true) ns-filter (constantly true)
                 dir "test" before (fn [& args]) after (fn [& args])}}]
  (before)
  (let [dir (clojure.java.io/file dir)
        nss (get-test-namespaces dir ns-filter)]
    (after (do-run-tests nss test-filter))))
