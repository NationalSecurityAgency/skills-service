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

describe('Metrics Tests - Achievements', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('achievements table - empty table', () => {
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();

        cy.get('[data-cy=achievementsNavigator]').contains('There are no records to show');
    });

    it('achievements table - few rows', () => {
        cy.server()
            .route('/admin/projects/proj1/charts/userAchievementsChartBuilder?**')
            .as('userAchievementsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        const numSkills = 5;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter < 3 ? '1' : '200',
            });
        };

        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(4, 'day').format('x')})

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();
        cy.wait('@userAchievementsChartBuilder')

        const tableSelector = '[data-cy=achievementsNavigator-table]'
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'user0good@skills.org'
            }, {
                colIndex: 1,
                value: 'Skill'
            }, {
                colIndex: 2,
                value: 'Very Great Skill # 1'
            }, {
                colIndex: 3,
                value: 'N/A'
            }, {
                colIndex: 4,
                value: '2020-09-11 07:00'
            }],
            [{
                colIndex: 0,
                value: 'user0good@skills.org'
            }, {
                colIndex: 1,
                value: 'Skill'
            }, {
                colIndex: 2,
                value: 'Very Great Skill # 2'
            }, {
                colIndex: 3,
                value: 'N/A'
            }, {
                colIndex: 4,
                value: '2020-09-08 07:00'
            }],
        ]);



    });

    it('achievements table - sorting', () => {
        cy.server()
            .route('/admin/projects/proj1/charts/userAchievementsChartBuilder?**')
            .as('userAchievementsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '150',
                numPerformToCompletion: '15',
            });
        };

        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(2, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'another@skills.org', timestamp: m.clone().subtract(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'another@skills.org', timestamp: m.clone().subtract(4, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'another@skills.org', timestamp: m.clone().subtract(5, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'thereYouGo@skills.org', timestamp: m.clone().subtract(6, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'thereYouGo@skills.org', timestamp: m.clone().subtract(7, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'thereYouGo@skills.org', timestamp: m.clone().subtract(8, 'day').format('x')})

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();
        cy.wait('@userAchievementsChartBuilder')


        // default is descending by date
        const tableSelector = '[data-cy=achievementsNavigator-table]'
        const expected = [
            [{ colIndex: 4,  value: '2020-09-11 07:00' }],
            [{ colIndex: 4,  value: '2020-09-11 07:00' }],
            [{ colIndex: 4,  value: '2020-09-09 07:00' }],
            [{ colIndex: 4,  value: '2020-09-09 07:00' }],
            [{ colIndex: 4,  value: '2020-09-06 07:00' }],
            [{ colIndex: 4,  value: '2020-09-06 07:00' }],
        ]
        cy.validateTable(tableSelector, expected);

        cy.get('[data-cy=achievementsNavigator-table] th').contains('Date').click()
        cy.wait('@userAchievementsChartBuilder')
        const expectedReversed = [...expected].reverse();
        cy.validateTable(tableSelector, expectedReversed);

        cy.get('[data-cy=achievementsNavigator-table] th').contains('Username').click()
        cy.wait('@userAchievementsChartBuilder')
        const expectedUsers = [
            [{ colIndex: 0,  value: 'another@skills.org' }],
            [{ colIndex: 0,  value: 'another@skills.org' }],
            [{ colIndex: 0,  value: 'thereyougo@skills.org' }],
            [{ colIndex: 0,  value: 'thereyougo@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
        ]
        cy.validateTable(tableSelector, expectedUsers);

        cy.get('[data-cy=achievementsNavigator-table] th').contains('Username').click()
        cy.wait('@userAchievementsChartBuilder')
        cy.validateTable(tableSelector, [...expectedUsers].reverse());
    });

    it('achievements table - filtering', () => {
        cy.server()
            .route('/admin/projects/proj1/charts/userAchievementsChartBuilder?**')
            .as('userAchievementsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '150',
                numPerformToCompletion: '15',
            });
        };

        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(2, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(4, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(5, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'another@skills.org', timestamp: m.clone().subtract(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'another@skills.org', timestamp: m.clone().subtract(4, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'another@skills.org', timestamp: m.clone().subtract(5, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'thereYouGo@skills.org', timestamp: m.clone().subtract(6, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'thereYouGo@skills.org', timestamp: m.clone().subtract(7, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'thereYouGo@skills.org', timestamp: m.clone().subtract(8, 'day').format('x')})

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();
        cy.wait('@userAchievementsChartBuilder')

        // default is descending by date
        const tableSelector = '[data-cy=achievementsNavigator-table]'

        cy.get('[data-cy=achievementsNavigator-usernameInput]').type('another');
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'another@skills.org' }, { colIndex: 1,  value: 'Overall' }],
            [{ colIndex: 0,  value: 'another@skills.org' }, { colIndex: 1,  value: 'Subject' }],
        ]);

        cy.get('[data-cy=achievementsNavigator-nameInput]').type('in');
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'another@skills.org' }, { colIndex: 1,  value: 'Subject' }],
        ]);

        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Subject').click();
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.get(tableSelector).contains('There are no records to show');

        cy.get('[data-cy=achievementsNavigator-nameInput]').clear();
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'another@skills.org' }, { colIndex: 1,  value: 'Overall' }],
        ]);

        cy.get('[data-cy=achievementsNavigator-resetBtn]').click();
        cy.wait('@userAchievementsChartBuilder');

        cy.get('[data-cy=achievementsNavigator-levelsInput]').select('Level 2');
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0good@skills.org' }, { colIndex: 3,  value: 2 }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }, { colIndex: 3,  value: 2 }],
        ]);

        cy.get('[data-cy=achievementsNavigator-nameInput]').type('in');
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Subject' }, { colIndex: 3,  value: 2 }],
        ]);

        cy.get('[data-cy=achievementsNavigator-nameInput]').clear();
        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Subject').click();
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Overall' }, { colIndex: 3,  value: 2 }],
        ]);

        cy.get('[data-cy=achievementsNavigator-resetBtn]').click();
        cy.wait('@userAchievementsChartBuilder');

        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Subject').click();
        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Overall').click();
        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Badge').click();
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.get(tableSelector).contains('There are no records to show');

        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Subject').click();
        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Skill').click();
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Subject' }],
            [{ colIndex: 1,  value: 'Subject' }],
            [{ colIndex: 1,  value: 'Subject' }],
            [{ colIndex: 1,  value: 'Subject' }],
        ]);

        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Subject').click();
        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Overall').click();
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'Overall' }],
            [{ colIndex: 1,  value: 'Overall' }],
            [{ colIndex: 1,  value: 'Overall' }],
            [{ colIndex: 1,  value: 'Overall' }],
        ]);

        cy.get('[data-cy=achievementsNavigator-resetBtn]').click();
        cy.wait('@userAchievementsChartBuilder');

        const now = new Date(2020, 8, 12).getTime()
        cy.clock(now)
        cy.get('[data-cy=achievementsNavigator-fromDateInput]').click();
        cy.get('.b-calendar-grid-body').contains('9').click();

        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 4,  value: '2020-09-11 07:00' }],
            [{ colIndex: 4,  value: '2020-09-11 07:00' }],
            [{ colIndex: 4,  value: '2020-09-11 07:00' }],
            [{ colIndex: 4,  value: '2020-09-11 07:00' }],
            [{ colIndex: 4,  value: '2020-09-09 07:00' }],
            [{ colIndex: 4,  value: '2020-09-09 07:00' }],
        ]);

        cy.get('[data-cy=achievementsNavigator-toDateInput]').click();
        cy.get('.b-calendar-grid-body').contains('10').click();
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 4,  value: '2020-09-09 07:00' }],
            [{ colIndex: 4,  value: '2020-09-09 07:00' }],
        ]);

        //////////////////////////////
        // select all of the filters
        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Skill').click();
        cy.get('[data-cy=achievementsNavigator-typeInput]').contains('Badge').click();
        cy.get('[data-cy=achievementsNavigator-nameInput]').type('subject');
        cy.get('[data-cy=achievementsNavigator-levelsInput]').select('Level 1');
        cy.get('[data-cy=achievementsNavigator-usernameInput]').type('another@skill');

        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 2,  value: 'Interesting Subject 1' }, { colIndex: 4,  value: '2020-09-09 07:00' }],
        ]);
    });


    it('achievements table - page sizes', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        const numSkills = 3;
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

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'This is a cool badge',
            "iconClass":"fas fa-jedi",
        });
        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1')

        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user0Good@skills.org', timestamp: m.clone().subtract(2, 'day').format('x')})

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user1Long0@skills.org', timestamp: m.clone().subtract(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user1Long0@skills.org', timestamp: m.clone().subtract(4, 'day').format('x')})

        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user2Smith0@skills.org', timestamp: m.clone().subtract(5, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user2Smith0@skills.org', timestamp: m.clone().subtract(6, 'day').format('x')})

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();

        const tableSelector = '[data-cy=achievementsNavigator-table]'
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
        ], 5, true, 35);

        cy.get('[data-cy=skillsBTablePageSize]').select('15');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user0good@skills.org' }],
            [{ colIndex: 0,  value: 'user1long0@skills.org' }],
        ], 15, true, 35);
    })

})
