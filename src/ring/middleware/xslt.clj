;; # Middleware
(ns ring.middleware.xslt
  (:gen-class)
  (:use
    [saxon :only (compile-xslt compile-xml serialize-to-string)]
    [clojure.string :only (join)]
    [clojure.java.io :only (file resource input-stream as-file)]
    [ring.middleware.file :only (wrap-file)]
    [ring.middleware.resource :only (wrap-resource)]
    [ring.adapter.jetty :only (run-jetty)])
  (:import
    [java.net URLDecoder]))

;; ## Bundled stylesheet
;;
;; This library comes with one XSLT stylesheet, here called `static-wrap`. Its
;; use is described in the [[Synopsis]] (found also in `README.md`).
(defn static-wrap
  []
  (slurp (resource "static-wrap.xsl")))

;; ## Output formats
;;
;; Stylesheet output can be either HTML or XML. Any file or resource with a
;; `.html` suffix is output as HTML; all others are output as XML.

(def output-html
  {:method "html"
   :doctype-public "about:legacy-compat"
   :omit-xml-declaration "yes"
   :indent "no"
   :encoding "UTF-8"})

(def output-xml
  {:method "xml"
   :indent "no"
   :encoding "UTF-8"})

;; ## Input locations
;;
;; Input is read from files or from resources. The keyword option `:from`
;; decides this: its value can be `:file` or `:resource`.

(defn- file-input
  [root path index]
  (let [f1 (file root path)
        f2 (if (and index (.isDirectory f1))
             (file root path index)
             f1)]
    (and (.isFile f2) {:path (.getPath f2) :data f2})))

(defn- resource-input
  [root path index]
  (let [p1 (-> (str (or root "") "/" path)
             (.replace "//" "/")
             (.replaceAll "^/" ""))]
    (if-let [r1 (resource p1)]
      (if (= "file" (.getProtocol r1))
        (file-input (as-file r1) "" index)
        {:path p1 :data (input-stream r1)})
      (when-let [p2 (and index (str (.replaceAll p1 "/$" "") index))]
        (let [r2 (resource p2)]
          (and r2 {:path p2 :data (input-stream r2)}))))))

;; ## Middleware function
;;
;; The middleware function is `wrap-xslt`. In addition to a handler function it
;; takes the following arguments:
;;
;; stylesheet
;; :  (Required) An XSLT stylesheet in the form of a File, URL, InputStream, Reader,
;;    String, XdmNode, or anything else that `saxon/compile-xslt` can handle.
;;    The special value `:static-wrap` means the bundled [[static-wrap]]
;;    stylesheet will be used.
;;
;; root
;; :  (Optional, String) A path to be prepended to the request `:uri` before
;;    file or resource resolution. Default: `""`.
;;
;; and the following key-value options (all optional):
;;
;; :from
;; :  (Keyword) `:file` or `:resource`. Where to read input from. All values
;;    other than `:file` are treated as if they were `:resource`. Default:
;;    `:resource`.
;;
;; :re-process
;; :  (Pattern) A regular expression denoting which paths are to be processed
;;    using the stylesheet. By default, all paths will be.
;;
;; :re-block
;; :  (Pattern) A regular expression denoting which paths are to be blocked
;;    from processing and from being passed to the handler. By default, all
;;    paths not processed will be passed to the handler.
;;
;; :index
;; :  (String) A file name to append to directory paths. Default: `nil`.
;;
;; :cache?
;; :  (Boolean) Whether to cache XSLT output per path. The cache is useful only
;;    for static transformations (it is never invalidated). Default: no cache.
;;
;; :html-mime
;; :  (String) The `Content-Type` header to set for `.html` files. Default:
;;    `"text/html"`.
(defn wrap-xslt
  [handler stylesheet root & opts]
  (pr stylesheet root)
  (let [{:keys [re-process re-block] :as opts} (apply hash-map opts)
        ss (if (= :static-wrap stylesheet) (static-wrap) stylesheet)
        cache (atom {})
        xslt (compile-xslt ss)]
    (fn [req]
      (let [path (.substring ^String
                             (URLDecoder/decode (:uri req) "UTF-8")
                             1)]
        (cond
          (not= :get (:request-method req))
          (handler req)

          (and re-block (re-find re-block path))
          {:status 404 :body "wrap-xslt: 404 not found"}

          (or (not re-process)
              (re-find re-process path))
          (if-let [input ((if (= :file (:from opts)) file-input resource-input)
                            (or root "") path (:index opts))]
            (let [html? (re-find #"\.html$" (:path input))
                  output (if html? output-html output-xml)
                  body (or (@cache path)
                           (serialize-to-string
                             (xslt (compile-xml (:data input)))
                             output))]
              (when (:cache? opts)
                (swap! cache assoc path body))
              (let [resp {:status 200 :body body}]
                (if-let [hmime (and html? (or (:html-mime opts) "text/html"))]
                  (assoc resp :headers {"content-type" hmime})
                  resp)))
            (handler req))

          :else
          (handler req))))))

;; ## Convenience and commandline
;;
;; A 'pre-packaged' `run-server` function wraps [[wrap-xslt]] around either
;; `wrap-file` or `wrap-resource` depending on the `:from` option, and serves
;; using the ring jetty adapter. It takes the same arguments as `wrap-xslt` plus
;; a `:port` key which is passed on to jetty (default: 8081).
(defn run-server
  [stylesheet root & opts]
  (run-jetty
    (apply wrap-xslt
           (if (= :file (:from (apply hash-map opts)))
             (wrap-file
               (constantly
                 {:status 404 :body "wrap-file: 404 not found"})
               (or root "")
               {:index-files? true})
             (wrap-resource
               (constantly
                 {:status 404 :body "wrap-resource: 404 not found"})
               (or root "")))
           stylesheet
           root
           opts)
    {:port (or (:port opts) 8081)}))

;; The main function wraps `run-server`. It takes the same arguments as
;; `run-server` except keys are prefixed with `-` instead of `:`, and `-cache`
;; has no question mark and takes the value `yes` or `no`.
;;
;; Example invocations:
;;
;; ~~~~
;; java -jar ring-xslt-X.X.X-standalone.jar clojure.main \
;;   -m ring.middleware.xslt :static-wrap "" \
;;   -re-process '\.html$' \
;;   -re-block '\.xml$'
;;
;; lein run -m ring.middleware.xslt path/to/foo.xsl public \
;;   -from file \
;;   -cache yes \
;;   -html-mime application/xml+xhtml
;; ~~~~
(defn -main
  [stylesheet root & args]
  (let [{:strs [-from -re-process -re-block -index -cache -html-mime -port]}
        (apply hash-map args)

        opts [:from (and -from (keyword -from))
              :re-process (and -re-process (re-pattern -re-process))
              :re-block (and -re-block (re-pattern -re-block))
              :index -index
              :cache? (and -cache (not= "no" -cache))
              :html-mime -html-mime
              :port (and -port (Integer/parseInt -port))]]
    (apply run-server
           (if (= \: (first stylesheet))
             (keyword (.substring stylesheet 1))
             (file stylesheet))
           root
           opts)))
