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

describe('Quiz User Role Management Tests', () => {

    const tableSelector = '[data-cy="roleManagerTable"]';

    it('add quiz admin', function () {
        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');

        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.visit('/administrator/quizzes/quiz1/access');

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains('root@skills.org').click();
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click()
        cy.get('[data-cy="userCell_root@skills.org"]')

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.contains('No results found')
    });

    it('delete quiz admin', function () {
      cy.fixture('vars.json')
        .then((vars) => {
          cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');

          const pass = 'password';
          cy.register('user1', pass);
          cy.register('user2', pass);
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
          cy.createQuizDef(1);
          cy.createQuizQuestionDef(1, 1);

          const oauthMode = Cypress.env('oauthMode');
          const defaultUser = oauthMode ? Cypress.env('proxyUser') : vars.defaultUser;

          cy.request('POST', `/admin/quiz-definitions/quiz1/users/user1/roles/ROLE_QUIZ_ADMIN`);
          cy.request('POST', `/admin/quiz-definitions/quiz1/users/user2/roles/ROLE_QUIZ_ADMIN`);
          cy.visit('/administrator/quizzes/quiz1/access');

          cy.get(`[data-cy="controlsCell_${defaultUser}"] [data-cy="removeUserBtn"]`).should('not.exist')
          cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('be.enabled')
          cy.get('[data-cy="controlsCell_user2"] [data-cy="removeUserBtn"]').should('be.enabled')

          cy.openDialog('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]')

          cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove user1 from having admin privileges.')
          cy.get('[data-cy="currentValidationText"]').type('Delete Me')
            cy.clickSaveDialogBtn()

          cy.get(`[data-cy="controlsCell_${defaultUser}"] [data-cy="removeUserBtn"]`).should('exist')
          cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('not.exist')
          cy.get('[data-cy="controlsCell_user2"] [data-cy="removeUserBtn"]').should('be.enabled')

          cy.openDialog('[data-cy="controlsCell_user2"] [data-cy="removeUserBtn"]')

          cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove user2 from having admin privileges.')
          cy.get('[data-cy="currentValidationText"]').type('Delete Me')
          cy.clickSaveDialogBtn()

          cy.get(`[data-cy="controlsCell_${defaultUser}"] [data-cy="removeUserBtn"]`).should('not.exist')

          cy.get('[data-cy="existingUserInput"] [data-cy="existingUserInputDropdown"] [data-pc-name="pcinputtext"]').should('have.focus')
        });
    })

    it('cancelling delete safety check returns focus to the delete button', function () {
        const pass = 'password';
        cy.register('user1', pass);
        cy.register('user2', pass);
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
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.visit('/administrator/quizzes/quiz1/access');
        cy.request('POST', `/admin/quiz-definitions/quiz1/users/user1/roles/ROLE_QUIZ_ADMIN`);
        cy.request('POST', `/admin/quiz-definitions/quiz1/users/user2/roles/ROLE_QUIZ_ADMIN`);

        cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').click()
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('have.focus')

        cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').click()
        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('[data-cy="controlsCell_user1"] [data-cy="removeUserBtn"]').should('have.focus')
    })

    it('paging users', function () {
        cy.fixture('vars.json')
            .then((vars) => {
                const pass = 'password';
                cy.register('0user', pass);
                cy.register('1user', pass);
                cy.register('2user', pass);
                cy.register('3user', pass);
                cy.register('4user', pass);
                cy.register('5user', pass);

                const oauthMode = Cypress.env('oauthMode');
                const defaultUserForDisplay = oauthMode ? 'foo' : 'Firstname LastName (skills@skills.org)';
                if (!oauthMode) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }

                cy.createQuizDef(1);
                cy.createQuizQuestionDef(1, 1);

                cy.request('POST', `/admin/quiz-definitions/quiz1/users/0user/roles/ROLE_QUIZ_ADMIN`);
                cy.request('POST', `/admin/quiz-definitions/quiz1/users/1user/roles/ROLE_QUIZ_ADMIN`);
                cy.request('POST', `/admin/quiz-definitions/quiz1/users/2user/roles/ROLE_QUIZ_ADMIN`);
                cy.request('POST', `/admin/quiz-definitions/quiz1/users/3user/roles/ROLE_QUIZ_ADMIN`);
                cy.request('POST', `/admin/quiz-definitions/quiz1/users/4user/roles/ROLE_QUIZ_ADMIN`);
                cy.request('POST', `/admin/quiz-definitions/quiz1/users/5user/roles/ROLE_QUIZ_ADMIN`);

                cy.visit('/administrator/quizzes/quiz1/access');

                const headerSelector = `${tableSelector} thead tr th`;
                cy.get(headerSelector)
                    .contains('Quiz Admin')
                    .click();

                cy.validateTable(tableSelector, [
                    [{ colIndex: 1, value: defaultUserForDisplay }],
                    [{ colIndex: 1, value: 'Firstname LastName (5user)' }],
                    [{ colIndex: 1, value: 'Firstname LastName (4user)' }],
                    [{ colIndex: 1, value: 'Firstname LastName (3user)' }],
                    [{ colIndex: 1, value: 'Firstname LastName (2user)' }],
                    [{ colIndex: 1, value: 'Firstname LastName (1user)' }],
                    [{ colIndex: 1, value: 'Firstname LastName (0user)' }],
                ], 5);
            });
    })

    it('user does not have any admin groups to assign to quiz', function () {
        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');

        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.visit('/administrator/quizzes/quiz1/access');


        cy.get('[data-cy="adminGroupSelector"]').click()
        cy.get('[data-pc-section="listcontainer"] [data-pc-section="emptymessage"]').contains('You currently do not administer any admin groups.').should('be.visible')
    });

    it('add admin group to quiz', function () {
        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');
        cy.intercept('POST', ' /admin/admin-group-definitions/adminGroup1/quizzes/quiz1')
            .as('addAdminGroupToQuiz');
        cy.intercept('GET', '/app/admin-group-definitions')
            .as('loadCurrentUsersAdminGroups');
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/adminGroups')
            .as('loadAdminGroupsForQuiz');

        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });
        cy.intercept('GET', '/app/userInfo')
            .as('loadUserInfo');

        cy.visit('/administrator/quizzes/quiz1/access');
        cy.wait('@loadAdminGroupsForQuiz');
        cy.wait('@loadCurrentUsersAdminGroups');


        const expectedUserName = Cypress.env('oauthMode') ? 'foo' : 'skills@';
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: expectedUserName }],
        ], 5, true, null, false);

        cy.get('[data-cy="adminGroupSelector"]').click()
        cy.get('[data-cy="availableAdminGroupSelection-adminGroup1"]').click()

        cy.wait('@addAdminGroupToQuiz');
        cy.wait('@loadAdminGroupsForQuiz');
        cy.wait('@loadCurrentUsersAdminGroups');
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'My Awesome Admin Group' }],
        ], 5, true, null, false);

        cy.get(`${tableSelector} [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get('[data-cy="userGroupMembers"]').find('li').should('have.length', 1);
        cy.get(`[data-cy^="userGroupMember_${expectedUserName}"]`)
    });

});
