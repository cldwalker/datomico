## Description

Use datomic with less cognitive overhead. Should work with datomic >= 0.8.3524.

## Usage

Setup should be easy:

```clojure
(require '[datomic-simple.core :as db])

; Define a model's database key once and don't think of it again while
; interacting with your model
(def user-key :user)

; Build schemas easily without needing to think of partitions and a number of
; internal schema attributes. Basically you don't have to pour over
; http://docs.datomic.com/schema.html
(def user-schema (db/build-schema user-key
  [[:username :string]
  [:password :string]]))

; Start datomic and initialize schemas without needing to think of database values and connections
; or adding middleware (if using noir)
(db/start {:uri "datomic:mem://my-app"
           :schemas [user-schema]})
```

Creation, updating, deleting and querying should be easy:

```clojure
(db/create user-key {:username "dodo" :password "bird"})
(db/local-find-by user-key {:username "dodo"})
; => ({:username "dodo" :password "bird" :id 1024053})

(db/update user-key 1024053 {:username "big"})
(db/delete 1024053)
```

## TODO
* provide functions that have model keys implicitly scoped
* Docs
* Tests!

## Credits
* @relevance for fridays!
* @bobby for much of the initial abstraction - https://gist.github.com/3150938

## Links
* [Sample blog using datomic-simple](https://github.com/cldwalker/datomic-noir-blog)
