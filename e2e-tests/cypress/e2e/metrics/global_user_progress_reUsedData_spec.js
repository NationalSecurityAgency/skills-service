/*
 * Copyright 2026 SkillTree
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
var moment = require('moment-timezone');

describe('Global Users Progress With Reused Data', () => {

    before(() => {
        cy.beforeTestSuiteThatReusesData()

        const createProj = (projNum) => {
            cy.createProject(projNum)
            cy.createSubject(projNum, 1)
            const numSkills = 5;
            for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
                cy.createSkill(projNum, 1, skillsCounter, { numPerformToCompletion: 1 })
            }
        }
        createProj(1)
        createProj(2)
        createProj(3)

        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.createBadge(1, 2)
        cy.assignSkillToBadge(1, 2, 2)
        cy.enableBadge(1, 2)

        cy.createBadge(1, 3)
        cy.assignSkillToBadge(1, 3, 3)
        cy.enableBadge(1, 3)

        cy.createGlobalBadge(10)
        cy.assignSkillToGlobalBadge(10, 1, 1 )
        cy.enableGlobalBadge(10)

        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)

        cy.createQuizDef(2)
        cy.createQuizQuestionDef(2, 1)

        cy.createSurveyDef(3)
        cy.createSurveyMultipleChoiceQuestionDef(3, 1)

        const users = ['user1', 'user2', 'user3']

        // user 1
        cy.reportSkill(1, 1, users[0])
        cy.runQuizForUser(1, users[0], [{selectedIndex: [0]}], true)
        cy.runQuizForUser(2, users[0], [{selectedIndex: [0]}], true)


        // user 2
        cy.reportSkill(1, 1, users[1])
        cy.reportSkill(2, 1, users[1])
        cy.runQuizForUser(3, users[1], [{selectedIndex: [0]}], false)

        // user 3
        cy.reportSkill(1, 1, users[2])
        cy.reportSkill(1, 2, users[2])
        cy.reportSkill(2, 1, users[2])
        cy.reportSkill(2, 2, users[2])
        cy.reportSkill(2, 3, users[2])
        cy.reportSkill(3, 1, users[2])
        cy.reportSkill(3, 2, users[2])
        cy.reportSkill(3, 3, users[2])
        cy.reportSkill(3, 3, users[2])

        cy.runQuizForUser(1, users[2], [{selectedIndex: [1]}], true)
        cy.runQuizForUser(1, users[2], [{selectedIndex: [0]}], true)
        cy.runQuizForUser(2, users[2], [{selectedIndex: [0]}], false)
        cy.runQuizForUser(3, users[2], [{selectedIndex: [0]}], true)

        cy.addUserTag([{
            tagKey: 'dutyOrganization',
            tags: ['ABC']
        }, {
            tagKey: 'dutyOrganization',
            tags: ['ABC1']
        }]);
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    it('user progress page', () => {
        cy.visit('/administrator/users-progress');

        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Projects')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('15 Skills')

        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Quizzes and 1 Survey')

        cy.get('[data-cy="overallMetricsBadgesCard"] [data-cy="mediaInfoCardTitle"]').contains('4 Badges')
        cy.get('[data-cy="overallMetricsBadgesCard"] [data-cy="mediaInfoCardSubTitle"]').contains('3 Project and 1 Global Badges')

        const expectedHeaders = ['User', 'Org', 'Skills & Projects', '# Quiz Runs', '# Survey Runs', "# Badges"]
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')
            .should('have.length', expectedHeaders.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeaders[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })

        verifyUserDataAtRow(0, 'user3', '', 53, '8 / 15 Skills', '3 / 3 Projects'
            , 3, 1,1, 1,
            '1 / 1', null,
            '2 / 4', 'Global: 1')
        verifyUserDataAtRow(1, 'user2', 'ABC1', 13, '2 / 15 Skills', '2 / 3 Projects'
            , 0, null, null, null,
            '0 / 1', 1,
            '1 / 4', 'Global: 1')
        verifyUserDataAtRow(2, 'user1', 'ABC', 6, '1 / 15 Skills', '1 / 3 Projects'
            , 2, 2, 0, null,
            '0 / 1', null,
            '1 / 4', 'Global: 1')
    });

    const verifyUserDataAtRow = (rowIndex, expectedUserId, expectedUserTag,
                                 progressPercent, skillsProgress, projectsProgress,
                                 quizAttempts, quizAttemptsPassed, quizAttemptsFailed, quizAttemptsInProgress,
                                 surveyRuns, surveyRunsInProgress,
                                 badges, globalBadges) => {
        cy.get(`[data-p-index="${rowIndex}"] [data-cy="userIdForDisplay"]`).should('have.text', expectedUserId)
        cy.get(`[data-p-index="${rowIndex}"] [data-cy="userTag"]`).should('have.text', expectedUserTag)
        cy.get(`[data-p-index="${rowIndex}"] [data-cy="skillAndProjProgress"] [data-cy="progressPercent"]`).should('have.text', `${progressPercent}%`)
        cy.get(`[data-p-index="${rowIndex}"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]`).should('have.text', skillsProgress)
        cy.get(`[data-p-index="${rowIndex}"] [data-cy="skillAndProjProgress"] [data-cy="projectCount"]`).should('have.text', projectsProgress)
        cy.get(`[data-p-index="${rowIndex}"] [data-cy="quizAttempts"]`).should('have.text', quizAttempts)
        if (quizAttemptsPassed !== null) {
            cy.get(`[data-p-index="${rowIndex}"] [data-cy="quizAttemptsPassed"]`).should('have.text', quizAttemptsPassed)
        } else {
            cy.get(`[data-p-index="${rowIndex}"] [data-cy="quizAttemptsPassed"]`).should('not.exist')
        }
        if (quizAttemptsFailed !== null) {
            cy.get(`[data-p-index="${rowIndex}"] [data-cy="quizAttemptsFailed"]`).should('have.text', quizAttemptsFailed)
        } else {
            cy.get(`[data-p-index="${rowIndex}"] [data-cy="quizAttemptsFailed"]`).should('not.exist')
        }
        if (quizAttemptsInProgress !== null) {
            cy.get(`[data-p-index="${rowIndex}"] [data-cy="quizAttemptsInProgress"]`).should('have.text', quizAttemptsInProgress)
        } else {
            cy.get(`[data-p-index="${rowIndex}"] [data-cy="quizAttemptsInProgress"]`).should('not.exist')
        }
        cy.get(`[data-p-index="${rowIndex}"] [data-cy="surveyRuns"]`).should('have.text', surveyRuns)
        if (surveyRunsInProgress !== null) {
            cy.get(`[data-p-index="${rowIndex}"] [data-cy="surveyRunsInProgress"]`).should('have.text', surveyRunsInProgress)
        } else {
            cy.get(`[data-p-index="${rowIndex}"] [data-cy="surveyRunsInProgress"]`).should('not.exist')
        }

        cy.get(`[data-p-index="${rowIndex}"] [data-cy="badges"]`).should('have.text', badges)
        cy.get(`[data-p-index="${rowIndex}"] [data-cy="globalBadges"]`).should('have.text', globalBadges)
    }

    it('sort by user', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user1')
        cy.get('[data-p-index="1"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-p-index="2"] [data-cy="userIdForDisplay"]').should('have.text', 'user3')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user3')
        cy.get('[data-p-index="1"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-p-index="2"] [data-cy="userIdForDisplay"]').should('have.text', 'user1')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user1')
        cy.get('[data-p-index="1"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-p-index="2"] [data-cy="userIdForDisplay"]').should('have.text', 'user3')

        cy.visit('/administrator/users-progress');
        cy.get('[data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user1')
        cy.get('[data-p-index="1"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-p-index="2"] [data-cy="userIdForDisplay"]').should('have.text', 'user3')
    })

    it('sort by userTag', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('Org').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="userTag"]').should('have.text', 'ABC')
        cy.get('[data-p-index="1"] [data-cy="userTag"]').should('have.text', 'ABC1')
        cy.get('[data-p-index="2"] [data-cy="userTag"]').should('have.text', '')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('Org').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="userTag"]').should('have.text', '')
        cy.get('[data-p-index="1"] [data-cy="userTag"]').should('have.text', 'ABC1')
        cy.get('[data-p-index="2"] [data-cy="userTag"]').should('have.text', 'ABC')

        cy.visit('/administrator/users-progress');
        cy.get('[data-p-index="0"] [data-cy="userTag"]').should('have.text', '')
        cy.get('[data-p-index="1"] [data-cy="userTag"]').should('have.text', 'ABC1')
        cy.get('[data-p-index="2"] [data-cy="userTag"]').should('have.text', 'ABC')
    })

    it('sort by skills and projects', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('Skills & Projects').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '1 / 15 Skills')
        cy.get('[data-p-index="1"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '2 / 15 Skills')
        cy.get('[data-p-index="2"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '8 / 15 Skills')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('Skills & Projects').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '8 / 15 Skills')
        cy.get('[data-p-index="1"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '2 / 15 Skills')
        cy.get('[data-p-index="2"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '1 / 15 Skills')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('Skills & Projects').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '1 / 15 Skills')
        cy.get('[data-p-index="1"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '2 / 15 Skills')
        cy.get('[data-p-index="2"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '8 / 15 Skills')

        cy.visit('/administrator/users-progress');
        cy.get('[data-p-index="0"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '1 / 15 Skills')
        cy.get('[data-p-index="1"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '2 / 15 Skills')
        cy.get('[data-p-index="2"] [data-cy="skillAndProjProgress"] [data-cy="skillCount"]').should('contain.text', '8 / 15 Skills')
    })

    it('sort by quiz runs', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('# Quiz Runs').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="quizAttempts"]').should('have.text', '0')
        cy.get('[data-p-index="1"] [data-cy="quizAttempts"]').should('have.text', '2')
        cy.get('[data-p-index="2"] [data-cy="quizAttempts"]').should('have.text', '3')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('# Quiz Runs').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="quizAttempts"]').should('have.text', '3')
        cy.get('[data-p-index="1"] [data-cy="quizAttempts"]').should('have.text', '2')
        cy.get('[data-p-index="2"] [data-cy="quizAttempts"]').should('have.text', '0')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('# Quiz Runs').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="quizAttempts"]').should('have.text', '0')
        cy.get('[data-p-index="1"] [data-cy="quizAttempts"]').should('have.text', '2')
        cy.get('[data-p-index="2"] [data-cy="quizAttempts"]').should('have.text', '3')

        cy.visit('/administrator/users-progress');
        cy.get('[data-p-index="0"] [data-cy="quizAttempts"]').should('have.text', '0')
        cy.get('[data-p-index="1"] [data-cy="quizAttempts"]').should('have.text', '2')
        cy.get('[data-p-index="2"] [data-cy="quizAttempts"]').should('have.text', '3')
    })

    it('sort by survey runs', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('# Survey Runs').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="surveyRuns"]').should('contain.text', '0 / 1')
        cy.get('[data-p-index="1"] [data-cy="surveyRuns"]').should('contain.text', '0 / 1')
        cy.get('[data-p-index="2"] [data-cy="surveyRuns"]').should('contain.text', '1 / 1')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('# Survey Runs').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="surveyRuns"]').should('contain.text', '1 / 1')
        cy.get('[data-p-index="1"] [data-cy="surveyRuns"]').should('contain.text', '0 / 1')
        cy.get('[data-p-index="2"] [data-cy="surveyRuns"]').should('contain.text', '0 / 1')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('# Survey Runs').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="surveyRuns"]').should('contain.text', '0 / 1')
        cy.get('[data-p-index="1"] [data-cy="surveyRuns"]').should('contain.text', '0 / 1')
        cy.get('[data-p-index="2"] [data-cy="surveyRuns"]').should('contain.text', '1 / 1')

        cy.visit('/administrator/users-progress');
        cy.get('[data-p-index="0"] [data-cy="surveyRuns"]').should('contain.text', '0 / 1')
        cy.get('[data-p-index="1"] [data-cy="surveyRuns"]').should('contain.text', '0 / 1')
        cy.get('[data-p-index="2"] [data-cy="surveyRuns"]').should('contain.text', '1 / 1')
    })

    it('sort by badges', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('# Badges').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="badges"]').should('contain.text', '1 / 4')
        cy.get('[data-p-index="1"] [data-cy="badges"]').should('contain.text', '1 / 4')
        cy.get('[data-p-index="2"] [data-cy="badges"]').should('contain.text', '2 / 4')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('# Badges').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="badges"]').should('contain.text', '2 / 4')
        cy.get('[data-p-index="1"] [data-cy="badges"]').should('contain.text', '1 / 4')
        cy.get('[data-p-index="2"] [data-cy="badges"]').should('contain.text', '1 / 4')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('# Badges').click();
        cy.wait('@progressMetrics')

        cy.get('[data-p-index="0"] [data-cy="badges"]').should('contain.text', '1 / 4')
        cy.get('[data-p-index="1"] [data-cy="badges"]').should('contain.text', '1 / 4')
        cy.get('[data-p-index="2"] [data-cy="badges"]').should('contain.text', '2 / 4')

        cy.visit('/administrator/users-progress');
        cy.get('[data-p-index="0"] [data-cy="badges"]').should('contain.text', '1 / 4')
        cy.get('[data-p-index="1"] [data-cy="badges"]').should('contain.text', '1 / 4')
        cy.get('[data-p-index="2"] [data-cy="badges"]').should('contain.text', '2 / 4')
    })

    it('filter by user', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        
        // Test filtering for user1
        cy.get('[data-cy="userFilter"]').type(' SeR1 ')
        cy.get('[data-cy="filterBtn"]').click()
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user1')
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-section="bodyrow"]').should('have.length', 1)

        // Clear filter and verify all users are back
        cy.get('[data-cy="resetBtn"]').click()
        cy.get('[data-cy="userFilter"]').should('be.empty')
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user3')
        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="1"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="2"] [data-cy="userIdForDisplay"]').should('have.text', 'user1')
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-section="bodyrow"]').should('have.length', 3)
    })

    it('filter by user tag', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')

        cy.get('[data-cy="userTagFilter"]').type(' bC1 ')
        cy.get('[data-cy="filterBtn"]').click()
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-section="bodyrow"]').should('have.length', 1)

        // Clear filter and verify all users are back
        cy.get('[data-cy="resetBtn"]').click()
        cy.get('[data-cy="userTagFilter"]').should('be.empty')
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user3')
        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="1"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="2"] [data-cy="userIdForDisplay"]').should('have.text', 'user1')
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-section="bodyrow"]').should('have.length', 3)
    })

    it('filter by user and user tag', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')

        cy.get('[data-cy="userFilter"]').type(' SeR2 ')
        cy.get('[data-cy="userTagFilter"]').type(' bC1 ')
        cy.get('[data-cy="filterBtn"]').click()
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-section="bodyrow"]').should('have.length', 1)

        // Clear filter and verify all users are back
        cy.get('[data-cy="resetBtn"]').click()
        cy.get('[data-cy="userFilter"]').should('be.empty')
        cy.get('[data-cy="userTagFilter"]').should('be.empty')
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user3')
        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="1"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="2"] [data-cy="userIdForDisplay"]').should('have.text', 'user1')
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-section="bodyrow"]').should('have.length', 3)
    })

    it('use reset button in the table', () => {
        cy.visit('/administrator/users-progress');

        cy.intercept('/app/progress-metrics**').as('progressMetrics')

        cy.get('[data-cy="userFilter"]').type(' !@#$^&*( ')
        cy.get('[data-cy="userTagFilter"]').type(' !@#$^&*( ')
        cy.get('[data-cy="filterBtn"]').click()
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-pc-section="bodyrow"]').should('have.length', 0)

        // Clear filter and verify all users are back
        cy.get('[data-cy="userOverallProgressTable"] [data-cy="tblFilterResetBtn"]').click()
        cy.get('[data-cy="userFilter"]').should('be.empty')
        cy.get('[data-cy="userTagFilter"]').should('be.empty')
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-cy="userIdForDisplay"]').should('have.text', 'user3')
        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="1"] [data-cy="userIdForDisplay"]').should('have.text', 'user2')
        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="2"] [data-cy="userIdForDisplay"]').should('have.text', 'user1')
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-section="bodyrow"]').should('have.length', 3)
    })


});
