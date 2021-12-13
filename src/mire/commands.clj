(ns mire.commands
  (:require [clojure.string :as str]
            [mire.rooms :as rooms]
            [mire.player :as player]
            [mire.enemies :as enemies]))

(defn- move-between-refs
  "Move one instance of obj between from and to. Must call in a transaction."
  [obj from to]
  (alter from disj obj)
  (alter to conj obj))

;; Command functions

(defn look
  "Get a description of the surrounding environs and its contents."
  []
  (str (:desc @player/*current-room*)
       "\nExits: " (keys @(:exits @player/*current-room*)) "\n"
       (str/join "\n" (map #(str "There is " % " here.\n")
                           @(:items @player/*current-room*)))))

(defn move
  "\"♬ We gotta get out of this place... ♪\" Give a direction."
  [direction]
  (dosync
   (let [target-name ((:exits @player/*current-room*) (keyword direction))
         target (@rooms/rooms target-name)]
     (if target
       (do
         (move-between-refs player/*name*
                            (:inhabitants @player/*current-room*)
                            (:inhabitants target))
         (ref-set player/*current-room* target)
         (look))
       "You can't go that way."))))

(defn add_group
  [name]
  (dosync
   (if (contains? @player/groups name)
    (str "Group " name " already exists.")
    (do
    (commute player/groups assoc name (ref #{}) )
    (str "Group " name " was created.")))))

(defn join_group
  [name] 
  (dosync
     (if (contains? @player/groups name)
     (
       do 
      (if (not (empty? @player/*group*)) 
        (commute (get @player/groups @player/*group*) disj player/*name*)) 
      (ref-set player/*group* name)
      (commute (get @player/groups name) conj player/*name*)
      (str "You joined into the group " name  ".\n"
      "Group members: " (str/join ", " @(get @player/groups name)) ))
      (str "Group " name " doesn't exists, type 'add_group groupname' to create new"))))

(defn leave_group
  [] 
  (dosync
     (do 
      (commute (get @player/groups @player/*group*) disj player/*name*)
      (ref-set player/*group* "")
      (str "You leaved the group"))))

(defn say_group
  "Say something out loud so everyone in the room can hear."
  [& words]
  (if (empty? @player/*group*)
  (str "You are not in group yet")
  (let [message (str/join " " words)]
    (doseq [inhabitant (disj @(get @player/groups @player/*group*)
                             player/*name*)]
      (binding [*out* (player/streams inhabitant)]
        (println message)
        (println player/prompt)))
    (str "You said to group: " message))))

(defn list_groups
  []
  (str/join ", " (keys @player/groups)))

(defn current_group [] (str @player/*group*))

(defn show-enemies
  "Get a description of enemies at current location."
  []
  (if (= 0 (count @(:enemies @player/*current-room*))) "There is no enemies here."
  (str
    "Enemies at this location: \n"
       (str/join "\n" (map #(
                             (fn []
                               (def entry (get @enemies/enemies %))
                               (str (:name entry) " (HP: " (:baseHp entry) ", DMG: " (:baseDamage entry) ")\n")))
                           @(:enemies @player/*current-room*))))))


(defn grab
  "Pick something up."
  [thing]
  (dosync
   (if (rooms/room-contains? @player/*current-room* thing)
     (do (move-between-refs (keyword thing)
                            (:items @player/*current-room*)
                            player/*inventory*)
         (str "You picked up the " thing "."))
     (str "There isn't any " thing " here."))))

(defn discard
  "Put something down that you're carrying."
  [thing]
  (dosync
   (if (player/carrying? thing)
     (do (move-between-refs (keyword thing)
                            player/*inventory*
                            (:items @player/*current-room*))
         (str "You dropped the " thing "."))
     (str "You're not carrying a " thing "."))))

(defn inventory
  "See what you've got."
  []
  (str "You are carrying:\n"
       (str/join "\n" (seq @player/*inventory*))))

(defn detect
  "If you have the detector, you can see which room an item is in."
  [item]
  (if (@player/*inventory* :detector)
    (if-let [room (first (filter #((:items %) (keyword item))
                                 (vals @rooms/rooms)))]
      (str item " is in " (:name room))
      (str item " is not in any room."))
    "You need to be carrying the detector for that."))

(defn say
  "Say something out loud so everyone in the room can hear."
  [& words]
  (let [message (str/join " " words)]
    (doseq [inhabitant (disj @(:inhabitants @player/*current-room*)
                             player/*name*)]
      (binding [*out* (player/streams inhabitant)]
        (println message)
        (println player/prompt)))
    (str "You said " message)))

(defn help
  "Show available commands and what they do."
  []
  (str/join "\n" (map #(str (key %) ": " (:doc (meta (val %))))
                      (dissoc (ns-publics 'mire.commands)
                              'execute 'commands))))

;; Command data

(def commands {"move" move,
               "north" (fn [] (move :north)),
               "south" (fn [] (move :south)),
               "east" (fn [] (move :east)),
               "west" (fn [] (move :west)),
               "up" (fn [] (move :up)),
               "down" (fn [] (move :down)),
               "grab" grab
               "discard" discard
               "inventory" inventory
               "detect" detect
               "look" look
               "say" say
               "help" help
               "add_group" add_group
               "join_group" join_group
               "current_group" current_group
               "say_group" say_group,
               "list_groups" list_groups,
               "leave_group" leave_group,
               "show_enemies" show-enemies
               })

;; Command handling

(defn execute
  "Execute a command that is passed to us."
  [input]
  (try (let [[command & args] (.split input " +")]
         (apply (commands command) args))
       (catch Exception e
         (.printStackTrace e (new java.io.PrintWriter *err*))
         "You can't do that!")))
