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
    cy.intercept('GET', '/app/projects').as('getProjects')
    cy.intercept('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
    cy.intercept('GET', '/app/userInfo').as('getUserInfo')
    cy.intercept('/admin/projects/proj1/users/root@skills.org/roles').as('getRolesForRoot');
  });

  it('Create new projects', function () {
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.intercept('POST', '/app/projects/MyNewtestProject').as('postNewProject');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.clickButton('Project');
    cy.get('[data-cy="projectName"]').type("My New test Project")
    cy.clickSave();

    cy.wait('@postNewProject');

    cy.contains('My New test Project')
    cy.contains('ID: MyNewtestProject')
  });


  it('Create new project using enter key', function () {
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.intercept('POST', '/app/projects/MyNewtestProject').as('postNewProject');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.clickButton('Project');
    cy.get('[data-cy="projectName"]').type("My New test Project")
    cy.get('[data-cy="projectName"]').type('{enter}')

    cy.wait('@postNewProject');

    cy.contains('My New test Project')
    cy.contains('ID: MyNewtestProject')
  });

  it('Close new project dialog', () => {
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.intercept('POST', '/app/projects/MyNewtestProject').as('postNewProject');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.clickButton('Project');
    cy.get('[data-cy=closeProjectButton]').click();
    cy.get('[data-cy="projectName"]').should('not.exist');
  });

  it('Duplicate project names are not allowed', () => {
    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
    })
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
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
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
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

    cy.intercept('POST', `/app/projects/${expectedId}`).as('postNewProject');
    cy.intercept('POST', '/app/projectExist').as('projectExists');
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
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

    cy.intercept('POST', `/app/projects/${expectedId}`)
        .as('postNewProject');

    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
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
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
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
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');
    cy.clickButton('Project');
    cy.contains('Enable').click();
    cy.getIdField().type('InitValue');

    cy.get('[data-cy=saveProjectButton').should('be.disabled');
  })

  it('Project id is required', () => {
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
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
    cy.intercept('POST', `/app/projects/${projId}`).as('postNewProject');
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
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
    const requiredMsg = 'Project ID is required';
    const projName = 'Project Name'

    const longInvalid = Array(51).fill('a').join('');
    const longValid = Array(50).fill('a').join('');
    cy.intercept('POST', `/app/projects/${longValid}`).as('postNewProject');
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');
    cy.clickButton('Project');;
    cy.contains('Enable').click();
    cy.getIdField().type('12')
    cy.get('[data-cy="projectName"]').type(projName);
    cy.contains(minLenMsg)

    cy.getIdField().type('3');
    cy.contains(minLenMsg).should('not.exist')

    cy.getIdField().clear();
    cy.contains(requiredMsg);
    cy.getIdField().click()
    cy.getIdField().invoke('val', longInvalid).trigger('input');
    cy.contains(maxLenMsg)

    cy.getIdField().clear();
    cy.contains(requiredMsg);
    cy.getIdField().click().invoke('val', longValid).trigger('input');
    cy.contains(maxLenMsg).should('not.exist')
    cy.contains(requiredMsg).should('not.exist')

    cy.clickSave();
    cy.wait('@postNewProject');

    cy.contains('ID: aaaaa')
  })

  it('Add Admin - User Not Found', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });

    cy.intercept({
      method: 'PUT',
      path: '/admin/projects/proj1/users/bar/roles/ROLE_PROJECT_ADMIN',
    }, {
      statusCode: 400,
      body: {errorCode: 'UserNotFound', explanation: 'User was not found'}
    }).as('addAdmin');

    cy.intercept({
      method: 'POST',
      path: '/app/users/suggest*',
    }, {
      statusCode: 200,
      body: [{userId:'bar', userIdForDisplay: 'bar', first: 'bar', last: 'bar', dn: 'bar'}]
    }).as('suggest');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
    cy.intercept('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/administrator/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.contains('Enter user id').type('bar');
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

    cy.intercept({
      method: 'PUT',
      path: '/admin/projects/proj1/users/bar/roles/ROLE_PROJECT_ADMIN',
    }, {
      statusCode: 400,
      body: {errorCode: 'InternalError', explanation: 'Some Error Occurred'}
    }).as('addAdmin');

    cy.intercept({
      method: 'POST',
      path: '/app/users/suggest*',
    }, {
      statusCode: 200,
      body: [{userId:'bar', userIdForDisplay: 'bar', first: 'bar', last: 'bar', dn: 'bar'}]
    }).as('suggest');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
    cy.intercept('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/administrator/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.contains('Enter user id').type('bar');
    cy.wait('@suggest');
    cy.get('.multiselect__input').type('{enter}');
    cy.clickButton('Add');
    cy.wait('@addAdmin');
    cy.get('[data-cy="errorPage"]').contains('Tiny-bit of an error!');
  });

  it('Add Admin No Query', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });

    cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN').as('addAdmin');

    cy.intercept('POST',  'suggestDashboardUsers').as('suggest');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
    cy.intercept('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/administrator/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.contains('Enter user id').type('{enter}');
    cy.wait('@suggest');
    cy.contains('root@skills.org').click();
    cy.clickButton('Add');
    cy.wait('@addAdmin');
    cy.wait('@getRolesForRoot');

    const rowSelector = '[data-cy=roleManagerTable] tbody tr'
    cy.get(rowSelector).should('have.length', 2).as('cyRows');
    cy.get('@cyRows').eq(1).find('td').as('row2');
    cy.get('@row2').eq(0).contains('root@skills.org');
  });

  it('Add and Remove Admin', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });


    cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN').as('addAdmin');

    cy.intercept('POST',  'suggestDashboardUsers').as('suggest');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
    cy.intercept('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/administrator/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.get('[data-cy="existingUserInput"]').type('root');
    cy.wait('@suggest');
    cy.contains('root@skills.org').click();
    cy.clickButton('Add');
    cy.wait('@addAdmin');
    cy.wait('@getRolesForRoot');

    const tableSelector = '[data-cy=roleManagerTable]'
    const rowSelector = `${tableSelector} tbody tr`
    cy.get(rowSelector).should('have.length', 2).as('cyRows');
    cy.get('@cyRows').eq(1).find('td').as('row2');
    cy.get('@row2').eq(0).contains('root@skills.org');

    // remove the other user now
    cy.get(`${tableSelector} [data-cy="removeUserBtn"]`).eq(1).click();
    cy.contains('YES, Delete It').click();

    cy.get(rowSelector).should('have.length', 1).as('cyRows1');
    cy.get('@cyRows1').eq(0).find('td').as('rowA');
    cy.get('@rowA').eq(0).contains('root@skills.org').should('not.exist');
  });

  it('Add Admin - forward slash character does not cause error', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });


    cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN').as('addAdmin');

    cy.intercept('POST',  'suggestDashboardUsers').as('suggest');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
    cy.intercept('GET', '/admin/projects/proj1').as('loadProject');

    cy.visit('/administrator/projects/proj1/access');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');

    cy.contains('Enter user id').type('root/bar{enter}');
    cy.wait('@suggest');
  });

  it('focus should be returned to new project button', ()=> {
    cy.visit('/administrator');
    cy.get('[data-cy=newProjectButton]').click();
    cy.get('body').type('{esc}');
    cy.get('[data-cy=newProjectButton]').should('have.focus');

    cy.get('[data-cy=newProjectButton]').click();
    cy.get('[data-cy=closeProjectButton]').click();
    cy.get('[data-cy=newProjectButton]').should('have.focus');

    cy.get('[data-cy=newProjectButton]').click();
    cy.get('[data-cy=projectName]').type('test 123');
    cy.get('[data-cy=saveProjectButton]').click();
    cy.get('[data-cy=newProjectButton]').should('have.focus');

    cy.get('[data-cy=newProjectButton]').click();
    cy.get('[aria-label=Close]').click();
    cy.get('[data-cy=newProjectButton]').should('have.focus');
  });

  it('focus should be returned to project edit button', () => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });
    cy.request('POST', '/app/projects/proj2', {
      projectId: 'proj2',
      name: "proj2"
    });
    cy.visit('/administrator/');
    cy.get('[data-cy="projOptions_proj1"]').click();
    cy.get('[data-cy="projOptions_proj1"] [data-cy=editMenuEditBtn]').click();
    cy.get('[data-cy=projectName]').should('be.visible');
    cy.get('body').type('{esc}{esc}');
    cy.get('[data-cy="projectCard_proj1"] div.project-settings .dropdown-toggle').should('have.focus');

    cy.get('[data-cy="projOptions_proj1"]').click();
    cy.get('[data-cy="projOptions_proj1"] [data-cy=editMenuEditBtn]').click();
    cy.get('[data-cy=closeProjectButton]').click();
    cy.get('[data-cy="projectCard_proj1"] div.project-settings .dropdown-toggle').should('have.focus');

    cy.get('[data-cy="projOptions_proj1"]').click();
    cy.get('[data-cy="projOptions_proj1"] [data-cy=editMenuEditBtn]').click();
    cy.get('[data-cy=projectName]').type('test 123');
    cy.get('[data-cy=saveProjectButton]').click();
    cy.get('[data-cy="projectCard_proj1"] div.project-settings .dropdown-toggle').should('have.focus');

    cy.get('[data-cy="projOptions_proj1"]').click();
    cy.get('[data-cy="projOptions_proj1"] [data-cy=editMenuEditBtn]').click();
    cy.get('[aria-label=Close]').click();
    cy.get('[data-cy="projectCard_proj1"] div.project-settings .dropdown-toggle').should('have.focus');

    //project 2
    cy.get('[data-cy="projOptions_proj2"]').click();
    cy.get('[data-cy="projOptions_proj2"] [data-cy=editMenuEditBtn]').click();
    cy.get('[data-cy=projectName]').should('be.visible');
    cy.get('body').type('{esc}{esc}');
    cy.get('[data-cy="projectCard_proj2"] div.project-settings .dropdown-toggle').should('have.focus');

    cy.get('[data-cy="projOptions_proj2"]').click();
    cy.get('[data-cy="projOptions_proj2"] [data-cy=editMenuEditBtn]').click();
    cy.get('[data-cy=closeProjectButton]').click();
    cy.get('[data-cy="projectCard_proj2"] div.project-settings .dropdown-toggle').should('have.focus');

    cy.get('[data-cy="projOptions_proj2"]').click();
    cy.get('[data-cy="projOptions_proj2"] [data-cy=editMenuEditBtn]').click();
    cy.get('[data-cy=projectName]').type('test 123');
    cy.get('[data-cy=saveProjectButton]').click();
    cy.get('[data-cy="projectCard_proj2"] div.project-settings .dropdown-toggle').should('have.focus');

    cy.get('[data-cy="projOptions_proj2"]').click();
    cy.get('[data-cy="projOptions_proj2"] [data-cy=editMenuEditBtn]').click();
    cy.get('[aria-label=Close]').click();
    cy.get('[data-cy="projectCard_proj2"] div.project-settings .dropdown-toggle').should('have.focus');
  });

  it('new level dialog should return focus to new level button', () => {

    cy.intercept('GET', '/admin/projects/MyNewtestProject').as('loadProject');

    cy.intercept('PUT', '/admin/projects/MyNewtestProject/levels/edit/**').as('saveLevel');

    cy.intercept('GET', '/admin/projects/MyNewtestProject/levels').as('loadLevels');

    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
    })

    cy.visit('/administrator/projects/MyNewtestProject/');
    cy.wait('@loadProject');

    cy.contains('Levels').click();
    cy.get('[data-cy=addLevel]').click();
    cy.get('[data-cy=cancelLevel]').click();
    cy.get('[data-cy=addLevel]').should('have.focus');

    cy.get('[data-cy=addLevel]').click();
    cy.get('[data-cy=levelName]').type('{esc}');
    cy.get('[data-cy=addLevel]').should('have.focus');

    cy.get('[data-cy=addLevel]').click();
    cy.get('[aria-label=Close]').filter('.text-light').click();
    cy.get('[data-cy=addLevel]').should('have.focus');

    cy.get('[data-cy=editLevelButton]').eq(0).click();
    cy.get('[data-cy=cancelLevel]').click();
    cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

    cy.get('[data-cy=editLevelButton]').eq(0).click();
    cy.get('[data-cy=levelName]').type('{esc}');
    cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

    cy.get('[data-cy=editLevelButton]').eq(0).click();
    cy.get('[aria-label=Close]').filter('.text-light').click();
    cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

    cy.get('[data-cy=editLevelButton]').eq(0).click();
    cy.get('[data-cy=levelName]').type('{selectall}Fooooooo');
    cy.get('[data-cy=saveLevelButton]').click();
    cy.wait('@saveLevel');
    cy.wait('@loadLevels');
    cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

    cy.get('[data-cy=editLevelButton]').eq(3).click();
    cy.get('[data-cy=cancelLevel]').click();
    cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

    cy.get('[data-cy=editLevelButton]').eq(3).click();
    cy.get('[data-cy=levelName]').type('{esc}');
    cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

    cy.get('[data-cy=editLevelButton]').eq(3).click();
    cy.get('[aria-label=Close]').filter('.text-light').click();
    cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

    cy.get('[data-cy=editLevelButton]').eq(3).click();
    cy.get('[data-cy=levelName]').type('{selectall}Baaaaar');
    cy.get('[data-cy=saveLevelButton]').click();
    cy.wait('@saveLevel');
    cy.wait('@loadLevels');
    cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');
  });

  it('Trusted client should be shown when oAuthOnly!=true', () => {
    cy.intercept('GET', '/public/config', {oAuthOnly: false, authMode: 'FORM'}).as('loadConfig');

    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "proj1"
    });

    cy.intercept({
      method: 'PUT',
      url: '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN',
    }).as('addAdmin');

    cy.intercept({
      method: 'POST',
      url: '/app/users/suggestDashboardUsers*',
    }).as('suggest');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
    cy.intercept('GET', '/admin/projects/proj1').as('loadProject');
    cy.intercept('GET', '/admin/projects/proj1/userRoles').as('loadUserRoles');

    cy.visit('/administrator/projects/proj1/access');
    cy.wait('@loadConfig');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProject');
    cy.wait('@loadUserRoles');

    cy.contains('Project Administrators').should('exist');
    cy.get('[data-cy="trusted-client-props-panel"]').should('exist')
  });
});

