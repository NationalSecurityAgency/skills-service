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

describe('Client Display Quiz Theme Tests', () => {

    beforeEach(() => {

    });

    it('theme - quiz splash screen', () => {
        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1?enableTheme=true');
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1');
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

    it('theme - quiz questions', () => {
        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizMultipleChoiceQuestionDef(1, 3);
        cy.createQuizMultipleChoiceQuestionDef(1, 4);
        cy.createQuizMultipleChoiceQuestionDef(1, 5);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1?enableTheme=true');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_4"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_4"]').blur()

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

    it('theme - quiz results - passed quiz - all correct', () => {
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/complete', (req) => {
            req.reply((res) => {
                const resBody = res.body;
                resBody.completed = '2023-02-15T22:52:53.990+00:00';
                resBody.started = '2023-02-15T21:45:50.917+00:00';
                res.send(resBody);
            });
        }).as('completeSurvey')

        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 3);
        // cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1?enableTheme=true');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.wait('@completeSurvey')
        cy.get('[data-cy="quizPassed"]').should('exist')
        cy.get('[data-cy="quizCompletion"]').contains('You just earned 150 points')
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

    it('theme - quiz results - passed quiz - some correct', () => {
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/complete', (req) => {
            req.reply((res) => {
                const resBody = res.body;
                resBody.completed = '2023-02-15T22:52:53.990+00:00';
                resBody.started = '2023-02-15T21:45:50.917+00:00';
                res.send(resBody);
            });
        }).as('completeSurvey')
        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizMultipleChoiceQuestionDef(1, 4);
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1?enableTheme=true');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_4"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.wait('@completeSurvey')
        cy.get('[data-cy="quizPassed"]').should('exist')
        cy.get('[data-cy="quizCompletion"]').contains('You just earned 150 points')
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

    it('theme - quiz results - failed', () => {
        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 3);
        cy.setQuizMaxNumAttempts(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1?enableTheme=true');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizFailed"]').should('exist')
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

    it('theme - survey splash screen', () => {
        cy.createSurveyDef(1, { name: 'Trivia Knowledge' });
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1?enableTheme=true');
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1');
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

    it('theme - survey questions', () => {
        cy.createSurveyDef(1, { name: 'Trivia Knowledge' });
        cy.createSurveyMultipleChoiceQuestionDef(1, 1, { questionType: 'SingleChoice' });
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
        cy.createSurveyMultipleChoiceQuestionDef(1, 3);
        cy.createSurveyMultipleChoiceQuestionDef(1, 4);
        cy.createSurveyMultipleChoiceQuestionDef(1, 5);
        cy.createTextInputQuestionDef(1, 6);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1?enableTheme=true');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_3"]').blur()

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

    it('theme - survey complete', () => {
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/complete', (req) => {
            req.reply((res) => {
                const resBody = res.body;
                resBody.completed = '2023-02-15T22:52:53.990+00:00';
                resBody.started = '2023-02-15T21:45:50.917+00:00';
                res.send(resBody);
            });
        }).as('completeSurvey')

        cy.createSurveyDef(1, { name: 'Trivia Knowledge' });
        cy.createSurveyMultipleChoiceQuestionDef(1, 1, { questionType: 'SingleChoice' });

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1?enableTheme=true');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.wait('@completeSurvey')
        cy.get('[data-cy="surveyCompletion"]').contains('You just earned 150 points')
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]');
    });

});


