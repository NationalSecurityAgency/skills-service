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

describe('Community Quiz Description Validation Tests', () => {

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

    it('quiz description is validated against custom validators', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})

        cy.intercept('GET', '/admin/quiz-definitions/quiz1/questions').as('loadQuestions');
        cy.intercept('GET', '/admin/quiz-definitions/quiz1').as('loadQuizDef');
        cy.intercept('POST', '/api/validation/description*').as('validateDescription');

        cy.visit('/administrator/quizzes/quiz1');
        cy.wait('@loadQuestions');

        cy.get('[data-cy="editQuizButton"]').click()
        cy.wait('@loadQuizDef');

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.wait('@validateDescription');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.wait('@validateDescription');
        cy.get('[data-cy="descriptionError"]').contains('Quiz/Survey Description - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.wait('@validateDescription');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('question text is validated against custom validators', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})

        cy.intercept('GET', '/admin/quiz-definitions/quiz1/questions').as('loadQuestions');

        cy.visit('/administrator/quizzes/quiz1');
        cy.wait('@loadQuestions');

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="descriptionError"]').contains('Question - May not contain divinedragon word');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('Input Text answer is validated against custom validators', () => {
        cy.createQuizDef(1, {enableProtectedUserCommunity: true})
        cy.createTextInputQuestionDef(1, 1)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="subPageHeader"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('{selectall}{backspace}')
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\ndivinedragon');
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - May not contain divinedragon word');

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.wait(1000)
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - May not contain divinedragon word');

        cy.get('[data-cy="markdownEditorInput"]').type('{backspace}');
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled');
    });

});
