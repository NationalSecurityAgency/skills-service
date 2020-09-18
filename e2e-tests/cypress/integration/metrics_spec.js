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
describe('Metrics Tests', () => {
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


    it.only('achievements table', () => {
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

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user0Good@skills.org', timestamp: new Date().getTime()})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user0Good@skills.org', timestamp: new Date().getTime()})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user0Good@skills.org', timestamp: new Date().getTime()})

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'user1Long0@skills.org', timestamp: new Date().getTime()})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user1Long0@skills.org', timestamp: new Date().getTime()})

        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: 'user2Smith0@skills.org', timestamp: new Date().getTime()})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: 'user2Smith0@skills.org', timestamp: new Date().getTime()})

        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();
    })

    it('overall levels - empty', () => {
        cy.visit('/projects/proj1/');
        cy.clickNav('Metrics');
        cy.get('[data-cy=metricsNav-Achievements]').click();

        cy.contains("Level 5: 0 users");
        // we have to wait for background animation to complete
        cy.wait(2000);
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
        cy.wait(2000);
        cy.get('[data-cy=projectOverallLevelsChart]').matchImageSnapshot('projectOverallLevelsChart-allLevels');
    });

})
