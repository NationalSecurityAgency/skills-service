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

describe('Projects Admin Management Tests', () => {
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

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type('My New test Project');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();

        cy.wait('@postNewProject');

        cy.get('[data-cy="projCard_MyNewtestProject_manageLink"]');
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
        cy.get('[data-cy="projectPreview"]')
            .should('be.visible');
        cy.get('[data-cy=btn_edit-project]')
            .click();
        cy.get('input[data-cy=projectName]')
            .type('{selectall}Edited Name');
        cy.get('button[data-cy=saveDialogBtn]')
          .should('be.enabled')
            .click();
        cy.contains('PROJECT: Proj 1')
            .should('not.exist');
        cy.contains('PROJECT: Edited Name')
            .should('be.visible');

        cy.get('[data-cy=btn_edit-project]')
            .click();
        cy.get('[data-cy=enableIdInput]').click();
        cy.get('input[data-cy=idInputValue]')
            .type('{selectall}editedProjectId');
        cy.get('button[data-cy="saveDialogBtn"]')
          .should('be.enabled')
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
        cy.get('[data-cy="projectPreview"]')
            .should('have.attr', 'href')
            .and('include', '/projects/editedProjectId');

        cy.location()
            .should((loc) => {
                expect(loc.pathname)
                    .to
                    .eq('/administrator/projects/editedProjectId');
            });
        cy.get('[data-cy=btn_edit-project]').should('be.focused')

        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.contains('SUBJECT: Subject 1')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-editedProjectId]')
            .click();
        cy.wait('@newIdSubjects')
        cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();
        cy.get('input[data-cy=subjectName]')
            .type('{selectall}I Am A Changed Subject');
        cy.get('button[data-cy=saveDialogBtn]')
            .click();
        cy.contains('I Am A Changed Subject')
            .should('be.visible');
        cy.get('button[data-cy=btn_Subjects]')
            .click();
        cy.get('input[data-cy=subjectName]')
            .type('A new subject');
        cy.get('button[data-cy=saveDialogBtn]')
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

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]')
            .type('My New test Project');
        cy.get('[data-cy="projectName"]')
            .type('{enter}');

        cy.wait('@postNewProject');

        cy.get('[data-cy="projCard_MyNewtestProject_manageLink"]');
    });

    it('delete project', () => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);
        cy.visit('/administrator');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="deleteProjBtn"]')
            .click();
        cy.contains('Removal Safety Check');

        cy.get('#stepOneCheck').check();
        cy.get('[data-cy="firstNextButton"]').click();

        cy.get('#stepTwoCheck').check();
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy=currentValidationText]')
            .type('Delete This Project');
        cy.get('[data-cy="deleteProjectButton"]')
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
        cy.get('[data-cy="removalSafetyCheckMsg"] [data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="projectCard_proj2"]')
            .should('exist');
        cy.get('[data-cy="projectCard_proj3"]')
            .should('exist');
    });

    it('respect max projects per admin config', () => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createProject(3);

        cy.intercept('GET', '/public/config', (req) => {
            req.continue((res) => {
                res.body.maxProjectsPerAdmin = 3
            })
        })
          .as('getConfig');

        cy.visit('/administrator')
        cy.wait('@getConfig')

        cy.get('[data-cy="addProjectDisabledWarning"]').contains('The maximum number of Projects allowed is 3')
        cy.get('[data-cy="newProjectButton"]').should('be.disabled')
    })

});