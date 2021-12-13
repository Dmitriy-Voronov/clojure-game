(ns mire.rooms)

(def rooms (ref {}))

(defn load-room [rooms file]
  (let [room (read-string (slurp (.getAbsolutePath file)))]
    (conj rooms
          {(keyword (.getName file))
           {:name (keyword (.getName file))
            :desc (:desc room)
            :exits (ref (:exits room))
            :enemies (ref (or (:enemies room) #{}))
            :items (ref (or (:items room) #{}))
            :inhabitants (ref #{})}})))

(defn load-rooms
  "Given a dir, return a map with an entry corresponding to each file
  in it. Files should be maps containing room data."
  [rooms dir]
  (dosync
   (reduce load-room rooms
           (.listFiles (java.io.File. dir)))))

(defn add-rooms
  "Look through all the files in a dir for files describing rooms and add
  them to the mire.rooms/rooms map."
  [dir]
  (dosync
   (alter rooms load-rooms dir)))

(defn room-contains?
  [room thing]
  (@(:items room) (keyword thing)))

(defn room-contains-enemy?
  "Checks if there is an enemy with given name in given room."
  [room enemy]
  (@(:enemies room) (keyword enemy)))

