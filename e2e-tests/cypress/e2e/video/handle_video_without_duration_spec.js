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
    });

    it('upload video without duration', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/configVideo');
        const videoFile = 'create-project-noDuration.webm';
        cy.get('[data-cy="videoFileUpload"]').attachFile({ filePath: videoFile, encoding: 'binary'});
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')

        cy.get('[data-cy="videoUrl"]').should('have.value', videoFile)
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
});