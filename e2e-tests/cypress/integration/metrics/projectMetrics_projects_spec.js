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

    it('projects - Distinct number of users over time', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/distinctUsersOverTimeForProject',
            status: 200,
            response: [{
                'value': 1600128000000,
                'count': 2
            }, {
                'value': 1600214400000,
                'count': 2
            }, {
                'value': 1600300800000,
                'count': 2
            }, {
                'value': 1600387200000,
                'count': 1
            }, {
                'value': 1600473600000,
                'count': 1
            }, {
                'value': 1600560000000,
                'count': 2
            }, {
                'value': 1600646400000,
                'count': 3
            }, {
                'value': 1600732800000,
                'count': 3
            }, {
                'value': 1600819200000,
                'count': 3
            }, {
                'value': 1600905600000,
                'count': 3
            }, {
                'value': 1600992000000,
                'count': 2
            }, {
                'value': 1601078400000,
                'count': 1
            }, {
                'value': 1601164800000,
                'count': 2
            }, {
                'value': 1601251200000,
                'count': 4
            }, {
                'value': 1601337600000,
                'count': 3
            }, {
                'value': 1601424000000,
                'count': 4
            }, {
                'value': 1601510400000,
                'count': 4
            }, {
                'value': 1601596800000,
                'count': 3
            }, {
                'value': 1601683200000,
                'count': 12
            }, {
                'value': 1601769600000,
                'count': 10
            }, {
                'value': 1601856000000,
                'count': 60
            }, {
                'value': 1601942400000,
                'count': 45
            }, {
                'value': 1602028800000,
                'count': 2
            }, {
                'value': 1602115200000,
                'count': 52
            }, {
                'value': 1602201600000,
                'count': 72
            }],
        }).as('distinctUsersOverTimeForProject');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.wait(waitForSnap);
        cy.get('[data-cy=distinctNumUsersOverTime]').matchImageSnapshot();
    })

    it('projects - Distinct number of users over time - empty project', () => {
        cy.server().route('/admin/projects/proj1/charts/distinctUsersOverTimeForProject').as('distinctUsersOverTimeForProject');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.get('[data-cy=distinctNumUsersOverTime]').contains('This chart needs at least 2 days of user activity.');

        cy.get('[data-cy=distinctNumUsersOverTime]').get('.apexcharts-svg').get('line');

        // verify there is no chart
        cy.get('[data-cy=distinctNumUsersOverTime]').get('.apexcharts-svg .apexcharts-area-series')
            .should('be.visible')
            .and(chart => {
                expect(chart.height()).to.be.lessThan(200)
            });

        cy.wait(waitForSnap);
        cy.get('[data-cy=distinctNumUsersOverTime]').matchImageSnapshot();
    })

    it('projects - Distinct number of users over time - two days with real data', () => {
        cy.server().route('/admin/projects/proj1/charts/distinctUsersOverTimeForProject').as('distinctUsersOverTimeForProject');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        })
        const numSkills =1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Skill ${skillsCounter}`,
                pointIncrement: '200',
                numPerformToCompletion: '25',
            });
        };

        const m = moment.utc();
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user1Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');
        // make sure a line i rendered on a chart
        cy.get('[data-cy=distinctNumUsersOverTime]').get('.apexcharts-svg .apexcharts-area-series')
            .should('be.visible')
            .and(chart => {
                expect(chart.height()).to.be.greaterThan(200)
            });
    })

    it('projects - Distinct number of users over time - two days', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/distinctUsersOverTimeForProject',
            status: 200,
            response: [ {
                'value': 1602115200000,
                'count': 52
            }, {
                'value': 1602201600000,
                'count': 82
            }],
        }).as('distinctUsersOverTimeForProject');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.wait(waitForSnap);
        cy.get('[data-cy=distinctNumUsersOverTime]').matchImageSnapshot();
    })

    it('projects - Distinct number of users over time - one days', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/distinctUsersOverTimeForProject',
            status: 200,
            response: [ {
                'value': 1602115200000,
                'count': 52
            }],
        }).as('distinctUsersOverTimeForProject');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');
        cy.get('[data-cy=distinctNumUsersOverTime]').contains('This chart needs at least 2 days of user activity.');

        cy.wait(waitForSnap);
        cy.get('[data-cy=distinctNumUsersOverTime]').matchImageSnapshot();
    })

})
