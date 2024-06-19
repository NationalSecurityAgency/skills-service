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

describe('Handle Video without duration Tests', () => {

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

    it('upload video without duration', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visitVideoConfPage();
        const videoFile = 'create-project-noDuration.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.get('[data-cy="videoFileInput"] input[type=text]').should('have.value', videoFile)
        cy.get('[data-cy="noDurationWarning"]')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTotalDuration"]').should('have.text', 'N/A')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', 'N/A')
        cy.get('[data-cy="videoPreviewCard"] [title="Play Video"]').click()

        // video is 9 seconds
        cy.wait(9000)
        cy.get('[data-cy="videoPreviewCard"] [data-cy="percentWatched"]').should('have.text', '100%')
        cy.get('[data-cy="videoPreviewCard"] [data-cy="videoTimeWatched"]').should('have.text', '9 seconds')
        cy.get('[data-cy="noDurationWarning"]')
    });

    it('Skills Display - do not display % for videos without duration', () => {
        cy.intercept('POST', '/api/projects/proj1/skills/skill1').as('reportSkill1')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1 });
        cy.saveVideoAttrs(1, 1, { file: 'create-project-noDuration.webm', transcript: 'another' })
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('not.exist')

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(8000) // video is 8 seconds
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 100 points')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('not.exist')
        cy.wait('@reportSkill1')
    });

});