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

describe('Metrics Tests', () => {

    const waitForSnap = 4000;

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('overall levels - empty', () => {
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();

        cy.contains("Level 5: 0 users");
        // we have to wait for background animation to complete
        cy.wait(waitForSnap);
        cy.get('[data-cy=projectOverallLevelsChart]').matchImageSnapshot('projectOverallLevelsChart-empty');
    });

    it('overall levels - users in all levels', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/numUsersPerLevelChartBuilder',
            status: 200,
            response: [{
                'value': 'Level 1',
                'count': 6251
            }, {
                'value': 'Level 2',
                'count': 4521
            }, {
                'value': 'Level 3',
                'count': 3525
            }, {
                'value': 'Level 4',
                'count': 1254
            }, {
                'value': 'Level 5',
                'count': 754
            }],
        }).as('getLevels');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();

        cy.contains("Level 5: 754 users");
        cy.contains("Level 4: 1,254 users");
        cy.contains("Level 3: 3,525 users");
        cy.contains("Level 2: 4,521 users");
        cy.contains("Level 1: 6,251 users");

        // we have to wait for background animation to complete
        cy.wait(waitForSnap);
        cy.get('[data-cy=projectOverallLevelsChart]').matchImageSnapshot('projectOverallLevelsChart-allLevels');
    });

})
