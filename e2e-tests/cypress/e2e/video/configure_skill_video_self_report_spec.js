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

describe('Configure Self Report Video Type Tests', () => {

    const testVideo = '/static/videos/create-quiz.mp4'
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

    it('self report type of video is disabled for a new skill', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSubject(1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="newSkillButton"]').click()
        cy.get('[data-cy="videoSelectionMsg"]').contains('Please create skill and configure video settings first')
        cy.get('[data-cy="selfReportEnableCheckbox"]').click()
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
        cy.get('[data-cy="selfReportEnableCheckbox"]').click();
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
        cy.get('[data-cy="numPerformToCompletion"] [data-pc-name="input"]').should('have.value', 4)
        cy.get('[data-cy="numPerformToCompletion"] [data-pc-name="input"]').should('be.enabled')
        cy.get('[data-cy="timeWindowInput"] [data-pc-section="togglebutton"]').click()
        cy.get('[data-cy="numPointIncrementMaxOccurrences"] [data-pc-name="input"]').should('have.value', 2)
        cy.get('[data-cy="numPointIncrementMaxOccurrences"] [data-pc-name="input"]').should('be.enabled')
        cy.get('[data-cy=timeWindowCheckbox] [data-pc-section="input"]').should('be.checked')

        cy.get('[data-cy="videoSelectionMsg"]').should('not.exist')
        cy.get('[data-cy="selfReportEnableCheckbox"]').click();
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
            .should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Video"]')
            .should('be.enabled');
        cy.get('[data-cy="videoSelectionMsg"]').should('not.exist')

        cy.get('[data-cy="selfReportTypeSelector"] [value="Video"]')
            .click({ force: true });
        cy.get('[data-cy="numPerformToCompletion"] [data-pc-name="input"]').should('have.value', 1)
        cy.get('[data-cy="numPerformToCompletion"] [data-pc-name="input"]').should('be.disabled')
        cy.get('[data-cy="timeWindowInput"] [data-pc-section="togglebutton"]').click()
        cy.get('[data-cy=timeWindowCheckbox] [data-pc-section="input"]').should('not.be.checked')
        cy.get('[data-cy="numPointIncrementMaxOccurrences"] [data-pc-name="input"]').should('have.value', 1)
        cy.get('[data-cy="numPointIncrementMaxOccurrences"] [data-pc-name="input"]').should('be.disabled')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.wait('@getSubjectSkills').then(() => {
            cy.wait(1000)
            cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill1"]')
            cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
            cy.get('[data-pc-section="overlay"] [aria-label="Self Report"]').click()
            cy.get('[data-cy="skillsTable"] [data-cy="selfReportCell-skill1"]').contains('Video')
        })
    });

    it('set skill self-report type to video', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion: 4, numMaxOccurrencesIncrementInterval: 2})
        cy.visitVideoConfPage()

        const videoFile = 'create-subject.webm';
        cy.fixture(videoFile, null).as('videoFile');
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile('@videoFile',  { force: true })
        cy.get('[data-cy="videoSelfReportAlert"]').should('not.exist')
        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.get('[data-cy="videoSelfReportAlert"]').contains('Optionally set Self Reporting type to Video in order to award the skill for watching this video')

        // refresh and re-validate
        cy.visitVideoConfPage()
        cy.get('[data-cy="videoSelfReportAlert"]').contains('Optionally set Self Reporting type to Video in order to award the skill for watching this video')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="videoSelectionMsg"]').should('not.exist')
        cy.get('[data-cy="selfReportEnableCheckbox"]').click();
        cy.get('[data-cy="selfReportTypeSelector"] [value="Video"]')
            .click({ force: true });
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="videoSelfReportAlert"]').contains('Users are required to watch this video in order to earn the skill and its points')

        // refresh and re-validate
        cy.visitVideoConfPage()
        cy.get('[data-cy="videoSelfReportAlert"]').contains('Users are required to watch this video in order to earn the skill and its points')
    });
});