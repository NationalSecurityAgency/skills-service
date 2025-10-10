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

const inputLines = [
    'paragraph 1 - divinedragon',
    'paragraph 2 - jabberwocky',
    'paragraph 3',
    'paragraph 4 - jabberwocky',
    'paragraph 5 - divinedragon',
]

Cypress.Commands.add("validatePrefixOps", (modalSelector, validationVal, expected, notExpected, itemQualifierSelector = '', checkPrefixDoesntExist = true, clearExistingInput= false) => {
    cy.log(`validatePrefixOps: modalSelector=${modalSelector}, validationVal=${validationVal}, expected=${expected}, notExpected=${notExpected}, itemQualifierSelector=${itemQualifierSelector}`);
    if (modalSelector) {
        cy.get(modalSelector).click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')
    }

    if (checkPrefixDoesntExist) {
        cy.get(`${itemQualifierSelector} [data-cy="prefixSelect"]`).should('not.exist')
    }

    const inputTxt = inputLines.join('\n\n')
    if (clearExistingInput) {
        cy.typeInMarkdownEditor(`${itemQualifierSelector} [data-cy="markdownEditorInput"]`, '{selectall}{backspace}');
    }
    cy.typeInMarkdownEditor(`${itemQualifierSelector} [data-cy="markdownEditorInput"]`, inputTxt);

    cy.get(`${itemQualifierSelector} [data-cy="descriptionError"]`).contains(`not contain ${validationVal}`);
    cy.get(`${itemQualifierSelector} [data-cy="prefixSelect"]`).click()

    expected.forEach((val) => {
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${val}) "]`)
    })
    notExpected.forEach((val) => {
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${val}) "]`).should("not.exist")
    })

    const selectedVal = expected[1]
    cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${selectedVal}) "]`).click()
    cy.get(`${itemQualifierSelector} [data-cy="missingPreviewText"]`).should('not.exist')
    cy.get(`${itemQualifierSelector} [data-cy="previewPrefixBtn"]`).click()
    cy.get(`${itemQualifierSelector} [data-cy="descriptionError"]`).should('not.be.visible')
    cy.get(`${itemQualifierSelector} [data-cy="previewPrefixBtn"]`).should('not.be.visible')
    cy.get(`${itemQualifierSelector} [data-cy="addPrefixBtn"]`).should('not.be.visible')
    cy.get(`${itemQualifierSelector} [data-cy="closeMissingPreviewBtn"]`).should('be.enabled')
    cy.get(`${itemQualifierSelector} [data-cy="markdownEditorInput"]`).should('not.be.visible')

    const expectedLines = inputLines.map((line) => line.includes(validationVal) ? `(${selectedVal}) ${line}` : line)
    cy.validateMarkdownViewerText(`${itemQualifierSelector} [data-cy="missingPreviewText"]`, expectedLines)

    cy.get(`${itemQualifierSelector} [data-cy="closeMissingPreviewBtn"]`).click()
    cy.get(`${itemQualifierSelector} [data-cy="descriptionError"]`).contains(`not contain ${validationVal}`);
    cy.get(`${itemQualifierSelector} [data-cy="prefixSelect"] [aria-label="(${selectedVal}) "]`)
    cy.get(`${itemQualifierSelector} [data-cy="previewPrefixBtn"]`).should('be.enabled')
    cy.get(`${itemQualifierSelector} [data-cy="addPrefixBtn"]`).should('be.enabled')
    cy.get(`${itemQualifierSelector} [data-cy="closeMissingPreviewBtn"]`).should('not.exist')

    cy.validateMarkdownEditorText(`${itemQualifierSelector} [data-cy="markdownEditorInput"]`, inputLines)

    cy.get(`${itemQualifierSelector} [data-cy="addPrefixBtn"]`).click()
    cy.validateMarkdownEditorText(`${itemQualifierSelector} [data-cy="markdownEditorInput"]`, expectedLines)

    if (modalSelector) {
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]').should('not.exist')
    }
});
Cypress.Commands.add("validateAllDragonPrefixOps", (modalSelector, itemQualifierSelector = '', clearExistingInput = false) => {
    cy.validatePrefixOps(modalSelector, 'jabberwocky', ['A', 'B'], ['C', 'D'], itemQualifierSelector, true, clearExistingInput)
});
Cypress.Commands.add("validateDivineDragonPrefixOps", (modalSelector, itemQualifierSelector= '', clearExistingInput = false) => {
    cy.validatePrefixOps(modalSelector, 'divinedragon', ['A', 'B', 'C', 'D'], [], itemQualifierSelector, true, clearExistingInput)
});





