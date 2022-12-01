(ns firestone.client.kth.server
  (:require [org.httpkit.server :refer [run-server]]
            [firestone.client.kth.endpoints :refer [handler]]))

(defonce server-atom (atom nil))

(defn server-started?
  []
  (boolean (deref server-atom)))

(defn start!
  []
  (if (server-started?)
    "Server already started"
    (let [stop-server-fn (run-server (fn [request]
                                       (handler request))
                                     {:port 8001})]
      (reset! server-atom stop-server-fn))))

(defn stop!
  []
  (if-let [stop-server-fn (deref server-atom)]
    (do (stop-server-fn :timeout 100)
        (reset! server-atom nil)
        "Server stopped")
    "Server already stopped"))

(comment
  (start!)
  (stop!)
  (server-started?)
  )
