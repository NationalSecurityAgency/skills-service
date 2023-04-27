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

describe('Community Projects Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    before(() => {
        cy.beforeTestSuiteThatReusesData()
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request({
                method: 'POST',
                url: '/root/saveSystemSettings',
                body: {
                    customHeader: '<div class="bg-success text-center text-white py-1 community-desc-header">{{ community.descriptor}}</div>',
                    customFooter: '<div class="bg-success text-center text-white py-1 community-desc-footer">{{ community.descriptor}}</div>',
                }
            });
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
            cy.createProject(1)
            cy.createProject(2)

            cy.request('POST', '/admin/projects/proj1/settings', [
                {
                    value: true,
                    setting: 'user_community',
                    projectId: 'proj1',
                },
            ]);
            cy.request('POST', `/admin/projects/proj1/users/${allDragonsUser}/roles/ROLE_PROJECT_ADMIN`);
            cy.request('POST', `/admin/projects/proj2/users/${allDragonsUser}/roles/ROLE_PROJECT_ADMIN`);
        });
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    it('dynamically populate "community.descriptor" prop for the header token substitution', () => {
        cy.visit('/administrator')
        cy.get('.community-desc-header').should('have.text', 'Divine Dragon')
        cy.get('.community-desc-footer').should('have.text', 'Divine Dragon')

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(allDragonsUser, vars.defaultPass);
        });

        cy.visit('/administrator')
        cy.get('.community-desc-header').should('have.text', 'All Dragons')
        cy.get('.community-desc-footer').should('have.text', 'All Dragons')
    });

    it('"community.descriptor" prop for the header token substitution - header is updated after user logged in', () => {
        cy.logout();
        cy.visit('/administrator')
        cy.get('.community-desc-header').should('have.text', 'All Dragons')
        cy.get('.community-desc-footer').should('have.text', 'All Dragons')

        cy.fixture('vars.json').then((vars) => {
            cy.get('[id="username"]').type(vars.defaultUser)
            cy.get('[id="inputPassword"]').type(vars.defaultPass)
        });
        cy.get('[data-cy="login"]').click()

        cy.get('.community-desc-header').should('have.text', 'Divine Dragon')
        cy.get('.community-desc-footer').should('have.text', 'Divine Dragon')
    });

    it('show community indicator on projects page', () => {
        cy.visit('/administrator')
        cy.get('[data-cy="projectCard_proj1"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
        cy.get('[data-cy="projectCard_proj2"] [data-cy="userCommunity"]').contains('For All Dragons Nation')
        cy.get('[data-cy="inception-button"]').contains('Level 0')

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(allDragonsUser, vars.defaultPass);
        });
        cy.visit('/administrator')
        cy.get('[data-cy="projectCard_proj1"]').should('not.exist')
        cy.get('[data-cy="projectCard_proj2"]')
        cy.get('[data-cy="projectCard_proj2"] [data-cy="userCommunity"]').should('not.exist')
    });

    it('show community indicator on the project page', () => {
        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="pageHeaderStat"] [data-cy="pageHeaderStat_For"]')
    });
});
