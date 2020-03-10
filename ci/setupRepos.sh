# Copyright 2020 SkillTree
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#!/usr/bin/env bash

echo "@skills:registry=http://$NEXUS_SERVER/repository/skills-registry/" > ~/.npmrc
cat ~/.npmrc
echo "<settings><mirrors><mirror><id>central</id><name>central</name><url>http://$NEXUS_SERVER/repository/maven-public/</url><mirrorOf>*</mirrorOf></mirror></mirrors></settings>" > ~/.m2/settings.xml
cat ~/.m2/settings.xml

