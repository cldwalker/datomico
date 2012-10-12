(ns datomic-simple.db
  (:require [datomic.api :as d]))
;;; Handles database values and connections

(def ^:dynamic *uri*)
(def ^:dynamic *connection*)
(def ^:dynamic *db*)

; from https://gist.github.com/3150938
(defn wrap-datomic
  "A Ring middleware that provides a request-consistent database connection and
  value for the life of a request."
  [handler uri]
  (fn [request]
    (let [conn (d/connect uri)]
      (binding [*connection* conn
                *db*         (d/db conn)]
        (handler request)))))

; *db* fns - these get overriden by repl-init in a repl
(defn q [query & args] (apply d/q query *db* args))
(defn entity [id] (d/entity *db* id))
(defn resolve-tempid [tempids tempid] (d/resolve-tempid *db* tempids tempid))

; from https://gist.github.com/3150938
(defmacro with-latest-database
  "Runs the body with the latest version of that database bound to
  *db*, rather than the request-consistent database."
  [& body]
  `(binding  [*db*  (d/db *connection*)]
    ~@body))

(defn repl-init [uri]
  (def ^:dynamic *connection* (d/connect uri))
  (defn q [query & args] (with-latest-database (apply d/q query *db* args)))
  (defn resolve-tempid [tempids tempid] (with-latest-database (d/resolve-tempid *db* tempids tempid)))
  (defn entity [id] (with-latest-database (d/entity *db* id))))

(defn transact [tx]
  (d/transact *connection* tx))

(defn transact! [tx]
  (prn "Transacting..." tx)
  @(transact tx))

(defn load-schemas [uri schemas]
  (binding [*connection* (d/connect uri)]
    (transact! (flatten schemas))))

(defn add-new-id [attr]
  (merge {:db/id (d/tempid :db.part/user)} attr))

(defn load-seed-data [uri data]
   (binding [*connection* (d/connect uri)]
    (transact! (map add-new-id (flatten data)))))

(defn set-uri [uri]
  (def ^:dynamic *uri* uri))
