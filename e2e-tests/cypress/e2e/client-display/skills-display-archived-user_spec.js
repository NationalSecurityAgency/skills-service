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

describe('Skills Display for Archived User Tests', () => {

    it('do not show ranking for archived user', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {numPerformToCompletion: 1})
        cy.createSkill(1, 1, 2, {numPerformToCompletion: 1})
        cy.createSkill(1, 1, 3, {numPerformToCompletion: 1})
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')

        cy.archiveUser(Cypress.env('proxyUser'))
        cy.cdVisit('/')
        cy.get('[data-cy="userArchivedMessage"]')
        cy.get('[data-cy="myRankBtn"]').should('not.exist')

        cy.cdVisit('/rank')
        cy.get('[data-cy="myRankPositionStatCard"]').contains('N/A (Archived)')
    })
})
