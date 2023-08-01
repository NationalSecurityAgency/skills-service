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

describe('Configure Video and SkillTree Features Tests', () => {

    const testVideo = '/static/videos/create-quiz.mp4'
    beforeEach(() => {
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