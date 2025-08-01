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

describe('Slides Resize Tests', () => {


    beforeEach(() => {
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/slides').as('getSlidesProps')
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('getSkillInfo')
        Cypress.Commands.add("visitSlidesConfPage", () => {
            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-slides');
            cy.wait('@getSlidesProps')
            cy.wait('@getSkillInfo')
            cy.get('.spinner-border').should('not.exist')
        });
    });

    it('resize slides setting', () => {
        cy.viewport(1500, 1600);
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { file: 'test-slides-1.pdf' })
        cy.visitSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="slidesResizeHandle"]')
            .trigger('mousedown')
            .trigger('mousemove')
            .trigger('mouseup', { force: true })

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /77\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*78[\d.]*px/)

        cy.get('[data-cy="slidesResizeHandle"]')
            .trigger('mousedown')
            .trigger('mousemove')
            .trigger('mouseup', { force: true })

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /76\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)

        cy.get('[data-cy="updateSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsgSecondBtn"]')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /76\d/)

        // refresh and re-validate
        cy.visitSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /76\d/)
        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)
    });

    it('resize slides using keyboard', () => {
        cy.viewport(1500, 1600);
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { file: 'test-slides-1.pdf' })
        cy.visitSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{enter}{leftArrow}')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /78\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*78[\d.]*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{enter}{leftArrow}')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /77\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*77[\d.]*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{enter}{rightArrow}')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /78\d/)
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*78[\d.]*px/)

        cy.get('[data-cy="updateSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsgSecondBtn"]')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /78\d/)

        // refresh and re-validate
        cy.visitSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="defaultVideoSize"]').invoke('text').and('match', /78\d/)
        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*78[\d.]*px/)
    });

    it('slides on skills-display skill page uses configured default unless overridden by the user', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { file: 'test-slides-1.pdf', width: 250 })

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1-slidesContainer #text-layer').contains('Sample slides')
        cy.get('#proj1-skill1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*25[\d.]*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{rightArrow}')
        cy.get('#proj1-skill1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*26[\d.]*px/)

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1-slidesContainer #text-layer').contains('Sample slides')
        cy.get('#proj1-skill1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*26[\d.]*px/)
    });

    it('slides on skills-display size is controlled by the user', () => {
        cy.viewport(1500, 1600);
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { file: 'test-slides-1.pdf' })

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1-slidesContainer #text-layer').contains('Sample slides')
        cy.get('#proj1-skill1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*79[\d.]*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{leftArrow}')
        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{leftArrow}')
        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{leftArrow}')
        cy.get('#proj1-skill1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)

        // user-set new size is used
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#proj1-skill1-slidesContainer').should('have.attr', 'style').and('match', /width:\s*76[\d.]*px/)
    });


});
