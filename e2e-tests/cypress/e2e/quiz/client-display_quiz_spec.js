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
        Cypress.env('disabledUILoginProp', true);

        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge', description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'});
        cy.createQuizQuestionDef(1, 1, {question: 'What word completes the phrase: ``Everything but the kitchen``?', answers: [{
                answer: 'Sink',
                isCorrect: true,
            }, {
                answer: 'Kaleidoscope',
                isCorrect: false,
            }, {
                answer: 'Hogwash',
                isCorrect: false,
            }]})
        cy.createQuizQuestionDef(1, 2, {question: 'Traditionally, an ``amuse-bouche`` arrives right before what part of the meal?', answers: [{
                answer: 'Appetizers',
                isCorrect: true,
            }, {
                answer: 'Entrée',
                isCorrect: false,
            }, {
                answer: 'Dessert',
                isCorrect: false,
            }]})
        cy.createQuizQuestionDef(1, 3, {question: 'Which one of these are Batman\'s villains?', answers: [{
                answer: 'Darkseid',
                isCorrect: false,
            }, {
                answer: 'Ra\'s al Ghul',
                isCorrect: true,
            }, {
                answer: 'Mongul',
                isCorrect: false,
            }, {
                answer: 'Poison Ivy',
                isCorrect: true,
            }, {
                answer: 'Mr. Mxyzptlk',
                isCorrect: false,
            }]})
        cy.createQuizQuestionDef(1, 4, {question: 'In the game of Candy Land, which player goes first?', answers: [{
                answer: 'Blue token holder',
                isCorrect: false,
            }, {
                answer: 'The youngest',
                isCorrect: true,
            }, {
                answer: 'First to draw a red card',
                isCorrect: false,
            }]})

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });

    });

    it('run quiz', () => {
        cy.cdVisit('/');
    });
});


