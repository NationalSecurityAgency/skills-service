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

describe('Metrics Tests - Skills Sorting', () => {

    before(() => {
        cy.beforeTestSuiteThatReusesData()
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('/admin/projects/proj1/metrics/skillUsageNavigatorChartBuilder')
            .as('skillUsageNavigatorChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

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
                            .subtract(skillsCounter - 1, 'day')
                            .format('x')
                    });
            }

            for (let i = skillsCounter; i >= 0; i -= 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill${skillsCounter}`,
                    {
                        userId: `progress${i}user@skills.org`,
                        timestamp: m.clone()
                            .subtract(skillsCounter - 1, 'day')
                            .format('x')
                    });
            }
        }
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    it('skills table - sorting by skill', () => {
        cy.visit('/administrator/projects/proj1/metrics/skills');
        cy.wait('@skillUsageNavigatorChartBuilder');
        const tableSelector = '[data-cy=skillsNavigator-table]';
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');
        cy.get(`${tableSelector} th`)
            .contains('Skill')
            .realClick();
        const expectedSkillNames = [
            [{
                colIndex: 0,
                value: 'Very Great Skill # 1'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill # 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill # 3'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill # 4'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill # 5'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill # 6'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill # 7'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill # 8'
            }],
        ];
        cy.validateTable(tableSelector, expectedSkillNames);

        cy.get(`${tableSelector} th`)
            .contains('Skill')
            .realClick();
        cy.validateTable(tableSelector, [...expectedSkillNames].reverse());
    });

    it('skills table - sorting by num user achieved', () => {
        cy.visit('/administrator/projects/proj1/metrics/skills');
        cy.wait('@skillUsageNavigatorChartBuilder');
        const tableSelector = '[data-cy=skillsNavigator-table]';
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');

        cy.get(`${tableSelector} th`)
            .contains('# Users Achieved')
            .realClick();
        const numUserAchievedExpected = [
            [{
                colIndex: 1,
                value: '1'
            }],
            [{
                colIndex: 1,
                value: '2'
            }],
            [{
                colIndex: 1,
                value: '3'
            }],
            [{
                colIndex: 1,
                value: '4'
            }],
            [{
                colIndex: 1,
                value: '5'
            }],
            [{
                colIndex: 1,
                value: '6'
            }],
            [{
                colIndex: 1,
                value: '7'
            }],
            [{
                colIndex: 1,
                value: '8'
            }],
        ];
        cy.validateTable(tableSelector, numUserAchievedExpected);

        cy.get(`${tableSelector} th`)
            .contains('# Users Achieved')
            .realClick();
        cy.validateTable(tableSelector, [...numUserAchievedExpected].reverse());
    });

    it('skills table - sorting by num user in progress', () => {
        cy.visit('/administrator/projects/proj1/metrics/skills');
        cy.wait('@skillUsageNavigatorChartBuilder');
        const tableSelector = '[data-cy=skillsNavigator-table]';
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');
        cy.get(`${tableSelector} th`)
            .contains('# Users In Progress')
            .realClick();
        const numUserInProgressExpected = [
            [{
                colIndex: 2,
                value: '2'
            }],
            [{
                colIndex: 2,
                value: '3'
            }],
            [{
                colIndex: 2,
                value: '4'
            }],
            [{
                colIndex: 2,
                value: '5'
            }],
            [{
                colIndex: 2,
                value: '6'
            }],
            [{
                colIndex: 2,
                value: '7'
            }],
            [{
                colIndex: 2,
                value: '8'
            }],
            [{
                colIndex: 2,
                value: '9'
            }],
        ];
        cy.validateTable(tableSelector, numUserInProgressExpected);

        cy.get(`${tableSelector} th`)
            .contains('# Users In Progress')
            .realClick();
        cy.validateTable(tableSelector, [...numUserInProgressExpected].reverse());
    });

    it('skills table - sorting by last reported', () => {
        cy.visit('/administrator/projects/proj1/metrics/skills');
        cy.wait('@skillUsageNavigatorChartBuilder');
        const tableSelector = '[data-cy=skillsNavigator-table]';
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '8');
        cy.get(`${tableSelector} th`)
            .contains('Last Reported')
            .realClick();
        const lastReportedExpected = [
            [{
                colIndex: 3,
                value: '2020-09-05 11:00'
            }],
            [{
                colIndex: 3,
                value: '2020-09-06 11:00'
            }],
            [{
                colIndex: 3,
                value: '2020-09-07 11:00'
            }],
            [{
                colIndex: 3,
                value: '2020-09-08 11:00'
            }],
            [{
                colIndex: 3,
                value: '2020-09-09 11:00'
            }],
            [{
                colIndex: 3,
                value: '2020-09-10 11:00'
            }],
            [{
                colIndex: 3,
                value: '2020-09-11 11:00'
            }],
            [{
                colIndex: 3,
                value: '2020-09-12 11:00'
            }],
        ];
        cy.validateTable(tableSelector, lastReportedExpected);

        cy.get(`${tableSelector} th`)
            .contains('Last Reported')
            .realClick();
        cy.validateTable(tableSelector, [...lastReportedExpected].reverse());
    });
});
