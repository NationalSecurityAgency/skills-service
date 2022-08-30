/*
 * Copyright 2020 SkillTree
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
describe('Root Pin and Unpin Tests', () => {
    beforeEach(() => {
        cy.intercept('GET', '/app/projects')
            .as('getProjects')
            .intercept('GET', '/api/icons/customIconCss')
            .as('getProjectsCustomIcons')
            .intercept('GET', '/app/userInfo')
            .as('getUserInfo')
            .intercept('/admin/projects/proj1/users/root@skills.org/roles')
            .as('getRolesForRoot');
    });

    it('Pin and Unpin projects', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'one'
        });

        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'two'
        });

        cy.request('POST', '/app/projects/proj3', {
            projectId: 'proj3',
            name: 'three'
        });

        cy.request('POST', '/app/projects/proj4', {
            projectId: 'proj4',
            name: 'four'
        });
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('GET', '/app/projects')
                    .as('default');
                cy.intercept('GET', '/app/projects?search=one')
                    .as('searchOne');
                cy.intercept('POST', '/root/pin/proj1')
                    .as('pinOne');
                cy.intercept('DELETE', '/root/pin/proj1')
                    .as('unpinOne');
                cy.intercept('GET', '/admin/projects/proj1/subjects')
                    .as('loadSubjects');

                cy.visit('/administrator/');
                //confirm that default project loading returns no projects for root user
                cy.wait('@default');
                cy.contains('No Projects Yet...')
                    .should('be.visible');

                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Pin Projects');
                cy.contains('Search Project Catalog');

                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('t');
                cy.get('[data-cy=pinProjectsSearchResultsNumRows]')
                    .contains('Rows: 3');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('three');

                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('wo');
                cy.get('[data-cy=pinProjectsSearchResultsNumRows]')
                    .contains('Rows: 1');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception')
                    .should('not.exist');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');

                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('1');
                cy.get('[data-cy=pinProjects]')
                    .contains('No Results');

                cy.get('[data-cy=pinProjectsClearSearch]')
                    .click();
                cy.get('[data-cy=pinProjects]')
                    .contains('Search Project Catalog');

                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get('[data-cy=pinProjectsSearchResultsNumRows]')
                    .contains('Rows: 5');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('three');

                // pin 1 project
                const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr';
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');
                cy.get('@cyRows')
                    .eq(0)
                    .find('td')
                    .as('row1');
                cy.get('@row1')
                    .eq(0)
                    .contains('four');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=unpinButton]')
                    .should('not.exist');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=pinButton]')
                    .click();
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=pinButton]')
                    .should('not.exist');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=unpinButton]')
                    .should('exist');
                cy.get('[data-cy=modalDoneButton]')
                    .click();

                const projectsSelector = '[data-cy=projectCard]';
                cy.get(projectsSelector)
                    .should('have.length', 1)
                    .as('projects');
                cy.get('@projects')
                    .eq(0)
                    .contains('four');

                // make sure the pinned project is still pinned
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');
                cy.get('@cyRows')
                    .eq(0)
                    .find('td')
                    .as('row1');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=pinButton]')
                    .should('not.exist');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=unpinButton]')
                    .should('exist');
                cy.get('[data-cy=modalDoneButton]')
                    .click();

                // unpin that project
                cy.get('@projects')
                    .eq(0)
                    .contains('Unpin')
                    .click();
                cy.contains('No Projects Yet');
            });
    });

    it('Pin all projects then unpin 1', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'one'
        });

        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'two'
        });

        cy.request('POST', '/app/projects/proj3', {
            projectId: 'proj3',
            name: 'three'
        });

        cy.request('POST', '/app/projects/proj4', {
            projectId: 'proj4',
            name: 'four'
        });
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('GET', '/app/projects')
                    .as('default');
                cy.intercept('GET', '/app/projects?search=one')
                    .as('searchOne');
                cy.intercept('POST', '/root/pin/proj1')
                    .as('pinOne');
                cy.intercept('DELETE', '/root/pin/proj1')
                    .as('unpinOne');
                cy.intercept('GET', '/admin/projects/proj1/subjects')
                    .as('loadSubjects');

                cy.visit('/administrator/');
                //confirm that default project loading returns no projects for root user
                cy.wait('@default');
                cy.contains('No Projects Yet...')
                    .should('be.visible');

                const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr';
                const projectsSelector = '[data-cy=projectCard]';

                // pin all projects
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Search Project Catalog');
                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');

                for (let i = 0; i < 5; i += 1) {
                    cy.get('@cyRows')
                        .eq(i)
                        .find('td')
                        .as('row1');
                    cy.get('@row1')
                        .eq(0)
                        .find('[data-cy=pinButton]')
                        .click();
                    cy.get('@row1')
                        .eq(0)
                        .find('[data-cy=unpinButton]')
                        .should('exist');
                }
                cy.get('[data-cy=modalDoneButton]')
                    .click();

                cy.get(projectsSelector)
                    .should('have.length', 5)
                    .as('projects');
                cy.contains('ID: Inception');
                cy.contains('one');
                cy.contains('two');
                cy.contains('three');
                cy.contains('four');

                // unpin from the component
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Pin Projects');
                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get('[data-cy=pinProjectsSearchResultsNumRows]')
                    .contains('Rows: 5');
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');
                cy.get('@cyRows')
                    .eq(2)
                    .find('td')
                    .as('row1');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=pinButton]')
                    .should('not.exist');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=unpinButton]')
                    .should('exist');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=unpinButton]')
                    .click();
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=pinButton]')
                    .should('exist');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=unpinButton]')
                    .should('not.exist');

                cy.get('[data-cy=modalDoneButton]')
                    .click();

                cy.get(projectsSelector)
                    .should('have.length', 4)
                    .as('projects');
                cy.contains('Inception');
                cy.contains('two');
                cy.contains('one')
                    .should('not.exist');
            });
    });

    it('Assign a root user as a project admin and verify the project is pinned, then remove admin and verify it is unpinned', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'one'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PROJECT_ADMIN**')
            .as('loadProjectAdmins');

        cy.intercept('GET', '/app/projects')
            .as('default');
        cy.intercept('GET', '/app/projects?search=one')
            .as('searchOne');
        cy.intercept('POST', '/root/pin/proj1')
            .as('pinOne');
        cy.intercept('DELETE', '/root/pin/proj1')
            .as('unpinOne');
        cy.intercept('GET', '/admin/projects/proj1/subjects')
            .as('loadSubjects');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .type('root');
        cy.wait('@suggest');
        cy.contains('root@skills.org')
            .click();
        cy.clickButton('Add');
        cy.wait('@addAdmin');
        cy.wait('@loadProjectAdmins');

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);

                cy.visit('/administrator/');
                cy.wait('@default');

                const projectsSelector = '[data-cy=projectCard]';
                cy.get(projectsSelector)
                    .should('have.length', 1)
                    .as('projects');
                cy.contains('one');

                cy.logout();

                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }

                cy.log('visiting proj1 access page');
                cy.visit('/administrator/projects/proj1/access');
                cy.wait('@loadUserInfo');
                cy.wait('@loadProject');
                cy.wait('@loadProjectAdmins');

                // remove the root user as an admin now
                const tableSelector = '[data-cy=roleManagerTable]';
                const rowSelector = `${tableSelector} tbody tr`;
                cy.log('removing user');
                if (!Cypress.env('oauthMode')) {
                    cy.get(`${tableSelector} [data-cy="removeUserBtn"]`)
                        .eq(0)
                        .click();
                } else {
                    // in oauth mode the default user name is different which affects how the users are sorted
                    cy.get(`${tableSelector} [data-cy="removeUserBtn"]`)
                        .eq(1)
                        .click();
                }
                cy.contains('YES, Delete It')
                    .click();
                cy.wait('@loadProjectAdmins');
                cy.get(rowSelector)
                    .should('have.length', 1)
                    .as('cyRows1');
                cy.get('@cyRows1')
                    .eq(0)
                    .find('td')
                    .as('rowA');
                cy.get('@rowA')
                    .eq(0)
                    .contains('root@skills.org')
                    .should('not.exist');
                cy.logout();

                cy.login(vars.rootUser, vars.defaultPass);

                cy.visit('/administrator/');
                cy.wait('@default');
                cy.contains('No Projects Yet...')
                    .should('be.visible');
            });
    });

    it('Assign root role to a project admin user and verify all projects are pinned', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'one'
        });
        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'two'
        });

        cy.request('POST', '/app/projects/proj3', {
            projectId: 'proj3',
            name: 'three'
        });

        cy.request('POST', '/app/projects/proj4', {
            projectId: 'proj4',
            name: 'four'
        });

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                let projAdminUser = vars.defaultUser;
                let projAdminUserPrefix = projAdminUser.substring(0, projAdminUser.indexOf('@'));
                let projAdminUserId = vars.defaultUser;
                if (Cypress.env('oauthMode')) {
                    projAdminUser = vars.oauthUser;
                    projAdminUserPrefix = projAdminUser.substring(0, projAdminUser.indexOf('@'));
                    projAdminUserId = `${projAdminUserPrefix}-hydra`;
                }
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE')
                    .as('getEligibleForRoot');
                cy.intercept('PUT', `/root/users/${projAdminUserId}/roles/ROLE_SUPER_DUPER_USER`)
                    .as('addRoot');
                cy.intercept({
                    method: 'GET',
                    url: '/app/projects'
                })
                    .as('loadProjects');
                cy.intercept({
                    method: 'GET',
                    url: '/root/isRoot'
                })
                    .as('checkRoot');

                const rootUsrTableSelector = '[data-cy="rootrm"] [data-cy="roleManagerTable"]';
                cy.visit('/administrator/');
                cy.get('[data-cy=subPageHeader]')
                    .contains('Projects');
                cy.get('button.dropdown-toggle')
                    .first()
                    .click({ force: true });
                cy.contains('Settings')
                    .click();
                cy.wait('@checkRoot');
                cy.clickNav('Security');
                cy.validateTable(rootUsrTableSelector, [
                    [{
                        colIndex: 0,
                        value: '(root@skills.org)'
                    }],
                ], 5, true, null, false);

                cy.get('[data-cy="existingUserInput"]')
                    .first()
                    .click()
                    .type(`${projAdminUserPrefix}{enter}`);
                cy.wait('@getEligibleForRoot');
                cy.contains(projAdminUserPrefix)
                    .click();
                cy.contains('Add')
                    .first()
                    .click();
                cy.wait('@addRoot');

                if (!Cypress.env('oauthMode')) {
                    cy.log('not oauthMode, expecting ascending sort with root user first');
                    cy.validateTable(rootUsrTableSelector, [
                        [{
                            colIndex: 0,
                            value: '(root@skills.org)'
                        }],
                        [{
                            colIndex: 0,
                            value: `(${projAdminUser})`
                        }],
                    ], 5, true, null, false);
                } else {
                    cy.validateTable(rootUsrTableSelector, [
                        [{
                            colIndex: 0,
                            value: `(${projAdminUserPrefix})`
                        }],
                        [{
                            colIndex: 0,
                            value: '(root@skills.org)'
                        }],
                    ], 5, true, null, false);
                }

                cy.logout();

                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }

                cy.intercept('GET', '/app/projects')
                    .as('default');
                cy.visit('/administrator/');
                cy.wait('@default');

                const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr';
                const projectsSelector = '[data-cy=projectCard]';

                cy.get(projectsSelector)
                    .should('have.length', 4)
                    .as('projects');
                cy.contains('one');
                cy.contains('two');
                cy.contains('three');
                cy.contains('four');

                // verify all projects are pinned except Inception
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Search Project Catalog');
                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');

                for (let i = 0; i < 5; i += 1) {
                    let pinState = 'not.exist';
                    let unpinState = 'exist';
                    cy.get('@cyRows')
                        .eq(i)
                        .find('td')
                        .as('rowI');
                    if (i == 1) {
                        // row 1 is the Inception project and should not be auto pinned, all others should be
                        cy.get('@rowI')
                            .contains('Inception');
                        pinState = 'exist';
                        unpinState = 'not.exist';
                    }
                    cy.get('@rowI')
                        .eq(0)
                        .find('[data-cy=pinButton]')
                        .should(pinState);
                    cy.get('@rowI')
                        .eq(0)
                        .find('[data-cy=unpinButton]')
                        .should(unpinState);
                }
                cy.get('[data-cy=modalDoneButton]')
                    .click();

            });
    });

    it('Pin all projects then unpin 1 using projects table', () => {
      for (let i = 1; i <= 10; i += 1) {
        cy.createProject(i);
      }
      cy.logout();
      cy.fixture('vars.json').then((vars) => {
        cy.login(vars.rootUser, vars.defaultPass);
        cy.intercept('GET', '/app/projects').as('default');
        cy.intercept('GET', '/app/projects?search=one').as('searchOne');
        cy.intercept('POST', '/root/pin/proj1').as('pinOne');
        cy.intercept('DELETE', '/root/pin/proj1').as('unpinOne');
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/');
        //confirm that default project loading returns no projects for root user
        cy.wait('@default');
        cy.contains('No Projects Yet...').should('be.visible');

        const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr'
        const projectsSelector = '[data-cy=projectCard]';

        // pin all projects
        cy.get('[data-cy=subPageHeaderControls]').contains('Pin').click();
        cy.contains('Search Project Catalog');
        cy.get('[data-cy=pinProjectsLoadAllButton]').click();
        cy.get(rowSelector).should('have.length', 5).as('cyRows');

        for (let page = 1; page <= 2; page += 1) {
          cy.get('[data-cy=pinedResultsPaging]').contains(page).click();
          for (let i = 0; i < 5; i += 1) {
            cy.get('@cyRows')
              .eq(i)
              .find('td')
              .as('row1');
            cy.get('@row1')
              .eq(0)
              .find('[data-cy=pinButton]')
              .click();
            cy.get('@row1').eq(0).find('[data-cy=unpinButton]').should('exist');
          }
        }
        cy.get('[data-cy=modalDoneButton]').click();

        const tableSelector = '[data-cy=projectsTable]'
        cy.validateTable(tableSelector, [
          [{ colIndex: 0,  value: 'proj9' }],
          [{ colIndex: 0,  value: 'proj8' }],
          [{ colIndex: 0,  value: 'proj7' }],
          [{ colIndex: 0,  value: 'proj6' }],
          [{ colIndex: 0,  value: 'proj5' }],
          [{ colIndex: 0,  value: 'proj4' }],
          [{ colIndex: 0,  value: 'proj3' }],
          [{ colIndex: 0,  value: 'proj2' }],
          [{ colIndex: 0,  value: 'proj1' }],
          [{ colIndex: 0,  value: 'Inception' }],
        ], 10);

        // unpin from the table
        cy.get('[data-cy="projectsTable-projectFilter"]').type('proj1');
        cy.get('[data-cy="projectsTable-filterBtn"]').click();
        cy.validateTable(tableSelector, [
          [{ colIndex: 0,  value: 'proj1' }],
        ], 10);
        cy.get('[data-cy=unpin]').click();

        // < 10 projects pinned now, so back to project cards
        cy.get(projectsSelector).should('have.length', 9).as('projects');
        cy.contains('Inception');
        cy.contains('proj9');
        cy.contains('proj8');
        cy.contains('proj7');
        cy.contains('proj6');
        cy.contains('proj5');
        cy.contains('proj4');
        cy.contains('proj3');
        cy.contains('proj2');
        cy.contains('proj1').should('not.exist');
      });
    });

    it('Browse projects catalog - many projects', () => {

        for (let i = 0; i < 12; i += 1) {
            cy.request('POST', `/app/projects/proj${i}`, {
                projectId: `proj${i}`,
                name: `Good project ${i}`
            });
        }

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('GET', '/app/projects')
                    .as('default');
                cy.intercept('GET', '/app/projects?search=one')
                    .as('searchOne');
                cy.intercept('POST', '/root/pin/proj1')
                    .as('pinOne');
                cy.intercept('DELETE', '/root/pin/proj1')
                    .as('unpinOne');
                cy.intercept('GET', '/admin/projects/proj1/subjects')
                    .as('loadSubjects');

                cy.visit('/administrator/');
                //confirm that default project loading returns no projects for root user
                cy.wait('@default');
                cy.contains('No Projects Yet...')
                    .should('be.visible');

                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Pin Projects');
                cy.contains('Search Project Catalog');

                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get('[data-cy=pinProjectsSearchResultsNumRows]')
                    .contains('Rows: 13');

                const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr';
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');

                for (let i = 0; i < 5; i += 1) {
                    cy.get('@cyRows')
                        .eq(i)
                        .find('td')
                        .as('row1');
                    cy.get('@row1')
                        .eq(0)
                        .contains(`Good project ${i}`);
                }

                cy.get('[data-cy=pinedResultsPaging]')
                    .contains('2')
                    .click();
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');
                for (let i = 0; i < 5; i += 1) {
                    cy.get('@cyRows')
                        .eq(i)
                        .find('td')
                        .as('row1');
                    cy.get('@row1')
                        .eq(0)
                        .contains(`Good project ${i + 5}`);
                }

                cy.get('[data-cy=pinedResultsPaging]')
                    .contains('3')
                    .click();
                cy.get(rowSelector)
                    .should('have.length', 3)
                    .as('cyRows');
                for (let i = 0; i < 2; i += 1) {
                    cy.get('@cyRows')
                        .eq(i)
                        .find('td')
                        .as('row1');
                    cy.get('@row1')
                        .eq(0)
                        .contains(`Good project ${i + 10}`);
                }

                cy.get('@cyRows')
                    .eq(2)
                    .find('td')
                    .as('row1');
                cy.get('@row1')
                    .eq(0)
                    .contains('Inception');
            });
    });

    it('Close Pin Projects modal using escape and then reopen', () => {
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('GET', '/app/projects')
                    .as('default');
                cy.intercept('GET', '/app/projects?search=one')
                    .as('searchOne');
                cy.intercept('POST', '/root/pin/proj1')
                    .as('pinOne');
                cy.intercept('DELETE', '/root/pin/proj1')
                    .as('unpinOne');
                cy.intercept('GET', '/admin/projects/proj1/subjects')
                    .as('loadSubjects');

                cy.visit('/administrator/');
                //confirm that default project loading returns no projects for root user
                cy.wait('@default');
                cy.contains('No Projects Yet...')
                    .should('be.visible');

                // open the pin projects modal
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.get('[data-cy=pinProjects')
                    .should('exist'); // dialog exists
                cy.contains('Pin Projects');
                cy.contains('Search Project Catalog');

                // close with escape
                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('{esc}', { force: true });
                cy.get('[data-cy=pinProjects')
                    .should('not.exist'); // dialog does not exists

                // can re-open the pin modal
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.get('[data-cy=pinProjects')
                    .should('exist'); // dialog exists
                cy.contains('Pin Projects');
                cy.contains('Search Project Catalog');

                // close with escape
                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('{esc}', { force: true });
                cy.get('[data-cy=pinProjects')
                    .should('not.exist'); // dialog does not exists

                // open the new project modal
                cy.get('[data-cy="newProjectButton"]')
                    .click();
                cy.contains('New Project'); // new project dialog does exist
                cy.get('[data-cy=pinProjects')
                    .should('not.exist'); // pin project dialog does not exists
            });

    });

    it('Sort and then un-sort projects', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: '000'
        });

        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: '100'
        });

        cy.request('POST', '/app/projects/proj3', {
            projectId: 'proj3',
            name: '200'
        });

        cy.request('POST', '/app/projects/proj4', {
            projectId: 'proj4',
            name: '300'
        });
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('GET', '/app/projects')
                    .as('default');
                cy.intercept('GET', '/app/projects?search=one')
                    .as('searchOne');
                cy.intercept('POST', '/root/pin/proj1')
                    .as('pinOne');
                cy.intercept('DELETE', '/root/pin/proj1')
                    .as('unpinOne');
                cy.intercept('GET', '/admin/projects/proj1/subjects')
                    .as('loadSubjects');
                cy.reportSkill('Inception', 'CreateProject', 'user1@skills.org', 'now', false);

                cy.visit('/administrator/');
                //confirm that default project loading returns no projects for root user
                cy.wait('@default');
                cy.contains('No Projects Yet...')
                    .should('be.visible');

                const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr';
                const headerSelector = '[data-cy=pinProjectsSearchResults] thead tr th';

                // load all projects in default (ASC) order
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Search Project Catalog');
                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');

                // verify rows are in ASC order based on project name
                const rowNamesAsc = ['000', '100', '200', '300', 'Inception'];
                for (let i = 0; i < 5; i += 1) {
                    cy.get('@cyRows')
                        .eq(i)
                        .find('td')
                        .as('row-i');
                    cy.get('@row-i')
                        .contains(rowNamesAsc[i]);
                }

                // now click the 'Name' header to sort in DESC order
                cy.get(headerSelector)
                    .contains('Name')
                    .click();

                // verify rows are in DESC order based on project name
                const rowNameDesc = rowNamesAsc.slice(0)
                    .reverse();
                for (let i = 0; i < 5; i += 1) {
                    cy.get('@cyRows')
                        .eq(i)
                        .find('td')
                        .as('row-i');
                    cy.get('@row-i')
                        .contains(rowNameDesc[i]);
                }

                //row names in creation order
                const rowNamesCreationOrderAsc = ['Inception', '000', '100', '200', '300'];
                cy.get(headerSelector)
                    .contains('Created')
                    .click();
                for (let i = 0; i < 5; i += 1) {
                    cy.get('@cyRows')
                        .eq(i)
                        .find('td')
                        .as('row-i');
                    cy.get('@row-i')
                        .contains(rowNamesCreationOrderAsc[i]);
                }
                cy.get(headerSelector)
                    .contains('Name')
                    .should('be.visible');

                cy.log('sorting by Last Reported Skill asc');
                cy.get(headerSelector)
                    .contains('Last Reported Skill')
                    .should('be.visible')
                    .click();
                cy.get('@cyRows')
                    .eq(4)
                    .find('td')
                    .contains('Inception');

                cy.log('sorting by Last Reported Skill desc');
                cy.get(headerSelector)
                    .contains('Last Reported Skill')
                    .click();
                cy.get('@cyRows')
                    .eq(0)
                    .find('td')
                    .contains('Inception');

                cy.get('[data-cy=modalDoneButton]')
                    .click();
            });
    });

    it('Pin Projects button should retain focus after dialog is closed', () => {
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('GET', '/app/projects')
                    .as('default');
                cy.intercept('GET', '/app/projects?search=one')
                    .as('searchOne');
                cy.intercept('POST', '/root/pin/proj1')
                    .as('pinOne');
                cy.intercept('DELETE', '/root/pin/proj1')
                    .as('unpinOne');
                cy.intercept('GET', '/admin/projects/proj1/subjects')
                    .as('loadSubjects');

                cy.visit('/administrator/');
                //confirm that default project loading returns no projects for root user
                cy.wait('@default');
                cy.contains('No Projects Yet...')
                    .should('be.visible');

                // open the pin projects modal
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.get('[data-cy=pinProjects')
                    .should('exist'); // dialog exists
                cy.contains('Pin Projects');
                cy.contains('Search Project Catalog');

                // close with escape
                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('{esc}', { force: true });
                cy.get('[data-cy=pinProjects')
                    .should('not.exist'); // dialog does not exists
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .should('have.focus');

                // can re-open the pin modal
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.get('[data-cy=pinProjects')
                    .should('exist'); // dialog exists
                cy.contains('Pin Projects');
                cy.contains('Search Project Catalog');
                cy.get('[data-cy=modalDoneButton]')
                    .click();
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .should('have.focus');

                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Pin Projects');
                cy.contains('Search Project Catalog');
                cy.get('[aria-label=Close]')
                    .click();
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .should('have.focus');
            });

    });

    it('View project from Pin Projects modal', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'one'
        });
        cy.logout();

        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('GET', '/app/projects')
                    .as('default');
                cy.intercept('GET', '/admin/projects/proj1/subjects')
                    .as('loadSubjects');

                cy.visit('/administrator/');
                cy.wait('@default');

                const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr';

                // view project from "Pin" dialog
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Search Project Catalog');
                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get(rowSelector)
                    .should('have.length', 2)
                    .as('cyRows');
                cy.get('@cyRows')
                    .eq(1)
                    .find('td')
                    .as('row1');
                cy.get('@row1')
                    .contains('one');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=viewProjectButton]')
                    .invoke('removeAttr', 'target')
                    .click();

                cy.url()
                    .should('eq', `${Cypress.config().baseUrl}/administrator/projects/proj1`);
            });
    });

    it('Sort auto-pinned project', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'one'
        });

        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'two'
        });

        cy.request('POST', '/app/projects/proj3', {
            projectId: 'proj3',
            name: 'three'
        });

        cy.request('POST', '/app/projects/proj4', {
            projectId: 'proj4',
            name: 'four'
        });
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('GET', '/app/projects')
                    .as('default');
                cy.intercept('GET', '/app/projects?search=one')
                    .as('searchOne');
                cy.intercept('POST', '/root/pin/proj1')
                    .as('pinOne');
                cy.intercept('DELETE', '/root/pin/proj1')
                    .as('unpinOne');
                cy.intercept('GET', '/admin/projects/proj1/subjects')
                    .as('loadSubjects');

                cy.intercept('GET', '/app/projects')
                    .as('loadProjects');

                cy.visit('/administrator/');
                //confirm that default project loading returns no projects for root user
                cy.wait('@default');
                cy.contains('No Projects Yet...')
                    .should('be.visible');

                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Pin Projects');
                cy.contains('Search Project Catalog');

                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('t');
                cy.get('[data-cy=pinProjectsSearchResultsNumRows]')
                    .contains('Rows: 3');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('three');

                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('wo');
                cy.get('[data-cy=pinProjectsSearchResultsNumRows]')
                    .contains('Rows: 1');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception')
                    .should('not.exist');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');

                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('1');
                cy.get('[data-cy=pinProjects]')
                    .contains('No Results');

                cy.get('[data-cy=pinProjectsClearSearch]')
                    .click();
                cy.get('[data-cy=pinProjects]')
                    .contains('Search Project Catalog');

                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get('[data-cy=pinProjectsSearchResultsNumRows]')
                    .contains('Rows: 5');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('three');

                // pin 1 project
                const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr';
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');
                cy.get('@cyRows')
                    .eq(0)
                    .find('td')
                    .as('row1');
                cy.get('@row1')
                    .eq(0)
                    .contains('four');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=unpinButton]')
                    .should('not.exist');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=pinButton]')
                    .click();
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=pinButton]')
                    .should('not.exist');
                cy.get('@row1')
                    .eq(0)
                    .find('[data-cy=unpinButton]')
                    .should('exist');
                cy.get('[data-cy=modalDoneButton]')
                    .click();

                cy.get('[data-cy=newProjectButton]')
                    .click();
                cy.contains('New Project')
                    .should('be.visible');
                cy.get('[data-cy=projectName]')
                    .type('A Brand New Project');
                cy.get('[data-cy=saveProjectButton]')
                    .click();
                cy.contains('A Brand New Project')
                    .should('be.visible');

                const proj4Card = '[data-cy="projectCard_proj4"] [data-cy="sortControlHandle"]';
                const newProjCard = '[data-cy="projectCard_ABrandNewProject"] [data-cy="sortControlHandle"]';

                cy.get('[data-cy=projectCard]')
                    .eq(0)
                    .should('not.contain', 'A Brand New Project');
                cy.get(proj4Card)
                    .dragAndDrop(newProjCard);
                cy.validateElementsOrder('[data-cy="projectCard"]', ['A Brand New Project', 'four']);

                cy.visit('/administrator/');
                cy.validateElementsOrder('[data-cy="projectCard"]', ['A Brand New Project', 'four']);
            });
    });

});

