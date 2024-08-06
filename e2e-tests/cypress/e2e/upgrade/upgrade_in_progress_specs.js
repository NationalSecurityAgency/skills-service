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

})