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

        cy.clickButton('Project');
        cy.get('[data-cy="projectName"]')
            .type('My New test Project');
        cy.get('[data-cy=projectNameError]')
            .contains('The value for the Project Name is already taken')
            .should('be.visible');
        cy.get('[data-cy=saveProjectButton]')
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
        cy.clickButton('Project');
        cy.get('[data-cy="projectName"]')
            .type('Other Project Name');
        cy.get('[data-cy="enableIdInput"]')
            .click();
        cy.getIdField()
            .clear()
            .type('MyNewtestProject');

        cy.get('[data-cy=idError]')
            .contains('The value for the Project ID is already taken')
            .should('be.visible');
        cy.get('[data-cy=saveProjectButton]')
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
        cy.clickButton('Project');
        cy.get('[data-cy="projectName"]')
            .type(providedName);
        cy.getIdField()
            .should('have.value', expectedId);

        cy.clickSave();
        cy.wait('@postNewProject');

        cy.clickButton('Project');
        cy.get('[data-cy="projectName"]')
            .type(providedName.toLowerCase());

        cy.get('[data-cy=projectNameError')
            .contains('The value for the Project Name is already taken')
            .should('be.visible');

        cy.get('[data-cy=saveProjectButton]')
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
        cy.clickButton('Project');
        cy.get('[data-cy="enableIdInput"]')
            .click();
        cy.getIdField()
            .type('InitValue');

        cy.get('[data-cy=saveProjectButton')
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
        cy.clickButton('Project');
        ;
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.get('[data-cy="enableIdInput"]')
            .click();
        cy.getIdField()
            .clear();
        cy.get('[data-cy=idError]')
            .contains('Project ID is required')
            .should('be.visible');
        cy.get('[data-cy=saveProjectButton')
            .should('be.disabled');
    });

    it('Project name must be > 3 chars < 50 chars', () => {
        const minLenMsg = 'Project Name cannot be less than 3 characters.';
        const maxLenMsg = 'Project Name cannot exceed 50 characters.';
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

        cy.clickButton('Project');

        cy.get('[data-cy="enableIdInput"]')
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

        cy.contains(`ID: ${projId}`);
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
        const minLenMsg = 'Project ID cannot be less than 3 characters.';
        const maxLenMsg = 'Project ID cannot exceed 50 characters.';
        const requiredMsg = 'Project ID is required';
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
        cy.clickButton('Project');

        cy.get('[data-cy="enableIdInput"]')
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

});