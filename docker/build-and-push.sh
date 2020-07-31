#!/usr/bin/env bash
# exit if a command returns non-zero exit code
set -e

./build-docker-image.sh

docker login
docker push "skilltree/skills-service"
