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

    const testVideo = '/static/videos/create-quiz.mp4'
    beforeEach(() => {
    });

    it('self report type of video is disabled for a new skill', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSubject(1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]').click()
        cy.get('[data-cy="videoSelectionMsg"]').contains('Please create skill and configure video settings first')
        cy.get('[data-cy="selfReportEnableCheckbox"]').check({ force: true });
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Video"]')
            .should('be.disabled');
        cy.get('[data-cy="videoSelectionMsg"]').contains('Please create skill and configure video settings first')
    });

    it('existing skill has video self-report type disabled if video settings are not defined', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="videoSelectionMsg"]').contains('Please configure video settings first')
        cy.get('[data-cy="selfReportEnableCheckbox"]').check({ force: true });
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Video"]')
            .should('be.disabled');
        cy.get('[data-cy="videoSelectionMsg"]').contains('Please configure video settings first')
    });

    it('set skill self-report type to video', () => {
        cy.intercept('/admin/projects/proj1/subjects/subj1').as('getSubjectSkills')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion: 4, numMaxOccurrencesIncrementInterval: 2})
        cy.saveVideoAttrs(1, 1, { videoUrl: 'http://someurl.mp4' })
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="numPerformToCompletion"]').should('have.value', 4)
        cy.get('[data-cy="numPerformToCompletion"]').should('be.enabled')
        cy.get('[data-cy="maxOccurrences"]').should('have.value', 2)
        cy.get('[data-cy="maxOccurrences"]').should('be.enabled')
        cy.get('[data-cy=timeWindowCheckbox').should('be.checked')

        cy.get('[data-cy="videoSelectionMsg"]').should('not.exist')
        cy.get('[data-cy="selfReportEnableCheckbox"]').check({ force: true });
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Video"]')
            .should('be.enabled');
        cy.get('[data-cy="videoSelectionMsg"]').should('not.exist')

        cy.get('[data-cy="selfReportTypeSelector"] [value="Video"]')
            .click({ force: true });
        cy.get('[data-cy="numPerformToCompletion"]').should('have.value', 1)
        cy.get('[data-cy="numPerformToCompletion"]').should('be.disabled')
        cy.get('[data-cy=timeWindowCheckbox').should('not.be.checked')
        cy.get('[data-cy="maxOccurrences"]').should('have.value', 1)
        cy.get('[data-cy="maxOccurrences"]').should('be.disabled')

        cy.get('[data-cy="saveSkillButton"]').click()
        cy.get('[data-cy="saveSkillButton"]').should('not.exist')
        cy.wait('@getSubjectSkills').then(() => {
            cy.wait(1000)
            cy.get('[data-cy="skillsTable"] [data-cy="manageSkillBtn_skill1"]')
            cy.get('[data-cy="skillsTable-additionalColumns"] [value="selfReportingType"]')
                .click({ force: true });
            cy.get('[data-cy="skillsTable"] [data-cy="selfReportCell-skill1"]').contains('Video')
        })
    });

    it('configure video with all attributes', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="videoUrl"]').type('http://some.vid')
        cy.get('[data-cy="videoCaptions"]').type('captions')
        cy.get('[data-cy="videoTranscript"]').type('transcript')
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="videoUrl"]').should('have.value', 'http://some.vid')
        cy.get('[data-cy="videoCaptions"]').should('have.value','captions')
        cy.get('[data-cy="videoTranscript"]').should('have.value','transcript')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')
    });

    it('video url is required', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="videoCaptions"]').type('captions', { delay: 0 })
        cy.get('[data-cy="videoCaptionsError"]').contains('Captions is not valid without Video URL field')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoTranscript"]').type('transcript', { delay: 0 })
        cy.get('[data-cy="videoTranscriptError"]').contains('Transcript is not valid without Video URL field')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoUrl"]').type(testVideo, { delay: 0 })

        cy.get('[data-cy="videoCaptionsError"]').should('not.be.visible')
        cy.get('[data-cy="videoTranscriptError"]').should('not.be.visible')

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        // validation should work after clear
        cy.get('[data-cy="clearVideoSettingsBtn"]').click()
        cy.get('footer .btn-danger').contains('Yes, Do clear').click()
        cy.get('[data-cy="videoUrl"]').should('be.empty')
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')
    });

    it('clear attributes', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        const vidAttr = { videoUrl: 'http://someurl.mp4', captions: 'some', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');

        cy.get('[data-cy="videoUrl"]').should('have.value', vidAttr.videoUrl)
        cy.get('[data-cy="videoCaptions"]').should('have.value', vidAttr.captions)
        cy.get('[data-cy="videoTranscript"]').should('have.value', vidAttr.transcript)

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="clearVideoSettingsBtn"]').click()
        cy.get('footer .btn-danger').contains('Yes, Do clear').click()

        cy.get('[data-cy="videoUrl"]').should('be.empty')
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="videoUrl"]').should('be.empty')
        cy.get('[data-cy="videoCaptions"]').should('be.empty')
        cy.get('[data-cy="videoTranscript"]').should('be.empty')

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')
    });

    it('fill caption example', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="fillCaptionsExamples"]').click()
        cy.get('[data-cy="videoCaptions"]').should('contain.value', 'WEBVTT')
        cy.get('[data-cy="fillCaptionsExamples"]').should('not.exist')
    });

    it('preview video', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.disabled')

        cy.get('[data-cy="videoUrl"]').type(testVideo)

        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')

        cy.get('[data-cy="videoPreviewCard"]').should('not.exist')
        cy.get('[data-cy="previewVideoSettingsBtn"]').click()
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '0%')
        cy.get('[data-cy="videoPreviewCard"] [title="Play Video"]').click()
        // video is 15 seconds
        cy.wait(15000)
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '100%')
    });

    it('transcript custom description validation', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="videoTranscript"]').type('jabberwocky', { delay: 0 })
        cy.get('[data-cy="videoTranscriptError"]').contains('Video Transcript - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('be.enabled')
    });

    it('transcript max chars validation', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="videoUrl"]').type('http://some.vid', { delay: 0 })
        const invalidValue = Array(51).fill('a').join('');
        cy.get('[data-cy="videoTranscript"]').type(invalidValue, { delay: 0 })
        cy.get('[data-cy="videoTranscriptError"]').contains('Video Transcript cannot exceed 50 characters.')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="videoTranscript"]').type('{backspace}');
        cy.get('[data-cy="videoTranscriptError"]').should('not.be.visible')
    });

    it('captions max chars validation', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="videoUrl"]').type('http://some.vid', { delay: 0 })
        const invalidValue = Array(50).fill('a').join('');
        cy.get('[data-cy="videoCaptions"]').type(invalidValue, { delay: 0 })
        cy.get('[data-cy="videoCaptionsError"]').contains('Captions cannot exceed 49 characters.')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('be.disabled')
        cy.get('[data-cy="videoCaptions"]').type('{backspace}');
        cy.get('[data-cy="videoCaptionsError"]').should('not.be.visible')
    });

    it('cannot configure video settings on reused skills', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        const vidAttr = { videoUrl: testVideo, captions: 'some\nok\nglad\n\nok\nmore\nlines\nshould\nrender\nok', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)

        cy.createSubject(1, 2);
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.visit('/administrator/projects/proj1/subjects/subj2/skills/skill1STREUSESKILLST0/configVideo');
        cy.get('[data-cy="readOnlyAlert"]').contains('Reused')
        cy.get('[data-cy="videoTranscript"]').should('be.disabled')
        cy.get('[data-cy="videoCaptions"]').should('be.disabled')
        cy.get('[data-cy="videoTranscript"]').should('be.disabled')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('not.exist')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('not.exist')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('not.exist')
    });

    it('cannot configure video settings on imported skills', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        const vidAttr = { videoUrl: testVideo, captions: 'some\nok\nglad\n\nok\nmore\nlines\nshould\nrender\nok', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.exportSkillToCatalog(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1/configVideo');
        cy.get('[data-cy="readOnlyAlert"]').contains('Imported')
        cy.get('[data-cy="videoTranscript"]').should('be.disabled')
        cy.get('[data-cy="videoCaptions"]').should('be.disabled')
        cy.get('[data-cy="videoTranscript"]').should('be.disabled')
        cy.get('[data-cy="saveVideoSettingsBtn"]').should('not.exist')
        cy.get('[data-cy="previewVideoSettingsBtn"]').should('not.exist')
        cy.get('[data-cy="clearVideoSettingsBtn"]').should('not.exist')
    });

    it('copying skill with video self-report type resets the self-report type', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        const vidAttr = { videoUrl: testVideo, captions: 'some\nok\nglad\n\nok\nmore\nlines\nshould\nrender\nok', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="copySkillButton_skill1"]').click()
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked')
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked')
    });
});
