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

describe('Slides Quiz Accessibility Tests', () => {

    const runWithDarkMode = ['', ' - dark mode']
    runWithDarkMode.forEach((darkMode) => {
        it(`skill page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.viewport(1500, 1600);
            cy.createQuizDef(1);
            cy.createQuizQuestionDef(1, 1);
            cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf' }, true)

            cy.visit('/progress-and-rankings/quizzes/quiz1');
            cy.get('[data-cy="quizSplashScreen"]').contains('This is quiz 1')

            cy.get('[data-cy="startQuizAttempt"]').click()
            cy.get('#pdfCanvasId').should('be.visible')
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
            cy.get('#quiz1-slidesContainer #text-layer').contains('Sample slides')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        const slidesFile = 'test-slides-1.pdf';
        const testSlidesUrl = `/static/videos/${slidesFile}`
        const externalPdfUrl = `${Cypress.config().baseUrl}${testSlidesUrl}`

        it(`admin config page with uploaded file ${darkMode}`, () => {
            cy.viewport(1500, 1600);
            cy.createQuizDef(1)
            cy.saveSlidesAttrs(1, null, { file: 'test-slides-1.pdf', width: 700 }, true)

            cy.visitQuizSlidesConfPage();

            cy.get('#pdfCanvasId').should('be.visible')
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
            cy.get('#quiz1Container #text-layer').contains('Sample slides')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        })

        it(`admin config page with external url ${darkMode}`, () => {
            cy.viewport(1500, 1600);
            cy.createQuizDef(1)
            cy.saveSlidesAttrs(1, null, { url: externalPdfUrl }, true)

            cy.visitQuizSlidesConfPage();

            cy.get('[data-cy="pdfUrl"]').should('have.value', externalPdfUrl)
            cy.get('#pdfCanvasId').should('be.visible')
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
            cy.get('#quiz1Container #text-layer').contains('Sample slides')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        })
    })

});
