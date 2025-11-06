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

Cypress.Commands.add("initiateSkillsCopyModal", (skillIndexesToCopy, optionalExpandedGroupSelector = '') => {
    const tableSelector = '[data-cy="skillsTable"]'
    skillIndexesToCopy.forEach((index) => {
        cy.get(`${optionalExpandedGroupSelector} ${tableSelector} [data-p-index="${index}"] [data-pc-name="pcrowcheckbox"] [data-pc-section="input"]`).click()
    })
    cy.get(`${optionalExpandedGroupSelector} [data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]`).should('have.text', skillIndexesToCopy.length)
    cy.get(`${optionalExpandedGroupSelector} [data-cy="skillActionsBtn"]`).click()
    cy.get('[data-cy="skillsActionsMenu"] [aria-label="Copy to another Project"]').click()

    cy.get('[data-pc-name="dialog"] [data-pc-section="title"]').should('have.text', 'Copy Selected Skills To Another Project')
});