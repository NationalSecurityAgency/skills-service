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

    it('skills page metrics', () => {
        cy.server()
            .route('/admin/projects/proj1/charts/singleSkillCountsChartBuilder**')
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

})
