#!/usr/bin/env bash

echo "@skills:registry=http://$NEXUS_SERVER/repository/skills-registry/" > ~/.npmrc
cat ~/.npmrc
echo "<settings><mirrors><mirror><id>central</id><name>central</name><url>http://$NEXUS_SERVER/repository/maven-public/</url><mirrorOf>*</mirrorOf></mirror></mirrors></settings>" > ~/.m2/settings.xml
cat ~/.m2/settings.xml
