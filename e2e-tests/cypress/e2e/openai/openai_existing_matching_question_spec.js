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
import { completedMsg, existingQuestionWelcomeMsg } from './openai_helper_commands'

describe('AI editing of existing matching questions', () => {
    it('sends existing pairs to the assistant and preserves them when applying and saving proofreading', () => {
        const pairs = [
            { term: 'SSH', value: '22' },
            { term: 'HTTPS', value: '443' }
        ]
        const generatedQuestion = 'Match each protocol to its default port.'
        const assertEditorPairs = () => {
            pairs.forEach((pair, index) => {
                cy.get(`[data-cy="termText-${index}"]`).should('have.value', pair.term)
                cy.get(`[data-cy="answerText-${index}"]`).should('have.value', pair.value)
            })
            cy.get('[data-cy="answer-2"]').should('not.exist')
        }
        const assertSavedPairs = () => {
            pairs.forEach((pair, index) => {
                cy.get(`[data-cy="questionDisplayCard-1"] [data-cy="question-1-answer-${index}-term"]`)
                    .should('have.text', pair.term)
                cy.get(`[data-cy="questionDisplayCard-1"] [data-cy="question-1-answer-${index}-value"]`)
                    .should('have.text', pair.value)
            })
            cy.get('[data-cy="question-1-answer-2-term"]').should('not.exist')
        }
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.enableOpenAIIntegration = true
                res.send(res.body)
            })
        })
        cy.createQuizDef(1)
        cy.createQuizMatchingQuestionDef(1, 1, {
            question: 'Match each protocol to its default port',
            answers: pairs.map((multiPartAnswer) => ({ answer: '', isCorrect: true, multiPartAnswer }))
        })
        cy.visit('/administrator/quizzes/quiz1')
        assertSavedPairs()
        cy.get('[data-cy="editQuestionButton_1"]').click()
        assertEditorPairs()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').should('contain.text', existingQuestionWelcomeMsg)

        // WireMock requires both original pairs in the existing-question section.
        // An empty answer array or missing multiPartAnswer cannot match the fixture.
        cy.get('[data-cy="instructionsInput"]')
            .type('AI existing matching pairs regression - proofread while keeping its answers{enter}')
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', completedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').should('contain.text', generatedQuestion)
        pairs.forEach((pair, index) => {
            cy.get(`[data-cy="aiMsg-2"] [data-cy="answer-${index}-term"]`).should('have.text', pair.term)
            cy.get(`[data-cy="aiMsg-2"] [data-cy="answer-${index}-value"]`).should('have.text', pair.value)
        })

        cy.intercept('POST', '/api/validation/description', (req) => {
            if (req.body.value === generatedQuestion) {
                req.alias = 'generatedQuestionValidation'
            }
        })
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled').click()
        cy.get('[data-cy="instructionsInput"]').should('not.exist')
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]')
            .should('contain.text', generatedQuestion)
        assertEditorPairs()
        // Wait for debounced validation before saving the applied question.
        cy.wait('@generatedQuestionValidation')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]')
            .should('contain.text', generatedQuestion)
        assertSavedPairs()
        cy.reload()
        assertSavedPairs()
        cy.get('[data-cy="editQuestionButton_1"]').click()
        assertEditorPairs()
    })
})
