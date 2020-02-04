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

Cypress.Commands.add("disableUILogin", () => {
    Cypress.env('disabledUILoginProp', true);
    cy.log(`UI Login: [${Cypress.env('disabledUILoginProp')}] (enabled)`)
});

Cypress.Commands.add("enableUILogin", () => {
    Cypress.env('disabledUILoginProp', false)
    cy.log(`UI Login: [${Cypress.env('disabledUILoginProp')}] (disabled)`)
});

Cypress.Commands.add("login", (user, pass) => {
    const disableUILogin = Cypress.env('disabledUILoginProp')
    cy.log(`UI Login enabled: [${disableUILogin}]`)
    if ( disableUILogin === true){
        cy.log('Disabled UI Login')
    } else {
        cy.visit('/skills-login');
        cy.get('#username').type(user);
        cy.get('#inputPassword').type(pass);
        cy.contains('Login').click();
    }

    // this will allow to execute endpoint request directly to the backend
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



