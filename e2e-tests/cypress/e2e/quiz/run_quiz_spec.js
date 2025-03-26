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
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Run Quiz Tests', () => {


    it('quiz run in dashboard display completion messages', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizCompletion"]').contains('Thank you for completing the Quiz')
    });

    it('Tests "no more attempts" message', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMaxNumAttempts(1, 2)
        cy.setQuizMultipleTakes(1, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizFailed"]')

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizFailed"]')

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="noMoreAttemptsAlert"]').contains('This quiz allows 2 maximum attempts')
    });

    it('Tests "no more attempts" message after passing and then failing a retakeable quiz.', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMaxNumAttempts(1, 2)
        cy.setQuizMultipleTakes(1, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizCompletion"]').contains('Thank you for completing the Quiz')

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizFailed"]')

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="noMoreAttemptsAlert"]').contains('This quiz allows 2 maximum attempts')
    });

    it('Multiple answer question with wrong answer selected then unselected', () => {
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*').as('reportAnswer')
        cy.createQuizDef(1);
        cy.createQuizMultipleChoiceQuestionDef(1, 1);

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.wait('@reportAnswer')
        // unselect answer
        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.wait('@reportAnswer')

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_3"]').click()
        cy.wait('@reportAnswer')
        cy.wait('@reportAnswer')

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizCompletion"]').contains('Thank you for completing the Quiz')
        cy.get('[data-cy="quizPassed"]')

        cy.get('[data-cy="question_1"] [data-cy="answer_1"] [data-cy="selected_true"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_2"] [data-cy="selected_false"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_3"] [data-cy="selected_true"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_4"] [data-cy="selected_false"]')
    });

    it('Multiple answer question with correct answer selected then unselected', () => {
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*').as('reportAnswer')
        cy.createQuizDef(1);
        cy.setQuizShowCorrectAnswers(1, true)
        cy.createQuizMultipleChoiceQuestionDef(1, 1, {
            answers: [{
                answer: 'First Answer',
                isCorrect: true,
            }, {
                answer: 'Second Answer',
                isCorrect: false,
            }, {
                answer: 'Third Answer',
                isCorrect: true,
            }, {
                answer: 'Fourth Answer',
                isCorrect: false,
            }, {
                answer: 'Fifth Answer',
                isCorrect: true,
            }]
        });

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.wait('@reportAnswer')
        // unselect answer
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.wait('@reportAnswer')

        cy.get('[data-cy="question_1"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_5"]').click()
        cy.wait('@reportAnswer')
        cy.wait('@reportAnswer')

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizFailed"]')

        cy.get('[data-cy="question_1"] [data-cy="answer_1"] [data-cy="selected_false"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_1"] [data-cy="missedSelection"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_2"] [data-cy="selected_false"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_3"] [data-cy="selected_true"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_4"] [data-cy="selected_false"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_5"] [data-cy="selected_true"]')
    });

});


