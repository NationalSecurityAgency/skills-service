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
        const providedName = "!L@o#t$s of %s^p&e*c(i)a_l++_|}{P c'ha'rs";
        cy.server().route('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');

        cy.visit('/projects/proj1');
        cy.clickButton('Subject');

        cy.get('#subjName').type(providedName)
        cy.getIdField().should('have.value', expectedId)

        cy.clickSave();
        cy.wait('@postNewSubject');

        cy.contains('ID: Lotsofspecial')
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
        cy.fixture(filename).then(fileContent => {
            cy.get('input[type=file]').upload(
                {fileContent: fileContent, fileName: filename, mimeType: 'text/plain'},
                { force: true });

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
        cy.fixture(filename).then(fileContent => {
            cy.get('input[type=file]').upload(
                {fileContent: fileContent, fileName: filename, mimeType: 'image/png'},
                { force: true });

            cy.get('.toast-body').contains('Encountered error when uploading');
        });
    });


});
