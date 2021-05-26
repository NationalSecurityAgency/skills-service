/*
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
// ***********************************************************
// This example plugins/index.js can be used to load plugins
//
// You can change the location of this file or turn off loading
// the plugins file with the 'pluginsFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/plugins-guide
// ***********************************************************

// This function is called when a project is opened or re-opened (e.g. due to
// the project's config changing)

const { lighthouse, pa11y, prepareAudit } = require("cypress-audit");
const objectScan = require('object-scan');
const fs = require('fs');
const Path = require('path');
const {
    addMatchImageSnapshotPlugin,
} = require('cypress-image-snapshot/plugin');
const { makeBadge, ValidationError } = require('badge-maker');

const deleteFolderRecursive = function(path) {
    if (fs.existsSync(path)) {
        fs.readdirSync(path).forEach((file, index) => {
            const curPath = Path.join(path, file);
            if (fs.lstatSync(curPath).isDirectory()) { // recurse
                deleteFolderRecursive(curPath);
            } else { // delete file
                fs.unlinkSync(curPath);
            }
        });
        fs.rmdirSync(path);
    }
};

const a11yScoresDir = 'a11y_scores';

const generateAvgLighthouseScore = () => {
    if (fs.existsSync(a11yScoresDir)) {
        const files = fs.readdirSync(a11yScoresDir)
        if (files) {
            let numScores = 0;
            let totalScore = 0;
            files.forEach((filename) => {
                const score = JSON.parse(fs.readFileSync(`${a11yScoresDir}/${filename}`, 'utf-8'))
                numScores++;
                totalScore+=score.score;
            });

            const score = Math.floor((totalScore/numScores)*100);
            let color = 'green';
            if (score < 90 && score > 75) {
                color = 'yellow';
            } else if (score <= 75) {
                color = 'red';
            }

            const format = {
                label: 'Avg Lighthouse Accessibility Score',
                message: `${score}/100`,
                color: color,
            };

            const svg = makeBadge(format)
            deleteFolderRecursive(a11yScoresDir);
            fs.writeFileSync('../average_accessibility_score.svg', svg);
        }
    }
};

module.exports = (on, config) => {
  // `on` is used to hook into various events Cypress emits
  // `config` is the resolved Cypress config
    addMatchImageSnapshotPlugin(on, config);

    on("before:browser:launch", (browser = {}, launchOptions) => {
        prepareAudit(launchOptions);
    });

    if (config.experimentalRunEvents) {
        on("after:run", () => {
            generateAvgLighthouseScore();
        });
    }

    on("task", {
        createAverageAccessibilityScore: ()=> {
            generateAvgLighthouseScore();
            return null;
        },
        lighthouse: lighthouse((lighthouseReport) => {
            if (!fs.existsSync(a11yScoresDir)) {
                fs.mkdirSync(a11yScoresDir)
            }

            if(lighthouseReport.lhr.categories.accessibility.score) {
                const timestamp = Date.now();
                const url = new URL(lighthouseReport.lhr.requestedUrl);
                const pathStr = url.pathname+url.search+url.hash;
                const score = { score: lighthouseReport.lhr.categories.accessibility.score, url: pathStr }
                const json = JSON.stringify(score);
                fs.writeFileSync(`${a11yScoresDir}/${timestamp}.score`, json);
            }

            if (lighthouseReport && lighthouseReport.artifacts
              && lighthouseReport.artifacts.Accessibility
              && lighthouseReport.artifacts.Accessibility.violations) {
                console.log('----------------------Lighthouse Violations----------------------');
                console.table(lighthouseReport.artifacts.Accessibility.violations, ['id', 'impact', 'description', 'help',]);
                lighthouseReport.artifacts.Accessibility.violations.forEach((viol) => {
                    if (viol.nodes) {
                        console.table(viol.nodes, ['impact', 'html', 'target', 'failureSummary', 'selector']);
                    }
                });
            }

            return null;
        }),
        pa11y: pa11y(),
        log(message) {
            console.log(message)

            return null;
        },
        table(message) {
            console.table(message)

            return null;
        },
    });
};

