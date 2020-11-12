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

    const waitForSnap = 4000;

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it ('stat cards with zero activity', () => {
        cy.server()
            .route('/admin/projects/proj1/metrics/singleSkillCountsChartBuilder?skillId=skill1')
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 2;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '2',
            });
        }

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.get('[data-cy=numUserAchievedStatCard] [data-cy=statCardValue]').contains('0');
        cy.get('[data-cy=inProgressStatCard] [data-cy=statCardValue]').contains('0');
        cy.get('[data-cy=lastAchievedStatCard] [data-cy=statCardValue]').contains('Never');
    });

    it('stat cards have data', () => {
        cy.server()
            .route('/admin/projects/proj1/metrics/singleSkillCountsChartBuilder**')
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 2;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '2',
            });
        }
        ;

        const m = moment.utc().subtract(2, 'months');
        const numUsers = 5;
        for (let userCounter = 1; userCounter <= numUsers; userCounter += 1) {
            cy.request('POST', `/api/projects/proj1/skills/skill1`,
                {
                    userId: `user${userCounter}achieved@skills.org`,
                    timestamp: m.clone()
                        .subtract(0, 'day')
                        .format('x')
                });
            cy.request('POST', `/api/projects/proj1/skills/skill1`,
                {
                    userId: `user${userCounter}achieved@skills.org`,
                    timestamp: m.clone()
                        .subtract(1, 'day')
                        .format('x')
                });
        }

        const numUsersInProgress = 3;
        for (let userCounter = 1; userCounter <= numUsersInProgress; userCounter += 1) {
            cy.request('POST', `/api/projects/proj1/skills/skill1`,
                {
                    userId: `user${userCounter}progress@skills.org`,
                    timestamp: m.clone()
                        .subtract(0, 'day')
                        .format('x')
                });
        }

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.get('[data-cy=numUserAchievedStatCard] [data-cy=statCardValue]').contains('5');
        cy.get('[data-cy=inProgressStatCard] [data-cy=statCardValue]').contains('3');
        cy.get('[data-cy=lastAchievedStatCard] [data-cy=statCardValue]').contains('2 months ago');
    });

    it('stat cards with large counts', () => {
        const m = moment.utc().subtract(5, 'years');
        const timestamp =  m.format('x');
        cy.log(timestamp);
        cy.server()
            .route({
                url: '/admin/projects/proj1/metrics/singleSkillCountsChartBuilder**',
                status: 200,
                response: {
                    'numUsersAchieved': 3828283,
                    'lastAchieved': parseInt(timestamp),
                    'numUsersInProgress': 5817714
                },
            })
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
        });

        const numSkills = 1;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '1000',
                numPerformToCompletion: '2',
            });
        }

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.get('[data-cy=numUserAchievedStatCard] [data-cy=statCardValue]').contains('3,828,283');
        cy.get('[data-cy=numUserAchievedStatCard] [data-cy=statCardDescription]').contains('Number of users that achieved this skill ');

        cy.get('[data-cy=inProgressStatCard] [data-cy=statCardValue]').contains('5,817,714');
        cy.get('[data-cy=inProgressStatCard] [data-cy=statCardDescription]').contains('Number of Users with some points earned toward the skill');

        cy.get('[data-cy=lastAchievedStatCard] [data-cy=statCardValue]').contains('5 years ago');
        cy.get('[data-cy=lastAchievedStatCard] [data-cy=statCardDescription]').contains(`This skill was last achieved on ${m.format('YYYY-MM-DD HH:mm')}`);
    });

    it('number of users over time', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.server()
            .route('/admin/projects/proj1/metrics/numUserAchievedOverTimeChartBuilder**')
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
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
        const numDays = 6;
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

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.wait(waitForSnap);
        cy.get('[data-cy=numUsersAchievedOverTimeMetric]').matchImageSnapshot();
    });

    it('skill metrics - empty', () => {
        cy.server()
            .route('/admin/projects/proj1/metrics/numUserAchievedOverTimeChartBuilder?skillId=skill1')
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
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

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.get('[data-cy=numUsersAchievedOverTimeMetric]').contains('This chart needs at least 1 day of user activity');

        cy.get('[data-cy=appliedSkillEventsOverTimeMetric]').contains('This chart needs at least 2 days of user activity');
    });

    it('number of users over time - 1 day', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.server()
            .route({
                url: '/admin/projects/proj1/metrics/numUserAchievedOverTimeChartBuilder?skillId=skill1',
                response: {
                    'achievementCounts': [{
                        'num': 1,
                        'timestamp': 1599130800000
                    }]
                },
            })
            .as('singleSkillCountsChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
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

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@singleSkillCountsChartBuilder');

        cy.wait(waitForSnap);
        cy.get('[data-cy=numUsersAchievedOverTimeMetric]').matchImageSnapshot();
    });

    it('applied skill events over time', {
        retries: {
            runMode: 0,
            openMode: 0
        }
    },() => {
        cy.server()
            .route('/admin/projects/proj1/metrics/numUserAchievedOverTimeChartBuilder**')
            .as('skillEventsOverTimeChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
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

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        const numDays = 3;
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            const numUsers = ( dayCounter % 2 == 0) ? 2 : 4
            for (let userCounter = 1; userCounter <= numUsers; userCounter += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `user${dayCounter}-${userCounter}achieved@skills.org`,
                        timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@skillEventsOverTimeChartBuilder');

        cy.wait(waitForSnap);
        cy.get('[data-cy=appliedSkillEventsOverTimeMetric]').matchImageSnapshot();
    });

    it('applied skill events over time - 1 skill', () => {
        cy.server()
            .route('/admin/projects/proj1/metrics/skillEventsOverTimeChartBuilder**')
            .as('skillEventsOverTimeChartBuilder');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Interesting Subject 1',
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

        const m = moment.utc('2020-09-02 11', 'YYYY-MM-DD HH');
        const numDays = 1;
        for (let dayCounter = 1; dayCounter <= numDays; dayCounter += 1) {
            const numUsers = ( dayCounter % 2 == 0) ? 2 : 4
            for (let userCounter = 1; userCounter <= numUsers; userCounter += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`,
                    {
                        userId: `user${dayCounter}-${userCounter}achieved@skills.org`,
                        timestamp: m.clone()
                            .add(dayCounter, 'day')
                            .format('x')
                    });
            }
        }

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.clickNav('Metrics');
        cy.wait('@skillEventsOverTimeChartBuilder');

        cy.get('[data-cy=appliedSkillEventsOverTimeMetric]').contains('This chart needs at least 2 days of user activity');
    });

})
