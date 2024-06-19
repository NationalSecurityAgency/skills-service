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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Approver Role Projects Table Tests', () => {
    const userName = 'approver@skills.org'
    const password = 'password'

    beforeEach(() => {
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });

        for (let i = 0; i < 10; i++) {
            cy.createProject(i+1)
        }

        cy.register(userName, password);
        cy.logout();
        cy.login(userName, password);

        cy.logout();

        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });
        for (let i = 0; i < 10; i++) {
            cy.request('POST', `/admin/projects/proj${i+1}/users/${userName}/roles/ROLE_PROJECT_APPROVER`);
        }

        cy.logout();
        cy.login(userName, password);

        cy.createProject(20)
    });

    it('approval role can approve and reject skills', function () {
        cy.visit('/administrator/')

        const runCheck = (projNum, manageButtonTxt = 'Manage', assertChainPrepend = null) => {
            const chainerPrepend = assertChainPrepend ? assertChainPrepend : '';

            cy.get(`[data-cy="projCard_proj${projNum}_manageBtn"]`).contains(manageButtonTxt)

            cy.get(`[data-cy="projectCard_proj${projNum}"] [data-cy="editProjBtn"]`).should(`${chainerPrepend}exist`)
            cy.get(`[data-cy="projectCard_proj${projNum}"] [data-cy="deleteProjBtn"]`).should(`${chainerPrepend}exist`)
        }
        runCheck(20)
        runCheck(10,  'View', 'not.')
        runCheck(9,  'View', 'not.')

        cy.get('[data-cy="projectCard_proj20"] [data-cy="userRole"]').should('have.text', 'Admin')
        cy.get('[data-cy="projectCard_proj10"] [data-cy="userRole"]').should('have.text', 'Approver')
        cy.get('[data-cy="projectCard_proj9"] [data-cy="userRole"]').should('have.text', 'Approver')
    })

});
