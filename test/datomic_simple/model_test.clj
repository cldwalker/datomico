(ns datomic-simple.model-test
  (:require [clojure.test :refer :all :exclude [testing]]
            [datomic-simple.test-helper :refer [always-with-latest-db]]
            [datomic-simple.db :refer [with-latest-database] :as dsb]
            datomic-simple.core
            [datomic-simple.model :refer [find-id] :as model]))

(def model :item)
(model/create-model-fn :create model)
(model/create-model-fn :find-all model)
(model/create-model-fn :find-first model)
(model/create-model-fn :delete-all model)

(defn load-schemas []
  (let [schema (datomic-simple.core/build-schema
                model [[:name :string] [:url :uri] [:type :string]])]
    (dsb/load-schemas schema)))

(defmacro testing [str & body]
  `(clojure.test/testing ~str
     (always-with-latest-db
       (load-schemas)
       ~@body)))

(deftest find-id-test
  (testing "finds by id and returns map"
    (let [ent (create {:name "dude"})]
      (is (= {:id (ent :id) :name "dude"} (find-id (:id ent))))))
  (testing "doesn't find id and returns nil"
    (let [ent (create {:name "dude"})]
      (is (nil? (find-id (inc (:id ent))))))))

(deftest find-all-test
  (testing "with no args returns all in namespace"
    (let [ent (create {:name "water"})]
      (is (= [ent] (find-all)))))
  (testing "with an empty map raises AssertionError"
    (is (thrown? java.lang.AssertionError
                 (find-all {}))))
  (testing "with one pair returns correct result"
    (let [_ (create {:name "nitrogen" :type "element"})
          ent (create {:name "oxygen" :type "element"})
          ent2 (create {:name "oxygen" :type "drink"})]
      (is (= [ent ent2] (find-all {:name "oxygen"})))))
  (testing "with two pairs returns correct result"
    (let [_ (create {:name "nitrogen" :type "element"})
          ent (create {:name "oxygen" :type "element"})
          ent2 (create {:name "oxygen" :type "drink"})]
      (is (= [ent] (find-all {:name "oxygen" :type "element"}))))))

(deftest delete-all-test
  (testing "with no args deletes all"
    (create {:name "one"})
    (create {:name "two"})
    (assert (= 2 (count (find-all))))
    (delete-all)
    (is (= 0 (count (find-all)))))
  (testing "with matching query deletes one"
    (create {:name "one"})
    (create {:name "two"})
    (assert (= 2 (count (find-all))))
    (delete-all {:name "one"})
    (is (= 1 (count (find-all)))))
  (testing "with no matching query deletes none"
    (create {:name "one"})
    (create {:name "two"})
    (assert (= 2 (count (find-all))))
    (delete-all {:name "none"})
    (is (= 2 (count (find-all))))))

(deftest find-first-test
  (testing "returns first result"
    (let [_ (create {:name "apple"})
          ent (create {:name "banana"})]
      (is (= ent (find-first {:name "banana"})))))
  (testing "returns nil if no result found"
    (create {:name "apple"})
    (is (nil? (find-first {:name "peac"})))))