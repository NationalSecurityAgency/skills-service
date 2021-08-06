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

        cy.fixture('vars.json').then((vars) => {
            cy.register(vars.rootUser, vars.defaultPass, true);
        });

        cy.login('root@skills.org', 'password');

        cy.request({
            method: 'POST',
            url: '/root/saveEmailSettings',
            body: {
                host: 'localhost',
                port: 1026,
                'protocol': 'smtp'
            },
        });

        cy.request({
            method: 'POST',
            url: '/root/saveSystemSettings',
            body: {
                publicUrl: 'http://localhost:8082/',
                resetTokenExpiration: 'PT2H',
                fromEmail: 'noreploy@skilltreeemail.org',
            }
        });

        cy.logout();
        cy.logout();
        cy.fixture('vars.json').then((vars) => {
            cy.login(vars.rootUser, vars.defaultPass);
        });

    });

    it('only visible to root users', () => {
        cy.visit('/administrator');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER').as('isRoot');
        cy.wait('@isRoot');
        cy.get('[data-cy="nav-Contact Admins"]').should('be.visible');
        cy.logout();
        cy.register('user1', 'password1', false);
        cy.login('user1', 'password1');
        cy.visit('/administrator');
        cy.wait('@isRoot');
        cy.get('[data-cy="nav-Contact Admins"]').should('not.exist');
        cy.visit('/administrator/contactAdmins');
        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
    });

    it('contact user form query interactions', () => {

        cy.logout();
        cy.register('user1', 'password1', false);
        cy.login('user1', 'password1');

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        });

        cy.logout();
        cy.register('user2', 'password1', false);
        cy.login('user2', 'password1');

        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: "proj2"
        });

        cy.logout();
        cy.register('user3', 'password1', false);
        cy.login('user3', 'password1');

        cy.request('POST', '/app/projects/proj3', {
            projectId: 'proj3',
            name: "proj3"
        });

        cy.logout();

        cy.intercept('/root/users/countAllProjectAdmins').as('countAdmins');
        cy.fixture('vars.json').then((vars) => {
            cy.login(vars.rootUser, vars.defaultPass);
            cy.visit('/administrator/');

            cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER').as('isRoot');
            cy.get('[data-cy="nav-Contact Admins"]').click();
            cy.wait('@isRoot');
            cy.wait('@countAdmins');
            cy.get('[data-cy=projectAdminCount]').should('have.text', '4');
            cy.get('[data-cy=emailUsers-submitBtn]').should('be.disabled');

            cy.get('[data-cy=emailUsers_subject]').type('foooo');
            cy.get('[data-cy="markdownEditorInput"]').type('body');
            cy.get('[data-cy=emailUsers-submitBtn]').should('be.enabled');
        });

    });

    it('email not enabled on instance', () => {
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');

        cy.visit('/administrator/');
        cy.get('[data-cy="nav-Contact Admins"]').click();
        cy.get('[data-cy=contactUsers_emailServiceWarning]').should('be.visible');
        cy.contains('Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.').should('be.visible');
    })

    it('preview email button', () => {
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('emailSupported');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER').as('isRoot');

        cy.visit('/administrator/');

        cy.get('[data-cy="nav-Contact Admins"]').click();
        cy.wait('@isRoot');

        cy.get('[data-cy=previewUsersEmail]').should('be.disabled');
        cy.get('[data-cy=emailUsers_subject]').type('Test Subject');
        cy.get('[data-cy=previewUsersEmail]').should('be.disabled');
        cy.get('[data-cy="markdownEditorInput"]').type('Test Body');
        cy.get('[data-cy=previewAdminEmail]').should('be.enabled');
    });
});
