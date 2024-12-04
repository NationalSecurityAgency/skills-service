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
describe('Subject Achievement Celebration Tests', () => {

    it('no badge achievements', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.cdVisit('/', false)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').should('not.exist')
    })

    it('show level-specific project celebration and badge celebration', () => {
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

        // not shown on subject page
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You took the first step in mastering Subject 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').should('not.exist')
    })

    it('badge celebration should link to the badge', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2, { pointIncrement: 1000 })

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Bravo! Badge 1 badge is all yours!')

        cy.get('[data-cy="badgeLink-badge1"]').click()
        cy.get('[data-cy="badgeTitle"]').contains('Badge 1')
    })

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

        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"] [data-cy="closeCelebrationMsgBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Bravo! Badge 1 badge is all yours!')

        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Bravo! Badge 1 badge is all yours!')
    })

    it('closing badge celebration does not effect level celebration', () => {
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
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Badge Achieved!')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Bravo! Badge 1 badge is all yours!')

        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"] [data-cy="closeCelebrationMsgBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You just crushed Level 1 and hit Level 2')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').should('not.exist')

        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You just crushed Level 1 and hit Level 2')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').should('not.exist')
    })

    it('badge celebration in multiple projects', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2, { pointIncrement: 1000})

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.createProject(2)
        cy.createSubject(2)
        cy.createSkill(2, 1, 1)
        cy.createSkill(2, 1, 2, { pointIncrement: 1000})

        cy.createBadge(2, 2);
        cy.assignSkillToBadge(2, 2, 1);
        cy.createBadge(2, 2, { enabled: true });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');

        cy.reportSkill(2, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(2, 1, Cypress.env('proxyUser'), 'now');


        cy.visit('/progress-and-rankings/projects/proj1')
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Badge Achieved!')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Bravo! Badge 1 badge is all yours!')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.visit('/progress-and-rankings/projects/proj2')
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Badge Achieved!')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Bravo! Badge 2 badge is all yours!')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"] [data-cy="closeCelebrationMsgBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').should('not.exist')

        cy.visit('/progress-and-rankings/projects/proj1')
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Badge Achieved!')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Bravo! Badge 1 badge is all yours!')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.visit('/progress-and-rankings/projects/proj2')
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').should('not.exist')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')
    })

    it('multiple badge celebration', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2, { pointIncrement: 1000})

        for (let i = 0; i < 3; i++) {
            cy.createBadge(1, i);
            cy.assignSkillToBadge(1, i, 1);
            cy.createBadge(1, i, { enabled: true, name: `Neat Things Happen ${i}` });
        }

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');

        cy.visit('/progress-and-rankings/projects/proj1')
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Badges Achieved!')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('You have earned 3 badges')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Neat Things Happen 0')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Neat Things Happen 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeAchievementCelebrationMsg"]').contains('Neat Things Happen 2')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.get('[data-cy="badgeLink-badge2"]').click()
        cy.get('[data-cy="badgeTitle"]').contains('Neat Things Happen 2')
    })

})





