#!/usr/bin/env bash
# exit if a command returns non-zero exit code
set -e

GITHUB_USERNAME=$1
if [ "$1" = "" ]
then
  echo "Usage: $0 <GitHub Username>"
  exit
fi

echo "-------------------"
echo "GITHUB_USERNAME=[$GITHUB_USERNAME]"
echo "-------------------"

./build-docker-image.sh

docker login --username=$GITHUB_USERNAME
docker push "skilltree/skills-service"
