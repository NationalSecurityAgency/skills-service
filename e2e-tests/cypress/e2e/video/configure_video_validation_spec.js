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

describe('Configure Video Validation Tests', () => {

    const testVideo = '/static/videos/create-quiz.mp4'
    const defaultCaption = Cypress.env("defaultCaptions")
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

    it('video is required - resolve error by providing a Video Url', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="videoCaptions"]').type(defaultCaption, { delay: 0 })
        cy.get('[data-cy="videoCaptionsError"]').contains('Captions is not valid without Video field')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoTranscript"]').type('transcript', { delay: 0 })
        cy.get('[data-cy="videoTranscriptError"]').contains('Transcript is not valid without Video field')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="showExternalUrlBtn"]').click()
        cy.get('[data-cy="videoUrl"]').type(testVideo, { delay: 0 })

        cy.get('[data-cy="videoCaptionsError"]').should('not.be.visible')
        cy.get('[data-cy="videoTranscriptError"]').should('not.be.visible')

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        // validation should work after clear
        cy.get('[data-cy="clearVideoSettingsBtn"]').click()
        cy.get('.p-dialog-footer').contains('Yes, Do clear').click()
        cy.get('[data-cy="videoUrl"]').should('not.exist')
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')
    });

    it('transcript custom description validation', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();
        const videoFile = 'create-subject.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoTranscript"]').type('jabberwocky', { delay: 0 })
        cy.get('[data-cy="videoTranscriptError"]').contains('Video Transcript - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')
    });

    it('transcript max chars validation', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();
        cy.get('[data-cy="showExternalUrlBtn"]').click()
        cy.get('[data-cy="videoUrl"]').type('http://some.vid', { delay: 0 })
        const invalidValue = Array(51).fill('a').join('');
        cy.get('[data-cy="videoTranscript"]').type(invalidValue, { delay: 0 })
        cy.get('[data-cy="videoTranscriptError"]').contains('Video Transcript must be at most 50 characters')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="videoTranscript"]').type('{backspace}');
        cy.get('[data-cy="videoTranscriptError"]').should('not.be.visible')
    });

    it('captions max chars validation', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();
        cy.get('[data-cy="showExternalUrlBtn"]').click()
        cy.get('[data-cy="videoUrl"]').type('http://some.vid', { delay: 0 })
        const invalidValue = Array(101).fill('a').join('');
        cy.get('[data-cy="videoCaptions"]').type(invalidValue, { delay: 0 })
        cy.get('[data-cy="videoCaptionsError"]').contains('Captions must be at most 100 characters')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="videoCaptions"]').type('{backspace}');
        cy.get('[data-cy="videoCaptionsError"]').should('not.have.text', 'Captions must be at most 100 characters')
    });

    it('only allow upload of valid video mime types', () => {
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/video').as('getVideoProps')
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('getSkillInfo')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();
        cy.wait('@getVideoProps')
        cy.wait('@getSkillInfo')
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.enabled')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="videoFileInputError"]').should('not.exist')

        const videoFile = 'valid_icon.png';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="videoFileInputError"]').contains('Unsupported [image/png] file type, supported types: [video/webm,video/mp4,audio/wav,audio/mpeg,audio/mp4,audio/aac,audio/aacp,audio/ogg,audio/webm,audio/flac]')
    });

    it('validate maximum size of the video', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxAttachmentSize = 1024*300;
                res.send(conf);
            });
        }).as('loadConfig');

        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/video').as('getVideoProps')
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('getSkillInfo')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video');
        cy.wait('@loadConfig')
        cy.wait('@getVideoProps')
        cy.wait('@getSkillInfo')
        cy.get('.spinner-border').should('not.exist')
        cy.get('[data-cy="showExternalUrlBtn"]').should('be.enabled')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="videoFileInputError"]').should('not.exist')

        const videoFile = 'create-project.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="videoFileInputError"]').contains('File exceeds maximum size of 300 KB')
    });

});