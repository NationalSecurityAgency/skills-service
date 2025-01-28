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
describe('Skill Reuse Modal Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
    });

    it('no destination', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()

        cy.get('[data-cy="destinationList"]')
            .should('not.exist');
        cy.get('[data-cy="reuseButton"]')
          .should('not.exist');
        cy.get('[data-cy="closeButton"]')
          .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="destListPagingControl"]')
            .should('not.exist');
        cy.get('[data-cy="reuseOrMoveDialog"] [data-cy="noContent"]')
            .contains('No Destinations');
    });

    it('1 skill - available to reuse', () => {
        cy.createSubject(1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.be.visible');

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 1 skill.');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('1 skill - already reused', () => {
        cy.createSubject(1, 2);
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('Selected skills can NOT be reused in the Subject 2 subject');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="okButton"]')
            .should('not.exist');
    });

    it('multiple skills - available to reuse', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click();
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('4 skills will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 4 skills.');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('multiple skills - some are already reused', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.reuseSkillIntoAnotherSubject(1, 5, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click();
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have already been reused');

        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 2 skills.');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('multiple skills - some are already reused in the group', () => {
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);
        cy.createSkill(1, 1, 8);
        cy.createSkill(1, 1, 9);
        cy.createSkillsGroup(1, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 6, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 9, 1, 12);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click();
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group12"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be reused in the [Awesome Group 12 Subj1] group.');
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have already been reused');

        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 3 skills.');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('skills with dependencies cannot be reused', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 3);
        cy.addLearningPathItem(1, 3, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click();

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('1 selected skill has other skill dependencies');

        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 1 skill.');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');

        // now none of the skills available for reuse
        cy.get('[data-cy="skillsTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click();

        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('Selected skills can NOT be reused in the Subject 2 subject');
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('1 selected skill has already been reused in that subject');
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('1 selected skill has other skill dependencies');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .should('not.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');

        // validate skill was actually reused
        cy.get(' [data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj2"]')
            .click();
        cy.get('[data-cy="importedBadge-skill3STREUSESKILLST0"]');
    });

    it('skills with dependencies cannot be reused - plural', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.addLearningPathItem(1, 3, 1)
        cy.addLearningPathItem(1, 4, 3)

        cy.reuseSkillIntoAnotherSubject(1, 4, 2);
        cy.reuseSkillIntoAnotherSubject(1, 5, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click();

        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('Selected skills can NOT be reused in the Subject 2 subject');
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have already been reused in that subject');
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have other skill dependencies');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .should('not.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('skills with dependencies cannot be reused into a group - plural', () => {
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.addLearningPathItem(1, 3, 1)
        cy.addLearningPathItem(1, 4, 3)

        cy.createSkillsGroup(1, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 4, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 5, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click();

        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group12"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('Selected skills can NOT be reused in the Awesome Group 12 Subj1 group');
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have already been reused in that group');
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have other skill dependencies');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .should('not.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('cancel modal will focus on the Actions button', () => {
        cy.createSubject(1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()

        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .should('have.focus');

        // close with X on top right
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()

        cy.get('[aria-label="Close"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .should('have.focus');
    });

    it('successful reuse should focus on the New Skill button', () => {
        cy.createSubject(1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();
        cy.get('[data-cy="newSkillButton"]')
            .should('have.focus');
    });

    it('successful reuse from a group should focus on its New Skill button of its parent table', () => {
        cy.createSubject(1, 2);
        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()

        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();
        cy.get('[data-cy="addSkillToGroupBtn-group11"]')
            .should('have.focus');
    });

    it('in a group canceling modal will focus on the Actions button', () => {
        cy.createSubject(1, 2);
        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()

        // cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]').click();
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="closeButton"]').click();
        cy.get('[data-cy="closeButton"]').should('not.exist')
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
          .should('have.focus');
    });

});