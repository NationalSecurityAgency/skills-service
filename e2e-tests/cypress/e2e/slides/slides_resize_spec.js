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
        Cypress.Commands.add("useVideoDimensions", () => {
            return cy.get('[data-cy="defaultVideoSize"]').invoke('text').then((text) => {
                // cy.get('[data-cy="defaultVideoSize"]').contains('705 x 488')
                const numbers = text.split(' x ');
                return {width: parseInt(numbers[0]), height: parseInt(numbers[1])}
            })
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

        cy.get('[data-cy="defaultVideoSize"]').contains('777')
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*782.*px/)

        cy.get('[data-cy="slidesResizeHandle"]')
            .trigger('mousedown')
            .trigger('mousemove')
            .trigger('mouseup', { force: true })

        cy.get('[data-cy="defaultVideoSize"]').contains('761')
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*766.*px/)

        cy.get('[data-cy="updateSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsgSecondBtn"]')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
        cy.get('[data-cy="defaultVideoSize"]').contains('761')

        // refresh and re-validate
        cy.visitSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="defaultVideoSize"]').contains('761')
        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*766.*px/)
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

        cy.get('[data-cy="defaultVideoSize"]').contains('784')
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*789.*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{enter}{leftArrow}')

        cy.get('[data-cy="defaultVideoSize"]').contains('774')
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*779.*px/)

        cy.get('[data-cy="slidesFullscreenBtn"]').tab().type('{enter}{rightArrow}')

        cy.get('[data-cy="defaultVideoSize"]').contains('784')
        cy.get('[data-cy="unsavedVideoSizeChanges"]')
        cy.get('[data-cy="updateSlidesSettingsBtn"]').should('be.enabled')

        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*789.*px/)

        cy.get('[data-cy="updateSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsgSecondBtn"]')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
        cy.get('[data-cy="defaultVideoSize"]').contains('784')

        // refresh and re-validate
        cy.visitSlidesConfPage();

        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="defaultVideoSize"]').contains('784')
        cy.get('#proj1-skill1Container').should('have.attr', 'style').and('match', /width:\s*789.*px/)

    });

    // it('video on skills-display skill page uses configured default unless overridden by the user', () => {
    //     cy.createProject(1)
    //     cy.createSubject(1, 1);
    //     cy.createSkill(1, 1, 1)
    //     const vid = { file: 'create-subject.webm', captions: defaultCaption, transcript: 'great' }
    //     cy.saveVideoAttrs(1, 1, vid)
    //     cy.visitVideoConfPage();
    //
    //     cy.get('[data-cy="defaultVideoSize"]').contains('Not Configured')
    //     cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
    //     cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
    //     cy.useVideoDimensions().then((dimensionsResize1) => {
    //         cy.get('[data-cy="updateVideoSettings"]').click()
    //         cy.get('[data-cy="savedMsg"]')
    //         cy.get('#videoConfigFor-proj1-skill1Container').should('have.css', 'width', `${dimensionsResize1.width}px`)
    //
    //         cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
    //         cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', `${dimensionsResize1.width}px`)
    //
    //         // user overrides the video size
    //         cy.get('[data-cy="contactOwnerBtn"]').tab().type('{enter}{leftArrow}')
    //         cy.get('#skillVideoFor-proj1-skill1Container').invoke('css', 'width').then((widthInPx) => {
    //             // console.log(width);
    //             const playerWidth = parseInt(widthInPx.replace('px', ''))
    //             cy.wrap(playerWidth).should('be.lt', dimensionsResize1.width)
    //             cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', `${playerWidth}px`)
    //
    //             // user-set new size is used
    //             cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
    //             cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', `${playerWidth}px`)
    //         })
    //     })
    // });
    //
    // it('video on skills-display size is controlled by the user', () => {
    //     cy.createProject(1)
    //     cy.createSubject(1, 1);
    //     cy.createSkill(1, 1, 1)
    //     const vid = { file: 'create-subject.webm', captions: defaultCaption, transcript: 'great' }
    //     cy.saveVideoAttrs(1, 1, vid)
    //
    //     cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
    //     cy.get('#skillVideoFor-proj1-skill1Container')
    //
    //     cy.get('[data-cy="contactOwnerBtn"]').tab().type('{enter}{leftArrow}')
    //     cy.get('[data-cy="contactOwnerBtn"]').tab().type('{enter}{leftArrow}')
    //     cy.get('[data-cy="contactOwnerBtn"]').tab().type('{enter}{leftArrow}')
    //     cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', '771px')
    //
    //     // user-set new size is used
    //     cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
    //     cy.get('#skillVideoFor-proj1-skill1Container').should('have.css', 'width', '771px')
    // });
    //

});
