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

describe('Client Display Survey Tests', () => {

    beforeEach(() => {
        Cypress.Commands.add("visitQuizPage", (env, validateSplash = true) => {
            const isDashboard = env == 'dashboard'
            if (isDashboard) {
                cy.log('Visiting Quiz Page in Dashboard')
                cy.visit('/progress-and-rankings/quizzes/quiz1')
                if (validateSplash) {
                    cy.get('[data-cy="subPageHeader"]').contains('Survey')
                    cy.get('[data-cy="quizSplashScreen"]').contains('Thank you for taking time to take this survey')
                }
            } else {
                cy.log('Visiting Quiz Page in client display')
                cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
                if (validateSplash) {
                    cy.get('[data-cy="title"]').contains('Survey')
                    cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by completing this survey')
                }
            }
        });

        Cypress.Commands.add("completedCheck", (env) => {
            const isDashboard = env == 'dashboard'
            if (isDashboard) {
                cy.get('[data-cy="surveyCompletion"]').contains('Thank you for taking the time to complete the survey')
            } else {
                cy.get('[data-cy="surveyCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by completing the survey')
            }
        });

        Cypress.Commands.add("closeQuizPage", (env) => {
            const isDashboard = env == 'dashboard'

            cy.completedCheck(env)
            if (isDashboard) {
                cy.get('[data-cy="surveyCompletion"] [data-cy="closeSurveyBtn"]').click()
                cy.get('[data-cy="manageMyProjsBtn"]')
            } else {
                cy.get('[data-cy="surveyCompletion"] [data-cy="closeSurveyBtn"]').click()
                cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
                cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]').should('have.text', '150')
            }
        });

    });

    it('initiate and run survey from a skill in client display', () => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Survey')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by completing this survey')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('not.exist')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool survey #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="surveyCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by completing the survey')

        cy.get('[data-cy="surveyCompletion"] [data-cy="closeSurveyBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]').should('have.text', '150')
    });

    it('cancel survey on the splash screen in client display', () => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="cancelQuizAttempt"]').click()
        cy.get('[data-cy="takeQuizBtn"]').should('be.enabled')
        cy.get('[data-cy="takeQuizBtn"]').click();
        cy.get('[data-cy="cancelQuizAttempt"]')
    });

    it('cancel survey on the splash screen in dashboard', () => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(1, 3);

        cy.visit('/progress-and-rankings/quizzes/quiz1')

        cy.get('[data-cy="cancelQuizAttempt"]').click()
        cy.get('[data-cy="manageMyProjsBtn"]')
    });

    it('answers are preserved after save-and-close in client-display', () => {
        // make validate call very slow that way as-you-type validation will not be performed by the time Done button is pressed
        cy.intercept('/api/quizzes/quiz1/attempt/*/answers/*', (req) => {
            req.reply((res) => {
                res.send({ delay: 3000 });
            });
        }).as('reportAnswer');
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Survey')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('a')

        cy.get('[data-cy="multipleChoiceMsg"]')
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()

        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="saveAndCloseQuizAttemptBtn"]').click()

        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').should('have.value', 'a')

        cy.get('[data-cy="question_2"] [data-cy="answer_1"] [data-cy="selected_true"]').should('exist')
        cy.get('[data-cy="question_2"] [data-cy="answer_1"] [data-cy="selected_false"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="answer_2"] [data-cy="selected_true"]').should('exist')
        cy.get('[data-cy="question_2"] [data-cy="answer_2"] [data-cy="selected_false"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="answer_3"] [data-cy="selected_true"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="answer_3"] [data-cy="selected_false"]').should('exist')

        cy.get('[data-cy="question_3"] [data-cy="answer_1"] [data-cy="selected_true"]').should('not.exist')
        cy.get('[data-cy="question_3"] [data-cy="answer_1"] [data-cy="selected_false"]').should('exist')
        cy.get('[data-cy="question_3"] [data-cy="answer_2"] [data-cy="selected_true"]').should('not.exist')
        cy.get('[data-cy="question_3"] [data-cy="answer_2"] [data-cy="selected_false"]').should('exist')
        cy.get('[data-cy="question_3"] [data-cy="answer_3"] [data-cy="selected_true"]').should('exist')
        cy.get('[data-cy="question_3"] [data-cy="answer_3"] [data-cy="selected_false"]').should('not.exist')

        cy.wait('@reportAnswer')
        cy.wait('@reportAnswer')
        cy.wait('@reportAnswer')
        cy.wait('@reportAnswer')
    });

    const environments =  ['dashboard', 'client-display']
    environments.forEach((env) => {
        it (`visit survey without any questions in [${env}]`, () => {
            cy.createSurveyDef(1);

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

            cy.visitQuizPage(env)
            cy.get('[data-cy="quizHasNoQuestions"]').contains('This Survey has no questions declared ')
            cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
            cy.get('[data-cy="cancelQuizAttempt"]').should('not.exist')
        });

        it(`run a survey in [${env}]`, () => {
            cy.createSurveyDef(1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2);
            cy.createSurveyMultipleChoiceQuestionDef(1, 3);

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

            cy.visitQuizPage(env)

            cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
            cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('not.exist')

            cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool survey #1! Thank you for taking it!')

            cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
            cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

            cy.get('[data-cy="startQuizAttempt"]').click()

            cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
            cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

            cy.get('[data-cy="completeQuizBtn"]').click()

            cy.closeQuizPage(env)
        });

        it(`run survey where answer reporting endpoints are very slow in [${env}]`, () => {
            // make validate call very slow that way as-you-type validation will not be performed by the time Done button is pressed
            cy.intercept('/api/quizzes/quiz1/attempt/*/answers/*', (req) => {
                req.reply((res) => {
                    res.send({ delay: 3000 });
                });
            }).as('reportAnswer');

            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

            cy.visitQuizPage(env)

            cy.get('[data-cy="startQuizAttempt"]').click()

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('a')

            cy.get('[data-cy="multipleChoiceMsg"]')
            cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()

            cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.completedCheck(env);
            cy.wait('@reportAnswer')
            cy.wait('@reportAnswer')
            cy.wait('@reportAnswer')
            cy.wait('@reportAnswer')
        });

        it(`answers are preserved after refreshing in [${env}]`, () => {
            cy.intercept('/api/quizzes/quiz1/attempt/*/answers/*').as('reportAnswer');
            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

            cy.visitQuizPage(env)

            cy.get('[data-cy="startQuizAttempt"]').click()

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('a')

            cy.get('[data-cy="multipleChoiceMsg"]')
            cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()

            cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

            cy.wait('@reportAnswer')
            cy.wait('@reportAnswer')
            cy.wait('@reportAnswer')
            cy.wait('@reportAnswer')

            cy.visitQuizPage(env, false)

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').should('have.value', 'a')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').should('not.be.visible');

            cy.get('[data-cy="question_2"] [data-cy="answer_1"] [data-cy="selected_true"]').should('exist')
            cy.get('[data-cy="question_2"] [data-cy="answer_1"] [data-cy="selected_false"]').should('not.exist')
            cy.get('[data-cy="question_2"] [data-cy="answer_2"] [data-cy="selected_true"]').should('exist')
            cy.get('[data-cy="question_2"] [data-cy="answer_2"] [data-cy="selected_false"]').should('not.exist')
            cy.get('[data-cy="question_2"] [data-cy="answer_3"] [data-cy="selected_true"]').should('not.exist')
            cy.get('[data-cy="question_2"] [data-cy="answer_3"] [data-cy="selected_false"]').should('exist')

            cy.get('[data-cy="question_3"] [data-cy="answer_1"] [data-cy="selected_true"]').should('not.exist')
            cy.get('[data-cy="question_3"] [data-cy="answer_1"] [data-cy="selected_false"]').should('exist')
            cy.get('[data-cy="question_3"] [data-cy="answer_2"] [data-cy="selected_true"]').should('not.exist')
            cy.get('[data-cy="question_3"] [data-cy="answer_2"] [data-cy="selected_false"]').should('exist')
            cy.get('[data-cy="question_3"] [data-cy="answer_3"] [data-cy="selected_true"]').should('exist')
            cy.get('[data-cy="question_3"] [data-cy="answer_3"] [data-cy="selected_false"]').should('not.exist')
        });

        it(`input text validation in [${env}]`, () => {
            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

            cy.visitQuizPage(env)
            cy.get('[data-cy="startQuizAttempt"]').click()

            cy.get('[data-cy="question_1"] [data-cy="questionsText"]').contains('This is a question # 1')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').should('not.be.visible')
            cy.get('[data-cy="questionErrors"]').should('not.exist')
            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get('[data-cy="question_1"] [data-cy="questionsText"]').contains('This is a question # 1')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').contains('Answer to question #1 is required')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #1 is required')

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('a')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').should('not.be.visible')
            cy.get('[data-cy="questionErrors"]').should('not.exist')

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').clear()
            cy.wait(1000)
            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get('[data-cy="question_1"] [data-cy="questionsText"]').contains('This is a question # 1')
            cy.get('[data-cy="questionErrors"]').should('exist')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').contains('Answer to question #1 is required')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #1 is required')

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('k')
            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.completedCheck(env)
        });

        it(`input text validation - custom validation in [${env}]`, () => {
            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

            cy.visitQuizPage(env)
            cy.get('[data-cy="startQuizAttempt"]').click()

            cy.get('[data-cy="question_1"] [data-cy="questionsText"]').contains('This is a question # 1')

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('jabberwocky')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky.')

            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky.')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky.')

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').clear().type('a')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').should('not.be.visible')
            cy.get('[data-cy="questionErrors"]').should('not.exist')

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('some jabberwocky some')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky.')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky.')
        });

        it(`input text validation - custom validation happens when completing the test in [${env}]`, () => {
            // make validate call very slow that way as-you-type validation will not be performed by the time Done button is pressed
            cy.intercept('/api/validation/description', (req) => {
                req.reply((res) => {
                    res.send({ delay: 3000 });
                });
            }).as('validateDescription');

            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

            cy.visitQuizPage(env)
            cy.get('[data-cy="startQuizAttempt"]').click()

            cy.get('[data-cy="question_1"] [data-cy="questionsText"]').contains('This is a question # 1')

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('jabberwock')
            cy.wait(500)
            // type last char and then click on the button to try to force validation at completion
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('y')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]')
                .should('be.visible').contains('Answer to question #1 - paragraphs may not contain jabberwocky.')
            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get('[data-cy="questionErrors"]')
                .should('be.visible').contains('Answer to question #1 - paragraphs may not contain jabberwocky.')
        });

        it(`all questions must be answered validation in [${env}]`, () => {
            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2);
            cy.createSurveyMultipleChoiceQuestionDef(1, 3);

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

            cy.visitQuizPage(env)
            cy.get('[data-cy="startQuizAttempt"]').click()

            cy.get('[data-cy="completeQuizBtn"]').click()

            cy.get('[data-cy="questionErrors"]').contains('Answer to question #1 is required')
            cy.get('[data-cy="questionErrors"]').contains('At least 1 choice must be selected')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').contains('Answer to question #1 is required')
            cy.get('[data-cy="question_2"] [data-cy="choiceAnswerErr"]').contains('At least 1 choice must be selected')
            cy.get('[data-cy="question_3"] [data-cy="choiceAnswerErr"]').contains('At least 1 choice must be selected')

            cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').contains('Answer to question #1 is required')
            cy.get('[data-cy="question_2"] [data-cy="choiceAnswerErr"]').should('not.be.visible')
            cy.get('[data-cy="question_3"] [data-cy="choiceAnswerErr"]').contains('At least 1 choice must be selected')

            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #1 is required')
            cy.get('[data-cy="questionErrors"]').contains('At least 1 choice must be selected')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').contains('Answer to question #1 is required')
            cy.get('[data-cy="question_2"] [data-cy="choiceAnswerErr"]').should('not.be.visible')
            cy.get('[data-cy="question_3"] [data-cy="choiceAnswerErr"]').contains('At least 1 choice must be selected')

            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('y')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #1 is required').should('not.exist')
            cy.get('[data-cy="questionErrors"]').contains('At least 1 choice must be selected')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').should('not.be.visible')
            cy.get('[data-cy="question_2"] [data-cy="choiceAnswerErr"]').should('not.be.visible')
            cy.get('[data-cy="question_3"] [data-cy="choiceAnswerErr"]').contains('At least 1 choice must be selected')

            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #1 is required').should('not.exist')
            cy.get('[data-cy="questionErrors"]').contains('At least 1 choice must be selected')
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswerErr"]').should('not.be.visible')
            cy.get('[data-cy="question_2"] [data-cy="choiceAnswerErr"]').should('not.be.visible')
            cy.get('[data-cy="question_3"] [data-cy="choiceAnswerErr"]').contains('At least 1 choice must be selected')
        });

        it('only up to 5 validation warnings are shown on the bottom', () => {
            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);
            cy.createTextInputQuestionDef(1, 2);
            cy.createTextInputQuestionDef(1, 3);
            cy.createTextInputQuestionDef(1, 4);
            cy.createTextInputQuestionDef(1, 5);
            cy.createTextInputQuestionDef(1, 6);
            cy.createTextInputQuestionDef(1, 7);

            cy.createProject(1)
            cy.createSubject(1,1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

            cy.visitQuizPage(env)
            cy.get('[data-cy="startQuizAttempt"]').click()

            cy.get('[data-cy="completeQuizBtn"]').click()

            cy.get('[data-cy="questionErrors"]').contains('Answer to question #1 is required')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #2 is required')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #3 is required')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #4 is required').should('not.be.visible')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #5 is required').should('not.be.visible')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #6 is required').should('not.be.visible')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #7 is required').should('not.be.visible')
            cy.get('[data-cy="questionErrors"]').contains('Expand 4 more...').click()
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #4 is required')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #5 is required')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #6 is required')
            cy.get('[data-cy="questionErrors"]').contains('Answer to question #7 is required')
            cy.get('[data-cy="questionErrors"]').contains('Collapse')
            cy.get('[data-cy="questionErrors"]').contains('Expand 4 more...').should('not.exist')
        });
    });

    it('taken survey cannot be taken again in client-display', () => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForUser(1, Cypress.env('proxyUser'), [{selectedIndex: [0]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You already completed this survey')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="cancelQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="quizPassInfo"]').should('not.exist')
        cy.get('[data-cy="closeQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="closeQuizAttemptInAlert"]').should('be.enabled')
        cy.get('[data-cy="closeQuizAttemptInAlert"]').contains('Close Survey')

        // close in alert
        cy.get('[data-cy="closeQuizAttemptInAlert"]').click()
        cy.get('[data-cy="skillDescription-skill1"]')

        // close on the bottom
        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You already completed this survey')
        cy.get('[data-cy="closeQuizAttempt"]').click()
        cy.get('[data-cy="skillDescription-skill1"]')
    })
});


