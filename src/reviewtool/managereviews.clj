(ns reviewtool.managereviews
  (:require [hiccup.page :as p]
            [htmlkit.html :as hk] 
            [reviewtool.reviews]
            [lambdaisland.uri :as liuri]))

(def red "#FADBD8")
(def orange "#FDEBD0")
(def grey "#D5D8DC")
(def green "#ABEBC6")
(def white "white")

(defn status->color [role status]
  (cond
    (and (= role :mandatory) (= status :outstanding)) [red red]
    (and (= role :mandatory) (= status :started)) [red orange]
    (and (= role :mandatory) (= status :nocomments)) [red red]
    (and (= role :mandatory) (= status :completed)) [green green]
    (and (= role :optional) (= status :outstanding)) [grey orange]
    (and (= role :optional) (= status :started)) [grey orange]
    (and (= role :optional) (= status :nocomments)) [grey green]
    (and (= role :optional) (= status :completed)) [grey green]
    :else [white white]))

(defn html-participant-status [name role status]
  (let [[role-color status-color] (status->color role status)]
       [:tr
        [:td {:style {:background-color role-color}} name]
        [:td {:style {:background-color role-color}} role]
        [:td {:style {:background-color status-color}} status]]))

(hk/let-events [show-config-button hide-config-button show-config-panel hide-config-panel]
               [:td (hk/reactive-toggle
                     ['node.style.visibility [["hidden" [hide-config-button show-config-panel]]
                                              ["visible" [show-config-button]]]]
                     (hk/add-events
                      [:button #_{:style {:visibility "hidden"}} "..."]
                      {:onClick show-config-panel}))])


(defn html-list-of-running-reviews [folder title reviewers make-review-url] 
  (hk/let-events [show-config-button hide-config-button show-config-panel hide-config-panel]
                 (hk/add-events
                  [:table
                   [:tr
                    [:td [:a {:href (make-review-url folder)}
                          title]]
                    [:td (hk/reactive-toggle
                          ['node.style.visibility [["hidden" [hide-config-button show-config-panel]]
                                                   ["visible" [show-config-button]]]]
                          (hk/add-events
                           [:button {:style {:visibility "hidden"}} "..."]
                           {:onClick show-config-panel}))]]
                   [:tr [:td {:align "right"}
                         (hk/reactive-toggle
                          ['node.style.display [["initial" [show-config-panel]]
                                                ["none" [hide-config-panel]]]]
                          [:p  {:style {:display "none"}}
                           [:button {:onClick "alert('This is not yet implemented')"} "change description"] " "
                           [:button {:onClick "alert('This is not yet implemented')"} "invite participants"]])]]
                   (hk/reactive-toggle
                    ['node.style.display [["initial" [show-config-button]]
                                          ["none" [hide-config-button]]]]
                    [:tr {:style {:display "none"}}
                     [:td
                      [:table (map (fn [[name [role status]]]  (html-participant-status name role status)) reviewers)]]])]
                  {:onMouseLeave [hide-config-button hide-config-panel]
                   :onMouseEnter show-config-button})))

(defn html [reviews make-review-url] 
  (p/html5
   [:head [:title "FJT Shared Reviews"]]
   [:body
    (hk/load-event-handling)
    [:h1 "FJT Shared Reviews"]
    [:h2 "Active Reviews"]
    (map (fn [[folder config]]
           (html-list-of-running-reviews folder (config :title) (config :reviewers) make-review-url))
         reviews) 
    [:button {:onClick "alert('This is not yet implemented')"} "new..."]]))