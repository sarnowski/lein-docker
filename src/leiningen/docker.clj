(ns leiningen.docker
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]))

(defn- exec [& args]
  (apply main/debug "Exec: docker" args)
  (apply eval/sh "docker" args))

(defn- build [image dockerfile build-dir]
  (main/info "Building Docker image:" image)
  (let [exit-code (exec "build" "-f" dockerfile "-t" image build-dir)]
    (if (zero? exit-code)
      (main/info "Docker image built.")
      (do
        (main/warn "Docker image could not be built.")
        (main/exit exit-code)))))

(defn- tag [image tagged-image]
  (main/info "Tagging Docker image:" tagged-image)
  (let [exit-code (exec "tag" image tagged-image)]
    (if (zero? exit-code)
      (main/info "Docker image tagged.")
      (do
        (main/warn "Docker image could not be tagged.")
        (main/exit exit-code)))))

(defn- push [image]
  (main/info "Pushing Docker image:" image)
  (let [exit-code (exec "push" image)]
    (if (zero? exit-code)
      (main/info "Docker image pushed.")
      (do
        (main/warn "Docker image could not be pushed.")
        (main/exit exit-code)))))

(def valid-command? #{:build :push})

(defn docker
  "Builds and delpoys docker images.
   Commands:
     'build' builds your docker image
     'push' pushes your docker image"
  [project command & [image-name]]

  (let [command (keyword command)]

    (when-not (valid-command? command)
      (main/warn "Invalid command" command)
      (main/exit 1))

    (let [config (:docker project)
          image-name (or image-name
                         (:image-name config)
                         (str (:name project)))
          tags (or (:tags config)
                   ["%s"])
          images (map
                   #(str image-name ":" (format % (:version project)))
                   tags)
          build-dir (or (:build-dir config)
                        (:root project))
          dockerfile (or (:dockerfile config)
                         "Dockerfile")]

      (case command
        :build (do
                 (build (first images) dockerfile build-dir)
                 (doseq [image (rest images)]
                   (tag (first images) image)))
        :push (doseq [image images]
                (push image))))))
