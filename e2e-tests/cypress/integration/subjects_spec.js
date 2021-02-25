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
// const attachFiles = require('cypress-form-data-with-file-upload');

describe('Subjects Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('Close level dialog', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');

        cy.contains('Levels').click();
        cy.contains('Add Next').click();
        cy.get('[data-cy=cancelLevel]').click();
        cy.get('[data-cy=cancelLevel]').should('not.exist');
    });

    it('create subject with special chars', () => {
        const expectedId = 'LotsofspecialPcharsSubject';
        const providedName = "!L@o#t$s of %s^p&e*c(i)/?#a_l++_|}{P c'ha'rs";
        cy.intercept('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');
        cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        cy.get('#subjName').type(providedName);
        cy.wait('@nameExists');
        cy.getIdField().should('have.value', expectedId);

        cy.clickSave();
        cy.wait('@postNewSubject');

        cy.contains('ID: Lotsofspecial')
    });

    it('create subject using enter key', () => {
        const expectedId = 'LotsofspecialPcharsSubject';
        const providedName = "!L@o#t$s of %s^p&e*c(i)/?#a_l++_|}{P c'ha'rs";
        cy.intercept('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');
        cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        cy.get('#subjName').type(providedName);
        cy.wait('@nameExists');
        cy.getIdField().should('have.value', expectedId);

        cy.get('#subjName').type('{enter}');
        cy.wait('@postNewSubject');

        cy.contains('ID: Lotsofspecial')
    });

    it('close subject dialog', () => {
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.get('[data-cy=closeSubjectButton]').should('not.exist');
    });

    it('name causes id to fail validation', () => {
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');
        cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        // name causes id to be too long
        const msg = 'Subject ID cannot exceed 50 characters.';
        const validNameButInvalidId = Array(44).fill('a').join('');
        cy.get('#subjName').click();
        cy.get('#subjName').invoke('val', validNameButInvalidId).trigger('input');
        cy.get('[data-cy=idError]').contains(msg).should('be.visible');
        cy.get('[data-cy=saveSubjectButton]').should('be.disabled');
        cy.get('#subjName').type('{backspace}');
        cy.get('[data-cy=idError]').should('be.empty');
        cy.get('[data-cy=saveSubjectButton]').should('be.enabled');
    });

    it('select font awesome icon', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1/');
        cy.get('.subject-settings .dropdown-toggle').click();
        cy.get('a.dropdown-item').contains('Edit').click({force:true});
        cy.get('div.modal-content .text-primary i.fa-question-circle').click();
        cy.get('a.nav-link').contains('Font Awesome Free').click();
        cy.wait(1500);
        cy.get('[role=tabpanel][aria-hidden=false]').should('be.visible');
        cy.get('[data-cy=fontAwesomeVirtualList]').scrollTo(0,540);
        cy.get('div[role=group] .icon-item>a:visible', {timeout:10000}).should('be.visible').last().then(($el)=> {
            const clazz = $el.attr('data-cy');
            cy.get(`[data-cy="${clazz}"]`).should('have.length', '1').click({force:true});
            cy.get('[data-cy=saveSubjectButton]').should('be.visible').click();
            cy.get('.preview-card-title').contains('Subject 1').should('be.visible');
            const classes = clazz.split(' ');
            let iconClass = classes[classes.length-1];
            iconClass = iconClass.replace(/-link$/, '')
            cy.get(`i.${iconClass}`).should('be.visible');
        })
    });

    it('select material icon', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1/');
        cy.get('.subject-settings .dropdown-toggle').click();
        cy.get('a.dropdown-item').contains('Edit').click();
        cy.get('div.modal-content .text-primary i.fa-question-circle').click();
        cy.get('a.nav-link').contains('Material').click();
        cy.wait(2500);
        cy.get('[role=tabpanel][aria-hidden=false]').should('be.visible');
        cy.get('[data-cy=materialVirtualList]').scrollTo(0,540);
        cy.get('div[role=group] .icon-item>a:visible',{timeout:1000}).last().then(($el)=> {
            const clazz = $el.attr('data-cy');
            cy.get(`[data-cy="${clazz}"]`).should('have.length', '1').click({ force: true });
            cy.get('[data-cy=saveSubjectButton]').should('be.visible').click();
            cy.get('.preview-card-title').contains('Subject 1').should('be.visible');
            const classes = clazz.split(' ');
            let iconClass = classes[classes.length - 1];
            iconClass = iconClass.replace(/-link$/, '')
            cy.get(`i.${iconClass}`).should('be.visible');
        });
    });

    it('search icons', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1/');
        cy.get('.subject-settings .dropdown-toggle').click();
        cy.get('a.dropdown-item').contains('Edit').click();
        cy.get('div.modal-content .text-primary i.fa-question-circle').click();
        cy.get('[data-cy=icon-search]').type('run');
        cy.get('.fas.fa-running').should('be.visible');
        //filter should persist between tab changes
        cy.get('a.nav-link').contains('Material').click();
        cy.get('.mi.mi-directions-run').should('be.visible');

        cy.get('a.nav-link').contains('Font Awesome Free').click();
        cy.get('.fas.fa-running').should('be.visible');

        //filter should not persist when icon manager is re-opened
        cy.contains('Cancel Icon Selection').click();
        cy.get('div.modal-content .text-primary i.fa-question-circle').click();
        cy.get('[data-cy=icon-search]').should('have.value', '');
        cy.get('i.fas.fa-ad').should('be.visible');
        cy.get('a.nav-link').contains('Material').click();
        cy.get('i.mi.mi-3d-rotation').should('be.visible');
    });

    it('upload custom icon', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });


        // TODO - waiting on https://github.com/cypress-io/cypress/issues/1647
        // cy.intercept('/admin/projects/proj1/icons/upload').as('uploadIcon');

        cy.server();
        cy.route({
            method: 'POST',
            url: '/admin/projects/proj1/icons/upload',
        }).as('uploadIcon');

        cy.visit('/administrator/projects/proj1/');

        cy.get('.subject-settings .dropdown-toggle').click();

        cy.get('a.dropdown-item').contains('Edit').click();

        cy.get('div.modal-content .text-primary i.fa-question-circle').click();

        cy.get('a.nav-link').contains('Custom').click();

        const filename = 'valid_icon.png';
        cy.get('input[type=file]').attachFile(filename);
        cy.wait('@uploadIcon')

        cy.get('#subj1___BV_modal_body_ .proj1-validiconpng');
    });


    it('upload custom icon - invalid mime type client validation', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1/');

        cy.get('.subject-settings .dropdown-toggle').click();

        cy.get('a.dropdown-item').contains('Edit').click();

        cy.get('div.modal-content .text-primary i.fa-question-circle').click();

        cy.get('a.nav-link').contains('Custom').click();

        const filename = 'invalid_file.txt';
        cy.get('input[type=file]').attachFile(filename);

        cy.get('.alert-danger').contains('File is not an image format');
    });

    it('upload custom icon - server side error', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.intercept('POST', '/admin/projects/proj1/icons/upload', {
            statusCode: 400,
            response: {explanation: 'Something bad'}
        }).as('addAdmin');

        cy.visit('/administrator/projects/proj1/');

        cy.get('.subject-settings .dropdown-toggle').click();

        cy.get('a.dropdown-item').contains('Edit').click();

        cy.get('div.modal-content .text-primary i.fa-question-circle').click();

        cy.get('a.nav-link').contains('Custom').click();

        const filename = 'valid_icon.png';
        cy.get('input[type=file]').attachFile(filename);

        cy.get('.toast-body').contains('Encountered error when uploading');
    });

    it('new subject button should retain focus after dialog closes', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/administrator/projects/proj1');
        cy.contains('Subjects').click();
        cy.get('[aria-label="new subject"]').click();
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.get('[aria-label="new subject"]').should('have.focus');

        cy.get('[aria-label="new subject"]').click();
        cy.get('body').type('{esc}');
        cy.get('[aria-label="new subject"]').should('have.focus');

        cy.get('[aria-label="new subject"]').click();
        cy.get('[aria-label=Close]').click();
        cy.get('[aria-label="new subject"]').should('have.focus');

        cy.get('[aria-label="new subject"]').click();
        cy.get('[data-cy=subjectNameInput]').type('foobarbaz');
        cy.get('[data-cy=saveSubjectButton]').click();
        cy.get('[aria-label="new subject"]').should('have.focus');
    });

    it('focus should be returned to subject edit button', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: "Subject 2"
        });
        cy.intercept({
            method: 'POST',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('saveSubject');
        cy.intercept({
            method: 'POST',
            url: '/admin/projects/proj1/subjects/subj2'
        }).as('saveSubject2');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj2'
        }).as('loadSubject2');

        cy.visit('/administrator/projects/proj1');
        cy.get('div.subject-settings').first().click();
        cy.get('[data-cy=editMenuEditBtn]').first().click();
        cy.get('[data-cy=subjectNameInput]').should('be.visible');
        cy.get('body').type('{esc}{esc}');
        cy.get('div.subject-settings').eq(0).children().first().should('have.focus');

        cy.get('div.subject-settings').first().click();
        cy.get('[data-cy=editMenuEditBtn]').first().click();
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.get('div.subject-settings').eq(0).children().first().should('have.focus');

        cy.get('div.subject-settings').first().click();
        cy.get('[data-cy=editMenuEditBtn]').first().click();
        cy.get('[data-cy=subjectNameInput]').type('test 123');
        cy.get('[data-cy=saveSubjectButton]').click();
        cy.wait('@saveSubject');
        cy.wait('@loadSubject');
        cy.get('div.subject-settings').eq(0).children().first().should('have.focus');

        cy.get('div.subject-settings').first().click();
        cy.get('[data-cy=editMenuEditBtn]').first().click();
        cy.get('[aria-label=Close]').click();
        cy.get('div.subject-settings').eq(0).children().first().should('have.focus');

        //subject 2
        cy.get('div.subject-settings').eq(1).click();
        cy.get('[data-cy=editMenuEditBtn]').eq(1).click();
        cy.get('[data-cy=subjectNameInput]').should('be.visible');
        cy.get('body').type('{esc}{esc}');
        cy.get('div.subject-settings').eq(1).children().first().should('have.focus');

        cy.get('div.subject-settings').eq(1).click();
        cy.get('[data-cy=editMenuEditBtn]').eq(1).click();
        cy.get('[data-cy=closeSubjectButton]').click();
        cy.get('div.subject-settings').eq(1).children().first().should('have.focus');

        cy.get('div.subject-settings').eq(1).click();
        cy.get('[data-cy=editMenuEditBtn]').eq(1).click();
        cy.get('[data-cy=subjectNameInput]').type('test 123');
        cy.get('[data-cy=saveSubjectButton]').click();
        cy.wait('@saveSubject2');
        cy.wait('@loadSubject2');
        cy.get('div.subject-settings').eq(1).children().first().should('have.focus');

        cy.get('div.subject-settings').eq(1).click();
        cy.get('[data-cy=editMenuEditBtn]').eq(1).click();
        cy.get('[aria-label=Close]').click();
        cy.get('div.subject-settings').eq(1).children().first().should('have.focus');
    });

    it('new level dialog should return focus to new level button', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');

        cy.intercept('PUT', '/admin/projects/proj1/subjects/subj1/levels/edit/*').as('saveLevel');

        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/levels').as('loadLevels');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');

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

    it('viewing subject user details does not break breadcrumb navigation', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'someuser', timestamp: new Date().getTime()})
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.clickNav('Users').click();
        cy.get('[data-cy="usersTable"]').contains('someuser').click();
        cy.get('[data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy=breadcrumb-Users]').should('be.visible');
    })

    it('description is validated against custom validators', () => {
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        cy.get('[data-cy="subjectNameInput"]').type('Great Name');
        cy.get('[data-cy="saveSubjectButton"]').should('be.enabled');
        //
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="subjectDescError"]').contains('Subject Description - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveSubjectButton"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveSubjectButton"]').should('be.enabled');
    });

});
