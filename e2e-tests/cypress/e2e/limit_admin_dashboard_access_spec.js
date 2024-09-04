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
describe('Limit Admin Dashboard Access Tests', () => {
  const limitAdminAccessRoleManagerSelector = '[data-cy="limitAdminDashboardAccessRoleManager"]'
  it('display Training Creators Management for root role when property is enabled', () => {
    cy.logout();
    cy.fixture('vars.json')
      .then((vars) => {
        cy.login(vars.rootUser, vars.defaultPass);
      });

    cy.visit('/settings/security')
    cy.get('[data-cy="rootrm"]')
    cy.get('[data-cy="supervisorrm"]')
    cy.get(limitAdminAccessRoleManagerSelector).should('not.exist')

    cy.intercept('GET', '/public/config', (req) => {
      req.continue((res) => {
        res.body.limitAdminAccess = true
      })
    }).as('getConfig');
    cy.visit('/settings/security')
    cy.wait('@getConfig')
    cy.get('[data-cy="rootrm"]')
    cy.get('[data-cy="supervisorrm"]')
    cy.get(limitAdminAccessRoleManagerSelector).should('exist')
  })

  it('crud Training Creators Management', () => {
    cy.logout();
    cy.fixture('vars.json')
      .then((vars) => {
        cy.login(vars.rootUser, vars.defaultPass);
      });

    cy.intercept('GET', '/public/config', (req) => {
      req.continue((res) => {
        res.body.limitAdminAccess = true
      })
    }).as('getConfig');
    cy.visit('/settings/security')
    cy.wait('@getConfig')
    cy.get('[data-cy="rootrm"]')
    cy.get('[data-cy="supervisorrm"]')
    cy.get(limitAdminAccessRoleManagerSelector)

    cy.get(`${limitAdminAccessRoleManagerSelector} [data-cy="existingUserInputDropdown"] [data-pc-name="dropdownbutton"]`).click();
    cy.get(`${limitAdminAccessRoleManagerSelector} [data-cy="existingUserInputDropdown"]`).type('sk');

    cy.get('[data-pc-section="item"]')
      .contains('skills@skills.org')
      .click({ force: true });
    cy.get(`${limitAdminAccessRoleManagerSelector} [data-cy="addUserBtn"]`).should('be.enabled').click();

    const userTableSelector = `${limitAdminAccessRoleManagerSelector} [data-cy="roleManagerTable"]`
    cy.validateTable(userTableSelector, [
      [{
        colIndex: 0,
        value: '(skills@skills.org)'
      }, {
        colIndex: 1,
        value: 'Training Creator'
      }],
    ], 5, true, null, false);
  })

})
