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

describe('AI image placement safety', () => {
    const unsafePlacementMsg = 'The AI placed an image placeholder in an invalid location.'
    const validPrompt = 'AI image placement safety - valid images'
    const safeText = 'Image placement is safe.'
    const assertImages = (selector, imageUrls, count = 12) => {
        const images = `${selector} img[alt$="diagram"]`
        cy.get(images).should('have.length', count)
        for (let index = 0; index < count; index++) {
            cy.get(images).eq(index).scrollIntoView().should('be.visible')
                .and('have.attr', 'src', imageUrls[index % imageUrls.length])
                .and('have.attr', 'alt', index % 2 === 0 ? 'First$&---IMG-99diagram' : 'Seconddiagram')
                .should(($image) => {
                    expect($image[0].naturalWidth, `original image ${index} loaded`).to.be.greaterThan(0)
                })
        }
    }
    const assertNoImageDataRequests = () => {
        // The streaming preview may request the raw placeholder URL. The security
        // guarantee is that neither original image data nor validation probes leave.
        cy.get('@imageCollector.all').should((requests) => {
            requests.forEach(({ request }) => {
                expect(decodeURIComponent(request.url)).not.to.contain('data:image/')
            })
        })
    }

    beforeEach(() => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.enableOpenAIIntegration = true
                res.send(res.body)
            })
        })
        // Only the attacker-controlled collector is stubbed, not AI generation.
        cy.intercept('GET', 'https://attacker.example/collect*', { statusCode: 204 }).as('imageCollector')
    })

    const contexts = ['question', 'description']
    contexts.forEach((context) => {
        const isQuestion = context === 'question'
        const editor = isQuestion
            ? '[data-cy="questionText"] [data-cy="markdownEditorInput"] .toastui-editor-ww-container'
            : '[data-cy="markdownEditorInput"] .toastui-editor-ww-container'
        const saved = isQuestion
            ? '[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]'
            : '[data-cy="skillOverviewDescription"]'
        const openAssistant = () => {
            return cy.fixture('valid_icon.png', 'base64').then((firstImage) => {
                return cy.fixture('anothervalid_icon.png', 'base64').then((secondImage) => {
                    const imageUrls = [firstImage, secondImage].map((image) => `data:image/png;base64,${image}`)
                    // Spaces in alt text can invalidate the enclosing attacker URL;
                    // keep captions whitespace-free so the Markdown exploit is exercised.
                    const original = `Original image content.\n\n![First$&---IMG-99diagram](${imageUrls[0]})\n\n![Seconddiagram](${imageUrls[1]})`
                    if (isQuestion) {
                        cy.createQuizDef(1)
                        cy.createQuizQuestionDef(1, 1, { question: original })
                        cy.visit('/administrator/quizzes/quiz1')
                        assertImages(saved, imageUrls, 2)
                        cy.get('[data-cy="editQuestionButton_1"]').click()
                    } else {
                        cy.createProject(1)
                        cy.createSubject(1, 1)
                        cy.createSkill(1, 1, 1, { description: original })
                        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
                        assertImages(saved, imageUrls, 2)
                        cy.get('[data-cy="editSkillButton_skill1"]').click()
                    }
                    assertImages(editor, imageUrls, 2)
                    cy.get('[data-cy="aiButton"]').click()
                    cy.get('[data-cy="aiMsg-0"]').should('contain.text', isQuestion ? existingQuestionWelcomeMsg : existingDescWelcomeMsg)
                    return cy.wrap(imageUrls)
                })
            })
        }

        ['Markdown URL', 'escaped Markdown URL', 'HTML src', 'HTML srcset', 'reference URL', 'code'].forEach((placement) => {
            it(`rejects ${placement} in a generated ${context} without transmitting image data`, () => {
                openAssistant().then((imageUrls) => {
                    cy.get('[data-cy="instructionsInput"]').type(`AI image placement safety - ${placement}{enter}`)
                    cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('be.visible').and('contain.text', unsafePlacementMsg)
                    cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
                    cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"] img[src^="data:image/"]').should('not.exist')
                    if (isQuestion) {
                        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').should('not.exist')
                    }
                    cy.get('[data-cy="instructionsInput"]').should('be.enabled').and('have.focus')
                    assertNoImageDataRequests()

                    // Failed restoration must not consume mappings needed for a retry.
                    cy.get('[data-cy="instructionsInput"]').type(`${validPrompt}{enter}`)
                    cy.get('[data-cy="aiMsg-4"] [data-cy="finalSegment"]').should('contain.text', completedMsg)
                    assertImages('[data-cy="aiMsg-4"] [data-cy="generatedSegment"]', imageUrls)
                    assertNoImageDataRequests()
                })
            })
        })

        it(`renders, applies, saves, and reloads safe image placements in a generated ${context}`, () => {
            openAssistant().then((imageUrls) => {
                cy.get('[data-cy="instructionsInput"]').type(`${validPrompt}{enter}`)
                cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', completedMsg)
                const preview = '[data-cy="aiMsg-2"] [data-cy="generatedSegment"]'
                cy.get(preview).should('contain.text', safeText).and('contain.text', '---IMG-01')
                assertImages(preview, imageUrls)
                cy.get(`${preview} img`).first().should('have.attr', 'alt', 'First$&---IMG-99diagram')
                assertNoImageDataRequests()
                cy.intercept('POST', '/api/validation/description', (req) => {
                    if (req.body.value?.includes(safeText)) {
                        req.alias = 'generatedImageValidation'
                    }
                })
                cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled').click()
                cy.get('[data-cy="instructionsInput"]').should('not.exist')
                cy.get(editor).should('contain.text', safeText).and('contain.text', '---IMG-01')
                assertImages(editor, imageUrls)
                // Applying AI content triggers debounced validation; synchronize before saving.
                cy.wait('@generatedImageValidation')
                cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
                cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
                cy.get(saved).should('contain.text', safeText).and('contain.text', '---IMG-01')
                assertImages(saved, imageUrls)
                cy.reload()
                cy.get(saved).should('contain.text', safeText).and('contain.text', '---IMG-01')
                assertImages(saved, imageUrls)
            })
        })
    })
})
