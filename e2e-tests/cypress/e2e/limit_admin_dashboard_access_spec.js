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
  const userTableSelector = `${limitAdminAccessRoleManagerSelector} [data-cy="roleManagerTable"]`

  it('display Training Creators Management for root role when property is enabled', () => {
    cy.loginAsRoot()

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

  it('add Training Creators users', () => {
    const users = ['skills@skills.org', 'skills1@skills.org', 'skills2@skills.org', 'skills3@skills.org']
    cy.register(users[1], 'password1', false)
    cy.register(users[2], 'password1', false)
    cy.register(users[3], 'password1', false)
    cy.loginAsRoot()

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

    const addUser = (userId) => {
      cy.get(`${limitAdminAccessRoleManagerSelector} [data-cy="existingUserInputDropdown"] [data-pc-name="dropdownbutton"]`).click();
      cy.get(`${limitAdminAccessRoleManagerSelector} [data-cy="existingUserInputDropdown"]`).clear().type(userId);

      cy.get('[data-pc-section="item"]')
        .contains(userId)
        .click({ force: true });
      cy.get(`${limitAdminAccessRoleManagerSelector} [data-cy="addUserBtn"]`).should('be.enabled').click();
    }

    addUser(users[0])

    cy.validateTable(userTableSelector, [
      [{
        colIndex: 0,
        value: `(${users[0]})`
      }, {
        colIndex: 1,
        value: 'Training Creator'
      }],
    ], 5, true, null, false);

    addUser(users[1])
    addUser(users[2])
    addUser(users[3])
    const expectedTableData = users.map((u) => [{
      colIndex: 0,
      value: `(${u})`
    }, {
      colIndex: 1,
      value: 'Training Creator'
    }])
    cy.validateTable(userTableSelector, expectedTableData, 5, true, null, false);
  })

  it('delete Training Creators users', () => {
    const users = ['skills1@skills.org', 'skills2@skills.org', 'skills3@skills.org']
    users.forEach((u) => {
      cy.register(u, 'password1', false)
    })
    cy.loginAsRoot()
    users.forEach((u) => {
      cy.request('PUT', `/root/users/${u}/roles/ROLE_DASHBOARD_ADMIN_ACCESS`)
    })

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
    const expectedTableData = users.map((u) => [{
      colIndex: 0,
      value: `(${u})`
    }, {
      colIndex: 1,
      value: 'Training Creator'
    }])
    cy.validateTable(userTableSelector, expectedTableData, 5, true, null, false);


    cy.get(`${userTableSelector} [data-p-index="1"] [data-cy="removeUserBtn"]`).click()
    cy.get('[data-pc-name="acceptbutton"]').click()
    cy.validateTable(userTableSelector, [
      [{
        colIndex: 0,
        value: `(${users[0]})`
      }],
      [{
        colIndex: 0,
        value: `(${users[2]})`
      }]
    ], 5, true, null, false);

    cy.get(`${userTableSelector} [data-p-index="0"] [data-cy="removeUserBtn"]`).click()
    cy.get('[data-pc-name="acceptbutton"]').click()
    cy.get(`${userTableSelector} [data-p-index="0"] [data-cy="removeUserBtn"]`).click()
    cy.get('[data-pc-name="acceptbutton"]').click()
    cy.get(userTableSelector).contains('There are no records to show')
  })

  it('navigation to admin is only available when /app/userInfo has adminDashboardAccess=true', () => {
    cy.intercept('GET', '/public/config', (req) => {
      req.continue((res) => {
        res.body.limitAdminAccess = true
      })
    }).as('getConfig');
    cy.intercept('GET', '/app/userInfo', (req) => {
      req.continue((res) => {
        res.body.adminDashboardAccess = true
      })
    }).as('getUserInfo1');

    cy.visit('/')
    cy.wait('@getConfig')
    cy.wait('@getUserInfo1')
    cy.get('[data-cy="manageMyProjsBtnInNoContent"')

    const pAndRNavSelector = '[aria-label="Progress and Ranking"]'
    const projAdminNavSelector = '[aria-label="Project Admin"]'
    cy.get('[data-cy="settings-button"]').click();
    cy.get(pAndRNavSelector).should('exist');
    cy.get(projAdminNavSelector).should('exist');

    cy.intercept('GET', '/app/userInfo', (req) => {
      req.continue((res) => {
        res.body.adminDashboardAccess = false
      })
    }).as('getUserInfo2');
    cy.visit('/')
    cy.wait('@getConfig')
    cy.wait('@getUserInfo2')
    cy.get('[data-cy="manageMyProjsBtnInNoContent"')

    cy.get('[data-cy="settings-button"]').click();
    cy.get(pAndRNavSelector).should('not.exist');
    cy.get(projAdminNavSelector).should('not.exist');
  })

})
