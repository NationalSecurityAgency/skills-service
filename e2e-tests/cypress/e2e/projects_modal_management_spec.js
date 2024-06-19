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

describe('Projects Modal Management Tests', () => {
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
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy=deleteProjBtn]')
            .eq(0)
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.wait(200);
        cy.get('[data-cy=deleteProjBtn]')
            .eq(0)
            .should('have.focus');
    });

    it('Open new project dialog with enter key', () => {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="noProjectsYet"]').contains('No Projects Yet');
        cy.get('[data-cy=newProjectButton]').should('be.enabled')
        cy.get('[data-cy=newProjectButton]')
            .focus()
            .realPress('Enter');
        cy.get('[data-cy="projectName"]')
            .should('have.value', '');
        cy.get('[data-cy="projectNameError"]')
            .should('have.value', '');
        cy.get('[data-cy=closeDialogBtn]')
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
        cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy="newProjectButton"]').click();
        cy.get('[data-cy="projectName"]')
            .type('test');
        cy.get('[data-cy="projectName"]')
            .type('{enter}');

        cy.wait('@postNewProject');

        cy.contains('test');

        cy.get('[data-cy=editProjBtn]')
            .focus();
        cy.realPress('Enter');

        cy.get('[data-cy="projectName"]')
            .should('have.value', 'test');
        cy.get('[data-cy="projectNameError"]')
            .should('have.value', '');
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.contains('test');
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
        cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy="projectName"]')
            .should('not.exist');
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
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type(providedName);
        cy.wait('@projectExists');
        cy.getIdField()
            .should('have.value', expectedId);

        cy.clickSave();
        cy.wait('@postNewProject');
    });

    it('Once project id is enabled name-to-id autofill should be turned off', () => {
        cy.intercept('GET', '/app/projects')
            .as('loadProjects');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProjects');
        cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type('InitValue');
        cy.getIdField()
            .should('have.value', 'InitValue');

        cy.get('[data-cy="enableIdInput"] input').click();

        cy.get('[data-cy="projectName"]')
            .type('MoreValue');
        cy.getIdField()
            .should('have.value', 'InitValue');

        cy.get('[data-cy="projectName"]')
            .clear();
        cy.getIdField()
            .should('have.value', 'InitValue');
    });

    it('focus should be returned to new project button', () => {
        cy.visit('/administrator');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy=newProjectButton]')
            .click();
        cy.contains('New Project').should('be.visible');
        cy.get('[data-cy="projectName"').should('have.focus');
        cy.realPress('Escape');
        cy.get('[data-cy=newProjectButton]')
            .should('have.focus');

        cy.get('[data-cy=newProjectButton]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy=newProjectButton]')
            .should('have.focus');

        cy.get('[data-cy=newProjectButton]')
            .click();
        cy.get('[data-cy=projectName]')
            .type('test 123');
        cy.get('[data-cy=saveDialogBtn]')
            .click();
        cy.get('[data-cy=newProjectButton]')
            .should('have.focus');

        cy.get('[data-cy=newProjectButton]')
            .click();
        cy.get('[data-cy="projectName"]')
        cy.get('[data-pc-group-section="headericon"][data-pc-section="closebutton"]')
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
        cy.get('[data-cy="inception-button"]').contains('Level');
        const proj1EditBtn = '[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]';

        cy.get(proj1EditBtn)
            .click();
        cy.get('[data-cy=projectName]').should('be.visible');
        cy.wait(250);
        cy.realPress('Escape');
        cy.get('[data-cy=projectName]').should('not.exist');
        cy.wait(600)
        cy.get(proj1EditBtn).should('have.focus');

        cy.get(proj1EditBtn).click();
        cy.get('[data-cy=projectName]').should('be.visible');
        cy.get('[data-cy=closeDialogBtn]').click();
        cy.get('[data-cy=projectName]').should('not.exist');
        cy.wait(600)
        cy.get(proj1EditBtn).should('have.focus');

        cy.get(proj1EditBtn).click();
        cy.get('[data-cy=projectName]').type('test 123');
        cy.get('[data-cy=saveDialogBtn]').click();
        cy.get('[data-cy=projectName]').should('not.exist');
        cy.wait(600)
        cy.get(proj1EditBtn).should('have.focus');

        cy.get(proj1EditBtn).click();
        cy.get('[data-cy=projectName]').should('be.visible');
        cy.get('[data-pc-group-section="headericon"][data-pc-section="closebutton"]').click()
        cy.get('[data-cy=projectName]').should('not.exist');
        cy.wait(600)
        cy.get(proj1EditBtn).should('have.focus');
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
        cy.get('[data-cy="inception-button"]').contains('Level');

        cy.contains('Levels')
            .click();
        cy.get('[data-cy=addLevel]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy=addLevel]')
            .should('have.focus');

        // cy.get('[data-cy=addLevel]').click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=addLevel]').should('have.focus');

        cy.get('[data-cy=addLevel]')
            .click();
        cy.get('[data-pc-section="closebuttonicon"]')
            .click();
        cy.get('[data-cy=addLevel]')
            .should('have.focus');

        cy.get('[data-cy=editLevelButton]')
            .eq(0)
            .click();
        cy.get('[data-cy=closeDialogBtn]')
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
        cy.get('[data-pc-section="closebuttonicon"]')
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
        cy.get('[data-cy=closeDialogBtn]')
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
        cy.get('[data-pc-section="closebuttonicon"]')
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

    it('project users input field submits on enter', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        const now = dayjs();
        cy.reportSkill('proj1', 1, 'user1@skills.org', now.subtract(1, 'year')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill('proj1', 1, 'user2@skills.org', now.subtract(1, 'year')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill('proj1', 1, 'user3@skills.org', now.subtract(1, 'year')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill('my_project_123', 1, 'user4@skills.org', now.subtract(1, 'year')
            .format('YYYY-MM-DD HH:mm'), false);

        cy.visit('/administrator/projects/proj1/users');
        cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '3');
        cy.get('[data-cy=users-skillIdFilter]').should('be.visible')
        cy.get('[data-cy=users-skillIdFilter]')
            .type('user1{enter}');
        cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '1');
    });
});