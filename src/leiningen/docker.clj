(ns leiningen.docker
  (:require [leiningen.core.eval :as eval]
            [leiningen.core.main :as main]))

(defn- exec [& args]
  (apply main/debug "Exec: docker" args)
  (apply eval/sh "docker" args))

(def valid-command? #{:build :push})

(defn docker
  "Builds and delpoys docker images.
   Commands:
     'build' builds your docker image
     'push' pushes your docker image"
  [project command]

  (let [command (keyword command)]

    (when-not (valid-command? command)
      (main/warn "Invalid command" command)
      (main/exit 1))

    (let [config (:docker project)
          image-name (or (:image-name config)
                         (str (:name project)))
          image-version (:version project)
          image (str image-name ":" image-version)
          build-dir (or (:build-dir config)
                        (:root project))
          dockerfile (or (:dockerfile config)
                         "Dockerfile")]

      (case command
        :build (do
                 (main/info "Building Docker image:" image)
                 (let [exit-code (exec "build" "-f" dockerfile "-t" image build-dir)]
                   (if (zero? exit-code)
                     (main/info "Docker image built.")
                     (do
                       (main/warn "Docker image could not be built.")
                       (main/exit exit-code)))))
        :push (do
                (main/info "Pushing Docker image:" image)
                (let [exit-code (exec "push" image)]
                  (if (zero? exit-code)
                    (main/info "Docker image pushed.")
                    (do
                      (main/warn "Docker image could not be pushed.")
                      (main/exit exit-code)))))))))
