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

describe('Admin Group Quiz Management Tests', () => {

    const adminGroupQuizzesTableSelector = '[data-cy="adminGroupQuizzesTable"]';
    
    beforeEach( () => {

        cy.intercept('GET', '/admin/admin-group-definitions/adminGroup1/quizzes')
            .as('loadGroupQuizzes');
        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');
    })

    it('user has no quizzes to assign to admin group', function () {

        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

        cy.visit('/administrator/adminGroups/adminGroup1/group-quizzes');
        cy.wait('@loadGroupQuizzes');

        cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('li.p-dropdown-empty-message').contains('You currently do not administer any quizzes or surveys.').should('be.visible')
    });

    it('admin groups quiz page, add quiz to group', function () {
        cy.createQuizDef(1);
        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

        cy.visit('/administrator/adminGroups/adminGroup1/group-quizzes');
        cy.wait('@loadGroupQuizzes');
        cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="noContent"]')
        cy.get('span.p-dropdown-label.p-inputtext').contains('Search available quizzes and surveys...').should('be.visible')
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('[data-cy="availableQuizSelection-quiz1"]').click()

        cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '1');
        cy.validateTable(adminGroupQuizzesTableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }],
        ], 5);
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('li.p-dropdown-empty-message').contains('All of your available quizzes and surveys have already been assigned to this admin group.').should('be.visible')
    });

    it('admin groups quiz page, remove quiz from group', function () {
        const userIdForDisplay = 'user id for display'
        cy.intercept('GET', '/app/userInfo', (req) => {
            req.continue((res) => {
                res.body.userIdForDisplay = userIdForDisplay
            })
        }).as('getUserInfo1');
        cy.fixture('vars.json')
            .then((vars) => {
                const oauthMode = Cypress.env('oauthMode');
                const defaultUser = oauthMode ? Cypress.env('proxyUser') : vars.defaultUser;
                cy.createQuizDef(1);
                cy.createQuizDef(2);
                cy.createAdminGroupDef(1, {name: 'My Awesome Admin Group'});
                cy.addQuizToAdminGroupDef(1, 1)
                cy.addQuizToAdminGroupDef(1, 2)

                cy.visit('/administrator/adminGroups/adminGroup1/group-quizzes');
                cy.wait('@loadGroupQuizzes');
                cy.get('[data-cy="noContent"]').should('not.exist')

                cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '2');
                cy.get(adminGroupQuizzesTableSelector)
                cy.validateTable(adminGroupQuizzesTableSelector, [
                    [{colIndex: 0, value: 'This is quiz 2'}],
                    [{colIndex: 0, value: 'This is quiz 1'}]
                ], 5);
                cy.get('[data-cy="removeQuiz_quiz2"]').click()
                cy.get('[data-cy="removalSafetyCheckMsg"]').contains(`This will remove the This is quiz 2 quiz from this admin group. All members of this admin group other than ${userIdForDisplay} will lose admin access to this quiz.`)
                cy.get('[data-cy="currentValidationText"]').type('Delete Me')
                cy.get('[data-cy="saveDialogBtn"]').click()

                cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '1');
                cy.validateTable(adminGroupQuizzesTableSelector, [
                    [{colIndex: 0, value: 'This is quiz 1'}]
                ], 5);

                cy.get('[data-cy="removeQuiz_quiz1"]').click()
                cy.get('[data-cy="removalSafetyCheckMsg"]').contains(`This will remove the This is quiz 1 quiz from this admin group. All members of this admin group other than ${userIdForDisplay} will lose admin access to this quiz.`)
                cy.get('[data-cy="closeDialogBtn"]').click()
                cy.get('[data-cy="removeQuiz_quiz1"]').should('have.focus')
            });
    });

    it('paging quizzes', function () {
        cy.createQuizDef(1);
        cy.createQuizDef(2);
        cy.createQuizDef(3);
        cy.createQuizDef(4);
        cy.createQuizDef(5);
        cy.createQuizDef(6);
        cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });
        cy.addQuizToAdminGroupDef(1, 1)
        cy.addQuizToAdminGroupDef(1, 2)
        cy.addQuizToAdminGroupDef(1, 3)
        cy.addQuizToAdminGroupDef(1, 4)
        cy.addQuizToAdminGroupDef(1, 5)
        cy.addQuizToAdminGroupDef(1, 6)

        cy.visit('/administrator/adminGroups/adminGroup1/group-quizzes');
        cy.wait('@loadGroupQuizzes');
        cy.get('[data-cy="noContent"]').should('not.exist')

        cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '6');

        cy.get(adminGroupQuizzesTableSelector)
        cy.validateTable(adminGroupQuizzesTableSelector, [
            [{ colIndex: 0, value: 'This is quiz 6' }],
            [{ colIndex: 0, value: 'This is quiz 5' }],
            [{ colIndex: 0, value: 'This is quiz 4' }],
            [{ colIndex: 0, value: 'This is quiz 3' }],
            [{ colIndex: 0, value: 'This is quiz 2' }],
            [{ colIndex: 0, value: 'This is quiz 1' }],
        ])
    })

});
