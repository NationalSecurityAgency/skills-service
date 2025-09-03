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

describe('Configure slides and SkillTree Features Tests', () => {

    beforeEach(() => {
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/slides').as('getSlidesProps')
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/slides').as('getSlidesPropsProj2')
    });

    it('slides upload warning message is present when configured', () => {
        cy.createQuizDef(1)

        const msg = 'Friendly Reminder: Only safe slides please'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.slidesUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visitQuizSlidesConfPage();
        cy.wait('@loadConfig')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.get('[data-cy="slidesUploadWarningMessage"]').contains(msg)

        cy.get('[data-cy="saveSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1Container #text-layer').contains('Sample slides')

        // click away and return
        cy.get('[data-cy="navLine-Questions"]').click()
        cy.get('[data-cy="noQuestionsYet"]').should('be.visible')

        cy.get('[data-cy="nav-Slides"]').click()
        cy.get('#pdfCanvasId').should('be.visible')
        cy.get('[data-cy="currentSlideMsg"]').should('have.text', 'Slide 1 of 5')
        cy.get('#quiz1Container #text-layer').contains('Sample slides')
        cy.get('[data-cy="slidesUploadWarningMessage"]').should('not.exist')
    });

    it('slides upload warning message supports community.descriptor property ', () => {
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
                cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.logout();
                cy.login(vars.defaultUser, vars.defaultPass);
            });
        cy.createQuizDef(1)
        cy.createQuizDef(2, {enableProtectedUserCommunity: true})

        const msg = 'Friendly Reminder: Only safe slides please for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.slidesUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visitQuizSlidesConfPage();
        cy.wait('@loadConfig')

        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
        cy.get('#pdfCanvasId').should('not.exist')

        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.get('[data-cy="slidesUploadWarningMessage"]').contains("Friendly Reminder: Only safe slides please for All Dragons")

        // nav to project 2
        cy.get('[data-cy="breadcrumb-Quizzes"]').click()
        cy.get('[data-cy="managesQuizBtn_quiz2"]').click()
        cy.get('[data-cy="nav-Slides"]').click();
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.get('[data-cy="slidesUploadWarningMessage"]').contains("Friendly Reminder: Only safe slides please for Divine Dragon")

        // nav to quiz 1
        cy.get('[data-cy="breadcrumb-Quizzes"]').click()
        cy.get('[data-cy="managesQuizBtn_quiz1"]').click()
        cy.get('[data-cy="nav-Slides"]').click();
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.get('[data-cy="slidesUploadWarningMessage"]').contains("Friendly Reminder: Only safe slides please for All Dragons")

        // straight to quiz 2
        cy.visit('/administrator/quizzes/quiz2/config-slides');
        cy.wait('@getSlidesPropsProj2')
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.get('[data-cy="slidesUploadWarningMessage"]').contains("Friendly Reminder: Only safe slides please for Divine Dragon")
    });

    it('slides upload warning message uses community.descriptor after quiz\'s UC protection is raised', () => {
        cy.intercept('/admin/quiz-definitions/quiz1/settings').as('getQuiz1Settings')
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
                cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.logout();
                cy.login(vars.defaultUser, vars.defaultPass);
            });
        cy.createQuizDef(1)

        const msg = 'Friendly Reminder: Only safe slides please for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.slidesUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visitQuizSlidesConfPage();
        cy.wait('@loadConfig')
        cy.wait('@getQuiz1Settings')

        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.get('[data-cy="slidesUploadWarningMessage"]').contains("Friendly Reminder: Only safe slides please for All Dragons")

        // change UC
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click({force: true})
        cy.get('[data-cy="restrictCommunityControls"]').contains('Please note that once the restriction is enabled it cannot be lifted/disabled')
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.wait('@getQuiz1Settings')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="resetBtn"]').click()
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.get('[data-cy="slidesUploadWarningMessage"]').contains("Friendly Reminder: Only safe slides please for Divine Dragon")
    });

    it('slides upload warning message uses community.descriptor for a brand new quiz with UC protection', () => {
        cy.intercept('/admin/quiz-definitions/quiz1/settings').as('getQuiz1Settings')
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/slides').as('getSlidesProps1')
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
                cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.logout();
                cy.login(vars.defaultUser, vars.defaultPass);
            });


        const msg = 'Friendly Reminder: Only safe slides please for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.slidesUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visit('/administrator/quizzes/')
        cy.wait('@loadConfig')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-cy="quizName"]').type('quiz1')
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click({force: true})
        cy.get('[data-cy="restrictCommunityControls"]').contains('Please note that once the restriction is enabled it cannot be lifted/disabled')
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')

        cy.get('[data-cy="managesQuizBtn_quiz1"]').click()
        cy.wait('@getQuiz1Settings')

        cy.get('[data-cy="nav-Slides"]').click()
        cy.wait('@getSlidesProps1')
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.get('[data-cy="slidesUploadWarningMessage"]').contains("Friendly Reminder: Only safe slides please for Divine Dragon")
    });

    it('slides upload warning message is not present when NOT configured', () => {
        cy.createQuizDef(1)

        cy.visitQuizSlidesConfPage();
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.wait(5000)
        cy.get('[data-cy="slidesUploadWarningMessage"]').should('not.exist')
    });

    it('throw an error if slides warning messages has community property but community setting is not available', () => {
        const msg = 'Friendly Reminder: Only safe slides please for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.slidesUploadWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.intercept('POST', '/public/log').as('reportError')

        cy.intercept('GET', '/admin/quiz-definitions/quiz1/settings', (req) => {
            req.reply((res) => {
                res.send([]);
            });
        }).as('loadQuizSettings');

        cy.createQuizDef(1)


        cy.visitQuizSlidesConfPage();
        cy.wait('@loadQuizSettings')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })
        cy.wait('@reportError')
        cy.get('[data-cy="errorPage"]').contains('something went wrong')
    });

});