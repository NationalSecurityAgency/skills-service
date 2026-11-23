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

describe('AI single-question image preservation instructions', () => {
    it('sends preservation instructions and retains the image when refining and saving a question', () => {
        const generatedQuestion = 'Which shape is shown in the diagram?'
        const editor = '[data-cy="questionText"] [data-cy="markdownEditorInput"] .toastui-editor-ww-container'
        const savedQuestion = '[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.enableOpenAIIntegration = true
                res.send(res.body)
            })
        })
        cy.createQuizDef(1)
        cy.fixture('valid_icon.png', 'base64').then((imageBase64) => {
            const imageUrl = `data:image/png;base64,${imageBase64}`
            const assertImage = (selector) => {
                cy.get(selector).should('not.contain.text', 'IMG-0')
                cy.get(`${selector} img`).filter(':visible').should('have.length', 1)
                    .and('be.visible').and('have.attr', 'src', imageUrl)
                    .should(($image) => {
                        expect($image[0].naturalWidth, 'original image loaded').to.be.greaterThan(0)
                    })
            }
            cy.createQuizQuestionDef(1, 1, {
                question: `Identify the shape in this diagram.\n\n![Quiz diagram](${imageUrl})`,
            })
            cy.visit('/administrator/quizzes/quiz1')
            assertImage(savedQuestion)
            cy.get('[data-cy="editQuestionButton_1"]').click()
            assertImage(editor)
            cy.get('[data-cy="aiButton"]').click()
            cy.get('[data-cy="aiMsg-0"]').should('contain.text', existingQuestionWelcomeMsg)

            // WireMock only returns this response when the initial prompt includes
            // the image-preservation instruction, not merely the image placeholder.
            cy.get('[data-cy="instructionsInput"]').type('AI single question preservation instructions regression - proofread{enter}')
            cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', completedMsg)
            cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').should('contain.text', generatedQuestion)
            assertImage('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]')

            cy.intercept('POST', '/api/validation/description', (req) => {
                if (req.body.value?.startsWith(generatedQuestion)) {
                    req.alias = 'generatedQuestionValidation'
                }
            })
            cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled').click()
            cy.get('[data-cy="instructionsInput"]').should('not.exist')
            cy.get(editor).should('contain.text', generatedQuestion)
            assertImage(editor)
            // Synchronize with debounced validation before saving the applied question.
            cy.wait('@generatedQuestionValidation')
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
            cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
            cy.get(savedQuestion).should('contain.text', generatedQuestion)
            assertImage(savedQuestion)
            cy.reload()
            cy.get(savedQuestion).should('contain.text', generatedQuestion)
            assertImage(savedQuestion)
        })
    })
})
