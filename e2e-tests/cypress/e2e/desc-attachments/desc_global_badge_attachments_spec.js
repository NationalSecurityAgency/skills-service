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

describe('Description Global Badge Attachments Tests', () => {

    const attachmentBtnSelector = '[data-cy="markdownEditorInput"] button.attachment-button'
    const markdownInput = '[data-cy=markdownEditorInput] div.toastui-editor-contents[contenteditable="true"]'
    const markdownEditorToolbarIconsSelector = '[data-cy="markdownEditorInput"] button.toastui-editor-toolbar-icons'

    it('attachments are disabled on global badge creation', () => {
        cy.viewport(1400, 1000)
        cy.visit('/administrator/globalBadges')

        cy.get('[data-cy="btn_Global Badges"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Badge')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('not.exist');
    });

    it('attachments are enabled on global badge edit', () => {
        cy.createGlobalBadge(1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/globalBadges')

        cy.get('[data-cy="editBtn"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Badge')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', null)
        cy.validateAttachmentInDb('skill_id', 'globalBadge1')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', null)
        cy.validateAttachmentInDb('skill_id', 'globalBadge1')
    });

    it('attachments are enabled on global badge edit - from global badge page', () => {
        cy.createGlobalBadge(1)
        cy.viewport(1400, 1000)
        cy.visit('/administrator/globalBadges/globalBadge1')

        cy.get('[data-cy="btn_edit-badge"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('Editing Existing Badge')
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');

        cy.addAttachment(attachmentBtnSelector)
        cy.validateAttachmentInDb('project_id', null)
        cy.validateAttachmentInDb('skill_id', 'globalBadge1')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.validateAttachmentInDb('project_id', null)
        cy.validateAttachmentInDb('skill_id', 'globalBadge1')
    });

});
