/*
 * Copyright 2024 SkillTree
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

describe('Configure Video Tests', () => {

    const defaultCaption = Cypress.env("defaultCaptions")
    const testVideo = '/static/videos/create-quiz.mp4'

    beforeEach(() => {
        cy.intercept('GET', '/admin/quiz-definitions/quiz1/questions/*/video').as('getVideoProps')

        Cypress.Commands.add("visitVideoConfPage", (quizId, questionNum) => {
            cy.visit(`/administrator/quizzes/${quizId}`);
            cy.get(`[data-cy="add-video-question-${questionNum}"]`).contains("Edit Audio/Video");
            cy.get(`[data-cy="add-video-question-${questionNum}"]`).click()
            cy.wait('@getVideoProps')
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

    it('resize video setting', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { videoUrl: testVideo, captions: defaultCaption, transcript: 'another' })
        cy.visitVideoConfPage('quiz1', 1)

        cy.wait(1000)
        cy.get('[data-cy="defaultVideoSize"]').contains('Not Configured')
        cy.get('[data-cy="updateVideoSettings"]').should('not.exist')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')

        cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
            .trigger('mousedown', )
            .trigger('mousemove', )
            .trigger('mouseup', { force: true })

        cy.useVideoDimensions().then((dimensionsResize1) =>  {
            cy.get('[data-cy="updateVideoSettings"]').should('exist')
            cy.get('[data-cy="unsavedVideoSizeChanges"]').should('exist')
            cy.url().then((url) => {
                const qNum = url.split('/')[7]
                cy.get(`#videoConfigFor-quiz1-${qNum}`).should('have.css', 'width', `${dimensionsResize1.width - 2}px`)

                cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
                    .trigger('mousedown', )
                    .trigger('mousemove', )
                    .trigger('mouseup', { force: true })
                cy.useVideoDimensions().then((dimensionsResize2) => {
                    cy.wrap(dimensionsResize1.width).should('be.gt', dimensionsResize2.width);
                    cy.get('[data-cy="updateVideoSettings"]').should('exist')
                    cy.get('[data-cy="unsavedVideoSizeChanges"]').should('exist')
                    cy.get(`#videoConfigFor-quiz1-${qNum}Container`).should('have.css', 'width', `${dimensionsResize2.width}px`)

                    cy.get('[data-cy="updateVideoSettings"]').click()
                    cy.get('[data-cy="savedMsg"]')
                    cy.get('[data-cy="defaultVideoSize"]').contains(`${dimensionsResize2.width} x ${dimensionsResize2.height}`)
                    cy.get(`#videoConfigFor-quiz1-${qNum}Container`).should('have.css', 'width', `${dimensionsResize2.width}px`)
                    cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
                })
            })
        })
    });

    it('resize the video using keyboard', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { videoUrl: testVideo, captions: defaultCaption, transcript: 'another' })
        cy.visitVideoConfPage('quiz1', 1)

        cy.wait(1000)
        cy.get('[data-cy="defaultVideoSize"]').contains('Not Configured')
        cy.get('[data-cy="updateVideoSettings"]').should('not.exist')
        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')

        cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
        cy.useVideoDimensions().then((dimensionsResize1) => {
            cy.url().then((url) => {
                const qNum = url.split('/')[7]
                cy.get('[data-cy="updateVideoSettings"]').should('exist')
                cy.get(`#videoConfigFor-quiz1-${qNum}Container`).should('have.css', 'width', `${dimensionsResize1.width}px`)
                cy.get('[data-cy="unsavedVideoSizeChanges"]').should('exist')


                cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
                cy.useVideoDimensions().then((dimensionsResize2) => {
                    cy.wrap(dimensionsResize1.width).should('be.gt', dimensionsResize2.width);
                    cy.get('[data-cy="updateVideoSettings"]').should('exist')
                    cy.get(`#videoConfigFor-quiz1-${qNum}Container`).should('have.css', 'width', `${dimensionsResize2.width}px`)
                    cy.get('[data-cy="unsavedVideoSizeChanges"]').should('exist')

                    cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{rightArrow}')
                    cy.useVideoDimensions().then((dimensionsResize3) => {
                        cy.wrap(dimensionsResize3.width).should('be.gt', dimensionsResize2.width);
                        cy.get('[data-cy="updateVideoSettings"]').click()
                        cy.get('[data-cy="savedMsg"]')
                        cy.get('[data-cy="defaultVideoSize"]').contains(`${dimensionsResize3.width} x ${dimensionsResize3.height}`)
                        cy.get('[data-cy="unsavedVideoSizeChanges"]').should('not.exist')
                        cy.get(`#videoConfigFor-quiz1-${qNum}Container`).should('have.css', 'width', `${dimensionsResize3.width}px`)
                    })
                })
            })
        })
    });

    it('video on skills-display skill page uses configured default unless overridden by the user', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { videoUrl: testVideo, captions: defaultCaption, transcript: 'another' })
        cy.visitVideoConfPage('quiz1', 1)

        cy.get('[data-cy="defaultVideoSize"]').contains('Not Configured')
        cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
        cy.get('[data-cy="clearVideoSettingsBtn"]').tab().type('{enter}{leftArrow}')
        cy.useVideoDimensions().then((dimensionsResize1) => {
            cy.get('[data-cy="updateVideoSettings"]').click()
            cy.get('[data-cy="savedMsg"]')
            cy.get(`.video-js`).should('have.css', 'width', `${dimensionsResize1.width - 2}px`)

            cy.visit('/progress-and-rankings/quizzes/quiz1');
            cy.get('[data-cy="startQuizAttempt"]').click()
            cy.get(`.video-js`).should('have.css', 'width', `${dimensionsResize1.width - 2}px`)

            // user overrides the video size
            cy.get('body').tab().type('{enter}{leftArrow}')
            cy.get(`.video-js`).invoke('css', 'width').then((widthInPx) => {
                // console.log(width);
                const playerWidth = parseInt(widthInPx.replace('px', ''))
                cy.wrap(playerWidth).should('be.lt', dimensionsResize1.width)
                cy.get(`.video-js`).should('have.css', 'width', `${playerWidth}px`)

                // user-set new size is used
                cy.visit('/progress-and-rankings/quizzes/quiz1');
                cy.get(`.video-js`).should('have.css', 'width', `${playerWidth}px`)
            })
        })
    });

    it('video on skills-display size is controlled by the user', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { videoUrl: testVideo, captions: defaultCaption, transcript: 'another' })

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get(`.video-js`)

        cy.get('body').tab().type('{enter}{leftArrow}')
        cy.get('body').tab().type('{enter}{leftArrow}')
        cy.get('body').tab().type('{enter}{leftArrow}')
        cy.get(`.video-js`).should('have.css', 'width', '885.25px')

        // user-set new size is used
        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get(`.video-js`).should('have.css', 'width', '885.25px')
    });

    it('playing the video removes the resize button', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { videoUrl: testVideo, captions: defaultCaption, transcript: 'another' })

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get(`.video-js`)

        cy.get('[data-cy="videoResizeHandle"]')
        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.get('[data-cy="videoResizeHandle"]').should('not.exist')
        cy.get('[data-cy="videoPlayer"] [title="Pause"]').click()
        cy.get('[data-cy="videoResizeHandle"]')
    });

    it('player will resize after playing then pausing the video', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { videoUrl: testVideo, captions: defaultCaption, transcript: 'another' })

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get(`.video-js`)

        cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
            .trigger('mousedown', )
            .trigger('mousemove', )
            .trigger('mouseup', { force: true })
        cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
            .trigger('mousedown', )
            .trigger('mousemove', )
            .trigger('mouseup', { force: true })
        cy.get(`.video-js`).should('have.css', 'width', '851px')

        cy.get('[data-cy="videoResizeHandle"]')
        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.get('[data-cy="videoResizeHandle"]').should('not.exist')
        cy.wait(1000)
        cy.get('[data-cy="videoPlayer"] [title="Pause"]').click()
        cy.get('[data-cy="videoResizeHandle"]')

        cy.get('[data-cy="videoResizeHandle"]').should('be.visible')
            .trigger('mousedown', )
            .trigger('mousemove', )
            .trigger('mouseup', { force: true })
        cy.get(`.video-js`).should('have.css', 'width', '834px')
    });

    it('audio only does not have a resize button', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { file: 'soundfile.wav', captions: '', transcript: 'another' })

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get(`.video-js`)

        cy.get('[data-cy="videoResizeHandle"]').should('not.exist')
    });
});
