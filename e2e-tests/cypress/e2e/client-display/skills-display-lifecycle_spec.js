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
      cy.get('#password').type('password');
      cy.get('[data-cy=login]').click();

      validateProgress()
    })
  }
})
