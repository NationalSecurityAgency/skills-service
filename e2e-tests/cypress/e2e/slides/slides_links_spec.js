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

    const stubFullscreen = () => {
        // Chrome disallows native fullscreen inside Cypress's runner iframe.
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
    };

    const loadActionSlides = () => {
        const url = `${Cypress.config('baseUrl')}/static/link-tests/slides/deck.pdf`;
        cy.intercept('GET', '**/static/link-tests/slides/deck.pdf', {
            fixture: 'slides-link-actions.pdf,null',
            headers: { 'Content-Type': 'application/pdf' },
        });
        cy.saveSlidesAttrs(1, 1, { url });
        cy.visitSlidesConfPage();
        cy.get('#text-layer').should('contain.text', 'Link actions slide 1');
    };

    const assertAbsoluteLinkAlignment = () => {
        cy.get(`${links}[href="https://skilltreeplatform.dev/"]`).should(($link) => {
            const linkRect = $link[0].getBoundingClientRect();
            const canvasRect = $link[0].ownerDocument.getElementById('pdfCanvasId').getBoundingClientRect();
            const scale = canvasRect.width / 600;
            expect(linkRect.left - canvasRect.left).to.be.closeTo(40 * scale, 1);
            expect(linkRect.top - canvasRect.top).to.be.closeTo(100 * scale, 1);
            expect(linkRect.width).to.be.closeTo(180 * scale, 1);
            expect(linkRect.height).to.be.closeTo(24 * scale, 1);
        });
    };

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
        loadActionSlides();
        assertAbsoluteLinkAlignment();
        cy.get(internalLink).then(($link) => {
            const originalWidth = $link[0].getBoundingClientRect().width;
            cy.get('[data-cy="slidesResizeHandle"]').trigger('keyup', { key: 'ArrowRight' });
            cy.get(internalLink).should(($resized) => {
                expect($resized[0].getBoundingClientRect().width).to.be.greaterThan(originalWidth);
            });
        });
        assertAbsoluteLinkAlignment();
        stubFullscreen();
        cy.get('[data-cy="slidesFullscreenBtn"]').click();
        cy.get('[data-cy="slidesExitFullscreenBtn"]').should('be.visible');
        // Wait for the fullscreen viewport, not just the fullscreen controls.
        cy.get('#pdfCanvasId').should(($canvas) => {
            const win = $canvas[0].ownerDocument.defaultView;
            expect($canvas[0].getBoundingClientRect().width)
                .to.be.closeTo(Math.min(win.innerWidth, win.innerHeight * 600 / 400), 1);
        });
        assertAbsoluteLinkAlignment();
        cy.get(internalLink).eq(3).focus();
        cy.realPress('Enter');
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 3');
        assertAbsoluteLinkAlignment();
        cy.get('[data-cy="slidesExitFullscreenBtn"]').click();
        cy.get('[data-cy="slidesFullscreenBtn"]').should('be.visible');
        assertAbsoluteLinkAlignment();
        cy.get(internalLink).eq(2).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 3');
    });

    it('activates named destinations with native Enter and focuses the destination text', () => {
        stubFullscreen();
        cy.get('[data-cy="slidesFullscreenBtn"]').click();
        cy.get('[data-cy="slidesExitFullscreenBtn"]').should('be.visible');
        cy.get(internalLink).focus();
        cy.realPress('Enter');
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 3');
        cy.get('#text-layer').should('be.focused').and('contain.text', 'Last slide');
    });

    it('supports standard named page actions including document boundaries', () => {
        loadActionSlides();
        cy.get(internalLink).eq(1).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 3');
        cy.get(internalLink).eq(0).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 3');
        cy.get(internalLink).eq(1).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 3');
        cy.get(internalLink).eq(3).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 3');
        cy.get(internalLink).eq(0).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 3');
        cy.get(internalLink).eq(2).click();
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 3');
    });

    it('resolves relative links against the PDF location and excludes unsafe URI and script actions', () => {
        loadActionSlides();
        cy.get(`${links}[href$="/slides/docs/intro.html"]`)
            .should('be.visible')
            .and('have.attr', 'href', `${Cypress.config('baseUrl')}/static/link-tests/slides/docs/intro.html`)
            .and('have.attr', 'target', '_blank')
            .and('have.attr', 'rel', 'noopener noreferrer');
        // Four supported named actions and two safe external links only.
        cy.get(links).should('have.length', 6);
        cy.get(`${links}[href^="javascript:"], ${links}[href^="data:"], ${links}[href^="file:"]`).should('not.exist');
        cy.window().its('pdfScriptExecuted').should('not.exist');
    });
});
