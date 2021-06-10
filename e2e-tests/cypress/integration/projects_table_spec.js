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
import dayjs from "dayjs";
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Projects Table Tests', () => {
  const tableSelector = '[data-cy=projectsTable]'

  beforeEach(() => {
    cy.intercept('GET', '/app/projects').as('getProjects')
    cy.intercept('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
    cy.intercept('GET', '/app/userInfo').as('getUserInfo')
    cy.intercept('/admin/projects/proj1/users/root@skills.org/roles').as('getRolesForRoot');

    for (let i = 1; i <= 10; i += 1) {
      cy.createProject(i);
    }
  });

  it('When more than 10 projects then projects should be displayed in a table', () => {

    // wait a second and create another project to help validate date order sorting
    cy.wait(1001);
    cy.createProject(11);

    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectsTable"]').should('be.visible');
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text', '11');

    // validate the projects are sorted in desc create date order
    cy.validateTable(tableSelector, [
      [{ colIndex: 0,  value: 'proj11' }],
      [{ colIndex: 0,  value: 'proj10' }],
      [{ colIndex: 0,  value: 'proj9' }],
      [{ colIndex: 0,  value: 'proj8' }],
      [{ colIndex: 0,  value: 'proj7' }],
      [{ colIndex: 0,  value: 'proj6' }],
      [{ colIndex: 0,  value: 'proj5' }],
      [{ colIndex: 0,  value: 'proj4' }],
      [{ colIndex: 0,  value: 'proj3' }],
      [{ colIndex: 0,  value: 'proj2' }],
      [{ colIndex: 0,  value: 'proj1' }],
    ], 10);

    cy.get(`${tableSelector}`).contains('Created').click();

    cy.validateTable(tableSelector, [
      [{ colIndex: 0,  value: 'proj1' }],
      [{ colIndex: 0,  value: 'proj2' }],
      [{ colIndex: 0,  value: 'proj3' }],
      [{ colIndex: 0,  value: 'proj4' }],
      [{ colIndex: 0,  value: 'proj5' }],
      [{ colIndex: 0,  value: 'proj6' }],
      [{ colIndex: 0,  value: 'proj7' }],
      [{ colIndex: 0,  value: 'proj8' }],
      [{ colIndex: 0,  value: 'proj9' }],
      [{ colIndex: 0,  value: 'proj10' }],
      [{ colIndex: 0,  value: 'proj11' }],
    ], 10);
  });

  it('Create new project', function () {

    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
    cy.intercept('POST', '/app/projects/MyBrandNewtestProject').as('postNewProject');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectsTable"]').should('exist')
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text', 10);

    cy.clickButton('Project');
    cy.get('[data-cy="projectName"]').type("My Brand New test Project")
    cy.clickSave();

    cy.wait('@postNewProject');

    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text', 11);
    cy.get('[data-cy="projectsTable-projectFilter"]').type('Brand');
    cy.get('[data-cy="projectsTable-filterBtn"]').click();
    cy.validateTable(tableSelector, [
        [{ colIndex: 0,  value: 'MyBrandNewtestProject' }],
    ], 10);

    cy.contains('My Brand New test Project')
    cy.contains('ID: MyBrandNewtestProject')

    cy.get('[data-cy="projectsTable-resetBtn"]').click();
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text','11');
  });

  it('Delete an existing project', function () {

    // add one more project so the table still exists after deletion
    cy.createProject(11)

    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
    cy.intercept('DELETE', '/admin/projects/proj10').as('deleteProject');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectsTable"]').should('exist')
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text', '11');

    cy.get('[data-cy="projectsTable-projectFilter"]').type('proj10');
    cy.get('[data-cy="projectsTable-filterBtn"]').click();
    cy.validateTable(tableSelector, [
        [{ colIndex: 0,  value: 'proj10' }],
    ], 10);

    cy.get('[data-cy=deleteProjectButton_proj10]').click();
    cy.contains('YES, Delete It').click();

    cy.wait('@deleteProject');

    cy.get('[data-cy="projectsTable-resetBtn"]').click();
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text','10');

    cy.get('[data-cy="projectsTable-projectFilter"]').type('proj10');
    cy.get('[data-cy="projectsTable-filterBtn"]').click();
    cy.contains('There are no records to show')
  });

  it('Edit existing project', function () {
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectsTable"]').should('exist')
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text', '10');

    cy.get('[data-cy=editProjectIdproj1]').click();
    cy.get('input[data-cy=projectName]').type('{selectall}I Am A Changed Project Name');
    cy.get('button[data-cy=saveProjectButton]').click();

    cy.get('[data-cy="projectsTable-projectFilter"]').type('Changed');
    cy.get('[data-cy="projectsTable-filterBtn"]').click();
    cy.validateTable(tableSelector, [
        [{ colIndex: 0,  value: 'proj1' }],
    ], 10);
    cy.validateTable(tableSelector, [
        [{ colIndex: 0,  value: 'I Am A Changed Project Name' }],
    ], 10);
    cy.contains('I Am A Changed Project Name').should('be.visible');
  });

  it('Manage existing project using Button', function () {
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectsTable"]').should('exist')
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text', '10');

    cy.get('[data-cy=manageProjBtn_proj1]').click();

    cy.contains('PROJECT: This is project 1').should('be.visible');
    cy.contains('ID: proj1').should('be.visible');
  });

  it('Manage existing project using Link', function () {
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectsTable"]').should('exist')
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text', '10');

    cy.get('[data-cy=manageProjLink_proj1]').click();

    cy.contains('PROJECT: This is project 1').should('be.visible');
    cy.contains('ID: proj1').should('be.visible');
  });

  it('Sort by project id', function () {
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectsTable"]').should('exist')
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text','10');

    cy.get(`${tableSelector}`).contains('Project').click();
    cy.validateTable(tableSelector, [
      [{ colIndex: 0,  value: 'proj1' }],
      [{ colIndex: 0,  value: 'proj2' }],
      [{ colIndex: 0,  value: 'proj3' }],
      [{ colIndex: 0,  value: 'proj4' }],
      [{ colIndex: 0,  value: 'proj5' }],
      [{ colIndex: 0,  value: 'proj6' }],
      [{ colIndex: 0,  value: 'proj7' }],
      [{ colIndex: 0,  value: 'proj8' }],
      [{ colIndex: 0,  value: 'proj9' }],
      [{ colIndex: 0,  value: 'proj10' }],
    ], 10);

    cy.get(`${tableSelector}`).contains('Project').click();
    cy.validateTable(tableSelector, [
      [{ colIndex: 0,  value: 'proj10' }],
      [{ colIndex: 0,  value: 'proj9' }],
      [{ colIndex: 0,  value: 'proj8' }],
      [{ colIndex: 0,  value: 'proj7' }],
      [{ colIndex: 0,  value: 'proj6' }],
      [{ colIndex: 0,  value: 'proj5' }],
      [{ colIndex: 0,  value: 'proj4' }],
      [{ colIndex: 0,  value: 'proj3' }],
      [{ colIndex: 0,  value: 'proj2' }],
      [{ colIndex: 0,  value: 'proj1' }],
    ], 10);
  });

  it('Validate project stats', function () {

    const now = dayjs().utc();
    cy.intercept('GET', '/app/projects').as('loadProjects');
    cy.intercept('GET', '/app/userInfo').as('loadUserInfo');

    cy.createProject(11)
    cy.createSubject(11)
    cy.createSkill(11);
    cy.reportSkill(11, 1, 'user@skills.org',  now.subtract(2, 'months').format('YYYY-MM-DD HH:mm'));

    cy.visit('/administrator/');
    cy.wait('@loadUserInfo');
    cy.wait('@loadProjects');

    cy.get('[data-cy="projectsTable"]').should('exist')
    cy.get('[data-cy=skillsBTableTotalRows]').should('have.text', '11');

    cy.get('[data-cy="projectsTable-projectFilter"]').type('project 11');
    cy.get('[data-cy="projectsTable-filterBtn"]').click();

    cy.get(`${tableSelector}`).contains('Project').click();
    cy.validateTable(tableSelector, [
      [
        { colIndex: 0,  value: 'proj1' },
        { colIndex: 1,  value: '1' },
        { colIndex: 2,  value: '1' },
        { colIndex: 3,  value: '200' },
        { colIndex: 4,  value: '0' },
        { colIndex: 5,  value:'2 months ago' },
      ],
    ], 1);

  });

});

