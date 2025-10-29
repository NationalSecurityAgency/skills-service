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

describe('Client Display Quiz Matching Question Tests', () => {

    beforeEach(() => {
        Cypress.Commands.add("dragAndDropAnswerMatch", (qNum, availableStr, toIndex) => {

            const destSelector = `[data-cy="question_${qNum}"] [data-cy="matchedList"] [data-cy="matchedNum-${toIndex}"]`

            return cy.get(`[data-cy="question_${qNum}"] [data-cy^="bank-"]`).contains(availableStr).then((bankItem) => {
                const dragHandle = bankItem[0].getAttribute('data-cy')
                cy.get(`[data-cy="question_${qNum}"] [data-cy="availableItems"] [data-cy="${dragHandle}"]`)
                    .dragAndDrop(destSelector)
                    .then(() => {
                        cy.get(`[data-cy="question_${qNum}"] [data-cy="matchedList"]`).contains(availableStr);
                    })

            })
        });

        Cypress.Commands.add("rearangeAnswerMatchViaDragAndDrop", (qNum, strValueToMove, destValue, isDestValueASelector = false) => {
            const getLocSelector = (dataCyVal) => {
                return `[data-cy="question_${qNum}"] [data-cy="matchedList"] [data-cy="${dataCyVal}"]`
            }

            const selectorToSearch = `[data-cy="question_${qNum}"] [data-cy="matchedList"] [data-cy^="available-"]`
            return cy.get(selectorToSearch).contains(strValueToMove).then((itemToMove) => {
                const dragHandle = itemToMove[0].parentElement.getAttribute('data-cy')
                cy.log(`dragHandle = ${dragHandle}`)

                if (isDestValueASelector) {
                    return cy.get(getLocSelector(dragHandle)).dragAndDrop(destValue)
                }
                return cy.get(selectorToSearch).contains(destValue).then((moveToItem) => {
                    const destLocation = moveToItem[0].parentElement.getAttribute('data-cy')
                    return cy.get(getLocSelector(dragHandle)).dragAndDrop(getLocSelector(destLocation))
                })
            })
        });

        Cypress.Commands.add("validateMatchedSelection", (qNum, movedToMatches) => {
            const allItems = ['First Answer', 'Second Answer', 'Third Answer']
            const expectedAvailable = allItems.filter(item => !movedToMatches.includes(item));

            movedToMatches.forEach(item => {
                cy.get(`[data-cy="question_${qNum}"] [data-cy="matchedList"]`).should('contain', item);
                cy.get(`[data-cy="question_${qNum}"] [data-cy="availableItems"]`).should('not.contain', item);
            });

            expectedAvailable.forEach(item => {
                cy.get(`[data-cy="question_${qNum}"] [data-cy="availableItems"]`).should('contain', item);
                cy.get(`[data-cy="question_${qNum}"] [data-cy="matchedList"]`).should('not.contain', item);
            });
        });
    });

    it('pass quiz with matching questions', () => {
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

        cy.dragAndDropAnswerMatch(1, 'First Answer', 0)
        cy.dragAndDropAnswerMatch(1, 'Second Answer', 1)
        cy.dragAndDropAnswerMatch(1, 'Third Answer', 2)

        cy.dragAndDropAnswerMatch(2, 'First Answer', 0)
        cy.dragAndDropAnswerMatch(2, 'Second Answer', 1)
        cy.dragAndDropAnswerMatch(2, 'Third Answer', 2)

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
        cy.get('[data-cy="quizRunQuestions"]').should('not.exist')
        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
    });

    it('fail quiz with matching questions', () => {
        cy.createQuizDef(1);
        cy.createQuizMatchingQuestionDef(1, 1);
        cy.createQuizMatchingQuestionDef(1, 2);
        cy.setQuizShowCorrectAnswers(1, true)

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

        cy.dragAndDropAnswerMatch(1, 'First Answer', 0)
        cy.dragAndDropAnswerMatch(1, 'Second Answer', 1)
        cy.dragAndDropAnswerMatch(1, 'Third Answer', 2)

        cy.dragAndDropAnswerMatch(2, 'First Answer', 2)
        cy.dragAndDropAnswerMatch(2, 'Second Answer', 1)
        cy.dragAndDropAnswerMatch(2, 'Third Answer', 0)

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"] [data-cy="quizFailed"]')
        cy.get('[data-cy="numCorrect"]').contains('1 out of 2')

        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_1"] [data-cy="questionAnsweredCorrectly"]')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_1"] [data-cy="matchedNum-0"] [data-cy="matchIsCorrect"]')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_1"] [data-cy="matchedNum-1"] [data-cy="matchIsCorrect"]')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_1"] [data-cy="matchedNum-2"] [data-cy="matchIsCorrect"]')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_1"] [data-cy="matchedNum-3"]').should('not.exist')

        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_2"] [data-cy="questionAnsweredWrong"]')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_2"] [data-cy="matchedNum-0"] [data-cy="matchIsWrong"]')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_2"] [data-cy="matchedNum-1"] [data-cy="matchIsCorrect"]')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_2"] [data-cy="matchedNum-2"] [data-cy="matchIsWrong"]')
        cy.get('[data-cy="quizRunQuestions"] [data-cy="question_2"] [data-cy="matchedNum-3"]').should('not.exist')
    });

    it('pass quiz by placing matching in wrong spots then re-arranging them', () => {
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

        cy.dragAndDropAnswerMatch(1, 'First Answer', 2)
        cy.dragAndDropAnswerMatch(1, 'Second Answer', 0)
        cy.dragAndDropAnswerMatch(1, 'Third Answer', 1)

        cy.rearangeAnswerMatchViaDragAndDrop(1, 'First Answer', 'Second Answer')
        cy.rearangeAnswerMatchViaDragAndDrop(1, 'Second Answer', 'Third Answer')

        cy.dragAndDropAnswerMatch(2, 'First Answer', 2)
        cy.rearangeAnswerMatchViaDragAndDrop(2, 'First Answer',
            '[data-cy="question_2"] [data-cy="matchedList"] [data-cy="matchedNum-0"]', true)

        cy.dragAndDropAnswerMatch(2, 'Second Answer', 0) // 0 was switched with 2 in the previous step
        cy.rearangeAnswerMatchViaDragAndDrop(2, 'Second Answer',
            '[data-cy="question_2"] [data-cy="matchedList"] [data-cy="matchedNum-1"]', true)
        cy.dragAndDropAnswerMatch(2, 'Third Answer', 1) // 1 was switched inot the 3rd slot in the last step

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
        cy.get('[data-cy="quizRunQuestions"]').should('not.exist')
        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
    });

    it('matching progress saved and restored appropriately on reload', () => {
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

        cy.dragAndDropAnswerMatch(1, 'First Answer', 0)
        cy.dragAndDropAnswerMatch(1, 'Third Answer', 2)
        cy.dragAndDropAnswerMatch(2, 'Second Answer', 1)

        cy.validateMatchedSelection(1, ['First Answer', 'Third Answer'])

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click();

        cy.validateMatchedSelection(1, ['First Answer', 'Third Answer'])

        cy.dragAndDropAnswerMatch(1, 'Second Answer', 1)
        cy.dragAndDropAnswerMatch(2, 'First Answer', 0)
        cy.dragAndDropAnswerMatch(2, 'Third Answer', 2)

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
    });

    it('can not submit an incomplete matching question', () => {
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

        cy.dragAndDropAnswerMatch(1, 'First Answer', 0)
        cy.dragAndDropAnswerMatch(1, 'Third Answer', 2)
        cy.dragAndDropAnswerMatch(2, 'Second Answer', 1)

        cy.validateMatchedSelection(1, ['First Answer', 'Third Answer'])

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="questions[0].quizAnswersError"]').contains('All choices must be matched')
        cy.get('[data-cy="questions[1].quizAnswersError"]').contains('All choices must be matched')
        cy.get('[data-cy="questionErrors"]').contains('All choices must be matched')

        cy.dragAndDropAnswerMatch(1, 'Second Answer', 1)
        cy.get('[data-cy="questions[0].quizAnswersError"]').should('not.exist')
        cy.get('[data-cy="questions[1].quizAnswersError"]').contains('All choices must be matched')
        cy.get('[data-cy="questionErrors"]').contains('All choices must be matched')

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="questions[0].quizAnswersError"]').should('not.exist')
        cy.get('[data-cy="questions[1].quizAnswersError"]').contains('All choices must be matched')
        cy.get('[data-cy="questionErrors"]').contains('All choices must be matched')

        cy.dragAndDropAnswerMatch(2, 'First Answer', 0)
        cy.get('[data-cy="questions[0].quizAnswersError"]').should('not.exist')
        cy.get('[data-cy="questions[1].quizAnswersError"]').contains('All choices must be matched')
        cy.get('[data-cy="questionErrors"]').contains('All choices must be matched')

        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="questions[0].quizAnswersError"]').should('not.exist')
        cy.get('[data-cy="questions[1].quizAnswersError"]').contains('All choices must be matched')
        cy.get('[data-cy="questionErrors"]').contains('All choices must be matched')

        cy.dragAndDropAnswerMatch(2, 'Third Answer', 2)
        cy.get('[data-cy="questions[0].quizAnswersError"]').should('not.exist')
        cy.get('[data-cy="questions[1].quizAnswersError"]').should('not.exist')
        cy.get('[data-cy="questionErrors"]').should('not.exist')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').contains('150')
    });

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
            cy.get(`[data-cy="question_${qNum}"] [data-cy^="bank-"]`).contains(answerText).then((bankItem) => {
                // Store the answer text before moving
                const answerToMove = bankItem.text().trim();

                // Move the answer
                cy.wrap(bankItem).click().type('{leftArrow}');

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


