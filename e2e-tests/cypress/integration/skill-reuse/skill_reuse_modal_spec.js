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
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[data-cy="destinationList"]')
            .should('not.exist');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="destListPagingControl"]')
            .should('not.exist');
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .contains('No Destinations');
    });

    it('paging controls are only if there are at least 6 items', () => {
        cy.createSubject(1, 2);
        cy.createSubject(1, 3);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[data-cy="destinationList"] .list-group-item')
            .should('have.length', 2);
        cy.get('[data-cy="destinationList"] [data-cy="destItem-0"]')
            .contains('Subject 2');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-1"]')
            .contains('Subject 3');
        cy.get('[data-cy="destListPagingControl"]')
            .should('not.exist');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.createSubject(1, 4);
        cy.createSubject(1, 5);
        cy.createSubject(1, 6);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[data-cy="destinationList"] .list-group-item')
            .should('have.length', 5);
        cy.get('[data-cy="destinationList"] [data-cy="destItem-0"]')
            .contains('Subject 2');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-1"]')
            .contains('Subject 3');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-2"]')
            .contains('Subject 4');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-3"]')
            .contains('Subject 5');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-4"]')
            .contains('Subject 6');
        cy.get('[data-cy="destListPagingControl"]')
            .should('not.exist');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.createSubject(1, 7);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[data-cy="destinationList"] .list-group-item')
            .should('have.length', 4);
        cy.get('[data-cy="destinationList"] [data-cy="destItem-0"]')
            .contains('Subject 2');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-1"]')
            .contains('Subject 3');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-2"]')
            .contains('Subject 4');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-3"]')
            .contains('Subject 5');
        cy.get('[data-cy="destListPagingControl"]')
            .should('exist');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="destListPagingControl"] [aria-label="Go to page 2"]')
            .click();
        cy.get('[data-cy="destinationList"] .list-group-item')
            .should('have.length', 2);
        cy.get('[data-cy="destinationList"] [data-cy="destItem-0"]')
            .contains('Subject 6');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-1"]')
            .contains('Subject 7');
    });

    it('destination selector paging', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSubject(1, 3);
        cy.createSubject(1, 4);
        cy.createSubject(1, 5);

        cy.createSkillsGroup(1, 1, 11);
        cy.createSkillsGroup(1, 1, 12);
        cy.createSkillsGroup(1, 2, 13);
        cy.createSkillsGroup(1, 3, 14);
        cy.createSkillsGroup(1, 3, 15);
        cy.createSkillsGroup(1, 3, 16);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill3"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[data-cy="destinationList"] .list-group-item')
            .should('have.length', 4);
        cy.get('[data-cy="destinationList"] [data-cy="destItem-0"]')
            .contains('Awesome Group 11 Subj1');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-1"]')
            .contains('Awesome Group 12 Subj1');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-2"]')
            .contains('Subject 2');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-3"]')
            .contains('Awesome Group 13 Subj2');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="destListPagingControl"] [aria-label="Go to page 2"]')
            .click();
        cy.get('[data-cy="destinationList"] .list-group-item')
            .should('have.length', 4);
        cy.get('[data-cy="destinationList"] [data-cy="destItem-0"]')
            .contains('Subject 3');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-1"]')
            .contains('Awesome Group 14 Subj3');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-2"]')
            .contains('Awesome Group 15 Subj3');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-3"]')
            .contains('Awesome Group 16 Subj3');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="destListPagingControl"] [aria-label="Go to page 3"]')
            .click();
        cy.get('[data-cy="destinationList"] .list-group-item')
            .should('have.length', 2);
        cy.get('[data-cy="destinationList"] [data-cy="destItem-0"]')
            .contains('Subject 4');
        cy.get('[data-cy="destinationList"] [data-cy="destItem-1"]')
            .contains('Subject 5');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
    });

    it('1 skill - available to reuse', () => {
        cy.createSubject(1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 1 skill.');
        cy.get('[data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
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
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('Selected skills can NOT be reused in the Subject 2 subject');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
    });

    it('multiple skills - available to reuse', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('4 skills will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 4 skills.');
        cy.get('[data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="okButton"]')
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
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be reused in the [Subject 2] subject.');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have already been reused');

        cy.get('[data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 2 skills.');
        cy.get('[data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="okButton"]')
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
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group12"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be reused in the [Awesome Group 12 Subj1] group.');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have already been reused');

        cy.get('[data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 3 skills.');
        cy.get('[data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('skills with dependencies cannot be reused', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 3);
        cy.assignDep(1, 1, 3);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be reused in the [Subject 2] subject.');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 selected skill has other skill dependencies');

        cy.get('[data-cy="reuseButton"]')
            .should('be.enabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');

        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 1 skill.');
        cy.get('[data-cy="reuseButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-cy="okButton"]')
            .should('be.enabled');

        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');

        // now none of the skills available for reuse
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('Selected skills can NOT be reused in the Subject 2 subject');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 selected skill has already been reused in that subject');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 selected skill has other skill dependencies');
        cy.get('[data-cy="reuseButton"]')
            .should('not.enabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');

        // validate skill was actually reused
        cy.get('[data-cy="breadcrumb-proj1"]')
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
        cy.assignDep(1, 1, 3);
        cy.assignDep(1, 3, 4);

        cy.reuseSkillIntoAnotherSubject(1, 4, 2);
        cy.reuseSkillIntoAnotherSubject(1, 5, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('Selected skills can NOT be reused in the Subject 2 subject');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have already been reused in that subject');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have other skill dependencies');
        cy.get('[data-cy="reuseButton"]')
            .should('not.enabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('skills with dependencies cannot be reused into a group - plural', () => {
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.assignDep(1, 1, 3);
        cy.assignDep(1, 3, 4);

        cy.createSkillsGroup(1, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 4, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 5, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group12"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('Selected skills can NOT be reused in the Awesome Group 12 Subj1 group');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have already been reused in that group');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 selected skills have other skill dependencies');
        cy.get('[data-cy="reuseButton"]')
            .should('not.enabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.enabled');
        cy.get('[data-cy="okButton"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .should('not.exist');
    });

    it('cancel modal will focus on the Clear button', () => {
        cy.createSubject(1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="clearSelectedSkillsBtn"]')
            .should('have.focus');

        // close with X on top right
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();

        cy.get('[aria-label="Close"]')
            .click();
        cy.get('[data-cy="clearSelectedSkillsBtn"]')
            .should('have.focus');
    });

    it('successful reuse should focus on the Select All button', () => {
        cy.createSubject(1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillSelect-skill1"]')
            .click({ force: true });
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .should('have.focus');
    });

});