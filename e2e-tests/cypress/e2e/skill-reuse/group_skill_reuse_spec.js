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

        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill3"]')

        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group11"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be reused in the [Awesome Group 11 Subj1] group');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
          .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 3 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
          .click();

        cy.get('[data-cy="nameCell_group11"] [data-cy="numSkillsInGroup"]')
            .contains('3 skills');

        cy.get(`[data-p-index="1"] [data-pc-section="rowtogglebutton"]`).first().click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="importedBadge-skill1STREUSESKILLST0"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="importedBadge-skill2STREUSESKILLST0"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="importedBadge-skill3STREUSESKILLST0"]');

        // original skills remain since they are reused and not moved
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill3"]')

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '3');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('have.text', '3');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '600');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('have.text', '600');

        cy.get('[data-cy="manageSkillLink_skill3STREUSESKILLST0"]')
            .click();
        cy.get('[data-cy="pageHeader"] [data-cy="importedBadge"]')
            .contains('Reused');
    });

    it('1 skill - already reused', () => {
        cy.createSkillsGroup(1, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 1, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
            .should('be.visible');
        cy.get('[data-cy="okButton"]')
            .should('not.visible');

        // test all skills have been reused in a subject
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group12"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('Selected skills can NOT be reused in the Awesome Group 12 Subj1 group');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 selected skill has already been reused in that group');
        cy.get('[data-cy="reuseButton"]')
            .should('be.disabled');
        cy.get('[data-cy="closeButton"]')
          .should('be.visible');
        cy.get('[data-cy="okButton"]')
          .should('not.visible');

    });

    it('reuse skill into a group under a different subject', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.createSkillsGroup(1, 2, 11);
        cy.createSkillsGroup(1, 2, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill3"]')

        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"')
          .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2group11Subj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be reused in the [Awesome Group 11 Subj2] group');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 3 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill3"]')

        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj2"]')
            .click();

        cy.get('[data-cy="nameCell_group11Subj2"] [data-cy="numSkillsInGroup"]')
            .contains('3 skills');

        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11Subj2"] [data-cy="importedBadge-skill1STREUSESKILLST0"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11Subj2"] [data-cy="importedBadge-skill2STREUSESKILLST0"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11Subj2"] [data-cy="importedBadge-skill3STREUSESKILLST0"]');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('have.text', '3');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('have.text', '600');
    });

    it('reuse skill from a group into a different group under the different subject', () => {
        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);
        cy.addSkillToGroup(1, 1, 11, 7);

        cy.createSkillsGroup(1, 2, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]')
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]')

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2group12Subj2"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be reused in the [Awesome Group 12 Subj2] group');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 2 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]')
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]')

        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj2"]')
            .click();

        cy.get('[data-cy="nameCell_group12Subj2"] [data-cy="numSkillsInGroup"]')
            .contains('2 skills');

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj2"] [data-cy="nameCell_skill6STREUSESKILLST0"]')
            .contains('Reused');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj2"] [data-cy="nameCell_skill7STREUSESKILLST0"]')
            .contains('Reused');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('have.text', '400');

        const tableSelector = '[data-cy="ChildRowSkillGroupDisplay_group12Subj2"] [data-cy="skillsTable"]';
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj2"] [data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj2"] [data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 5,
                value: 200
            }],
            [{
                colIndex: 5,
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
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]')
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]')

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"]')
            .click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group12"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be reused in the [Awesome Group 12 Subj1] group');
        cy.get('[ data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 2 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="nameCell_group12"] [data-cy="numSkillsInGroup"]')
            .contains('2 skills');

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]')
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]')

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).first().click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="nameCell_skill6STREUSESKILLST0"]')
            .contains('Reused');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="nameCell_skill7STREUSESKILLST0"]')
            .contains('Reused');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '3');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '600');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('have.text', '400');

        const tableSelector = '[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="skillsTable"]';
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]`).click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]`).click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 5,
                value: 200
            }],
            [{
                colIndex: 5,
                value: 200
            }],
        ], 10, true, null, false);

        // validate groups points
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).first().click()
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]`).first().click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()

        cy.validateTable('[data-cy="skillsTable"]', [
            [{
                colIndex: 5,
                value: 400
            }],
            [{
                colIndex: 5,
                value: 400
            }],
            [{
                colIndex: 5,
                value: 200
            }],
        ], 10, true, null, false);
    });

    it('remove the reused skill', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.createSkillsGroup(1, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 1, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('have.text', '200');

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.openDialog('[data-cy="deleteSkillButton_skill1STREUSESKILLST1"]')
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('this action will only remove the reused skill');
        cy.get('[data-cy="currentValidationText"]')
            .type('Delete Me', {delay: 0});
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="noContent"]')
            .contains('Group has no Skills');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('not.exist');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('not.exist');
        cy.get('[data-cy="manageSkillLink_skill1"]');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="noContent"]')
            .contains('Group has no Skills');
        cy.get('[data-cy="manageSkillLink_skill1"]');
    });

    it('remove the original skill', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.createSkillsGroup(1, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 1, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.openDialog('[data-cy="deleteSkillButton_skill1"]')
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('Deleting this skill will also remove its reused copies');
        cy.get('[data-cy="currentValidationText"]')
            .type('Delete Me', {delay: 0});
        cy.clickSaveDialogBtn()
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="noContent"]')
            .contains('Group has no Skills');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="manageSkillLink_skill1"]')
            .should('not.exist');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12"] [data-cy="noContent"]')
            .contains('Group has no Skills');
        cy.get('[data-cy="manageSkillLink_skill1"]')
            .should('not.exist');
    });

    it('remove warning when skill is reused and imported', () => {
        cy.exportSkillToCatalog(1, 1, 1);
        cy.createSkillsGroup(1, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 1, 1, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="deleteSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('This will PERMANENTLY remove [Very Great Skill 1] Skill from the catalog');
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('Deleting this skill will also remove its reused copies');
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This skill is currently imported by 0 projects')
    });

});