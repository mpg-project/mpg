# mpg - more modern postgres to the gallon

Eases interoperability between clojure/java datatypes and postgres
datatypes. No more boilerplate!

Handles the following:

- `DATE` <-> `java.time.LocalDate`
- `TIMESTAMP/TIMESTAMPTZ` <-> `java.time.ZonedDateTime`
- `JSON/JSONB` <-> clojure map/vector
- `ARRAY` (e.g. `int[]`) <-> clojure vector
- `BYTEA` <-> byte array
- `HSTORE` <-> clojure map (limited support - jdbc stringifies all contents)

Can also insert (but not retrieve) the following types:

- `java.util.Date` -> `DATE/TIMESTAMP/TIMESTAMPTZ`
- `java.sql.Timestamp` -> `DATE/TIMESTAMP/TIMESTAMPTZ`
- `java.nio.ByteBuffer` -> `BYTEA`


[![Build Status](https://travis-ci.org/mpg-project/mpg.svg?branch=master)](https://travis-ci.org/mpg-project/mpg)

Note: this library was once called [jdbc-pg-sanity](https://clojars.org/jdbc-pg-sanity), `mpg` is the new version.

## Installation

Add `mpg` as a leiningen or boot dependency:

```clojure
[mpg "1.3.0"]
```

[![Clojars Project](https://img.shields.io/clojars/v/mpg.svg)](https://clojars.org/mpg)


## Usage

Just require the `mpg.core` namespace and call `patch`

```clojure
(ns whatever.db
    (require [clojure.java.jdbc :as j]
             [mpg.core :as mpg]]))
(mpg/patch) ;; take the default settings
(mpg/patch {:default-map :hstore}) ;; custom settings are merged with the defaults
;; valid settings:
    :data        - boolean, default true. auto-map maps and vectors?
    :datetime    - boolean, default true. auto-map java.time.{LocalDate, ZonedDateTime} ?
    :default-map - keyword. one of :json, :jsonb, :hstore. Default :jsonb
```

## Limitations

The current clojure.java.jdbc interface imposes some limitations on us.

1. You only get the autoconversion when using clojure.java.jdbc or something built on it
2. When using unbound statements, we cannot save a vector as an array type (we therefore use json)
3. When using unbound statements, you must choose between storing maps as json or hstore (default: json)
4. All applications that have written to the database are assumed to have correctly saved timestamps in UTC. If you only use this library, you won't have to worry about that. Most applications can be configured with the TZ environment variable

## Running tests

You need a database and a user on it with which we can run tests.

You can provide information about these with environment variables:
```bash
MPG_TEST_DB_URI # default is '//127.0.0.1:5432/mpg_test'
MPG_TEST_DB_USER
MPG_TEST_DB_PASS
```

You can create a postgres database to test with `createdb` and give your
user permissions with `GRANT` as per normal postgres.

Running the tests is the same as any leiningen-based project:

```shell
boot test
```

## Contributing

Contributions and improvements welcome, just open an issue! :)

If this library should do something, and it doesn't currently do it, please fork
and open a pull request. All reasonable contributions will be accepted.

Please run the tests before opening a pull request :)

## Acknowledgements

This library was originally extracted from luminus boilerplate which
hails from code floating around the internet
generally. [James Laver](https://github.com/jjl) basically rewrote it.

## License

Copyright © 2016 Shane Kilkelly, James Laver

Distributed under the MIT license.
