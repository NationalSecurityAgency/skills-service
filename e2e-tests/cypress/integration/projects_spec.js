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
describe('Projects Tests', () => {
  beforeEach(() => {
    cy.server()
      .route('GET', '/app/projects').as('getProjects')
      .route('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
      .route('GET', '/app/userInfo').as('getUserInfo')
      .route('/admin/projects/proj1/users/root@skills.org/roles').as('getRolesForRoot');
  });

  it('Create new projects', function () {
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.route('POST', '/app/projects/MyNewtestProject').as('postNewProject');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.clickButton('Project');
    cy.get('[data-cy="projectName"]').type("My New test Project")
    cy.clickSave();

    cy.wait('@postNewProject');

    cy.contains('My New test Project')
    cy.contains('ID: MyNewtestProject')
  });

  it('Close new project dialog', () => {
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.route('POST', '/app/projects/MyNewtestProject').as('postNewProject');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.clickButton('Project');
    cy.get('[data-cy=closeProjectButton]').click();
    cy.get('[data-cy="projectName"]').should('not.be.visible');
  });

  it('Duplicate project names are not allowed', () => {
    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
    })
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.clickButton('Project');
    cy.get('[data-cy="projectName"]').type("My New test Project")
    cy.get('[data-cy=projectNameError]').contains('The value for the Project Name is already taken').should('be.visible')
    cy.get('[data-cy=saveProjectButton]').should('be.disabled');
  });


  it('Duplicate project ids are not allowed', () => {
    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
    })
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');
    cy.clickButton('Project');
    cy.get('[data-cy="projectName"]').type("Other Project Name")
    cy.contains('Enable').click();
    cy.getIdField().clear().type("MyNewtestProject")

    cy.get('[data-cy=idError]').contains('The value for the Project ID is already taken').should('be.visible');
    cy.get('[data-cy=saveProjectButton]').should('be.disabled');
  });

  it('Project id autofill strips out special characters and spaces', () => {
    const expectedId = 'LotsofspecialPchars';
    const providedName = "!L@o#t$s of %s^p&e*c(i)a_l++_|}/[]#?{P c'ha'rs";

    cy.route('POST', `/app/projects/${expectedId}`).as('postNewProject');
    cy.route('POST', '/app/projectExist').as('projectExists');
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');
    cy.clickButton('Project');
    cy.get('[data-cy="projectName"]').type(providedName);
    cy.wait('@projectExists');
    cy.getIdField().should('have.value', expectedId)

    cy.clickSave();
    cy.wait('@postNewProject');

    cy.contains(`ID: ${expectedId}`)
  });

  it('Validate that cannot create project with the same name in lowercase', () => {
    const expectedId = 'TestProject1';
    const providedName = "Test Project #1";

    cy.route('POST', `/app/projects/${expectedId}`)
        .as('postNewProject');

    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');
    cy.clickButton('Project');
    cy.get('[data-cy="projectName"]').type(providedName)
    cy.getIdField().should('have.value', expectedId)

    cy.clickSave();
    cy.wait('@postNewProject');

    cy.clickButton('Project');
    cy.get('[data-cy="projectName"]').type(providedName.toLowerCase())

    cy.get('[data-cy=projectNameError').contains('The value for the Project Name is already taken').should('be.visible');

    cy.get('[data-cy=saveProjectButton]').should('be.disabled');
  });

  it('Once project id is enabled name-to-id autofill should be turned off', () => {
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.clickButton('Project');;
    cy.get('[data-cy="projectName"]').type('InitValue');
    cy.getIdField().should('have.value', 'InitValue');

    cy.contains('Enable').click();
    cy.contains('Enabled').not('a');

    cy.get('[data-cy="projectName"]').type('MoreValue');
    cy.getIdField().should('have.value', 'InitValue');

    cy.get('[data-cy="projectName"]').clear();
    cy.getIdField().should('have.value', 'InitValue');
  });

  it('Project name is required', () => {
    cy.server();
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');
    cy.clickButton('Project');
    cy.contains('Enable').click();
    cy.getIdField().type('InitValue');

    cy.get('[data-cy=saveProjectButton').should('be.disabled');
  })

  it('Project id is required', () => {
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');
    cy.clickButton('Project');;
    cy.get('[data-cy="projectName"]').type('New Project');
    cy.contains('Enable').click();
    cy.getIdField().clear()
    cy.get('[data-cy=idError]').contains('Project ID is required').should('be.visible');
    cy.get('[data-cy=saveProjectButton').should('be.disabled');
  })


  it('Project name must be > 3 chars < 50 chars', () => {
    const minLenMsg = 'Project Name cannot be less than 3 characters';
    const maxLenMsg = 'Project Name cannot exceed 50 characters';
    const projId = 'ProjectId'
    cy.route('POST', `/app/projects/${projId}`).as('postNewProject');
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.clickButton('Project');;
    cy.contains('Enable').click();
    cy.getIdField().type('ProjectId')
    cy.get('[data-cy="projectName"]').type('12');
    cy.contains(minLenMsg)

    cy.get('[data-cy="projectName"]').type('3');
    cy.contains(minLenMsg).should('not.exist')

    const longInvalid = Array(51).fill('a').join('');
    const longValid = Array(50).fill('a').join('');

    cy.get('[data-cy="projectName"]').clear().type(longInvalid);
    cy.contains(maxLenMsg)

    cy.get('[data-cy="projectName"]').clear().type(longValid);
    cy.contains(maxLenMsg).should('not.exist')

    cy.clickSave();
    cy.wait('@postNewProject');

    cy.contains(`ID: ${projId}`)
    cy.contains(longValid)
  })

  it('Project ID must be > 3 chars < 50 chars', () => {
    const minLenMsg = 'Project ID cannot be less than 3 characters';
    const maxLenMsg = 'Project ID cannot exceed 50 characters';
    const projName = 'Project Name'

    const longInvalid = Array(51).fill('a').join('');
    const longValid = Array(50).fill('a').join('');
    cy.route('POST', `/app/projects/${longValid}`).as('postNewProject');
    cy.route('GET', '/app/projects').as('loadProjects');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');
    cy.clickButton('Project');;
    cy.contains('Enable').click();
    cy.getIdField().type('12')
    cy.get('[data-cy="projectName"]').type(projName);
    cy.contains(minLenMsg)

    cy.getIdField().type('3');
    cy.contains(minLenMsg).should('not.exist')

    cy.getIdField().clear().click()
    cy.getIdField().invoke('val', longInvalid).trigger('input');
    cy.contains(maxLenMsg)

    cy.getIdField().clear().click().invoke('val', longValid).trigger('input');
    cy.contains(maxLenMsg).should('not.exist')

    cy.clickSave();
    cy.wait('@postNewProject');

    cy.contains('ID: aaaaa')
  })

  it('Add Admin - User Not Found', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });

    cy.route({
      method: 'PUT',
      url: '/admin/projects/proj1/users/foo/roles/ROLE_PROJECT_ADMIN',
      status: 400,
      response: {errorCode: 'UserNotFound', explanation: 'User was not found'}
    }).as('addAdmin');

    cy.route({
      method: 'POST',
      url: '/app/users/suggest*',
      status: 200,
      response: [{userId:'foo', userIdForDisplay: 'foo', first: 'foo', last: 'foo', dn: 'foo'}]
    }).as('suggest');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');
    cy.route('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.contains('Enter user id').type('foo');
    cy.wait('@suggest');
    cy.get('.multiselect__input').type('{enter}');
    cy.clickButton('Add');
    cy.wait('@addAdmin');
    cy.get('.alert-danger').contains('User was not found');
  });

  it('Add Admin - InternalError', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });

    cy.route({
      method: 'PUT',
      url: '/admin/projects/proj1/users/foo/roles/ROLE_PROJECT_ADMIN',
      status: 400,
      response: {errorCode: 'InternalError', explanation: 'Some Error Occurred'}
    }).as('addAdmin');

    cy.route({
      method: 'POST',
      url: '/app/users/suggest*',
      status: 200,
      response: [{userId:'foo', userIdForDisplay: 'foo', first: 'foo', last: 'foo', dn: 'foo'}]
    }).as('suggest');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');
    cy.route('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.contains('Enter user id').type('foo');
    cy.wait('@suggest');
    cy.get('.multiselect__input').type('{enter}');
    cy.clickButton('Add');
    cy.wait('@addAdmin');
    cy.get('h4').contains('Tiny-bit of an error!');
  });

  it('Add Admin No Query', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });

    cy.route({
      method: 'PUT',
      url: '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN',
    }).as('addAdmin');

    cy.route({
      method: 'POST',
      url: '/app/users/suggestDashboardUsers*',
    }).as('suggest');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');
    cy.route('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.contains('Enter user id').type('{enter}');
    cy.wait('@suggest');
    cy.contains('root@skills.org').click();
    cy.clickButton('Add');
    cy.wait('@addAdmin');
    cy.wait('@getRolesForRoot');
    cy.contains('Firstname LastName (root@skills.org)').should('exist');
  });

  it('Add Admin', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });

    cy.route({
      method: 'PUT',
      url: '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN',
    }).as('addAdmin');

    cy.route({
      method: 'POST',
      url: '/app/users/suggestDashboardUsers*',
    }).as('suggest');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');
    cy.route('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.contains('Enter user id').type('root{enter}');
    cy.wait('@suggest');
    cy.contains('root@skills.org').click();
    cy.clickButton('Add');
    cy.wait('@addAdmin');
    cy.wait('@getRolesForRoot');
    cy.contains('Firstname LastName (root@skills.org)')
  });

  it('Add Admin - forward slash character does not cause error', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });

    cy.route({
      method: 'PUT',
      url: '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN',
    }).as('addAdmin');

    cy.route({
      method: 'POST',
      url: '/app/users/suggestDashboardUsers*',
    }).as('suggest');
    cy.route('GET', '/app/userInfo').as('loadUserInfo');
    cy.route('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.contains('Enter user id').type('root/foo{enter}');
    cy.wait('@suggest');
  });

  it('Root User - Pin and Unpin projects', () => {

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
      cy.get('@row1').eq(0).contains('Inception');
      cy.get('@row1').eq(3).find('[data-cy=pinedButtonIndicator]').should('not.exist');
      cy.get('@row1').eq(3).find('[data-cy=pinButton]').click();
      cy.get('@row1').eq(3).find('[data-cy=pinButton]').should('not.exist');
      cy.get('@row1').eq(3).find('[data-cy=pinedButtonIndicator]').should('exist');
      cy.get('[data-cy=modalDoneButton]').click();

      const projectsSelector = '[data-cy=projectCard]';
      cy.get(projectsSelector).should('have.length', 1).as('projects');
      cy.get('@projects').eq(0).contains('Inception');

      // make sure the pinned project is filtered
      cy.get('[data-cy=subPageHeaderControls]').contains('Pin').click();
      cy.get('[data-cy=pinProjectsLoadAllButton]').click();
      cy.get(rowSelector).should('have.length', 4).as('cyRows');
      cy.get('@cyRows').eq(0).find('td').as('row1');
      cy.get('@row1').eq(0).contains('one');
      cy.get('[data-cy=modalDoneButton]').click();

      // unpin that project
      cy.get('@projects').eq(0).contains('Unpin').click();
      cy.contains('No Projects Yet');

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
            .eq(3)
            .find('[data-cy=pinButton]')
            .click();
        cy.get('@row1').eq(3).find('[data-cy=pinedButtonIndicator]').should('exist');
      }
      cy.get('[data-cy=modalDoneButton]').click();

      cy.get(projectsSelector).should('have.length', 5).as('projects');
      cy.get('@projects').eq(0).contains('Inception');
      cy.get('@projects').eq(1).contains('one');
      cy.get('@projects').eq(2).contains('two');
      cy.get('@projects').eq(3).contains('three');
      cy.get('@projects').eq(4).contains('four');
    });
  });

  it('Root User - Pin and Unpin projects - many projects', () => {

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

      cy.get('@cyRows').eq(0).find('td').as('row1');
      cy.get('@row1').eq(0).contains('Inception');

      for (let i = 1; i <= 4; i += 1) {
        cy.get('@cyRows').eq(i).find('td').as('row1');
        cy.get('@row1').eq(0).contains(`Good project ${i-1}`);
      }

      cy.get('[data-cy=pinedResultsPaging]').contains('2').click();
      cy.get(rowSelector).should('have.length', 5).as('cyRows');
      for (let i = 0; i < 5; i += 1) {
        cy.get('@cyRows').eq(i).find('td').as('row1');
        cy.get('@row1').eq(0).contains(`Good project ${i+4}`);
      }

      cy.get('[data-cy=pinedResultsPaging]').contains('3').click();
      cy.get(rowSelector).should('have.length', 3).as('cyRows');
      for (let i = 0; i < 3; i += 1) {
        cy.get('@cyRows').eq(i).find('td').as('row1');
        cy.get('@row1').eq(0).contains(`Good project ${i+9}`);
      }
    });
  });
});

