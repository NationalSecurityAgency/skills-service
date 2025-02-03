/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
describe('Show warning when upgrade is in progress', () => {

  beforeEach(() => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createProject(2)
    cy.createQuizDef(1)
    cy.createAdminGroupDef(1);

    cy.intercept('GET', '/public/config', (req) => {
      req.reply((res) => {
        const conf = res.body;
        conf.dbUpgradeInProgress = true;
        res.send(conf);
      });
    }).as('loadConfigWithDbInProgressUpgrade')
  })

  it('upgrade warning banner is displayed on admin page', () => {
    cy.visit('/administrator/')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy=upgradeInProgressWarning]').should('be.visible')
  })

  it('upgrade warning banner is displayed on p&r page', () => {
    cy.visit('/')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy=upgradeInProgressWarning]').should('be.visible')
  })

  it('upgrade warning banner must not be displayed in skills-client iframe', () => {
    cy.ignoreSkillsClientError()
    cy.visit('/test-skills-client/proj1')
    cy.wait('@loadConfigWithDbInProgressUpgrade')
    cy.wrapIframe().find('[data-cy="subjectTileBtn"]')
    cy.wait(2000)
    cy.wrapIframe().find('[data-cy=upgradeInProgressWarning]').should('not.exist')
  })

  it('when adding project to an admin group and DB upgrade is in progress redirect to the upgrade in progress page', () => {
    cy.intercept('POST', '/admin/admin-group-definitions/adminGroup1/projects/proj1', {
      statusCode: 503,
        body: {
          errorCode: 'DbUpgradeInProgress',
        } }
    ).as('saveEndpoint')
    cy.intercept('GET', '/admin/admin-group-definitions/adminGroup1/projects').as('loadGroupProjects');
    cy.visit('/administrator/adminGroups/adminGroup1/group-projects');
    cy.wait('@loadGroupProjects');
    cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="noContent"]')
    cy.get('[data-cy="projectSelector"] [data-pc-section="label"]').contains('Search available projects...').should('be.visible')
    cy.get('[data-cy="projectSelector"]').click()
    cy.get('[data-cy="availableProjectSelection-proj1"]').click()

    cy.wait('@saveEndpoint')
    cy.get('[data-cy="upgradeInProgressError"]').contains('A database upgrade is in progress!')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('when adding quiz to an admin group and DB upgrade is in progress redirect to the upgrade in progress page', () => {
    cy.intercept('POST', '/admin/admin-group-definitions/adminGroup1/quizzes/quiz1', {
      statusCode: 503,
      body: {
        errorCode: 'DbUpgradeInProgress',
      } }
    ).as('saveEndpoint')
    cy.intercept('GET', '/admin/admin-group-definitions/adminGroup1/quizzes').as('loadGroupQuizzes');
    cy.visit('/administrator/adminGroups/adminGroup1/group-quizzes');
    cy.wait('@loadGroupQuizzes');
    cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="noContent"]')
    cy.get('[data-cy="quizSelector"] [data-pc-section="label"]').contains('Search available quizzes and surveys...').should('be.visible')
    cy.get('[data-cy="quizSelector"]').click()
    cy.get('[data-cy="availableQuizSelection-quiz1"]').click()

    cy.wait('@saveEndpoint')
    cy.get('[data-cy="upgradeInProgressError"]').contains('A database upgrade is in progress!')
    cy.url().should('include', '/upgrade-in-progress')
  })

})