/*
 * Copyright 2024 SkillTree
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

describe('Accessibility Quiz Tests', () => {

    beforeEach(() => {
    });
    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {
        it(`empty quiz definitions page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/quizzes/')
            cy.get('[data-cy="noQuizzesYet"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`new quiz modal${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/quizzes/')
            cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
            cy.get('[data-cy="quizName"]').type('hello')
            cy.get('[data-cy="quizDescription"]').type('hi')
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`quiz definitions page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1, { name: 'Test Your Trivia Knowledge' });
            cy.createSurveyDef(2);
            cy.createQuizDef(3);
            cy.createSurveyDef(4);
            cy.createSurveyDef(5);
            cy.createSurveyDef(6);

            cy.visit('/administrator/quizzes/')
            cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '6')
            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`empty survey page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1, { name: 'Test Your Trivia Knowledge' });

            cy.visit('/administrator/quizzes/quiz1')
            cy.get('[data-cy="noQuestionsYet"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`new question modal${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1, { name: 'Test Your Trivia Knowledge' });

            cy.visit('/administrator/quizzes/quiz1')
            cy.get('[data-cy="noQuestionsYet"]')

            cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
            cy.get('[data-cy="questionText"]').type('hi')
            cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('answer1')
            cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`new survey modal with TextInput question type${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1, { name: 'Test Your Trivia Knowledge' });

            cy.visit('/administrator/quizzes/quiz1')
            cy.get('[data-cy="noQuestionsYet"]')

            cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
            cy.get('[data-cy="answerTypeSelector"]').click()
            cy.get('[data-cy="selectionItem_TextInput"]').click()
            cy.get('[data-cy="textAreaPlaceHolder"]').should('be.visible')
            cy.get('[data-cy="questionText"]').type('hi')

            cy.wait(1111);
            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`survey page with questions${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

            cy.visit('/administrator/quizzes/quiz1')
            cy.get('[data-cy="editQuestionButton_1"]').should('be.enabled')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`empty results page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1, { name: 'Test Your Trivia Knowledge' });

            cy.visit('/administrator/quizzes/quiz1/results')
            cy.get('[data-cy="noMetricsYet"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`runs page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 1);
            cy.runQuizForUser(1, 1, [{ selectedIndex: [1] }]);
            cy.runQuizForUser(1, 2, [{ selectedIndex: [0] }], false);

            cy.visit('/administrator/quizzes/quiz1/runs')
            cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '2')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`single run${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
            cy.createTextInputQuestionDef(1, 3);
            cy.runQuizForUser(1, 1, [{ selectedIndex: [1] }, { selectedIndex: [0] }, { selectedIndex: [0] }])

            cy.visit('/administrator/quizzes/quiz1/runs')
            cy.get('[data-cy="row0-viewRun"]').click();
            cy.get('[data-cy="questionDisplayCard-1"]').contains('This is a question # 1')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`survey results page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
            cy.createTextInputQuestionDef(1, 3);
            cy.runQuizForUser(1, 1, [{ selectedIndex: [1] }, { selectedIndex: [0] }, { selectedIndex: [0] }])

            cy.visit('/administrator/quizzes/quiz1/results')
            cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`quiz results page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1);
            cy.createQuizQuestionDef(1, 1, { question: 'This is a Single Choice Question example for metrics.'})
            cy.createQuizMultipleChoiceQuestionDef(1, 2, { question: 'This is a Multiple Choice Question example for metrics.'});
            cy.createTextInputQuestionDef(1, 3);
            cy.createQuizMatchingQuestionDef(1, 4)
            cy.runQuizForUser(1, Cypress.env('proxyUser'), [{ selectedIndex: [1] }, { selectedIndex: [0] }, { selectedIndex: [0] }, {selectedIndex: [0, 2, 1]}])
            cy.gradeQuizAttempt(1, false, 'Wrong answer', false)
            cy.visit('/administrator/quizzes/quiz1/results')
            cy.get('[data-cy="metrics-q1"] [data-p-index="1"] [data-pc-section="rowtogglebutton"]').should('be.enabled')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`grading page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1);
            cy.createTextInputQuestionDef(1, 1)
            cy.runQuizForUser(1, 1, [{selectedIndex: [0]}], true, 'My Answer')


            cy.visit('/administrator/quizzes/quiz1/grading')
            cy.get('[data-cy="gradeBtn_user1"]').should('be.enabled').click()
            cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
            cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').contains('My Answer')
            cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]').should('not.exist')
            cy.get('[data-cy="attemptGradedFor_user1"]').should('not.exist')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();

            cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="markCorrectBtn"]').should('be.enabled').click()
            cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="questionDisplayText"]').should('not.exist')
            cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="answer_1displayText"]').should('not.exist')
            cy.get('[data-cy="gradeAttemptFor_user1"] [data-cy="question_1"] [data-cy="gradedTag"]')
            cy.get('[data-cy="attemptGradedFor_user1"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`quiz access page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1);

            cy.visit('/administrator/quizzes/quiz1/access')
            cy.get('[data-cy="roleManagerTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`quiz settings page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1);
            cy.createQuizQuestionDef(1, 1);
            cy.createQuizQuestionDef(1, 2);
            cy.createQuizQuestionDef(1, 3);

            cy.visit('/administrator/quizzes/quiz1/settings');
            cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`text input ai grader settings page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.fixture('vars.json').then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
                cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.logout();

                cy.login(vars.defaultUser, vars.defaultPass);
            });

            const descMsg = 'Friendly Reminder: Only safe descriptions for {{community.project.descriptor}}'
            cy.intercept('GET', '/public/config', (req) => {
                req.reply((res) => {
                    const conf = res.body;
                    conf.enableOpenAIIntegration = true;
                    conf.descriptionWarningMessage = descMsg;
                    res.send(conf);
                });
            }).as('getConfig');
            cy.createQuizDef(1);
            cy.createTextInputQuestionDef(1, 1)

            cy.createQuizDef(2, {enableProtectedUserCommunity: true});
            cy.createTextInputQuestionDef(2, 1)

            cy.request(`/admin/quiz-definitions/quiz1/questions`)
                .then((response) => {
                    const questions = response.body.questions
                    cy.visit(`/administrator/quizzes/quiz1/questions/${questions[0].id}/ai-grader`);
                    cy.wait('@getConfig')

                    cy.get('[data-cy="aiGraderEnabled"]').click()
                    cy.get('[data-cy="gradingInstructionsWarningMessage"]').contains('Friendly Reminder: Only safe descriptions for All Dragons')

                    cy.customLighthouse();
                    cy.injectAxe();
                    cy.customA11y();
                })
        });

        it(`override override dialog${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.intercept('GET', '/public/config', (req) => {
                req.reply((res) => {
                    const conf = res.body;
                    conf.enableOpenAIIntegration = true;
                    res.send(conf);
                });
            }).as('getConfig');

            cy.createQuizDef(1);
            cy.createTextInputQuestionDef(1, 1)

            cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}], true,'answer 51')
            cy.gradeQuizAttempt(1, false)

            cy.visit('/administrator/quizzes/quiz1/runs');
            cy.wait('@getConfig')

            const tableSelector = '[data-cy="quizRunsHistoryTable"]'
            cy.validateTable(tableSelector, [
                [{ colIndex: 2, value: 'Failed'}],
            ], 5);
            cy.get(`${tableSelector} [data-cy="row0-viewRun"]`).click()

            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]')
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="overrideGradeBtn"]').click()
            cy.get('[data-cy="overrideGradeWarningToCorrect"]')
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
            cy.get('[data-cy="descriptionError"]').should('not.be.visible')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });
    })
});
