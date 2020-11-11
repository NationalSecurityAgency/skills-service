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
var moment = require('moment-timezone');

describe('Multiple Project Metrics', () => {
    const multiProjSel = '[data-cy=multiProjectUsersInCommon]';
    const trainingProfSel = '[data-cy=trainingProfileComparator]';

    beforeEach(() => {
        Cypress.Commands.add("usrsInCommon", (optionalSelector = null) => {
            return cy.get(`${multiProjSel}${optionalSelector ? (' ' + optionalSelector) : ''}`);
        });
        Cypress.Commands.add("trainingProf", (optionalSelector = null) => {
            return cy.get(`${trainingProfSel}${optionalSelector ? (' ' + optionalSelector) : ''}`);
        });

        cy.fixture('vars.json').then((vars) => {
            cy.logout();

            cy.login(vars.rootUser, vars.defaultPass);
            cy.request('PUT', `/root/users/${vars.defaultUser}/roles/ROLE_SUPERVISOR`);

            cy.logout();
            cy.login(vars.defaultUser, vars.defaultPass);
        });
    });


    it('2 projects required for training profile comparator to work', () => {
        cy.visit('/');
        cy.clickNav('Metrics');
        cy.trainingProf().contains('Feature is disabled');

        cy.request('POST', `/app/projects/proj1`, {
            projectId: 'proj1',
            name: `Grand Project 1`
        })

        cy.clickNav('Projects');
        cy.get('[data-cy=projectCard]').contains(`Grand Project 1`);

        cy.clickNav('Metrics');

        cy.trainingProf().contains('Feature is disabled').should('not.exist');

        // just make sure that a chart is displaying
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Inception');
        cy.trainingProf('[data-cy=numOfSkillsChart]').contains('Grand Project 1');
    });

    it('2 projects needed to enable users in common chart', () => {
        cy.visit('/');
        cy.clickNav('Metrics');
        cy.usrsInCommon().contains('No Projects Selected').should('not.exist');

        // for (let i=0; i<5; i+= 1) {
        //     cy.usrsInCommon('[data-cy=projectSelector]')
        //         .click();
        //     cy.usrsInCommon()
        //         .contains(`Grand Project ${i}`)
        //         .click();
        // }
    });


})
