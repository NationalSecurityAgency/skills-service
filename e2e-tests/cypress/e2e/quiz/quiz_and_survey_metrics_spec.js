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

    it('survey metrics with Input Text fields', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0, 2]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'Answer 1');
        cy.runQuizForUser(1, 2, [{selectedIndex: [0, 2]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'Answer 2');
        cy.runQuizForUser(1, 3, [{selectedIndex: [0, 2]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'Answer 3');
        cy.runQuizForUser(1, 4, [{selectedIndex: [0, 2]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'Answer 4');
        cy.runQuizForUser(1, 5, [{selectedIndex: [0, 2]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'Answer 5');
        cy.runQuizForUser(1, 6, [{selectedIndex: [0, 2]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'Answer 6');

        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '6')
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardDescription"]').contains('Survey was completed 6 times')


        const verifyInputTextQuestion = (qNum) => {
            cy.get(`[data-cy="metrics-q${qNum}"]`).contains('Text Input')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '6')
            // page 1
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-pc-section="bodyrow"]`).should('have.length', 5)
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="0"]`).contains('user1')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="0"]`).should('not.contain', 'CORRECT')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="1"]`).contains('user2')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="2"]`).contains('user3')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="3"]`).contains('user4')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="4"]`).contains('user5')

            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row1-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row2-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row3-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row4-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="collapseAll"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="expandAll"]`).click()
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="collapseAll"]`).should('exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="expandAll"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('contain', 'Answer 1')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row1-colAnswerTxt"]`).should('contain', 'Answer 2')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row2-colAnswerTxt"]`).should('contain', 'Answer 3')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row3-colAnswerTxt"]`).should('contain', 'Answer 4')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row4-colAnswerTxt"]`).should('contain', 'Answer 5')
            // page2
            cy.get(`[data-cy="metrics-q${qNum}"] [data-pc-name="pcpaginator"] [aria-label="Page 2"]`).click()
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-pc-section="bodyrow"]`).should('have.length', 1)
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="0"]`).contains('user6')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('contain', 'Answer 6')

            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="expandAll"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="collapseAll"]`).click()
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="collapseAll"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="expandAll"]`).should('exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('not.exist')

            // back to page 1
            cy.get(`[data-cy="metrics-q${qNum}"] [data-pc-name="pcpaginator"] [aria-label="Page 1"]`).click()
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-pc-section="bodyrow"]`).should('have.length', 5)
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="0"]`).contains('user1')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="1"]`).contains('user2')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="2"]`).contains('user3')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="3"]`).contains('user4')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-p-index="4"]`).contains('user5')

            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row1-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row2-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row3-colAnswerTxt"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row4-colAnswerTxt"]`).should('not.exist')

            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="expandAll"]`).click()
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="collapseAll"]`).should('exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="expandAll"]`).should('not.exist')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row0-colAnswerTxt"]`).should('contain', 'Answer 1')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row1-colAnswerTxt"]`).should('contain', 'Answer 2')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row2-colAnswerTxt"]`).should('contain', 'Answer 3')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row3-colAnswerTxt"]`).should('contain', 'Answer 4')
            cy.get(`[data-cy="metrics-q${qNum}"] [data-cy="quizAnswerHistoryTable"] [data-cy="row4-colAnswerTxt"]`).should('contain', 'Answer 5')
        }
        verifyInputTextQuestion(2)
        verifyInputTextQuestion(3)
    });

    it('attempts that require grading do not contribute to the quiz metrics', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)

        cy.runQuizForUser(1, 1,[{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.runQuizForUser(1, 2,[{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/administrator/quizzes/quiz1/results');
        cy.get('[data-cy="noMetricsYet"]').contains('Results will be available once at least 1 Quiz is completed')

        cy.gradeQuizAttempt(1, true, 'correct!')
        cy.visit('/administrator/quizzes/quiz1/results');

        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '1')
        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardDescription"]').contains('1 attempt by 1 user')

        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardValue"]').should('have.text', '1')
        cy.get('[data-cy="metricsCardPassed"] [data-cy="statCardDescription"]').contains('1 attempt passed by 1 user')

        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardValue"]').should('have.text', '0')
        cy.get('[data-cy="metricsCardFailed"] [data-cy="statCardDescription"]').contains('0 attempts failed by 0 users')

        cy.get('[data-cy="metricsCardRuntime"] [data-cy="statCardDescription"]').contains('Average Quiz runtime for 1 attempt')
        cy.get('[data-cy="noMetricsYet"]').should('not.exist')

        cy.get('[data-cy="metrics-q1"]').contains('Correct: 1 Attempts')
        cy.get('[data-cy="metrics-q1"]').contains('Wrong: 0 Attempts')
        cy.get('[data-cy="metrics-q1"] [data-p-index="0"] [data-pc-group-section="rowactionbutton"]').click()
        cy.get('[data-cy="metrics-q1"] [data-cy="row0-answerHistory"]')
        cy.get(`[data-cy="metrics-q1"] [data-cy="row0-answerHistory"] [data-cy="quizAnswerHistoryTable"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.get(`[data-cy="metrics-q1"] [data-cy="row0-answerHistory"] [data-cy="quizAnswerHistoryTable"] [data-pc-section="bodyrow"]`).should('have.length', 1)
        cy.get(`[data-cy="metrics-q1"] [data-cy="row0-answerHistory"] [data-cy="quizAnswerHistoryTable"] [data-p-index="0"]`).contains('user2')

        cy.get(`[data-cy="metrics-q2"]`).contains('Text Input')
        cy.get('[data-cy="metrics-q2"]').contains('Correct: 1 Attempts')
        cy.get('[data-cy="metrics-q2"]').contains('Wrong: 0 Attempts')
        cy.get(`[data-cy="metrics-q2"] [data-cy="quizAnswerHistoryTable"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.get(`[data-cy="metrics-q2"] [data-cy="quizAnswerHistoryTable"] [data-pc-section="bodyrow"]`).should('have.length', 1)
        cy.get(`[data-cy="metrics-q2"] [data-cy="quizAnswerHistoryTable"] [data-p-index="0"]`).contains('user2')

        cy.get(`[data-cy="metrics-q3"]`).contains('Text Input')
        cy.get('[data-cy="metrics-q3"]').contains('Correct: 1 Attempts')
        cy.get('[data-cy="metrics-q3"]').contains('Wrong: 0 Attempts')
        cy.get(`[data-cy="metrics-q3"] [data-cy="quizAnswerHistoryTable"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.get(`[data-cy="metrics-q3"] [data-cy="quizAnswerHistoryTable"] [data-pc-section="bodyrow"]`).should('have.length', 1)
        cy.get(`[data-cy="metrics-q3"] [data-cy="quizAnswerHistoryTable"] [data-p-index="0"]`).contains('user2')

    });

    it('display grading information on answer history table', function () {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.runQuizForUser(1, 1,[{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.gradeQuizAttempt(1, true, 'correct!')

        cy.visit('/administrator/quizzes/quiz1/results');

        cy.get('[data-cy="metricsCardTotal"] [data-cy="statCardValue"]').should('have.text', '1')

        cy.get(`[data-cy="metrics-q1"] [data-cy="quizAnswerHistoryTable"] [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.get(`[data-cy="metrics-q1"] [data-cy="quizAnswerHistoryTable"] [data-pc-section="bodyrow"]`).should('have.length', 1)
        cy.validateTable('[data-cy="metrics-q1"] [data-cy="quizAnswerHistoryTable"]', [
            [{ colIndex: 1, value: 'user1' }, { colIndex: 2, value: 'CORRECT' }],
        ], 5);

    });
});
