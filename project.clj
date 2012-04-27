;; # Ring XSLT
;;
;; *Repository:* <https://github.com/jedahu/ring-xslt>
(defproject
  ring-xslt "0.1.1-SNAPSHOT"

  :description "Middleware to run selected files through an XSLT stylesheet."

  ;; Saxon is used via `clojure-saxon` and will need to be installed manually
  ;; to a local repository because public distribution of saxon via Maven is
  ;; [not authorized](http://sourceforge.net/news/?group_id=29872). Leiningen
  ;; and Maven will display instructions on how to do this the first time
  ;; dependency resolution fails.

  :dependencies
  [[org.clojure/clojure "1.4.0"]
   [org.clojars.jedahu/clojure-saxon "0.9.3"]
   [ring/ring-jetty-adapter "1.0.2"]]

  :story
  {:output "doc/index.html"})

;;%include README.md
;;%include src/ring/middleware/xslt.clj
