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
describe('Copy Project Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
    });

    it('copy project', () => {
        cy.createProject(2); // another project in the mix

        cy.visit('/administrator/');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.get('[data-cy="saveProjectButton"]')
            .should('be.enabled');
        cy.get('[data-cy="saveProjectButton"]')
            .should('have.text', 'Copy Project');
        cy.get('[data-cy="saveProjectButton"]')
            .click();
        cy.get('[data-cy="lengthyOpModal"] [data-cy="successMessage"]')
            .contains('Project\'s training profile was successfully copied');
        cy.get('[data-cy="allDoneBtn"]')
            .click();
        cy.get('[id="projNewProject"]')
            .should('have.focus');

        // now there are 3 projects
        cy.get('[data-cy="projCard_proj1_manageBtn"]');
        cy.get('[data-cy="projCard_proj2_manageBtn"]');
        cy.get('[data-cy="projCard_NewProject_manageBtn"]');

        // validate new project stats
        cy.get('[data-cy="projectCard_NewProject"] [data-cy="pagePreviewCardStat_Subjects"] [data-cy="statNum"]')
            .should('have.text', '2');
        cy.get('[data-cy="projectCard_NewProject"] [data-cy="pagePreviewCardStat_Skills"] [data-cy="statNum"]')
            .should('have.text', '2');
        cy.get('[data-cy="projectCard_NewProject"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .should('have.text', '400');
        cy.get('[data-cy="projectCard_NewProject"] [data-cy="pagePreviewCardStat_Badges"] [data-cy="statNum"]')
            .should('have.text', '0');

        // navigate to new project
        cy.get('[data-cy="projCard_NewProject_manageBtn"]')
            .click();
        cy.get('[data-cy="manageBtn_subj2"]');
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');

        // refresh and verify that project still good
        cy.visit('/administrator/');
        cy.get('[data-cy="projCard_proj1_manageBtn"]');
        cy.get('[data-cy="projCard_proj2_manageBtn"]');
        cy.get('[data-cy="projCard_NewProject_manageBtn"]');

        // validate new project stats
        cy.get('[data-cy="projectCard_NewProject"] [data-cy="pagePreviewCardStat_Subjects"] [data-cy="statNum"]')
            .should('have.text', '2');
        cy.get('[data-cy="projectCard_NewProject"] [data-cy="pagePreviewCardStat_Skills"] [data-cy="statNum"]')
            .should('have.text', '2');
        cy.get('[data-cy="projectCard_NewProject"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .should('have.text', '400');
        cy.get('[data-cy="projectCard_NewProject"] [data-cy="pagePreviewCardStat_Badges"] [data-cy="statNum"]')
            .should('have.text', '0');

        // navigate to new project
        cy.get('[data-cy="projCard_NewProject_manageBtn"]')
            .click();
        cy.get('[data-cy="manageBtn_subj2"]');
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
    });

    it('focus is returned after modal close button is clicked', () => {
        cy.createProject(2); // another project in the mix

        cy.visit('/administrator/');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.get('[data-cy="saveProjectButton"]')
            .click();
        cy.get('[data-cy="allDoneBtn"]')
            .should('exist');
        cy.get('.modal-content [aria-label="Close"]')
            .click();
        cy.get('[id="projNewProject"]')
            .should('have.focus');
    });

    it('canceling copy modal should return focus to the copy button', () => {
        cy.createProject(2); // another project in the mix

        cy.visit('/administrator/');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('[data-cy="closeProjectButton"]')
            .click();
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .should('have.focus');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('.modal-header [aria-label="Close"]')
            .click();
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .should('have.focus');
    });

    it('validation: duplicate project name is not allowed', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('This is project 1');
        cy.get('[data-cy="projectNameError"]')
            .contains('The value for the Project Name is already taken');
        cy.get('[data-cy="saveProjectButton"]')
            .should('be.disabled');
    });

    it('projects table: copy project', () => {
        for (let i = 2; i <= 18; i += 1) {
            cy.createProject(i);
        }
        cy.createProject(19);
        cy.createSubject(19, 1);
        cy.createSubject(19, 2);
        cy.createSkill(19, 1, 1);
        cy.createSkill(19, 1, 2);
        cy.createProject(20);
        cy.visit('/administrator/');
        cy.get('[data-cy="copyProjectIdproj19"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.get('[data-cy="saveProjectButton"]')
            .should('be.enabled');
        cy.get('[data-cy="saveProjectButton"]')
            .should('have.text', 'Copy Project');
        cy.get('[data-cy="saveProjectButton"]')
            .click();
        cy.get('[data-cy="allDoneBtn"]')
            .click();
        cy.get('[id="projNewProject"]')
            .should('have.focus');

        cy.get('[data-cy="manageProjBtn_NewProject"]')
            .click();
        cy.get('[data-cy="manageBtn_subj2"]');
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
    });

    it('project copy cause view to switch to a table', () => {
        for (let i = 2; i <= 9; i += 1) {
            cy.createProject(i);
        }
        cy.visit('/administrator/');
        // verify that cards are shown
        cy.get('[data-cy="projectCard_proj1"]');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.get('[data-cy="saveProjectButton"]')
            .click();
        cy.get('[data-cy="lengthyOpModal"] [data-cy="successMessage"]')
            .contains('Project\'s training profile was successfully copied');
        cy.get('[data-cy="allDoneBtn"]')
            .click();
        cy.get('[id="projNewProject"]')
            .should('have.focus');

        // verify it is now table view
        cy.get('[data-cy="projCell_NewProject"]');
    });

    it('project table: canceling copy modal should return focus to the copy button', () => {
        for (let i = 2; i <= 20; i += 1) {
            cy.createProject(i);
        }
        cy.visit('/administrator/');
        cy.get('[data-cy="copyProjectIdproj19"]')
            .click();
        cy.get('[data-cy="closeProjectButton"]')
            .click();
        cy.get('[data-cy="copyProjectIdproj19"]')
            .should('have.focus');

        cy.get('[data-cy="copyProjectIdproj19"]')
            .click();
        cy.get('.modal-header [aria-label="Close"]')
            .click();
        cy.get('[data-cy="copyProjectIdproj19"]')
            .should('have.focus');
    });

    it('pressing enter when enable skill id help icon is selected does not prevent saving of copied project', () => {
        cy.createProject(3);
        cy.intercept('POST', '/admin/projects/proj3/copy').as('copyProject');
        cy.intercept('GET', '/app/projects').as('loadProjects');
        cy.visit('/administrator/');
        cy.get('[data-cy="copyProjBtn"]').eq(1)
            .click();
        cy.contains('Copy Project').should('be.visible');
        cy.get('[data-cy="projectName"]').type('Lorem Ipsum');
        cy.get('[data-cy="idInputValue"]').should('have.value', 'LoremIpsum')

        cy.realPress('Tab');
        cy.realPress('Tab');
        cy.contains('Enable to override auto-generated value').should('be.visible');
        cy.get('[data-cy="saveProjectButton"]').should('be.enabled')

        cy.realPress('Enter');
        cy.contains('Copying Project').should('be.visible');
        cy.get('[data-cy="allDoneBtn"]').click();
        cy.wait('@copyProject');
        cy.wait('@loadProjects');
        cy.get('[data-cy="projCard_LoremIpsum_manageLink"]').should('be.visible');
    })

});