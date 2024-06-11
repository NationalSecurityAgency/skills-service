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

describe('Projects Modal Validation Tests', () => {
    beforeEach(() => {
        cy.intercept('GET', '/app/projects')
            .as('getProjects');
        cy.intercept('GET', '/api/icons/customIconCss')
            .as('getProjectsCustomIcons');
        cy.intercept('GET', '/app/userInfo')
            .as('getUserInfo');
        cy.intercept('/admin/projects/proj1/users/root@skills.org/roles*')
            .as('getRolesForRoot');
    });

    it('Duplicate project names are not allowed', () => {
        cy.request('POST', '/app/projects/MyNewtestProject', {
            projectId: 'MyNewtestProject',
            name: 'My New test Project'
        });
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type('My New test Project');
        cy.get('[data-cy=projectNameError]')
            .contains('Project Name already exists')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
    });

    it('Duplicate project ids are not allowed', () => {
        cy.request('POST', '/app/projects/MyNewtestProject', {
            projectId: 'MyNewtestProject',
            name: 'My New test Project'
        });
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type('Other Project Name');
        cy.get('[data-cy="enableIdInput"] input').click();
        cy.getIdField()
            .clear()
            .type('MyNewtestProject');

        cy.get('[data-cy=idError]')
            .contains('Project ID already exists')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
    });

    it('Validate that cannot create project with the same name in lowercase', () => {
        const expectedId = 'TestProject1';
        const providedName = 'Test Project #1';

        cy.intercept('POST', `/app/projects/${expectedId}`)
            .as('postNewProject');

        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type(providedName);
        cy.getIdField()
            .should('have.value', expectedId);

        cy.clickSave();
        cy.wait('@postNewProject');

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type(providedName.toLowerCase());

        cy.get('[data-cy=projectNameError')
            .contains('Project Name already exists')
            .should('be.visible');

        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
    });

    it('Project name is required', () => {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="enableIdInput"] input')
            .click();
        cy.getIdField()
            .type('InitValue');

        cy.get('[data-cy=saveDialogBtn')
            .should('be.disabled');
    });

    it('Project id is required', () => {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.get('[data-cy="newProjectButton"]').click()
        ;
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.get('[data-cy="enableIdInput"] input')
            .click();
        cy.getIdField()
            .clear();
        cy.get('[data-cy=idError]')
            .contains('Project ID is a required field')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn')
            .should('be.disabled');
    });

    it('name is validated against custom validators', () => {
        cy.intercept('GET', '/app/projects')
          .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
          .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.get('[data-cy="newProjectButton"]').click()

        cy.get('[data-cy="projectName"]')
          .type('Great Name');

        cy.get('[data-cy="projectNameError"]')
          .should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.enabled');

        cy.get('[data-cy="projectName"]')
          .type('{selectall}(A) Updated Project Name');
        cy.get('[data-cy="projectNameError"]')
          .contains('Project Name - names may not contain (A)');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.disabled');

        cy.get('[data-cy="projectName"]')
          .type('{selectall}(B) A Updated Project Name');
        cy.get('[data-cy="projectNameError"]')
          .should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.enabled')
    });

    it('Project name must be > 3 chars < 50 chars', () => {
        const minLenMsg = 'Project Name must be at least 3 characters';
        const maxLenMsg = 'Project Name must be at most 50 characters';
        const projId = 'ProjectId';
        cy.intercept('POST', `/app/projects/${projId}`)
            .as('postNewProject');
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy="newProjectButton"]').click()

        cy.get('[data-cy="enableIdInput"] input')
            .click();
        cy.getIdField()
            .type('ProjectId');
        cy.get('[data-cy="projectName"]')
            .type('12');
        cy.get('[data-cy="projectNameError"]')
            .should('have.text', minLenMsg);

        cy.get('[data-cy="projectName"]')
            .type('3');
        cy.get('[data-cy="projectNameError"]')
            .should('not.be.visible');

        const longInvalid = Array(51)
            .fill('a')
            .join('');
        const longValid = Array(50)
            .fill('a')
            .join('');

        cy.get('[data-cy="projectName"]')
            .clear()
            .type(longInvalid);
        cy.get('[data-cy="projectNameError"]')
            .should('have.text', maxLenMsg);

        cy.get('[data-cy="projectName"]')
            .clear()
            .type(longValid);
        cy.get('[data-cy="projectNameError"]')
            .should('not.be.visible');

        cy.clickSave();
        cy.wait('@postNewProject');

        cy.contains(longValid);
    });

    it('Project ID must be > 3 chars < 50 chars', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxIdLength = 50;
                res.send(conf);
            });
        })
            .as('loadConfig');
        const minLenMsg = 'Project ID must be at least 3 characters';
        const maxLenMsg = 'Project ID must be at most 50 characters';
        const requiredMsg = 'Project ID is a required field';
        const projName = 'Project Name';

        const longInvalid = Array(51)
            .fill('a')
            .join('');
        const longValid = Array(50)
            .fill('a')
            .join('');
        cy.intercept('POST', `/app/projects/${longValid}`)
            .as('postNewProject');
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.get('[data-cy="newProjectButton"]').click()

        cy.get('[data-cy="enableIdInput"] input')
            .click();

        cy.getIdField()
            .type('12');
        cy.get('[data-cy="projectName"]')
            .type(projName);
        cy.get('[data-cy="idError"]')
            .should('have.text', minLenMsg);

        cy.getIdField()
            .type('3');
        cy.get('[data-cy="idError"]')
            .should('not.be.visible');

        cy.getIdField()
            .clear();
        cy.get('[data-cy="idError"]')
            .should('have.text', requiredMsg);
        cy.getIdField()
            .click();
        cy.getIdField()
            .invoke('val', longInvalid)
            .trigger('input');
        cy.get('[data-cy="idError"]')
            .should('have.text', maxLenMsg);

        cy.getIdField()
            .clear();
        cy.get('[data-cy="idError"]')
            .should('have.text', requiredMsg);
        cy.getIdField()
            .click()
            .invoke('val', longValid)
            .trigger('input');
        cy.getIdField().should('have.value', longValid)
        cy.get('[data-cy="idError"]')
            .should('not.be.visible');

        cy.clickSave();
        cy.wait('@postNewProject');

        cy.get(`[data-cy="projectCard_${longValid}"]`)
    });

    it('run validation on load in case validation improved and existing values fail to validate', () => {
        cy.intercept('POST', '/api/validation/description*', {
            valid: false,
            msg: 'Mocked up validation failure'
        }).as('validateDesc');

        cy.createProject(1, {description: 'Very cool project'})
        cy.visit('/administrator/');
        cy.get('[data-cy="editProjBtn"]').click()
        cy.wait('@validateDesc')
        cy.get('[data-cy="descriptionError"]').contains('Mocked up validation failure')
    });

    it('null word is not allowed for project ID or project name', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type('null');
        cy.get('[data-cy=projectNameError]')
            .contains('Null value is not allowed')
            .should('be.visible');
        cy.get('[data-cy="idError"]')
            .contains('Null value is not allowed')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.get('[data-cy="projectName"]').clear().type('one')
        cy.get('[data-cy=projectNameError]').should('not.be.visible')
        cy.get('[data-cy=idError]').should('not.be.visible')

        // verify validator is case-insensitive and trims whitespace
        cy.get('[data-cy="projectName"]').clear()
            .type(' NUlL ');
        cy.get('[data-cy=projectNameError]')
            .contains('Null value is not allowed')
            .should('be.visible');
        cy.get('[data-cy="idError"]')
            .contains('Null value is not allowed')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
    });

});