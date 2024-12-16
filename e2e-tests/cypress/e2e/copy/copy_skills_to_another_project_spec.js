/*
 * Copyright 2024 SkillTree
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
describe('Copy skills from one project to another Tests', () => {
    const tableSelector = '[data-cy="skillsTable"]'

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);

        cy.createProject(2)
        cy.createSubject(2, 1)

        Cypress.Commands.add("initiateSkillsCopyModal", (skillIndexesToCopy) => {
            skillIndexesToCopy.forEach((index) => {
                cy.get(`${tableSelector} [data-p-index="${index}"] [data-pc-name="rowcheckbox"] [data-pc-section="input"]`).click()
            })
            cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]').should('have.text', skillIndexesToCopy.length)
            cy.get('[data-cy="skillActionsBtn"]').click()
            cy.get('[data-cy="skillsActionsMenu"] [aria-label="Copy to another Project"]').click()

            cy.get('[data-pc-name="dialog"] [data-pc-section="title"]').should('have.text', 'Copy Selected Skills To Another Project')
        });

    });

    it('copy skills to another project', () => {
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsCopyModal([1,2,4])

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.exist')
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 1. select project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 2. select subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1').click();

        cy.get('[data-cy="validationPassedMsg"]').contains('Validation Passed! 3 skill(s) are eligible to be copied to This is project 2 project')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="closeDialogBtn"]').contains('Close')
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="newSkillButton"]').should('have.focus')
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]').should('have.text', '0')

        // project 2
        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill3"]')
        cy.get('[data-cy="manageSkillLink_skill4"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');
    });


});