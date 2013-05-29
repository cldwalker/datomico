## Description

Use datomic with less cognitive overhead. Should work with datomic >= 0.8.3789.

[![Build Status](https://travis-ci.org/cldwalker/datomico.png?branch=master)](https://travis-ci.org/cldwalker/datomico)

## Usage

Defining a model and starting datomic should be easy:

```clojure
(ns models.user)
(require '[datomico.core :as dc])

; Define a model's database key once and don't think of it again while
; interacting with your model
(def model-namespace :user)

; Build schemas easily without needing to think of partitions and a number of
; internal schema attributes. Basically you don't have to pour over
; http://docs.datomic.com/schema.html
(def schema (dc/build-schema model-namespace
  [[:username :string]
  [:password :string]]))

; Starting in a repl is easy;  a uri, db and connection will be auto-generated
(ns user)
(require '[datomico.core :as dc])
(dc/start {:dynamic-vars true
           :schemas [models.user/schema]})
```

Creating, updating, deleting and querying should be easy for models:

```clojure
(ns models.user)
; creates several helper functions that scope your database interaction to the model.
(dc/create-model-fns model-namespace)

(create {:username "dodo" :password "bird"})
; => {:username "dodo" :password "bird" :id 1024053}
(find-all {:username "dodo"})
; => ({:username "dodo" :password "bird" :id 1024053})
(find-first {:password "bird"})
; => {:username "dodo" :password "bird" :id 1024053}

(update 1024053 {:username "big"})
(dc/delete 1024053)
```

## Transacting Batch Data

To transact data in batches, datomico provides *-tx corollaries to fns in `datomico.action` and
`datomico.model`. These fns generate transaction data which can be batched and transacted as needed.

For this example, assume we're in a url model that has string attributes :name and :desc and a many ref with :tags.
Let's create a new entity that associates itself with two existing entities "clojure" and "database". We'll use
`create`'s corollary `create-tx` to do this

```clojure
(ns models.url
  (:require [datomico.db :as db]
            [datomic.model :as model]))

(def model-namespace :url)
(def find-first (partial model/find-first model-namespace))
(def create-tx (partial model/create-tx model-namespace))

(let [input {:name "http://datomic.com" :desc "DESC" :tags ["clojure" "database"]}
      new-map (create-tx (dissoc input :tags))]
  (->> (:tags input)
       (map #(find-first {:name %}))
       (map #(create-tx {:id (:db/id new-map) :tags (:id %)}))
       (cons new-map)
       db/transact!))
```

Note that `create-tx` provides `:db/id` which is a tempid you can use to associate it to other entities
in the same transaction.

## Dynamic Binding

For actual production code, it is *not* recommended to use `:dynamic-vars` with
`datomico.core/start`. Instead, you should be more explicit about handling your `*db*` and
`*connection*` as `:dynamic-vars` incurs a performance penalty in exchange for convenience. For
example, a ring app should start datomic with:

```clojure
(dc/start {:uri "datomic:mem://my-app"
           :schemas [models.user/schema]})
```

Then, your ring app should use datomico's ring middleware to
explicitly define `*db*` and `*connection*` for the duration of a web request:

```clojure
(-> app datomico.db/wrap-datomic)
```

Look at `wrap-datomic`'s implementation if not in a ring context.

## TODO
* More tests!

## Bugs/Issues

Please report them [on github](http://github.com/cldwalker/datomico/issues).

## License

See LICENSE.TXT

## Credits
* @relevance for fridays!
* @bobby for much of the initial abstraction - https://gist.github.com/3150938
* @boxxxie - for schema improvements
* @ravster - for tests

## Links
* [Sample blog using datomico](https://github.com/cldwalker/datomic-noir-blog) - Needs to be
  updated. Last known working commit - 50da3787e11608a98ed54220a371c7035ccdfa12.
* [Sample pastebin using datomico](https://github.com/cldwalker/datomic-refheap) - Needs to be
  updated. Last known working commit - 50da3787e11608a98ed54220a371c7035ccdfa12.
