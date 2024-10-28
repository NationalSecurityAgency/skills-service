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
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Display Single Quiz Attempt Tests', () => {

    let defaultUser
    beforeEach(() => {
        defaultUser = Cypress.env('proxyUser')

        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.enableProdMode(1);
        cy.addToMyProjects(1);

        Cypress.Commands.add("createQuizzesForThisTest", (num) => {
            for (let i = 0; i < num; i++) {
                const quizNum = i + 1
                if (i % 3 === 0) {
                    cy.createSurveyDef(quizNum);
                    cy.createSurveyMultipleChoiceQuestionDef(quizNum, 1, { questionType: 'SingleChoice' });
                } else {
                    cy.createQuizDef(quizNum);
                    cy.createQuizQuestionDef(quizNum, 1)
                }

                cy.runQuizForUser(quizNum, defaultUser, [{selectedIndex: [i%2===0?0:1]}], true, 'My Answer')
            }
        })
    })

    it('quizzes card with 1 survey', () => {
        cy.createQuizzesForThisTest(1)
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="numQuizAndSurveyRuns"]').should('have.text', 1)
        cy.get('[data-cy="numQuizzes"]').should('have.text', 0)
        cy.get('[data-cy="numSurveys"]').should('have.text', 1)
    })

    it('has surveys and quizzes', () => {
        cy.createQuizzesForThisTest(5)
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="numQuizAndSurveyRuns"]').should('have.text', 5)
        cy.get('[data-cy="numQuizzes"]').should('have.text', 3)
        cy.get('[data-cy="numSurveys"]').should('have.text', 2)
    })

    it('navigate to quiz history and back', () => {
        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="numQuizAndSurveyRuns"]').should('have.text', 0)
        cy.get('[data-cy="numQuizzes"]').should('have.text', 0)
        cy.get('[data-cy="numSurveys"]').should('have.text', 0)
        cy.get('[data-cy="viewQuizzesAttemptsBtn"]').click()
        cy.get('[data-cy="noQuizzesOrSurveys"]')
        cy.get('[data-cy="backToProgressAndRankingBtn"]').click()
    })

});


