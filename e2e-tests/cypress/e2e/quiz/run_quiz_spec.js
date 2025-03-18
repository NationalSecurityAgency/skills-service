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

});


