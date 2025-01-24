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

describe('Community Quiz Access Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    beforeEach(() => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });
    });

    it('community protected quiz cannot assign non community member admin access', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})

        cy.intercept('POST', `/admin/quiz-definitions/quiz1/users/${allDragonsUser.toLowerCase()}/roles/ROLE_QUIZ_ADMIN`)
        // cy.intercept('POST', '/admin/quiz-definitions/quiz1/users/alldragons@email.org/roles/ROLE_QUIZ_ADMIN')
          .as('addAdminAttempt');

        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/summary').as('loadQuizSummary');

        cy.visit('/administrator/quizzes/quiz1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadQuizSummary');

        cy.get('[data-cy="existingUserInput"]').type('all');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains(allDragonsUser).click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addAdminAttempt').its('response.statusCode').should('eq', 400);
        cy.get('[data-cy=error-msg]')
          .contains(`Error! Request could not be completed! User [${allDragonsUser}] is not allowed to be assigned [Admin] user role`);
    });

    it('Non community protected quiz can assign non community member admin access', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: false})

        cy.intercept('POST', `/admin/quiz-definitions/quiz1/users/${allDragonsUser.toLowerCase()}/roles/ROLE_QUIZ_ADMIN`)
            .as('addAdminAttempt');

        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/summary').as('loadQuizSummary');

        cy.visit('/administrator/quizzes/quiz1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadQuizSummary');

        cy.get('[data-cy="existingUserInput"]').type('all');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains(allDragonsUser).click();
        cy.get(`[data-cy="userCell_${allDragonsUser}"]`).should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addAdminAttempt')

        cy.get(`[data-cy="userCell_${allDragonsUser}"]`).should('not.exist')
        cy.get('[data-cy="existingUserInput"]').type('all');
        cy.wait('@suggest');
        cy.wait(500);
        cy.contains('No results found')
    });
});
