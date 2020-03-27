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
// ***********************************************
// This example commands.js shows you how to
// create various custom commands and overwrite
// existing commands.
//
// For more comprehensive examples of custom
// commands please read more here:
// https://on.cypress.io/custom-commands
// ***********************************************
//
//
// -- This is a parent command --
// Cypress.Commands.add("login", (email, password) => { ... })
//
//
// -- This is a child command --
// Cypress.Commands.add("drag", { prevSubject: 'element'}, (subject, options) => { ... })
//
//
// -- This is a dual command --
// Cypress.Commands.add("dismiss", { prevSubject: 'optional'}, (subject, options) => { ... })
//
//
// -- This will overwrite an existing command --
// Cypress.Commands.overwrite("visit", (originalFn, url, options) => { ... })

import { addMatchImageSnapshotCommand } from 'cypress-image-snapshot/command';
import './cliend-display-commands';
import 'cypress-file-upload';

addMatchImageSnapshotCommand();

Cypress.Commands.add("register", (user, pass, grantRoot) => {
    return cy.request(`/app/users/validExistingDashboardUserId/${user}`)
        .then((response) => {
            if (response.body !== true) {
                cy.log(`Creating user [${user}]`)
                cy.request('PUT', '/createAccount', {
                    firstName: 'Firstname',
                    lastName: 'LastName',
                    email: user,
                    password: pass,
                });
                if (grantRoot) {
                    cy.request('POST', '/grantFirstRoot');
                }
                cy.request('POST', '/logout');
            } else {
                cy.log(`User [${user}] already exist`)
            }
        });

});

Cypress.Commands.add("login", (user, pass) => {
    cy.request( {
        method: 'POST',
        url: '/performLogin',
        body: {
            username: user,
            password: pass
        },
        form: true,
    })
});

Cypress.Commands.add("logout", () => {
    cy.request('POST', '/logout');
});

Cypress.Commands.add("clickSave", () => {
    cy.get("button:contains('Save')").click();
});

Cypress.Commands.add("clickButton", (label) => {
    cy.get(`button:contains('${label}')`).click();
});

Cypress.Commands.add("getIdField", () => {
    return cy.get("#idInput");
});


Cypress.Commands.add("setResolution", (size) => {
    if (size !== 'default') {
        if (Cypress._.isArray(size)) {
            cy.viewport(size[0], size[1]);
        } else {
            cy.viewport(size);
        }
        cy.log(`Set viewport to ${size}`);
    } else {
        cy.log(`Using default viewport`);
    }
});

Cypress.Commands.add('vuex', () => {
   return cy.window().its('vm.$store');
});


