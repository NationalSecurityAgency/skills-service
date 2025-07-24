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

describe('Configure Skill Slides Tests', () => {

    const testSlidesUrl = '/static/videos/test-slides-1.pdf'
    const slidesFile = 'test-slides-1.pdf';
    beforeEach(() => {
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/slides').as('getSlidesProps')
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('getSkillInfo')
        Cypress.Commands.add("visitSlidesConfPage", (projNum) => {
            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video');
            cy.wait('@getSlidesProps')
            cy.wait('@getSkillInfo')
            cy.get('.spinner-border').should('not.exist')
        });
    });

    it('configure slides url', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitSlidesConfPage();
        // cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        // cy.get('[data-cy="showExternalUrlBtn"]').click()
        // cy.get('[data-cy="showFileUploadBtn"]').should('be.visible')
        // cy.get('[data-cy="showExternalUrlBtn"]').should('not.exist')
        // cy.get('[data-cy="videoUrl"]').type('http://some.vid')
        // cy.get('[data-cy="videoCaptions"]').type(defaultCaption)
        // cy.get('[data-cy="videoTranscript"]').type('transcript')
        // cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        // cy.get('[data-cy="savedMsg"]')
        //
        // cy.visitVideoConfPage();
        // cy.get('[data-cy="videoUrl"]').should('have.value', 'http://some.vid')
        // cy.get('[data-cy="videoCaptions"]').should('have.value',defaultCaption)
        // cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        // cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        // cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')
    });


});
