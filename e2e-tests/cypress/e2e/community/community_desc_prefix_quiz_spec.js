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

describe('Community and Desc Prefix Quiz Tests', () => {

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

    it('load proper options when switching between quizzes', () => {
        cy.visit('/administrator/quizzes/quiz1')
        cy.wait('@getConfig')
        cy.validateAllDragonOptions('[data-cy="btn_Questions"]')
        cy.validateAllDragonOptions('[data-cy="editQuestionButton_1"]')

        cy.get('[data-cy="navLine-Grading"]').click()
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled')
        cy.get('[data-cy="gradeBtn_user1"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="gradeAttemptFor_user1"]')
        cy.get('[data-cy="gradeBtn_user2"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="gradeAttemptFor_user2"]')

        cy.get('[data-cy="breadcrumb-Quizzes"]').click()
        cy.get('[data-cy="managesQuizBtn_quiz2"]').click()
        cy.validateDivineDragonOptions('[data-cy="btn_Questions"]')
        cy.validateDivineDragonOptions('[data-cy="editQuestionButton_1"]')

        cy.get('[data-cy="navLine-Grading"]').click()
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled')
        cy.get('[data-cy="gradeBtn_user1"]').click()
        cy.validateDivineDragonOptions(null, '[data-cy="gradeAttemptFor_user1"]')
        cy.get('[data-cy="gradeBtn_user2"]').click()
        cy.validateDivineDragonOptions(null, '[data-cy="gradeAttemptFor_user2"]')
    })

    it('load proper options when for a quiz as - as all dragons user', () => {
        cy.request('POST', `/admin/quiz-definitions/quiz1/users/${allDragonsUser}/roles/ROLE_QUIZ_ADMIN`);
        cy.logout()
        cy.login(allDragonsUser)
        cy.visit('/administrator/quizzes/quiz1')
        cy.wait('@getConfig')
        cy.validateAllDragonOptions('[data-cy="btn_Questions"]')
        cy.validateAllDragonOptions('[data-cy="editQuestionButton_1"]')

        cy.get('[data-cy="navLine-Grading"]').click()
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled')
        cy.get('[data-cy="gradeBtn_user1"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="gradeAttemptFor_user1"]')
        cy.get('[data-cy="gradeBtn_user2"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="gradeAttemptFor_user2"]')
    })

    it('load proper options when taking a quiz', () => {
        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.wait('@getConfig')
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="question_2"]')
        cy.validateAllDragonOptions(null, '[data-cy="question_3"]')

        cy.visit('/progress-and-rankings/quizzes/quiz2')
        cy.wait('@getConfig')
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.validateDivineDragonOptions(null, '[data-cy="question_2"]')
        cy.validateDivineDragonOptions(null, '[data-cy="question_3"]')
    })

    it('load proper options when taking a quiz - as all dragons user', () => {
        cy.logout()
        cy.login(allDragonsUser)

        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.wait('@getConfig')
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="question_2"]')
        cy.validateAllDragonOptions(null, '[data-cy="question_3"]')
    })

    it('new quiz', () => {
        cy.visit('/administrator/quizzes')
        cy.wait('@getConfig')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Quiz')

        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`).should("not.exist")
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`).should("not.exist")

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`).should("not.exist")
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`).should("not.exist")
    })

    it('edit a quiz - all dragons', () => {
        cy.visit('/administrator/quizzes')
        cy.get('@getConfig')
        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`).should("not.exist")
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`).should("not.exist")

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`)
    })

    it('edit a quiz - all dragons - quiz page', () => {
        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`).should("not.exist")
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`).should("not.exist")

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`)
    })

    it('edit a project - divine dragons', () => {
        cy.visit('/administrator/quizzes')
        cy.get('@getConfig')
        cy.validateDivineDragonOptions('[data-cy="editQuizButton_quiz2"]')
    })

    it('edit a quiz - divine dragons - quiz page', () => {
        cy.visit('/administrator/quizzes/quiz2')
        cy.get('@getConfig')
        cy.validateDivineDragonOptions('[data-cy="editQuizButton"]')
    })
});

