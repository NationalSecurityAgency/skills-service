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
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', 'Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_1"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the test.')

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
            num: 3,
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

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]').should('have.text', '150')
    });

    it('quiz attempts been exhausted', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.setQuizMaxNumAttempts(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForUser(1, 'user0', [{selectedIndex: [1]}]);

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');

        cy.get('[data-cy="noMoreAttemptsAlert"]').contains('This quiz allows 1 maximum attempt.')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="quizDescription"]').should('not.exist')
        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
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


    it('run survey', () => {
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
});


