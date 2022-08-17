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

const snapshotOptions = {
    blackout: ['[data-cy=projectCreated]', '[data-cy=projectLastReportedSkill]', '[data-cy="dashboardVersionContainer"]'],
    failureThreshold: 0.03, // threshold for entire image
    failureThresholdType: 'percent', // percent of image or number of pixels
    customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
    capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
};

describe('Projects Tests', () => {
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

    it('Create new projects', function () {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.intercept('POST', '/app/projects/MyNewtestProject')
            .as('postNewProject');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.clickButton('Project');
        cy.get('[data-cy="projectName"]')
            .type('My New test Project');
        cy.clickSave();

        cy.wait('@postNewProject');

        cy.contains('My New test Project');
        cy.contains('ID: MyNewtestProject');
    });

    it('Canceling delete dialog should return focus to delete button', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'proj2'
        });

        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.get('[data-cy=deleteProjBtn]')
            .eq(0)
            .click();
        cy.get('[data-cy=closeRemovalSafetyCheck]')
            .click();
        cy.wait(200);
        cy.get('[data-cy=deleteProjBtn]')
            .eq(0)
            .should('have.focus');
    });

    it('Ampersand in project name', () => {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.intercept('POST', '/app/projects/MyNewtestProject')
            .as('postNewProject');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.clickButton('Project');
        cy.get('[data-cy="projectName"]')
            .type('My New & test Project');
        cy.clickSave();

        cy.wait('@postNewProject');

        cy.contains('My New & test Project');
        cy.contains('ID: MyNewtestProject');
    });

    it('Provide clear instructions how to create a new project - root user', function () {
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });
        cy.visit('/administrator/');
        cy.contains('No Projects Yet...');
        cy.contains('A Project represents a gamified training profile that consists of skills divided into subjects');
        cy.get('[data-cy="firstNewProjectButton"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('one');
        cy.get('[data-cy="saveProjectButton"]')
            .click();
        cy.get('[data-cy="projCard_one_manageBtn"]');
    });

    it('Provide clear instructions how to create a new project - regular user', function () {
        cy.visit('/administrator/');
        cy.contains('No Projects Yet...');
        cy.contains('Note: This section of SkillTree is for project administrators only. If you do not plan on creating and integrating a project with SkillTree then please return to the Progress and Ranking page.');
    });

    it('Preview project training plan', function () {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy=projectPreview]')
            .should('be.visible');
        cy.get('a[data-cy=projectPreview]')
            .should('have.attr', 'href')
            .and('include', '/progress-and-rankings/projects/proj1');
        cy.get('[data-cy=projectPreview]')
            .click();
        //opens in a new tab, cypress can't interact with those
    });

    it('Preview project training plan for non-production project', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.visit('/progress-and-rankings/projects/proj1');
        cy.dashboardCd()
            .contains('Overall Points');
        cy.contains('proj1');
    });

    it('Edit in place', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'Proj 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.intercept('GET', '/admin/projects/editedProjectId/subjects')
            .as('newIdSubjects');

        cy.visit('/administrator/projects/proj1/');
        cy.contains('PROJECT: Proj 1')
            .should('be.visible');
        cy.contains('ID: proj1')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.get('[data-cy=btn_edit-project]')
            .click();
        cy.get('input[data-cy=projectName]')
            .type('{selectall}Edited Name');
        cy.get('button[data-cy=saveProjectButton]')
            .click();
        cy.contains('PROJECT: Proj 1')
            .should('not.exist');
        cy.contains('PROJECT: Edited Name')
            .should('be.visible');

        cy.get('[data-cy=btn_edit-project]')
            .click();
        cy.get('[data-cy=idInputEnableControl] a')
            .click();
        cy.get('input[data-cy=idInputValue]')
            .type('{selectall}editedProjectId');
        cy.get('button[data-cy=saveProjectButton]')
            .click();
        cy.wait('@newIdSubjects');
        cy.contains('ID: proj1')
            .should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]')
            .should('not.exist');
        cy.contains('ID: editedProjectId')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-editedProjectId]')
            .should('be.visible');
        cy.get('a[data-cy=projectPreview]')
            .should('have.attr', 'href')
            .and('include', '/projects/editedProjectId');

        cy.location()
            .should((loc) => {
                expect(loc.pathname)
                    .to
                    .eq('/administrator/projects/editedProjectId/');
            });
        cy.contains('Subject 1')
            .should('be.visible');
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.contains('SUBJECT: Subject 1')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-editedProjectId]')
            .click();
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]')
            .click();
        cy.get('input[data-cy=subjectNameInput]')
            .type('{selectall}I Am A Changed Subject');
        cy.get('button[data-cy=saveSubjectButton]')
            .click();
        cy.contains('I Am A Changed Subject')
            .should('be.visible');
        cy.get('button[data-cy=btn_Subjects]')
            .click();
        cy.get('input[data-cy=subjectNameInput]')
            .type('A new subject');
        cy.get('button[data-cy=saveSubjectButton]')
            .click();
        cy.contains('A new subject')
            .should('be.visible');
    });

    it('Create new project using enter key', function () {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.intercept('POST', '/app/projects/MyNewtestProject')
            .as('postNewProject');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.clickButton('Project');
        cy.get('[data-cy="projectName"]')
            .type('My New test Project');
        cy.get('[data-cy="projectName"]')
            .type('{enter}');

        cy.wait('@postNewProject');

        cy.contains('My New test Project');
        cy.contains('ID: MyNewtestProject');
    });

    it('Open new project dialog with enter key', () => {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.get('[data-cy=newProjectButton]')
            .focus()
            .realPress('Enter');
        cy.get('[data-cy="projectName"]')
            .should('have.value', '');
        cy.get('[data-cy="projectNameError"]')
            .should('have.value', '');
        cy.get('[data-cy=closeProjectButton]')
            .click();
        cy.get('[data-cy="projectName"]')
            .should('not.exist');
    });

    it('Open edit project dialog using enter key', function () {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.intercept('POST', '/app/projects/test')
            .as('postNewProject');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.clickButton('Project');
        cy.get('[data-cy="projectName"]')
            .type('test');
        cy.get('[data-cy="projectName"]')
            .type('{enter}');

        cy.wait('@postNewProject');

        cy.contains('test');
        cy.contains('ID: test');

        cy.get('[data-cy=editProjBtn]')
            .focus();
        cy.realPress('Enter');

        cy.get('[data-cy="projectName"]')
            .should('have.value', 'test');
        cy.get('[data-cy="projectNameError"]')
            .should('have.value', '');
        cy.get('[data-cy=closeProjectButton]')
            .click();
        cy.contains('test');
        cy.contains('ID: test');
    });

    it('Close new project dialog', () => {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.intercept('POST', '/app/projects/MyNewtestProject')
            .as('postNewProject');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');

        cy.clickButton('Project');
        cy.get('[data-cy=closeProjectButton]')
            .click();
        cy.get('[data-cy="projectName"]')
            .should('not.exist');
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
        cy.contains('Enable')
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

    it('Project id autofill strips out special characters and spaces', () => {
        const expectedId = 'LotsofspecialPchars';
        const providedName = '!L@o#t$s of %s^p&e*c(i)a_l++_|}/[]#?{P c\'ha\'rs';

        cy.intercept('POST', `/app/projects/${expectedId}`)
            .as('postNewProject');
        cy.intercept('POST', '/app/projectExist')
            .as('projectExists');
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
        cy.wait('@projectExists');
        cy.getIdField()
            .should('have.value', expectedId);

        cy.clickSave();
        cy.wait('@postNewProject');

        cy.contains(`ID: ${expectedId}`);
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

    it('Once project id is enabled name-to-id autofill should be turned off', () => {
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
            .type('InitValue');
        cy.getIdField()
            .should('have.value', 'InitValue');

        cy.contains('Enable')
            .click();
        cy.contains('Enabled')
            .not('a');

        cy.get('[data-cy="projectName"]')
            .type('MoreValue');
        cy.getIdField()
            .should('have.value', 'InitValue');

        cy.get('[data-cy="projectName"]')
            .clear();
        cy.getIdField()
            .should('have.value', 'InitValue');
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
        cy.contains('Enable')
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
        cy.contains('Enable')
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
        const minLenMsg = 'Project Name cannot be less than 3 characters';
        const maxLenMsg = 'Project Name cannot exceed 50 characters';
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
        ;
        cy.contains('Enable')
            .click();
        cy.getIdField()
            .type('ProjectId');
        cy.get('[data-cy="projectName"]')
            .type('12');
        cy.contains(minLenMsg);

        cy.get('[data-cy="projectName"]')
            .type('3');
        cy.contains(minLenMsg)
            .should('not.exist');

        const longInvalid = Array(51)
            .fill('a')
            .join('');
        const longValid = Array(50)
            .fill('a')
            .join('');

        cy.get('[data-cy="projectName"]')
            .clear()
            .type(longInvalid);
        cy.contains(maxLenMsg);

        cy.get('[data-cy="projectName"]')
            .clear()
            .type(longValid);
        cy.contains(maxLenMsg)
            .should('not.exist');

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
        const minLenMsg = 'Project ID cannot be less than 3 characters';
        const maxLenMsg = 'Project ID cannot exceed 50 characters';
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
        ;
        cy.contains('Enable')
            .click();
        cy.getIdField()
            .type('12');
        cy.get('[data-cy="projectName"]')
            .type(projName);
        cy.contains(minLenMsg);

        cy.getIdField()
            .type('3');
        cy.contains(minLenMsg)
            .should('not.exist');

        cy.getIdField()
            .clear();
        cy.contains(requiredMsg);
        cy.getIdField()
            .click();
        cy.getIdField()
            .invoke('val', longInvalid)
            .trigger('input');
        cy.contains(maxLenMsg);

        cy.getIdField()
            .clear();
        cy.contains(requiredMsg);
        cy.getIdField()
            .click()
            .invoke('val', longValid)
            .trigger('input');
        cy.contains(maxLenMsg)
            .should('not.exist');
        cy.contains(requiredMsg)
            .should('not.exist');

        cy.clickSave();
        cy.wait('@postNewProject');

        cy.contains('ID: aaaaa');
    });

    it('Add Admin - User Not Found', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept({
            method: 'PUT',
            path: '/admin/projects/proj1/users/bar/roles/ROLE_PROJECT_ADMIN',
        }, {
            statusCode: 400,
            body: {
                errorCode: 'UserNotFound',
                explanation: 'User was not found'
            }
        })
            .as('addAdmin');

        cy.intercept({
            method: 'POST',
            path: '/app/users/suggest*',
        }, {
            statusCode: 200,
            body: [{
                userId: 'bar',
                userIdForDisplay: 'bar',
                first: 'bar',
                last: 'bar',
                dn: 'bar'
            }]
        })
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .click()
            .type('bar');
        cy.wait('@suggest');
        //cy.pause();
        //cy.get('.multiselect__input').type('{enter}');
        // cy.get('.multiselect__option multiselect__option--highlight').click();
        cy.get('[data-cy="existingUserInput"] .vs__dropdown-option')
            .eq(0)
            .click({ force: true });
        cy.clickButton('Add');
        cy.wait('@addAdmin');
        cy.get('.alert-danger')
            .contains('User was not found');
    });

    it('Add Admin - InternalError', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept({
            method: 'PUT',
            path: '/admin/projects/proj1/users/bar/roles/ROLE_PROJECT_ADMIN',
        }, {
            statusCode: 400,
            body: {
                errorCode: 'InternalError',
                explanation: 'Some Error Occurred'
            }
        })
            .as('addAdmin');

        cy.intercept({
            method: 'POST',
            path: '/app/users/suggest*',
        }, {
            statusCode: 200,
            body: [{
                userId: 'bar',
                userIdForDisplay: 'bar',
                first: 'bar',
                last: 'bar',
                dn: 'bar'
            }]
        })
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .click()
            .type('bar{enter}');
        cy.wait('@suggest');
        cy.get('[data-cy="existingUserInput"]')
            .click()
            .type('{enter}');
        cy.clickButton('Add');
        cy.wait('@addAdmin');
        cy.get('[data-cy="errorPage"]')
            .contains('Tiny-bit of an error!');
    });

    it('Add Admin No Query', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .type('{enter}');
        cy.wait('@suggest');
        cy.contains('root@skills.org')
            .click();
        cy.clickButton('Add');
        cy.wait('@addAdmin');

        const rowSelector = '[data-cy=roleManagerTable] tbody tr';
        cy.get(rowSelector)
            .should('have.length', 2)
            .as('cyRows');
        if (!Cypress.env('oauthMode')) {
            cy.get('@cyRows')
                .eq(0)
                .find('td')
                .as('row1');
            cy.get('@cyRows')
                .eq(1)
                .find('td')
                .as('row2');
            cy.get('@row1')
                .eq(0)
                .contains('root@skills.org');
            cy.get('@row2')
                .eq(0)
                .contains('skills@skills.org');
        } else {
            // the default user in oauth mode is different and results in a different sorting order
            cy.get('@cyRows')
                .eq(0)
                .find('td')
                .as('row1');
            cy.get('@cyRows')
                .eq(1)
                .find('td')
                .as('row2');
            cy.get('@row1')
                .eq(0)
                .contains('foo bar');
            cy.get('@row2')
                .eq(0)
                .contains('root@skills.org');
        }
    });

    it('Add and Remove Admin', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

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

        const tableSelector = '[data-cy=roleManagerTable]';
        const rowSelector = `${tableSelector} tbody tr`;
        cy.get(rowSelector)
            .should('have.length', 2)
            .as('cyRows');

        if (!Cypress.env('oauthMode')) {
            cy.get('@cyRows')
                .eq(0)
                .find('td')
                .as('row1');
            cy.get('@row1')
                .eq(0)
                .contains('root@skills.org');

            // remove the other user now
            cy.get(`${tableSelector} [data-cy="removeUserBtn"]`)
                .eq(0)
                .click();
            cy.contains('YES, Delete It')
                .click();

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
        } else {
            // in oauth mode the initial user has a different username which sorts lower than the root user in the default asc sort
            cy.get('@cyRows')
                .eq(1)
                .find('td')
                .as('row1');
            cy.get('@row1')
                .eq(1)
                .contains('root@skills.org');

            // remove the other user now
            cy.get(`${tableSelector} [data-cy="removeUserBtn"]`)
                .eq(1)
                .click();
            cy.contains('YES, Delete It')
                .click();

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
        }
    });

    it('Add Admin - forward slash character does not cause error', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .click()
            .type('root/bar{enter}');
        cy.wait('@suggest');
    });

    it('focus should be returned to new project button', () => {
        cy.visit('/administrator');
        cy.get('[data-cy=newProjectButton]')
            .click();
        cy.get('body')
            .type('{esc}');
        cy.get('[data-cy=newProjectButton]')
            .should('have.focus');

        cy.get('[data-cy=newProjectButton]')
            .click();
        cy.get('[data-cy=closeProjectButton]')
            .click();
        cy.get('[data-cy=newProjectButton]')
            .should('have.focus');

        cy.get('[data-cy=newProjectButton]')
            .click();
        cy.get('[data-cy=projectName]')
            .type('test 123');
        cy.get('[data-cy=saveProjectButton]')
            .click();
        cy.get('[data-cy=newProjectButton]')
            .should('have.focus');

        cy.get('[data-cy=newProjectButton]')
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[data-cy=newProjectButton]')
            .should('have.focus');
    });

    it('focus should be returned to project edit button', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'proj2'
        });
        cy.visit('/administrator/');
        const proj1EditBtn = '[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]';
        const proj2EditBtn = '[data-cy="projectCard_proj2"] [data-cy="editProjBtn"]';

        cy.get(proj1EditBtn)
            .click();
        cy.get('[data-cy=projectName]')
            .should('be.visible');
        cy.get('body')
            .type('{esc}{esc}');
        cy.get(proj1EditBtn)
            .should('have.focus');

        cy.get(proj1EditBtn)
            .click();
        cy.get('[data-cy=closeProjectButton]')
            .click();
        cy.get(proj1EditBtn)
            .should('have.focus');

        cy.get(proj1EditBtn)
            .click();
        cy.get('[data-cy=projectName]')
            .type('test 123');
        cy.get('[data-cy=saveProjectButton]')
            .click();
        cy.get(proj1EditBtn)
            .should('have.focus');

        cy.get(proj1EditBtn)
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get(proj1EditBtn)
            .should('have.focus');

        //project 2
        cy.get(proj2EditBtn)
            .click();
        cy.get('[data-cy=projectName]')
            .should('be.visible');
        cy.get('body')
            .type('{esc}{esc}');
        cy.get(proj2EditBtn)
            .should('have.focus');

        cy.get(proj2EditBtn)
            .click();
        cy.get('[data-cy=closeProjectButton]')
            .click();
        cy.get(proj2EditBtn)
            .should('have.focus');

        cy.get(proj2EditBtn)
            .click();
        cy.get('[data-cy=projectName]')
            .type('test 123');
        cy.get('[data-cy=saveProjectButton]')
            .click();
        cy.get(proj2EditBtn)
            .should('have.focus');

        cy.get(proj2EditBtn)
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get(proj2EditBtn)
            .should('have.focus');
    });

    it('new level dialog should return focus to new level button', () => {

        cy.intercept('GET', '/admin/projects/MyNewtestProject')
            .as('loadProject');

        cy.intercept('PUT', '/admin/projects/MyNewtestProject/levels/edit/**')
            .as('saveLevel');

        cy.intercept('GET', '/admin/projects/MyNewtestProject/levels')
            .as('loadLevels');

        cy.request('POST', '/app/projects/MyNewtestProject', {
            projectId: 'MyNewtestProject',
            name: 'My New test Project'
        });

        cy.visit('/administrator/projects/MyNewtestProject/');
        cy.wait('@loadProject');

        cy.contains('Levels')
            .click();
        cy.get('[data-cy=addLevel]')
            .click();
        cy.get('[data-cy=cancelLevel]')
            .click();
        cy.get('[data-cy=addLevel]')
            .should('have.focus');

        // cy.get('[data-cy=addLevel]').click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=addLevel]').should('have.focus');

        cy.get('[data-cy=addLevel]')
            .click();
        cy.get('[aria-label=Close]')
            .filter('.text-light')
            .click();
        cy.get('[data-cy=addLevel]')
            .should('have.focus');

        cy.get('[data-cy=editLevelButton]')
            .eq(0)
            .click();
        cy.get('[data-cy=cancelLevel]')
            .click();
        cy.get('[data-cy=editLevelButton]')
            .eq(0)
            .should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(0).click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]')
            .eq(0)
            .click();
        cy.get('[aria-label=Close]')
            .filter('.text-light')
            .click();
        cy.get('[data-cy=editLevelButton]')
            .eq(0)
            .should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(0).click();
        // cy.get('[data-cy=levelName]').type('{selectall}Fooooooo');
        // cy.get('[data-cy=saveLevelButton]').click();
        // cy.wait('@saveLevel');
        // cy.wait('@loadLevels');
        // cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]')
            .eq(3)
            .click();
        cy.get('[data-cy=cancelLevel]')
            .click();
        cy.get('[data-cy=editLevelButton]')
            .eq(3)
            .should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(3).click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

        cy.get('[data-cy=editLevelButton]')
            .eq(3)
            .click();
        cy.get('[aria-label=Close]')
            .filter('.text-light')
            .click();
        cy.get('[data-cy=editLevelButton]')
            .eq(3)
            .should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(3).click();
        // cy.get('[data-cy=levelName]').type('{selectall}Baaaaar');
        // cy.get('[data-cy=saveLevelButton]').click();
        // cy.wait('@saveLevel');
        // cy.wait('@loadLevels');
        // cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');
    });

    it('Trusted client should be shown when oAuthOnly!=true', () => {
        cy.intercept('GET', '/public/config', {
            oAuthOnly: false,
            authMode: 'FORM'
        })
            .as('loadConfig');

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept({
            method: 'PUT',
            url: '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN',
        })
            .as('addAdmin');

        cy.intercept({
            method: 'POST',
            url: '/app/users/suggestDashboardUsers*',
        })
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/*')
            .as('loadUserRoles');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadConfig');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');
        cy.wait('@loadUserRoles');

        cy.contains('Project Administrators')
            .should('exist');
        cy.get('[data-cy="trusted-client-props-panel"]')
            .should('exist');
    });

    it('Project stats should all be the same size when they wrap', () => {
        cy.setResolution([1440, 900]); //original issue presented when stat cards wrapped to another row

        cy.request('POST', '/app/projects/abcdeghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxy', {
            projectId: 'abcdeghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxy',
            name: 'abcdeghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxy'
        });
        cy.intercept('GET', '/admin/projects/abcdeghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxy')
            .as('loadProj');
        cy.intercept('GET', '/api/projects/Inception/level')
            .as('loadInception');
        cy.visit('/administrator/projects/abcdeghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxy/');
        cy.wait('@loadProj');
        cy.wait('@loadInception');

        cy.contains('No Subjects Yet');
        cy.get('[data-cy="pageHeader"]')
            .contains('ID: abcdeghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxy');
        cy.wait(2000);
        cy.get('[data-cy="pageHeader"] .container-fluid')
            .should('have.length', 1);
        cy.matchSnapshotImageForElement('[data-cy="pageHeader"] .container-fluid');

        cy.get('[data-cy=pageHeaderStat]')
            .first()
            .invoke('width')
            .then((val) => {
                cy.get('[data-cy=pageHeaderStat]')
                    .eq(1)
                    .invoke('width')
                    .should('eq', val);
                cy.get('[data-cy=pageHeaderStat]')
                    .eq(2)
                    .invoke('width')
                    .should('eq', val);
                cy.get('[data-cy=pageHeaderStat]')
                    .eq(3)
                    .invoke('width')
                    .should('eq', val);
                cy.get('[data-cy=pageHeaderStat]')
                    .eq(4)
                    .invoke('width')
                    .should('eq', val);
            });
    });

    it('Created and Last Reported Skill data should be visible on projects page', () => {
        cy.request('POST', '/app/projects/my_project_123', {
            projectId: 'my_project_123',
            name: 'My Project 123'
        });

        cy.request('POST', '/admin/projects/my_project_123/subjects/subj1', {
            projectId: 'my_project_123',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/my_project_123/subjects/subj1/skills/skill1`, {
            projectId: 'my_project_123',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 10,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.intercept('GET', '/api/projects/Inception/level')
            .as('loadInception');
        cy.visit('/administrator/');
        cy.wait('@getProjects');
        cy.wait('@loadInception');

        cy.get('[data-cy=projectCreated]')
            .should('be.visible')
            .contains('Today');
        cy.get('[data-cy=projectLastReportedSkill]')
            .should('not.exist');
    });

    it('Created and Last Reported Skill data should be visible on project page', () => {
        cy.request('POST', '/app/projects/my_project_123', {
            projectId: 'my_project_123',
            name: 'My Project 123'
        });

        cy.request('POST', '/admin/projects/my_project_123/subjects/subj1', {
            projectId: 'my_project_123',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/my_project_123/subjects/subj1/skills/skill1`, {
            projectId: 'my_project_123',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 10,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.intercept('GET', '/admin/projects/my_project_123')
            .as('loadProj');
        cy.intercept('GET', '/api/projects/Inception/level')
            .as('loadInception');
        cy.visit('/administrator/projects/my_project_123');
        cy.wait('@loadProj');
        cy.wait('@loadInception');

        cy.get('[data-cy=projectCreated]')
            .should('be.visible')
            .contains('Today');
        cy.get('[data-cy=projectLastReportedSkill]')
            .should('be.visible')
            .contains('Never');

        const now = dayjs()
            .utc();
        cy.reportSkill('my_project_123', 1, 'user@skills.org', now.subtract(1, 'year')
            .utc()
            .format('YYYY-MM-DD HH:mm'), false);

        cy.visit('/administrator/projects/my_project_123');
        cy.wait('@loadProj');
        cy.wait('@loadInception');
        cy.get('[data-cy=projectCreated]')
            .should('be.visible')
            .contains('Today');
        cy.get('[data-cy=projectLastReportedSkill]')
            .should('be.visible')
            .contains('a year ago');

        cy.reportSkill('my_project_123', 1, 'user@skills.org', now.subtract(2, 'months')
            .utc()
            .format('YYYY-MM-DD HH:mm'), false);
        cy.visit('/administrator/projects/my_project_123');
        cy.wait('@loadProj');
        cy.wait('@loadInception');
        cy.get('[data-cy=projectCreated]')
            .should('be.visible')
            .contains('Today');
        cy.get('[data-cy=projectLastReportedSkill]')
            .should('be.visible')
            .contains('2 months ago');

        cy.reportSkill('my_project_123', 1, 'user@skills.org', now.subtract(7, 'days')
            .utc()
            .format('YYYY-MM-DD HH:mm'), false);
        cy.visit('/administrator/projects/my_project_123');
        cy.wait('@loadProj');
        cy.wait('@loadInception');
        cy.get('[data-cy=projectCreated]')
            .should('be.visible')
            .contains('Today');
        cy.get('[data-cy=projectLastReportedSkill]')
            .should('be.visible')
            .contains('7 days ago');
    });

    it('project users input field submits on enter', () => {
        cy.request('POST', '/app/projects/my_project_123', {
            projectId: 'my_project_123',
            name: 'My Project 123'
        });

        cy.request('POST', '/admin/projects/my_project_123/subjects/subj1', {
            projectId: 'my_project_123',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/my_project_123/subjects/subj1/skills/skill1`, {
            projectId: 'my_project_123',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 10,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        const now = dayjs();
        cy.reportSkill('my_project_123', 1, 'user1@skills.org', now.subtract(1, 'year')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill('my_project_123', 1, 'user2@skills.org', now.subtract(1, 'year')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill('my_project_123', 1, 'user3@skills.org', now.subtract(1, 'year')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill('my_project_123', 1, 'user4@skills.org', now.subtract(1, 'year')
            .format('YYYY-MM-DD HH:mm'), false);

        cy.intercept('GET', '/admin/projects/my_project_123')
            .as('loadProj');
        cy.intercept('GET', '/api/projects/Inception/level')
            .as('loadInception');
        cy.intercept('GET', '/admin/projects/my_project_123/users**')
            .as('loadUsers');
        cy.visit('/administrator/projects/my_project_123');
        cy.wait('@loadProj');
        cy.wait('@loadInception');
        cy.get('[data-cy=nav-Users]')
            .click();
        cy.wait('@loadUsers');
        cy.get('[data-cy=usersTable_viewDetailsBtn]')
            .should('have.length', 4);
        cy.get('[data-cy=users-skillIdFilter]')
            .type('user1{enter}');
        cy.wait('@loadUsers');
        cy.get('[data-cy=usersTable_viewDetailsBtn]')
            .should('have.length', 1);
    });

    it('project-level settings: rank opt-out for all admins', () => {
        cy.createProject(1);
        cy.visit('/administrator/projects/proj1/settings');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .check({ force: true });

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .uncheck({ force: true });

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .check({ force: true });

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // refresh
        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .uncheck({ force: true });

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .check({ force: true });
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
    });

    it('project-level settings: set custom labels', () => {
        cy.createProject(1);
        cy.visit('/administrator/projects/proj1/settings');

        cy.get('[data-cy="customLabelsSwitch"')
            .should('not.be.checked');
        cy.get('[data-cy="customLabelsSwitch"')
            .click({ force: true });
        cy.get('[data-cy="customLabelsSwitch"')
            .should('be.checked');
        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', 'Project');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', 'Subject');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', 'Group');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', 'Skill');
        cy.get('[data-cy="levelDisplayTextInput"]')
            .should('have.value', 'Level');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy=projectDisplayTextInput]')
            .clear()
            .type('Work Role');
        cy.get('[data-cy=subjectDisplayTextInput]')
            .clear()
            .type('Competency');
        cy.get('[data-cy=groupDisplayTextInput]')
            .clear()
            .type('KSA');
        cy.get('[data-cy=skillDisplayTextInput]')
            .clear()
            .type('Course');
        cy.get('[data-cy=levelDisplayTextInput]')
            .clear()
            .type('Stage');

        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', 'Work Role');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', 'Competency');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', 'KSA');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', 'Course');
        cy.get('[data-cy="levelDisplayTextInput"]')
            .should('have.value', 'Stage');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // refresh
        cy.visit('/administrator/projects/proj1/settings');

        cy.get('[data-cy="customLabelsSwitch"')
            .should('be.checked');
        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', 'Work Role');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', 'Competency');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', 'KSA');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', 'Course');
        cy.get('[data-cy="levelDisplayTextInput"]')
            .should('have.value', 'Stage');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // set back to default
        cy.get('[data-cy=projectDisplayTextInput]')
            .clear();
        cy.get('[data-cy=subjectDisplayTextInput]')
            .clear();
        cy.get('[data-cy=groupDisplayTextInput]')
            .clear();
        cy.get('[data-cy=skillDisplayTextInput]')
            .clear();
        cy.get('[data-cy=levelDisplayTextInput]')
            .clear();

        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="levelDisplayTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // refresh, validate default is back
        cy.visit('/administrator/projects/proj1/settings');

        cy.get('[data-cy="customLabelsSwitch"')
            .should('not.be.checked');
        cy.get('[data-cy="customLabelsSwitch"')
            .click({ force: true });
        cy.get('[data-cy="customLabelsSwitch"')
            .should('be.checked');
        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', 'Project');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', 'Subject');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', 'Group');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', 'Skill');
        cy.get('[data-cy="levelDisplayTextInput"]')
            .should('have.value', 'Level');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy=projectDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=projectDisplayTextError]')
            .contains('Project Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=projectDisplayTextInput]')
            .clear();
        cy.get('[data-cy=projectDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=subjectDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=subjectDisplayTextError]')
            .contains('Subject Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=subjectDisplayTextInput]')
            .clear();
        cy.get('[data-cy=subjectDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=groupDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=groupDisplayTextError]')
            .contains('Group Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=groupDisplayTextInput]')
            .clear();
        cy.get('[data-cy=groupDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=skillDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=skillDisplayTextError]')
            .contains('Skill Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=skillDisplayTextInput]')
            .clear();
        cy.get('[data-cy=skillDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=levelDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=levelDisplayTextError]')
            .contains('Level Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=levelDisplayTextInput]')
            .clear();
        cy.get('[data-cy=levelDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
    });

    it('project-level settings: project visibility', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'PUBLIC');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Not Discoverable');

        cy.get('[data-cy="projectVisibilitySelector"]')
            .select('dpr');
        cy.get('[data-cy="saveSettingsBtn"')
            .click();
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'PUBLIC');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Discoverable');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('not.include.text', 'Not Discoverable');

        cy.get('[data-cy="projectVisibilitySelector"]')
            .select('pio');
        cy.get('.modal-content')
            .should('be.visible')
            .should('include.text', 'Changing to Invite Only')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users.');
        cy.clickButton('Ok');
        cy.get('[data-cy="saveSettingsBtn"')
            .click({ force: true });
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'PRIVATE');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Invite Only');

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait(200);
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'PRIVATE');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Invite Only');
    });

    it('navigate to subjects by click on project name', () => {
        cy.createProject(1);
        cy.visit('/administrator');
        cy.get('[data-cy="projCard_proj1_manageBtn"]');
        cy.get('[data-cy="projCard_proj1_manageLink"]')
            .click();
        cy.contains('No Subjects Yet');
    });

    it('delete project', () => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);
        cy.visit('/administrator');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="deleteProjBtn"]')
            .click();
        cy.contains('Removal Safety Check');
        cy.get('[data-cy=currentValidationText]')
            .type('Delete Me');
        cy.get('[data-cy=removeButton]')
            .should('be.enabled')
            .click();

        cy.get('[data-cy="projectCard_proj1"]')
            .should('not.exist');
        cy.get('[data-cy="projectCard_proj2"]')
            .should('exist');
        cy.get('[data-cy="projectCard_proj3"]')
            .should('exist');

        cy.get('[data-cy="projectCard_proj2"] [data-cy="deleteProjBtn"]')
            .click();
        cy.contains('Removal Safety Check');
        cy.get('[data-cy=closeRemovalSafetyCheck]')
            .click();
        cy.get('[data-cy="projectCard_proj2"]')
            .should('exist');
        cy.get('[data-cy="projectCard_proj3"]')
            .should('exist');
    });

    it('drag-and-drop project sort management', () => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);
        cy.createProject(4);
        cy.createProject(5);
        cy.visit('/administrator');

        const project1Card = '[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]';
        const project2Card = '[data-cy="projectCard_proj2"] [data-cy="sortControlHandle"]';
        const project4Card = '[data-cy="projectCard_proj4"] [data-cy="sortControlHandle"]';
        const project5Card = '[data-cy="projectCard_proj5"] [data-cy="sortControlHandle"]';

        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 1', 'This is project 2', 'This is project 3', 'This is project 4', 'This is project 5']);
        cy.get(project1Card)
            .dragAndDrop(project4Card);
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 4', 'This is project 1', 'This is project 5']);

        // refresh to make sure it was saved
        cy.visit('/administrator');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 4', 'This is project 1', 'This is project 5']);

        cy.get(project5Card)
            .dragAndDrop(project2Card);
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 5', 'This is project 2', 'This is project 3', 'This is project 4', 'This is project 1']);

        cy.get(project2Card)
            .dragAndDrop(project1Card);
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 5', 'This is project 3', 'This is project 4', 'This is project 1', 'This is project 2']);

        // refresh to make sure it was saved
        cy.visit('/administrator');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 5', 'This is project 3', 'This is project 4', 'This is project 1', 'This is project 2']);

    });

    it('no drag-and-drag sort controls when there is only 1 project', () => {
        cy.createProject(1);

        cy.visit('/administrator');
        cy.get('[data-cy="projectCard_proj1"]');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]')
            .should('not.exist');

        cy.createProject(2);
        cy.visit('/administrator');
        cy.get('[data-cy="projectCard_proj1"]');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]');
    });

    it('drag-and-drag sort should spinner while backend operation is happening', () => {
        cy.intercept('/admin/projects/proj1', (req) => {
            req.reply((res) => {
                res.send({ delay: 6000 });
            });
        })
            .as('proj1Async');

        cy.createProject(1);
        cy.createProject(2);

        const proj1Card = '[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]';
        const proj2Card = '[data-cy="projectCard_proj2"] [data-cy="sortControlHandle"]';

        cy.visit('/administrator');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 1', 'This is project 2']);
        cy.get(proj1Card)
            .dragAndDrop(proj2Card);

        // overlay over both cards but loading message only on project 1
        cy.get('[data-cy="proj1_overlayShown"] [data-cy="updatingSortMsg"]')
            .contains('Updating sort order');
        cy.get('[data-cy="proj2_overlayShown"]');
        cy.get('[data-cy="proj2_overlayShown"] [data-cy="updatingSortMsg"]')
            .should('not.exist');
        cy.wait('@proj1Async');
        cy.get('[data-cy="proj1_overlayShown"]')
            .should('not.exist');
        cy.get('[data-cy="proj2_overlayShown"]')
            .should('not.exist');
    });

    it('project card stats', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);

        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 2, 3);

        cy.createBadge(1, 1);
        cy.createProject(2);
        cy.createProject(3);
        cy.visit('/administrator');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Subjects"] [data-cy="statNum"]')
            .contains(2);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Skills"] [data-cy="statNum"]')
            .contains(3);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .contains(600);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Badges"] [data-cy="statNum"]')
            .contains(1);

        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Subjects"] [data-cy="statNum"]')
            .contains(0);
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Skills"] [data-cy="statNum"]')
            .contains(0);
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .contains(0);
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Badges"] [data-cy="statNum"]')
            .contains(0);

        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Subjects"] [data-cy="warning"]')
            .should('not.exist');
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Skills"] [data-cy="warning"]')
            .should('not.exist');
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Points"] [data-cy="warning"]')
            .should('exist');
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Badges"] [data-cy="warning"]')
            .should('not.exist');
    });

    it('page header rendering on small screen', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);

        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 2, 3);

        cy.setResolution('iphone-6');
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="manageBtn_subj1"]');
        cy.get('[data-cy="manageBtn_subj2"]');

        cy.matchSnapshotImage(`project-page-iphone6`, snapshotOptions);
    });

    it('change sort order using keyboard', () => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);
        cy.visit('/administrator/');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 1', 'This is project 2', 'This is project 3']);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 1', 'This is project 3']);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 1']);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 1']);
        cy.get('[data-cy="projectCard_proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // refesh and re-validate
        cy.visit('/administrator/');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 2', 'This is project 3', 'This is project 1']);

        // now let's move up
        cy.get('[data-cy="projectCard_proj3"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 3', 'This is project 2', 'This is project 1']);
        cy.get('[data-cy="projectCard_proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');
        cy.get('[data-cy="projectCard_proj3"] [data-cy="deleteProjBtn"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="projectCard"]', ['This is project 3', 'This is project 2', 'This is project 1']);
        cy.get('[data-cy="projectCard_proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');
    });

    it('invite only project full flow', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = ['abc@cba.org'];
                result.unsuccessful = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        const userIdForDisplay = 'highlander';
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*', (req) => {
            req.reply((res) => {
                const payload = res.body;
                if (payload.data && payload.data.length > 0) {
                    const userInfo = payload.data.find(el => el.userId === 'uuuuuu');
                    if (userInfo) {
                        userInfo.userIdForDisplay = userIdForDisplay;
                    }
                }
                res.send(payload);
            });
        })
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]')
            .select('pio');
        cy.get('.modal-content')
            .should('be.visible')
            .should('include.text', 'Changing to Invite Only')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users.');
        cy.clickButton('Ok');
        cy.get('[data-cy="saveSettingsBtn"')
            .click({ force: true });
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.get('[data-cy="inviteExpirationSelect"]')
            .select('PT30M');
        cy.get('[data-cy=addEmails]')
            .should('be.disabled');
        cy.get('[data-cy="sendInvites-btn"]')
            .should('be.disabled');
        cy.get('[data-cy="inviteEmailInput"]')
            .type('foo;@bar;abc@cba.org;Bob Smith <bsmith@fake.email>');
        cy.get('[data-cy=addEmails]')
            .click();
        cy.get('[data-cy=invalidEmails]')
            .should('be.visible')
            .should('include.text', 'Unable to add the following invalid email recipients: foo, @bar');
        cy.get('[data-cy=inviteRecipient]')
            .eq(0)
            .should('include.text', 'abc@cba.org');
        cy.get('[data-cy=inviteRecipient]')
            .eq(1)
            .should('include.text', 'bsmith@fake.email');
        cy.get('[data-cy="sendInvites-btn"]')
            .should('be.enabled')
            .click();
        cy.wait('@sendInvites');
        cy.get('[data-cy=failedEmails]')
            .should('be.visible')
            .should('include.text', 'Unable to send invites to: bsmith@fake.email');
        cy.logout();
        cy.wait(2000); //wait for invite only cache to clear

        cy.getLinkFromEmail()
            .then((inviteLink) => {
                cy.register('uuuuuu', 'password', false);
                cy.logout();
                cy.login('uuuuuu', 'password');

                cy.visit('/progress-and-rankings/projects/proj1');
                cy.contains('User Not Authorized')
                    .should('be.visible');

                cy.visit(inviteLink);
                cy.get('[data-cy=joinProject]')
                    .should('be.visible');
                cy.get('[data-cy=breadcrumb-item]')
                    .contains('Join Project This is project 1')
                    .should('be.visible');
                cy.get('[data-cy=joinProject]')
                    .click();
                cy.get('[data-cy=project-link-proj1]')
                    .should('be.visible')
                    .should('have.attr', 'href', '/progress-and-rankings/projects/proj1')
                    .contains('This is project 1')
                    .should('be.visible');
                cy.wait(10 * 1000); //wait for countdown timer
                cy.wait('@getToken');
                cy.dashboardCd()
                    .contains('My Level');
                cy.logout();

                cy.fixture('vars.json')
                    .then((vars) => {
                        if (!Cypress.env('oauthMode')) {
                            cy.log('NOT in oauthMode, using form login');
                            cy.login(vars.defaultUser, vars.defaultPass);
                        } else {
                            cy.log('oauthMode, using loginBySingleSignOn');
                            cy.loginBySingleSignOn();
                        }
                        cy.visit('/administrator/projects/proj1/access');
                        cy.wait('@emailSupported');
                        cy.wait('@getApprovedUsers');
                        cy.get('[data-cy=privateProjectUsersTable]')
                            .contains(userIdForDisplay)
                            .should('be.visible');
                        cy.get('[data-cy="privateProjectUsersTable_revokeUserAccessBtn"]')
                            .click();
                        cy.contains(`Are you sure you want to revoke ${userIdForDisplay}'s access to this Project? ${userIdForDisplay}'s achievements will NOT be deleted, however ${userIdForDisplay} will no longer be able to access the training profile.`)
                            .should('be.visible');
                        cy.clickButton('Yes, revoke access!');
                        cy.wait('@removeAccess');
                        cy.wait('@getApprovedUsers');
                        cy.get('[data-cy=privateProjectUsersTable]')
                            .contains(userIdForDisplay)
                            .should('not.exist');
                        cy.logout();
                        cy.login('uuuuuu', 'password');

                        cy.visit('/progress-and-rankings/projects/proj1');
                        cy.contains('User Not Authorized')
                            .should('be.visible');
                    });
            });

    });

    it('Invite Only duplicate input validation', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = ['abc@cba.org'];
                result.unsuccessful = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');
        cy.intercept('GET', '/admin/projects/proj1/errors*')
            .as('loadIssues');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]')
            .select('pio');
        cy.get('.modal-content')
            .should('be.visible')
            .should('include.text', 'Changing to Invite Only')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users.');
        cy.clickButton('Ok');
        cy.get('[data-cy="saveSettingsBtn"')
            .click({ force: true });
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.get('[data-cy="inviteExpirationSelect"]')
            .select('PT30M');
        cy.get('[data-cy=addEmails]')
            .should('be.disabled');
        cy.get('[data-cy="sendInvites-btn"]')
            .should('be.disabled');
        cy.get('[data-cy="inviteEmailInput"]')
            .type('abc@cba.org;abc@cba.org,abc@cba.org\nabc@cba.org\nBob Smith <abc@cba.org>');
        // try to navigate away while the input form has content
        cy.get('[data-cy="nav-Settings"')
            .click();
        cy.get('[data-cy="nav-Settings"]')
            .should('not.have.class', 'bg-primary');
        cy.get('[data-cy="nav-Access"]')
            .should('have.class', 'bg-primary');
        cy.contains('Discard Emails?')
            .should('be.visible');
        cy.contains('Cancel')
            .click();
        //navigation should be cancelled
        cy.get('[data-cy="nav-Settings"]')
            .should('not.have.class', 'bg-primary');
        cy.get('[data-cy="nav-Access"]')
            .should('have.class', 'bg-primary');
        cy.get('[data-cy="inviteEmailInput"]')
            .should('be.visible');
        cy.get('[data-cy=addEmails]')
            .click();
        cy.get('[data-cy=inviteRecipient]')
            .should('have.length', 1);
        cy.get('[data-cy="inviteEmailInput"]')
            .should('be.empty');
        cy.get('[data-cy=inviteRecipient]')
            .should('include.text', 'abc@cba.org');
        //try navigate away with recipients that have not yet been sent
        cy.get('[data-cy="nav-Issues"')
            .click();
        cy.get('[data-cy="nav-Issues"]')
            .should('not.have.class', 'bg-primary');
        cy.get('[data-cy="nav-Access"]')
            .should('have.class', 'bg-primary');
        cy.contains('Discard Recipients?')
            .should('be.visible');
        cy.contains('Let\'s Go!')
            .click();
        //navigation should proceed
        cy.wait('@loadIssues');
        cy.contains('Project Issues')
            .should('be.visible');
        cy.get('[data-cy="nav-Access"]')
            .should('not.have.class', 'bg-primary');
        cy.get('[data-cy="nav-Issues"]')
            .should('have.class', 'bg-primary');
    });

    if (!Cypress.env('oauthMode')) {
        it('revoke access should support paging when users exceed minimum page size', () => {
            cy.createInviteOnly();
            cy.intercept('GET', '/admin/projects/TestInviteOnlyProject1/settings')
                .as('getSettings');
            cy.intercept('POST', '/admin/projects/TestInviteOnlyProject1/settings')
                .as('saveSettings');
            cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
                .as('emailSupported');
            cy.intercept('GET', '/api/myprojects/TestInviteOnlyProject1/name')
                .as('getName');
            cy.intercept('GET', '/api/projects/TestInviteOnlyProject1/token')
                .as('getToken');
            cy.intercept('GET', '/admin/projects/TestInviteOnlyProject1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
                .as('getApprovedUsers');
            cy.intercept('DELETE', '/admin/projects/TestInviteOnlyProject1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
                .as('revokeUser');

            cy.visit('/administrator/projects/TestInviteOnlyProject1/access');
            cy.wait('@emailSupported');
            cy.wait('@getApprovedUsers');
            const tableSelector = '[data-cy=privateProjectUsersTable]';
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user9@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user8@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user7@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user6@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user55@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user3@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user1@skills.org'
                }],
            ], 5);

            cy.get('[data-cy="privateProjectUsers-userIdFilter"]')
                .type('user2');
            cy.get('[data-cy="privateProjectUsers-filterBtn"]')
                .click();
            cy.wait('@getApprovedUsers');
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
            ], 5, true, null, false);
            cy.get(`${tableSelector} [data-cy=skillsBTableTotalRows]`)
                .should('not.exist');
            cy.get(`${tableSelector} tbody [role="row"]`)
                .should('have.length', '2');

            cy.get('[data-cy=privateProjectUsers-resetBtn]')
                .click();
            cy.wait('@getApprovedUsers');
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user9@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user8@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user7@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user6@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user55@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user3@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user1@skills.org'
                }],
            ], 5);

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.contains('Yes, revoke access!')
                .click();
            cy.wait('@revokeUser');
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user1@skills.org'
                }],
            ], 5, true, null, false);
            cy.get('[data-cy=skillsBTableTotalRows]')
                .should('have.text', '8');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.contains('Cancel')
                .click();
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user1@skills.org'
                }],
            ], 5, true, null, false);
            cy.get('[data-cy=skillsBTableTotalRows]')
                .should('have.text', '8');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.contains('Yes, revoke access!')
                .click();
            cy.wait('@revokeUser');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.contains('Yes, revoke access!')
                .click();
            cy.wait('@revokeUser');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.contains('Yes, revoke access!')
                .click();
            cy.wait('@revokeUser');

            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user9@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user8@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user7@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user6@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user55@skills.org'
                }],
            ], 5, false, null, false);
            cy.get('[data-cy=skillsBTableTotalRows]')
                .should('not.exist');

        });
    }

});

