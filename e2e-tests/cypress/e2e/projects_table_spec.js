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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Projects Table Tests', () => {
    const tableSelector = '[data-cy=projectsTable]';
    const numProjCreated = 10;
    beforeEach(() => {
        cy.intercept('GET', '/app/projects')
            .as('getProjects');
        cy.intercept('GET', '/api/icons/customIconCss')
            .as('getProjectsCustomIcons');
        cy.intercept('GET', '/app/userInfo')
            .as('getUserInfo');
        cy.intercept('/admin/projects/proj1/users/root@skills.org/roles')
            .as('getRolesForRoot');

        for (let i = 1; i <= numProjCreated; i += 1) {
            cy.createProject(i);
        }
    });

    it('When more than 10 projects then projects should be displayed in a table', () => {

        // wait a second and create another project to help validate date order sorting
        cy.wait(1001);
        cy.createProject(11);

        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="projectsTable"]')
            .should('be.visible');
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '11');

        // validate the projects are sorted in desc create date order
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'project 11'
            }],
            [{
                colIndex: 0,
                value: 'project 10'
            }],
            [{
                colIndex: 0,
                value: 'project 9'
            }],
            [{
                colIndex: 0,
                value: 'project 8'
            }],
            [{
                colIndex: 0,
                value: 'project 7'
            }],
            [{
                colIndex: 0,
                value: 'project 6'
            }],
            [{
                colIndex: 0,
                value: 'project 5'
            }],
            [{
                colIndex: 0,
                value: 'project 4'
            }],
            [{
                colIndex: 0,
                value: 'project 3'
            }],
            [{
                colIndex: 0,
                value: 'project 2'
            }],
            [{
                colIndex: 0,
                value: 'project 1'
            }],
        ], 10);

        cy.get(`${tableSelector}`)
            .contains('Created')
            .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'project 1'
            }],
            [{
                colIndex: 0,
                value: 'project 2'
            }],
            [{
                colIndex: 0,
                value: 'project 3'
            }],
            [{
                colIndex: 0,
                value: 'project 4'
            }],
            [{
                colIndex: 0,
                value: 'project 5'
            }],
            [{
                colIndex: 0,
                value: 'project 6'
            }],
            [{
                colIndex: 0,
                value: 'project 7'
            }],
            [{
                colIndex: 0,
                value: 'project 8'
            }],
            [{
                colIndex: 0,
                value: 'project 9'
            }],
            [{
                colIndex: 0,
                value: 'project 10'
            }],
            [{
                colIndex: 0,
                value: 'project 11'
            }],
        ], 10);
    });

    it('Create new project', function () {

        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('POST', '/app/projects/MyBrandNewtestProject')
            .as('postNewProject');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="projectsTable"]')
            .should('exist');
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', 10);

        cy.clickButton('Project');
        cy.get('[data-cy="projectName"]')
            .type('My Brand New test Project');
        cy.clickSave();

        cy.wait('@postNewProject');

        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', 11);
        cy.get('[data-cy="projectsTable-projectFilter"]')
            .type('Brand');
        cy.get('[data-cy="projectsTable-filterBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'My Brand New test Project'
            }],
        ], 10);

        cy.get('[data-cy="projectsTable-resetBtn"]')
            .click();
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '11');
    });

    it('Delete an existing project', function () {

        // add one more project so the table still exists after deletion
        cy.createProject(11);

        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('DELETE', '/admin/projects/proj10')
            .as('deleteProject');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="projectsTable"]')
            .should('exist');
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '11');

        cy.get('[data-cy="projectsTable-projectFilter"]')
            .type('proj10');
        cy.get('[data-cy="projectsTable-filterBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'project 10'
            }],
        ], 10);

        cy.get('[data-cy=deleteProjectButton_proj10]')
            .click();
        cy.contains('Removal Safety Check');
        cy.get('[data-cy=currentValidationText]')
            .type('Delete Me');
        cy.get('[data-cy=removeButton]')
            .should('be.enabled')
            .click();

        cy.wait('@deleteProject');

        cy.get('[data-cy="projectsTable-resetBtn"]')
            .click();
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '10');

        cy.get('[data-cy="projectsTable-projectFilter"]')
            .type('proj10');
        cy.get('[data-cy="projectsTable-filterBtn"]')
            .click();
        cy.contains('There are no records to show');
    });

    it('Edit existing project', function () {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="projectsTable"]')
            .should('exist');
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '10');

        cy.get('[data-cy=editProjectIdproj1]')
            .click();
        cy.get('input[data-cy=projectName]')
            .type('{selectall}I Am A Changed Project Name');
        cy.get('button[data-cy=saveProjectButton]')
            .click();

        /* focus is automatically getting switched back to the edit button which pre-emptively moves the cursor out of the text input field
          before the full text has been entered depending on test timing
         */
        cy.wait(2000);
        cy.get('[data-cy="projectsTable-projectFilter"]').should('be.enabled')
        cy.get('[data-cy="projectsTable-projectFilter"]')
            .type('Changed');
        cy.get('[data-cy="projectsTable-filterBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'I Am A Changed Project Name'
            }],
        ], 10);
        cy.contains('I Am A Changed Project Name')
            .should('be.visible');
    });

    it('Edit existing project id', function () {
        cy.visit('/administrator/');
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '10');

        cy.get('[data-cy=editProjectIdproj1]')
            .click();
        cy.get('[data-cy=enableIdInput]').click({force: true});
        cy.get('[data-cy="idInputValue"]')
            .type('A');
        cy.get('button[data-cy=saveProjectButton]')
            .click();
        cy.get('[data-cy="projCell_proj1A"]')
    });

    it('Manage existing project using Button', function () {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="projectsTable"]')
            .should('exist');
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '10');

        cy.get('[data-cy=manageProjBtn_proj1]')
            .click();

        cy.contains('PROJECT: This is project 1')
            .should('be.visible');
        cy.contains('ID: proj1')
            .should('be.visible');
    });

    it('Manage existing project using Link', function () {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="projectsTable"]')
            .should('exist');
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '10');

        cy.get('[data-cy=manageProjLink_proj1]')
            .click();

        cy.contains('PROJECT: This is project 1')
            .should('be.visible');
        cy.contains('ID: proj1')
            .should('be.visible');
    });

    it('Sort by project id', function () {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="projectsTable"]')
            .should('exist');
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '10');

        cy.get(`${tableSelector}`)
            .contains('Project')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'project 1'
            }],
            [{
                colIndex: 0,
                value: 'project 2'
            }],
            [{
                colIndex: 0,
                value: 'project 3'
            }],
            [{
                colIndex: 0,
                value: 'project 4'
            }],
            [{
                colIndex: 0,
                value: 'project 5'
            }],
            [{
                colIndex: 0,
                value: 'project 6'
            }],
            [{
                colIndex: 0,
                value: 'project 7'
            }],
            [{
                colIndex: 0,
                value: 'project 8'
            }],
            [{
                colIndex: 0,
                value: 'project 9'
            }],
            [{
                colIndex: 0,
                value: 'project 10'
            }],
        ], 10);

        cy.get(`${tableSelector}`)
            .contains('Project')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'project 10'
            }],
            [{
                colIndex: 0,
                value: 'project 9'
            }],
            [{
                colIndex: 0,
                value: 'project 8'
            }],
            [{
                colIndex: 0,
                value: 'project 7'
            }],
            [{
                colIndex: 0,
                value: 'project 6'
            }],
            [{
                colIndex: 0,
                value: 'project 5'
            }],
            [{
                colIndex: 0,
                value: 'project 4'
            }],
            [{
                colIndex: 0,
                value: 'project 3'
            }],
            [{
                colIndex: 0,
                value: 'project 2'
            }],
            [{
                colIndex: 0,
                value: 'project 1'
            }],
        ], 10);
    });

    it('Validate project stats', function () {

        const now = dayjs()
            .utc();
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.createProject(11);
        cy.createSubject(11);
        cy.createSkill(11);
        cy.reportSkill(11, 1, 'user@skills.org', now.subtract(2, 'months')
            .format('YYYY-MM-DD HH:mm'));

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="projectsTable"]')
            .should('exist');
        cy.get('[data-cy=skillsBTableTotalRows]')
            .should('have.text', '11');

        cy.get('[data-cy="projectsTable-projectFilter"]')
            .type('project 11');
        cy.get('[data-cy="projectsTable-filterBtn"]')
            .click();

        cy.get(`${tableSelector}`)
            .contains('Project')
            .click();
        cy.validateTable(tableSelector, [
            [
                {
                    colIndex: 0,
                    value: 'project 1'
                },
                {
                    colIndex: 1,
                    value: '1'
                },
                {
                    colIndex: 2,
                    value: '1'
                },
                {
                    colIndex: 3,
                    value: '200'
                },
                {
                    colIndex: 4,
                    value: '0'
                },
            ],
        ], 1);
    });

    it('Project Table View: edit focus should be returned to project edit button', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy="editProjectIdproj10"]')
            .click();

        cy.get('[data-cy="closeProjectButton"]')
            .click();
        cy.get('[data-cy="editProjectIdproj10"]')
            .should('have.focus');

        cy.get('[data-cy="editProjectIdproj9"]')
            .click();
        cy.wait(250);
        cy.get('body')
            .type('{esc}{esc}');
        cy.get('[data-cy="editProjectIdproj9"]')
            .should('have.focus');

        cy.get('[data-cy="editProjectIdproj8"]')
            .click();

        cy.get('[aria-label=Close]')
            .click();
        cy.get('[data-cy="editProjectIdproj8"]')
            .should('have.focus');

        cy.intercept('POST', '/admin/projects/proj7')
            .as('nameChange');
        cy.intercept('GET', '/admin/projects/proj7')
            .as('reload');
        cy.get('[data-cy="editProjectIdproj7"]')
            .click();
        cy.get('[data-cy=projectName]')
            .type('123');
        cy.get('[data-cy=saveProjectButton]')
            .click();
        cy.wait('@nameChange');
        cy.wait('@reload');
        cy.get('[data-cy="manageProjLink_proj7"]')
            .contains('This is project 7123');
        cy.get('[data-cy="editProjectIdproj7"]')
            .should('have.focus');
    });

    it('Canceling delete dialog should return focus to delete button when projects are presented in table view', () => {

        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.get('[data-cy=deleteProjectButton_proj10]')
            .click();
        cy.get('[data-cy=closeRemovalSafetyCheck]')
            .click();
        cy.wait(200);
        cy.get('[data-cy=deleteProjectButton_proj10]')
            .should('have.focus');
    });

    const maxNumProjects = 25;
    it('projects table validation: user cannot create more than configured max projects', () => {
        for (let i = numProjCreated + 1; i <= maxNumProjects; i += 1) {
            cy.createProject(i);
        }
        cy.visit('/administrator/');
        cy.get('[data-cy="addProjectDisabled"]')
            .contains('Cannot create or copy projects - The maximum number of Projects allowed is 25');
        cy.get('[data-cy="newProjectButton"]')
            .should('be.disabled');
        cy.get('[data-cy="copyProjectIdproj25"]')
            .should('be.disabled');
        cy.get('[data-cy="copyProjectIdproj24"]')
            .should('be.disabled');
        cy.get('[data-cy="copyProjectIdproj23"]')
            .should('be.disabled');
    });

    it('root user can create unlimited number of projects', () => {
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });
        for (let i = numProjCreated + 1; i <= maxNumProjects; i += 1) {
            cy.createProject(i);
        }
        for (let i = 1; i <= maxNumProjects; i += 1) {
            cy.request('POST', `/root/pin/proj${i}`, {});
        }
        cy.visit('/administrator/');
        cy.get('[data-cy="copyProjectIdproj25"]')
        cy.get('[data-cy="addProjectDisabled"]').should('not.exist')
        cy.get('[data-cy="newProjectButton"]').should('be.enabled');
        cy.get('[data-cy="copyProjectIdproj25"]')
            .should('be.enabled');
        cy.get('[data-cy="copyProjectIdproj24"]')
            .should('be.enabled');
        cy.get('[data-cy="copyProjectIdproj23"]')
            .should('be.enabled');

        // save new project as root
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type('NewProj');
        cy.get('[data-cy="saveProjectButton"]').click();
        cy.get('[data-cy="manageProjLink_NewProj"]')

        // copy project as root
        cy.get('[data-cy="copyProjectIdproj25"]').click();
        cy.get('[data-cy="projectName"]')
            .type('Copy Proj');
        cy.get('[data-cy="saveProjectButton"]').click();
        cy.get('[data-cy="allDoneBtn"]').click();
        cy.get('[data-cy="manageProjLink_CopyProj"]')
    });

});

