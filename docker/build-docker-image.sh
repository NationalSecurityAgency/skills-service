#!/usr/bin/env bash
# exit if a command returns non-zero exit code
set -e

BUILD_DATE=$(date -u +'%Y-%m-%dT%H:%M:%SZ')
VERSION=$(cd ../ && mvn org.apache.maven.plugins:maven-help-plugin:3.1.0:evaluate -Dexpression=project.version -q -DforceStdout)
JAR_LOC=$(ls ../service/target/*.jar)
if [ "$JAR_LOC" = "" ]
then
  echo "Failed to find jar"
  exit
fi
rm -f skills-service.jar
cp $JAR_LOC skills-service.jar

#VCS_REF=$(git rev-parse $VERSION)

echo "-------------------"
echo "BUILD_DATE=[$BUILD_DATE]"
echo "VERSION=[$VERSION]"
echo "JAR_LOC=[$JAR_LOC]"
echo "VCS_REF=[$VCS_REF]"
echo "-------------------"

docker build --no-cache=true --build-arg BUILD_DATE=$BUILD_DATE --build-arg VERSION=$VERSION --build-arg VCS_REF=$VCS_REF -t "skilltree/skills-service" .
docker tag "skilltree/skills-service" "skilltree/skills-service:$VERSION"
