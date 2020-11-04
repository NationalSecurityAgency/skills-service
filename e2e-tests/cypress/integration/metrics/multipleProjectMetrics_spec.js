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
    const waitForSnap = 4000;

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it ('only root or supervisor should see multiple project metrics', () => {
        cy.visit('/');
        cy.get('[data-cy=nav-Metrics]').should('not.exist')

        cy.logout();

        cy.fixture('vars.json').then((vars) => {
            cy.login(vars.rootUser, vars.defaultPass);
            cy.visit('/');
            cy.clickNav('Metrics');

            cy.logout();
            const supervisorUser = 'supervisor@skills.org';
            cy.register(supervisorUser, 'password');
            cy.login(vars.rootUser, vars.defaultPass);
            cy.request('PUT', `/root/users/${supervisorUser}/roles/ROLE_SUPERVISOR`);
            cy.logout();
            cy.login(supervisorUser, vars.defaultPass);
            cy.visit('/');
            cy.clickNav('Metrics');
        });
    });
})
