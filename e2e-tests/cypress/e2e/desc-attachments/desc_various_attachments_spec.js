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
    const markdownInput = '[data-cy=markdownEditorInput] div.toastui-editor-contents[contenteditable="true"]'
    const markdownEditorToolbarIconsSelector = '[data-cy="markdownEditorInput"] button.toastui-editor-toolbar-icons'

    it('attachments are not enabled on Contact Admins page', () => {
        cy.loginAsRoot()
        cy.viewport(1400, 1000)
        cy.visit('/administrator/contactAdmins')

        cy.get(`[data-cy="emailUsers_body"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="emailUsers_body"] ${attachmentBtnSelector}`).should('not.exist');
    });

    it('user must acknowledge user agreement after logging in', () => {
        cy.loginAsRoot()
        cy.visit('/settings/system');
        cy.get(`[data-cy="userAgreementMarkdownEditor"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="userAgreementMarkdownEditor"] ${attachmentBtnSelector}`).should('not.exist');
    });

});
