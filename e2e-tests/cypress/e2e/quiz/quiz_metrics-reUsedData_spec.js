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
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Quiz Metrics With Reused Data Tests', () => {

    before(() => {
       cy.beforeTestSuiteThatReusesData()

        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});
        cy.createQuizQuestionDef(1, 1, { question: 'This is a Single Choice Question example for metrics.'})
        cy.createQuizMultipleChoiceQuestionDef(1, 2, { question: 'This is a Multiple Choice Question example for metrics.'});

        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}, {selectedIndex: [0,2]}], false);
        cy.runQuizForUser(1, 3, [{selectedIndex: [1]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 3, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 4, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 5, [{selectedIndex: [1]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 6, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 7, [{selectedIndex: [0]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 8, [{selectedIndex: [1]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 9, [{selectedIndex: [1]}, {selectedIndex: [0,2]}]);
        cy.runQuizForUser(1, 10, [{selectedIndex: [1]}, {selectedIndex: [0]}]);
        cy.runQuizForUser(1, 11, [{selectedIndex: [0]}, {selectedIndex: [0]}]);


        cy.createSurveyDef(2);
        cy.createSurveyMultipleChoiceQuestionDef(2, 1);
        cy.createTextInputQuestionDef(2, 2);
        cy.createSurveyMultipleChoiceQuestionDef(2, 3, { questionType: 'SingleChoice' });

        cy.runQuizForUser(2, 1, [{selectedIndex: [0, 1]}, {selectedIndex: [0]}, {selectedIndex: [0]}]);
        cy.runQuizForUser(2, 2, [{selectedIndex: [0]}, {selectedIndex: [0]}, {selectedIndex: [2]}]);
        cy.runQuizForUser(2, 3, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}]);
        cy.runQuizForUser(2, 5, [{selectedIndex: [0, 1]}, {selectedIndex: [0]}, {selectedIndex: [0]}]);
        cy.runQuizForUser(2, 6, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}]);
        cy.runQuizForUser(2, 7, [{selectedIndex: [2]}, {selectedIndex: [0]}, {selectedIndex: [1]}]);
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    beforeEach(() => {

    });


    it('quiz metrics page', function () {
        cy.visit('/administrator/quizzes/quiz1');

    });


});
