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
    const waitForSnap = 4000;

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('overall levels - empty', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    }, () => {
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();

        cy.get('[data-cy=levelsChart]').contains("Level 5: 0 users");
        cy.get('[data-cy=levelsChart]').contains("Level 4: 0 users");
        cy.get('[data-cy=levelsChart]').contains("Level 3: 0 users");
        cy.get('[data-cy=levelsChart]').contains("Level 2: 0 users");
        cy.get('[data-cy=levelsChart]').contains("Level 1: 0 users");

        cy.get('[data-cy=levelsChart]').contains("No one reached Level 1 yet...");
    });

    it('overall levels - users in all levels',{
        retries: {
            runMode: 0,
            openMode: 0
        }
    }, () => {
        cy.intercept({
            url: '/admin/projects/proj1/metrics/numUsersPerLevelChartBuilder',
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
    });

    it('achievements table - empty table', () => {
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();

        cy.get('[data-cy=achievementsNavigator]').contains('There are no records to show');
    });

    it('achievements table - few rows', () => {
        cy.viewport(1500, 750);

        cy.server()
            .route('/admin/projects/proj1/metrics/userAchievementsChartBuilder?**')
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
                value: 'user0Good@skills.org'
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
                value: '2020-09-11 11:00'
            }],
            [{
                colIndex: 0,
                value: 'user0Good@skills.org'
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
                value: '2020-09-08 11:00'
            }],
        ]);



    });

    it('achievements table - validate the link to user client display', () => {
        cy.server()
            .route('/admin/projects/proj1/metrics/userAchievementsChartBuilder?**')
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
                value: 'user0Good@skills.org'
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
                value: '2020-09-11 11:00'
            }],
            [{
                colIndex: 0,
                value: 'user0Good@skills.org'
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
                value: '2020-09-08 11:00'
            }],
        ]);

        cy.get(`${tableSelector} tbody tr`).should('have.length', 2).as('cyRows');
        cy.get('@cyRows').eq(0).find('td').as('row');
        cy.get('@row').eq(0).find('[data-cy=achievementsNavigator-clientDisplayBtn]').click();

        cy.get('[data-cy=subPageHeader]').contains('Client Display');
        // userId has lowercase "g" while userIdForDisplay has uppercase "G"; this must be userId
        cy.get('[data-cy=pageHeader]').contains('user0good@skills.org');
    });


    it('achievements table - sorting', () => {
        cy.server()
            .route('/admin/projects/proj1/metrics/userAchievementsChartBuilder?**')
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
            [{ colIndex: 4,  value: '2020-09-11 11:00' }],
            [{ colIndex: 4,  value: '2020-09-11 11:00' }],
            [{ colIndex: 4,  value: '2020-09-09 11:00' }],
            [{ colIndex: 4,  value: '2020-09-09 11:00' }],
            [{ colIndex: 4,  value: '2020-09-06 11:00' }],
            [{ colIndex: 4,  value: '2020-09-06 11:00' }],
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
            [{ colIndex: 0,  value: 'thereYouGo@skills.org' }],
            [{ colIndex: 0,  value: 'thereYouGo@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
        ]
        cy.validateTable(tableSelector, expectedUsers);

        cy.get('[data-cy=achievementsNavigator-table] th').contains('Username').click()
        cy.wait('@userAchievementsChartBuilder')
        cy.validateTable(tableSelector, [...expectedUsers].reverse());
    });

    it('achievements table - filtering', () => {
        cy.server()
            .route('/admin/projects/proj1/metrics/userAchievementsChartBuilder?**')
            .as('userAchievementsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: "Other Subject 2",
        })

        cy.request('POST', '/admin/projects/proj1/subjects/subj3', {
            projectId: 'proj1',
            subjectId: 'subj3',
            name: "Other Subject 3",
        })

        const numSkills = 3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj${skillsCounter}/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: `subj${skillsCounter}`,
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '150',
                numPerformToCompletion: 25,
            });
        };

        const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(1, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(2, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(4, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(5, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(6, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(7, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(8, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(5, 'day').format('x')})
        for (let i = 1; i <= 10; i += 1) {
            cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(i, 'day').format('x')})
            cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user0Good@skills.org', timestamp: m.clone().add(i, 'day').format('x')})
        }

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'another@skills.org', timestamp: m.clone().add(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'another@skills.org', timestamp: m.clone().add(4, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'another@skills.org', timestamp: m.clone().add(5, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'another@skills.org', timestamp: m.clone().add(6, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'another@skills.org', timestamp: m.clone().add(7, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'another@skills.org', timestamp: m.clone().add(8, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'another@skills.org', timestamp: m.clone().add(15, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'another@skills.org', timestamp: m.clone().add(16, 'day').format('x')})


        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'thereYouGo@skills.org', timestamp: m.clone().add(6, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'thereYouGo@skills.org', timestamp: m.clone().add(7, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'thereYouGo@skills.org', timestamp: m.clone().add(8, 'day').format('x')})

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
            [{ colIndex: 0,  value: 'another@skills.org' }, { colIndex: 2,  value: 'Other Subject 2' }],
            [{ colIndex: 0,  value: 'another@skills.org' }, { colIndex: 2,  value: 'Interesting Subject 1' }],
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
            [{ colIndex: 0,  value: 'user0Good@skills.org' }, { colIndex: 3,  value: 2 }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }, { colIndex: 3,  value: 2 }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }, { colIndex: 3,  value: 2 }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }, { colIndex: 3,  value: 2 }],
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
            [{ colIndex: 1,  value: 'Subject' }],
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
        ]);

        cy.get('[data-cy=achievementsNavigator-resetBtn]').click();
        cy.wait('@userAchievementsChartBuilder');

        const now = new Date(2020, 8, 12).getTime()
        cy.clock(now)
        cy.get('[data-cy=achievementsNavigator-fromDateInput]').click();
        cy.get('.b-calendar-grid-body').contains('18').click();

        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 4,  value: '2020-09-28 11:00' }],
            [{ colIndex: 4,  value: '2020-09-20 11:00' }],
            [{ colIndex: 4,  value: '2020-09-20 11:00' }],
            [{ colIndex: 4,  value: '2020-09-20 11:00' }],
            [{ colIndex: 4,  value: '2020-09-20 11:00' }],
            [{ colIndex: 4,  value: '2020-09-19 11:00' }],
            [{ colIndex: 4,  value: '2020-09-19 11:00' }],
            [{ colIndex: 4,  value: '2020-09-19 11:00' }],
            [{ colIndex: 4,  value: '2020-09-19 11:00' }],
        ]);

        cy.get('[data-cy=achievementsNavigator-toDateInput]').click();
        cy.get('.b-calendar-grid-body').contains('19').click();
        cy.get('[data-cy=achievementsNavigator-filterBtn]').click();
        cy.wait('@userAchievementsChartBuilder');
        cy.validateTable(tableSelector, [
            [{ colIndex: 4,  value: '2020-09-19 11:00' }],
            [{ colIndex: 4,  value: '2020-09-19 11:00' }],
            [{ colIndex: 4,  value: '2020-09-19 11:00' }],
            [{ colIndex: 4,  value: '2020-09-19 11:00' }],
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
            [{ colIndex: 2,  value: 'Interesting Subject 1' }, { colIndex: 4,  value: '2020-09-19 11:00' }],
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
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
        ], 5, true, 35);

        cy.get('[data-cy=skillsBTablePageSize]').select('15');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user0Good@skills.org' }],
            [{ colIndex: 0,  value: 'user1Long0@skills.org' }],
        ], 15, true, 35);
    })

})
