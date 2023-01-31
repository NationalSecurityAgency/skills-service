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

describe('Quiz Tests', () => {

    const quizTableSelector = '[data-cy="quizDeffinitionsTable"]';
    beforeEach(() => {

    });

    it('create a quiz and a survey', function () {
        cy.visit('/administrator/quizzes/')
        cy.get('[data-cy="noQuizzesYet"]')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.modal-title').contains('New Quiz/Survey')

        cy.get('[data-cy="quizName"]').type('My First Quiz')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'MyFirstQuiz')

        cy.get('[data-cy="quizDescription"]').type('Some cool Description')

        cy.get('[data-cy="saveQuizButton"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Quiz'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
        ], 5);

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-cy="quizName"]').type('My First Survey')
        cy.get('[data-cy="quizTypeSelector"]').select('Survey')
        cy.get('[data-cy="saveQuizButton"]').click()
        cy.get('[data-cy="quizName"]').should('not.exist')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Quiz'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
            [{
                colIndex: 0,
                value: 'My First Survey'
            }, {
                colIndex: 1,
                value: 'Survey'
            }],
        ], 5);


        // refresh and revalidate
        cy.visit('/administrator/quizzes/')
        cy.validateTable(quizTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Quiz'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }],
            [{
                colIndex: 0,
                value: 'My First Survey'
            }, {
                colIndex: 1,
                value: 'Survey'
            }],
        ], 5);
    });

    it('Edit Quiz Validation: Name', function () {
        cy.createSurveyDef(1, { name: 'Already Exist' });

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.modal-title').contains('New Quiz/Survey')

        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')

        // name is not taken
        cy.get('[data-cy="quizName"]').type('Already Exist')
        cy.get('[data-cy="quizNameError"]').contains('The value for the Quiz Name is already taken')
        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')

        // min 3 chars
        cy.get('[data-cy="quizName"]').clear().type('ab')
        cy.get('[data-cy="quizNameError"]').contains('Quiz Name cannot be less than 3 characters')
        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')
        cy.get('[data-cy="quizName"]').type('c')
        cy.get('[data-cy="quizNameError"]').should('not.be.visible')
        cy.get('[data-cy="saveQuizButton"]').should('be.enabled')

        // max 75 chars
        const longName = new Array(75).join('A');
        cy.get('[data-cy="quizName"]').clear().fill(longName)
        cy.get('[data-cy="quizName"]').type('AA')
        cy.get('[data-cy="quizNameError"]').contains('Quiz Name cannot exceed 75 characters')
        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')
        cy.get('[data-cy="quizName"]').type('{backspace}')
        cy.get('[data-cy="quizNameError"]').should('not.be.visible')
        cy.get('[data-cy="saveQuizButton"]').should('be.enabled')

        // required field
        cy.get('[data-cy="quizName"]').clear()
        cy.get('[data-cy="quizNameError"]').contains('Quiz Name is required')
        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')
    });

    it('Edit Quiz Validation: Quiz Id', function () {
        cy.createSurveyDef(1);

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.modal-title').contains('New Quiz/Survey')

        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')
        cy.get('[data-cy="quizName"]').type('Good')
        cy.get('[data-cy="quizNameError"]').should('not.be.visible')
        cy.get('[data-cy="saveQuizButton"]').should('be.enabled')

        // id is not taken
        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="idError"]').should('not.be.visible')
        cy.get('[data-cy="idInputValue"]').clear().type('quiz1');
        cy.get('[data-cy="idError"]').contains('The value for the Quiz/Survey ID is already taken')
        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')

        // at least 3 chars
        cy.get('[data-cy="idInputValue"]').clear().type('ab');
        cy.get('[data-cy="idError"]').contains('Quiz/Survey ID cannot be less than 3 characters')
        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')
        cy.get('[data-cy="idInputValue"]').type('c');
        cy.get('[data-cy="idError"]').should('not.be.visible')
        cy.get('[data-cy="saveQuizButton"]').should('be.enabled')

        // no more than 50 chars
        const longId = new Array(100).join('A');
        cy.get('[data-cy="idInputValue"]').clear().fill(longId);
        cy.get('[data-cy="idInputValue"]').type('bb');
        cy.get('[data-cy="idError"]').contains('Quiz/Survey ID cannot exceed 100 characters')
        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')

        // required field
        cy.get('[data-cy="idInputValue"]').clear()
        cy.get('[data-cy="idError"]').contains('Quiz/Survey ID is required')
        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')
    });

    it('Edit Quiz Validation: description', function () {
        cy.createSurveyDef(1);

        cy.visit('/administrator/quizzes/')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('.modal-title').contains('New Quiz/Survey')

        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')
        cy.get('[data-cy="quizName"]').type('Good')
        cy.get('[data-cy="quizNameError"]').should('not.be.visible')
        cy.get('[data-cy="saveQuizButton"]').should('be.enabled')

        // custom description validation
        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="quizDescription"]').type('a jabberwocky b');
        cy.get('[data-cy="quizDescriptionError"]').contains('Quiz/Survey Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveQuizButton"]').should('be.disabled')

    });

    it('quiz name search / filtering', function () {
        cy.createSurveyDef(1, { name: 'a Quiz 1' });
        cy.createQuizDef(2, { name: 'b Survey 1' });
        cy.createSurveyDef(3, { name: 'c Quiz 2' });
        cy.createQuizDef(4, { name: 'd Survey 2' });
        cy.createSurveyDef(5, { name: 'e Quiz 3' });
        cy.createQuizDef(6, { name: 'f Survey 3' });

        cy.visit('/administrator/quizzes/')

        // sort by name
        const headerSelector = `${quizTableSelector} thead tr th`;
        cy.get(headerSelector)
            .contains('Name')
            .click();

        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 6)
        cy.get('[data-cy="quizNameFilter"]').type('3')
        cy.get('[data-cy="quizFilterBtn"]').click();
        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 2)

        cy.get(`${quizTableSelector} [aria-rowindex="1"] [data-cy="managesQuizLink_quiz5"]`)
        cy.get(`${quizTableSelector} [aria-rowindex="2"] [data-cy="managesQuizLink_quiz6"]`)
        cy.get(`${quizTableSelector} [aria-rowindex="3"]`).should('not.exist')

        cy.get('[data-cy="quizResetBtn"]').click()
        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 6)
        cy.get('[data-cy="quizNameFilter"]').should('have.value', '')
        cy.get('[data-cy="quizNameFilter"]').type('sUrVeY{enter}')

        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 3)

        cy.get(`${quizTableSelector} [aria-rowindex="1"] [data-cy="managesQuizLink_quiz2"]`)
        cy.get(`${quizTableSelector} [aria-rowindex="2"] [data-cy="managesQuizLink_quiz4"]`)
        cy.get(`${quizTableSelector} [aria-rowindex="3"] [data-cy="managesQuizLink_quiz6"]`)
        cy.get(`${quizTableSelector} [aria-rowindex="4"]`).should('not.exist')

        cy.get('[data-cy="quizNameFilter"]').type('{backspace}{backspace}{backspace}{backspace}{backspace}{backspace}{enter}')
        cy.get(`${quizTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 6)
    });

});

