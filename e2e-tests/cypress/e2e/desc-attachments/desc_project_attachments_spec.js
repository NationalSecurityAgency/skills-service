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

import './desc-attachment-commands'

describe('Description Project Attachments Tests', () => {

    const attachmentBtnSelector = '[data-cy="markdownEditorInput"] button.attachment-button'
    const markdownEditorToolbarIconsSelector = '[data-cy="markdownEditorInput"] button.toastui-editor-toolbar-icons'

    it('attachments are not enabled on project creation', () => {
        cy.viewport(1400, 1000)
        cy.visit('/administrator/')
        cy.get('[data-cy="noProjectsYet"]')

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('.p-dialog-header').contains('New Project')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('not.exist');
    });

    it('attachments are enabled when editing a project', () => {
        cy.createProject(1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/')

        cy.get('[data-cy="editProjBtn"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Project')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
    });

    it('attachments are enabled when editing a project - from project page', () => {
        cy.createProject(1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1')

        cy.get('[data-cy="btn_edit-project"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Project')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
    });

    it('attachments are enabled on subject creation', () => {
        cy.createProject(1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1')

        cy.get('[data-cy="btn_Subjects"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Subject')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', null)

        cy.get('[data-cy="subjectName"]').type('subj1')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'subj1Subject')
    });

    it('attachments are enabled on subject edit', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1')

        cy.get('[data-cy="editBtn"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Subject')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', null)

        cy.get('[data-cy="subjectName"]').type('subj1')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'subj1')
    });

    it('attachments are enabled on subject edit - from subject page', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1')

        cy.get('[data-cy="btn_edit-subject"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Subject')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', null)

        cy.get('[data-cy="subjectName"]').type('subj1')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'subj1')
    });

    it('attachments are enabled on skill creation', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1')

        cy.get('[data-cy="newSkillButton"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Skill')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', null)

        cy.get('[data-cy="skillName"]').type('type')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'typeSkill')
    });

    it('attachments are enabled on skill edit', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Edit Skill')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', null)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'skill1')
    });

    it('attachments are enabled on skill edit - from skill page', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Edit Skill')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', null)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'skill1')
    });

    it('attachments are enabled on project badge creation', () => {
        cy.createProject(1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/badges')

        cy.get('[data-cy="btn_Badges"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Badge')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', null)

        cy.get('[data-cy="name"]').type('type')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'typeBadge')
    });

    it('attachments are enabled on project badge edit', () => {
        cy.createProject(1)
        cy.createBadge(1, 1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/badges')

        cy.get('[data-cy="editBtn"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Badge')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', null)

        cy.get('[data-cy="name"]').type('type')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'badge1')
    });

    it('attachments are enabled on project badge edit - from badge page', () => {
        cy.createProject(1)
        cy.createBadge(1, 1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/badges/badge1')

        cy.get('[data-cy="btn_edit-badge"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Badge')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', null)

        cy.get('[data-cy="name"]').type('type')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'badge1')
    });

    it('attachments are not enabled on project email users', () => {
        cy.createProject(1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/contact-users')

        cy.get(`[data-cy="emailUsers_body"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="emailUsers_body"] ${attachmentBtnSelector}`).should('not.exist');
    });

    it('attachments are enabled on Skill Group creation', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1')

        cy.get('[data-cy="newGroupButton"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Skills Group')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.get('[data-cy="name"]').type('type')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'typeGroup')
    });

    it('attachments are enabled on Skill Group edit', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkillsGroup(1, 1,1 )
        cy.viewport(1400, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1')

        cy.get('[data-cy="editSkillButton_group1"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Edit Skills Group')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'group1')
    });

    it('attachments are for user reporting approval based skills justification from skill page', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.viewport(1400, 1000)
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')

        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'skill1')

        cy.get('[data-cy="selfReportSubmitBtn"]').click()
        cy.get('[data-cy="selfReportAlert"]').contains('Submitted')
        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'skill1')
    });

    it('attachments are for user reporting approval based skills justification from subject page', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.viewport(1400, 1000)
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1')

        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Great Skill 2')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]').click()
        cy.get(`[data-cy="skillProgress_index-1"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="skillProgress_index-1"] ${attachmentBtnSelector}`).should('be.visible');
        cy.addAttachment(`[data-cy="skillProgress_index-1"] ${attachmentBtnSelector}`)

        cy.validateAttachmentInDb('project_id', 'proj1')
        cy.validateAttachmentInDb('skill_id', 'skill2')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requestApprovalBtn"]').click()
        cy.get(`[data-cy="skillProgress_index-2"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="skillProgress_index-2"] ${attachmentBtnSelector}`).should('be.visible');
        cy.addAttachment(`[data-cy="skillProgress_index-2"] ${attachmentBtnSelector}`, '[data-cy="skillProgress_index-2"] ')

        cy.execSql(`SELECT * FROM attachments where skill_id = 'skill3'`).then((result) => {
            expect(result).to.have.length(1);
            expect(result[0]).to.have.property('project_id', 'proj1')
        })
        cy.execSql(`SELECT * FROM attachments where skill_id = 'skill2'`).then((result) => {
            expect(result).to.have.length(1);
            expect(result[0]).to.have.property('project_id', 'proj1')
        })

        cy.get('[data-cy="skillProgress_index-2"]  [data-cy="selfReportSubmitBtn"]').click()
        cy.get('[data-cy="skillProgress_index-1"]  [data-cy="selfReportSubmitBtn"]').click()
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportAlert"]').contains('Submitted')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="selfReportAlert"]').contains('Submitted')

        cy.execSql(`SELECT * FROM attachments where skill_id = 'skill3'`).then((result) => {
            expect(result).to.have.length(1);
            expect(result[0]).to.have.property('project_id', 'proj1')
        })
        cy.execSql(`SELECT * FROM attachments where skill_id = 'skill2'`).then((result) => {
            expect(result).to.have.length(1);
            expect(result[0]).to.have.property('project_id', 'proj1')
        })
    });



});
