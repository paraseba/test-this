(ns test-this
  "test-this is a powerful runner for clojure.test. It's main features are:

  * Fine-grained selection of which namespaces to tests and which tests to run
  * Automatically reload modified files
  * No changes to source code needed, use all your clojure.test tests with all its features
  * Extensible
  * Easy to use.
  "
  {:author "Sebastián B. Galkin (@paraseba)"}
  (:use [clojure.core.incubator :only [seqable?]]
        [clojure.tools.namespace :only [find-namespaces-in-dir]]
        [clojure.string :only [join]]
        [clojure.java.io :only [file]]
        [lazytest.reload :only [reload]]
        [lazytest.tracker :only [tracker]])
  (:require [clojure.test :as cljtest]))


;; # Auxiliary functions

(defn- get-test-namespaces
  "Return all namespaces under base-dir, after filtering by ns-filter predicate."
  [base-dir ns-filter]
  (filter ns-filter (find-namespaces-in-dir base-dir)))

(defn- my-test-all-vars
  "Replacement for clojure.test/test-all-vars.
  This function replaces the original using with-redefs. The original function
  has no way to filter testing vars.
  test-filter is a predicate to filter all vars in namespace ns"
  [test-filter ns]
  (let [once-fixture-fn (cljtest/join-fixtures (:clojure.test/once-fixtures (meta ns)))
        each-fixture-fn (cljtest/join-fixtures (:clojure.test/each-fixtures (meta ns)))]
    (once-fixture-fn
     (fn []
       (doseq [v (filter test-filter (vals (ns-interns ns)))]
         (when (:test (meta v))
           (each-fixture-fn (fn [] (cljtest/test-var v)))))))))

(defn- do-run-tests
  "Run all tests in namespaces subject to a var filter.
  test-filter is a predicate to filter vars in the namespace.
  It will require all namespaces"
  [namespaces test-filter]
  (doseq [n namespaces]
    (require n))
  (with-redefs [cljtest/test-all-vars (partial my-test-all-vars test-filter)]
    (apply cljtest/run-tests namespaces)))

(defn has-meta?
  "Predicate function, true if the passed argument contains the truthy metadata key."
  [key]
  #(-> % meta (get key)))

(defonce
  ^{:doc "File modification time tracker"}
  main-tracker (atom nil))

(defn init-tracker
  "Initialize the modification time tracker. It will reload everything the first time."
  [watch-dirs]
  (swap! main-tracker #(or (and % (= (:dirs %) (set watch-dirs)) %)
                           {:dirs (set watch-dirs)
                            :tracker (tracker (map file watch-dirs) 0)})))

(defn- changed-namespaces
  "Return a sequence of modified namespaces or nil if none."
  []
  (seq ((:tracker @main-tracker))))

(defn- reload-changed
  "Returns a no arguments function that will reload modified files in watch-dirs."
  [& watch-dirs]
  (init-tracker watch-dirs)
  (fn []
    (when-let [n (changed-namespaces)]
      (println (format "Reloading: %s" (join ", " n)))
      (apply reload n))))

;; # Public API

(defn match-namespaces
  "Convert a set of namespace matching instructions in a predicate function.
  For each argument passed, the matching depends on the type:

  * :all matches all namespaces.
  * Symbols match namespaces with equal names.
  * Keywords match corresponding metadata keys on the namespaces. So for instance
  (ns ^:wip my-test) will be matched by (match-namespaces :wip).
  * Functions are used as predicates for the match.
  * Regular expressions are matched against the namespace name.
  * Sequences have their elements anded, so (match-namespaces [:wip :integration #\".*edit.*\"]) will match all edit integration namespaces that are Work In Progress.
  "
  [& nss]
  (let [f (fn f [ns]
            (cond
              (symbol? ns) #(= ns (ns-name %))
              (= :all ns) (constantly true)
              (keyword? ns) (has-meta? ns)
              (isa? (class ns) java.util.regex.Pattern)
              #(re-matches ns (str (ns-name %)))
              (fn? ns) ns
              (seqable? ns) (fn [n] (every? #(% n) (map f ns)))
              :else (throw (IllegalArgumentException.
                             (str "Invalid namespace definition: " ns)))))]
    {:namespace-filter (fn [n] (some #(% n) (map f nss)))}))

(defn match-tests
  "Convert a set of test matching instructions in a predicate function.
  For each argument passed, the matching depends on the type:

  * :all matches all tests.
  * Symbols match test vars with equal names.
  * Keywords match corresponding metadata keys on the vars. So for instance:
        (deftest ^:wip my-test
          (is true))
    will be matched by `(match-tests :wip)`.
  * Functions are used as predicates for the match.
  * Regular expressions are matched against the test function name.
  * Sequences have their elements anded, so (match-tests [:wip :integration #\".*edit.*\"]) will match all edit integration tests that are Work In Progress.
  "
  [& ts]
  (let [f (fn f [v]
            (cond
              (symbol? v) #(= (.sym %) v)
              (= :all v) (constantly true)
              (keyword? v) (has-meta? v)
              (fn? v) v
              (isa? (class v) java.util.regex.Pattern) #(re-matches v (str (.sym %)))
              (seqable? v) (fn [va] (every? #(% va) (map f v)))
              :else (throw (IllegalArgumentException. "Invalid tests definition"))))]
  {:test-filter (fn [n] (some #(% n) (map f ts)))}))

(defn run
  "Run tests. Receives a map of options:

  * :test-filter is a predicate that will receive each testing var, it should return
  falsey for those var that shouldn't be run. Default: run all tests.
  * :namespace-filter does the same filtering but with whole namespaces, it's a predicate
  that receives a namespace and returns truthy if it should be tested. Default: run all namespaces.
  * :before is a function called without arguments before executing any tests. Default: noop
  * :after is a function called with the results map after executing all tests. Default: identity.
  * :test-dir is a string with the directory where your tests reside. Default: \"test\".
  "
  [{test-filter :test-filter ns-filter :namespace-filter
    before :before after :after dir :test-dir
    :or {test-filter (constantly true) ns-filter (constantly true)
         dir "test" before (fn []) after identity}}]
  (before)
  (let [dir (file dir)
        nss (get-test-namespaces dir ns-filter)]
    (after (do-run-tests nss test-filter))))

(defn run-tests
  "Run tests. This is the main entry point for normal use. Receives keyword arguments:

  * :namespaces is one or a sequence of namespace selection instructions, see (match-namespaces). Default: all namespaces.
  * :tests is one or a sequence of test selection instructions, see (match-tests). Default: all tests.
  * :test-dir is a string with the directory where your tests reside. Default: \"test\".
  * :reload-dirs is one or a sequence of directories to watch for changes and reload if needed. Default: [\"src\" \"test\"].
  * :reload? if false, no reload will be done, even if source files changed. Default: true.
  * :before is a function called without arguments before executing any tests. Default: noop
  * :after is a function called with the results map after executing all tests. Default: identity.
  "
  [& {:keys [namespaces tests reload-dirs reload? before after test-dir]
      :or {reload-dirs ["src" "test"] before (fn []) after (fn [_]) test-dir "test"}
      :as options}]
  (let [nss (if namespaces
              (apply match-namespaces namespaces)
              {})
        tests (if tests
                (apply match-tests tests)
                {})
        reload-dirs (flatten [reload-dirs])
        reload? (not (false? reload?))
        hooks {:before (if reload?
                         #(do
                            (before)
                            ((apply reload-changed reload-dirs)))
                         before)
               :after after}]
  (run (merge hooks nss tests))))
