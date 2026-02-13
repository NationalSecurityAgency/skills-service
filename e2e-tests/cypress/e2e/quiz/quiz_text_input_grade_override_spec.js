/*
 * Copyright 2026 SkillTree
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

describe('Text Input question grading override', () => {

    const otherUser = 'user1@skills.net'
    let defaultUser
    beforeEach(() => {
        defaultUser = Cypress.env('oauthMode') ? 'foo': Cypress.env('proxyUser')
    })

    it('override correct status for ai-graded question', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');

        cy.createQuizDef(1);
        cy.setQuizShowCorrectAnswers(1, true)
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.request(`/admin/quiz-definitions/quiz1/questions`)
            .then((response) => {
                const questions = response.body.questions
                cy.saveQuizTextInputAiGraderConfigs(1, questions[1].id, "correct answer", 75)
            })

        // cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}], 'answer 92')
        cy.runQuizForUser(1, otherUser, [{selectedIndex: [0]}, {selectedIndex: [0]}], true,'answer 51')

        cy.waitForBackendAsyncTasksToComplete()

        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.wait('@getConfig')

        const tableSelector = '[data-cy="quizRunsHistoryTable"]'
        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: 'Failed'}],
        ], 5);
        cy.get(`${tableSelector} [data-cy="row0-viewRun"]`).click()

        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', 'Grader: AI Assistant')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Your answer has confidence level of 51')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="quizRequiresGradingMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="aiGradingFailedButHasRetriesMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="aiGradingFailedMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="gradeResConfidence"]').should('have.text', '51%')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="overrideGradeBtn"]').click()
        cy.get('[data-cy="overrideGradeWarningToCorrect"]')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.typeInMarkdownEditor('[data-cy="feedback"]', 'Updated Feedback')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="overrideGradeWarningToCorrect"]').should('not.exist')

        cy.get('[data-cy="quizRunStatus"]').contains('Passed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('2 / 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', `Grader: ${defaultUser}`)
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Updated Feedback')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="quizRequiresGradingMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="aiGradingFailedButHasRetriesMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="aiGradingFailedMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="gradeResConfidence"]').should('not.exist')
    });

    it('override correct status for manually-graded question', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');

        cy.createQuizDef(1);
        cy.setQuizShowCorrectAnswers(1, true)
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)

        cy.runQuizForUser(1, otherUser, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [0]}], true,'answer 51')
        cy.gradeQuizAttempt(1, false)
        cy.getEmails(2).then((emails) => {
            const sortedEmails = emails.sort((a, b) => b.subject - a.subject)
            expect(sortedEmails[0].subject).to.eq('SkillTree Quiz Grading Requested');
            expect(sortedEmails[1].subject).to.eq('SkillTree Quiz Graded');
        });
        cy.resetEmail()

        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.wait('@getConfig')

        const tableSelector = '[data-cy="quizRunsHistoryTable"]'
        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: 'Failed'}],
        ], 5);
        cy.get(`${tableSelector} [data-cy="row0-viewRun"]`).click()

        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('1 / 3')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', `Grader: ${defaultUser}`)
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Good answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="quizRequiresGradingMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="aiGradingFailedButHasRetriesMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="aiGradingFailedMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="gradeResConfidence"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', `Grader: ${defaultUser}`)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Good answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="quizRequiresGradingMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="aiGradingFailedButHasRetriesMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="aiGradingFailedMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="gradeResConfidence"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="overrideGradeBtn"]').click()
        cy.get('[data-cy="overrideGradeWarningToCorrect"]')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.typeInMarkdownEditor('[data-cy="feedback"]', 'Updated Feedback')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="overrideGradeWarningToCorrect"]').should('not.exist')

        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="numQuestionsToPass"]').contains('2 / 3')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', `Grader: ${defaultUser}`)
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Updated Feedback')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="quizRequiresGradingMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="aiGradingFailedButHasRetriesMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="aiGradingFailedMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="gradeResConfidence"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', `Grader: ${defaultUser}`)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Good answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="quizRequiresGradingMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="aiGradingFailedButHasRetriesMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="aiGradingFailedMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="gradeResConfidence"]').should('not.exist')
        cy.wait(5000)
        cy.getEmails(0).then((emails) => {
            expect(emails).to.be.empty
        });
    });

    it('can notify quiz taker when grade is overridden', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');

        cy.createQuizDef(1);
        cy.setQuizShowCorrectAnswers(1, true)
        cy.createTextInputQuestionDef(1, 1)

        cy.runQuizForUser(1, otherUser, [{selectedIndex: [0]}], true,'answer 51')
        cy.gradeQuizAttempt(1, false)
        cy.getEmails(2).then((emails) => {
            const sortedEmails = emails.sort((a, b) => b.subject - a.subject)
            expect(sortedEmails[0].subject).to.eq('SkillTree Quiz Grading Requested');
            expect(sortedEmails[1].subject).to.eq('SkillTree Quiz Graded');
        });
        cy.resetEmail()

        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.wait('@getConfig')

        const tableSelector = '[data-cy="quizRunsHistoryTable"]'
        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: 'Failed'}],
        ], 5);
        cy.get(`${tableSelector} [data-cy="row0-viewRun"]`).click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', `Grader: ${defaultUser}`)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Good answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="quizRequiresGradingMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="aiGradingFailedButHasRetriesMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="aiGradingFailedMsg"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="gradeResConfidence"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="overrideGradeBtn"]').click()
        cy.get('[data-cy="overrideGradeWarningToCorrect"]')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.typeInMarkdownEditor('[data-cy="feedback"]', 'Updated Feedback')
        cy.get('[data-cy="inputSwitch-notifyUser"]').click()
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="overrideGradeWarningToCorrect"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Updated Feedback')

        cy.getEmails().then((emails) => {
            const sortedEmails = emails.sort((a, b) => b.subject - a.subject)
            console.log(sortedEmails)
            expect(sortedEmails[0].subject).to.eq('SkillTree Quiz Graded');
            expect(emails[0].text).to.contain('Congratulations, you passed the quiz [This is quiz 1]');
        });
    });

});


