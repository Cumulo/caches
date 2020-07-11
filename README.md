
Function memoization sulotion for Cumulo and Respo
----

> a memo library as a replacement for memoizations in Cumulo and Respo. It's mostly experimental.

### Usage

[![Clojars Project](https://img.shields.io/clojars/v/cumulo/memof.svg)](https://clojars.org/cumulo/memof)

```edn
[cumulo/memof "0.2.0-a1"]
```

```clojure
(defonce *states (memof.core/new-caches))
(memof.core/show-summay! *states)
```

States structure:

```edn
{
  :loop 0 ; counter
  :entries { ; where entries are stored
    f1 {
      :hit-times 1, :missed-times 1
      :records {
        [p1 p2] {:value 1, :hit-times 1, :last-hit-loop 1, :initial-loop 1}
      }
    }
  }
  :gc { ; configurations
    :cold-duration 400, ; wait for N loops before triggering GC
    :trigger-loop 100, ; trigger GC every N loops
    :elapse-loop 50 ; entries are considered unuseful after not used for N loops
  }
}
```

Methods:

* `(new-states)` returns an atom holding states
* `(show-summary! *states)` list entries after formatted
* `(write-record! *states f params value)` write to cache, `params` supposed to be a collection
* `(access-record *states f params)` access and return value(or `nil`)
* `(new-loop! *states)` loop and trigger actions
* `(perform-gc! *states)` remove entries that are probably no longer useful
* `(reset-entries! *states)` clear entries

### Workflow

https://github.com/mvc-works/calcit-nodejs-workflow

### License

MIT
