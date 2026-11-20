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
import { completedMsg, existingDescWelcomeMsg, existingQuestionWelcomeMsg } from './openai_helper_commands'

describe('AI restored content length', () => {
    const lengthError = 'The generated content exceeds the maximum allowed length.'
    const imageText = 'Bounded image content.'
    const plainText = 'This generated plain text is too long for the configured limit, even though it contains no image placeholders.'
    const preview = '[data-cy="aiMsg-2"] [data-cy="generatedSegment"]'

    const openAssistant = (context, maxLength, imageMarkdown = null) => {
        const isQuestion = context === 'question'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.enableOpenAIIntegration = true
                // Production configuration exposes this setting as a string.
                res.body.descriptionMaxLength = String(maxLength)
                res.send(res.body)
            })
        })
        const original = imageMarkdown ? `Original content.\n\n${imageMarkdown}` : 'Original content.'
        if (isQuestion) {
            cy.createQuizDef(1)
            cy.createQuizQuestionDef(1, 1, { question: original })
            cy.visit('/administrator/quizzes/quiz1')
            cy.get('[data-cy="editQuestionButton_1"]').click()
        } else {
            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1, { description: original })
            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
            cy.get('[data-cy="editSkillButton_skill1"]').click()
        }
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').should('contain.text', isQuestion ? existingQuestionWelcomeMsg : existingDescWelcomeMsg)
    }

    const assertRejected = () => {
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('be.visible').and('contain.text', lengthError)
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get(`${preview} img[src^="data:image/"]`).should('not.exist')
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').should('be.enabled').and('have.focus')
    }

    const assertApplied = (context, expectedText) => {
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', completedMsg)
        cy.get(preview).should('contain.text', expectedText)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled').click()
        cy.get('[data-cy="instructionsInput"]').should('not.exist')
        const editor = context === 'question'
            ? '[data-cy="questionText"] [data-cy="markdownEditorInput"]'
            : '[data-cy="markdownEditorInput"]'
        cy.get(editor).should('contain.text', expectedText)
        return editor
    }

    const contexts = ['question', 'description']
    contexts.forEach((context) => {
        const oversizedScenarios = [true, false]
        oversizedScenarios.forEach((oversized) => {
            it(`${oversized ? 'rejects' : 'accepts'} repeated images ${oversized ? 'one character over' : 'exactly at'} the ${context} limit`, () => {
                cy.fixture('valid_icon.png', 'base64').then((imageBase64) => {
                    const imageUrl = `data:image/png;base64,${imageBase64}`
                    const imageMarkdown = `![Diagram](${imageUrl})`
                    const restored = `${imageText}\n\n${imageMarkdown}\n\n${imageMarkdown}\n\n${imageMarkdown}`
                    openAssistant(context, restored.length - (oversized ? 1 : 0), imageMarkdown)
                    cy.get('[data-cy="instructionsInput"]').type('AI restored content length - repeated images{enter}')
                    if (oversized) {
                        assertRejected()
                    } else {
                        // The question preview also contains headings; these must not
                        // cause an otherwise exact-limit question to be rejected.
                        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', completedMsg)
                        cy.get(`${preview} img`).should('have.length', 3).each(($image) => {
                            cy.wrap($image).should('have.attr', 'src', imageUrl)
                        })
                        const editor = assertApplied(context, imageText)
                        cy.get(`${editor} .toastui-editor-ww-container img[alt="Diagram"]`).should('have.length', 3).each(($image) => {
                            cy.wrap($image).scrollIntoView().should('be.visible').and('have.attr', 'src', imageUrl)
                        })
                    }
                })
            })

            it(`${oversized ? 'rejects' : 'accepts'} plain text ${oversized ? 'one character over' : 'exactly at'} the ${context} limit without extracted images`, () => {
                openAssistant(context, plainText.length - (oversized ? 1 : 0))
                cy.get('[data-cy="instructionsInput"]').type('AI restored content length - plain text{enter}')
                if (oversized) {
                    assertRejected()
                } else {
                    assertApplied(context, plainText)
                }
            })
        })
    })

    it('rejects oversized displayed content even when the restored question fits', () => {
        cy.fixture('valid_icon.png', 'base64').then((imageBase64) => {
            const imageMarkdown = `![Diagram](data:image/png;base64,${imageBase64})`
            const restoredQuestion = `${imageText}\n\n${imageMarkdown}`
            openAssistant('question', restoredQuestion.length, imageMarkdown)
            cy.get('[data-cy="instructionsInput"]').type('AI restored content length - preview overflow{enter}')
            assertRejected()
        })
    })
})
