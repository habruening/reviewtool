(ns reviewtool.httpservice
  (:require [ring.adapter.jetty :as jty]
            [ring.middleware.params]
            [ring.middleware.resource]
            [ring.middleware.content-type]
            [ring.util.response] 
            [reviewtool.index]
            #_[reviewtool.pdfviewer]
            #_[reviewtool.annotationservice] 
            [reviewtool.managereviews]
            [reviewtool.reviews]
            [compojure.core :refer :all]
            [lambdaisland.uri :as liuri]))

(defroutes app 
  (GET "/index.html" [] (reviewtool.index/html))
  (GET "/review.html" [folder realname]
    (reviewtool.pdfviewer/html realname
                               folder
                               #(liuri/assoc-query "/export_annotations.html" {:file %})
                               (liuri/assoc-query "/manage_reviews.html" {:realname realname})))
  (GET "/review_status_changed" [folder user status] (reviewtool.reviews/update-review-status folder user status))
  (POST "/annotation_changed.html" req
    (reviewtool.annotationservice/update-annotation (get-in req [:query-params "file"])
                                                    (req :body)))
  (GET "/get_annotations.xfdf" [file] (reviewtool.annotationservice/get-annotations file))
  (GET "/export_annotations.html" [file] (reviewtool.annotationservice/export-annotations file))
  (GET "/manage_reviews.html" req
    (reviewtool.managereviews/html (reviewtool.reviews/list-reviews)
                                   #(str (liuri/assoc-query "/review.html" (assoc (req :params) :folder %)))))
  (GET "/reviews/*" req (reviewtool.reviews/html (clojure.string/replace (req :uri) "/reviews/" ""))))


(defn start-server [port]
  (println "starting server")
  (jty/run-jetty (-> app
                     (ring.middleware.params/wrap-params)
                     (ring.middleware.resource/wrap-resource "static_web_resources")
                     (ring.middleware.content-type/wrap-content-type))
                 {:port port
                  :join? false}))

(defn stop-server [server]
  (println "stopping server")
  (.stop server))

