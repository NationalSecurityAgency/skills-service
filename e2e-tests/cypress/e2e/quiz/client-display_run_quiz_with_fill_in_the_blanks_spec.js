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

describe('Skills Display Run Quizzes With Fill In the Blank Questions', () => {

    beforeEach(() => {
    });

    it('run quiz with 1 fill in the blank', () => {
        cy.createQuizDef(1);
        cy.createFillInTheBlankQuestionDef(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizMsg"]')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[0]"]').type('Question 1 - First Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[1]"]').type('Question 1 - Second Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[2]"]').type('Question 1 - Third Answer')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

    });

    it('run quiz with multiple questions where 1 is fill in the blank', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createFillInTheBlankQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()

        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[0]"]').type('Question 2 - First Answer')
        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[1]"]').type('Question 2 - Second Answer')
        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[2]"]').type('Question 2 - Third Answer')

        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
    });

    it('run quiz with multiple questions and multiple fill in the blank questions', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createFillInTheBlankQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)
        cy.createFillInTheBlankQuestionDef(1, 4)
        cy.createFillInTheBlankQuestionDef(1, 5)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '5')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()

        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[0]"]').type('Question 2 - First Answer')
        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[1]"]').type('Question 2 - Second Answer')
        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[2]"]').type('Question 2 - Third Answer')

        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="question_4"] [data-cy="questions[3].answerTextArray[0]"]').type('Question 4 - First Answer')
        cy.get('[data-cy="question_4"] [data-cy="questions[3].answerTextArray[1]"]').type('Question 4 - Second Answer')
        cy.get('[data-cy="question_4"] [data-cy="questions[3].answerTextArray[2]"]').type('Question 4 - Third Answer')

        cy.get('[data-cy="question_5"] [data-cy="questions[4].answerTextArray[0]"]').type('Question 5 - First Answer')
        cy.get('[data-cy="question_5"] [data-cy="questions[4].answerTextArray[1]"]').type('Question 5 - Second Answer')
        cy.get('[data-cy="question_5"] [data-cy="questions[4].answerTextArray[2]"]').type('Question 5 - Third Answer')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
    });

    it('fill in the blank trims leading and trailing spaces when grading', () => {
        cy.createQuizDef(1);
        cy.createFillInTheBlankQuestionDef(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizMsg"]')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[0]"]').type('       Question 1 - First Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[1]"]').type('Question 1 - Second Answer       ')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[2]"]').type('Question 1 - Third Answer')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

    });

    it('fill in the blank is not case sensitive', () => {
        cy.createQuizDef(1);
        cy.createFillInTheBlankQuestionDef(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizMsg"]')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[0]"]').type('QUESTION 1 - First Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[1]"]').type('Question 1 - SECOND Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[2]"]').type('QUESTION 1 - Third ANSWER')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

    });

    it('answers are not reported after quiz is completed', () => {
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*', cy.spy().as('reportAnswer'))
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/complete', cy.spy().as('completeQuiz'))
        cy.createQuizDef(1);
        cy.createFillInTheBlankQuestionDef(1, 1, {
            answers: [{
                answer: `Only Answer`,
                isCorrect: false,
            }]
        })

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizMsg"]')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[0]"]').type('Only Answer')

        cy.clickCompleteQuizBtn()
        cy.get('@completeQuiz').should('have.been.called');
        cy.get('@reportAnswer').should('have.been.calledBefore', '@completeQuiz');

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

    });
});


