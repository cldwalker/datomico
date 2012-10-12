(ns datomic-simple.model
  (:require [datomic-simple.util :as util]
            [datomic-simple.db :as db]
            [datomic-simple.action :as action]))
;;; CRUD db action that scope to a datomic model/namespace i.e. :user

(defn- localize-attr [attr]
  (util/map-keys attr #(keyword (name %))))

(defn- all [query]
  (map localize-attr (action/find-all query)))

(defn local-find-id [id]
  (if-let [m (action/find-id id)] (localize-attr m)))

(defn local-find-by [nsp query-map]
  (map localize-attr (action/find-by (util/namespace-keys nsp query-map))))

(defn expand-ref [m]
  (if (empty? m) nil (localize-attr (action/entity->map m))))

(defn delete-by [nsp query-map]
  (let [results (local-find-by nsp query-map)]
    (when (seq results)
      (apply action/delete (map :id results)))))

(defn local-all-by [nsp field]
  (all (format "[:find ?e :where [?e %s/%s]]" nsp (name field))))

(defn local-find-first-by [nsp query-map]
  (first (local-find-by nsp query-map)))

(defn- build-attr [nsp attr]
  (db/add-new-id (util/namespace-keys nsp attr)))

(defn create [nsp attr]
  (let [nsp-attr (build-attr nsp attr)
        tx-result (db/transact! [nsp-attr])
        new-id (db/resolve-tempid (:tempids tx-result) (:db/id nsp-attr))]
    (merge attr {:id new-id})))

(defn update [nsp id attr]
  (action/bare-update id (util/namespace-keys nsp attr)))

; TODO: fix - only worked in global namespace
;(defmacro create-model-fn
;  "Creates a local function that wraps a datomic-simple fn with a namespace arg."
;  [fn-name nsp]
;  `(do
;    (def ~(symbol (name fn-name))
;      (partial ~(deref (resolve (symbol "datomic-simple.model" (name fn-name)))) ~nsp))))
;
;(defmacro create-model-fns-for
;  "Creates model fns that scope to the given model namespace."
;  [nsp]
;  `(do
;    (create-model-fn :create ~nsp)
;    (create-model-fn :local-all-by ~nsp)
;    (create-model-fn :local-find-first-by ~nsp)
;    (create-model-fn :local-find-by ~nsp)
;    (create-model-fn :delete-by ~nsp)
;    (create-model-fn :update ~nsp)))
