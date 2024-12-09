(ns reviewtool.annotationservice
  (:require [ring.util.response]
            [clojure.data.xml :as xml]
            [clojure.zip :as zip]
            [clojure.data.zip.xml :as xmlzipper]
            [hiccup.page :as h]
            [htmlkit.html :as html]
            [clojure.walk]))

(def annotations (atom {}))

(defn get-added-or-modified-or-removed-annotations [xfdf-mod kind]
  (-> xfdf-mod zip/xml-zip
      (xmlzipper/xml-> :xfdf kind zip/down zip/node)))

(defn get-annotation-name [annotation]
  (-> annotation :attrs :name))

(defn add-annotations [annotations xfdf-mod kind]
  (-> (get-added-or-modified-or-removed-annotations xfdf-mod kind)
      (concat annotations)))

(defn remove-modiefied-annotations [annotations xfdf-mod]
  (as-> xfdf-mod i
    (get-added-or-modified-or-removed-annotations i :modify)
    (map get-annotation-name i)
    (remove #(some #{(get-annotation-name %)} i) annotations)))

(defn remove-deleted-annotations [annotations xfdf-mod]
  (loop [names-to-be-removed (as-> xfdf-mod i
                               (zip/xml-zip i)
                               (xmlzipper/xml-> i :xfdf :delete :id)
                               (map zip/node i)
                               (map :content i)
                               (apply concat i))
         remaining-annotations annotations]
    (if (empty? names-to-be-removed) remaining-annotations
        (recur (as-> annotations i
                 (filter #(some #{(-> % :attrs :inreplyto)} names-to-be-removed) i)
                 (map #(-> % :attrs :name) i))
               (remove #(some #{(get-annotation-name %)} names-to-be-removed) remaining-annotations)))))

(defn annotations-as-xml [annotations]
  (xml/element :xfdf {:xml:space "preserve" :xmlns "http://ns.adobe.com/xfdf"}
               (xml/element :pdf-info {:xmlns "http://www.pdftron.com/pdfinfo" :version 2 :import-version 4}
                            (xml/element :fields {}
                                         (xml/element :field {:name "ACombo"} "sdf"
                                                      (xml/element :value {} "Red")))
                            (xml/element :annots {} annotations))))


; Web Interface

(defn update-annotation [file xfdf]
  (let [xml (xml/parse xfdf)
        update #(assoc % file (-> file %
                                  (remove-modiefied-annotations xml)
                                  (remove-deleted-annotations xml)
                                  (add-annotations xml :add)
                                  (add-annotations xml :modify)))]
    (swap! annotations update)))

(defn get-annotations [file-name]
  (->> file-name ((deref annotations)) annotations-as-xml xml/emit-str))

; Annotations Page

(defn annotation->data [annotation]
  {:id (-> annotation :attrs :name)
   :page (inc (read-string (-> annotation :attrs :page)))
   :author (-> annotation :attrs :title)
   :type (-> annotation :attrs :subject)
   :state (-> annotation :attrs :state)
   :comment (as-> annotation i
              (zip/xml-zip i)
              (xmlzipper/xml-> i :text :contents)
              (map zip/node i)
              (map :content i)
              (clojure.string/replace (clojure.string/join "[:br]" (apply concat i)) "\n" "<br>"))})


(defn as-html-table-rows
  ([related-annotations]
   (as-html-table-rows related-annotations nil))
  ([related-annotations inreplyto]
   (->> inreplyto related-annotations
        (map annotation->data)
        (map #(list
               [:tr
                (html/let-events
                 [hover unhover]
                 (html/add-events
                  [:td (html/reactive-toggle
                        ['node.style.display [["none" [unhover]]
                                              ["inline" [hover]]]]
                        [:input {:type "checkbox" :style {:display :none}}])]
                  {:onMouseEnter hover :onMouseLeave unhover}))
                [:td (if (not inreplyto) (% :page))]
                [:td (% :author)]
                [:td (if (not inreplyto) (% :type) [:center "|reply note|"])]
                [:td (if (% :state) (list "|" (% :state) "|"))]
                [:td (% :comment)]]
               (as-html-table-rows related-annotations (% :id)))))))

(defn get-related-annotations [file-name]
  (let [comments
        (reduce (fn [comments-and-replies comment]
                  (let [inreplyto (-> comment :attrs :inreplyto)]
                    (assoc comments-and-replies
                           inreplyto
                           (into [] (conj (comments-and-replies inreplyto) comment)))))
                {}
                (@annotations file-name))]
    (assoc comments nil (sort-by #(-> % :attrs :page read-string) (comments nil)))))

(defn button [visibility label onclick visible-if hidden-if]
  (html/reactive-toggle
   ['node.style.display [["none" hidden-if]
                         ["inline" visible-if]]]
   (html/add-events [:button {:border 1 :style {:display visibility}} label]
                    {:onClick onclick})))

(defn clause [label visible-if hidden-if]
  (html/reactive-toggle
   ['node.style.display [["none" hidden-if]
                         ["inline" visible-if]]]
   [:span {:style {:display "none"}} label]))

(defn export-annotations-to-hiccup-vector [file-name]
  (list
   [:head
    [:title "Annotations"]]
   [:body
    (html/load-event-handling)
    (html/let-events
     [cancel
      filter filter-all filter-not-answered filter-by-author filter-by-date filter-by-update filter-by-status
      author date update status action unimplemented color import-action export-action
      import export]
     (button "inline" "filter" filter [cancel] [filter import export])
     (clause "Filter" [filter] [cancel])
     (button "inline" "import" import [cancel] [filter import export])
     (clause "Import from " [import] [cancel])
     (button "inline" "export" export [cancel] [filter import export])
     (clause "Export as " [export] [cancel])
     (clause " ... " [filter import export] [cancel filter-all filter-not-answered filter-by-author filter-by-date filter-by-update filter-by-status import-action export-action])
     (button "none" "all" filter-all [filter] [cancel filter-all filter-not-answered filter-by-author filter-by-date filter-by-update filter-by-status])
     (clause " all" [filter-all] [cancel])
     (button "none" "not answered" filter-not-answered [filter] [cancel filter-all filter-not-answered filter-by-author filter-by-date filter-by-update filter-by-status])
     (clause " not answered " [filter-not-answered] [cancel])
     (button "none" "by author" filter-by-author [filter] [cancel filter-all filter-not-answered filter-by-author filter-by-date filter-by-update filter-by-status])
     (clause " from " [filter-by-author] [cancel])
     (button "none" "by date" filter-by-date [filter] [cancel filter-all filter-not-answered filter-by-author filter-by-date filter-by-update filter-by-status])
     (clause " since " [filter-by-date] [cancel])
     (button "none" "by update date" filter-by-update [filter] [cancel filter-all filter-not-answered filter-by-author filter-by-date filter-by-update filter-by-status])
     (clause " updated since " [filter-by-update] [cancel])
     (button "none" "by status" filter-by-status [filter] [cancel filter-all filter-not-answered filter-by-author filter-by-date filter-by-update filter-by-status])
     (clause " with status " [filter-by-status] [cancel]) 
     (clause " ... " [filter-by-author filter-by-date filter-by-update filter-by-status] [cancel author date update status])
     (->> "/reviews/review_001/howto.pdf" ((deref annotations))
          (map #(-> % :attrs :title)) distinct
          (map #(html/let-events
                 [name-clicked]
                 (button "none" %1 [author name-clicked] [filter-by-author] [cancel author])
                 (clause %1 [name-clicked] [cancel]))))
     (html/let-events
      [date-changed]
      (html/reactive-toggle
       ['node.style.display [["none" [cancel date]]
                             ["inline" [filter-by-date]]]]
       [:span {:style {:display "none"}}
        (html/add-events [:input {:type "date"}]
                         {:onChange [[date-changed 'this.value]]})
        (html/add-events [:button {:border 1} "ok"]
                         {:onClick date})])

      (html/reactive-toggle
       ['node.style.display [["none" [cancel]]
                             ["inline" [date]]]]
       [:span {:style {:display "none"}}
        (html/create-with-event-handler<-q
         [:span
          "ever"]
         (date-changed [node date_str] (set! node.innerHTML (+ date_str " "))))]))
     (html/let-events
      [update-changed]
      (html/reactive-toggle
       ['node.style.display [["none" [cancel update]]
                             ["inline" [filter-by-update]]]]
       [:span {:style {:display "none"}}
        (html/add-events [:input {:type "date"}]
                         {:onChange [[update-changed 'this.value]]})
        (html/add-events [:button {:border 1} "ok"]
                         {:onClick update})])

      (html/reactive-toggle
       ['node.style.display [["none" [cancel]]
                             ["inline" [update]]]]
       [:span {:style {:display "none"}}
        (html/create-with-event-handler<-q
         [:span
          "ever"]
         (update-changed [node date_str] (set! node.innerHTML (+ date_str " "))))]))
     (map #(html/let-events
            [status-clicked]
            (button "none" %1 [status-clicked status] [filter-by-status] [cancel status])
            (clause %1 [status-clicked] [cancel])) ["accepted" "rejected" "completed"])
     (clause " and " [filter-all filter-not-answered author date update status] [cancel])
     (map #(html/let-events
            [action-clicked]
            (button "none" %1 [action-clicked action unimplemented] [filter-all filter-not-answered author date update status] [cancel action])
            (clause (str %1 ".") [action-clicked] [cancel])) ["select" "unselect" "hide" "reduce"])
     (html/let-events
      [highlight]
      (button "none" "highlight" [highlight action] [filter-all filter-not-answered author date update status] [cancel action])
      (clause "highlight " [highlight] [cancel])
     

     (html/let-events
      [paint-background]
      (html/reactive-toggle
       ['node.style.display [["none" [cancel paint-background]]
                             ["inline" [highlight]]]]
       [:span {:style {:display "none"}}
        (map #(html/add-events [:button {:style {:background-color %1}} %1]
                         {:onClick [[paint-background %1] unimplemented]}) ["LightGrey" "LightBlue" "LightGreen"])])
      (html/reactive-toggle
       ['node.style.display [["none" [cancel]]
                             ["inline" [highlight]]]]
       [:span {:style {:display "none"}}
        (html/create-with-event-handler<-q
         [:span {:style {:display "none"}} "ever"]
         (paint-background [node color] (do (set! node.style.display "inline")
                                            (set! node.style.backgroundColor color)
                                            (set! node.innerHTML (+ color "."))))
         (cancel [node] (set! node.style.display "none")))])))
     
     (map #(html/let-events
            [xxx]
            (button "none" %1 [xxx unimplemented import-action] [import] [cancel import-action])
            (clause (str %1 ".") [xxx] [cancel])) ["PDF" "CSV" "Excel" "external E-Mail contribution"])
     (map #(html/let-events
            [xxx]
            (button "none" %1 [xxx unimplemented export-action] [export] [cancel export-action])
            (clause (str %1 ".") [xxx] [cancel])) ["PDF" "CSV" "Excel" "external E-Mail contribution"])
     
     ()
" "
     (button "none" "x" cancel [filter import export] [cancel])

     (html/add-id "unimplemented" [:span ""])
     (html/register-handler unimplemented "unimplemented" '(fn [x] (alert "not yet implmented"))))
    
    [:hr]
    [:table
     [:style "table, th, td {
               border: 1px solid black;
               border-collapse: collapse;
             }"]
     [:tr [:th {:style {:visibility "hidden"}} "sel"] [:th "Page"] [:th "Author"] [:th "Type"] [:th "State"] [:th "Comment"]]
     (as-html-table-rows (get-related-annotations "/reviews/review_001/howto.pdf"))]]))

(defn export-annotations [file-name]
  (h/html5 (export-annotations-to-hiccup-vector file-name)))

(keys @annotations)

(get-annotations "/reviews/review_001/howto.pdf")