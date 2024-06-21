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
describe('User Agreement Specs', ()=> {
   beforeEach(() => {
       cy.register('user1@fake.fake', 'password1', false);
       cy.logout();
       cy.login('user1@fake.fake', 'password1');
       cy.request('POST', '/app/projects/projBanana', {
           projectId: 'projBanana',
           name: 'Bananas!'
       });
       cy.logout();
       cy.fixture('vars.json').then((vars) => {
           cy.login(vars.rootUser, vars.defaultPass);
       });

   });

   it('user must acknowledge user agreement after logging in', () => {
       cy.request('POST', '/root/saveSystemSettings', {
           publicUrl: 'http://foo.bar',
           userAgreement: '#This is a user agreement\n* one \n * two \n * 3\n more text'
       });

       cy.intercept('GET', '/app/userAgreement').as('loadUserAgreement');
       cy.intercept('GET', '/admin/projects/projBanana').as('loadProject');
       cy.intercept('POST', '/app/userInfo/settings').as('acknowledgeUa');
       cy.intercept('POST', '/logout').as('logout');
       cy.intercept('POST', '/performLogin').as('performLogin');
       cy.intercept('GET', '/api/myProgressSummary').as('progressSummary');
       cy.logout();
       cy.visit('/administrator/projects/projBanana');
       cy.get('#username').type('user1@fake.fake');
       cy.get('#inputPassword').type('password1');
       cy.get('[data-cy=login]').click();
       cy.wait('@loadUserAgreement');
       cy.contains('User Agreement');
       cy.get('[data-cy="breadcrumb-User Agreement"]').should('not.exist');
       cy.get('[data-cy=userAgreement]').should('be.visible');
       cy.get('[data-cy=rejectUserAgreement]').should('be.visible');
       cy.get('[data-cy=acknowledgeUserAgreement]').should('be.visible').click();
       cy.wait('@acknowledgeUa');
       cy.wait('@loadProject');
       cy.contains('PROJECT: Bananas').should('be.visible');
       cy.get('[data-cy=breadcrumb-projBanana]').should('be.visible');

       cy.get('[data-cy="settings-button"] button').click();
       cy.contains('Log Out').click();
       cy.contains('Email Address').should('be.visible');
       cy.get('#username').type('user1@fake.fake');
       cy.get('#inputPassword').type('password1');
       cy.get('[data-cy=login]').click();
       cy.wait('@performLogin');
       cy.wait('@progressSummary');
       cy.visit('/administrator/projects/projBanana');
       cy.wait('@loadProject');

       cy.contains('PROJECT: Bananas!').should('be.visible');
       cy.get('[data-cy=breadcrumb-projBanana]').should('be.visible');
   });

    it('user may not navigate to other links in application if agreement is not acknowledged', () => {
        cy.request('POST', '/root/saveSystemSettings', {
            publicUrl: 'http://foo.bar',
            userAgreement: '#This is a user agreement\n* one \n * two \n * 3\n more text'
        });

        cy.intercept('GET', '/app/userAgreement').as('loadUserAgreement');
        cy.intercept('GET', '/admin/projects/projBanana/').as('loadProject');
        cy.logout();
        cy.login('user1@fake.fake', 'password1');
        cy.visit('/');
        cy.wait('@loadUserAgreement');
        cy.contains('User Agreement').should('be.visible');
        cy.get('[data-cy="breadcrumb-User Agreement"]').should('not.exist');
        cy.get('[data-cy=userAgreement]').should('be.visible');

        cy.visit('/settings/');
        cy.wait('@loadUserAgreement');
        cy.contains('User Agreement').should('be.visible');
        cy.get('[data-cy=userAgreement]').should('be.visible');

        cy.visit('/progress-and-rankings/');
        cy.wait('@loadUserAgreement');
        cy.contains('User Agreement').should('be.visible');
        cy.get('[data-cy=userAgreement]').should('be.visible');
    });

    it('rejecting user agreement returns user to login page', () => {
        cy.request('POST', '/root/saveSystemSettings', {
            publicUrl: 'http://foo.bar',
            userAgreement: '#This is a user agreement\n* one \n * two \n * 3\n more text'
        });

        cy.intercept('GET', '/app/userAgreement').as('loadUserAgreement');
        cy.intercept('GET', '/admin/projects/projBanana/').as('loadProject');
        cy.intercept('POST', '/logout').as('logout');

        cy.logout();
        cy.login('user1@fake.fake', 'password1');
        cy.visit('/');
        cy.wait('@loadUserAgreement');
        cy.contains('User Agreement').should('be.visible');
        cy.get('[data-cy=userAgreement]').should('be.visible');

        cy.get('[data-cy=rejectUserAgreement]').click();
        cy.wait('@logout');
        cy.location().should((loc) => {
            expect(loc.pathname).to.eq('/skills-login');
        });
    });

    it('rejecting user agreement does not impact subsequent login with user who previously accepted', () => {
        cy.request('POST', '/root/saveSystemSettings', {
            publicUrl: 'http://foo.bar',
            userAgreement: '#This is a user agreement\n* one \n * two \n * 3\n more text'
        });

        cy.intercept('GET', '/app/userAgreement').as('loadUserAgreement');
        cy.intercept('GET', '/admin/projects/projBanana/').as('loadProject');
        cy.intercept('POST', '/logout').as('lo');
        cy.intercept('POST', '/app/userInfo/settings').as('acknowledgeUa');
        cy.intercept('GET', ' /app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/api/myProgressSummary').as('progressSummary');

        cy.visit('/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadUserAgreement');
        cy.get('[data-cy=acknowledgeUserAgreement]').should('be.visible').click();
        cy.wait('@acknowledgeUa');

        cy.logout();
        cy.login('user1@fake.fake', 'password1');
        cy.visit('/');
        cy.wait('@loadUserInfo');
        cy.wait('@loadUserAgreement');
        cy.contains('User Agreement').should('be.visible');
        cy.get('[data-cy=userAgreement]').should('be.visible');

        cy.get('[data-cy=rejectUserAgreement]').click();
        cy.location().should((loc) => {
            expect(loc.pathname).to.eq('/skills-login');
        });

        cy.fixture('vars.json').then((vars) => {
            cy.get('#username').type(vars.rootUser);
            cy.get('#inputPassword').type(vars.defaultPass);
            cy.get('[data-cy=login]').click();
            cy.wait('@loadUserInfo');
            cy.contains('Progress And Rankings');
            cy.wait('@progressSummary');
            cy.contains('User Agreement').should('not.exist');
            cy.get('[data-cy=acknowledgeUserAgreement]').should('not.exist');
        });
    });


    it('edits to user agreement require acknowledgement of new ua on next login', () => {
        cy.request('POST', '/root/saveSystemSettings', {
            publicUrl: 'http://foo.bar',
            userAgreement: '#This is a user agreement\n* one \n * two \n * 3\n more text'
        });

        cy.intercept('GET', '/app/userAgreement').as('loadUserAgreement');
        cy.intercept('GET', '/admin/projects/projBanana/').as('loadProject');
        cy.intercept('GET', '/root/getSystemSettings').as('loadSystemSettings');
        cy.logout();
        cy.fixture('vars.json').then((vars) => {
            cy.login(vars.rootUser, vars.defaultPass);
            cy.visit('/settings/system');
            cy.wait('@loadUserAgreement');
            cy.contains('User Agreement');
            cy.get('[data-cy=userAgreement]').should('be.visible');
            cy.get('[data-cy=acknowledgeUserAgreement]').should('be.visible').click();
            cy.wait('@loadSystemSettings');
            cy.get('[data-cy="markdownEditorInput"]').type('change change change');
            cy.get('[data-cy=saveSystemSettings]').click();
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass);
            cy.visit('/');
            cy.wait('@loadUserAgreement');
            cy.contains('User Agreement');
            cy.get('[data-cy=userAgreement]').should('be.visible');
        })
    });
});
