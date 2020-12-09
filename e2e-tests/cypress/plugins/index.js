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

const {
    addMatchImageSnapshotPlugin,
} = require('cypress-image-snapshot/plugin');

module.exports = (on, config) => {
  // `on` is used to hook into various events Cypress emits
  // `config` is the resolved Cypress config
    addMatchImageSnapshotPlugin(on, config);

    on("before:browser:launch", (browser = {}, launchOptions) => {
        prepareAudit(launchOptions);
    });

    on("task", {
        lighthouse: lighthouse((lighthouseReport) => {
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
        }
    });
};
