(ns reviewtool.core
  (:require [reviewtool.httpservice :as ws]) 
  (:gen-class))

(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (ws/start-server 5000))


(comment
  (def server (ws/start-server 5000))

  (ws/stop-server server)

  (do (ws/stop-server serEver)
      (def server (ws/start-server 5000))))

