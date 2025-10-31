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

describe('Client Display Accessibility Quiz Tests', () => {

    beforeEach(() => {
    });

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {

        it(`quiz splash screen${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1);
            cy.createQuizQuestionDef(1, 1);
            cy.createQuizMultipleChoiceQuestionDef(1, 2);
            cy.createQuizQuestionDef(1, 3);

            cy.setQuizMaxNumAttempts(1, 1)
            cy.setMinNumQuestionsToPass(1, 2)

            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1, {
                selfReportingType: 'Quiz',
                quizId: 'quiz1',
                pointIncrement: '150',
                numPerformToCompletion: 1
            });

            cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
            cy.get('[data-cy="title"]').contains('Quiz')
            cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`quiz with questions screen${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1);
            cy.setQuizShowCorrectAnswers(1, true)
            cy.createQuizQuestionDef(1, 1);
            cy.createQuizMultipleChoiceQuestionDef(1, 2);
            cy.createQuizQuestionDef(1, 3);
            cy.createQuizMultipleChoiceQuestionDef(1, 4);
            cy.createQuizMultipleChoiceQuestionDef(1, 5);
            cy.createTextInputQuestionDef(1, 6)
            cy.createQuizMatchingQuestionDef(1, 7);
            cy.createQuizMatchingQuestionDef(1, 8);

            cy.setQuizMaxNumAttempts(1, 3)
            cy.setMinNumQuestionsToPass(1, 2)

            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1, {
                selfReportingType: 'Quiz',
                quizId: 'quiz1',
                pointIncrement: '150',
                numPerformToCompletion: 1
            });

            cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
            cy.get('[data-cy="title"]').contains('Quiz')

            cy.get('[data-cy="startQuizAttempt"]').click()
            cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
            cy.get('[data-cy="question_5"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_5"] [data-cy="answer_2"]').click()
            cy.get('[data-cy="question_5"] [data-cy="answer_3"]').click()
            cy.get('[data-cy="question_5"] [data-cy="answer_4"]').click()

            cy.get('[data-cy="question_7"] [data-cy="bank-0"]').dragAndDrop('[data-cy="question_7"] [data-cy="matchedList"] [data-cy="matchedNum-0"]')
            cy.get('[data-cy="question_8"] [data-cy="bank-0"]').dragAndDrop('[data-cy="question_8"] [data-cy="matchedList"] [data-cy="matchedNum-0"]')
            cy.get('[data-cy="question_8"] [data-cy="bank-1"]').dragAndDrop('[data-cy="question_8"] [data-cy="matchedList"] [data-cy="matchedNum-1"]')
            cy.get('[data-cy="question_8"] [data-cy="bank-2"]').dragAndDrop('[data-cy="question_8"] [data-cy="matchedList"] [data-cy="matchedNum-2"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`quiz passed screen${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1);
            cy.createQuizQuestionDef(1, 1);
            cy.createQuizMultipleChoiceQuestionDef(1, 2);
            cy.createQuizQuestionDef(1, 3);
            cy.createQuizMultipleChoiceQuestionDef(1, 4);

            cy.setQuizMaxNumAttempts(1, 3)
            cy.setMinNumQuestionsToPass(1, 2)

            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1, {
                selfReportingType: 'Quiz',
                quizId: 'quiz1',
                pointIncrement: '150',
                numPerformToCompletion: 1
            });

            cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
            cy.get('[data-cy="title"]').contains('Quiz')

            cy.get('[data-cy="startQuizAttempt"]').click()
            cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_2"] [data-cy="answer_3"]').click()

            cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()
            cy.get('[data-cy="question_4"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_4"] [data-cy="answer_2"]').click()

            cy.clickCompleteQuizBtn()
            cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
            cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`quiz failed screen${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createQuizDef(1);
            cy.createQuizQuestionDef(1, 1);
            cy.createQuizMultipleChoiceQuestionDef(1, 2);
            cy.createQuizQuestionDef(1, 3);
            cy.createQuizMultipleChoiceQuestionDef(1, 4);

            cy.setQuizMaxNumAttempts(1, 1)
            cy.setMinNumQuestionsToPass(1, 2)

            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1, {
                selfReportingType: 'Quiz',
                quizId: 'quiz1',
                pointIncrement: '150',
                numPerformToCompletion: 1
            });

            cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
            cy.get('[data-cy="title"]').contains('Quiz')

            cy.get('[data-cy="startQuizAttempt"]').click()
            cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
            cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_2"] [data-cy="answer_3"]').click()

            cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()
            cy.get('[data-cy="question_4"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_4"] [data-cy="answer_2"]').click()

            cy.clickCompleteQuizBtn()
            cy.get('[data-cy="quizFailed"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`survey splash screen${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1, {
                selfReportingType: 'Quiz',
                quizId: 'quiz1',
                pointIncrement: '150',
                numPerformToCompletion: 1
            });

            cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
            cy.get('[data-cy="title"]').contains('Survey')
            cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`survey questions screen${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1, {
                selfReportingType: 'Quiz',
                quizId: 'quiz1',
                pointIncrement: '150',
                numPerformToCompletion: 1
            });

            cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
            cy.get('[data-cy="title"]').contains('Survey')
            cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points')

            cy.get('[data-cy="startQuizAttempt"]').click()
            cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`survey completed screen${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createSurveyDef(1);
            cy.createTextInputQuestionDef(1, 1);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2);
            cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1, {
                selfReportingType: 'Quiz',
                quizId: 'quiz1',
                pointIncrement: '150',
                numPerformToCompletion: 1
            });

            cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
            cy.get('[data-cy="title"]').contains('Survey')
            cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points')

            cy.get('[data-cy="startQuizAttempt"]').click()
            cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('a')
            cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
            cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()

            cy.clickCompleteQuizBtn()
            cy.get('[data-cy="surveyCompletion"]').contains('Congrats!! You just earned 150 points')

            cy.wait(500)
            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });
    })
});
