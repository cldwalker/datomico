(ns datomico.model
  (:require [datomico.util :as util]
            [datomico.db :as db]
            [datomico.action :as action]))
;;; CRUD db action that scope to a datomic model/namespace i.e. :user

(defn find-id
  "If entity is found, returns it as a map with no namespace. Otherwise returns nil."
  [id]
  (if-let [m (action/find-id id)] (util/localize-attr m)))

(defn find-all
  "Queries with given map of attribute names to values and returns a vector of maps with no namespace."
  ([nsp] (map util/localize-attr (action/all nsp)))
  ([nsp query-map]
    {:pre [(seq query-map)]}
    (map util/localize-attr (action/find-all (util/namespace-keys nsp query-map)))))

(defn expand-ref
  "Given the value from a field that is of type ref, expands the relationship to return its map."
  [m]
  (if (empty? m) nil (util/localize-attr (action/entity->map m))))

(defn delete-all
  "Deletes all entities that match a map-based query."
  [nsp & [query-map]]
  (let [results (if query-map (find-all nsp query-map) (find-all nsp))]
    (when (seq results)
      (apply action/delete (map :id results)))))

(defn find-first
  "Given a map-based query, returns first result as a map with no namespace or nil."
  [nsp query-map]
  (first (find-all nsp query-map)))

(defn build-attr [nsp attr]
  (db/add-new-id (util/namespace-keys nsp attr)))

(defn create
  "Creates an entity given a map and returns the created map with its new id in :id."
  [nsp attr]
  (let [nsp-attr (build-attr nsp attr)
        tx-result (db/transact! [nsp-attr])
        new-id (db/resolve-tempid (:tempids tx-result) (:db/id nsp-attr))]
    (merge attr {:id new-id})))

(defn find-or-create
  "Finds an entity by given attributes or creates it."
  [nsp attr]
  (or (find-first nsp attr) (create nsp attr)))

(defn update
  "Updates given id with map of attributes."
  [nsp id attr]
  (action/update id (util/namespace-keys nsp attr)))

(defmacro create-model-fn
  "Creates a local function that wraps a datomico fn with a keyword namespace (model scope)."
  [fn-name nsp]
  `(do
    (def ~(symbol (name fn-name))
      (partial ~(deref (resolve (symbol "datomico.model" (name fn-name)))) ~nsp))))
