(ns reviewtool.index
  (:require [hiccup.page :as p]))

; TODO: The session (credentials) cannot be in the uri, because then uris could not be shared.

(defn html []
  (p/html5
   [:head
    [:title "Review Tool Login"]
    [:style "body {
               font-family: Arial, sans-serif;
             }
             button {
               background-color: #06AA6D;
               color: white;
               padding: 14px 20px;
               margin: 8px 0;
               border: none;
               cursor: pointer;
               width: 30%;
               border-radius: 10px;
             }
             button:hover {
               opacity: 0.8;
             }"]] 
   [:body
    [:div {:style {:width "50%"
                   :background-color "#FEF5E7"
                   :border "4px solid green"
                   :margin "auto"
                   :border-radius "20px"
                   :margin-top "20px"
                   :padding "15px"
                   :padding-top 0
                   :vertical-align "bottom"}}
     [:p {:align "right" :style {:color "grey"}} "experimental"]
     [:h1 [:center "FJT Review Tool"]]
     [:h2 [:center "Login"]]
     [:center [:form {:action "/submitted"}
               [:p [:label {:style {:color "#CCCCCC"} :for "login"} "User"]]
               [:p [:input {:style {:border 0 :color "#9C9C9C" :background-color "#ECECEC"} :id "login" :name "login"}]]
               [:p [:label {:for "pass" :style {:color "#CCCCCC"}} "Password"]]
               [:p [:input {:style {:border 0 :color "#9C9C9C" :background-color "#ECECEC"} :id "pass" :name "pass" :type "password"}]]
               #_[:p [:button {:style {:border 0 :color "#9C9C9C" :background-color "#ECECEC"}} "Login"]]]]
     #_[:hr]
     [:div {:style {:margin "50px"}}
      [:p "This version of the Review Tool is a proof of concept only. It does not yet have a full user"
       " manamgement with credentials and a session management. It will have to be connected to the"
       " company Single Sign On, but this is not yet implemented."]
      [:p " For this demo please provide your real name instead."]]
     [:center [:form {:action "/manage_reviews.html"}
               [:p [:label {:for "uname"} [:b "Real Name"]]]
               [:p [:input {:id "text" :name "realname"}]]
               [:p [:button "Login"]]]]]]))