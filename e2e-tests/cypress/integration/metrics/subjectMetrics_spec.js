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


    it('level breakdown', {
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

})
