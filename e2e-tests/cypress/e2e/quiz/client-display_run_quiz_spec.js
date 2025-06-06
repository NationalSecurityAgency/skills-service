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
import moment from "moment-timezone";

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

        cy.clickCompleteQuizBtn()

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

    it('run quiz from subject page with expanded skills', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1');
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="takeQuizBtn"]').click();

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

        cy.clickCompleteQuizBtn()

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

    it('run quiz from a badge page', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.visit('/progress-and-rankings/projects/proj1/badges/badge1/skills/skill1');
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

        cy.clickCompleteQuizBtn()

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

    it('run quiz from badges page with expanded skills', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.visit('/progress-and-rankings/projects/proj1/badges/badge1');
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="takeQuizBtn"]').click();

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

        cy.clickCompleteQuizBtn()

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

    it('run quiz from a global badge page', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass);
            cy.createGlobalBadge(1)
            cy.assignSkillToGlobalBadge(1, 1, 1)
            cy.enableGlobalBadge(1)
            cy.logout();
            cy.login(vars.defaultUser, vars.defaultPass);
        })

        cy.visit('/progress-and-rankings/projects/proj1/badges/global/globalBadge1/skills/skill1');
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

        cy.clickCompleteQuizBtn()

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

    it('run quiz from a global badge page with expanded skills', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass);
            cy.createGlobalBadge(1)
            cy.assignSkillToGlobalBadge(1, 1, 1)
            cy.enableGlobalBadge(1)
            cy.logout();
            cy.login(vars.defaultUser, vars.defaultPass);
        })

        cy.visit('/progress-and-rankings/projects/proj1/badges/global/globalBadge1');
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="takeQuizBtn"]').click();

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

        cy.clickCompleteQuizBtn()

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

        cy.clickCompleteQuizBtn()

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

        cy.clickCompleteQuizBtn()
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

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizFailed"]')
        cy.get('[data-cy="percentCorrectInfoCard"] [data-cy="percentCorrect"]').should('have.text', '33%')
        cy.get('[data-cy="percentCorrectInfoCard"] [data-cy="percentToPass"]').should('have.text', '66%')
        cy.get('[data-cy="numCorrectInfoCard"] [data-cy="numCorrect"]').contains('1 out of 3')
        cy.get('[data-cy="numCorrectInfoCard"] [data-cy="subTitleMsg"]').contains('Missed by 1 question')
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

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizFailed"]')
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

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizFailed"]')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="title"]').contains('1 More Attempt')
        cy.get('[data-cy="numAttemptsInfoCard"] [data-cy="subTitle"]').contains('Used 2 out of 3 attempts')

        cy.get('[data-cy="quizRunQuestions"]').should('not.exist')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Failed')
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .find('[data-cy="myQuizAttemptsLink"]')
            .should('exist')
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(1)
            .should('contain.text', 'Failed')
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

        cy.clickCompleteQuizBtn()
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
        const overrideProps = {
            answers: [{
                answer: 'Answer 1',
                isCorrect: true,
            }, {
                answer: 'Answer 2',
                isCorrect: false,
            }]
        }
        cy.createQuizQuestionDef(1, 1, overrideProps);
        cy.createQuizQuestionDef(1, 2, overrideProps);
        cy.createQuizQuestionDef(1, 3, overrideProps);
        cy.createQuizQuestionDef(1, 4, overrideProps);
        cy.createQuizQuestionDef(1, 5, overrideProps);

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
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()

        cy.clickCompleteQuizBtn()

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
            }]
        });
        cy.validateQuestionAnswer({
            num: 2,
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

        cy.get('[data-cy="question_1"] [data-cy="answer_1"] [data-cy="selected_true"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_2"] [data-cy="selected_true"]').should('not.exist')
        cy.get('[data-cy="question_1"] [data-cy="answer_3"] [data-cy="selected_true"]').should('not.exist')

        cy.get('[data-cy="question_2"] [data-cy="answer_1"] [data-cy="selected_true"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="answer_2"] [data-cy="selected_true"]')
        cy.get('[data-cy="question_2"] [data-cy="answer_3"] [data-cy="selected_true"]').should('not.exist')
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
        cy.get('[data-cy="percentCorrectInfoCard"]').contains('5 seconds')
        cy.get('[data-cy="outOfTimeMsg"]')
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

    it('quiz allows multiple attempts if enabled', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMultipleTakes(1, false);

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });

        cy.runQuizForUser(1, Cypress.env('proxyUser'), [{selectedIndex: [1]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')

        cy.setQuizMultipleTakes(1, true);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').should('exist')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Passed')
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .find('[data-cy="myQuizAttemptsLink"]')
            .should('not.exist')
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(1)
            .should('contain.text', 'Passed')
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(2)
            .should('contain.text', 'Failed')
    });

    it('quiz attached to skill expiring in a day can be retaken', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMultipleTakes(1, false);

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });

        cy.configureExpiration(1, 0, 1, 'DAILY');

        cy.runQuizForUser(1, Cypress.env('proxyUser'), [{selectedIndex: [1]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="takeQuizBtn"]').click()

        cy.get('[data-cy="startQuizAttempt"]').should('exist')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
    });

    it('quiz attached to skill expiring in more than a day can not be retaken', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMultipleTakes(1, false);

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });

        cy.configureExpiration(1, 0, 3, 'DAILY');

        cy.runQuizForUser(1, Cypress.env('proxyUser'), [{selectedIndex: [1]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="takeQuizBtn"]').click()

        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')

    });

    it('shows wrong answers if setting is enabled', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_3"]').click()

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizFailed"]')
        cy.get('[data-cy="quizRunQuestions"]').should('not.exist');

        cy.setQuizShowCorrectAnswers(1, true);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_3"]').click()

        cy.clickCompleteQuizBtn()

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

    it('visit quiz connected to skill with insufficient project points', () => {
        cy.createQuizDef(1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '1', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="cantStartQuiz"]').contains('This Quiz is assigned to a Skill (skill1) that does not have enough points to be completed. The Project (proj1) that contains this skill must have at least 100 points.')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="cancelQuizAttempt"]').should('not.exist')

        cy.get('[data-cy="closeQuizAttempt"]').click()
        cy.get('[data-cy="skillDescription-skill1"]')
    });

    it('visit quiz connected to skill with insufficient subject points', () => {
        cy.createQuizDef(1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSubject(1,2)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '1', numPerformToCompletion: 1 });
        cy.createSkill(1, 2, 1, { pointIncrement: '200', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="cantStartQuiz"]').contains('This Quiz is assigned to a Skill (skill1) that does not have enough points to be completed. The Subject (subj1) that contains this skill must have at least 100 points.')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="cancelQuizAttempt"]').should('not.exist')

        cy.get('[data-cy="closeQuizAttempt"]').click()
        cy.get('[data-cy="skillDescription-skill1"]')
    });

    it('show timeline for single failed quiz attempt', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });

        cy.runQuizForUser(1, Cypress.env('proxyUser'), [{selectedIndex: [1]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Failed')
    });

    it('do not show timeline for single passed quiz attempt', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMultipleTakes(1, false);

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="approvalHistoryTimeline"]')
            .should('not.exist')
    });

    it('Take quiz multiple times when limiting to only failed questions is configured', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.setLimitToIncorrectQuestions(1, true)

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

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="runQuizAgainBtn"]').click()

        cy.get('[data-cy="onlyIncorrectMessage"]').should('exist')
        cy.get('[data-cy="onlyIncorrectMessage"]').contains('You only need to retake the questions you did not answer correctly on your last attempt. You\'ve already answered 2 correctly, so you need to answer 1 question(s) to pass.')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"]').should('exist');
        cy.get('[data-cy="question_2"]').should('not.exist');
        cy.get('[data-cy="question_3"]').should('not.exist');

        cy.get('[data-cy="question_1"]').contains('This is a question # 3')

        cy.get('[data-cy="question_1"] [data-cy="answer_3"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
    });

    it('Take quiz multiple times when limiting to only failed questions is configured with subset of questions', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizQuestionDef(1, 4);
        cy.setLimitToIncorrectQuestions(1, true)
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 2 / 4 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('50%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_4"] [data-cy="answer_2"]').click()

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="runQuizAgainBtn"]').click()

        cy.get('[data-cy="onlyIncorrectMessage"]').should('exist')
        cy.get('[data-cy="onlyIncorrectMessage"]').contains('You only need to retake the questions you did not answer correctly on your last attempt. You\'ve already answered 1 correctly, so you need to answer 1 question(s) to pass.')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"]').should('exist');
        cy.get('[data-cy="question_2"]').should('exist');
        cy.get('[data-cy="question_3"]').should('exist');
        cy.get('[data-cy="question_4"]').should('not.exist');

        cy.get('[data-cy="question_1"]').contains('This is a question # 2')

        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
    });

    it('Retake message is different when quiz has already been passed', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.setLimitToIncorrectQuestions(1, true)
        cy.setQuizMultipleTakes(1, true)


        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 3 / 3 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('100%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');

        cy.get('[data-cy="onlyIncorrectMessage"]').should('exist')
        cy.get('[data-cy="onlyIncorrectMessage"]').contains('You only need to retake the questions you did not answer correctly on your last attempt. You need to answer 3 question(s) to pass.')

    });

    it('Take quiz multiple times when limiting answer hints to only be displayed on retakes', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizQuestionDef(1, 4);
        cy.setMinNumQuestionsToPass(1, 2)

        cy.request('POST', `/admin/quiz-definitions/quiz1/settings`, [{
            setting: 'quizShowAnswerHintsOnRetakeAttemptsOnly',
            value: 'true'
        }]);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 2 / 4 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('50%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="answerHint"]').should('not.exist')

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_4"] [data-cy="answer_2"]').click()

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="runQuizAgainBtn"]').click()

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="answerHint"]').should('exist')

        cy.get('[data-cy="question_1"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 1');
        cy.get('[data-cy="question_2"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 2');
        cy.get('[data-cy="question_3"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 3');
        cy.get('[data-cy="question_4"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 4');
    });

    it('Answer hints are always displayed by default', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizQuestionDef(1, 4);
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 2 / 4 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('50%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="answerHint"]').should('exist')

        cy.get('[data-cy="question_1"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 1');
        cy.get('[data-cy="question_2"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 2');
        cy.get('[data-cy="question_3"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 3');
        cy.get('[data-cy="question_4"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 4');
    });

    it('Answer hints are always displayed when retakes settings is not true', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizQuestionDef(1, 4);
        cy.setMinNumQuestionsToPass(1, 2)

        cy.request('POST', `/admin/quiz-definitions/quiz1/settings`, [{
            setting: 'quizShowAnswerHintsOnRetakeAttemptsOnly',
            value: 'false'
        }]);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 2 / 4 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('50%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="answerHint"]').should('exist')

        cy.get('[data-cy="question_1"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 1');
        cy.get('[data-cy="question_2"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 2');
        cy.get('[data-cy="question_3"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 3');
        cy.get('[data-cy="question_4"] [data-cy="answerHintMsgContent"]').contains('This is a hint for question # 4');
    });

    it('Answer hints honor new lines', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, { answerHint: 'This is a hint for line #1\nThis is a hint for line #2\nThis is a hint for line #3' });

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="answerHint"]').should('exist')

        cy.get('[data-cy="answerHintMsgContent"]')
          .should('contains.html', 'This is a hint for line #1\nThis is a hint for line #2\nThis is a hint for line #3')
    });

    it('Answer hints are not displayed when null or empty string', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, {  answerHint: '' });
        cy.createQuizQuestionDef(1, 2, {  answerHint: '' });
        cy.createQuizQuestionDef(1, 3, {  answerHint: null });
        cy.createQuizQuestionDef(1, 4, {  answerHint: null });
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('Must get 2 / 4 questions')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizPassInfo"]').contains('50%')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="answerHint"]').should('not.exist')

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_4"] [data-cy="answer_2"]').click()

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="runQuizAgainBtn"]').click()

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="answerHint"]').should('not.exist')
    });

    it('run quiz with a video', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, {}, { videoUrl: '/static/videos/create-quiz.mp4', captions: 'some', transcript: 'another' });

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

        cy.get('[data-cy="question_1"] [data-cy="videoPlayer"]').should('exist')
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

    });
});


