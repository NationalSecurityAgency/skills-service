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
describe('Contact Project Admins Specs', () => {

    beforeEach(() => {
        cy.logout();
        cy.resetEmail();

        cy.fixture('vars.json')
            .then((vars) => {
                cy.register(vars.rootUser, vars.defaultPass, true);
            });

        cy.login('root@skills.org', 'password');

        cy.request({
            method: 'POST',
            url: '/root/saveEmailSettings',
            body: {
                host: 'localhost',
                port: 1025,
                'protocol': 'smtp',
                publicUrl: 'http://localhost:8082/',
                fromEmail: 'noreploy@skilltreeemail.org',
            },
        });

        cy.request({
            method: 'POST',
            url: '/root/saveSystemSettings',
            body: {
                resetTokenExpiration: 'PT2H',
            }
        });

        cy.logout();
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });

    });

    it('only visible to root users', () => {
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER')
            .as('isRoot');
        cy.visit('/administrator');
        cy.wait('@isRoot');
        cy.get('[data-cy="nav-Contact Admins"]')
            .should('be.visible');
        cy.logout();
        cy.register('user1', 'password1', false);
        cy.login('user1', 'password1');
        cy.visit('/administrator');
        cy.wait('@isRoot');
        cy.get('[data-cy="nav-Contact Admins"]')
            .should('not.exist');

        cy.on('uncaught:exception', (err, runnable) => {
            return false
        })
        cy.visit('/administrator/contactAdmins');
        cy.get('[data-cy="errorPage"]').contains('User Not Authorized')
            .should('be.visible');
    });

    it('contact user form query interactions', () => {

        cy.logout();
        cy.register('user1', 'password1', false);
        cy.login('user1', 'password1');

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.logout();
        cy.register('user2', 'password1', false);
        cy.login('user2', 'password1');

        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'proj2'
        });

        cy.logout();
        cy.register('user3', 'password1', false);
        cy.login('user3', 'password1');

        cy.request('POST', '/app/projects/proj3', {
            projectId: 'proj3',
            name: 'proj3'
        });

        cy.logout();

        cy.intercept('/root/users/countAllProjectAdmins')
            .as('countAdmins');
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
                cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER')
                    .as('isRoot');

                cy.visit('/administrator/');
                cy.get('[data-cy="nav-Contact Admins"]')
                    .click();
                cy.wait('@isRoot');
                cy.wait('@countAdmins');
                cy.get('[data-cy=projectAdminCount]')
                    .should('have.text', '4');
                cy.get('[data-cy=emailUsers-submitBtn]')
                    .should('be.disabled');

                cy.get('[data-cy=emailUsers_subject]')
                    .type('foooo');
                cy.get('[data-cy="markdownEditorInput"]')
                    .type('thisbody');
                cy.get('[data-cy=emailUsers-submitBtn]')
                    .should('be.enabled');
                cy.get('[data-cy=emailUsers-submitBtn]')
                    .click();
                cy.get('[data-cy=emailSent]')
                    .should('be.visible');

                cy.getEmails().then((emails) => {
                    expect(emails[0].text).to.contain('thisbody');
                })
            });

    });

    it('email not enabled on instance', () => {
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');

        cy.visit('/administrator/');
        cy.get('[data-cy="nav-Contact Admins"]')
            .click();
        cy.get('[data-cy=contactUsers_emailServiceWarning]')
            .should('be.visible');
        cy.contains('Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.')
            .should('be.visible');
    });

    it('preview email', () => {
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER')
            .as('isRoot');
        cy.intercept('POST', '/api/validation/description')
          .as('validateDescription');

        cy.resetEmail()
        cy.visit('/administrator/');

        cy.get('[data-cy="nav-Contact Admins"]')
            .click();
        cy.wait('@isRoot');
        cy.wait(1000);

        cy.get('[data-cy=previewAdminEmail]')
            .should('be.disabled');
        cy.get('[data-cy=emailUsers_subject]')
            .type('Test Subject');
        cy.get('[data-cy=previewAdminEmail]')
            .should('be.disabled');
        cy.get('[data-cy="markdownEditorInput"]')
            .type('Test Body Preview');
        cy.wait('@validateDescription');
        cy.get('[data-cy=previewAdminEmail]')
            .should('be.enabled');
        cy.get('[data-cy=previewAdminEmail]')
            .click();
        cy.get('[data-cy=emailSent]')
            .should('be.visible');

        cy.getEmails().then((emails) => {
            expect(emails[0].text).to.contain('TestBodyPreview');
        })
    });

    it('attachments are not enabled', () => {

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
          .as('emailSupported');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER')
          .as('isRoot');

        cy.intercept('POST', '/root/users/previewEmail', {
            statusCode: 200,
            body: {
                success: true
            }
        });

        cy.visit('/administrator/');

        cy.get('[data-cy="nav-Contact Admins"]')
          .click();
        cy.wait('@isRoot');

        cy.get(`button.bold`).should('exist');
        cy.get(`button.attachment-button`).should('not.exist');
    });

    it('attachments cannot be uploaded via drag and drop', () => {

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
          .as('emailSupported');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER')
          .as('isRoot');

        cy.intercept('POST', '/root/users/previewEmail', {
            statusCode: 200,
            body: {
                success: true
            }
        });

        cy.visit('/administrator/');

        cy.get('[data-cy="nav-Contact Admins"]')
          .click();
        cy.wait('@isRoot');
        const markdownInput = '[data-cy=markdownEditorInput] div.toastui-editor-contents[contenteditable="true"]';
        cy.get(markdownInput).focus().selectFile('cypress/attachments/test-pdf.pdf', { action: 'drag-drop' })

        cy.get('a[href^="/api/download/"]:contains(test-pdf.pdf)').should('not.exist');
    });

    it('validation works correctly', () => {
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER')
            .as('isRoot');
        cy.intercept('POST', '/api/validation/description')
          .as('validateDescription');

        cy.intercept('POST', '/root/users/previewEmail', {
            statusCode: 200,
            body: {
                success: true
            }
        });

        cy.visit('/administrator/');

        cy.get('[data-cy="nav-Contact Admins"]')
            .click();
        cy.wait('@isRoot');
        cy.wait(1000);

        cy.get('[data-cy="emailUsers-submitBtn"]').should('be.disabled');

        cy.get('[data-cy="emailUsers_subject"]').type('test');
        cy.get('[data-cy="emailUsers_body"]').type('test');
        cy.wait('@validateDescription');
        cy.get('[data-cy="emailUsers-submitBtn"]').should('be.enabled');

        cy.get('[data-cy="emailUsers_subject"]').type('jabberwocky');
        cy.get('[data-cy="emailUsers-submitBtn"]').should('be.disabled');
        cy.get('#subjectLineError').contains('paragraphs may not contain jabberwocky')

        cy.get('[data-cy="emailUsers_subject"]').clear();
        cy.get('[data-cy="emailUsers_subject"]').type('test');
        cy.get('[data-cy="emailUsers-submitBtn"]').should('be.enabled');
        cy.get('#subjectLineError').should('be.empty');

        cy.get('[data-cy="emailUsers_body"]').type('jabberwocky');
        cy.wait('@validateDescription');
        cy.get('[data-cy="emailUsers-submitBtn"]').should('be.disabled');
        cy.get('#emailBodyError').contains('paragraphs may not contain jabberwocky')

        cy.get('[data-cy="emailUsers_body"]').type('{selectall}{backspace}');
        cy.get('[data-cy="emailUsers_body"]').type('test');
        cy.wait('@validateDescription');
        cy.get('[data-cy="emailUsers-submitBtn"]').should('be.enabled');
        cy.get('#emailBodyError').should('be.empty');
    });

});
