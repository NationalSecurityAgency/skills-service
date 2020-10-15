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

        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: "Some other subject",
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



})
