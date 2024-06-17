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
import dayjs from 'dayjs';

const moment = require('moment-timezone');

describe('Skill Reuse Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
    });

    it('skill is reused in another subject', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj2');
        cy.get('[data-cy="importedBadge-skill1STREUSESKILLST0"]')
            .contains('Reused');
        cy.get('[data-cy="nameCell_skill1STREUSESKILLST0"] [data-cy="highlightedValue"]')
            .should('have.text', 'Very Great Skill 1');

        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
        cy.get('[data-cy="childRowDisplay_skill1STREUSESKILLST0"] [data-cy="reusedAlert"]');

        // navigate down to a skill page
        cy.get('[data-cy="copySkillButton_skill1STREUSESKILLST0]').should('not.exist')
        cy.get('[data-cy="editSkillButton_skill1STREUSESKILLST0"]').should('not.exist')
        cy.get('[data-cy="manageSkillLink_skill1STREUSESKILLST0"]').click()

        cy.get('[data-cy="pageHeader"] [data-cy="importedBadge"]')
            .contains('Reused');
        cy.get('[data-cy="pageHeader"]')
            .contains('SKILL: Very Great Skill 1');
        cy.get('[data-cy="pageHeader"] [data-cy="skillId"] [data-cy="smtText"]')
            .should('have.text', 'ID: skill1');
        cy.get('[data-cy="childRowDisplay_skill1STREUSESKILLST0"] [data-cy="reusedAlert"]');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj2/skills/skill1STREUSESKILLST0/');
        cy.get('[data-cy="pageHeader"] [data-cy="importedBadge"]')
            .contains('Reused');
        cy.get('[data-cy="pageHeader"]')
            .contains('SKILL: Very Great Skill 1');
        cy.get('[data-cy="pageHeader"] [data-cy="skillId"] [data-cy="smtText"]')
            .should('have.text', 'ID: skill1');
        cy.get('[data-cy="childRowDisplay_skill1STREUSESKILLST0"] [data-cy="reusedAlert"]');
    });

    it('ability to navigate to the original skill from the reused skill', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj2');
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').contains('ID: subj2')
        cy.get('[data-cy="manageSkillLink_skill1STREUSESKILLST0"]')
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
        cy.get('[data-cy="childRowDisplay_skill1STREUSESKILLST0"] [data-cy="reusedAlert"]').contains('Original Skill')
        cy.get('[data-cy="childRowDisplay_skill1STREUSESKILLST0"] [data-cy="reusedAlert"] [data-cy="linkToTheOriginalSkill"]')
            .click();
        cy.get('[data-cy="breadcrumb-subj1"]')
            .contains('subj1');
        cy.get('[data-cy="pageHeader"] [data-cy="skillId"] [data-cy="smtText"]')
            .should('have.text', 'ID: skill1');
        cy.get('[data-cy="skillOverviewTotalpoints"] [data-cy="mediaInfoCardTitle"]').should('have.text', '200 Points')

        // from the skill page by navigating down to the skill page
        cy.visit('/administrator/projects/proj1/subjects/subj2');
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').contains('ID: subj2')
        cy.get('[data-cy="manageSkillLink_skill1STREUSESKILLST0"]').click();
        cy.get('[data-cy="breadcrumb-skill1STREUSESKILLST0"]')
        cy.get('[data-cy="childRowDisplay_skill1STREUSESKILLST0"] [data-cy="reusedAlert"]').contains('Original Skill')
        cy.get('[data-cy="childRowDisplay_skill1STREUSESKILLST0"] [data-cy="reusedAlert"] [data-cy="linkToTheOriginalSkill"]')
            .click();
        cy.get('[data-cy="breadcrumb-subj1"]')
            .contains('subj1');
        cy.get('[data-cy="pageHeader"] [data-cy="skillId"] [data-cy="smtText"]')
            .should('have.text', 'ID: skill1');

        // from the skill page directly
        cy.visit('/administrator/projects/proj1/subjects/subj2/skills/skill1STREUSESKILLST0/');
        cy.get('[data-cy="childRowDisplay_skill1STREUSESKILLST0"] [data-cy="reusedAlert"]').contains('Original Skill')
        cy.get('[data-cy="childRowDisplay_skill1STREUSESKILLST0"] [data-cy="reusedAlert"] [data-cy="linkToTheOriginalSkill"]')
            .click();
        cy.get('[data-cy="breadcrumb-subj1"]')
            .contains('subj1');
        cy.get('[data-cy="pageHeader"] [data-cy="skillId"] [data-cy="smtText"]')
            .should('have.text', 'ID: skill1');
    });

    it('search reused skills', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.reuseSkillIntoAnotherSubject(1, 2, 2);
        cy.reuseSkillIntoAnotherSubject(1, 3, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj2');
        cy.get('[data-cy="manageSkillLink_skill1STREUSESKILLST0"]');
        cy.get('[data-cy="manageSkillLink_skill2STREUSESKILLST0"]');
        cy.get('[data-cy="manageSkillLink_skill3STREUSESKILLST0"]');

        cy.get('[data-cy="skillsTable-skillFilter"]')
            .type('skill 1');
        cy.get('[data-cy="manageSkillLink_skill1STREUSESKILLST0"]');
        cy.get('[data-cy="manageSkillLink_skill2STREUSESKILLST0"]')
            .should('not.exist');
        cy.get('[data-cy="manageSkillLink_skill3STREUSESKILLST0"]')
            .should('not.exist');

        cy.get('[data-cy="skillsTable-skillFilter"]')
            .clear()
            .type('Skill 2');
        cy.get('[data-cy="manageSkillLink_skill1STREUSESKILLST0"]')
            .should('not.exist');
        cy.get('[data-cy="manageSkillLink_skill2STREUSESKILLST0"]');
        cy.get('[data-cy="manageSkillLink_skill3STREUSESKILLST0"]')
            .should('not.exist');
    });

    it('reuse skill into a subject', () => {
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
        cy.get('[data-p-index="2"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj3"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be reused in the [Subject 3] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 1 skill.');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj3"]')
            .click();
        cy.get('[data-cy="importedBadge-skill3STREUSESKILLST0"]')
            .contains('Reused');
    });

    it('remove the reused skill', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj2');
        cy.get('[data-cy="deleteSkillButton_skill1STREUSESKILLST0"]')
            .click();
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('this action will only remove the reused skill');
        cy.get('[data-cy="currentValidationText"]')
            .type('Delete Me');
        cy.get('[data-cy="saveDialogBtn"]')
            .click();
        cy.get('[data-cy="noContent"]')
            .contains('No Skills Yet');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '0');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="deleteSkillButton_skill1"]')
            .click();
        cy.contains('This will remove Very Great Skill 1');
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('Deleting this skill will also remove its reused copies')
            .should('not.exist');
    });

    it('remove the original skill', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="deleteSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('Deleting this skill will also remove its reused copies');
        cy.get('[data-cy="currentValidationText"]')
            .type('Delete Me');
        cy.get('[data-cy="saveDialogBtn"]')
            .click();
        cy.get('[data-cy="noContent"]')
            .contains('No Skills Yet');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '0');

        cy.visit('/administrator/projects/proj1/subjects/subj2');
        cy.get('[data-cy="noContent"]')
            .contains('No Skills Yet');
    });

    it('remove warning when skill is reused and imported', () => {
        cy.exportSkillToCatalog(1, 1, 1);
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="deleteSkillButton_skill1"]')
            .click();
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('This will PERMANENTLY remove [Very Great Skill 1] Skill from the catalog');
        cy.get('[data-cy="removalSafetyCheckMsg"]')
            .contains('Deleting this skill will also remove its reused copies');
    });

});
