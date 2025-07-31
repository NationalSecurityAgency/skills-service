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

    const slidesFile = 'test-slides-1.pdf';
    const testSlidesUrl = `/static/videos/${slidesFile}`
    const externalPdfUrl = `${Cypress.config().baseUrl}${testSlidesUrl}`
    beforeEach(() => {
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/slides').as('getSlidesProps')
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('getSkillInfo')
        Cypress.Commands.add("visitSlidesConfPage", () => {
            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-slides');
            cy.wait('@getSlidesProps')
            cy.wait('@getSkillInfo')
            cy.get('.spinner-border').should('not.exist')
        });


        Cypress.Commands.add("navThroughSlides", (navBackToStart = false) => {
            cy.get('#pdfCanvasId').should('be.visible')
            cy.get('[data-cy="slidesFullscreenBtn"]').should('be.enabled')
            cy.get('[data-cy="slidesDownloadPdfBtn"]').should('be.enabled')

            cy.get('[data-cy="prevSlideBtn"]').should('be.disabled')
            cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
            cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')

            cy.get('[data-cy="nextSlideBtn"]').click()
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
            cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
            cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
            cy.get('#proj1-skill1Container #text-layer').contains('First cool slide')

            cy.get('[data-cy="nextSlideBtn"]').click()
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 5')
            cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
            cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
            cy.get('#proj1-skill1Container #text-layer').contains('Second slide')

            cy.get('[data-cy="nextSlideBtn"]').click()
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 4 of 5')
            cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
            cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
            cy.get('#proj1-skill1Container #text-layer').contains('Third Slide')

            cy.get('[data-cy="nextSlideBtn"]').click()
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 5 of 5')
            cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
            cy.get('[data-cy="nextSlideBtn"]').should('be.disabled')
            cy.get('#proj1-skill1Container #text-layer').contains('Fourth Slide')

            if (navBackToStart) {
                cy.get('[data-cy="prevSlideBtn"]').click()
                cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 4 of 5')
                cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
                cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
                cy.get('#proj1-skill1Container #text-layer').contains('Third Slide')

                cy.get('[data-cy="prevSlideBtn"]').click()
                cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 3 of 5')
                cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
                cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
                cy.get('#proj1-skill1Container #text-layer').contains('Second slide')

                cy.get('[data-cy="prevSlideBtn"]').click()
                cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 5')
                cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
                cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
                cy.get('#proj1-skill1Container #text-layer').contains('First cool slide')

                cy.get('[data-cy="prevSlideBtn"]').click()
                cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
                cy.get('[data-cy="prevSlideBtn"]').should('be.disabled')
                cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
                cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')
            }
        });

        Cypress.Commands.add("navThroughSlides2", () => {
            cy.get('#pdfCanvasId').should('be.visible')
            cy.get('[data-cy="slidesFullscreenBtn"]').should('be.enabled')
            cy.get('[data-cy="slidesDownloadPdfBtn"]').should('be.enabled')

            cy.get('[data-cy="prevSlideBtn"]').should('be.disabled')
            cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 2')
            cy.get('#proj1-skill1Container #text-layer').contains('This will be first slide')

            cy.get('[data-cy="nextSlideBtn"]').click()
            cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 2 of 2')
            cy.get('[data-cy="prevSlideBtn"]').should('be.enabled')
            cy.get('[data-cy="nextSlideBtn"]').should('be.disabled')
            cy.get('#proj1-skill1Container #text-layer').contains('Second slide this is')
        })

    });

    it('configure slides external url', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitSlidesConfPage();
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').click()

        cy.get('[data-cy="showFileUploadBtn"]').should('be.visible')
        cy.get('[data-cy="showExternalUrlBtn"]').should('not.exist')
        cy.get('[data-cy="pdfUrl"]').type(externalPdfUrl)
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.navThroughSlides(true)

        cy.visitSlidesConfPage();
        cy.get('[data-cy="pdfUrl"]').should('have.value', externalPdfUrl)
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="slidesFullscreenBtn"]').should('be.enabled')
        cy.get('[data-cy="slidesDownloadPdfBtn"]').should('be.enabled')
        cy.get('[data-cy="prevSlideBtn"]').should('be.disabled')
        cy.get('[data-cy="nextSlideBtn"]').should('be.enabled')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')

        cy.navThroughSlides()
    });

    it('upload slides and preview', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitSlidesConfPage();
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })

        cy.get('[data-cy="saveSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.navThroughSlides(true)
    })

    it('reset existing slides and upload new slides', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { file: 'test-slides-1.pdf' })

        cy.visitSlidesConfPage();

        cy.get('[data-cy="videoFileInput"]').should('have.value', 'test-slides-1.pdf')
        cy.navThroughSlides()

        cy.get('[data-cy="resetBtn"]').click()
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-2.pdf`,  { force: true })

        cy.get('[data-cy="saveSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.navThroughSlides2()
    })

    it('replace existing uploaded slides with an external url', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { file: 'test-slides-2.pdf' })

        cy.visitSlidesConfPage();

        cy.get('[data-cy="videoFileInput"]').should('have.value', 'test-slides-2.pdf')
        cy.navThroughSlides2()

        cy.get('[data-cy="showExternalUrlBtn"]').click()

        cy.get('[data-cy="showFileUploadBtn"]').should('be.visible')
        cy.get('[data-cy="showExternalUrlBtn"]').should('not.exist')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="pdfUrl"]').type(externalPdfUrl)
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.navThroughSlides()

        cy.visitSlidesConfPage()
        cy.navThroughSlides()
    })

    it('replace existing external url with uploaded slides', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { url: externalPdfUrl })

        cy.visitSlidesConfPage();
        cy.get('[data-cy="pdfUrl"]').should('have.value', externalPdfUrl)
        cy.get('[data-cy="showFileUploadBtn"]').should('be.visible')
        cy.get('[data-cy="showExternalUrlBtn"]').should('not.exist')
        cy.navThroughSlides()

        cy.get('[data-cy="showFileUploadBtn"]').click()

        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-2.pdf`,  { force: true })

        cy.get('[data-cy="saveSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.navThroughSlides2()
    })

    it('clear attributes when external url is configured', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { url: externalPdfUrl })

        cy.visitSlidesConfPage();

        cy.get('[data-cy="pdfUrl"]').should('have.value', externalPdfUrl)
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="clearSlidesSettingsBtn"]').click()
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]').contains('Slide settings will be permanently cleared')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcacceptbutton"]').click()

        cy.get('[data-cy="pdfUrl"]').should('not.exist')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.enabled')
    })

    it('clear attributes when file was uploaded', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveSlidesAttrs(1, 1, { file: 'test-slides-1.pdf' })

        cy.visitSlidesConfPage();

        cy.get('[data-cy="videoFileInput"]').should('have.value', 'test-slides-1.pdf')
        cy.get('[data-cy="pdfUrl"]').should('not.exist')
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#proj1-skill1Container #text-layer').contains('Sample slides')

        cy.get('[data-cy="clearSlidesSettingsBtn"]').click()
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]').contains('Slide settings will be permanently cleared')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcacceptbutton"]').click()

        cy.get('[data-cy="pdfUrl"]').should('not.exist')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.enabled')
    })

    it('only allow valid types for uploaded slides', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitSlidesConfPage();
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/create-quiz.mp4`,  { force: true })

        cy.get('[data-cy="slidesFileError"]').contains('Unsupported [video/mp4] file type')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
    })

    it('validate maximum size of the video', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxAttachmentSize = 1024 * 3;
                res.send(conf);
            });
        }).as('loadConfig');

        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitSlidesConfPage();
        cy.wait('@loadConfig')

        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`, {force: true})

        cy.get('[data-cy="slidesFileError"]').contains('File exceeds maximum size of 3 KB')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
    })
})
