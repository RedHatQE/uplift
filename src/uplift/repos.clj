;; This namespaces contains functionality to parse through a repo file as well as generate new
;; repo files

(ns uplift.repos
  (:require [clojure.string :as str]
            [clojure.java.io :as cjio]
            [uplift.core :as uc])
  (:import [java.nio.file Files]))

(def latest-rhel7-server "[latest-rhel7-server]") 
(def latest-rhel7-server-optional "[latest-rhel7-server-optional]")
(def latest-rhel7-server-debuginfo "[latest-rhel7-server-debuginfo]")
(def repodata {:debug "debug/tree"
               :prod "os"})


(defprotocol ToConfig
  (write-to-config [this filename] "Creates a config file representation"))

(defn- write-conf [obj fname]
  (letfn [(mkstr [key]
            (let [keyname (clojure.core/name key)
                  val (key obj)]
              (str keyname "=" val "\n")))]
    (with-open [newfile (cjio/writer fname :append true)]
      (.write newfile (:reponame obj))
      (.newLine newfile)
      (doseq [line (map mkstr [:name :baseurl :enabled :gpgcheck])]
        (.write newfile line))
      (.newLine newfile))))

(defrecord YumRepo
    [^String reponame  ;; The section eg [latest-rhel7-server
     ^String name      ;; description of repo
     ^String baseurl   ;; the baseurl to pull down content
     ^String enabled   ;; boolean (0 or 1 or True|False)
     ^String gpgcheck  ;; Boolean to decide to check gpg key
     ]
  ToConfig
  (write-to-config [this filename]
    (write-conf this filename)))

(comment
    (letfn [(mkstr [key]
              (let [keyname (clojure.core/name key)
                    val (key this)]
                (str keyname "=" val)))]
    (with-open [newfile (cjio/writer filename :append true)]
      (.write newfile (:reponame this))
      (.newLine newfile)
      (doseq [line (map mkstr [:name :baseurl :enabled :gpgcheck])]
        (.write newfile line))
      (.newLine newfile)))
    )
    


;; Scrape results of the baseurl link
(defn list-dirs
  "Retrieves a list of all elements"
  [base-url]
  )


(defn build-url
  "Formats the compose string
   :type one of :released :rel-eng or :nightly
   :extra a suffix to be added RHEL version (eg '20150720.0' or '7.2-Beta-1.0')
   :flavor can be something like 'Server' or 'Workstation'
   :arch the arch to target (eg :x86_64, :aarch64, :ppc64, :ppc64le, :s390x)
   "
  [& {:keys [rtype version flavor arch debug]
      :as opts
      :or {rtype :rel-eng
           version "7.2"
           flavor "Server"
           arch "x86_64"
           debug false}}]
  (println "in build-url")
  (doseq [[k v] opts]
    (println k "=" v))
  (let [repod (if debug "debug/tree" "os")]
    (format (rtype base-urls) version flavor arch repod)))

(defn make-base-server
  [rtype version repo & {:keys [flavor arch enabled gpgcheck description debug]
                         :as opts
                         :or {flavor "Server"
                              arch "x86_64"
                              enabled "1"
                              gpgcheck "0"
                              debug false}}]
  (println "in make-base-server")
  (doseq [[k v] opts]
    (println k "=" v))
  (let [url (build-url :rtype rtype :version version :flavor flavor :arch arch :debug debug)]
    (println url)
    (-> {:reponame repo
         :name (if description
                 description
                 "latest-RHEL7 Server from download.devel.redhat.com")
         :baseurl url
         :enabled enabled
         :gpgcheck gpgcheck}
        map->YumRepo)))

;; TODO Make a macro to autogenerate 
(defn latest-rel-eng-server
  "Convenience function to make latest repo"
  [version]
  (make-base-server :rel-eng version latest-rhel7-server))

(defn latest-released-server
  [version]
  (make-base-server :released version latest-rhel7-server))

(defn latest-nightly-server
  [version]
  (make-base-server :nightly version latest-rhel7-server))

(defn make-default-repo-file
  "Creates a repo file in /etc/yum.repos.d/rhel-latest.repo"
  [version & {:keys [fpath clear]
              :or {fpath "/etc/yum.repos.d/rhel-latest.repo"
                   clear false}}]
  (let [latest (latest-rel-eng-server version)
        latest-optional (make-base-server :rel-eng version latest-rhel7-server-optional :flavor "Server-optional")
        latest-debuginfo (make-base-server :rel-eng version latest-rhel7-server-debuginfo :enabled 0 :debug true)]
    (write-to-config latest fpath)
    (write-to-config latest-optional fpath)
    (write-to-config latest-debuginfo fpath)))
    