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

describe('Client Display Quiz Tests', () => {

    beforeEach(() => {
        Cypress.Commands.add("validateQuestionAnswer", (question) => {

            const questionSelector = `[data-cy="question_${question.num}"]`
            if (question.correct) {
                cy.get(`${questionSelector} [data-cy="questionAnsweredCorrectly"]`).should('exist')
                cy.get(`${questionSelector} [data-cy="questionAnsweredWrong"]`).should('not.exist')
            } else {
                cy.get(`${questionSelector} [data-cy="questionAnsweredWrong"]`).should('exist')
                cy.get(`${questionSelector} [data-cy="questionAnsweredCorrectly"]`).should('not.exist')
            }

            question.answers.forEach((a) => {
                const answerSelector = `[data-cy="answer_${a.num}"]`
                if (a.selected) {
                    cy.get(`${questionSelector} ${answerSelector} [data-cy="selected_true"]`).should('exist')
                    cy.get(`${questionSelector} ${answerSelector} [data-cy="selected_false"]`).should('not.exist')
                } else {
                    cy.get(`${questionSelector} ${answerSelector} [data-cy="selected_false"]`).should('exist')
                    cy.get(`${questionSelector} ${answerSelector} [data-cy="selected_true"]`).should('not.exist')
                }

                if (a.wrongSelection) {
                    cy.get(`${questionSelector} ${answerSelector} [data-cy="wrongSelection"]`).should('exist')
                } else {
                    cy.get(`${questionSelector} ${answerSelector} [data-cy="wrongSelection"]`).should('not.exist')
                }

                if (a.missedSelection) {
                    cy.get(`${questionSelector} ${answerSelector} [data-cy="missedSelection"]`).should('exist')
                } else {
                    cy.get(`${questionSelector} ${answerSelector} [data-cy="missedSelection"]`).should('not.exist')
                }
            });

        });
    });

    it('visit quiz without any questions', () => {
        cy.createQuizDef(1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizHasNoQuestions"]').contains('This Quiz has no questions declared')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="cancelQuizAttempt"]').should('not.exist')

        cy.get('[data-cy="closeQuizAttempt"]').click()
        cy.get('[data-cy="skillDescription-skill1"]')
    });

    it('run quiz', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.validateQuestionAnswer({
            num: 1,
            correct: true,
            answers: [{
                num: 1,
                selected: true,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 2,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 2,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }]
        });
        cy.validateQuestionAnswer({
            num: 2,
            correct: true,
            answers: [{
                num: 1,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 2,
                selected: true,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 3,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }]
        });
        cy.validateQuestionAnswer({
            num: 3,
            correct: true,
            answers: [{
                num: 1,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 2,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 3,
                selected: true,
                wrongSelection: false,
                missedSelection: false
            }]
        });

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
    });

    it('wrong answers are accurately depicted on the result screen', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.setQuizMaxNumAttempts(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()

        cy.get('[data-cy="quizFailed"]')

        cy.validateQuestionAnswer({
            num: 1,
            correct: false,
            answers: [{
                num: 1,
                selected: false,
                wrongSelection: false,
                missedSelection: true
            }, {
                num: 2,
                selected: true,
                wrongSelection: true,
                missedSelection: false
            }, {
                num: 3,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }]
        });
        cy.validateQuestionAnswer({
            num: 2,
            correct: false,
            answers: [{
                num: 1,
                selected: false,
                wrongSelection: false,
                missedSelection: true
            }, {
                num: 2,
                selected: true,
                wrongSelection: true,
                missedSelection: false
            }, {
                num: 3,
                selected: true,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 4,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }]
        });
    });

    it('quiz attempts been exhausted', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMaxNumAttempts(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForUser(1, Cypress.env('proxyUser'), [{selectedIndex: [1]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');

        cy.get('[data-cy="noMoreAttemptsAlert"]').contains('This quiz allows 1 maximum attempt.')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="quizDescription"]').should('not.exist')
        cy.get('[data-cy="closeQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '1 / 1')

        // has 1 attempt left
        cy.setQuizMaxNumAttempts(1, 2)
        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="noMoreAttemptsAlert"]').should('not.exist')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="quizDescription"]').should('exist')
        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '1 / 2')

        // unlimited
        cy.setQuizMaxNumAttempts(1, -1)
        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="noMoreAttemptsAlert"]').should('not.exist')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="quizDescription"]').should('exist')
        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '1 / Unlimited')
    });

    it('pass when MinNumQuestionsToPass is configured', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 2 / 3 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('66%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_1"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
        cy.get('[data-cy="percentCorrectInfoCard"] [data-cy="percentCorrect"]').should('have.text', '66%')
        cy.get('[data-cy="percentCorrectInfoCard"] [data-cy="percentToPass"]').should('have.text', '66%')
        cy.get('[data-cy="numCorrectInfoCard"] [data-cy="numCorrect"]').contains('2 out of 3')
        cy.get('[data-cy="numCorrectInfoCard"] [data-cy="subTitleMsg"]').contains('Well done')
    });

    it('fail when MinNumQuestionsToPass is configured', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 2 / 3 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('66%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_1"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizFailed"]')
        cy.get('[data-cy="quizCompletion"]').contains('Thank you for completing the Quiz')
        cy.get('[data-cy="percentCorrectInfoCard"] [data-cy="percentCorrect"]').should('have.text', '33%')
        cy.get('[data-cy="percentCorrectInfoCard"] [data-cy="percentToPass"]').should('have.text', '66%')
        cy.get('[data-cy="numCorrectInfoCard"] [data-cy="numCorrect"]').contains('1 out of 3')
        cy.get('[data-cy="numCorrectInfoCard"] [data-cy="subTitleMsg"]').contains('Missed by 2 questions')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="title"] .fa-infinity')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="subTitle"]').contains('Unlimited Attempts')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="subTitle"]').contains('1 attempt so far')
    });

    it('one attempt is used when max attempts is configured', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.setQuizMaxNumAttempts(1, 3)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 3 / 3 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('100%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_1"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizFailed"]')
        cy.get('[data-cy="quizCompletion"]').contains('Thank you for completing the Quiz')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="title"]').contains('2 More Attempts')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="subTitle"]').contains('Used 1 out of 3 attempts')

        cy.get('[data-cy="quizRunQuestions"]').should('not.exist')
    });

    it('two attempts are used when max attempts is configured', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMaxNumAttempts(1, 3)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForUser(1, Cypress.env('proxyUser'), [{selectedIndex: [1]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 1 / 1 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('100%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizFailed"]')
        cy.get('[data-cy="quizCompletion"]').contains('Thank you for completing the Quiz')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="title"]').contains('1 More Attempt')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="subTitle"]').contains('Used 2 out of 3 attempts')

        cy.get('[data-cy="quizRunQuestions"]').should('not.exist')
    });

    it('passed quiz cannot be attempted again', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMaxNumAttempts(1, 3)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForUser(1, Cypress.env('proxyUser'), [{selectedIndex: [0]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You already passed this quiz')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="cancelQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="quizPassInfo"]').should('not.exist')
        cy.get('[data-cy="closeQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="closeQuizAttemptInAlert"]').should('be.enabled')
        cy.get('[data-cy="closeQuizAttemptInAlert"]').contains('Close Quiz')

        // close in alert
        cy.get('[data-cy="closeQuizAttemptInAlert"]').click()
        cy.get('[data-cy="skillDescription-skill1"]')

        // close on the bottom
        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You already passed this quiz')
        cy.get('[data-cy="closeQuizAttempt"]').click()
        cy.get('[data-cy="skillDescription-skill1"]')
    });

    it('do no present retry button after failing last attempt', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.setQuizMaxNumAttempts(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 2 / 2 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('100%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizCompletion"]').contains('Thank you for completing the Quiz')
        cy.get('[data-cy="quizFailed"]')

        cy.get('[data-cy="quizCompletion"] [data-cy="runQuizAgainBtn"]').should('not.exist')
        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').should('be.enabled')
        cy.get('[data-cy="quizCompletion"]').contains('Would you like to try again?').should('not.exist')
        cy.get('[data-cy="numAttemptsInfoCard"]').contains('No More Attempts')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="subTitle"]').contains('Used 1 out of 1 attempts')

        cy.get('[data-cy="quizRunQuestions"]').contains('Would you like to try again?').should('not.exist')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="runQuizAgainBtn"]').should('not.exist')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="closeQuizBtn"]').should('be.enabled')
    });

    it('passed quiz with 1 attempt should only display the passed information', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMaxNumAttempts(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForUser(1, Cypress.env('proxyUser'), [{selectedIndex: [0]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You already passed this quiz')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="cancelQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="quizPassInfo"]').should('not.exist')
        cy.get('[data-cy="closeQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="closeQuizAttemptInAlert"]').should('be.enabled')
        cy.get('[data-cy="closeQuizAttemptInAlert"]').contains('Close Quiz')
        cy.get('[data-cy="noMoreAttemptsAlert"]').should('not.exist')
    })

    it('run quiz with subset of questions', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizQuestionDef(1, 4);
        cy.createQuizQuestionDef(1, 5);

        cy.setNumQuestionsForQuiz(1, 2);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"]').should('exist');
        cy.get('[data-cy="question_2"]').should('exist');
        cy.get('[data-cy="question_3"]').should('not.exist');

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.validateQuestionAnswer({
            num: 1,
            correct: true,
            answers: [{
                num: 1,
                selected: true,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 2,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 2,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }]
        });
        cy.validateQuestionAnswer({
            num: 2,
            correct: true,
            answers: [{
                num: 1,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 2,
                selected: true,
                wrongSelection: false,
                missedSelection: false
            }, {
                num: 3,
                selected: false,
                wrongSelection: false,
                missedSelection: false
            }]
        });

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains( '150')
    });

    it('subset of randomized questions is consistent on refresh', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizQuestionDef(1, 4);
        cy.createQuizQuestionDef(1, 5);

        cy.setNumQuestionsForQuiz(1, 2);
        cy.setRandomizedQuestions(1, true);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"]').should('exist');
        cy.get('[data-cy="question_2"]').should('exist');
        cy.get('[data-cy="question_3"]').should('not.exist');

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').should('have.class', 'selected-answer')
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').should('have.class', 'selected-answer')

    });

    it('quiz redirects to failure page when time runs out', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.setQuizTimeLimit(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.wait(4000);

        cy.get('[data-cy="completionSummaryTitle"]').contains("You've run out of time!")

    });

    it('quiz redirects to failure page when returning to a quiz that has run out of time', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.setQuizTimeLimit(1, 5);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.wait(5000);

        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();
        cy.get('[data-cy="completionSummaryTitle"]').contains("You've run out of time!")
    });

    it('can refresh in the middle of a timed quiz', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.setQuizTimeLimit(1, 300);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.wait(1000);
        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="completionSummaryTitle"]').should('not.exist');
    });
});


