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

    beforeEach(() => {

    });

    it('display slides on skill page', () => {
        cy.viewport(1500, 1600);
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { file: 'test-slides-1.pdf' })

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.navThroughSlides(true, '#proj1-skill1-slidesContainer')
    });

    it('download slides', () => {
        cy.viewport(1500, 1600);
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { file: 'test-slides-1.pdf' })

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1-slidesContainer #text-layer').contains('Sample slides')

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
