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

import '../desc-attachments/desc-attachment-commands'

describe('Copy Project Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });
    });

    it('copy project', () => {
        cy.createProject(2); // another project in the mix

        cy.visit('/administrator/');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('have.text', 'Copy Project');
        cy.clickSaveDialogBtn()
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
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="allDoneBtn"]')
            .should('exist');
        cy.get('[data-cy="allDoneBtn"]')
            .click();
        cy.get('[id="projNewProject"]')
            .should('have.focus');
    });

    it('canceling copy modal should return focus to the copy button', () => {
        cy.createProject(2); // another project in the mix

        cy.visit('/administrator/');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').should('have.focus');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
        cy.get('.p-dialog-header [aria-label="Close"]').click();
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').should('have.focus');
    });

    it('validation: duplicate project name is not allowed', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('This is project 1');
        cy.get('[data-cy="projectNameError"]')
            .contains('Project Name already exists');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
    });

    it('copy project gracefully handles errors when an attachment is missing in skill description', () => {
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2');

        cy.get('[data-cy="editSkillButton_skill2"]').click()

        cy.addAttachment('[data-cy="markdownEditorInput"] button.attachment-button')
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')
        cy.validateAttachmentInDb('project_id', 'proj1')

        cy.execSql(`delete from attachments where project_id='proj1'`, true)

        cy.visit('/administrator/')

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
        cy.get('[data-cy="projectName"]').type('New Project');
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('The skill with ID skill2 has a missing or invalid attachment.');
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('Please update the skill\'s description to resolve the issue, then try copying the project again.')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').contains('skill2')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSkillLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="skillId"]').contains('ID: skill2')
    });

    it('copy project gracefully handles errors when an attachment is missing in subject description', () => {
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="btn_edit-subject"]').click()

        cy.addAttachment('[data-cy="markdownEditorInput"] button.attachment-button')
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')
        cy.validateAttachmentInDb('project_id', 'proj1')

        cy.execSql(`delete from attachments where project_id='proj1'`, true)

        cy.visit('/administrator/')

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
        cy.get('[data-cy="projectName"]').type('New Project');
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('The subject with ID subj1 has a missing or invalid attachment.');
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('Please update the subject\'s description to resolve the issue, then try copying the project again.')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSubjectLink"]').contains('subj1')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedSubjectLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').contains('ID: subj1')
    });

    it('copy project gracefully handles errors when an attachment is missing in badge description', () => {
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/badges/badge1');

        cy.get('[data-cy="btn_edit-badge"]').click()

        cy.addAttachment('[data-cy="markdownEditorInput"] button.attachment-button')
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')
        cy.validateAttachmentInDb('project_id', 'proj1')

        cy.execSql(`delete from attachments where project_id='proj1'`, true)

        cy.visit('/administrator/')

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]').click();
        cy.get('[data-cy="projectName"]').type('New Project');
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('The badge with ID badge1 has a missing or invalid attachment.');
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"]')
            .contains('Please update the badge\'s description to resolve the issue, then try copying the project again.')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedBadgeLink"]').contains('badge1')
        cy.get('[data-cy="lengthyOpModal"] [data-cy="copyFailedMsg"] [data-cy="failedBadgeLink"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="subTitle"]').contains('ID: badge1')
    });
});