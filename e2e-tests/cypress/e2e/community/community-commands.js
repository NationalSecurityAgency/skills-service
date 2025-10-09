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

Cypress.Commands.add("validatePrefixOptions", (modalSelector, validationVal, expected, notExpected, itemQualifierSelector = '' ) => {
    if (modalSelector) {
        cy.get(modalSelector).click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')
    }

    cy.get(`${itemQualifierSelector} [data-cy="prefixSelect"]`).should('not.exist')
    cy.get(`${itemQualifierSelector} [data-cy="markdownEditorInput"]`).type(validationVal);
    cy.get(`${itemQualifierSelector} [data-cy="descriptionError"]`).contains(`not contain ${validationVal}`);
    cy.get(`${itemQualifierSelector} [data-cy="prefixSelect"]`).click()

    expected.forEach((val) => {
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${val}) "]`)
    })
    notExpected.forEach((val) => {
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${val}) "]`).should("not.exist")
    })

    if (modalSelector) {
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]').should('not.exist')
    }
});
Cypress.Commands.add("validateAllDragonOptions", (modalSelector, itemQualifierSelector = '') => {
    cy.validatePrefixOptions(modalSelector, 'jabberwocky', ['A', 'B'], ['C', 'D'], itemQualifierSelector)
});
Cypress.Commands.add("validateDivineDragonOptions", (modalSelector, itemQualifierSelector= '') => {
    cy.validatePrefixOptions(modalSelector, 'divinedragon', ['A', 'B', 'C', 'D'], [], itemQualifierSelector)
});





