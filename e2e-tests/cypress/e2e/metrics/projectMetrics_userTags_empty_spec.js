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
var moment = require('moment-timezone');

describe('Metrics Using User Tags Tests', () => {

    beforeEach(() => {
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.defaultUser, vars.defaultPass);
            });

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.defaultUser, vars.defaultPass);
            });
        cy.intercept('GET', '/public/config', (req) => {
            req.reply({
                body: {
                    projectMetricsTagCharts: '[{"key":"manyValues","type":"table","title":"Many Values","tagLabel":"Best Label"},{"key":"someValues","type":"bar","title":"Some Values"}]'
                },
            });
        })
            .as('getConfig');
        cy.viewport(1200, 1000);
    });

    it('user tag table and chart are empty', () => {
        cy.visit('/administrator/projects/proj1/');
        cy.wait('@getConfig');

        cy.clickNav('Metrics');
        cy.get('[data-cy="userTagTableCard"] [data-pc-section="header"]')
            .contains('Many Values');
        cy.get('[data-cy="userTagTableCard"]')
            .contains('There are no records to show');

        cy.get('[data-cy="userTagChart"] [data-pc-section="header"]')
            .contains('Some Values');
        cy.get('[data-cy="userTagChart"]')
            .contains('No data yet');
    });

});
