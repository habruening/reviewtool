(ns reviewtool.pdfviewer
  (:require [hiccup.page :as hp]
            [hiccup2.core :as h]
            [htmlkit.html :as hk]
            [htmlkit.js :refer [js jsq q]]
            [reviewtool.base.urilib :as urilib]
            [lambdaisland.uri :as liuri]))

(defn start-web-viewer [license-key file realname]
  (jsq
   (.then (WebViewer {path "WebViewer/lib"
                      licenceKey (uq license-key)
                      initialDoc (uq file)}
                     (document.getElementById "viewer"))
          (fn [instance]
            (set! annotation_manager instance.Core.annotationManager)
            (.setCurrentUser annotation_manager (uq realname))
            (.setDocumentXFDFRetriever instance.Core.documentViewer
             (async (fn []
                      (set! response (await (fetch (uq (str "get_annotations.xfdf?file=" file)))))
                      (set! xfdf (await (response.text)))
                      (console.log xfdf)
                      (return xfdf))))
            (.addEventListener instance.Core.annotationManager "annotationChanged"
                                                              (async (fn [e]
                                                                       (await (fetch (uq (str "annotation_changed.html?file=" file))
                                                                                     {method "POST"
                                                                                      headers {"Content-Type" "application/xml"}
                                                                                      body (await (instance.Core.annotationManager.exportAnnotationCommand))}))
                                                                       (console.log (instance.Core.annotationManager.exportAnnotations)))))
            (.disableElements instance.UI ["toolbarGroup-Shapes"])
            (.setHeaderItems instance.UI (fn [header]
                                          (set! item (header.getItems))
                                          (set! item (item.slice 2 -1))   ; todo: Better not use the indices here
                                          (header.update item)))))))

(defn status->color [status] 
  (if status
    (case status
      :outstanding "#FADBD8"
      :started "#FDEBD0"
      :nocomments "#D5D8DC"
      :completed "#ABEBC6"
      "white")
    "white"))

(def js-status->color
  (q (fn [status]
         (if (= status "outstanding") (return "#FADBD8"))
         (if (= status "started") (return "#FDEBD0"))
         (if (= status "nocomments") (return "#D5D8DC"))
         (if (= status "completed") (return "#ABEBC6"))
         (return "white"))))

(defn header-line [events status manage-reviews-url config export-url realname]
  [:table [:tr
           [:td [:button  {:onClick (str "location.href='" manage-reviews-url "'")} "fjt"]
            [:span {:style {:visibility "hidden"}} "EMPTY SPACE"]]
           (hk/reactive-toggle
            ['node.style.visibility [["hidden" [(events :about-window-hide) :init]]
                                     ["visible" [(events :about-window-show)]]]]
            [:td {:style {:visibility "hidden"}} [:button {:onclick (hk/fire (events :about-window-hide))} "close"]])
           (hk/reactive-toggle
            ['node.style.visibility [["hidden" [(events :about-window-show)] (if (not (config :about-the-document)) :init)]
                                     ["visible" [(events :about-window-hide)]]]]
            [:td [:button {:onclick (hk/fire (events :about-window-show))} "About this document"]
             [:span {:style {:visibility "hidden"}} "EMPT"]])
           [:td [:button {:onClick "alert('This is not yet implemented')"} "Export for Archive"]]
           [:td [:button {:onClick (str "location.href='" export-url "'")} "Manage Annotations"]]
           [:td [:span {:style {:visibility "hidden"}} "EMPTY SPACE"]]
           [:td [:label "Reviewer Name:"]]
           [:td [:input {:type "text" :value realname :readonly true}]]
           [:td [:label "Status:"]]
           [:td [:select {:onchange (hk/fire (events :review-status-changed) (q this.value))}
                 [:option {:value "outstanding" :selected (= status :outstanding)} "Review Outstanding"]
                 [:option {:value "started" :selected (= status :started)} "Review Started"]
                 [:option {:value "nocomments" :selected (= status :nocomments)} "No Comments"]
                 [:option {:value "completed" :selected (= status :completed)} "Review Completed"]]]]])

(defn about-panel [folder config]
  [:div {:style {:height "95vh" :resize "horizontal" :float "left" :width "15%" :display "none"}}
   [:iframe {:style {:border 0 :height "90vh" :width "100%"} :src (str "/reviews/" folder (config :about-the-document))}]])

(defn viewer []
  [:div#viewer
   {:style {:height "95vh" :margin "0 auto" :background-color "white"}}])

(defn html [realname folder make-export-annotation-url manage-reviews-url] 
  (let [config ((reviewtool.reviews/list-reviews) folder)
        status (get-in config [:reviewers realname 1])
        hiccup-vector
        (list [:head
               [:title "Basic WebViewer"]
               [:link {:rel "stylesheet" :href "css/index.css"}]
               [:meta
                {:name "viewport"
                 :content "width=device-width, initial-scale=1.0, minimum-scale=1.0, maximum-scale=1.0, user-scalable=no"}]
               [:script {:src "WebViewer/lib/webviewer.min.js"}]]
              [:body {:style {:marPgin "0.6em" :display "flex" :flex-direction "column"}}
               (hk/load-event-handling) 
               (hk/let-event-map
                [events [:review-status-changed :about-window-hide :about-window-show]] 
                (hk/create-with-event-handler<-q
                 [:div {:style {:background-color (status->color status)}}
                  (header-line events status manage-reviews-url config (make-export-annotation-url (str "/reviews/" (config :document-under-review))) realname)
                  [:div {:style {:background-color "white"}}
                   (hk/reactive-toggle
                    ['node.style.display [["initial" [(events :about-window-show)]]
                                          ["none" [(events :about-window-hide)] :init]]]
                    (about-panel folder config))
                   (viewer)]]
                 ((events :review-status-changed) [node status]
                                                  (do (fetch (.replace
                                                              (uq (str (liuri/assoc-query
                                                                        "/review_status_changed"
                                                                        {:folder folder :user realname :status "new-status-to-be-replaced"})))
                                                              "new-status-to-be-replaced" status))
                                                      (set! node.style.backgroundColor ((uq js-status->color) status))))))
               [:script (start-web-viewer
                         "demo:1711628820807:7f0b31a90300000000a2462ac32c70d19d86e7de509af061bcc416117b"
                         (str "/reviews/" folder (config :document-under-review))
                         realname)]])] 
    (hp/html5 hiccup-vector)))
