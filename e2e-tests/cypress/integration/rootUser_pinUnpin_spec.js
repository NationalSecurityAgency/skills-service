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
    cy.server()
      .route('GET', '/app/projects').as('getProjects')
      .route('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
      .route('GET', '/app/userInfo').as('getUserInfo')
      .route('/admin/projects/proj1/users/root@skills.org/roles').as('getRolesForRoot');
  });

  it('Pin and Unpin projects', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "one"
    });

    cy.request('POST', '/app/projects/proj2', {
      projectId: 'proj2',
      name: "two"
    });

    cy.request('POST', '/app/projects/proj3', {
      projectId: 'proj3',
      name: "three"
    });

    cy.request('POST', '/app/projects/proj4', {
      projectId: 'proj4',
      name: "four"
    });
    cy.logout();
    cy.fixture('vars.json').then((vars) => {
      cy.login(vars.rootUser, vars.defaultPass);
      cy.route('GET', '/app/projects').as('default');
      cy.route('GET', '/app/projects?search=one').as('searchOne');
      cy.route('POST', '/root/pin/proj1').as('pinOne');
      cy.route('DELETE', '/root/pin/proj1').as('unpinOne');
      cy.route('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

      cy.visit('/');
      //confirm that default project loading returns no projects for root user
      cy.wait('@default');
      cy.contains('No Projects Yet...').should('be.visible');

      cy.get('[data-cy=subPageHeaderControls]').contains('Pin').click();
      cy.contains('Pin Projects');
      cy.contains('Search Project Catalog');

      cy.get('[data-cy=pinProjectsSearchInput]').type('t');
      cy.get('[data-cy=pinProjectsSearchResultsNumRows]').contains('Rows: 3');
      cy.get('[data-cy=pinProjectsSearchResults]').contains('Inception');
      cy.get('[data-cy=pinProjectsSearchResults]').contains('two');
      cy.get('[data-cy=pinProjectsSearchResults]').contains('three');

      cy.get('[data-cy=pinProjectsSearchInput]').type('wo');
      cy.get('[data-cy=pinProjectsSearchResultsNumRows]').contains('Rows: 1');
      cy.get('[data-cy=pinProjectsSearchResults]').contains('Inception').should('not.exist');
      cy.get('[data-cy=pinProjectsSearchResults]').contains('two');

      cy.get('[data-cy=pinProjectsSearchInput]').type('1');
      cy.get('[data-cy=pinProjects]').contains('No Results');

      cy.get('[data-cy=pinProjectsClearSearch]').click();
      cy.get('[data-cy=pinProjects]').contains('Search Project Catalog');

      cy.get('[data-cy=pinProjectsLoadAllButton]').click();
      cy.get('[data-cy=pinProjectsSearchResultsNumRows]').contains('Rows: 5');
      cy.get('[data-cy=pinProjectsSearchResults]').contains('Inception');
      cy.get('[data-cy=pinProjectsSearchResults]').contains('two');
      cy.get('[data-cy=pinProjectsSearchResults]').contains('three');

      // pin 1 project
      const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr'
      cy.get(rowSelector).should('have.length', 5).as('cyRows');
      cy.get('@cyRows').eq(0).find('td').as('row1');
      cy.get('@row1').eq(0).contains('four');
      cy.get('@row1').eq(0).find('[data-cy=unpinButton]').should('not.exist');
      cy.get('@row1').eq(0).find('[data-cy=pinButton]').click();
      cy.get('@row1').eq(0).find('[data-cy=pinButton]').should('not.exist');
      cy.get('@row1').eq(0).find('[data-cy=unpinButton]').should('exist');
      cy.get('[data-cy=modalDoneButton]').click();

      const projectsSelector = '[data-cy=projectCard]';
      cy.get(projectsSelector).should('have.length', 1).as('projects');
      cy.get('@projects').eq(0).contains('four');

      // make sure the pinned project is still pinned
      cy.get('[data-cy=subPageHeaderControls]').contains('Pin').click();
      cy.get('[data-cy=pinProjectsLoadAllButton]').click();
      cy.get(rowSelector).should('have.length', 5).as('cyRows');
      cy.get('@cyRows').eq(0).find('td').as('row1');
      cy.get('@row1').eq(0).find('[data-cy=pinButton]').should('not.exist');
      cy.get('@row1').eq(0).find('[data-cy=unpinButton]').should('exist');
      cy.get('[data-cy=modalDoneButton]').click();

      // unpin that project
      cy.get('@projects').eq(0).contains('Unpin').click();
      cy.contains('No Projects Yet');
    });
  });

  it('Pin all projects then unpin 1', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "one"
    });

    cy.request('POST', '/app/projects/proj2', {
      projectId: 'proj2',
      name: "two"
    });

    cy.request('POST', '/app/projects/proj3', {
      projectId: 'proj3',
      name: "three"
    });

    cy.request('POST', '/app/projects/proj4', {
      projectId: 'proj4',
      name: "four"
    });
    cy.logout();
    cy.fixture('vars.json').then((vars) => {
      cy.login(vars.rootUser, vars.defaultPass);
      cy.route('GET', '/app/projects').as('default');
      cy.route('GET', '/app/projects?search=one').as('searchOne');
      cy.route('POST', '/root/pin/proj1').as('pinOne');
      cy.route('DELETE', '/root/pin/proj1').as('unpinOne');
      cy.route('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

      cy.visit('/');
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
      cy.get('[data-cy=modalDoneButton]').click();

      cy.get(projectsSelector).should('have.length', 5).as('projects');
      cy.contains('ID: Inception');
      cy.contains('one');
      cy.contains('two');
      cy.contains('three');
      cy.contains('four');

      // unpin from the component
      cy.get('[data-cy=subPageHeaderControls]').contains('Pin').click();
      cy.contains('Pin Projects');
      cy.get('[data-cy=pinProjectsLoadAllButton]').click();
      cy.get('[data-cy=pinProjectsSearchResultsNumRows]').contains('Rows: 5');
      cy.get(rowSelector).should('have.length', 5).as('cyRows');
      cy.get('@cyRows').eq(2).find('td').as('row1');
      cy.get('@row1').eq(0).find('[data-cy=pinButton]').should('not.exist');
      cy.get('@row1').eq(0).find('[data-cy=unpinButton]').should('exist');
      cy.get('@row1').eq(0).find('[data-cy=unpinButton]').click();
      cy.get('@row1').eq(0).find('[data-cy=pinButton]').should('exist');
      cy.get('@row1').eq(0).find('[data-cy=unpinButton]').should('not.exist');

      cy.get('[data-cy=modalDoneButton]').click();

      cy.get(projectsSelector).should('have.length', 4).as('projects');
      cy.contains('Inception');
      cy.contains('two');
      cy.contains('one').should('not.exist');
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
    cy.fixture('vars.json').then((vars) => {
      cy.login(vars.rootUser, vars.defaultPass);
      cy.route('GET', '/app/projects').as('default');
      cy.route('GET', '/app/projects?search=one').as('searchOne');
      cy.route('POST', '/root/pin/proj1').as('pinOne');
      cy.route('DELETE', '/root/pin/proj1').as('unpinOne');
      cy.route('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

      cy.visit('/');
      //confirm that default project loading returns no projects for root user
      cy.wait('@default');
      cy.contains('No Projects Yet...').should('be.visible');

      cy.get('[data-cy=subPageHeaderControls]').contains('Pin').click();
      cy.contains('Pin Projects');
      cy.contains('Search Project Catalog');

      cy.get('[data-cy=pinProjectsLoadAllButton]').click();
      cy.get('[data-cy=pinProjectsSearchResultsNumRows]').contains('Rows: 13');

      const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr'
      cy.get(rowSelector).should('have.length', 5).as('cyRows');

      for (let i = 0; i < 5; i += 1) {
        cy.get('@cyRows').eq(i).find('td').as('row1');
        cy.get('@row1').eq(0).contains(`Good project ${i}`);
      }

      cy.get('[data-cy=pinedResultsPaging]').contains('2').click();
      cy.get(rowSelector).should('have.length', 5).as('cyRows');
      for (let i = 0; i < 5; i += 1) {
        cy.get('@cyRows').eq(i).find('td').as('row1');
        cy.get('@row1').eq(0).contains(`Good project ${i+5}`);
      }

      cy.get('[data-cy=pinedResultsPaging]').contains('3').click();
      cy.get(rowSelector).should('have.length', 3).as('cyRows');
      for (let i = 0; i < 2; i += 1) {
        cy.get('@cyRows').eq(i).find('td').as('row1');
        cy.get('@row1').eq(0).contains(`Good project ${i+10}`);
      }

      cy.get('@cyRows').eq(2).find('td').as('row1');
      cy.get('@row1').eq(0).contains('Inception');
    });
  });

  it('Close Pin Projects modal using escape and then reopen', () => {
    cy.logout();
    cy.fixture('vars.json').then((vars) => {
      cy.login(vars.rootUser, vars.defaultPass);
      cy.route('GET', '/app/projects').as('default');
      cy.route('GET', '/app/projects?search=one').as('searchOne');
      cy.route('POST', '/root/pin/proj1').as('pinOne');
      cy.route('DELETE', '/root/pin/proj1').as('unpinOne');
      cy.route('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

      cy.visit('/');
      //confirm that default project loading returns no projects for root user
      cy.wait('@default');
      cy.contains('No Projects Yet...').should('be.visible');

      // open the pin projects modal
      cy.get('[data-cy=subPageHeaderControls]').contains('Pin').click();
      cy.get('[data-cy=pinProjects').should('exist') // dialog exists
      cy.contains('Pin Projects');
      cy.contains('Search Project Catalog');

      // close with escape
      cy.get('[data-cy=pinProjectsSearchInput]').type('{esc}', {force: true});
      cy.get('[data-cy=pinProjects').should('not.exist') // dialog does not exists

      // can re-open the pin modal
      cy.get('[data-cy=subPageHeaderControls]').contains('Pin').click();
      cy.get('[data-cy=pinProjects').should('exist') // dialog exists
      cy.contains('Pin Projects');
      cy.contains('Search Project Catalog');

      // close with escape
      cy.get('[data-cy=pinProjectsSearchInput]').type('{esc}', {force: true});
      cy.get('[data-cy=pinProjects').should('not.exist') // dialog does not exists

      // open the new project modal
      cy.clickButton('Project');
      cy.contains('New Project'); // new project dialog does exist
      cy.get('[data-cy=pinProjects').should('not.exist') // pin project dialog does not exists
    });

  });

  it.only('Sort and then un-sort projects', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "000"
    });

    cy.request('POST', '/app/projects/proj2', {
      projectId: 'proj2',
      name: "100"
    });

    cy.request('POST', '/app/projects/proj3', {
      projectId: 'proj3',
      name: "200"
    });

    cy.request('POST', '/app/projects/proj4', {
      projectId: 'proj4',
      name: "300"
    });
    cy.logout();
    cy.fixture('vars.json').then((vars) => {
      cy.login(vars.rootUser, vars.defaultPass);
      cy.route('GET', '/app/projects').as('default');
      cy.route('GET', '/app/projects?search=one').as('searchOne');
      cy.route('POST', '/root/pin/proj1').as('pinOne');
      cy.route('DELETE', '/root/pin/proj1').as('unpinOne');
      cy.route('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

      cy.visit('/');
      //confirm that default project loading returns no projects for root user
      cy.wait('@default');
      cy.contains('No Projects Yet...').should('be.visible');

      const rowSelector = '[data-cy=pinProjectsSearchResults] tbody tr'
      const headerSelector = '[data-cy=pinProjectsSearchResults] thead tr th'

      // load all projects in default (ASC) order
      cy.get('[data-cy=subPageHeaderControls]').contains('Pin').click();
      cy.contains('Search Project Catalog');
      cy.get('[data-cy=pinProjectsLoadAllButton]').click();
      cy.get(rowSelector).should('have.length', 5).as('cyRows');

      // verify rows are in ASC order based on project name
      const rowNamesAsc = ['000', '100', '200', '300', 'Inception']
      for (let i = 0; i < 5; i += 1) {
        cy.get('@cyRows')
          .eq(i)
          .find('td')
          .as('row-i');
        cy.get('@row-i').contains(rowNamesAsc[i])
      }

      // now click the 'Name' header to sort in DESC order
      cy.get(headerSelector).contains('Name').click()

      // verify rows are in DESC order based on project name
      const rowNameDesc = rowNamesAsc.reverse()
      for (let i = 0; i < 5; i += 1) {
        cy.get('@cyRows')
          .eq(i)
          .find('td')
          .as('row-i');
        cy.get('@row-i').contains(rowNameDesc[i])
      }

      // finally click a non-sortable column and sort order is reset and 'Name' column should still be visible
      cy.get(headerSelector).contains('Subjects').click()
      cy.get(headerSelector).contains('Name').should('be.visible')

      // verify the order did not change
      for (let i = 0; i < 5; i += 1) {
        cy.get('@cyRows')
          .eq(i)
          .find('td')
          .as('row-i');
        cy.get('@row-i').contains(rowNameDesc[i])
      }

      cy.get('[data-cy=modalDoneButton]').click();

    });
  });



});

