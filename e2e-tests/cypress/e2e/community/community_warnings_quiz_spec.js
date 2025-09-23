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
import './community_warnings_help_commands.js'

describe('Quiz - Community Attachment Warning Tests', () => {

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

        const fileMsg = 'Friendly Reminder: Only safe files please for {{community.project.descriptor}}'
        const descMsg = 'Friendly Reminder: Only safe descriptions for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.attachmentWarningMessage = fileMsg;
                conf.descriptionWarningMessage = descMsg;
                res.send(conf);
            });
        }).as('loadConfig');

        cy.viewport(1400, 1000)
        cy.createQuizDef(1)

        cy.createQuizDef(2, {enableProtectedUserCommunity: true});
    });

    it('create quiz', () => {
        cy.visit('/administrator/quizzes/')
        cy.wait('@loadConfig')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Quiz/Survey')
        cy.validateAllDragonsWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateAllDragonsWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonWarning(false)
    })

    it('edit quiz', () => {
        cy.visit('/administrator/quizzes/')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editQuizButton_quiz1"]', 'Editing Existing Quiz/Survey')
        cy.validateAllDragonsWarning()

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.openDescModalAndAttachFile('[data-cy="editQuizButton_quiz2"]', 'Editing Existing Quiz/Survey')
        cy.validateDivineDragonWarning()
    })

    it('edit quiz - from quiz page', () => {
        cy.visit('/administrator/quizzes/quiz1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editQuizButton"]', 'Editing Existing Quiz/Survey')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/quizzes/quiz2')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editQuizButton"]', 'Editing Existing Quiz/Survey')
        cy.validateDivineDragonWarning()
    })

    it('new question', () => {
        cy.visit('/administrator/quizzes/quiz1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_Questions"]', 'New Question')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/quizzes/quiz2')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_Questions"]', 'New Question')
        cy.validateDivineDragonWarning()
    })

    it('edit question', () => {
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(2, 1)
        cy.visit('/administrator/quizzes/quiz1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editQuestionButton_1"]', 'Editing Existing Question')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/quizzes/quiz2')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editQuestionButton_1"]', 'Editing Existing Question')
        cy.validateDivineDragonWarning()
    })

    it('text answers', () => {
        cy.createTextInputQuestionDef(1, 1)
        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="descriptionWarningMessage"]').contains('Friendly Reminder: Only safe descriptions for All Dragons')

        cy.createTextInputQuestionDef(2, 1)
        cy.visit('/progress-and-rankings/quizzes/quiz2')
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="descriptionWarningMessage"]').contains('Friendly Reminder: Only safe descriptions for Divine Dragon')
    })

    it('questions grading input', () => {
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(2, 1)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}], true, '**My Answer**')
        cy.runQuizForUser(2, 1, [{selectedIndex: [0]}], true, '**My Answer**')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="feedbackTxtMarkdownEditor"] [data-cy="descriptionWarningMessage"]').contains('Friendly Reminder: Only safe descriptions for All Dragons')

        cy.visit('/administrator/quizzes/quiz2/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="feedbackTxtMarkdownEditor"] [data-cy="descriptionWarningMessage"]').contains('Friendly Reminder: Only safe descriptions for Divine Dragon')
    })

});