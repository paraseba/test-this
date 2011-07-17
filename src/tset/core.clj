(ns tset.core
  (:use [lazytest.reload :only [reload]]
        [clojure.string :only [join]])
  (:require clojure.tools.namespace
            clojure.java.io
            [lazytest.tracker :as tracker]
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
       (doseq [v (filter test-filter (vals (ns-interns ns)))]
         (when (:test (meta v))
           (each-fixture-fn (fn [] (cljtest/test-var v)))))))))

(defn do-run-tests [namespaces test-filter]
  (doseq [n namespaces]
    (require n))
  (with-redefs [cljtest/test-all-vars (partial my-test-all-vars test-filter)]
    (apply cljtest/run-tests namespaces)))

(defn run [{test-filter :test-filter ns-filter :namespace-filter
            before :before after :after dir :test-dir
            :or {test-filter (constantly true) ns-filter (constantly true)
                 dir "test" before (fn [& args]) after (fn [& args])}}]
  (before)
  (let [dir (clojure.java.io/file dir)
        nss (get-test-namespaces dir ns-filter)]
    (after (do-run-tests nss test-filter))))

(defn before [f] {:before f})

(defn namespaces [nss]
  {:namespace-filter
   (cond
     (symbol? nss) #(= nss (ns-name %))
     (= :all nss) (constantly true)
     (keyword? nss) #(= (symbol (name nss)) (ns-name %))
     (vector? nss) (fn [n] (some #(% n) (map (comp :namespace-filter namespaces) nss)))
     (isa? (class nss) java.util.regex.Pattern) #(re-matches nss (str (ns-name %)))
     (fn? nss) nss
     :else (throw (IllegalArgumentException. "Invalid namespace definition")))})

(defn tests [ts]
  {:test-filter
   (cond
     (symbol? ts) #(= (.sym %) ts)
     (= :all ts) (constantly true)
     (keyword? ts) #(= (.sym %) (symbol (name ts)))
     (vector? ts) (fn [v] (some #(% v) (map (comp :test-filter tests) ts)))
     (fn? ts) ts
     (isa? (class ts) java.util.regex.Pattern) #(re-matches ts (str (.sym %)))
     :else (throw (IllegalArgumentException. "Invalid tests definition")))})

(defn reload-changed [& watch-dirs]
  (let [track (tracker/tracker (map clojure.java.io/file watch-dirs) 0)]
    (fn []
      (when-let [t (seq (track))]
        (println (format "Reloading: %s" (join ", " t)))
        (apply reload t)))))

(defonce default (before (reload-changed "src" "test")))
