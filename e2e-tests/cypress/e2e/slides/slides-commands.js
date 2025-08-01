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

Cypress.Commands.add("visitSlidesConfPage", (projNum = 1, subjNum = 1, skillNum = 1) => {
    cy.intercept('GET', `/admin/projects/proj${projNum}/skills/skill${skillNum}/slides`).as('getSlidesProps')
    cy.intercept('GET', `/admin/projects/proj${projNum}/subjects/subj${subjNum}/skills/skill${skillNum}`).as('getSkillInfo')

    cy.visit(`/administrator/projects/proj${projNum}/subjects/subj${subjNum}/skills/skill${skillNum}/config-slides`);
    cy.wait('@getSlidesProps')
    cy.wait('@getSkillInfo')
    cy.get('.spinner-border').should('not.exist')
});

Cypress.Commands.add("navThroughSlides", (navBackToStart = false, containerId = '#proj1-skill1Container') => {
    cy.get('#pdfCanvasId').should('be.visible')
    cy.get('[data-cy="slidesFullscreenBtn"]').should('be.enabled')
    cy.get('[data-cy="slidesDownloadPdfBtn"]').should('be.enabled')

    cy.get('[data-cy="prevSlideBtn"]').should('be.disabled')
    cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
    cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
    cy.get(`${containerId}  #text-layer`).contains('Sample slides')

    cy.get('[data-cy="nextSlideBtn"]').click()
    cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
    cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
    cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
    cy.get(`${containerId}  #text-layer`).contains('First cool slide')

    cy.get('[data-cy="nextSlideBtn"]').click()
    cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 5')
    cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
    cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
    cy.get(`${containerId}  #text-layer`).contains('Second slide')

    cy.get('[data-cy="nextSlideBtn"]').click()
    cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 4 of 5')
    cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
    cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
    cy.get(`${containerId}  #text-layer`).contains('Third Slide')

    cy.get('[data-cy="nextSlideBtn"]').click()
    cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 5 of 5')
    cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
    cy.get('[data-cy="nextSlideBtn"]').should('be.disabled')
    cy.get(`${containerId}  #text-layer`).contains('Fourth Slide')

    if (navBackToStart) {
        cy.get('[data-cy="prevSlideBtn"]').click()
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 4 of 5')
        cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('Third Slide')

        cy.get('[data-cy="prevSlideBtn"]').click()
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 5')
        cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('Second slide')

        cy.get('[data-cy="prevSlideBtn"]').click()
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
        cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('First cool slide')

        cy.get('[data-cy="prevSlideBtn"]').click()
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('[data-cy="prevSlideBtn"]').should('be.disabled')
        cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get(`${containerId}  #text-layer`).contains('Sample slides')
    }
});

Cypress.Commands.add("navThroughSlides2", (containerId = '#proj1-skill1Container') => {
    cy.get('#pdfCanvasId').should('be.visible')
    cy.get('[data-cy="slidesFullscreenBtn"]').should('be.enabled')
    cy.get('[data-cy="slidesDownloadPdfBtn"]').should('be.enabled')

    cy.get('[data-cy="prevSlideBtn"]').should('be.disabled')
    cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
    cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 2')
    cy.get(`${containerId}  #text-layer`).contains('This will be first slide')

    cy.get('[data-cy="nextSlideBtn"]').click()
    cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 2')
    cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
    cy.get('[data-cy="nextSlideBtn"]').should('be.disabled')
    cy.get(`${containerId}  #text-layer`).contains('Second slide this is')
})
