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

describe('Test persistence of table sorting', () => {
    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
    });

    it('Verify skills table sort remains sorted after navigating away', () => {

        const numSkills = 13;
        const expected = [];
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            expected.push([{ colIndex: 2,  value: skillName }, { colIndex: 3,  value: skillsCounter }])
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter < 3 ? '1' : '200',
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // test skill name sorting
        cy.get('[data-cy="skillsTable"]').contains('Skill').click();
        cy.validateTable('[data-cy="skillsTable"]', expected, 10);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.validateTable('[data-cy="skillsTable"]', expected, 10);
        cy.get('[data-cy="skillsTable"]').contains('Skill').click();
        cy.validateTable('[data-cy="skillsTable"]', expected.map((item) => item).reverse(), 10);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.validateTable('[data-cy="skillsTable"]', expected.map((item) => item).reverse(), 10);
    });

    it('Verify skills table sort remains sorted after navigating away and sorting another table', () => {

        const numSkills = 13;
        const expected = [];
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            expected.push([{ colIndex: 2,  value: skillName }, { colIndex: 3,  value: skillsCounter }])
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter < 3 ? '1' : '200',
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // test skill name sorting
        cy.get('[data-cy="skillsTable"]').contains('Skill').click();
        cy.validateTable('[data-cy="skillsTable"]', expected, 10);

        cy.visit('/administrator/projects/proj1/users');
        cy.get('[data-cy="usersTable"]').contains('Progress').click();

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.validateTable('[data-cy="skillsTable"]', expected, 10);

    });
});