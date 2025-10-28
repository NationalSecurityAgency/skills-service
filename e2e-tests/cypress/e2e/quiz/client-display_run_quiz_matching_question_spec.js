/*
 * Copyright 2025 SkillTree
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

    // cy.get('[data-cy="question_2"] [data-cy="matchedNum-0"] [data-cy="matchedAnswer"]').contains('First Answer')
    //
    // it('matching progress saved and restored appropriately on reload', () => {
    //     cy.createQuizDef(1);
    //     cy.createQuizMatchingQuestionDef(1, 1);
    //     cy.createQuizMatchingQuestionDef(1, 2);
    //
    //     cy.createProject(1)
    //     cy.createSubject(1,1)
    //     cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
    //
    //     cy.cdVisit('/subjects/subj1/skills/skill1');
    //     cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
    //     cy.get('[data-cy="takeQuizBtn"]').click();
    //
    //     cy.get('[data-cy="title"]').contains('Quiz')
    //     cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
    //
    //     cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
    //     cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
    //
    //     cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')
    //
    //     cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
    //     cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
    //
    //     cy.get('[data-cy="startQuizAttempt"]').click()
    //
    //     cy.get('[data-cy="bank-1"]').contains('First Answer').click().type('{leftArrow}')
    //     cy.get('[data-cy="bank-1"]').contains('Second Answer').click().type('{leftArrow}')
    //     cy.get('[data-cy="bank-1"]').contains('Third Answer').click().type('{leftArrow}')
    //
    //     cy.get('[data-cy="bank-2"]').contains('First Answer').click().type('{leftArrow}')
    //     cy.get('[data-cy="bank-2"]').contains('Second Answer').click().type('{leftArrow}')
    //     cy.get('[data-cy="bank-2"]').contains('Third Answer').click().type('{leftArrow}')
    //
    //     cy.get('[data-cy="answers-1-0"]').contains('First Answer')
    //     cy.get('[data-cy="answers-1-1"]').contains('Second Answer')
    //     cy.get('[data-cy="answers-1-2"]').contains('Third Answer')
    //     cy.get('[data-cy="answers-2-0"]').contains('First Answer')
    //     cy.get('[data-cy="answers-2-1"]').contains('Second Answer')
    //     cy.get('[data-cy="answers-2-2"]').contains('Third Answer')
    //
    //     cy.cdVisit('/subjects/subj1/skills/skill1');
    //     cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
    //     cy.get('[data-cy="takeQuizBtn"]').click();
    //
    //     cy.get('[data-cy="answers-1-0"]').contains('First Answer')
    //     cy.get('[data-cy="answers-1-1"]').contains('Second Answer')
    //     cy.get('[data-cy="answers-1-2"]').contains('Third Answer')
    //     cy.get('[data-cy="answers-2-0"]').contains('First Answer')
    //     cy.get('[data-cy="answers-2-1"]').contains('Second Answer')
    //     cy.get('[data-cy="answers-2-2"]').contains('Third Answer')
    //
    //     cy.clickCompleteQuizBtn()
    //
    //     cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
    //
    //     cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
    //     cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
    //     cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    //     cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
    // });
    //
    // it('can not submit an incomplete matching question', () => {
    //     cy.createQuizDef(1);
    //     cy.createQuizMatchingQuestionDef(1, 1);
    //     cy.createQuizMatchingQuestionDef(1, 2);
    //
    //     cy.createProject(1)
    //     cy.createSubject(1,1)
    //     cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
    //
    //     cy.cdVisit('/subjects/subj1/skills/skill1');
    //     cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
    //     cy.get('[data-cy="takeQuizBtn"]').click();
    //
    //     cy.get('[data-cy="title"]').contains('Quiz')
    //     cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
    //
    //     cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
    //     cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
    //
    //     cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')
    //
    //     cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
    //     cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')
    //
    //     cy.get('[data-cy="startQuizAttempt"]').click()
    //
    //     cy.get('[data-cy="bank-1"]').contains('First Answer').click().type('{leftArrow}')
    //     cy.get('[data-cy="bank-1"]').contains('Second Answer').click().type('{leftArrow}')
    //
    //     cy.get('[data-cy="bank-2"]').contains('First Answer').click().type('{leftArrow}')
    //     cy.get('[data-cy="bank-2"]').contains('Second Answer').click().type('{leftArrow}')
    //
    //     cy.get('[data-cy="answers-1-0"]').contains('First Answer')
    //     cy.get('[data-cy="answers-1-1"]').contains('Second Answer')
    //     cy.get('[data-cy="answers-2-0"]').contains('First Answer')
    //     cy.get('[data-cy="answers-2-1"]').contains('Second Answer')
    //
    //     cy.clickCompleteQuizBtn()
    //
    //     cy.get('[data-cy="questions[0].quizAnswersError"]').contains('All choices must be matched')
    //     cy.get('[data-cy="questions[1].quizAnswersError"]').contains('All choices must be matched')
    //     cy.get('[data-cy="questionErrors"]').contains('All choices must be matched')
    //
    //     cy.get('[data-cy="bank-1"]').contains('Third Answer').click().type('{leftArrow}')
    //     cy.get('[data-cy="answers-1-2"]').contains('Third Answer')
    //     cy.clickCompleteQuizBtn()
    //
    //     cy.get('[data-cy="questions[0].quizAnswersError"]').should('not.exist')
    //     cy.get('[data-cy="questions[1].quizAnswersError"]').contains('All choices must be matched')
    //     cy.get('[data-cy="questionErrors"]').contains('All choices must be matched')
    //
    //     cy.get('[data-cy="bank-2"]').contains('Third Answer').click().type('{leftArrow}')
    //     cy.get('[data-cy="answers-2-2"]').contains('Third Answer')
    //
    //     cy.clickCompleteQuizBtn()
    //
    //     cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
    //
    //     cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
    //     cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
    //     cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    //     cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
    //
    // });

    it('run quiz with matching questions using keyboard only', () => {
        cy.createQuizDef(1);
        cy.createQuizMatchingQuestionDef(1, 1);
        cy.createQuizMatchingQuestionDef(1, 2);

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

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()


        const moveAndVerifyAnswer = (qNum, answerText) => {
            cy.get(`[data-cy="question_${qNum}"] [data-cy^="bank-"]`).contains(answerText).then(($bankItem) => {
                // Store the answer text before moving
                const answerToMove = $bankItem.text().trim();

                // Move the answer
                cy.wrap($bankItem).click().type('{leftArrow}');

                // Verify it's no longer in the bank
                cy.get(`[data-cy="question_${qNum}"] [data-cy="availableItems"]`).should('not.contain', answerToMove);

                // Verify it appears in the matched section
                cy.get(`[data-cy="question_${qNum}"] [data-cy="matchedList"]`).contains(answerToMove).should('exist');
            });
        };

        moveAndVerifyAnswer(1, 'Second Answer');
        moveAndVerifyAnswer(1, 'First Answer');
        cy.get('[data-cy="question_1"] [data-cy="allAnswersPlaced"]').should('not.exist')
        moveAndVerifyAnswer(1, 'Third Answer');
        cy.get('[data-cy="question_1"] [data-cy="allAnswersPlaced"]').should('exist')

        moveAndVerifyAnswer(2, 'Second Answer');
        moveAndVerifyAnswer(2, 'First Answer');
        cy.get('[data-cy="question_2"] [data-cy="allAnswersPlaced"]').should('not.exist')
        moveAndVerifyAnswer(2, 'Third Answer');
        cy.get('[data-cy="question_2"] [data-cy="allAnswersPlaced"]').should('exist')

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]')
    });
});


