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

describe('Display Single Quiz Attempt Tests', () => {

    const tableSelector = '[data-cy="myQuizAttemptsTable"]'
    let defaultUser
    let defaultUserDisplay
    beforeEach(() => {
        defaultUser = Cypress.env('proxyUser')
        defaultUserDisplay = Cypress.env('oauthMode') ? 'foo' : Cypress.env('proxyUser')
    })

    it('passed quiz show its correct answers ', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createTextInputQuestionDef(1, 3)
        cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}, {selectedIndex: [0, 2]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.gradeQuizAttempt(1, true)

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('3 / 3')
        cy.get('[data-cy="numQuestionsToPass"]').contains('Need 3 questions to pass')

        // q1
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').contains('Question 1 - First Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_displayText"]').contains('Question 1 - Second Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').contains('Question 1 - Third Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-1"] [data-cy="notSelected"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-2"] [data-cy="notSelected"]')

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-0_displayText"]').contains('First Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-1_displayText"]').contains('Second Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-2_displayText"]').contains('Third Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-3_displayText"]').contains('Fourth Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-1"] [data-cy="notSelected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-2"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-3"] [data-cy="notSelected"]')

        // q2
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUserDisplay)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Good answer')


    });

    it('passed quiz show its wrong answers ', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createTextInputQuestionDef(1, 3)
        cy.createQuizQuestionDef(1, 4)
        cy.setMinNumQuestionsToPass(1, 1)
        cy.runQuizForUser(1, defaultUser, [
            {selectedIndex: [1]},
            {selectedIndex: [1, 2]},
            {selectedIndex: [0]},
            {selectedIndex: [0]}
        ], true, 'My Answer')
        cy.gradeQuizAttempt(1, false, 'Wrong answer', true)

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 4')
        cy.get('[data-cy="numQuestionsToPass"]').contains('Need 1 question to pass')

        // q1
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').contains('Question 1 - First Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_displayText"]').contains('Question 1 - Second Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').contains('Question 1 - Third Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="missedSelection"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-1"] [data-cy="wrongSelection"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-2"] [data-cy="notSelected"]')

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-0_displayText"]').contains('First Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-1_displayText"]').contains('Second Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-2_displayText"]').contains('Third Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-3_displayText"]').contains('Fourth Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-0"] [data-cy="missedSelection"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-1"] [data-cy="wrongSelection"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-2"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-3"] [data-cy="notSelected"]')

        // q3
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUserDisplay)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Wrong answer')

        // q4
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 4')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-0_displayText"]').contains('Question 4 - First Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-1_displayText"]').contains('Question 4 - Second Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-2_displayText"]').contains('Question 4 - Third Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answerDisplay-1"] [data-cy="notSelected"]')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answerDisplay-2"] [data-cy="notSelected"]')
    });

    it('non manually graded answers are not shown for failed attempts', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 4)
        cy.setMinNumQuestionsToPass(1, 2)
        cy.runQuizForUser(1, defaultUser, [
            {selectedIndex: [1]},
            {selectedIndex: [1, 2]},
            {selectedIndex: [0]}
        ], true, 'My Answer')

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 3')
        cy.get('[data-cy="numQuestionsToPass"]').contains('Need 2 questions to pass')

        cy.get('[data-cy="questionDisplayCard-1"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-4"]').should('not.exist')

        cy.get('[data-cy="allQuestionsNotDisplayedMsg"]')
    });

    it('only manually graded answers are show for failed attempts', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createTextInputQuestionDef(1, 3)
        cy.createQuizQuestionDef(1, 4)
        cy.setMinNumQuestionsToPass(1, 2)
        cy.runQuizForUser(1, defaultUser, [
            {selectedIndex: [1]},
            {selectedIndex: [1, 2]},
            {selectedIndex: [0]},
            {selectedIndex: [0]}
        ], true, 'My Answer')
        cy.gradeQuizAttempt(1, false, 'Wrong answer')

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 4')
        cy.get('[data-cy="numQuestionsToPass"]').contains('Need 2 questions to pass')

        cy.get('[data-cy="questionDisplayCard-1"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"]').should('not.exist')

        // q3
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUserDisplay)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Wrong answer')

        cy.get('[data-cy="questionDisplayCard-4"]').should('not.exist')

        cy.get('[data-cy="someQuestionsNotDisplayedMsg"]')
    });

    it('show quiz answers for failed attempts if quizAlwaysShowCorrectAnswers=true', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createTextInputQuestionDef(1, 3)
        cy.createQuizQuestionDef(1, 4)
        cy.setMinNumQuestionsToPass(1, 2)
        cy.setQuizShowCorrectAnswers(1, true)
        cy.runQuizForUser(1, defaultUser, [
            {selectedIndex: [1]},
            {selectedIndex: [1, 2]},
            {selectedIndex: [0]},
            {selectedIndex: [0]}
        ], true, 'My Answer')
        cy.gradeQuizAttempt(1, false, 'Wrong answer')

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 4')
        cy.get('[data-cy="numQuestionsToPass"]').contains('Need 2 questions to pass')

        // q1
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').contains('Question 1 - First Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_displayText"]').contains('Question 1 - Second Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').contains('Question 1 - Third Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="missedSelection"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-1"] [data-cy="wrongSelection"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-2"] [data-cy="notSelected"]')

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-0_displayText"]').contains('First Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-1_displayText"]').contains('Second Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-2_displayText"]').contains('Third Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-3_displayText"]').contains('Fourth Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-0"] [data-cy="missedSelection"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-1"] [data-cy="wrongSelection"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-2"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-3"] [data-cy="notSelected"]')

        // q2
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUserDisplay)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Wrong answer')

        // q4
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 4')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-0_displayText"]').contains('Question 4 - First Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-1_displayText"]').contains('Question 4 - Second Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answer-2_displayText"]').contains('Question 4 - Third Answer')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answerDisplay-1"] [data-cy="notSelected"]')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="answerDisplay-2"] [data-cy="notSelected"]')
    });

    it('pending grading attempt', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createTextInputQuestionDef(1, 3)
        cy.createQuizQuestionDef(1, 4)
        cy.setMinNumQuestionsToPass(1, 2)
        cy.runQuizForUser(1, defaultUser, [
            {selectedIndex: [1]},
            {selectedIndex: [1, 2]},
            {selectedIndex: [0]},
            {selectedIndex: [0]}
        ], true, 'My Answer')

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
        cy.get('[data-cy="quizRunStatus"]').contains('Needs Grading')
        cy.get('[data-cy="numQuestionsToPass"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-4"]').should('not.exist')

        cy.get('[data-cy="quizRequiresGradingMsg"]')
    });

});


