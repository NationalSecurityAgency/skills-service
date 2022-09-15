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

describe('Projects Admin Management Tests', () => {
    beforeEach(() => {
        cy.intercept('GET', '/app/projects')
            .as('getProjects');
        cy.intercept('GET', '/api/icons/customIconCss')
            .as('getProjectsCustomIcons');
        cy.intercept('GET', '/app/userInfo')
            .as('getUserInfo');
        cy.intercept('/admin/projects/proj1/users/root@skills.org/roles*')
            .as('getRolesForRoot');
    });

    it('Add Admin - User Not Found', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept({
            method: 'PUT',
            path: '/admin/projects/proj1/users/bar/roles/ROLE_PROJECT_ADMIN',
        }, {
            statusCode: 400,
            body: {
                errorCode: 'UserNotFound',
                explanation: 'User was not found'
            }
        })
            .as('addAdmin');

        cy.intercept({
            method: 'POST',
            path: '/app/users/suggest*',
        }, {
            statusCode: 200,
            body: [{
                userId: 'bar',
                userIdForDisplay: 'bar',
                first: 'bar',
                last: 'bar',
                dn: 'bar'
            }]
        })
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .click()
            .type('bar');
        cy.wait('@suggest');
        //cy.pause();
        //cy.get('.multiselect__input').type('{enter}');
        // cy.get('.multiselect__option multiselect__option--highlight').click();
        cy.get('[data-cy="existingUserInput"] .vs__dropdown-option')
            .eq(0)
            .click({ force: true });
        cy.clickButton('Add');
        cy.wait('@addAdmin');
        cy.get('.alert-danger')
            .contains('User was not found');
    });

    it('Add Admin - InternalError', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept({
            method: 'PUT',
            path: '/admin/projects/proj1/users/bar/roles/ROLE_PROJECT_ADMIN',
        }, {
            statusCode: 400,
            body: {
                errorCode: 'InternalError',
                explanation: 'Some Error Occurred'
            }
        })
            .as('addAdmin');

        cy.intercept({
            method: 'POST',
            path: '/app/users/suggest*',
        }, {
            statusCode: 200,
            body: [{
                userId: 'bar',
                userIdForDisplay: 'bar',
                first: 'bar',
                last: 'bar',
                dn: 'bar'
            }]
        })
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .click()
            .type('bar{enter}');
        cy.wait('@suggest');
        cy.get('[data-cy="existingUserInput"]')
            .click()
            .type('{enter}');
        cy.clickButton('Add');
        cy.wait('@addAdmin');
        cy.get('[data-cy="errorPage"]')
            .contains('Tiny-bit of an error!');
    });

    it('Add Admin No Query', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .type('{enter}');
        cy.wait('@suggest');
        cy.contains('root@skills.org')
            .click();
        cy.clickButton('Add');
        cy.wait('@addAdmin');

        const rowSelector = '[data-cy=roleManagerTable] tbody tr';
        cy.get(rowSelector)
            .should('have.length', 2)
            .as('cyRows');
        if (!Cypress.env('oauthMode')) {
            cy.get('@cyRows')
                .eq(0)
                .find('td')
                .as('row1');
            cy.get('@cyRows')
                .eq(1)
                .find('td')
                .as('row2');
            cy.get('@row1')
                .eq(0)
                .contains('root@skills.org');
            cy.get('@row2')
                .eq(0)
                .contains('skills@skills.org');
        } else {
            // the default user in oauth mode is different and results in a different sorting order
            cy.get('@cyRows')
                .eq(0)
                .find('td')
                .as('row1');
            cy.get('@cyRows')
                .eq(1)
                .find('td')
                .as('row2');
            cy.get('@row1')
                .eq(0)
                .contains('foo bar');
            cy.get('@row2')
                .eq(0)
                .contains('root@skills.org');
        }
    });

    it('Add and Remove Admin', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .type('root');
        cy.wait('@suggest');
        cy.contains('root@skills.org')
            .click();
        cy.clickButton('Add');
        cy.wait('@addAdmin');

        const tableSelector = '[data-cy=roleManagerTable]';
        const rowSelector = `${tableSelector} tbody tr`;

        cy.get(`${tableSelector} [data-cy="userCell_root@skills.org"]`);
        cy.get(rowSelector)
            .should('have.length', 2)
            .as('cyRows');

        cy.get(`${tableSelector} [data-cy="userCell_root@skills.org"] [data-cy="removeUserBtn"]`)
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.get(`${tableSelector} [data-cy="userCell_root@skills.org"]`).should('not.exist')
        cy.get(rowSelector)
            .should('have.length', 1)
            .as('cyRows1');
    });

    it('Add Admin - forward slash character does not cause error', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN')
            .as('addAdmin');

        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1')
            .as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]')
            .click()
            .type('root/bar{enter}');
        cy.wait('@suggest');
    });

})