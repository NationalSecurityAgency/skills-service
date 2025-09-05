/*
 * Copyright 2025 SkillTree
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
import './slides-commands';

describe('Slides Resize Tests', () => {

    it('display slides on quiz run', () => {
        cy.viewport(1500, 1600);
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf' }, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.navThroughSlides(true, '#quiz1-slidesContainer')
    });

    it('display slides on survey run', () => {
        cy.viewport(1500, 1600);
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf' }, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is survey 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.navThroughSlides(true, '#quiz1-slidesContainer')
    });

    it('display slides on quiz run and navigate in full screen mode', () => {
        cy.viewport(1500, 1600);
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf' }, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        const containerId = '#quiz1-slidesContainer'
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="slidesFullscreenBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesDownloadPdfBtn"]').should('be.enabled')

        cy.get('[data-cy="slidesFullscreenBtn"]').realClick()
        cy.wait(2000)
        cy.get('[data-cy="slidesFullscreenMsg"]')

        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.disabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get(`${containerId}  #text-layer`).contains('Sample slides')

        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').realHover()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('First cool slide')

        cy.get('[data-cy="slidesFullscreenMsg"]').realHover()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('Second slide')

        cy.get('[data-cy="slidesFullscreenMsg"]').realHover()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 4 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('Third Slide')

        cy.get('[data-cy="slidesFullscreenMsg"]').realHover()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 5 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.disabled')
        cy.get(`${containerId}  #text-layer`).contains('Fourth Slide')

        cy.get('[data-cy="slidesFullscreenMsg"]').realHover()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 4 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('Third Slide')

        cy.get('[data-cy="slidesFullscreenMsg"]').realHover()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('Second slide')

        cy.get('[data-cy="slidesFullscreenMsg"]').realHover()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('First cool slide')

        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.disabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('Sample slides')

        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="slidesExitFullscreenBtn"]').realPress('Escape')
        cy.get('[data-cy="slidesFullscreenMsg"]').should('not.exist')
    });

    it('enter and exit fullscreen', () => {
        cy.viewport(1500, 1600);
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf' }, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()

        const containerId = '#quiz1-slidesContainer'
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="slidesFullscreenBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesDownloadPdfBtn"]').should('be.enabled')

        cy.get('[data-cy="slidesFullscreenBtn"]').realClick()
        cy.wait(2000)
        cy.get('[data-cy="slidesFullscreenMsg"]')

        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.disabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get(`${containerId}  #text-layer`).contains('Sample slides')

        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').realHover()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('First cool slide')

        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="slidesExitFullscreenBtn"]').realPress('Escape')
        cy.get('[data-cy="slidesFullscreenMsg"]').should('not.exist')
        cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
        cy.get(`${containerId}  #text-layer`).contains('First cool slide')

        cy.get('[data-cy="slidesFullscreenBtn"]').realClick()
        cy.wait(2000)
        cy.get('[data-cy="slidesFullscreenMsg"]')

        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('First cool slide')

        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="slidesExitFullscreenBtn"]').click()
        cy.get('[data-cy="slidesFullscreenMsg"]').should('not.exist')
        cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
        cy.get(`${containerId}  #text-layer`).contains('First cool slide')
    });

    it('download slides', () => {
        cy.viewport(1500, 1600);
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf' }, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        const containerId = '#quiz1-slidesContainer'
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get(`${containerId} #text-layer`).contains('Sample slides')

        cy.get('[data-cy="slidesDownloadPdfBtn"]').click()

        // a bug to watch out for: https://github.com/cypress-io/cypress/issues/25443
        cy.fixture('test-slides-1.pdf').then(fixture => {
            cy.readFile('cypress/downloads/test-slides-1.pdf').then(download => {
                // expect(fixture).to.eq(download)
                assert(fixture === download, 'files are matching')
            })
        })

    });

});
