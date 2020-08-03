#!/usr/bin/env bash
# exit if a command returns non-zero exit code
set -e

IMG_NAME=${1:-"skilltree/skills-service"}
DOCKER_USER=${2:-"${docker_username}"}
DOCKER_PASS=${3:-"${docker_password}"}

./build-docker-image.sh

docker login --username "${DOCKER_USER}" --password "${DOCKER_PASS}"
docker push $IMG_NAME
