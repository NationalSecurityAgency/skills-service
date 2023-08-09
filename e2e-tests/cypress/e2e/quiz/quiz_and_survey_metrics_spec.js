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

describe('Quiz and Survey Metrics', () => {

    it('no quiz runs - no metrics', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createSurveyDef(2);
        cy.createSurveyMultipleChoiceQuestionDef(2, 1);

        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="noMetricsYet"]').contains('Results will be available once at least 1 Quiz is completed')

        cy.visit('/administrator/quizzes/quiz2/results');
        cy.get('[data-cy="noMetricsYet"]').contains('Results will be available once at least 1 Survey is completed')
    });


    it('quiz metrics summary cards', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);

        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '1')
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardDescription"]').contains('1 attempt by 1 user')

        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardValue"]').should('have.text', '1')
        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardDescription"]').contains('1 attempt passed by 1 user')

        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardValue"]').should('have.text', '0')
        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardDescription"]').contains('0 attempts failed by 0 users')

        cy.get('[data-cy="metricsCardRuntime"] [data-cy="statCardDescription"]').contains('Average Quiz runtime for 1 attempt')
        cy.get('[data-cy="noMetricsYet"]').should('not.exist')

        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}, {selectedIndex: [1,2]}]);
        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '2')
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardDescription"]').contains('2 attempts by 2 users')

        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardValue"]').should('have.text', '1')
        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardDescription"]').contains('1 attempt passed by 1 user')

        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardValue"]').should('have.text', '1')
        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardDescription"]').contains('1 attempt failed by 1 user')

        cy.get('[data-cy="metricsCardRuntime"] [data-cy="statCardDescription"]').contains('Average Quiz runtime for 2 attempts')

        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 3, [{selectedIndex: [0]}, {selectedIndex: [1,2]}]);

        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '4')
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardDescription"]').contains('4 attempts by 3 users')

        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardValue"]').should('have.text', '2')
        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardDescription"]').contains('2 attempts passed by 2 users')

        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardValue"]').should('have.text', '2')
        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardDescription"]').contains('2 attempts failed by 2 users')

        cy.get('[data-cy="metricsCardRuntime"] [data-cy="statCardDescription"]').contains('Average Quiz runtime for 4 attempts')
    });

    it('survey metrics summary cards', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.runQuizForUser(1, 1, [{selectedIndex: [0, 2]}], true);

        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '1')
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardDescription"]').contains('Survey was completed 1 time')

        cy.get('[data-cy="metricsCardPassed"]').should('not.exist')
        cy.get('[data-cy="metricsCardFailed"]').should('not.exist')

        cy.get('[data-cy="metricsCardRuntime"] [data-cy="statCardDescription"]').contains('Average Survey runtime for 1 user')

        cy.runQuizForUser(1, 2, [{selectedIndex: [0, 2]}], true);
        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '2')
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardDescription"]').contains('Survey was completed 2 times')

        cy.get('[data-cy="metricsCardPassed"]').should('not.exist')
        cy.get('[data-cy="metricsCardFailed"]').should('not.exist')

        cy.get('[data-cy="metricsCardRuntime"] [data-cy="statCardDescription"]').contains('Average Survey runtime for 2 users')
    });

    it('quiz metrics does not produce NaN results', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.createQuizMultipleChoiceQuestionDef(1, 3);

        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metrics-q3"] [data-cy="row0-colNumAnswered"] [data-cy="percent"]').contains('0%')
        cy.get('[data-cy="metrics-q3"] [data-cy="row1-colNumAnswered"] [data-cy="percent"]').contains('0%')
        cy.get('[data-cy="metrics-q3"] [data-cy="row2-colNumAnswered"] [data-cy="percent"]').contains('0%')
        cy.get('[data-cy="metrics-q3"] [data-cy="row3-colNumAnswered"] [data-cy="percent"]').contains('0%')

    });
});
