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

describe('Group Skill Reuse Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
    });

    it('reuse skill into a group under the same subject', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.createSkillsGroup(1, 1, 11);
        cy.createSkillsGroup(1, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group11"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be reused in the [Awesome Group 11 Subj1] group');
        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 3 skills');
        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="nameCell_group11"] [data-cy="numSkillsInGroup"]')
            .contains('3 skills');

        cy.get('[data-cy="expandDetailsBtn_group11"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="importedBadge-skill1STREUSESKILLST0"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="importedBadge-skill2STREUSESKILLST0"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="importedBadge-skill3STREUSESKILLST0"]');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '6');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '1,200');

        cy.get('[data-cy="manageSkillBtn_skill3STREUSESKILLST0"]')
            .click();
        cy.get('[data-cy="pageHeader"] [data-cy="importedBadge"]')
            .contains('Reused');
    });

    it('reuse skill into a group under a different subject', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.createSkillsGroup(1, 2, 11);
        cy.createSkillsGroup(1, 2, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillReuseBtn"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2group11Subj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be reused in the [Awesome Group 11 Subj2] group');
        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 3 skills');
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj2"]')
            .click();

        cy.get('[data-cy="nameCell_group11Subj2"] [data-cy="numSkillsInGroup"]')
            .contains('3 skills');

        cy.get('[data-cy="expandDetailsBtn_group11Subj2"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11Subj2"] [data-cy="importedBadge-skill1STREUSESKILLST0"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11Subj2"] [data-cy="importedBadge-skill2STREUSESKILLST0"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11Subj2"] [data-cy="importedBadge-skill3STREUSESKILLST0"]');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '3');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '600');
    });

    it('reuse skill from a group into a different group under the different subject', () => {
        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);
        cy.addSkillToGroup(1, 1, 11, 7);

        cy.createSkillsGroup(1, 2, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="expandDetailsBtn_group11"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillReuseBtn"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2group12Subj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be reused in the [Awesome Group 12 Subj2] group');
        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 2 skills');
        cy.get('[data-cy="okButton"]')
            .click();
        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj2"]')
            .click();

        cy.get('[data-cy="nameCell_group12Subj2"] [data-cy="numSkillsInGroup"]')
            .contains('2 skills');

        cy.get('[data-cy="expandDetailsBtn_group12Subj2"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj2"] [data-cy="nameCell_skill6STREUSESKILLST0"]')
            .contains('Reused');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj2"] [data-cy="nameCell_skill7STREUSESKILLST0"]')
            .contains('Reused');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '400');

        const tableSelector = '[data-cy="ChildRowSkillGroupDisplay_group12Subj2"] [data-cy="skillsTable"]';
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj2"] [data-cy="skillsTable-additionalColumns"]')
            .contains('Points')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 3,
                value: 200
            }],
            [{
                colIndex: 3,
                value: 200
            }],
        ], 10, true, null, false);
    });

    it('reuse skill from a group into a different group under the same subject', () => {
        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);
        cy.addSkillToGroup(1, 1, 11, 7);

        cy.createSkillsGroup(1, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="expandDetailsBtn_group11"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="selectAllSkillsBtn"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillReuseBtn"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group12"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.exist');

        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be reused in the [Awesome Group 12 Subj1] group');
        cy.get('[data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 2 skills');
        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="nameCell_group12"] [data-cy="numSkillsInGroup"]')
            .contains('2 skills');

        cy.get('[data-cy="expandDetailsBtn_group12"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="nameCell_skill6STREUSESKILLST0"]')
            .contains('Reused');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="nameCell_skill7STREUSESKILLST0"]')
            .contains('Reused');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '5');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '1,000');

        const tableSelector = '[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="skillsTable"]';
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="skillsTable-additionalColumns"]')
            .contains('Points')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 3,
                value: 200
            }],
            [{
                colIndex: 3,
                value: 200
            }],
        ], 10, true, null, false);

        // validate groups points
        cy.get('[data-cy="expandDetailsBtn_group12"]')
            .click();
        cy.get('[data-cy="expandDetailsBtn_group11"]')
            .click();
        cy.get('[data-cy="skillsTable-additionalColumns"]')
            .contains('Points')
            .click();
        cy.validateTable('[data-cy="skillsTable"]', [
            [{
                colIndex: 3,
                value: 400
            }],
            [{
                colIndex: 3,
                value: 400
            }],
            [{
                colIndex: 3,
                value: 200
            }],
        ], 10, true, null, false);
    });

});