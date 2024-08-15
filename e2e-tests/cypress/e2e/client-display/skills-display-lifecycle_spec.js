/*
 * Copyright 2024 SkillTree
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
describe('Various Test to verify Skills Display creation lifecycle works properly', () => {

  beforeEach(() => {

  })


  if (!Cypress.env('oauthMode')) {
    it('overall progress data is loaded after page login', () => {
      cy.createProject(1)
      cy.createSubject(1)
      cy.createSkill(1, 1, 1)
      cy.createSkill(1, 1, 2)
      cy.createSkill(1, 1, 3)

      cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');

      cy.cdVisit('/')

      const validateProgress = () => {
        // overall
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '100')
        cy.get('[data-cy="overallPoints"] [data-cy="totalPoints"]').should('have.text', '600')

        cy.get('[data-cy="overallLevel"] [data-cy="levelOnTrophy"]').should('have.text', '1')
        cy.get('[data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')

        cy.get('[data-cy="levelProgress"] [data-cy="pointsTillNextLevelSubtitle"]').should('have.text', '50 Points to Level 2')

        // subject
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="levelTitle"]').should('have.text', 'Level 1')
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="pointsProgress"]').should('have.text', '100 / 600')
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="levelProgress"]').should('have.text', '40 / 90')
      }
      validateProgress()

      cy.logout()
      cy.visit('/test-skills-display/proj1')
      cy.get('#username').type(Cypress.env('proxyUser'));
      cy.get('#inputPassword').type('password');
      cy.get('[data-cy=login]').click();

      validateProgress()
    })
  }
})
