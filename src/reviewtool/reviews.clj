(ns reviewtool.reviews
  (:require [clojure.java.io :as io]
            [hiccup.page :as p]))

;;;; The code in this file works, but is not very nice.

(def ^:dynamic review-folder "/home/hartmut/reviewtool/test/example")
(def ^:dynamic review-folder-path (clojure.java.io/file  "/home/hartmut/reviewtool/test/example"))

(defn folder-index [folder]
  (p/html5
   [:head [:title "FJT Shared Reviews"]]
   [:body
    [:h1 "Contents of " folder]
    [:p {:style {:margin 0}} [:a {:href (str
                                         (clojure.string/join "/"
                                                              (drop-last (clojure.string/split folder #"/")))
                                         "/")} ".."]]
    (let [path (clojure.string/replace folder "/reviews" review-folder)]
      (map #(vector :p {:style {:margin 0}}
                    [:a {:href (str folder % (if (.isDirectory (clojure.java.io/file (str path %))) "/"))} %])
           (-> path clojure.java.io/file .list)))]))

(defn html [path]
  (if (clojure.string/ends-with? path "/")
    (folder-index path)
    (io/input-stream (clojure.java.io/file review-folder-path path))))

(defn list-reviews []
  (->>  review-folder-path .listFiles
        (map #(clojure.java.io/file % "config.edn"))
        (filter #(.exists %))
        (map #(vector (->> % .getParentFile .toURI (.relativize (.toURI review-folder-path)) str)
                      (-> % io/reader java.io.PushbackReader. clojure.edn/read)))
        (into (sorted-map))))

(defn update-review-status [folder user status-update] 
  ;; Todo: Lock!
  (let [config ((list-reviews) folder)
        reviewers (config :reviewers)
        status (reviewers user)
        new-status [(if status (first status) :uninvolved)
                    (keyword status-update)]
        new-config (update-in config [:reviewers user] (fn [x] new-status))]
    (with-open [wr (clojure.java.io/writer (clojure.java.io/file review-folder-path folder "config.edn"))] 
      (.write wr (with-out-str (clojure.pprint/pprint new-config)))))
  "done")

(comment
  (update-review-status "review_001/" "Hartmut Brüning" :coddmpleted))

(comment 
  (list-reviews)
  (html "review_001/howto.pdf"))