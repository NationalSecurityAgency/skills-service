/*
 * Copyright 2020 SkillTree
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
    const videoFile = 'create-subject.webm';
    const audioFile = 'soundfile.wav';
    const captionsButtonSelector = '.vjs-menu-button  button[title="Captions"]'
    beforeEach(() => {
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/video').as('getVideoProps')
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('getSkillInfo')
        Cypress.Commands.add("visitVideoConfPage", (projNum) => {
            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video');
            cy.wait('@getVideoProps')
            cy.wait('@getSkillInfo')
            cy.get('.spinner-border').should('not.exist')
        });
    });

    it('configure Video URL, transcript and captions', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showExternalUrlBtn"]').click()
        cy.get('[data-cy="showFileUploadBtn"]').should('be.visible')
        cy.get('[data-cy="showExternalUrlBtn"]').should('not.exist')
        cy.get('[data-cy="videoUrl"]').type('http://some.vid')
        cy.get('[data-cy="videoCaptions"]').type(defaultCaption)
        cy.get('[data-cy="videoTranscript"]').type('transcript')
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.visitVideoConfPage();
        cy.get('[data-cy="videoUrl"]').should('have.value', 'http://some.vid')
        cy.get('[data-cy="videoCaptions"]').should('have.value',defaultCaption)
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')
    });

    it('upload a video, set transcript and captions, preview the video', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/${videoFile}`,  { force: true })
        // cy.get('[data-cy="videoFileUpload"]').attachFile({ filePath: videoFile, encoding: 'binary'});
        cy.get('[data-cy="videoCaptions"]').type(defaultCaption, {delay: 0})
        cy.get('[data-cy="videoTranscript"]').type('transcript', {delay: 0})
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')

        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', videoFile)
        cy.get('[data-cy="videoCaptions"]').should('have.value',defaultCaption)
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTotalDuration"]').should('have.text', '7 seconds')
        cy.get('[data-cy="videoPreviewCard"] [title="Play Video"]').click()

        // video is 7 seconds
        cy.wait(7000)
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '100%')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTimeWatched"]').should('have.text', '7 seconds')

        // refresh and re-validate
        cy.visitVideoConfPage();
        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', videoFile)
        cy.get('[data-cy="videoCaptions"]').should('have.value',defaultCaption)
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTotalDuration"]').should('have.text', '7 seconds')
        cy.get('[data-cy="videoPreviewCard"] [title="Play Video"]').click()
        cy.wait(7000)
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '100%')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTimeWatched"]').should('have.text', '7 seconds')
        cy.get(captionsButtonSelector)
    });

    it('clear attributes', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vidAttr = { videoUrl: 'http://someurl.mp4', captions: 'some', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.visitVideoConfPage();

        cy.get('[data-cy="videoUrl"]').should('have.value', vidAttr.videoUrl)
        cy.get('[data-cy="videoCaptions"]').should('have.value', vidAttr.captions)
        cy.get('[data-cy="videoTranscript"]').should('have.value', vidAttr.transcript)

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="clearVideoSettingsBtn"]').click()
        cy.get('.p-dialog-footer').contains('Yes, Do clear').click()

        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoUrl"]').should('not.exist')
        cy.get('[data-cy="videoFileUpload"]').should('exist')
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')

        cy.visitVideoConfPage();
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoUrl"]').should('not.exist')
        cy.get('[data-cy="videoFileUpload"]').should('exist')
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')
    });

    it('fill caption example', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();
        cy.get('[data-cy="fillCaptionsExamples"]').click()
        cy.get('[data-cy="videoCaptions"]').should('contain.value', 'WEBVTT')
        cy.get('[data-cy="fillCaptionsExamples"]').should('not.exist')
    });

    it('preview video with Video URL', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="showExternalUrlBtn"]').click()
        cy.get('[data-cy="videoUrl"]').type(testVideo, {delay: 0})

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoPreviewCard"]').should('not.exist')
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '0%')
        cy.get('[data-cy="videoPreviewCard"] [title="Play Video"]').click()
        // video is 15 seconds
        cy.wait(15000)
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '100%')
        cy.get(captionsButtonSelector).should('not.be.visible')
    });

    it('discard changes - Video URL is configured', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.saveVideoAttrs(1, 1, { videoUrl: testVideo })
        cy.visitVideoConfPage();

        cy.get('[data-cy="videoUrl"]').should('have.value', testVideo)
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')

        // modify
        cy.get('[data-cy="showFileUploadBtn"]').click();
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/${videoFile}`,  { force: true })
        // cy.get('[data-cy="videoFileUpload"]').attachFile({ filePath: videoFile, encoding: 'binary'});
        cy.get('[data-cy="videoCaptions"]').type(defaultCaption, {delay: 0})
        cy.get('[data-cy="videoTranscript"]').type('transcript', {delay: 0})

        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', videoFile)
        cy.get('[data-cy="videoCaptions"]').should('have.value',defaultCaption)
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')

        // discard changes
        cy.get('[data-cy="discardChangesBtn"]').click()
        cy.get('[data-cy="videoUrl"]').should('have.value', testVideo)
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="showExternalUrlBtn"]').should('not.exist')
        cy.get('[data-cy="showFileUploadBtn"]').should('exist')
    });

    it('discard changes - Video uploaded, captions and transcript are configured', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vid = { file: 'create-subject.webm', captions: 'cool caption', transcript: 'great' }
        cy.saveVideoAttrs(1, 1, vid)
        cy.visitVideoConfPage();

        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', vid.file)
        cy.get('[data-cy="videoCaptions"]').should('have.value', vid.captions)
        cy.get('[data-cy="videoTranscript"]').should('have.value', vid.transcript)

        // modify all fields
        cy.get('[data-cy="showExternalUrlBtn"]').click()
        cy.get('[data-cy="videoUrl"]').type(testVideo, {delay: 0})
        cy.get('[data-cy="videoCaptions"]').clear()
        cy.get('[data-cy="videoTranscript"]').clear()

        cy.get('[data-cy="videoUrl"]').should('have.value', testVideo)
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')

        // discard changes
        cy.get('[data-cy="discardChangesBtn"]').click()
        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', vid.file)
        cy.get('[data-cy="videoCaptions"]').should('have.value', vid.captions)
        cy.get('[data-cy="videoTranscript"]').should('have.value', vid.transcript)
        cy.get('[data-cy="showExternalUrlBtn"]').should('exist')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
    });

    it('upload an audio file, set transcript, preview the file', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile({contents: `cypress/fixtures/${audioFile}`, mimeType: 'audio/wav'},  { force: true })
        cy.get('[data-cy="videoTranscript"]').type('transcript', {delay: 0})
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')

        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', audioFile)
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTotalDuration"]').should('have.text', '5 seconds')
        cy.get('[data-cy="videoPreviewCard"] [title="Play"]').click()

        // audio is 5.9 seconds
        cy.wait(6000)
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '100%')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTimeWatched"]').should('have.text', '5 seconds')

        // refresh and re-validate
        cy.visitVideoConfPage();
        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', audioFile)
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTotalDuration"]').should('have.text', '5 seconds')
        cy.get('[data-cy="videoPreviewCard"] [title="Play"]').click()
        cy.wait(6000)
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '100%')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTimeWatched"]').should('have.text', '5 seconds')
    });

    it('captions are cleared for audio files', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')
        cy.get('[data-cy="videoCaptions"]').type(defaultCaption)
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile({contents: `cypress/fixtures/${audioFile}`, mimeType: 'audio/wav'},  { force: true })
        cy.get('[data-cy="videoTranscript"]').type('transcript', {delay: 0})
        cy.get('[data-cy="videoCaptions"]').should('not.exist')
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.visible')

        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', audioFile)
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTotalDuration"]').should('have.text', '5 seconds')
        cy.get('[data-cy="videoPreviewCard"] [title="Play"]').click()

        // refresh and re-validate
        cy.visitVideoConfPage();
        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', audioFile)
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="videoCaptions"]').should('not.exist')

        cy.get('[data-cy="resetBtn"]').click();
        cy.get('[data-cy="videoCaptions"]').should('exist')
        cy.get('[data-cy="videoCaptions"]').should('not.have.value',defaultCaption)

    });
});
