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
import '../desc-attachments/desc-attachment-commands.js'

const attachmentBtnSelector = '[data-cy="markdownEditorInput"] button.attachment-button'
const markdownEditorToolbarIconsSelector = '[data-cy="markdownEditorInput"] button.toastui-editor-toolbar-icons'

Cypress.Commands.add('validateAllDragonsWarning', () => {
    cy.get('[data-cy="attachmentWarningMessage"]').contains('Friendly Reminder: Only safe files please for All Dragons')
});
Cypress.Commands.add('validateDivineDragonWarning', () => {
    cy.get('[data-cy="attachmentWarningMessage"]').contains('Friendly Reminder: Only safe files please for Divine Dragon')
});
Cypress.Commands.add('openDescModalAndAttachFile', (btnSelector, expectedTitle) => {
    cy.get(btnSelector).click()
    cy.get('[data-p="modal"] [data-pc-section="title"]').contains(expectedTitle)
    cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
    cy.get(attachmentBtnSelector).should('be.visible');
    cy.addAttachment(attachmentBtnSelector)
});