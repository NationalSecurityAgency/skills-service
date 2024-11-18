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
describe('Register Root Users', () => {

    beforeEach(() => {
        cy.logout();
        cy.clearDb();
    });

    afterEach(() => {
        cy.logout();
        cy.clearDb();

        cy.fixture('vars.json')
            .then((vars) => {
                cy.register(vars.rootUser, vars.defaultPass, true);
                cy.register(vars.defaultUser, vars.defaultPass);
            });
    });

    it('register root user', () => {
        const username = 'rob.smith@madeup.org';
        const pass = 'password';

        cy.visit('/administrator/');
        cy.contains('New Root Account');
        cy.get('[data-cy="requestAccountFirstName"]')
            .type('Robert');
        cy.get('[data-cy="requestAccountLastName"]')
            .type('Smith');
        cy.get('[data-cy="requestAccountEmail"]')
            .type(username);
        cy.get('#password')
            .type(pass);
        cy.get('[data-cy="requestAccountConfirmPassword"]')
            .type('password');
        cy.contains('Create Account')
            .click();

        // default test install sets Progress and Ranking as a default home page
        cy.url()
            .should('include', '/progress-and-ranking');
    });

    it('create account form is not shown for oauth only bootstrap', () => {
        cy.intercept('GET', '/public/config', {
            oAuthOnly: true,
            rankingAndProgressViewsEnabled: true,
            needToBootstrap: true
        })
            .as('loadConfig');
        cy.intercept('GET', '/app/oAuthProviders', [{
            'registrationId': 'gitlab',
            'clientName': 'GitLab',
            'iconClass': 'fab fa-gitlab'
        }])
            .as('getOauthProviders');

        cy.visit('/administrator/');

        cy.wait('@loadConfig');
        cy.wait('@getOauthProviders');
        cy.contains('New Root Account');

        cy.get('[data-cy="requestAccountFirstName"]')
            .should('not.exist');
        cy.get('[data-cy="requestAccountLastName"]')
            .should('not.exist');
        cy.get('[data-cy="requestAccountEmail"]')
            .should('not.exist');
        cy.get('#password')
            .should('not.exist');
        cy.get('[data-cy="requestAccountConfirmPassword"]')
            .should('not.exist');
        cy.contains('Create Account')
            .should('not.exist');
        cy.get('[data-cy=oAuthProviders]')
            .should('exist');
        cy.contains('Login via GitLab');
    });

    it('register root user when Progress and Ranking views are disabled', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.rankingAndProgressViewsEnabled = 'false';
                result.needToBootstrap = true;
                res.send(result);
            });
        }).as('getConfig');

        const username = 'rob.smith@madeup.org';
        const pass = 'password';

        cy.visit('/administrator/');
        cy.wait('@getConfig');
        cy.contains('New Root Account');

        cy.get('[data-cy="requestAccountFirstName"]')
            .type('Robert');
        cy.get('[data-cy="requestAccountLastName"]')
            .type('Smith');
        cy.get('[data-cy="requestAccountEmail"]')
            .type(username);
        cy.get('#password')
            .type(pass);
        cy.get('[data-cy="requestAccountConfirmPassword"]')
            .type('password');
        cy.contains('Create Account')
            .click();

        // if rankingAndProgressViewsEnabled are disabled then always navigate to admin page
        cy.url()
            .should('include', '/administrator');

        cy.get('[data-cy="newProjectButton"]')
            .click();
        cy.wait(2000)
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.get('[data-cy="saveDialogBtn"]')
            .click();
        cy.get('[data-cy="projCard_NewProject_manageLink"]');
    });

    it('register root user when default home page configured - progress and ranking', () => {

        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.rankingAndProgressViewsEnabled = 'true';
                result.defaultLandingPage = 'progress';
                result.needToBootstrap = true;
                res.send(result);
            });
        }).as('getConfig');

        const username = 'rob.smith@madeup.org';
        const pass = 'password';

        cy.visit('/administrator/');
        cy.wait('@getConfig');
        cy.contains('New Root Account');

        cy.get('[data-cy="requestAccountFirstName"]')
            .type('Robert');
        cy.get('[data-cy="requestAccountLastName"]')
            .type('Smith');
        cy.get('[data-cy="requestAccountEmail"]')
            .type(username);
        cy.get('#password')
            .type(pass);
        cy.get('[data-cy="requestAccountConfirmPassword"]')
            .type('password');
        cy.contains('Create Account')
            .click();
        cy.get('[data-cy="manageMyProjsBtnInNoContent"]')

        // if rankingAndProgressViewsEnabled are enabled
        cy.url()
            .should('include', '/progress-and-ranking');
    });

    it('register root user when default home page configured - admin', () => {

        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.rankingAndProgressViewsEnabled = 'true';
                result.defaultLandingPage = 'admin';
                result.needToBootstrap = true;
                res.send(result);
            });
        }).as('getConfig');

        const username = 'rob.smith@madeup.org';
        const pass = 'password';

        cy.visit('/administrator/');
        cy.wait('@getConfig');
        cy.contains('New Root Account');

        cy.get('[data-cy="requestAccountFirstName"]')
            .type('Robert');
        cy.get('[data-cy="requestAccountLastName"]')
            .type('Smith');
        cy.get('[data-cy="requestAccountEmail"]')
            .type(username);
        cy.get('#password')
            .type(pass);
        cy.get('[data-cy="requestAccountConfirmPassword"]')
            .type('password');
        cy.contains('Create Account')
            .click();

        // if rankingAndProgressViewsEnabled are disabled then always navigate to admin page
        cy.url()
            .should('include', '/administrator');
    });

});
