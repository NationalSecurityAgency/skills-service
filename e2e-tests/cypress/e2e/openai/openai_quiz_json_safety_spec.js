/*
 * Copyright 2026 SkillTree
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
import { completedMsg, errMsg, newQuizWelcomeMsg } from './openai_helper_commands'

describe('Generated quiz JSON safety', () => {
    beforeEach(() => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.enableOpenAIIntegration = true
                res.send(res.body)
            })
        })
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            description: 'Quiz responses must be parsed as JSON data, never executed as JavaScript.',
            numPerformToCompletion: 1,
        })
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1', {
            onBeforeLoad(win) {
                win.__quizProbe = false
            },
        })
        cy.get('[data-cy="generateQuizBtn"]').click()
        cy.get('[data-cy="aiMsg-0"]').should('contain.text', newQuizWelcomeMsg)
    })

    const assertRejectedResponse = () => {
        // Completion-time JSON parsing fails too, but cannot undo streaming-time side effects.
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', errMsg)
        cy.window().should('have.property', '__quizProbe', false)
        cy.get('[data-cy="generatedQuestion"]').should('not.exist')
        cy.get('[data-cy="selfReportMediaCard"]').should('not.contain.text', 'Self Report: Quiz')
    }

    it('rejects executable model output delivered in one content chunk without setting a browser probe', () => {
        cy.get('[data-cy="instructionsInput"]').type('Quiz JSON safety regression - executable single chunk{enter}')
        assertRejectedResponse()
    })

    it('rejects executable model output split across content chunks without setting a browser probe', () => {
        cy.get('[data-cy="instructionsInput"]').type('Quiz JSON safety regression - executable split chunks{enter}')
        assertRejectedResponse()
    })

    it('rejects JavaScript object syntax instead of evaluating it as a question', () => {
        cy.get('[data-cy="instructionsInput"]').type('Quiz JSON safety regression - object syntax{enter}')
        assertRejectedResponse()
    })

    it('renders, applies, and saves valid JSON questions split across content chunks', () => {
        cy.get('[data-cy="instructionsInput"]').type('Quiz JSON safety regression - valid split chunks{enter}')
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', completedMsg)
        cy.get('[data-cy="generatedQuestion"]').should('have.length', 2)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]')
            .should('contain.text', 'Which parser treats model output as data?')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]')
            .should('contain.text', 'Which inputs are valid JSON?')
        cy.window().should('have.property', '__quizProbe', false)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled').click()
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get('[data-cy="selfReportMediaCard"]').should('contain.text', 'Self Report: Quiz')
        cy.get('[data-cy="buttonToQuiz"]').click()
        cy.get('[data-cy="btn_Questions"]').click()
        cy.contains('[data-cy^="questionDisplayCard-"]', 'Which parser treats model output as data?')
            .should('be.visible').within(() => {
                cy.get('[data-cy="answer-0_displayText"]').should('have.text', 'JSON.parse')
                cy.get('[data-cy="answerDisplay-0"] [data-cy="selected"]').should('be.visible')
            })
        cy.contains('[data-cy^="questionDisplayCard-"]', 'Which inputs are valid JSON?')
            .should('be.visible').within(() => {
                cy.get('[data-cy="answer-0_displayText"]').should('have.text', 'A quoted string')
                cy.get('[data-cy="answer-1_displayText"]').should('have.text', 'An array of numbers')
                cy.get('[data-cy="answerDisplay-0"] [data-cy="selected"]').should('be.visible')
                cy.get('[data-cy="answerDisplay-1"] [data-cy="selected"]').should('be.visible')
            })
    })
})
