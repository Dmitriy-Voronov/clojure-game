(ns mire.server
  (:require [clojure.java.io :as io]
            [server.socket :as socket]
            [mire.player :as player]
            [mire.commands :as commands]
            [mire.rooms :as rooms]
            [mire.enemies :as enemies]
            [clojure.string :as str]))

(defn replace-data [message from to] (str/replace message #(str from) to))

(defn say-global
  "Say something out loud so everyone in the room can hear."
  [prefix & words]
  (let [message (str/join " " words)]
    (doseq [[room-name room-data] @rooms/rooms]
      (doseq [inhabitant @(:inhabitants room-data)]
        (binding [*out* (player/streams inhabitant)] (println prefix message) (print player/prompt))
        ))
  ))

(defn start-game [] (println "Game started"))
(defn end-game [] (println "Game finished")
  (say-global "[SERVER]" "TIME IS UP")
  (say-global "[SERVER]" (str @player/points))
  (System/exit 0))

(defn start-timer [seconds on-start on-end] (future (on-start) (Thread/sleep (* seconds 1000)) (on-end)))

(defn- cleanup []
  "Drop all inventory and remove player from room and player list."
  (dosync
   (doseq [item @player/*inventory*]
     (commands/discard item))
   (commute player/streams dissoc player/*name*)
   (commute (:inhabitants @player/*current-room*)
            disj player/*name*)))

(defn- get-unique-player-name [name]
  (if (@player/streams name)
    (do (print "That name is in use; try again: ")
        (flush)
        (recur (read-line)))
    name))

(defn- mire-handle-client [in out]
  (binding [*in* (io/reader in)
            *out* (io/writer out)
            *err* (io/writer System/err)]

    ;; We have to nest this in another binding call instead of using
    ;; the one above so *in* and *out* will be bound to the socket
    (print "\nWhat is your name? ") (flush)
    (binding [player/*name* (get-unique-player-name (read-line))
              player/*current-room* (ref (@rooms/rooms :station))
              player/*inventory* (ref #{})
              player/*group* (ref "")
              ]
      (dosync
       (commute (:inhabitants @player/*current-room*) conj player/*name*)
       (commute player/streams assoc player/*name* *out*)
       (commute player/points assoc (keyword player/*name*) 0))

      (println (commands/look)) (print player/prompt) (flush)

      (try (loop [input (read-line)]
             (when input
               (println (commands/execute input))
               (.flush *err*)
               (print player/prompt) (flush)
               (recur (read-line))))
           (finally (cleanup))))))

(defn -main
  ([port dir enemies-dir]
     (rooms/add-rooms dir)
     (enemies/add-enemies enemies-dir)
     (defonce server (socket/create-server (Integer. port) mire-handle-client))
     (println "Launching Mire server on port" port)
     (start-timer 5 start-game end-game)
   )
  ([port] (-main port "resources/rooms" "resources/enemies/"))
  ([] (-main 3333)))
