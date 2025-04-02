/*
 * Copyright 2025 SkillTree
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

describe('Skill Form Tests', () => {

    it('Form content saved on back button press', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.openNewSkillDialog();
        cy.get('[data-cy="skillName"]').type('testname')

        cy.go('back');
        cy.go('forward');

        cy.openNewSkillDialog();
        cy.get('[data-cy="contentRestoredMessage"]').contains('Form\'s values have been restored from backup.');
        cy.get('[data-cy="skillName"]').should('have.value','testname');
    })

    it('save button is disable for validation and saving of the skill', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.intercept('POST', '/api/validation/name').as('validateName')
        cy.intercept('POST', '/admin/projects/proj1/skillNameExists').as('nameExists')
        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/testnameSkill', (req) => {
            req.continue(res => {
                res.delay = 6000;
                res.send();
            });
        }).as('saveSkill')

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.openNewSkillDialog();
        cy.get('[data-cy="skillName"]').type('testname')
        cy.wait('@validateName')
        cy.wait('@nameExists')
        cy.wait(2000)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@validateName')
        cy.wait('@nameExists')
        cy.wait(1000)
        cy.get('[data-cy="saveDialogBtn"]', {timeout:2000} ).should('be.disabled')
        cy.get('[data-cy="closeDialogBtn"]', {timeout:2000} ).should('be.disabled')
        cy.wait('@saveSkill')

        cy.get('[data-cy="skillName"]').should('not.exist')
    })
})