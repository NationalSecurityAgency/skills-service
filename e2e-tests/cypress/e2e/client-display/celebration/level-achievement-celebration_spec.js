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
describe('Achievement Celebration Messages Tests', () => {

    it('show level-specific project celebration', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)

        cy.cdVisit('/')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');

        cy.cdVisit('/')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You just crushed Level 1 and hit Level 2')

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 2 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You achieved Level 2, a significant milestone!')

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 3 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Reaching Level 3 is an impressive achievement!')

        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');
        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 4 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Achieving Level 4 is a testament to your perseverance and determination!')

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'now');
        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 5 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Congratulation on Level 5')
    })

    it('show level-specific subject celebration', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)

        cy.cdVisit('/subjects/subj1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');

        cy.cdVisit('/subjects/subj1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You took the first step in mastering Subject 1')

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.cdVisit('/subjects/subj1', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 2 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 2')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You have reached Level 2 in Subject 1')

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.cdVisit('/subjects/subj1', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 3 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 3')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You\'ve reached Level 3 in Subject 1')

        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');
        cy.cdVisit('/subjects/subj1', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 4 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 4')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Reaching Level 4 is an impressive achievement')

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'now');
        cy.cdVisit('/subjects/subj1', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 5 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Congratulations on reaching Level 5 in Subject 1')
    })

    it('only show project achievement kudos if achieved date is within 7 days', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), '8 days ago');

        cy.cdVisit('/')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), '9 days ago');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), '6 days ago');
        cy.cdVisit('/', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 2 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You achieved Level 2, a significant milestone!')
    })

    it('only show subject achievement kudos if achieved date is within 7 days', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), '8 days ago');

        cy.cdVisit('/subjects/subj1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), '9 days ago');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), '6 days ago');
        cy.cdVisit('/subjects/subj1', true)
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 2 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 2')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You have reached Level 2 in Subject 1')
    })

    it('closing project achievement kudos will not show that particular achievement kudos again', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)

        cy.createProject(2)
        cy.createSubject(2)
        cy.createSkill(2, 1, 1)
        cy.createSkill(2, 1, 2)
        cy.createSkill(2, 1, 3)

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');

        cy.reportSkill(2, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(2, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(2, 3, Cypress.env('proxyUser'), 'now');

        cy.visit('/progress-and-rankings/projects/proj1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You just crushed Level 1 and hit Level 2')
        cy.get('[data-cy="closeCelebrationMsgBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').eq(0).click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You took the first step in mastering Subject 1')

        cy.visit('/progress-and-rankings/projects/proj1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.visit('/progress-and-rankings/projects/proj2')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 3 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Reaching Level 3 is an impressive achievement!')
    })

    it('closing subject achievement kudos will not show that particular achievement kudos again', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)

        cy.createSubject(1,2 )
        cy.createSkill(1, 2, 4)
        cy.createSkill(1, 2, 5)
        cy.createSkill(1, 2, 6)

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');

        cy.log('do report!!')
        cy.doReportSkill({ project: 1, subjNum: 2, date: 'now', skill: 4, userId:  Cypress.env('proxyUser')})
        cy.doReportSkill({ project: 1, subjNum: 2, date: 'now', skill: 5, userId:  Cypress.env('proxyUser')})
        cy.doReportSkill({ project: 1, subjNum: 2, date: 'now', skill: 6, userId:  Cypress.env('proxyUser')})

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You took the first step in mastering Subject 1')
        cy.get('[data-cy="closeCelebrationMsgBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')

        cy.get('[data-cy="breadcrumbItemValue"]').contains('proj1').click()
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTile-subj2"] [data-cy="subjectTileBtn"]').click()
        cy.get('[data-cy="pointHistoryChartNoData"]')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 3 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 3')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You\'ve reached Level 3 in Subject 2')

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')
    })

    it('honor skill will achieve a level and show celebration msg', () => {
        cy.createProject(1)
        cy.createSubject(1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem' })
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' })
        cy.createSkill(1, 1, 3, { selfReportingType: 'HonorSystem' })
        cy.createSkill(1, 1, 4, { selfReportingType: 'HonorSystem' })
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' })

        cy.cdVisit('/subjects/subj1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 0 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').should('not.exist')
        cy.get('[data-cy=toggleSkillDetails]').click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="claimPointsBtn"]').click();

        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallLevel"] [data-cy="overallLevelDesc"]').should('have.text', 'Level 1 out of 5')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('Subject Level 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="levelAchievementCelebrationMsg"]').contains('You took the first step in mastering Subject 1')
    })

})





