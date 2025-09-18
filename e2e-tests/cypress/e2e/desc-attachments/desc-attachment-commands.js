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

const markdownInput = '[data-cy=markdownEditorInput] div.toastui-editor-contents[contenteditable="true"]'

Cypress.Commands.add('addAttachment', (attachBtnSelector, prependToSelectors = '') => {
    cy.get(attachBtnSelector).click()
    cy.get(`${prependToSelectors}input[type=file]`).selectFile('cypress/attachments/test-pdf.pdf', {force: true})
    cy.get(markdownInput).get(`${prependToSelectors}a[href^="/api/download/"]:contains(test-pdf.pdf)`)
        .should('have.attr', 'target', '_blank');
});
Cypress.Commands.add('validateAttachmentInDb', (idColumn, expectedId) => {
    cy.execSql('SELECT * FROM attachments').then((result) => {
        expect(result).to.have.length(1);
        expect(result[0]).to.have.property(idColumn, expectedId)
    })
});


