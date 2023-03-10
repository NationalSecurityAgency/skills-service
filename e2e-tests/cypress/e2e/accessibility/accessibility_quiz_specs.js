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
        // cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('new quiz modal', () => {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-cy="quizName"]').type('hello')
        cy.get('[data-cy="quizDescription"]').type('hi')
        cy.get('[data-cy="saveQuizButton"]').should('be.enabled')

        // cy.customLighthouse();
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
        // cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });

    it('empty survey page', () => {
        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="noQuestionsYet"]')

        // cy.customLighthouse();
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

        // cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y();
    });
    //
    //
    //
    // it('survey page with questions', () => {
    //     cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});
    //     cy.createSurveyDef(1);
    //     cy.createTextInputQuestionDef(1, 1);
    //     cy.createSurveyMultipleChoiceQuestionDef(1, 2);
    //     cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });
    //
    //     cy.visit('/administrator/quizzes/quiz1')
    //     cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '6')
    //     // cy.customLighthouse();
    //     cy.injectAxe();
    //     cy.customA11y();
    // });




});
