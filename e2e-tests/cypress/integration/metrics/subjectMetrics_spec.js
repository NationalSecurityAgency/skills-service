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

describe('Metrics Tests - Subject', () => {

    const waitForSnap = 4000;

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });


    it.only('level breakdown', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.server()
            .route('/admin/projects/proj1/charts/numUsersPerLevelChartBuilder?subjectId=subj1')
            .as('numUsersPerLevelChartBuilderSubj1');

        cy.server()
            .route('/admin/projects/proj1/charts/numUsersPerLevelChartBuilder?subjectId=subj2')
            .as('numUsersPerLevelChartBuilderSubj2');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: 'Interesting Subject 2',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
        }
        ;

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        const numDays = 1;
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            for (let userCounter = 1; userCounter <= dayCounter; userCounter += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `user${dayCounter}-${userCounter}achieved@skills.org`,
                        timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        cy.visit('/projects/proj1/subjects/subj1');
        cy.clickNav('Metrics');
        cy.wait('@numUsersPerLevelChartBuilderSubj1');

        cy.wait(waitForSnap);
        cy.get('[data-cy=levelsChart]').matchImageSnapshot();

        cy.visit('/projects/proj1/subjects/subj2');
        cy.clickNav('Metrics');
        cy.wait('@numUsersPerLevelChartBuilderSubj2');

        cy.get('[data-cy=levelsChart]').contains('No one reached Level 1');
    });


    it('subject users per day - with real data', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.server()
            .route('/admin/projects/proj1/charts/distinctUsersOverTimeForProject**')
            .as('distinctUsersOverTimeForProject');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: 'Interesting Subject 2',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
            cy.request('POST', `/admin/projects/proj1/subjects/subj2/skills/subj2skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj2',
                skillId: `subj2skill${skillsCounter}`,
                name: `Very Other Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '1',
            });
        }

        const m = moment.utc().subtract(1, 'day');
        const numDays = 5;
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            for (let userCounter = 1; userCounter <= dayCounter; userCounter += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `user${dayCounter}-${userCounter}achieved@skills.org`,
                        timestamp: m.clone()
                            .subtract(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        const noDataMsg = 'This chart needs at least 2 days of user activity';

        cy.visit('/projects/proj1/subjects/subj1');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');
        cy.get('[data-cy=distinctNumUsersOverTime]').contains(noDataMsg).should('not.exist');

        cy.get('[data-cy=distinctNumUsersOverTime] [data-cy=apexchart]')
            .should('be.visible')
            .and(chart => {
                // we can assert anything about the chart really
                expect(chart.height()).to.be.greaterThan(350)
            })

        cy.visit('/projects/proj1/subjects/subj2');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');
        cy.get('[data-cy=distinctNumUsersOverTime]').contains(noDataMsg)
    });

    it('subject users per day - time controls call out to the server', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.server()
            .route('/admin/projects/proj1/charts/distinctUsersOverTimeForProject**')
            .as('distinctUsersOverTimeForProject');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        cy.visit('/projects/proj1/subjects/subj1');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.get('[data-cy=distinctNumUsersOverTime] [data-cy=timeLengthSelector]').contains('6 months').click();
        cy.wait('@distinctUsersOverTimeForProject');
        cy.get('[data-cy=distinctNumUsersOverTime]').contains('This chart needs at least 2 days of user activity')

        cy.get('[data-cy=distinctNumUsersOverTime] [data-cy=timeLengthSelector]').contains('1 year').click();
        cy.wait('@distinctUsersOverTimeForProject');
        cy.get('[data-cy=distinctNumUsersOverTime]').contains('This chart needs at least 2 days of user activity')
    });


    it('subject users per day - with user counts', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.server()
            .route({
                url: '/admin/projects/proj1/charts/distinctUsersOverTimeForProject**',
                response: [{
                    'value': 1600819200000,
                    'count': 35
                }, {
                    'value': 1600905600000,
                    'count': 125
                }, {
                    'value': 1600992000000,
                    'count': 15
                }, {
                    'value': 1601078400000,
                    'count': 0
                }, {
                    'value': 1601164800000,
                    'count': 70
                }, {
                    'value': 1601251200000,
                    'count': 552
                }, {
                    'value': 1601337600000,
                    'count': 1250
                }, {
                    'value': 1601424000000,
                    'count': 6232
                }, {
                    'value': 1601510400000,
                    'count': 125
                }, {
                    'value': 1601596800000,
                    'count': 526
                }, {
                    'value': 1601683200000,
                    'count': 0
                }, {
                    'value': 1601769600000,
                    'count': 1523
                }, {
                    'value': 1601856000000,
                    'count': 2513
                }, {
                    'value': 1601942400000,
                    'count': 542
                }, {
                    'value': 1602028800000,
                    'count': 2151
                }, {
                    'value': 1602115200000,
                    'count': 1526
                }, {
                    'value': 1602201600000,
                    'count': 562
                }, {
                    'value': 1602288000000,
                    'count': 1585
                }, {
                    'value': 1602374400000,
                    'count': 0
                }, {
                    'value': 1602460800000,
                    'count': 0
                }, {
                    'value': 1602547200000,
                    'count': 0
                }, {
                    'value': 1602633600000,
                    'count': 0
                }, {
                    'value': 1602720000000,
                    'count': 0
                }, {
                    'value': 1602806400000,
                    'count': 0
                }, {
                    'value': 1602892800000,
                    'count': 5
                }, {
                    'value': 1602979200000,
                    'count': 4
                }, {
                    'value': 1603065600000,
                    'count': 3322
                }, {
                    'value': 1603152000000,
                    'count': 2512
                }, {
                    'value': 1603238400000,
                    'count': 1525
                }, {
                    'value': 1603324800000,
                    'count': 1526
                }, {
                    'value': 1603411200000,
                    'count': 6253
                }]
            })
            .as('distinctUsersOverTimeForProject');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        cy.visit('/projects/proj1/subjects/subj1');
        cy.clickNav('Metrics');
        cy.wait('@distinctUsersOverTimeForProject');

        cy.wait(waitForSnap);
        cy.get('[data-cy=distinctNumUsersOverTime]').matchImageSnapshot();
    });


})
