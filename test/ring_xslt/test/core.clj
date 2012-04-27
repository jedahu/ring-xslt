(ns ring-xslt.test.core
  (:use
    ring.middleware.xslt
    clojure.test))

(deftest static-wrap-plain-page
  (let [handler (wrap-xslt
                  (constantly nil)
                  :static-wrap
                  "resources"
                  :from :file)
        resp (handler {:request-method :get
                       :uri "/footer.html"})]
    (is (map? resp))
    (is (= "text/html" (get-in resp [:headers "content-type"])))
    (is (seq (re-seq #"Site designed by the Queen of Hearts"
                     (:body resp))))))

(deftest static-wrap-page-with-includes
  (let [handler (wrap-xslt
                  (constantly nil)
                  :static-wrap
                  nil
                  :from :resource)
        resp (handler {:request-method :get
                       :uri "/jabberwock.html"})]
    (is (map? resp))
    (is (= "text/html" (get-in resp [:headers "content-type"])))
    (is (seq (re-seq #"'Twas brillig and the slithy toves..."
                     (:body resp))))
    (is (seq (re-seq #"Site designed by the Queen of Hearts"
                     (:body resp))))
    (is (seq (re-seq #"Jabberwock" (:body resp))))
    (is (seq (re-seq #"<article>" (:body resp))))))

;;. vim: set lispwords+=deftest:
