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
        window.localStorage.setItem('tableState', JSON.stringify({'PinProjects-table': {'sortDesc': false, 'sortBy': 'name'}}))
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
                cy.get('[data-cy=skillsBTableTotalRows]')
                    .contains('3');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('three');

                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('wo');
                cy.get('[data-cy=skillsBTableTotalRows]')
                    .contains('1');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception')
                    .should('not.exist');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');

                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('1');
                cy.get('[data-cy=pinProjects] [data-cy="pinProjectsSearchResults"]')
                    .contains('No Results');

                cy.get('[data-cy=pinProjectsClearSearch]')
                    .click();
                cy.wait(1000)
                cy.get('[data-cy=pinProjects]')
                    .contains('Search Project Catalog');

                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get('[data-cy="skillsBTableTotalRows"]')
                    .contains('5');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('three');

                // pin 1 project
                const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr';

                const headerSelector = '[data-cy=pinProjectsSearchResults] thead tr th';
                cy.get(headerSelector)
                    .contains('Name')
                    .click();
                cy.wait(1000)
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
                cy.get('[data-cy=closeDialogBtn]')
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
                cy.get('[data-cy=closeDialogBtn]')
                    .click();

                // unpin that project
                cy.get('[data-cy="projectCard_proj4"] [data-cy="unpin"]').click()
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
                cy.get('[data-cy=closeDialogBtn]')
                    .click();

                cy.get(projectsSelector)
                    .should('have.length', 5)
                    .as('projects');
                cy.get('[data-cy="projectCard_Inception"]')
                cy.get('[data-cy="projectCard_proj1"]')
                cy.get('[data-cy="projectCard_proj2"]')
                cy.get('[data-cy="projectCard_proj3"]')
                cy.get('[data-cy="projectCard_proj4"]')

                // unpin from the component
                cy.get('[data-cy=subPageHeaderControls]')
                    .contains('Pin')
                    .click();
                cy.contains('Pin Projects');
                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get('[data-pc-section="columnheadercontent"]').contains('Name').click()
                cy.wait(1000)
                cy.get('[data-cy=skillsBTableTotalRows]')
                    .contains('5');
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

                cy.get('[data-cy=closeDialogBtn]')
                    .click();

                cy.get(projectsSelector)
                    .should('have.length', 4)
                    .as('projects');

                cy.get('[data-cy="projectCard_Inception"]')
                cy.get('[data-cy="projectCard_proj2"]')
                cy.get('[data-cy="projectCard_proj3"]')
                cy.get('[data-cy="projectCard_proj4"]')
                cy.get('[data-cy="projectCard_proj1"]').should('not.exist')
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
        cy.intercept('GET', '/admin/projects/proj1/userRoles**')
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
        cy.wait(500);
        cy.get('[role="option"]').contains('root@skills.org')
            .click();
        cy.selectItem('[data-cy="userRoleSelector"]', 'Administrator');
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
                cy.get(`${tableSelector} [data-cy="removeUserBtn"]`)
                    .eq(0)
                    .click();

                cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove')
                cy.get('[data-cy="currentValidationText"]').type('Delete Me', {delay: 0})
                cy.get('[data-cy="saveDialogBtn"]').click()
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
                cy.get('[data-cy=skillsBTableTotalRows]')
                    .contains('13');

                const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr';
                cy.get(rowSelector)
                    .should('have.length', 5)
                    .as('cyRows');

                const headerSelector = '[data-cy=pinProjectsSearchResults] thead tr th';
                cy.get(headerSelector)
                    .contains('Name')
                    .click();
                cy.wait(1000)

                for (let i = 0; i < 5; i += 1) {
                    cy.get('@cyRows')
                        .eq(i)
                        .find('td')
                        .as('row1');
                    cy.get('@row1')
                        .eq(0)
                        .contains(`Good project ${i}`);
                }

                cy.get('[data-pc-section="pages"]')
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

                cy.get('[data-pc-section="pages"]')
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
                cy.get(headerSelector)
                    .contains('Name')
                    .click();
                cy.wait(1000)

                const tableSelector = '[data-cy="pinProjectsSearchResults"]';
                cy.validateTable(tableSelector, [
                    [{
                        colIndex: 0,
                        value: '000'
                    }],
                    [{
                        colIndex: 0,
                        value: '100'
                    }],
                    [{
                        colIndex: 0,
                        value: '200'
                    }],
                    [{
                        colIndex: 0,
                        value: '300'
                    }],
                    [{
                        colIndex: 0,
                        value: 'Inception'
                    }],
                ], 5);

                // now click the 'Name' header to sort in DESC order
                cy.get(headerSelector)
                    .contains('Name')
                    .click();
                cy.wait(1000)
                cy.validateTable(tableSelector, [
                    [{
                        colIndex: 0,
                        value: 'Inception'
                    }],
                    [{
                        colIndex: 0,
                        value: '300'
                    }],
                    [{
                        colIndex: 0,
                        value: '200'
                    }],
                    [{
                        colIndex: 0,
                        value: '100'
                    }],
                    [{
                        colIndex: 0,
                        value: '000'
                    }],
                ], 5);

                //row names in creation order
                cy.get(headerSelector)
                    .contains('Created')
                    .click();

                cy.validateTable(tableSelector, [
                    [{
                        colIndex: 0,
                        value: 'Inception'
                    }],
                    [{
                        colIndex: 0,
                        value: '000'
                    }],
                    [{
                        colIndex: 0,
                        value: '100'
                    }],
                    [{
                        colIndex: 0,
                        value: '200'
                    }],
                    [{
                        colIndex: 0,
                        value: '300'
                    }],
                ], 5);

                cy.get(headerSelector)
                    .contains('Name')
                    .should('be.visible');

                cy.log('sorting by Last Reported Skill asc');
                cy.get(headerSelector)
                    .contains('Last Reported Skill')
                    .should('be.visible')
                    .click();

                cy.validateTable(tableSelector, [
                    [{
                        colIndex: 2,
                        value: 'never'
                    }],
                    [{
                        colIndex: 2,
                        value: 'never'
                    }],
                    [{
                        colIndex: 2,
                        value: 'never'
                    }],
                    [{
                        colIndex: 2,
                        value: 'never'
                    }],
                    [{
                        colIndex: 2,
                        value: 'today'
                    }],
                ], 5);

                cy.log('sorting by Last Reported Skill desc');
                cy.get(headerSelector)
                    .contains('Last Reported Skill')
                    .click();

                cy.validateTable(tableSelector, [
                  [{
                    colIndex: 2,
                    value: 'today'
                  }],
                  [{
                    colIndex: 2,
                    value: 'never'
                  }],
                  [{
                    colIndex: 2,
                    value: 'never'
                  }],
                  [{
                    colIndex: 2,
                    value: 'never'
                  }],
                  [{
                    colIndex: 2,
                    value: 'never'
                  }],
                ], 5);

                cy.get('[data-cy=closeDialogBtn]')
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
                cy.get('[data-cy=closeDialogBtn]')
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
              const headerSelector = '[data-cy=pinProjectsSearchResults] thead tr th';
              cy.get(headerSelector)
                .contains('Name')
                .click();
                cy.wait(1000)
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
                cy.get('[data-cy="skillsBTableTotalRows"]')
                    .contains('3');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('three');

                cy.get('[data-cy=pinProjectsSearchInput]')
                    .type('wo');
                cy.get('[data-cy="skillsBTableTotalRows"]')
                    .contains('1');
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
                cy.wait(1000)
                cy.get('[data-cy=pinProjects]')
                    .contains('Search Project Catalog');

                cy.get('[data-cy=pinProjectsLoadAllButton]')
                    .click();
                cy.get('[data-cy=skillsBTableTotalRows]')
                    .contains('5');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('Inception');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('two');
                cy.get('[data-cy=pinProjectsSearchResults]')
                    .contains('three');

                const headerSelector = '[data-cy=pinProjectsSearchResults] thead tr th';
                cy.get(headerSelector)
                    .contains('Name')
                    .click();
                cy.wait(1000)
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
                cy.get('[data-cy=closeDialogBtn]')
                    .click();

                cy.get('[data-cy=newProjectButton]')
                    .click();
                cy.contains('New Project')
                    .should('be.visible');
                cy.get('[data-cy=projectName]')
                    .type('A Brand New Project');
                cy.get('[data-cy=saveDialogBtn]')
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

  it('Pin button should be displayed after login', () => {
    cy.createProject(1)
    cy.logout()
    cy.fixture('vars.json')
      .then((vars) => {
        cy.visit('/administrator/')
        cy.get('#username').type(vars.rootUser)
        cy.get('#inputPassword').type(vars.defaultPass)
        cy.get('[data-cy="login"]').click()

        cy.get('[data-cy="pinProjectsButton"]')
      })
  });

});

