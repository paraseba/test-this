(ns tset.core
  (:use [clojure.core.incubator :only [seqable?]]
        [clojure.tools.namespace :only [find-namespaces-in-dir]]
        [clojure.string :only [join]]
        [clojure.java.io :only [file]]
        [lazytest.reload :only [reload]]
        [lazytest.tracker :only [tracker]])
  (:require [clojure.test :as cljtest]))

(defn get-test-namespaces [base-dir ns-filter]
  (filter ns-filter
          (find-namespaces-in-dir base-dir)))

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
  (let [dir (file dir)
        nss (get-test-namespaces dir ns-filter)]
    (after (do-run-tests nss test-filter))))

(defn before [f] {:before f})

(defn namespaces [& nss]
  (let [f (fn f [ns]
            (cond
              (symbol? ns) #(= ns (ns-name %))
              (= :all ns) (constantly true)
              (keyword? ns) #(= (symbol (name ns)) (ns-name %))
              (isa? (class ns) java.util.regex.Pattern)
              #(re-matches ns (str (ns-name %)))
              (fn? ns) ns
              (seqable? ns) (fn [n] (every? #(% n) (map f ns)))
              :else (throw (IllegalArgumentException.
                             (str "Invalid namespace definition: " ns)))))]
    {:namespace-filter (fn [n] (some #(% n) (map f nss)))}))

(defn tests [& ts]
  (let [f (fn f [v]
            (cond
              (symbol? v) #(= (.sym %) v)
              (= :all v) (constantly true)
              (keyword? v) #(= (.sym %) (symbol (name v)))
              (fn? v) v
              (isa? (class v) java.util.regex.Pattern) #(re-matches v (str (.sym %)))
              (seqable? v) (fn [va] (every? #(% va) (map f v)))
              :else (throw (IllegalArgumentException. "Invalid tests definition"))))]
  {:test-filter (fn [n] (some #(% n) (map f ts)))}))

(defn reload-changed [& watch-dirs]
  (let [track (tracker (map file watch-dirs) 0)]
    (fn []
      (when-let [t (seq (track))]
        (println (format "Reloading: %s" (join ", " t)))
        (apply reload t)))))

(defonce default (before (reload-changed "src" "test")))

(defn has-meta? [key] (fn [v] (contains? (meta v) key)))

(defn tset [& options]
  (run (apply merge default options)))
