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

describe('Metrics Tests - Skills', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('skills table - empty table', () => {
        cy.server()
            .route('/admin/projects/proj1/charts/skillUsageNavigatorChartBuilder')
            .as('skillUsageNavigatorChartBuilder');
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Skills]').click();
        cy.wait('@skillUsageNavigatorChartBuilder')

        cy.get('[data-cy=skillsNavigator]').contains('There are no records to show');
    });

    it('skills table - paging', () => {
        cy.server()
            .route('/admin/projects/proj1/charts/skillUsageNavigatorChartBuilder')
            .as('skillUsageNavigatorChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        const numSkills = 8;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '50',
                numPerformToCompletion: '1',
            });
        };

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Skills]').click();
        cy.wait('@skillUsageNavigatorChartBuilder');

        const tableSelector = '[data-cy=skillsNavigator-table]'
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 1' }, { colIndex: 3,  value: 'Never' }, { colIndex: 4,  value: 'Never' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 2' }, { colIndex: 3,  value: 'Never' }, { colIndex: 4,  value: 'Never' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }, { colIndex: 3,  value: 'Never' }, { colIndex: 4,  value: 'Never' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 4' }, { colIndex: 3,  value: 'Never' }, { colIndex: 4,  value: 'Never' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 5' }, { colIndex: 3,  value: 'Never' }, { colIndex: 4,  value: 'Never' }],
        ], 5, true, 8);

        // test paging
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 4' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 5' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 6' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 7' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 8' }],
        ]);

        // test page size
        cy.get('[data-cy=skillsBTablePageSize]').select('10');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 4' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 5' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 6' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 7' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 8' }],
        ], 10, true, 8);

    });

    it('skills table - sorting', () => {
        cy.server()
            .route('/admin/projects/proj1/charts/skillUsageNavigatorChartBuilder')
            .as('skillUsageNavigatorChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        const numSkills = 8;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '50',
                numPerformToCompletion: '2',
            });
        }

        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            for (let i = 0; i < skillsCounter; i += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill${skillsCounter}`,
                    {
                        userId: `user${i}achieved@skills.org`,
                        timestamp: m.clone()
                            .subtract(skillsCounter, 'day')
                            .format('x')
                    });
                cy.request('POST', `/api/projects/proj1/skills/skill${skillsCounter}`,
                    {
                        userId: `user${i}achieved@skills.org`,
                        timestamp: m.clone()
                            .subtract(skillsCounter-1, 'day')
                            .format('x')
                    });
            }

            for (let i = skillsCounter; i >= 0; i -= 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill${skillsCounter}`,
                    {
                        userId: `progress${i}user@skills.org`,
                        timestamp: m.clone()
                            .subtract(skillsCounter-1, 'day')
                            .format('x')
                    });
            }
        }
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Skills]')
            .click();
        cy.wait('@skillUsageNavigatorChartBuilder')

        const tableSelector = '[data-cy=skillsNavigator-table]'

        cy.get(`${tableSelector} th`).contains('Skill').click()
        const expectedSkillNames = [
            [{ colIndex: 0,  value: 'Very Great Skill # 1' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 2' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 4' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 5' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 6' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 7' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 8' }],
        ]
        cy.validateTable(tableSelector, expectedSkillNames);

        cy.get(`${tableSelector} th`).contains('Skill').click()
        cy.validateTable(tableSelector, [...expectedSkillNames].reverse());

        cy.get(`${tableSelector} th`).contains('# Users Achieved').click()
        const numUserAchievedExpected = [
            [{ colIndex: 1,  value: '1' }],
            [{ colIndex: 1,  value: '2' }],
            [{ colIndex: 1,  value: '3' }],
            [{ colIndex: 1,  value: '4' }],
            [{ colIndex: 1,  value: '5' }],
            [{ colIndex: 1,  value: '6' }],
            [{ colIndex: 1,  value: '7' }],
            [{ colIndex: 1,  value: '8' }],
        ]
        cy.validateTable(tableSelector, numUserAchievedExpected);

        cy.get(`${tableSelector} th`).contains('# Users Achieved').click()
        cy.validateTable(tableSelector, [...numUserAchievedExpected].reverse());

        cy.get(`${tableSelector} th`).contains('# Users In Progress').click()
        const numUserInProgressExpected = [
            [{ colIndex: 2,  value: '2' }],
            [{ colIndex: 2,  value: '3' }],
            [{ colIndex: 2,  value: '4' }],
            [{ colIndex: 2,  value: '5' }],
            [{ colIndex: 2,  value: '6' }],
            [{ colIndex: 2,  value: '7' }],
            [{ colIndex: 2,  value: '8' }],
            [{ colIndex: 2,  value: '9' }],
        ]
        cy.validateTable(tableSelector, numUserInProgressExpected);

        cy.get(`${tableSelector} th`).contains('# Users In Progress').click()
        cy.validateTable(tableSelector, [...numUserInProgressExpected].reverse());

        cy.get(`${tableSelector} th`).contains('Last Reported').click()
        const lastReportedExpected = [
            [{ colIndex: 3,  value: '2020-09-05 07:00' }],
            [{ colIndex: 3,  value: '2020-09-06 07:00' }],
            [{ colIndex: 3,  value: '2020-09-07 07:00' }],
            [{ colIndex: 3,  value: '2020-09-08 07:00' }],
            [{ colIndex: 3,  value: '2020-09-09 07:00' }],
            [{ colIndex: 3,  value: '2020-09-10 07:00' }],
            [{ colIndex: 3,  value: '2020-09-11 07:00' }],
            [{ colIndex: 3,  value: '2020-09-12 07:00' }],
        ]
        cy.validateTable(tableSelector, lastReportedExpected);

        cy.get(`${tableSelector} th`).contains('Last Reported').click()
        cy.validateTable(tableSelector, [...lastReportedExpected].reverse());
    });


    it('skills table - tag filtering', () => {
        // have to make viewport very wide so all the tags are on the same line
        // looks like there is an issue with cypress not being able to click on a tag
        // if it's pushed to the 2nd line
        cy.viewport(2048, 1024);
        cy.server()
            .route('/admin/projects/proj1/charts/skillUsageNavigatorChartBuilder')
            .as('skillUsageNavigatorChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        const numSkills = 17;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '50',
                numPerformToCompletion: skillsCounter < 17 ? '1' : '2',
            });
        }

        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');
        const skip = [3, 6]
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            if (!skip.includes(skillsCounter)) {
                for (let i = 0; i < skillsCounter; i += 1) {
                    cy.request('POST', `/api/projects/proj1/skills/skill${skillsCounter}`,
                        {
                            userId: `user${i}achieved@skills.org`,
                            timestamp: m.clone()
                                .subtract(skillsCounter, 'day')
                                .format('x')
                        });
                }
            }
        }


        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Skills]').click();
        cy.wait('@skillUsageNavigatorChartBuilder');

        cy.get('[data-cy=skillsNavigator-filters]').contains('Overlooked Skill').click();
        cy.get('[data-cy=skillsNavigator-filterBtn]').click();


        const tableSelector = '[data-cy=skillsNavigator-table]'

        cy.get(`${tableSelector} th`).contains('Skill').click()
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }, { colIndex: 1,  value: 'Overlooked Skill' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 6' }, { colIndex: 1,  value: 'Overlooked Skill' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 17' }, { colIndex: 1,  value: 'Overlooked Skill' }],
        ]);

        cy.get('[data-cy=skillsNavigator-resetBtn]').click();
        cy.get('[data-cy=skillsBTableTotalRows]').contains(17);

        cy.get('[data-cy=skillsNavigator-filters]').contains('Top Skill').click();
        cy.get('[data-cy=skillsNavigator-filterBtn]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 16' }, { colIndex: 1,  value: 'Top Skill' }],
        ]);

        cy.get('[data-cy=skillsNavigator-filters]').contains('Top Skill').click();
        cy.get('[data-cy=skillsNavigator-filters]').contains('High Activity').click();
        cy.get('[data-cy=skillsNavigator-filterBtn]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 17' }, { colIndex: 2,  value: 'High Activity' }],
        ]);

        cy.get('[data-cy=skillsNavigator-filters]').contains('Never Achieved').click();
        cy.get('[data-cy=skillsNavigator-filters]').contains('High Activity').click();
        cy.get('[data-cy=skillsNavigator-filterBtn]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }, { colIndex: 4,  value: 'Never' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 6' }, { colIndex: 4,  value: 'Never' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 17' }, { colIndex: 4,  value: 'Never' }],
        ])

        cy.get('[data-cy=skillsNavigator-filters]').contains('Never Achieved').click();
        cy.get('[data-cy=skillsNavigator-filters]').contains('Never Reported').click();
        cy.get('[data-cy=skillsNavigator-filterBtn]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }, { colIndex: 3,  value: 'Never' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 6' }, { colIndex: 3,  value: 'Never' }],
        ])

        cy.get('[data-cy=skillsNavigator-filters]').contains('Never Achieved').click();
        cy.get('[data-cy=skillsNavigator-filterBtn]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }, { colIndex: 3,  value: 'Never' }],
            [{ colIndex: 0,  value: 'Very Great Skill # 6' }, { colIndex: 3,  value: 'Never' }],
        ])

        cy.get('[data-cy=skillsNavigator-filters]').contains('Top Skill').click();
        cy.get('[data-cy=skillsNavigator-filterBtn]').click();

        cy.get(tableSelector).contains('There are no records to show');

        cy.get('[data-cy=skillsNavigator-resetBtn]').click();
        cy.get('[data-cy=skillsBTableTotalRows]').contains(17);

        cy.get('[data-cy=skillsNavigator-skillNameFilter]').type('12');
        cy.get('[data-cy=skillsNavigator-filterBtn]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 12' }],
        ])

        cy.get('[data-cy=skillsNavigator-skillNameFilter]').clear().type('1');
        cy.get('[data-cy=skillsNavigator-filters]').contains('Overlooked Skill').click();
        cy.get('[data-cy=skillsNavigator-filterBtn]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 17' }, { colIndex: 1,  value: 'Overlooked Skill' }],
        ]);

    });

})
