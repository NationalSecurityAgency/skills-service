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


describe('Grade Quizzes', () => {

    let defaultUser
    beforeEach(() => {
        defaultUser = Cypress.env('oauthMode') ? 'foo': Cypress.env('proxyUser')
    })

    it('quiz without Input Text questions', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="noContent"]').contains('No Manual Grading Required')
        cy.get( '[data-cy="quizRunsToGradeTable"]').should('not.exist')
    });

    it('ability to subscribe and unsubscribe from grading notifications', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get( '[data-cy="quizRunsToGradeTable"]').should('exist')
        cy.get('[data-cy="noContent"]').should('not.exist')

        cy.get('[data-cy="subscribeSwitch"] input').should('be.checked')

        // emails are enabled by default
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}], true, 'My Answer')
        cy.getEmails().then((emails) => {
            expect(emails[0].text).to.contain('User [user1] has completed the [This is quiz 1] quiz which requires manual grading');
        });
        cy.resetEmail()

        // disable notifications
        cy.get('[data-cy="subscribeSwitch"]').click()
        cy.get('[data-cy="subscribeSwitch"] input').should('not.be.checked')
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}], true, 'My Answer')

        cy.wait(2000)
        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get( '[data-cy="quizRunsToGradeTable"]').should('exist')
        cy.get('[data-cy="noContent"]').should('not.exist')

        cy.get('[data-cy="subscribeSwitch"] input').should('not.be.checked')

        cy.get('[data-cy="subscribeSwitch"]').click()
        cy.get('[data-cy="subscribeSwitch"] input').should('be.checked')
        cy.runQuizForUser(1, 3, [{selectedIndex: [0]}], true, 'My Answer')
        cy.getEmails().then((emails) => {
            expect(emails[0].text).to.contain('User [user3] has completed the [This is quiz 1] quiz which requires manual grading');
        });
    });

    it('mark quiz with 1 question as correct', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}], true, '**My Answer**')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]').should('not.exist')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')
        cy.get('[data-cy="attemptGradedFor_user1"]')

        cy.get('[data-cy="nav-Runs"]').click()

        const tableSelector = '[data-cy="quizRunsHistoryTable"]';
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
        ], 10);

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="userInfoCard"]').contains('user1')
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 1')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="TextInputAnswer"]').should('contain.text', 'My Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="TextInputAnswer"]').should('not.contain.text', '**My Answer**')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUser)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').should('not.exist')
    });

    it('mark quiz with 1 question as wrong', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]').should('not.exist')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')
        cy.get('[data-cy="attemptGradedFor_user1"]')

        cy.get('[data-cy="nav-Runs"]').click()

        const tableSelector = '[data-cy="quizRunsHistoryTable"]';
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Failed' }],
        ], 10);

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="userInfoCard"]').contains('user1')
        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('0 / 1')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUser)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').should('not.exist')
    });

    it('grade quiz with multiple gradable questions', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="gradedTag"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]').should('not.exist')

        // q1
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="feedbackTxtMarkdownEditor"]').type('Question 1 is correct')
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled').click()
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').should('not.exist')
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').should('not.exist')
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')
        // cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')
        //
        // // q2
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled').click()
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="questionDisplayText"]').should('not.exist')
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="answer_1displayText"]').should('not.exist')
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="gradedTag"]')
        // cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')
        //
        // // q3
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="feedbackTxtMarkdownEditor"]').type('Question 3 is wrong')
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="questionDisplayText"]').should('not.exist')
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="answer_1displayText"]').should('not.exist')
        // cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]')
        //
        // // all 3 are graded
        // cy.get('[data-cy="attemptGradedFor_user1"]')
        //
        // cy.get('[data-cy="nav-Runs"]').click()
        //
        // const tableSelector = '[data-cy="quizRunsHistoryTable"]';
        // cy.validateTable(tableSelector, [
        //     [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Failed' }],
        // ], 10);
        //
        // cy.get('[data-cy="row0-viewRun"]').click()
        // cy.get('[data-cy="userInfoCard"]').contains('user1')
        // cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        // cy.get('[data-cy="numQuestionsToPass"]').contains('2 / 3')
        //
        // cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        // cy.get('[data-cy="questionDisplayCard-1"] [data-cy="TextInputAnswer"]').contains('My Answer')
        // cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('not.exist')
        // cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUser)
        // cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Question 1 is correct')
        //
        // cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        // cy.get('[data-cy="questionDisplayCard-2"] [data-cy="TextInputAnswer"]').contains('My Answer')
        // cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('not.exist')
        // cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUser)
        // cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').should('not.exist')
        //
        // cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        // cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('My Answer')
        // cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]')
        // cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUser)
        // cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Question 3 is wrong')
    });

    it('partially graded quiz', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="gradedTag"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]').should('not.exist')

        // q1
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="feedbackTxtMarkdownEditor"]').type('Question 1 is correct')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')

        // q3
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="feedbackTxtMarkdownEditor"]').type('Question 3 is wrong')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="questionDisplayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="answer_1displayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')

        cy.get('[data-cy="nav-Runs"]').click()

        const tableSelector = '[data-cy="quizRunsHistoryTable"]';
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Needs Grading' }],
        ], 10);

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="userInfoCard"]').contains('user1')
        cy.get('[data-cy="quizRunStatus"]').contains('Needs Grading')
        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 3')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUser)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Question 1 is correct')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="needsGradingTag"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUser)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Question 3 is wrong')
    });

    it('grade quiz with multiple gradable questions and auto-graded questions', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)
        cy.createQuizQuestionDef(1, 4)
        cy.setMinNumQuestionsToPass(1, 2)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="gradedTag"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]').should('not.exist')

        // q1
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="feedbackTxtMarkdownEditor"]').type('Question 1 is correct')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')

        // q3
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="feedbackTxtMarkdownEditor"]').type('Question 3 is wrong')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="questionDisplayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="answer_1displayText"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]')

        // all 3 are graded
        cy.get('[data-cy="attemptGradedFor_user1"]')

        cy.get('[data-cy="nav-Runs"]').click()

        const tableSelector = '[data-cy="quizRunsHistoryTable"]';
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
        ], 10);

        cy.get('[data-cy="row0-viewRun"]').click()
        cy.get('[data-cy="userInfoCard"]').contains('user1')
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('2 / 4')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUser)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Question 1 is correct')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]')

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUser)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Question 3 is wrong')

        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 4')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="wrongAnswer"]').should('not.exist')
    });

    it('validate feedback: custom description validator', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('hi jabberwocky')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markCorrectBtn"]').should('be.enabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback - paragraphs may not contain jabberwocky')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="gradedTag"]')

        // all 3 are graded
        cy.get('[data-cy="attemptGradedFor_user1"]')
    });

    it('validate feedback: max char length', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
        cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')

        const longValue = 'a'.repeat(500)
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"] .toastui-editor-contents').invoke('text', longValue)
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('b')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback must be at most 500 characters')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markCorrectBtn"]').should('be.enabled')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_3"] [data-cy="gradedTag"]')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.disabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').contains('Feedback must be at most 500 characters')

        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="feedbackTxtMarkdownEditor"]').type('{backspace}')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markCorrectBtn"]').should('be.enabled')
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="markWrongBtn"]').should('be.enabled').click()
        cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_2"] [data-cy="gradedTag"]')

        // all 3 are graded
        cy.get('[data-cy="attemptGradedFor_user1"]')
    });

    it('grade multiple quizzes with multiple gradable questions', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.runQuizForUser(1, 3, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
        cy.get('[data-cy="gradeBtn_user2"]').should('be.enabled').click()
        cy.get('[data-cy="gradeBtn_user3"]').should('be.enabled').click()

        const preValidateUserQuiz = (userId) => {
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_1"] [data-cy="questionDisplayText"]`).contains('This is a question # 1')
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_1"] [data-cy="answer_1displayText"]`).contains('My Answer')
            cy.get(`[data-cy="attemptGradedFor_${userId}"]`).should('not.exist')
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_2"] [data-cy="questionDisplayText"]`).contains('This is a question # 2')
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_2"] [data-cy="answer_2displayText"]`).contains('My Answer')
            cy.get(`[data-cy="attemptGradedFor_${userId}"]`).should('not.exist')

            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_1"] [data-cy="gradedTag"]`).should('not.exist')
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_2"] [data-cy="gradedTag"]`).should('not.exist')
        }
        preValidateUserQuiz('user1')
        preValidateUserQuiz('user2')
        preValidateUserQuiz('user3')


        const gradeUserAttempt = (userId, pass=true) => {
            // q1
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_1"] [data-cy="feedbackTxtMarkdownEditor"]`).type('Question 1 is correct')
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_1"] [data-cy="markCorrectBtn"]`).should('be.enabled').click()
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_1"] [data-cy="questionDisplayText"]`).should('not.exist')
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_1"] [data-cy="answer_1displayText"]`).should('not.exist')
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_1"] [data-cy="gradedTag"]`)
            cy.get(`[data-cy="attemptGradedFor_${userId}"]`).should('not.exist')

            // q2
            const btnToclick = pass ? 'markCorrectBtn' : 'markWrongBtn'
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_2"] [data-cy="${btnToclick}"]`).should('be.enabled').click()
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_2"] [data-cy="questionDisplayText"]`).should('not.exist')
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_2"] [data-cy="answer_1displayText"]`).should('not.exist')
            cy.get(`[data-cy="gradeAttemptFor_${userId}"] [data-cy="question_2"] [data-cy="gradedTag"]`)

            // done grading
            cy.get(`[data-cy="attemptGradedFor_${userId}"]`)
        }

        gradeUserAttempt('user1')
        gradeUserAttempt('user2', false)
        gradeUserAttempt('user3')

        cy.get('[data-cy="nav-Runs"]').click()

        const tableSelector = '[data-cy="quizRunsHistoryTable"]';
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'user3' }, { colIndex: 2, value: 'Passed' }],
            [{ colIndex: 0, value: 'user2' }, { colIndex: 2, value: 'Failed' }],
            [{ colIndex: 0, value: 'user1' }, { colIndex: 2, value: 'Passed' }],
        ], 10);
    });

});


