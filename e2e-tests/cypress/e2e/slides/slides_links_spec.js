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
import './slides-commands';

describe('Slide hyperlinks', () => {
    const links = '[data-cy="slidesAnnotationLayer"] a';
    const internalLink = '[data-cy="slidesAnnotationLayer"] [data-internal-link] a';

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.saveSlidesAttrs(1, 1, { file: 'slides-with-links.pdf' });
        cy.visitSlidesConfPage();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 3');
    });

    it('renders safe external links and navigates internal destinations without stale links', () => {
        cy.get('[data-cy="slidesAnnotationLayer"] section:not([data-internal-link]) a')
            .should('be.visible')
            .and('have.attr', 'href', 'https://skilltreeplatform.dev/')
            .and('have.attr', 'target', '_blank')
            .and('have.attr', 'rel', 'noopener noreferrer');
        cy.get(internalLink).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 3');
        cy.get('#text-layer').should('contain.text', 'Last slide');
        cy.get(links).should('have.length', 1);
        cy.get(internalLink).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 3');
        cy.get('[data-cy="nextSlideBtn"]').click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 3');
        cy.get(links).should('not.exist');
    });

    it('keeps links aligned after resizing and preserves keyboard activation in fullscreen', () => {
        cy.get(internalLink).then(($link) => {
            const originalWidth = $link[0].getBoundingClientRect().width;
            cy.get('[data-cy="slidesResizeHandle"]').trigger('keyup', { key: 'ArrowRight' });
            cy.get(internalLink).should(($resized) => {
                expect($resized[0].getBoundingClientRect().width).to.be.greaterThan(originalWidth);
            });
        });
        cy.get(internalLink).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 3');
        cy.get(internalLink).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 3');
        // Chrome disallows native fullscreen inside Cypress's runner iframe.
        // Model the API and its change event rather than ignoring promise errors.
        cy.document().then((doc) => {
            let fullscreenElement = null;
            Object.defineProperty(doc, 'fullscreenElement', {
                configurable: true,
                get: () => fullscreenElement,
            });
            cy.stub(doc.getElementById('proj1-skill1Container'), 'requestFullscreen').callsFake(function () {
                fullscreenElement = this;
                doc.dispatchEvent(new Event('fullscreenchange'));
                return Promise.resolve();
            });
            cy.stub(doc, 'exitFullscreen').callsFake(() => {
                fullscreenElement = null;
                doc.dispatchEvent(new Event('fullscreenchange'));
                return Promise.resolve();
            });
        });
        cy.get('[data-cy="slidesFullscreenBtn"]').click();
        cy.get('[data-cy="slidesExitFullscreenBtn"]').should('be.visible');
        cy.get(internalLink).focus().trigger('keydown', { key: 'Enter' }).then(($link) => {
            // Cypress synthetic key events do not perform native anchor activation.
            $link[0].click();
        });
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 3');
        cy.get('[data-cy="slidesExitFullscreenBtn"]').click();
        cy.get(internalLink).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 3');
    });
});
