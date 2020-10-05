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

    it('end-to-end metrics with real user data', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        })

        const numSkills =3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Skill ${skillsCounter}`,
                pointIncrement: '50',
                numPerformToCompletion: '25',
            });
        };

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'This is a cool badge',
            "iconClass":"fas fa-jedi",
        });
        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1')

        cy.reportHistoryOfEvents('proj1', 'user0Good@skills.org', 25, [6,7], ['skill1', 'skill2', 'skill3']);
        cy.reportHistoryOfEvents('proj1', 'user1Long0@skills.org', 12, [3], ['skill1', 'skill2', 'skill3']);
        cy.reportHistoryOfEvents('proj1', 'user2Smith0@skills.org', 20, [5,6], ['skill1', 'skill2', 'skill3']);

        cy.reportHistoryOfEvents('proj1', 'user3Some0@skills.org', 25, [], ['skill1']);

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();
    })


    it('achievements table', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        const numSkills =3;
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
    })

    it('subjects - num users per level', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/NumUsersPerSubjectPerLevelChartBuilder',
            status: 200,
        }).as('getChartData');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: "Interesting Subject 2",
        })

        cy.request('POST', '/admin/projects/proj1/subjects/subj3', {
            projectId: 'proj1',
            subjectId: 'subj3',
            name: "Interesting Subject 3",
        })

        const numSkills =3;
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
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    function createSubjectObj(name, numUsers) {
        const res = {
            subject: name,
            numUsersPerLevels: [],
        }
        for (let i = 1; i <= numUsers.length; i+= 1) {
            res.numUsersPerLevels.push({
                level: i,
                numberUsers: numUsers[i-1],
            })
        }
        return res;
    }

    it('subjects - num users per level - typical 6 subjects', () => {
        cy.server().route({
            url: '/admin/projects/proj1/charts/NumUsersPerSubjectPerLevelChartBuilder',
            status: 200,
            response: [
                createSubjectObj('First Cool Subject', [23,293,493,625,293]),
                createSubjectObj('Awesome Subject', [1265,2352,493,625,293]),
                createSubjectObj('Other Subject', [1254,1000,852,625,293]),
                createSubjectObj('Where subjects no go', [856,293,493,625,293]),
                createSubjectObj('Short', [325,293,493,625,293]),
                createSubjectObj('Interesting Subject', [1568,859,493,625,293]),
            ],
        }).as('getChartData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    it.only('subjects - num users per level - many subjects', () => {
        const response = [];
        for (let i=0; i < 15; i+=1) {
            response.push(createSubjectObj(`Subject # ${i}`, [1265,852,493,625,293]))
        }

        cy.server().route({
            url: '/admin/projects/proj1/charts/NumUsersPerSubjectPerLevelChartBuilder',
            status: 200,
            response,
        }).as('getChartData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    it('subjects - num users per level - many levels', () => {
        const response = [];
        for (let i=0; i < 6; i+=1) {
            response.push(createSubjectObj(`Subject # ${i}`, [1265,852,493,625,293,392,293,983,1923,1209]))
        }

        cy.server().route({
            url: '/admin/projects/proj1/charts/NumUsersPerSubjectPerLevelChartBuilder',
            status: 200,
            response,
        }).as('getChartData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.get('[data-cy=userCountsBySubjectMetric]').get('.b-overlay').should('not.exist');
        cy.contains("Subject # 5")
        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })

    it('subjects - num users per level - empty', () => {
        const response = [];
        cy.server().route({
            url: '/admin/projects/proj1/charts/NumUsersPerSubjectPerLevelChartBuilder',
            status: 200,
            response,
        }).as('getChartData');

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Subjects]').click();
        cy.wait('@getChartData');

        cy.wait(waitForSnap);
        cy.get('[data-cy=userCountsBySubjectMetric]').matchImageSnapshot();
    })


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
