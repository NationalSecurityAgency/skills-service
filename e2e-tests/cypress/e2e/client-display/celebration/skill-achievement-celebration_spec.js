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
describe('Skill Achievement Celebration Tests', () => {

    it('achieved skill shows celebration message', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1, {numPerformToCompletion: 1, pointIncrement: 100})
        cy.createSkill(1, 1, 2, {numPerformToCompletion: 1, pointIncrement: 100})

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/subjects/subj1/skills/skill1')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillCompletedCheck-skill1"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="achievementDate"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('be.visible')

        cy.cdVisit('/subjects/subj1/skills/skill2')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillCompletedCheck-skill2"]').should('not.exist')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="achievementDate"]').should('not.exist')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('not.exist')
    })

    it('only show skill achievement kudos if achieved date is within 7 days', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1, {numPerformToCompletion: 1, pointIncrement: 100})
        cy.createSkill(1, 1, 2, {numPerformToCompletion: 1, pointIncrement: 100})

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), '6 days ago');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), '8 days ago');

        cy.cdVisit('/subjects/subj1/skills/skill1')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillCompletedCheck-skill1"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="achievementDate"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('be.visible')

        cy.cdVisit('/subjects/subj1/skills/skill2')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillCompletedCheck-skill2"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="achievementDate"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('not.exist')
    })

    it('close skill achievement is not shown again for that skill only', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1, {numPerformToCompletion: 1, pointIncrement: 100})
        cy.createSkill(1, 1, 2, {numPerformToCompletion: 1, pointIncrement: 100})

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');

        cy.createProject(2)
        cy.createSubject(2)
        cy.createSkill(2, 1, 1, {numPerformToCompletion: 1, pointIncrement: 100})
        cy.createSkill(1, 1, 2, {numPerformToCompletion: 1, pointIncrement: 100})

        cy.reportSkill(2, 1, Cypress.env('proxyUser'), 'now');

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillCompletedCheck-skill1"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="achievementDate"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('be.visible')
        cy.get('[data-cy="closeCelebrationMsgBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('not.exist')

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill2')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillCompletedCheck-skill2"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="achievementDate"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('exist')

        cy.visit('/progress-and-rankings/projects/proj2/subjects/subj1/skills/skill1')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillCompletedCheck-skill1"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="achievementDate"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('be.visible')

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillCompletedCheck-skill1"]').should('be.visible')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="achievementDate"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillAchievementCelebrationMsg"]').should('not.exist')
    })

})





