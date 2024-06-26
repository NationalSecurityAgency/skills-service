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
            cy.request('POST', '/logout');
            cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
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
            cy.request('POST', `/root/users/${Cypress.env('proxyUser')}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.loginAsAdminUser();
            cy.createProject(1, {enableProtectedUserCommunity: true})
            cy.createProject(2)

            cy.request('POST', `/admin/projects/proj2/users/${allDragonsUser}/roles/ROLE_PROJECT_ADMIN`);
        });
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    it('dynamically populate "community.descriptor" prop for the header token substitution', () => {
        cy.visit('/administrator')
        cy.get('[data-cy="projCard_proj1_manageLink"]')
        cy.get('[data-cy="inception-button"]').contains('Level 0')
        cy.get('.community-desc-header').should('have.text', 'Divine Dragon')
        cy.get('.community-desc-footer').should('have.text', 'Divine Dragon')

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(allDragonsUser, vars.defaultPass);
        });

        cy.visit('/administrator')
        cy.get('[data-cy="projCard_proj2_manageLink"]')
        cy.get('[data-cy="inception-button"]').contains('Level 0')
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

    it('show community indicator on the project page - via direct page loading', () => {
        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.visit('/administrator/projects/proj2')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For All Dragons Nation')
    });

    it('show community indicator using defaults on the project page - via direct page loading', () => {

        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.userCommunityBeforeLabel = null
                conf.userCommunityAfterLabel = null
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visit('/administrator/projects/proj1')
        cy.wait('@loadConfig');
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('Divine Dragon')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For').should('not.exist')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('Nation').should('not.exist')

        cy.visit('/administrator/projects/proj2')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('All Dragons')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For').should('not.exist')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('Nation').should('not.exist')
    });

    it('show community indicator on the project page - via navigation', () => {
        cy.visit('/administrator')
        cy.get('[data-cy="projectCard_proj1"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
        cy.get('[data-cy="projectCard_proj2"] [data-cy="userCommunity"]').contains('For All Dragons Nation')

        cy.get('[data-cy="projCard_proj1_manageBtn"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="breadcrumb-Projects"]').click()
        cy.get('[data-cy="projectCard_proj1"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
        cy.get('[data-cy="projectCard_proj2"] [data-cy="userCommunity"]').contains('For All Dragons Nation')

        cy.get('[data-cy="projCard_proj2_manageBtn"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For All Dragons Nation')
    });



});
