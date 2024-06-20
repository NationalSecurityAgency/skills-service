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

        cy.get('[data-cy="newProjectButton"]').click();
        cy.get('[data-cy="projectName"]')
            .type('My New & test Project');
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.wait('@postNewProject');

        cy.contains('My New & test Project');
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
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]').contains('proj1');
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
        cy.intercept('GET', '/admin/projects/proj1/userRoles**')
            .as('loadUserRoles');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadConfig');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');
        cy.wait('@loadUserRoles');

        cy.contains('Project Management Users')
            .should('exist');
        cy.get('[data-cy="trusted-client-props-panel"]')
            .should('exist');
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

    it('navigate to subjects by click on project name', () => {
        cy.createProject(1);
        cy.visit('/administrator');
        cy.get('[data-cy="projCard_proj1_manageBtn"]');
        cy.get('[data-cy="projCard_proj1_manageLink"]')
            .click();
        cy.contains('No Subjects Yet');
    });

    it('insufficient points warning', () => {
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

        cy.get('[data-cy="projectCard_proj1"] [data-cy="projectCardWarning"]').should('not.exist')
        cy.get('[data-cy="projectCard_proj2"] [data-cy="projectCardWarning"]')
            .contains('Project has insufficient points assigned');
        cy.get('[data-cy="projectCard_proj3"] [data-cy="projectCardWarning"]')
          .contains('Project has insufficient points assigned');

        cy.get('[data-cy="projectCard_proj2"] [data-cy="projectCardWarning"] [data-pc-section="closeicon"]').click();
        cy.get('[data-cy="projectCard_proj3"] [data-cy="projectCardWarning"]')
          .contains('Project has insufficient points assigned');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="projectCardWarning"]').should('not.exist')
        cy.get('[data-cy="projectCard_proj2"] [data-cy="projectCardWarning"]').should('not.exist')

        cy.visit('/administrator');
        cy.get('[data-cy="projectCard_proj3"] [data-cy="projectCardWarning"]')
          .contains('Project has insufficient points assigned');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="projectCardWarning"]').should('not.exist')
        cy.get('[data-cy="projectCard_proj2"] [data-cy="projectCardWarning"]').should('not.exist')

        cy.get('[data-cy="projCard_proj2_manageLink"]').click()
        cy.contains('No Subjects Yet')
        cy.get('[data-cy="projectInsufficientPoints"]')

        cy.get('[data-cy="breadcrumb-Projects"]').click()
        cy.get('[data-cy="projCard_proj1_manageLink"]').click()
        cy.get('[data-cy="subjectCard-subj1"]')
        cy.get('[data-cy="projectInsufficientPoints"]').should('not.exist')

        cy.visit('/administrator/projects/proj2');
        cy.contains('No Subjects Yet')
        cy.get('[data-cy="projectInsufficientPoints"]')

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="subjectCard-subj1"]')
        cy.get('[data-cy="projectInsufficientPoints"]').should('not.exist')
    });

    it('project description is retained after editing', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');
        cy.intercept('GET', '/app/projects/proj1/description').as('loadDescription');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');
        cy.intercept('POST', '/admin/projects/proj1').as('saveProject');

        // validate that edit on both /projects and /project/projId view retain edits to description
        const makdownDivSelector = '#markdown-editor div.toastui-editor-main.toastui-editor-ww-mode > div > div.toastui-editor-ww-container > div > div'
        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.get('[data-cy="btn_edit-project"]').click();
        cy.wait('@loadDescription');
        cy.get(makdownDivSelector).invoke('text').invoke('trim').should('equal', '')
        cy.get('[data-cy="markdownEditorInput"]').type('I am a description');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.wait('@saveProject');
        cy.get('[data-cy="btn_edit-project"]').click();
        cy.wait('@loadDescription');
        cy.get(makdownDivSelector).should('have.text', 'I am a description');
        cy.get('[data-cy="markdownEditorInput"]').click().type('jabberwocky jabberwocky jabberwocky');
        cy.wait('@validateDescription');
        cy.get('[data-cy="descriptionError"]').should('be.visible');
        cy.get('[data-cy="descriptionError"]').should('contain.text', 'Project Description - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="markdownEditorInput"]').click().type('{selectall}I am a description sans jw');
        cy.wait('@validateDescription');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.wait('@saveProject');
        cy.visit('/administrator/');
        cy.contains('This is project 1');
        cy.get('[data-cy="editProjBtn"]').click();
        cy.wait('@loadDescription');
        cy.get(makdownDivSelector).should('have.text', 'I am a description sans jw');
        cy.get('[data-cy="markdownEditorInput"]').click().type('{selectall}Am I a description?');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.wait('@saveProject');
        cy.contains('This is project 1');
        cy.get('[data-cy="editProjBtn"]').click();
        cy.wait('@loadDescription');
        cy.get(makdownDivSelector).should('have.text', 'Am I a description?');
    });

});

