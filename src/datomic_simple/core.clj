(ns datomic-simple.core
  (:require [datomic.api :as api]
            [datomic-simple.model :as dm]
            datomic-simple.action
            [datomic-simple.db :as db]
            [datomic-simple.util :as util]))

(defn- add-noir-middleware [uri]
  (@(resolve (symbol "noir.server" "add-middleware")) db/wrap-datomic uri))

(defn- rand-connection []
  (str "datomic:mem://" (java.util.UUID/randomUUID)))

(defn start
  "Start datomic by creating a connection, creating a database and optionally loading schema and
  seed data.  When calling this from a noir app, appopriate middleware is added so the latest
  database value and connection are made available.
  Options:

  :uri       Specify a uri string. Defaults to a randomly generated value.
  :schemas   A vector of schemas, each schema representing a model. A schema is a vector of maps,
             each requiring 3 attributes. They can be simply defined using build-schema. Default is nil.
  :seed-data A vector of vectors, each subvector representing seed data for a model. This is loaded
             after schemas. Default is nil.
  :repl      A boolean when enabled that allows all datomic fns in the repl to work. Default is false.
  "
  [{:keys [uri schemas seed-data repl]}]
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
    
(defn build-schema
  "Given a keyword namespace and a vector of vectors, creates a schema as described at
  http://docs.datomic.com/schema.html.  Each subvector represents a field and only requires a name
  and type. By default, all fields are indexed, have history and are assumed to have a cardinality
  of one. To override these defaults, a subvector can contain these additional elements:

  :many       Indicates a cardinality of many.
  :nofulltext Disables fulltext. If a field is of type string, fulltext is enabled by default.
  :nohistory  Disables history.
  :noindex    Disables index.

  Example:
  (build-schema :user
    [[:username :string :nofulltext]
     [:email :string]])
  "
  [nsp attrs]
  (map #(apply build-schema-attr
               (keyword (name nsp) (name (first %))) (rest %))
       attrs))

(defn build-seed-data
  "Given a keyword namespace and a vector of data, prepares seed data for a transaction."
  [nsp attrs]
  (map (partial util/namespace-keys nsp) attrs))

(def find-id datomic-simple.model/find-id)
(def expand-ref datomic-simple.model/expand-ref)
(def delete datomic-simple.action/delete)

; TODO: allow user to pass in which fns they want to create
(defmacro create-model-fns
  "Creates model fns that are scoped to the given model (keyword namespace). Creates the following
  fns: create, update, delete-all, find-all, find-first and find-all-by."
  [nsp]
  `(do
    (dm/create-model-fn :create ~nsp)
    (dm/create-model-fn :find-all-by ~nsp)
    (dm/create-model-fn :find-first ~nsp)
    (dm/create-model-fn :find-all ~nsp)
    (dm/create-model-fn :delete-all ~nsp)
    (dm/create-model-fn :update ~nsp)))
