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
// ***********************************************************
// This example support/index.js is processed and
// loaded automatically before your test files.
//
// This is a great place to put global configuration and
// behavior that modifies Cypress.
//
// You can change the location of this file or turn off
// automatically serving support files with the
// 'supportFile' configuration option.
//
// You can read more here:
// https://on.cypress.io/configuration
// ***********************************************************

// Import commands.js using ES2015 syntax:
import './commands'
import 'cypress-axe'

// Alternatively you can use CommonJS syntax:
// require('./commands')

Cypress.on('window:before:load', (win) => {
    cy.spy(win.console, 'error');
    cy.spy(win.console, 'warn');
});

before(function () {
    cy.fixture('vars.json').then((vars) => {
        cy.register(vars.rootUser, vars.defaultPass, true);
        cy.register(vars.defaultUser, vars.defaultPass);
        if (!Cypress.env('oauthMode')) {
            Cypress.env('proxyUser', 'user0')
        } else {
            Cypress.env('proxyUser', 'foo-hydra')
            Cypress.env('hydraAuthenticated', false)
        }
    });
});

beforeEach(function () {
    let disable = Cypress.env('disableResetDb');

    if (!disable) {
        cy.resetDb();
    } else {
        cy.log('Disabled [cy.resetDb()] in beforeEach')
    }

    cy.fixture('vars.json').then((vars) => {
        cy.logout()

        if (!Cypress.env('oauthMode')) {
            cy.log('NOT in oauthMode, using form login')
            cy.login(vars.defaultUser, vars.defaultPass);
        } else {
            cy.log('oauthMode, using loginBySingleSignOn')
            cy.loginBySingleSignOn()
        }
    });
});


afterEach(function () {
    Cypress.env('hydraAuthenticated', false);

    cy.window().then((win) => {
        expect(win.console.error).to.have.callCount(0);
        expect(win.console.warn).to.have.callCount(0);
    });
});

