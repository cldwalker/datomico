(ns datomic-simple.core
  (:require [datomic.api :as api]
            [datomic-simple.model :as dm]
            datomic-simple.action
            [datomic-simple.db :as db]
            [datomic-simple.util :as util]))

(defmacro ^:private add-noir-middleware [uri]
  `(noir.server/add-middleware db/wrap-datomic ~uri))

(defn- rand-connection []
  (str "datomic:mem://" (java.util.UUID/randomUUID)))

(defn start [{:keys [uri schemas seed-data repl]}]
  (let [uri (or uri (rand-connection))]
    (db/set-uri uri)
    (api/create-database uri)
    (when (seq schemas)
      (db/load-schemas uri schemas))
    (when (seq seed-data)
      (db/load-seed-data uri seed-data))
    (when (some #{"noir.server"} (map str (all-ns)))
      (add-noir-middleware uri))
    (when repl
      (db/repl-init uri))))

(defn- build-schema-attr [attr-name value-type & options]
  (let [cardinality (if (some #{:many} options)
                      :db.cardinality/many
                      :db.cardinality/one)
        fulltext    (if-not (= value-type :string) false 
                      (not (boolean (some #{:nofulltext} options))))
        history     (boolean (some #{:nohistory} options))
        index       (not (boolean (some #{:noindex} options)))]
    
    {:db/id           (api/tempid :db.part/db)
     :db/ident        attr-name
     :db/valueType    (keyword "db.type" (name value-type))
     :db/cardinality  cardinality
     :db/index        index
     :db/fulltext     fulltext
     :db/noHistory    history
     :db.install/_attribute :db.part/db}))
    
(defn build-schema [nsp attrs]
  (map #(apply build-schema-attr
               (keyword (name nsp) (name (first %))) (rest %))
       attrs))

(defn build-seed-data [nsp attrs]
  (map (partial util/namespace-keys nsp) attrs))

(def find-id datomic-simple.model/find-id)
(def expand-ref datomic-simple.model/expand-ref)
(def delete datomic-simple.action/delete)

; TODO: allow user to pass in which fns they want to create
(defmacro create-model-fns
  "Creates model fns that scope to the given model namespace."
  [nsp]
  `(do
    (dm/create-model-fn :create ~nsp)
    (dm/create-model-fn :find-all-by ~nsp)
    (dm/create-model-fn :find-first ~nsp)
    (dm/create-model-fn :find-all ~nsp)
    (dm/create-model-fn :delete-all ~nsp)
    (dm/create-model-fn :update ~nsp)))
