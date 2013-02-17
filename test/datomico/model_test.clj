(ns datomico.model-test
  (:require [clojure.test :refer :all :exclude [testing]]
            [datomico.test-helper :refer [always-with-latest-db]]
            [datomico.db :as dsb]
            datomico.core
            [datomico.model :refer [find-id] :as model]))

(def model :item)
(model/create-model-fn :create model)
(model/create-model-fn :find-all model)
(model/create-model-fn :find-first model)
(model/create-model-fn :delete-all model)
(model/create-model-fn :update model)
(model/create-model-fn :find-or-create model)
(model/create-model-fn :build-attr model)

(defn load-schemas []
  (let [schema (datomico.core/build-schema
                model [[:name :string] [:type :string]])]
    (dsb/load-schemas schema)))

(defmacro testing [str & body]
  `(clojure.test/testing ~str
     (always-with-latest-db
       (load-schemas)
       ~@body)))

(defn count-changes-by [f diff]
  (let [begin-count (count (find-all))
        _ (f)
        end-count (count (find-all))]
    (is (= diff (- end-count begin-count)))))

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
    (count-changes-by delete-all -2))
  (testing "with matching query deletes one"
    (create {:name "one"})
    (create {:name "two"})
    (count-changes-by #(delete-all {:name "one"}) -1))
  (testing "with no matching query deletes none"
    (create {:name "one"})
    (create {:name "two"})
    (count-changes-by #(delete-all {:name "none"}) 0)))

(deftest find-first-test
  (testing "returns first result"
    (let [_ (create {:name "apple"})
          ent (create {:name "banana"})]
      (is (= ent (find-first {:name "banana"})))))
  (testing "returns nil if no result found"
    (create {:name "apple"})
    (is (nil? (find-first {:name "peac"})))))

;; yeah, we've been using this everywhere but at least show
;; what it does
(deftest create-test
  (testing "creates an ent and returns its map with id assoc'd in"
    (let [ent (create {:name "dude"})]
      (is (integer? (:id ent)))
      (is (= ent (find-id (:id ent)))))))

(deftest find-or-create-test
  (testing "creates an ent if none found"
    (count-changes-by
     #(find-or-create {:name "singleton"})
     1))
  (testing "finds an ent"
    (create {:name "singleton"})
    (count-changes-by
     #(let [ent (find-or-create {:name "singleton"})]
        (is (= "singleton" (:name ent))))
     0)))

(deftest build-attr-test
  (clojure.test/testing "Generates a namespaced map with a temp id"
    (let [attr (build-attr {:name "jim" :type "actor"})]
      (is (= datomic.db.DbId (class (:db/id attr))))
      (is (= {:item/name "jim" :item/type "actor"}
             (dissoc attr :db/id))))))

(deftest update-test
  (testing "updates attributes of a given id"
    (let [ent (create {:name "vague"})]
      (update (:id ent) {:name "specific"})
      (= ent (find-first {:name "specific"})))))
