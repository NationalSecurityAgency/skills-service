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

describe('Accessibility Quiz Tests', () => {

    beforeEach(() => {
    });

    it('empty quiz definitions page', () => {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('new quiz modal', () => {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-cy="quizName"]').type('hello')
        cy.get('[data-cy="quizDescription"]').type('hi')
        cy.get('[data-cy="saveQuizButton"]').should('be.enabled')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('quiz definitions page', () => {
        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});
        cy.createSurveyDef(2);
        cy.createQuizDef(3);
        cy.createSurveyDef(4);
        cy.createSurveyDef(5);
        cy.createSurveyDef(6);

        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '6')
        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('empty survey page', () => {
        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="noQuestionsYet"]')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('new question modal', () => {
        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="noQuestionsYet"]')

        cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
        cy.get('[data-cy="questionText"]').type('hi')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('answer1')
        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').click()

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('new survey modal with TextInput question type', () => {
        cy.createSurveyDef(1, {name: 'Test Your Trivia Knowledge'});

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="noQuestionsYet"]')

        cy.get('[data-cy="newQuestionOnBottomBtn"]').click()
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_TextInput"]').click()
        cy.get('[data-cy="textAreaPlaceHolder"]').should('be.visible')
        cy.get('[data-cy="questionText"]').type('hi')

        cy.wait(1111);
        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('survey page with questions', () => {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="editQuestionButton_1"]').should('be.enabled')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('empty results page', () => {
        cy.createSurveyDef(1, {name: 'Test Your Trivia Knowledge'});

        cy.visit('/administrator/quizzes/quiz1/results')
        cy.get('[data-cy="noMetricsYet"]')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('runs page', () => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}], false);

        cy.visit('/administrator/quizzes/quiz1/runs')
        cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '2')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('single run', () => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
        cy.createTextInputQuestionDef(1, 3);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}, {selectedIndex: [0]}, {selectedIndex: [0]}])

        cy.visit('/administrator/quizzes/quiz1/runs')
        cy.get('[data-cy="row0-viewRun"]').click();
        cy.get('[data-cy="questionDisplayCard-1"]').contains('This is a question # 1')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });


    it('results oage', () => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
        cy.createTextInputQuestionDef(1, 3);
        cy.runQuizForUser(1, 1, [{selectedIndex: [1]}, {selectedIndex: [0]}, {selectedIndex: [0]}])

        cy.visit('/administrator/quizzes/quiz1/results')
        cy.get('[data-cy="metrics-q1"] [data-cy="row1-colNumAnswered"] [data-cy="answerHistoryBtn"]').should('be.enabled')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('quiz access page', () => {
        cy.createSurveyDef(1);

        cy.visit('/administrator/quizzes/quiz1/access')
        cy.get('[data-cy="quizUserRoleTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('quiz access page', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.visit('/administrator/quizzes/quiz1/settings');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

});
