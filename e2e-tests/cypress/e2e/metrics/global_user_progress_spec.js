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

describe('Global Users Progress', () => {

    it('no user progress', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createQuizDef(1)
        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.visit('/administrator/users-progress');
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('1 Project')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('1 Skill')

        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('1 Assessment')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('1 Quiz and 0 Surveys')

        cy.get('[data-cy="overallMetricsBadgesCard"] [data-cy="mediaInfoCardTitle"]').contains('1 Badge')
        cy.get('[data-cy="overallMetricsBadgesCard"] [data-cy="mediaInfoCardSubTitle"]').contains('1 Project Badge and 0 Global Badges')

        cy.get('[data-cy="noUserOverallProgress"]')
        cy.get('[data-cy="userOverallProgressTable"]').should('not.exist')
    });

    it('plural verbiage on cards', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createProject(2)
        cy.createQuizDef(1)
        cy.createQuizDef(2)

        cy.createSurveyDef(3)
        cy.createSurveyDef(4)

        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.createBadge(1, 2)
        cy.assignSkillToBadge(1, 2, 1)
        cy.enableBadge(1, 2)

        cy.createGlobalBadge(10)
        cy.assignSkillToGlobalBadge(10, 1, 1 )
        cy.enableGlobalBadge(10)
        cy.createGlobalBadge(11)
        cy.assignSkillToGlobalBadge(11, 1, 1 )
        cy.enableGlobalBadge(11)

        cy.visit('/administrator/users-progress');
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('2 Projects')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Skills')

        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('4 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Quizzes and 2 Surveys')

        cy.get('[data-cy="overallMetricsBadgesCard"] [data-cy="mediaInfoCardTitle"]').contains('4 Badges')
        cy.get('[data-cy="overallMetricsBadgesCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Project Badges and 2 Global Badges')

        cy.get('[data-cy="noUserOverallProgress"]')
    });

    it('all columns are shown ', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createQuizDef(1)
        cy.createSurveyDef(2)
        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.reportSkill(1, 1)

        cy.visit('/administrator/users-progress');

        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')

        const expectedHeaders = ['User', 'Org', 'Skills & Projects', '# Quiz Runs', '# Survey Runs', "# Badges"]
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')
            .should('have.length', expectedHeaders.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeaders[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })

        cy.get('[data-cy="userTagFilter"]').should('be.visible')
    });

    it('should hide badges column when no badges are configured', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createQuizDef(1)
        cy.createSurveyDef(2)

        cy.reportSkill(1, 1)

        cy.visit('/administrator/users-progress');

        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')

        const expectedHeaders = ['User', 'Org', 'Skills & Projects', '# Quiz Runs', '# Survey Runs']
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')
            .should('have.length', expectedHeaders.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeaders[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })
    });

    it('should hide quiz column when no quizzes are configured', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.createSurveyDef(2)

        cy.reportSkill(1, 1)

        cy.visit('/administrator/users-progress');

        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')

        const expectedHeaders = ['User', 'Org', 'Skills & Projects', '# Survey Runs', '# Badges']
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')
            .should('have.length', expectedHeaders.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeaders[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })
    });

    it('should hide survey column when no surveys are configured', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.createQuizDef(2)

        cy.reportSkill(1, 1)

        cy.visit('/administrator/users-progress');

        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')

        const expectedHeaders = ['User', 'Org', 'Skills & Projects', '# Quiz Runs', '# Badges']
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')
            .should('have.length', expectedHeaders.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeaders[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })
    });

    it('should hide quiz, survey and badges columns when they are not configured', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.reportSkill(1, 1)

        cy.visit('/administrator/users-progress');

        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')

        const expectedHeaders = ['User', 'Org', 'Skills & Projects']
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')
            .should('have.length', expectedHeaders.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeaders[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })
    });

    it('user tag column and filter are not shown when usersTableAdditionalUserTagKey prop is not configured', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.usersTableAdditionalUserTagKey = null;
                res.send(conf);
            });
        })
            .as('loadConfig');
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createQuizDef(1)
        cy.createSurveyDef(2)
        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.reportSkill(1, 1)

        cy.visit('/administrator/users-progress');

        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')

        const expectedHeaders = ['User', 'Skills & Projects', '# Quiz Runs', '# Survey Runs', "# Badges"]
        cy.get('[data-cy="userOverallProgressTable"] [data-pc-name="headercell"] [data-pc-section="columntitle"]')
            .should('have.length', expectedHeaders.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeaders[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })

        cy.get('[data-cy="userTagFilter"]').should('not.exist')
    });

});
