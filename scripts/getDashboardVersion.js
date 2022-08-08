/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

var pjson = require('../dashboard/package.json');
var fs = require('fs');

var dependencies = pjson.dependencies;
var dependencyKeys = Object.keys(dependencies);
var skillsClient = dependencyKeys.find((value) => {
    return value.includes("@skilltree/skills-client-");
});
var version = dependencies[skillsClient];

var versionString = skillsClient + '-' + version;

fs.writeFile('../service/src/main/resources/client-version.json', versionString, function (err) {
    if (err) {
        return console.log(err);
    }
})