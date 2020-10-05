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

    it('create subject with special chars', () => {
        const expectedId = 'LotsofspecialPcharsSubject';
        const providedName = "!L@o#t$s of %s^p&e*c(i)/?#a_l++_|}{P c'ha'rs";
        cy.server();
        cy.route('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');
        cy.route('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
        cy.route('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

        cy.visit('/projects/proj1');
        cy.wait('@loadSubjects');
        cy.clickButton('Subject');

        cy.get('#subjName').type(providedName);
        cy.wait('@nameExists');
        cy.getIdField().should('have.value', expectedId);

        cy.clickSave();
        cy.wait('@postNewSubject');

        cy.contains('ID: Lotsofspecial')
    });

    it.only('select font awesome icon', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.server();

        cy.visit('/projects/proj1/');
        cy.get('.subject-settings .dropdown-toggle').click();
        cy.get('a.dropdown-item').contains('Edit').click({force:true});
        cy.get('div.modal-content .text-info i.fa-question-circle').click();
        cy.get('a.nav-link').contains('Font Awesome Free').click();
        cy.get('[data-cy=fontAwesomeVirtualList]').scrollTo(0,540);
        cy.get('.icon-item>a:visible', {timeout:10000}).should('be.visible').last().then(($el)=> {
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

    it.only('select material icon', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.server();

        cy.visit('/projects/proj1/');
        cy.get('.subject-settings .dropdown-toggle').click();
        cy.get('a.dropdown-item').contains('Edit').click();
        cy.get('div.modal-content .text-info i.fa-question-circle').click();
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
        cy.server();

        cy.visit('/projects/proj1/');
        cy.get('.subject-settings .dropdown-toggle').click();
        cy.get('a.dropdown-item').contains('Edit').click();
        cy.get('div.modal-content .text-info i.fa-question-circle').click();
        cy.get('[data-cy=icon-search]').type('run');
        cy.get('.fas.fa-running').should('be.visible');
        //filter should persist between tab changes
        cy.get('a.nav-link').contains('Material').click();
        cy.get('.mi.mi-directions-run').should('be.visible');

        cy.get('a.nav-link').contains('Font Awesome Free').click();
        cy.get('.fas.fa-running').should('be.visible');

        //filter should not persist when icon manager is re-opened
        cy.contains('Cancel Icon Selection').click();
        cy.get('div.modal-content .text-info i.fa-question-circle').click();
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
        cy.server();

        cy.route({
            method: 'POST',
            url: '/admin/projects/proj1/icons/upload',
        }).as('uploadIcon');

        cy.visit('/projects/proj1/');

        cy.get('.subject-settings .dropdown-toggle').click();

        cy.get('a.dropdown-item').contains('Edit').click();

        cy.get('div.modal-content .text-info i.fa-question-circle').click();

        cy.get('a.nav-link').contains('Custom').click();

        const filename = 'valid_icon.png';
        cy.fixture(filename, 'binary')
            .then(Cypress.Blob.binaryStringToBlob)
            .then((fileContent) => {
                cy.get('input[type=file]').attachFile({ fileContent, filePath: filename, encoding: 'utf-8' });
                cy.wait('@uploadIcon')

                cy.get('#subj1___BV_modal_body_ .proj1-validiconpng');
            });
    });


    it('upload custom icon - invalid mime type client validation', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/projects/proj1/');

        cy.get('.subject-settings .dropdown-toggle').click();

        cy.get('a.dropdown-item').contains('Edit').click();

        cy.get('div.modal-content .text-info i.fa-question-circle').click();

        cy.get('a.nav-link').contains('Custom').click();

        const filename = 'invalid_file.txt';
        cy.fixture(filename, 'binary')
            .then(Cypress.Blob.binaryStringToBlob)
            .then((fileContent) => {
                cy.get('input[type=file]').attachFile({ fileContent, filePath: filename, encoding: 'utf-8' });

                cy.get('.alert-danger').contains('File is not an image format');
            });
    });

    it('upload custom icon - server side error', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.server();

        cy.route({
            method: 'POST',
            url: '/admin/projects/proj1/icons/upload',
            status: 400,
            response: {explanation: 'Something bad'}
        }).as('addAdmin');

        cy.visit('/projects/proj1/');

        cy.get('.subject-settings .dropdown-toggle').click();

        cy.get('a.dropdown-item').contains('Edit').click();

        cy.get('div.modal-content .text-info i.fa-question-circle').click();

        cy.get('a.nav-link').contains('Custom').click();

        const filename = 'valid_icon.png';
        cy.fixture(filename, 'binary')
            .then(Cypress.Blob.binaryStringToBlob)
            .then((fileContent) => {
                cy.get('input[type=file]').attachFile({ fileContent, filePath: filename, encoding: 'utf-8' });

                cy.get('.toast-body').contains('Encountered error when uploading');
            });
    });


});
