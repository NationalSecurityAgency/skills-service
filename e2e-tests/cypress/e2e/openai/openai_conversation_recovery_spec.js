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

import { completedMsg, stopMsg } from './openai_helper_commands'

describe('AI question conversation history and recovery', () => {
    const editor = '[data-cy="questionText"] [data-cy="markdownEditorInput"]'
    const savedQuestion = '[data-cy="questionDisplayCard-1"]'
    const updatedAnswers = [
        { text: 'Queen', isCorrect: true },
        { text: 'The Beatles', isCorrect: true },
        { text: 'Mozart', isCorrect: false },
    ]
    const matchingAnswers = [
        { term: 'Queen', value: 'We Will Rock You' },
        { term: 'The Beatles', value: 'Yesterday' },
    ]
    const questions = {
        MultipleChoice: 'Select the bands that recorded these updated hits.',
        Matching: 'Match the bands with their updated hit songs.',
        FillInTheBlank: 'The bands behind We Will Rock You and Yesterday are ___ and ___.',
    }

    const selectType = (type) => {
        cy.get('[data-cy="genQuestionTypeSelector"]').should('not.have.class', 'p-disabled').click()
        cy.get(`[role="option"] [data-cy="selectionItem_${type}"]`).click()
    }
    const openConversation = (type) => {
        cy.viewport(1280, 1400)
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.enableOpenAIIntegration = true
                res.body.openAiDisableSingleQuestionTypeChange = false
                res.send(res.body)
            })
        })
        cy.createQuizDef(1)
        cy.visit('/administrator/quizzes/quiz1')
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="aiButton"]').click()
        selectType(type)
    }
    const generate = (prompt, id) => {
        cy.get('[data-cy="instructionsInput"]').should('be.enabled').type(`${prompt}{enter}`)
        cy.get(`[data-cy="aiMsg-${id}"] [data-cy="finalSegment"]`).should('contain.text', completedMsg)
        cy.get(`[data-cy="useGenValueBtn-${id}"]`).should('be.enabled')
    }
    const assertUpdatedPreview = (type, id) => {
        const preview = `[data-cy="aiMsg-${id}"]`
        cy.get(`${preview} [data-cy="generatedSegment"]`).should('contain.text', questions[type])
        if (type === 'Matching') {
            matchingAnswers.forEach((answer, index) => {
                cy.get(`${preview} [data-cy="answer-${index}-term"]`).should('have.text', answer.term)
                cy.get(`${preview} [data-cy="answer-${index}-value"]`).should('have.text', answer.value)
            })
        } else {
            const answers = type === 'FillInTheBlank' ? updatedAnswers.slice(0, 2) : updatedAnswers
            answers.forEach((answer) => {
                cy.get(`${preview} [data-cy="generatedAnswers"]`).should('contain.text', answer.text)
            })
        }
    }
    const applySaveAndReload = (type, id) => {
        // Applying generated Markdown triggers debounced validation before it can be saved.
        // The editor may escape underscores when serializing FillInTheBlank Markdown.
        const validationPrefix = questions[type].split('___')[0]
        cy.intercept('POST', '/api/validation/description', (req) => {
            if (req.body.value?.startsWith(validationPrefix)) {
                req.alias = 'generatedQuestionValidation'
            }
        })
        cy.get(`[data-cy="useGenValueBtn-${id}"]`).click()
        cy.get(editor).should('contain.text', questions[type])
        cy.get(`[data-cy="answerTypeSelector"] [data-cy="selectionItem_${type}"]`).should('exist')
        if (type === 'Matching') {
            matchingAnswers.forEach((answer, index) => {
                cy.get(`[data-cy="termText-${index}"]`).should('have.value', answer.term)
                cy.get(`[data-cy="answerText-${index}"]`).should('have.value', answer.value)
            })
            cy.get('[data-cy="termText-2"]').should('not.exist')
        } else {
            const answers = type === 'FillInTheBlank' ? updatedAnswers.slice(0, 2) : updatedAnswers
            answers.forEach((answer, index) => {
                cy.get(`[data-cy="answer-${index}"] [data-cy="answerText"]`).should('have.value', answer.text)
                if (type === 'MultipleChoice') {
                    cy.get(`[data-cy="answer-${index}"] [data-cy="selectCorrectAnswer"] [data-cy="${answer.isCorrect ? 'selected' : 'notSelected'}"]`).should('exist')
                }
            })
            cy.get(`[data-cy="answer-${answers.length}"]`).should('not.exist')
        }
        cy.wait('@generatedQuestionValidation')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
        cy.get(editor).should('not.exist')
        const assertSaved = () => {
            cy.get(`${savedQuestion} [data-cy="questionDisplayText"]`).should('contain.text', questions[type])
            if (type === 'Matching') {
                matchingAnswers.forEach((answer, index) => {
                    cy.get(`${savedQuestion} [data-cy="question-1-answer-${index}-term"]`).should('have.text', answer.term)
                    cy.get(`${savedQuestion} [data-cy="question-1-answer-${index}-value"]`).should('have.text', answer.value)
                })
            } else {
                const answers = type === 'FillInTheBlank' ? updatedAnswers.slice(0, 2) : updatedAnswers
                answers.forEach((answer, index) => {
                    if (type === 'FillInTheBlank') {
                        cy.get(savedQuestion).should('contain.text', answer.text)
                    } else {
                        cy.get(`${savedQuestion} [data-cy="answer-${index}_displayText"]`).should('have.text', answer.text)
                        cy.get(`${savedQuestion} [data-cy="answerDisplay-${index}"] [data-cy="${answer.isCorrect ? 'selected' : 'notSelected'}"]`).should('exist')
                    }
                })
            }
        }
        assertSaved()
        cy.reload()
        assertSaved()
    }

    const answerFormats = ['MultipleChoice', 'Matching', 'FillInTheBlank']
    answerFormats.forEach((type) => {
        it(`uses updated ${type} answers from the second response in a third generation`, () => {
            openConversation(type)
            generate('Great rock bands', 2)
            // WireMock requires the initial assistant's answers, not just the displayed question.
            generate(`AI format history regression - update ${type} answers`, 4)
            assertUpdatedPreview(type, 4)
            // WireMock sees the prepended system message, so the second assistant response is at messages[4].
            generate(`AI format history regression - retain updated ${type} answers`, 6)
            assertUpdatedPreview(type, 6)
            applySaveAndReload(type, 6)
        })
    })

    it('changes from SingleChoice to Matching to FillInTheBlank without losing updated answer history', () => {
        openConversation('SingleChoice')
        generate('AI answer history regression - generate a rock band question', 2)
        selectType('Matching')
        generate('AI format history regression - convert SingleChoice to Matching', 4)
        assertUpdatedPreview('Matching', 4)
        selectType('FillInTheBlank')
        generate('AI format history regression - convert Matching to FillInTheBlank', 6)
        assertUpdatedPreview('FillInTheBlank', 6)
        // Earlier messages must still render their own answer format after changing the selector.
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').should('contain.text', 'Led Zeppelin')
        assertUpdatedPreview('Matching', 4)
        applySaveAndReload('FillInTheBlank', 6)
    })

    const failureScenarios = ['partial', 'cancelled', 'malformed']
    failureScenarios.forEach((failure) => {
        it(`recovers from a ${failure} answer response and saves the next generation`, () => {
            openConversation('SingleChoice')
            generate('AI answer history regression - generate a rock band question', 2)
            cy.get('[data-cy="instructionsInput"]').type(`AI recovery regression - ${failure} answers{enter}`)
            cy.get('[data-cy="aiMsg-4"] [data-cy="generatedSegment"]').should('contain.text', 'Unusable draft')
            if (failure === 'cancelled') {
                // Cancel only after the answers section has started, exercising parser-state reset.
                cy.get('[data-cy="aiMsg-4"] [aria-label="Loading"]').should('be.visible')
                cy.get('[data-cy="genQuestionTypeSelector"]').should('have.class', 'p-disabled')
                cy.get('[data-cy="sendAndStopBtn"]').should('be.enabled').click()
            }
            const error = failure === 'cancelled' ? stopMsg : 'The AI returned answers in an invalid format.'
            cy.get('[data-cy="aiMsg-4"] [data-cy="finalSegment"]').should('contain.text', error)
            cy.get('[data-cy="useGenValueBtn-4"]').should('not.exist')
            cy.get('[data-cy="instructionsInput"]').should('be.enabled').and('have.focus')
            cy.get('[data-cy="genQuestionTypeSelector"]').should('not.have.class', 'p-disabled')

            // Recovery requires the successful answers and the failed draft's raw answers section in history.
            generate('AI recovery regression - regenerate valid answers', 6)
            const question = 'Which of these performers is a rock band?'
            cy.intercept('POST', '/api/validation/description', (req) => {
                if (req.body.value?.startsWith(question)) {
                    req.alias = 'recoveredQuestionValidation'
                }
            })
            cy.get('[data-cy="useGenValueBtn-6"]').click()
            cy.get(editor).should('contain.text', question).and('not.contain.text', 'Unusable draft')
            const answers = ['Led Zeppelin', 'Mozart', 'Eminem', 'Bach']
            answers.forEach((answer, index) => {
                cy.get(`[data-cy="answer-${index}"] [data-cy="answerText"]`).should('have.value', answer)
                cy.get(`[data-cy="answer-${index}"] [data-cy="${index === 0 ? 'selected' : 'notSelected'}"]`).should('exist')
            })
            cy.get('[data-cy="answer-4"]').should('not.exist')
            cy.wait('@recoveredQuestionValidation')
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
            cy.get(editor).should('not.exist')
            cy.get(savedQuestion).should('contain.text', question)
            cy.reload()
            cy.get(savedQuestion).should('contain.text', question).and('not.contain.text', 'Unusable draft')
            answers.forEach((answer, index) => {
                cy.get(`${savedQuestion} [data-cy="answer-${index}_displayText"]`).should('have.text', answer)
                cy.get(`${savedQuestion} [data-cy="answerDisplay-${index}"] [data-cy="${index === 0 ? 'selected' : 'notSelected'}"]`).should('exist')
            })
        })
    })
})
