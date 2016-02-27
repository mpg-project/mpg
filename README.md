# jdbc-pg-sanity

Sane defaults for working with clojure.jdbc and postgresql.
No more boilerplate.

Handles the following:

- dates to java.util.Date
- json and jsonb to clojure maps
- postgres arrays to clojure vectors


## Usage

```clojure
(ns whatever.db
    (require [clojure.java.jdbc :as j]
             jdbc-pg-sanity))

;; do your postgres thing here
```


## Contributing

Contributions and improvements welcome, just open an issue! :)


## License

Copyright © 2016 Shane Kilkelly

Distributed under the MIT license.
