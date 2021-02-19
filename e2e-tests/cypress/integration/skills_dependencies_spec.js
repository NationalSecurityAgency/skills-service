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
describe('Skills Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        })
    });

    it('Add Dependency failure', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill2', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill2",
            name: "Skill 2",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.intercept({
            method: 'POST',
            path: '/admin/projects/proj1/skills/skill1/dependency/*',
        }, {
            statusCode: 400,
            body: {errorCode: 'FailedToAssignDependency', explanation: 'Error Adding Dependency'}
        }).as('addDependencyError');

        cy.intercept({
            method: 'GET',
            path: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');

        cy.visit('/Administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill')

        cy.get('div#menu-collapse-control li').contains('Dependencies').click();

        cy.get('.multiselect__tags').click();
        cy.get('.multiselect__tags input').type('{enter}')

        cy.wait('@addDependencyError')
        cy.get('div .alert').contains('Error! Request could not be completed! Error Adding Dependency');
    })

    it('do not allow circular dependencies', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill2', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill2",
            name: "Skill 2",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', '/admin/projects/proj1/skills/skill1/dependency/skill2')

        cy.visit('/Administrator/projects/proj1/subjects/subj1/skills/skill2/dependencies');
        cy.contains('No Dependencies Yet');

        cy.get('.multiselect__tags').click();
        cy.get('.multiselect__tags input').type('Skill 1{enter}')

        cy.contains('Error! Request could not be completed! Discovered circular dependency [proj1:skill2 -> proj1:skill1 -> proj1:skill2]');
    });

    it('add dependencies - then remove via table', () => {
        const numSkills = 5;
        for (let i = 0; i < numSkills; i+=1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: "subj1",
                skillId: `skill${i}`,
                name: `Skill ${i}`,
                pointIncrement: '50',
                numPerformToCompletion: '5'
            });
        }

        cy.visit('/Administrator/projects/proj1/subjects/subj1/skills/skill1/dependencies');
        cy.contains('No Dependencies Yet');

        cy.get('.multiselect__tags').click();
        cy.get('.multiselect__tags input').type('Skill 2{enter}')

        const tableSelector = '[data-cy="simpleSkillsTable"]';
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill 2' }, { colIndex: 1,  value: 'skill2' }],
        ], 5);


        cy.get('.multiselect__tags').click();
        cy.get('.multiselect__tags input').type('Skill 3{enter}')
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill 2' }, { colIndex: 1,  value: 'skill2' }],
            [{ colIndex: 0,  value: 'Skill 3' }, { colIndex: 1,  value: 'skill3' }],
        ], 5);

        cy.get('[data-cy="deleteSkill_skill2"]').click();
        cy.contains('Yes, Please').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill 3' }, { colIndex: 1,  value: 'skill3' }],
        ], 5);

        cy.get('[data-cy="deleteSkill_skill3"]').click();
        cy.contains('Yes, Please').click();
        cy.contains('No Dependencies Yet');
    });


    it('remove dependency after navigating directly to the page', () => {
        const tableSelector = '[data-cy="simpleSkillsTable"]';

        const numSkills = 5;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: "subj1",
                skillId: `skill${i}`,
                name: `Skill ${i}`,
                pointIncrement: '50',
                numPerformToCompletion: '5'
            });
        }

        cy.request('POST', '/admin/projects/proj1/skills/skill1/dependency/skill2')
        cy.request('POST', '/admin/projects/proj1/skills/skill1/dependency/skill3')
        cy.request('POST', '/admin/projects/proj1/skills/skill1/dependency/skill4')

        cy.visit('/Administrator/projects/proj1/subjects/subj1/skills/skill1/dependencies');
        cy.get(`${tableSelector} th`).contains('Skill ID').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'skill2' }],
            [{ colIndex: 1,  value: 'skill3' }],
            [{ colIndex: 1,  value: 'skill4' }],
        ], 5);

        cy.get('[data-cy="deleteSkill_skill3"]').click();
        cy.contains('Yes, Please').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'skill2' }],
            [{ colIndex: 1,  value: 'skill4' }],
        ], 5);
    });

    it('ability to navigate to skills that this skill depends on', () => {
        const tableSelector = '[data-cy="simpleSkillsTable"]';

        const numSkills = 5;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: "subj1",
                skillId: `skill${i}`,
                name: `Skill ${i}`,
                pointIncrement: '50',
                numPerformToCompletion: '5'
            });
        }

        cy.request('POST', '/admin/projects/proj1/skills/skill1/dependency/skill2')
        cy.request('POST', '/admin/projects/proj1/skills/skill1/dependency/skill3')
        cy.request('POST', '/admin/projects/proj1/skills/skill1/dependency/skill4')

        cy.visit('/Administrator/projects/proj1/subjects/subj1/skills/skill1/dependencies');
        cy.get('[data-cy="manage_skill3"]').click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill3');
    });


});
