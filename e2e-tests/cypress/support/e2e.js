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
import 'cypress-axe';
import 'cypress-plugin-tab';
import "cypress-real-events/support";
import moment from 'moment-timezone';
import { clear } from 'idb-keyval';

// Alternatively you can use CommonJS syntax:
// require('./commands')

Cypress.on('window:before:load', (win) => {
    cy.spy(win.console, 'error').as('consoleError')
    cy.spy(win.console, 'warn').as('consoleWarn')
});

before(function () {
    cy.fixture('vars.json').then((vars) => {
        cy.register(vars.rootUser, vars.defaultPass, true);
        if (!Cypress.env('verifyEmail')) {
            cy.register(vars.defaultUser, vars.defaultPass);
            if (!Cypress.env('oauthMode')) {
                Cypress.env('proxyUser', 'skills@skills.org')
            } else {
                Cypress.env('proxyUser', 'foo-hydra')
                Cypress.env('hydraAuthenticated', false)
            }
        }
    });
});

beforeEach(function () {
    // this will abort XHR requests from previous test
    // https://github.com/cypress-io/cypress/issues/216#issuecomment-247361529
    // cy.visit('');
    cy.window().then((win) => {
        if (win && win.location) {
            cy.log('setting win.location.href to blank')
            win.location.href = 'about:blank'
        }
    })

    clear();

    let disable = Cypress.env('disableResetDb');

    if (!disable) {
        cy.resetDb();
    } else {
        cy.log('Disabled [cy.resetDb()] in beforeEach')
    }
    cy.resetEmail();
    cy.fixture('vars.json').then((vars) => {
        cy.logout()
        cy.login(vars.rootUser, vars.defaultPass);
        cy.log('configuring email');
        cy.request({
            method: 'POST',
            url: '/root/saveEmailSettings',
            body: {
                publicUrl: 'http://localhost:8080/',
                fromEmail: 'noreploy@skilltreeemail.org',
                host: 'localhost',
                port: 1025,
                'protocol': 'smtp'
            },
        });
        cy.logout();

        if (!Cypress.env('verifyEmail')) {
            cy.loginAsAdminUser()
        }
    });
    cy.log(`[${Cypress.currentTest.title}] [${moment.utc().toISOString()}] start`)
});


afterEach(function () {
    cy.log(`[${Cypress.currentTest.title}] [${moment.utc().toISOString()}] end`)
    Cypress.env('hydraAuthenticated', false);
    const aliases = cy.state('aliases');
    // TODO: put this back when the time is right
    // if (!Cypress.env('ignoreConsoleErrors')) {
    //     if (aliases && aliases.consoleError) {
    //         cy.get('@consoleError').should('not.be.called')
    //     }
    //     Cypress.env('ignoreConsoleErrors', false); // reset flag
    // }
    // if (!Cypress.env('ignoreConsoleWarnings')) {
    //     if (aliases && aliases.consoleWarn) {
    //         cy.get('@consoleWarn').should('not.be.called')
    //     }
    //     Cypress.env('ignoreConsoleWarnings', false);  // reset flag
    // }
});

// Cypress.on('fail', (err) => {
//     console.error(err)
//     err.message = `on [${moment.utc().toISOString()}] \n ${err.message}`
//     throw err
// });

Cypress.on('command:start', ({ attributes }) => {
    if (attributes.type === 'parent') {
        Cypress.log({
            name: `[${moment.utc().toISOString()}] ${attributes.name} ----------`,
        })
    }
});
