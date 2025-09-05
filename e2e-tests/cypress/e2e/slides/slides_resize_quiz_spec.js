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

describe('Slides Resize On Quiz Run Tests', () => {

    it('resize slides setting', () => {
        cy.viewport(1500, 1600);
        cy.createQuizDef(1)
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf' }, true)

        cy.visitQuizSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="slidesResizeHandle"]')
            .trigger('mousedown')
            .trigger('mousemove')
            .trigger('mouseup', { force: true })

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /77\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#quiz1Container').should('have.attr', 'style').and('match', /width:\s*78[\d.]*px/)

        cy.get('[data-cy="slidesResizeHandle"]')
            .trigger('mousedown')
            .trigger('mousemove')
            .trigger('mouseup', { force: true })

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /76\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#quiz1Container').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)

        cy.get('[data-cy="updateSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsgSecondBtn"]')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /76\d/)

        // refresh and re-validate
        cy.visitQuizSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /76\d/)
        cy.get('#quiz1Container').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)
    });

    it('resize slides setting after exciting fullscreen mode', () => {
        cy.viewport(1500, 1600);
        cy.createQuizDef(1)
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf' }, true)

        cy.visitQuizSlidesConfPage();

        cy.wait(1000)
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="slidesFullscreenBtn"]').realClick()

        cy.wait(2000)
        cy.get('[data-cy="slidesFullscreenMsg"]')
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="slidesExitFullscreenBtn"]').realPress('Escape')

        cy.wait(1000)
        cy.get('[data-cy="slidesResizeHandle"]')
            .trigger('mousedown')
            .trigger('mousemove')
            .trigger('mouseup', { force: true })

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /77\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#quiz1Container').should('have.attr', 'style').and('match', /width:\s*78[\d.]*px/)

        cy.get('[data-cy="slidesResizeHandle"]')
            .trigger('mousedown')
            .trigger('mousemove')
            .trigger('mouseup', { force: true })

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /76\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#quiz1Container').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)

        cy.get('[data-cy="updateSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsgSecondBtn"]')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /76\d/)

    });

    it('resize slides using keyboard', () => {
        cy.viewport(1500, 1600);
        cy.createQuizDef(1)
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf' }, true)

        cy.visitQuizSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{enter}{leftArrow}')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /78\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#quiz1Container').should('have.attr', 'style').and('match', /width:\s*78[\d.]*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{enter}{leftArrow}')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /77\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#quiz1Container').should('have.attr', 'style').and('match', /width:\s*77[\d.]*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{enter}{rightArrow}')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /78\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#quiz1Container').should('have.attr', 'style').and('match', /width:\s*78[\d.]*px/)

        cy.get('[data-cy="updateSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsgSecondBtn"]')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /78\d/)

        // refresh and re-validate
        cy.visitQuizSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /78\d/)
        cy.get('#quiz1Container').should('have.attr', 'style').and('match', /width:\s*78[\d.]*px/)
    });

    it('slides on skills-display skill page uses configured default unless overridden by the user', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf', width: 250 }, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

        cy.get('[data-cy="startQuizAttempt"]').click()
        const containerId = '#quiz1Container'


        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1-slidesContainer #text-layer').contains('Sample slides')
        cy.get('#quiz1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*25[\d.]*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{rightArrow}')
        cy.get('#quiz1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*26[\d.]*px/)

        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1-slidesContainer #text-layer').contains('Sample slides')
        cy.get('#quiz1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*26[\d.]*px/)
    });

    it('slides on skills-display size is controlled by the user', () => {
        cy.viewport(1500, 1600);
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf'}, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1-slidesContainer #text-layer').contains('Sample slides')
        cy.get('#quiz1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*79[\d.]*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{leftArrow}')
        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{leftArrow}')
        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{leftArrow}')
        cy.get('#quiz1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)

        // user-set new size is used
        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.get('#quiz1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)
    });

    it('slides on skills-display still resize after exciting fullscreen mode', () => {
        cy.viewport(1500, 1600);
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf'}, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1-slidesContainer #text-layer').contains('Sample slides')
        cy.get('#quiz1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*79[\d.]*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').realClick()

        cy.get('[data-cy="slidesFullscreenMsg"]')
        cy.wait(500)
        cy.get('[data-cy="slidesFullscreenMsg"] [data-cy="slidesExitFullscreenBtn"]').realPress('Escape')

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{leftArrow}')
        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{leftArrow}')
        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{leftArrow}')
        cy.get('#quiz1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)
    });

});
