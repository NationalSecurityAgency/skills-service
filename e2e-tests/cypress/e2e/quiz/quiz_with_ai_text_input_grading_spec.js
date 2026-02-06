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
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';
import moment from "moment-timezone";

describe('Display quiz that has ai-graded answers', () => {

    const otherUser = 'user1@skills.net'
    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();
        cy.loginAsDefaultUser()

        cy.createQuizDef(1);
        cy.setQuizShowCorrectAnswers(1, true)
        cy.createTextInputQuestionDef(1, 1)
        cy.request(`/admin/quiz-definitions/quiz1/questions`)
            .then((response) => {
                const questions = response.body.questions
                cy.saveQuizTextInputAiGraderConfigs(1, questions[0].id, "correct answer", 75)
            })

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}], 'answer 92')

        cy.runQuizForUser(1, otherUser, [{selectedIndex: [0]}], true,'will fail')
        cy.logout()
        cy.loginAsDefaultUser()

        cy.waitForBackendAsyncTasksToComplete()
    })

    after(() => {
        Cypress.env('disableResetDb', false);
    });

    it('ai graded question on trainee quiz attempt pages', () => {
        cy.visit('/progress-and-rankings/my-quiz-attempts');
        const tableSelector = '[data-cy="myQuizAttemptsTable"]'
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }, {
                colIndex: 2,
                value: 'Passed'
            }],
        ], 5);
        cy.get('[data-p-index="0"] [data-cy="viewQuizAttempt"]').click()

        cy.get('[data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]')
        cy.get('[data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]').should('not.exist')
        cy.get('[data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', 'Grader: AI Assistant')
        cy.get('[data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Your answer has confidence level of 92')
    });

    it('ai graded question on trainee skill page', () => {
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('[data-cy="viewQuizAttemptInfo"]').click()

        cy.get('[data-cy="quizCompletedMsg"]').contains('You have passed This is quiz 1 Quiz')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', 'Grader: AI Assistant')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Your answer has confidence level of 92')
    });

    it('skills-client: ai graded question on trainee skill page', () => {
        cy.visit('/test-skills-client/proj1?skillsClientDisplayPath=%2Fsubjects%2Fsubj1%2Fskills%2Fskill1')
        cy.wrapIframe().find('[data-cy="viewQuizAttemptInfo"]').click()

        cy.wrapIframe().find('[data-cy="quizCompletedMsg"]').contains('You have passed This is quiz 1 Quiz')
        cy.wrapIframe().find('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]')
        cy.wrapIframe().find('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]').should('not.exist')
        cy.wrapIframe().find('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', 'Grader: AI Assistant')
        cy.wrapIframe().find('[data-cy="questionDisplayCard-1"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Your answer has confidence level of 92')
    });

    it('graded pended question on trainee quiz attempt pages', () => {
        cy.logout()
        cy.login(otherUser)

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        const tableSelector = '[data-cy="myQuizAttemptsTable"]'
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }, {
                colIndex: 2,
                value: 'Needs Grading'
            }],
        ], 5);
        cy.get('[data-p-index="0"] [data-cy="viewQuizAttempt"]').click()

        cy.get('[data-cy="quizRequiresGradingMsg"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="queuedForAiGradingTag"]').should('not.exist')
        cy.get('[data-cy="manuallyGradedInfo"]').should('not.exist')

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')

        cy.get('[data-cy="quizRequiresGradingMsg"]')
        cy.get('[data-cy="quizCompletedMsg"]').should('not.exist')
        cy.wait(2000)

        cy.logout()
        cy.loginAsDefaultUser()
    });

    it('ai graded question on admin quiz run page', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.wait('@getConfig')

        const tableSelector = '[data-cy="quizRunsHistoryTable"]'
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Needs Grading'
            }],
            [{
                colIndex: 2,
                value: 'Passed'
            }],
        ], 5);
        cy.get(`${tableSelector} [data-cy="row1-viewRun"]`).click()

        cy.get('[data-cy="manuallyGradedInfo"] [data-cy="aiGraded"]')
        cy.get('[data-cy="manuallyGradedInfo"] [data-cy="adminGraded"]').should('not.exist')
        cy.get('[data-cy="manuallyGradedInfo"] [data-cy="grader"]').should('have.text', 'Grader: AI Assistant')
        cy.get('[data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Your answer has confidence level of 92')
        cy.get('[data-cy="quizRequiresGradingMsg"]').should('not.exist')

        cy.go('back')
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Needs Grading'
            }],
            [{
                colIndex: 2,
                value: 'Passed'
            }],
        ], 5);
        cy.get(`${tableSelector} [data-cy="row0-viewRun"]`).click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="noAnswer"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="queuedForAiGradingTag"]')
        cy.get('[data-cy="manuallyGradedInfo"]').should('not.exist')
        cy.get('[data-cy="quizRequiresGradingMsg"]').should('not.exist')
    });

    it('ai grading pending ', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.wait('@getConfig')

        const tableSelector = '[data-cy="quizRunsToGradeTable"]'
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="queuedForAiGradingMsg"]`)
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="gradeBtn_user1@skills.net"]`).click()
        cy.get('[data-cy="gradeAttemptFor_user1@skills.net"] [data-cy="question_1"] [data-cy="aiManualGradingLongMsg"]')
    });

    it('all questions were graded since the table was loaded', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/runs/*', (req) => {
            req.reply((res) => {
                const quizRun = res.body;
                quizRun.questions[0].needsGrading = false;
                res.send(quizRun);
            });
        }).as('getSingleQuizRun');

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.wait('@getConfig')

        const tableSelector = '[data-cy="quizRunsToGradeTable"]'
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="queuedForAiGradingMsg"]`)
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="gradeBtn_user1@skills.net"]`).click()
        cy.wait('@getSingleQuizRun')

        cy.get('[data-cy="questionGradedSinceLoadedMsg"]')
        cy.get('[data-cy="aiManualGradingLongMsg"]').should('not.exist')
    });

    it('single question was graded after the grading input loaded', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.intercept('POST', '/admin/quiz-definitions/quiz1/users/user1@skills.net/attempt/**', (req) => {
            req.reply({
                statusCode: 400,
                body: {
                    errorCode: 'QuizAlreadyCompleted',
                    message: 'Provided quiz attempt id was already completed'
                }
            });
        }).as('grade');

        cy.visit('/administrator/quizzes/quiz1/grading');
        cy.wait('@getConfig')

        const tableSelector = '[data-cy="quizRunsToGradeTable"]'
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="queuedForAiGradingMsg"]`)
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="gradeBtn_user1@skills.net"]`).click()
        cy.get(`[data-cy="markCorrectBtn"]`).click()


        cy.wait('@grade')

        cy.get('[data-cy="singleQuestionGradedSinceLoadedMsg"]')
        cy.get('[data-cy="aiManualGradingLongMsg"]').should('not.exist')
    });

});


