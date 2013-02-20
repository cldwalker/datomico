(ns datomico.model-test
  (:require [clojure.test :refer :all :exclude [testing] :as ct]
            [datomico.test-helper :refer [always-with-latest-db]]
            [datomico.db :as db]
            datomico.core
            [datomico.model :as model]))

(def model :item)
(model/create-model-fn :find-id model)
(model/create-model-fn :find-all model)
(model/create-model-fn :find-first model)
(model/create-model-fn :delete-all model)
(model/create-model-fn :delete-all-tx model)
(model/create-model-fn :find-or-create model)
(model/create-model-fn :update model)
(model/create-model-fn :update-tx model)
(model/create-model-fn :create model)
(model/create-model-fn :create-tx model)
(model/create-model-fn :delete-value model)
(model/create-model-fn :delete-value-tx model)

(defn load-schemas []
  (let [schema (datomico.core/build-schema
                model [[:name :string] [:type :string]])]
    (db/load-schemas schema)))

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

(deftest delete-all-tx-test
  (testing "returns tx data for delete-all"
    (let [ent (create {:name "one"})]
      (is (= (list [:db.fn/retractEntity (:id ent)]) (delete-all-tx {:name "one"}))))))

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

(deftest create-tx-test
  (ct/testing "Generates a namespaced map with a temp id"
    (let [attr (create-tx {:name "jim" :type "actor"})]
      (is (= datomic.db.DbId (class (:db/id attr))))
      (is (= {:item/name "jim" :item/type "actor"}
             (dissoc attr :db/id)))))
  (ct/testing "Moves :id to :db/id if given one"
    (let [attr (create-tx {:name "jim" :type "actor" :id -1000})]
      (is (= {:db/id -1000 :item/name "jim" :item/type "actor"}
             attr)))))

(deftest update-test
  (testing "updates attributes of a given id"
    (let [ent (create {:name "vague"})]
      (update (:id ent) {:name "specific"})
      (= ent (find-first {:name "specific"})))))

(deftest update-tx-test
  (ct/testing "returns tx data for update"
    (is (= {:item/name "specific" :db/id -100})
        (update-tx -100 {:name "specific"}))))

(deftest delete-value-test
  (testing "deletes value for attribute"
    (let [ent (create {:name "forgetfulness" :type "memory"})]
      (delete-value (:id ent) :type "memory")
      (let [new-ent (find-id (:id ent))]
        (is (= {:name "forgetfulness"} (dissoc new-ent :id)))))))

(deftest delete-value-tx-test
  (ct/testing "returns tx data for delete-value"
    (is (= [:db/retract 100 :item/type "fake"] (delete-value-tx 100 :type "fake")))))