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
describe('Ability to disable Celebration as a project setting Tests', () => {

    it('closing level-specific project celebration does not effect badge celebration', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You just crushed Level 1 and hit Level 2')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Bravo! Badge 1 badge is all yours!')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').eq(0).click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You took the first step in mastering Subject 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillProgressTitle-skill1"] [data-cy="skillProgressTitle"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillCompletedCheck-skill1"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="achievementDate"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('be.visible')

        cy.visit('/administrator/projects/proj1/settings')
        cy.get('[data-cy="disableAchievementsCelebrationSwitch"]').click()
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes');
        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');

        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').should('not.exist')
    })

})





