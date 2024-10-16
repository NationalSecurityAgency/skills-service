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

describe('Run Quizzes With Text Input Questions', () => {

    beforeEach(() => {
    });

    it('run quiz with 1 text input', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

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
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer # 1')

        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled').click()

        cy.get('[data-cy="requiresManualGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').should('be.enabled').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should( 'have.text', '0 Total')
        cy.get('[data-cy="quizRequiresGradingMsg"]')
        cy.get('[data-cy="takeQuizMsg"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
    });

    it('run quiz with multiple questions where 1 is text input', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
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
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer # 1')
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled').click()

        cy.get('[data-cy="requiresManualGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').should('be.enabled').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should( 'have.text', '0 Total')
        cy.get('[data-cy="quizRequiresGradingMsg"]')
        cy.get('[data-cy="takeQuizMsg"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
    });

    it('show input text needs grading when quizAlwaysShowCorrectAnswers property is enabled', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)
        cy.setQuizShowCorrectAnswers(1, true);

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
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer # 1')
        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()

        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled').click()

        cy.get('[data-cy="requiresManualGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="question_1"] [data-cy="questionAnsweredCorrectly"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_1"] [data-cy="selected_true"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_2"] [data-cy="selected_false"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_3"] [data-cy="selected_false"]')

        cy.get('[data-cy="question_1"] [data-cy="needsGradingTag"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="needsGradingTag"]')
        cy.get('[data-cy="question_2"] [data-cy="questionAnsweredCorrectly"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="questionAnsweredWrong"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="questionsText"]').contains('This is a question # 2')
        cy.get('[data-cy="question_2"] [data-cy="textInputAnswer"]').contains('Answer # 1')
        cy.get('[data-cy="question_3"] [data-cy="needsGradingTag"]').should('not.exist')


        cy.get('[data-cy="question_3"] [data-cy="questionAnsweredWrong"]')
        cy.get('[data-cy="question_3"] [data-cy="answer_1"] [data-cy="selected_false"]')
        cy.get('[data-cy="question_3"] [data-cy="answer_2"] [data-cy="selected_true"]')
        cy.get('[data-cy="question_3"] [data-cy="answer_3"] [data-cy="selected_false"]')
        // errors are rendered async
        cy.wait(2000)
        cy.get('[data-cy="questionErrors"]').should('not.exist')

        cy.get('[data-cy="quizRunQuestions"] [data-cy="closeQuizBtn"]').should('be.enabled').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should( 'have.text', '0 Total')
        cy.get('[data-cy="quizRequiresGradingMsg"]')
        cy.get('[data-cy="takeQuizMsg"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
    });

});


