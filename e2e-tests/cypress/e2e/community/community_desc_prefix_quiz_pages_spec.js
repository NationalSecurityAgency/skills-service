/*
 * Copyright 2025 SkillTree
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

import './community-commands'

describe('Community and Desc Prefix Quiz Pages Tests', () => {

    const allDragonsUser = 'allDragons@email.org'
    beforeEach( () => {
        const descMsg = 'Friendly Reminder: Only safe descriptions for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.addPrefixToInvalidParagraphsOptions = 'All Dragons:(A) ,(B) |Divine Dragon:(A) ,(B) ,(C) ,(D) ';
                conf.descriptionWarningMessage = descMsg;
                res.send(conf);
            });
        }).as('getConfig');

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

        cy.viewport(1400, 1000)
        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        cy.createQuizDef(2, {enableProtectedUserCommunity: true});
        cy.createQuizQuestionDef(2, 1)
        cy.createTextInputQuestionDef(2, 1)
        cy.createTextInputQuestionDef(2, 2)
        cy.runQuizForUser(2, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.runQuizForUser(2, 2, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
    });

    it('questions - switching between quizzes', () => {
        cy.visit('/administrator/quizzes/quiz1')
        cy.wait('@getConfig')
        cy.validateAllDragonPrefixOps('[data-cy="btn_Questions"]')
        cy.validateAllDragonPrefixOps('[data-cy="editQuestionButton_1"]', '', true)

        cy.get('[data-cy="breadcrumb-Quizzes"]').click()
        cy.get('[data-cy="managesQuizBtn_quiz2"]').click()
        cy.validateDivineDragonPrefixOps('[data-cy="btn_Questions"]')
        cy.validateDivineDragonPrefixOps('[data-cy="editQuestionButton_1"]', '', true)
    })

    it('grading - all dragons', () => {
        cy.visit('/administrator/quizzes/quiz1/grading')
        cy.wait('@getConfig')

        cy.get('[data-cy="navLine-Grading"]').click()
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled')
        cy.get('[data-cy="gradeBtn_user1"]').click()
        cy.validateAllDragonPrefixOps(null, '[data-cy="gradeAttemptFor_user1"]')
        cy.get('[data-cy="gradeBtn_user2"]').click()
        cy.validateAllDragonPrefixOps(null, '[data-cy="gradeAttemptFor_user2"]')
    })

    it('grading - divine dragon', () => {
        cy.visit('/administrator/quizzes/quiz2/grading')
        cy.wait('@getConfig')

        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled')
        cy.get('[data-cy="gradeBtn_user1"]').click()
        cy.validateDivineDragonPrefixOps(null, '[data-cy="gradeAttemptFor_user1"]')
        cy.get('[data-cy="gradeBtn_user2"]').click()
        cy.validateDivineDragonPrefixOps(null, '[data-cy="gradeAttemptFor_user2"]')
    })

    it('questions - all dragons - as all dragons user', () => {
        cy.request('POST', `/admin/quiz-definitions/quiz1/users/${allDragonsUser}/roles/ROLE_QUIZ_ADMIN`);
        cy.logout()
        cy.login(allDragonsUser)
        cy.visit('/administrator/quizzes/quiz1')
        cy.wait('@getConfig')
        cy.validateAllDragonPrefixOps('[data-cy="btn_Questions"]')
        cy.validateAllDragonPrefixOps('[data-cy="editQuestionButton_1"]', '', true, false)

    })

    it('grading - all dragons - as all dragons user', () => {
        cy.request('POST', `/admin/quiz-definitions/quiz1/users/${allDragonsUser}/roles/ROLE_QUIZ_ADMIN`);
        cy.logout()
        cy.login(allDragonsUser)
        cy.visit('/administrator/quizzes/quiz1/grading')
        cy.wait('@getConfig')
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled')
        cy.get('[data-cy="gradeBtn_user1"]').click()
        cy.validateAllDragonPrefixOps(null, '[data-cy="gradeAttemptFor_user1"]')
        cy.get('[data-cy="gradeBtn_user2"]').click()
        cy.validateAllDragonPrefixOps(null, '[data-cy="gradeAttemptFor_user2"]')
    })

});

