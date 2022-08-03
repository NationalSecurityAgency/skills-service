#!/usr/bin/node

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