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
        cy.get('[data-cy="overallMetricsBadgesCard"] [data-cy="mediaInfoCardSubTitle"]').contains('1 Project and 0 Global')

        cy.get('[data-cy="noUserOverallProgress"]')
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
        cy.get('[data-cy="overallMetricsBadgesCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Project and 2 Global')

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

    it('expand user progress - empty quiz runs table', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createProject(2)
        cy.createQuizDef(1)
        cy.reportSkill(1, 1, 'user1')

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-pc-group-section="rowactionbutton"]').click()

        cy.get('[data-cy="usrOverallProgress-user1"]').should('be.visible')
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="projectsProgress"] [data-cy="projProgress-proj1"] [data-cy="projName"]').contains('This is project 1')
        cy.get('[data-cy="usrOverallProgress-user1"]').contains(' This user hasn\'t completed any quizzes or surveys yet ')
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="quizRunsHistoryTable"]').should("not.be.visible")
    });

    it('expand user progress - no project progress', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createProject(2)
        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)
        cy.runQuizForUser(1, 'user1', [{selectedIndex: [1]}], true)

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-pc-group-section="rowactionbutton"]').click()

        cy.get('[data-cy="usrOverallProgress-user1"]').should('be.visible')
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="projectsProgress"]').contains('This user hasn\'t made progress in any projects yet')
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="projectsProgress"] [data-cy="projProgress-proj1"]').should('not.exist')
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="quizRunsHistoryTable"]').should("be.visible")
    });

    it('expand user progress - filter by quiz name', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createProject(2)
        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)

        cy.createQuizDef(2)
        cy.createQuizQuestionDef(2, 1)

        cy.runQuizForUser(1, 'user1', [{selectedIndex: [1]}], true)
        cy.runQuizForUser(2, 'user1', [{selectedIndex: [0]}], true)

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-pc-group-section="rowactionbutton"]').click()

        cy.get('[data-cy="usrOverallProgress-user1"]').should('be.visible')
        const quizTableSelector = '[data-cy="usrOverallProgress-user1"] [data-cy="quizRunsHistoryTable"]'
        const expectedFull = [
            [{ colIndex: 0, value: 'This is quiz 2'}],
            [{ colIndex: 0, value: 'This is quiz 1'}],
        ];
        cy.validateTable(quizTableSelector, expectedFull);
        cy.get(`${quizTableSelector} [aria-label="Rows per page"]`).should('have.text', 5)

        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="quizNameFilter"]').type(' QuIz 2   {enter}')

        const expectedOne = [
            [{ colIndex: 0, value: 'This is quiz 2'}],
        ];
        cy.validateTable(quizTableSelector, expectedOne);

        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="userResetBtn"]').click()
        cy.validateTable(quizTableSelector, expectedFull);

        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="quizNameFilter"]').type(' !@#^&  {enter}')
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="userFilterBtn"]').click()
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="quizRunsHistoryTable"] [data-pc-section="emptymessage"]').contains('There are no records to show')
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="quizRunsHistoryTable"] [data-cy="tblFilterResetBtn"]').click()
        cy.validateTable(quizTableSelector, expectedFull);
    });

    it('expand user progress - optional fields', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createProject(2)
        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)

        cy.createQuizDef(2)
        cy.createQuizQuestionDef(2, 1)

        cy.runQuizForUser(1, 'user1', [{selectedIndex: [1]}], true)
        cy.runQuizForUser(2, 'user1', [{selectedIndex: [0]}], true)

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-pc-group-section="rowactionbutton"]').click()

        cy.get('[data-cy="usrOverallProgress-user1"]').should('be.visible')
        const quizTableSelector = '[data-cy="usrOverallProgress-user1"] [data-cy="quizRunsHistoryTable"]'

        const expectedHeaders = ['Name', 'Type', 'Status', 'Started']
        cy.get(`${quizTableSelector} [data-pc-name="headercell"]`)
            .should('have.length', expectedHeaders.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeaders[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })

        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="quizRunTable-additionalColumns"] [data-pc-section="dropdown"]').click()

        cy.get('[data-pc-section="listcontainer"] [aria-label="Results"]').click()
        cy.get('[data-pc-section="listcontainer"] [aria-label="Runtime"]').click()
        cy.realPress('Escape');

        const expectedHeadersAfter = ['Name', 'Type', 'Status', 'Runtime', 'Results', 'Started']
        cy.get(`${quizTableSelector} [data-pc-name="headercell"]`)
            .should('have.length', expectedHeadersAfter.length)
            .each(($el, index) => {
                const actualText = $el.text().trim()
                const expectedText = expectedHeadersAfter[index]
                expect(actualText, `Header at index ${index}`).to.equal(expectedText)
            })

        const expectedFull = [
            [{ colIndex: 0, value: 'This is quiz 2'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'},  { colIndex: 4, value: '1 correct out of 1'}],
            [{ colIndex: 0, value: 'This is quiz 1'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'},  { colIndex: 4, value: '0 correct out of 1'}],
        ];

        cy.validateTable(quizTableSelector, expectedFull);
    });

    it('expand user progress - only 1 user can be expanded at a time', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 })
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1 })
        cy.createProject(2)
        cy.createQuizDef(1)
        cy.reportSkill(1, 1, 'user1')
        cy.reportSkill(1, 2, 'user1')
        cy.reportSkill(1, 1, 'user2')

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-pc-group-section="rowactionbutton"]').click()

        cy.get('[data-cy="usrOverallProgress-user1"]').should('be.visible')
        cy.get('[data-cy="usrOverallProgress-user2"]').should('not.exist')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="1"] [data-pc-group-section="rowactionbutton"]').click()

        cy.get('[data-cy="usrOverallProgress-user2"]').should('be.visible')
        cy.get('[data-cy="usrOverallProgress-user1"]').should('not.exist')

    });

    it('paging', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createProject(2)
        cy.createQuizDef(1)
        const users = ['user1', 'user2', 'user3', 'user4', 'user5', 'user6', 'user7', 'user8', 'user9', 'user10', 'user11', 'user12'].sort()
        users.forEach(user => cy.reportSkill(1, 1, user))

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')
        cy.get('[data-cy="userOverallProgressTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '12')


        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')
        cy.get('[data-cy="userOverallProgressTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '12')
        const expected = users.map((user) => {
            return [{colIndex: 1, value: user}]
        })
        cy.validateTable('[data-cy="userOverallProgressTable"]', expected, 10);

    });

    it('navigate to user project progress', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createProject(2)
        cy.createQuizDef(1)
        cy.reportSkill(1, 1, 'user1')
        cy.reportSkill(1, 2, 'user1')
        cy.reportSkill(1, 1, 'user2')

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')
        cy.get('[data-cy="userOverallProgressTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')
        cy.get('[data-cy="userOverallProgressTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-pc-group-section="rowactionbutton"]').click()
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="viewInProject"]').click()

        cy.get('[data-cy="subTitle"]').contains('ID: user1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="earnedPoints"]').should('have.text', '200')
    });

    it('navigate to quiz run', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createProject(2)
        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)

        cy.reportSkill(1, 1, 'user1')
        cy.reportSkill(1, 2, 'user1')
        cy.reportSkill(1, 1, 'user2')

        cy.runQuizForUser(1, 'user1', [{selectedIndex: [1]}], true)
        cy.runQuizForUser(1, 'user1', [{selectedIndex: [0]}], true)

        cy.runQuizForUser(1, 'user2', [{selectedIndex: [1]}], true)
        cy.runQuizForUser(1, 'user2', [{selectedIndex: [0]}], true)

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')
        cy.get('[data-cy="userOverallProgressTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')

        cy.get('[data-cy="userOverallProgressTable"] [data-p-index="0"] [data-pc-group-section="rowactionbutton"]').click()

        cy.get('[data-cy="usrOverallProgress-user1"]').should('be.visible')
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="quizRunsHistoryTable"]').should("be.visible")
        cy.get('[data-cy="usrOverallProgress-user1"] [data-cy="quizRunsHistoryTable"] [data-cy="row1-quizPageLink"]').click()

        cy.get('[data-cy="subPageHeader"]').contains('User Run')
        cy.get('[data-cy="userInfoCard"]').contains('user1')
        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
    });

    it('all quiz status types', () => {
        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)

        cy.createQuizDef(2);
        cy.createTextInputQuestionDef(2, 1)

        cy.createQuizDef(3)
        cy.createQuizQuestionDef(3, 1)

        cy.createQuizDef(4);
        cy.createTextInputQuestionDef(4, 1)

        cy.runQuizForUser(1, 'user1', [{selectedIndex: [1]}], true)
        cy.runQuizForUser(1, 'user1', [{selectedIndex: [1]}], true)
        cy.runQuizForUser(1, 'user1', [{selectedIndex: [0]}], true)
        cy.runQuizForUser(2, 'user1', [{selectedIndex: [0]}], true, 'Pass me please')
        cy.runQuizForUser(3, 'user1', [{selectedIndex: [0]}], false)

        cy.runQuizForUser(1, 'user2', [{selectedIndex: [1]}], true)
        cy.runQuizForUser(1, 'user2', [{selectedIndex: [0]}], true)
        cy.runQuizForUser(2, 'user2', [{selectedIndex: [0]}], true, 'My answer')
        cy.runQuizForUser(4, 'user2', [{selectedIndex: [0]}], true, 'My answer1')

        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="userOverallProgressTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '2')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')

        // First user (index 0) quiz status checks
        cy.get(`[data-p-index="0"] [data-cy="userIdForDisplay"]`).contains('user1')
        cy.get(`[data-p-index="0"] [data-cy="quizAttempts"]`).should('contain.text', '5')
        cy.get(`[data-p-index="0"] [data-cy="quizAttemptsPassed"]`).should('have.text', '1')
        cy.get(`[data-p-index="0"] [data-cy="quizAttemptsFailed"]`).should('have.text', '2')
        cy.get(`[data-p-index="0"] [data-cy="quizAttemptsInProgress"]`).should('have.text', '1')
        cy.get(`[data-p-index="0"] [data-cy="quizAttemptsNeedsGrading"]`).should('have.text', '1')

        // Second user (index 1) quiz status checks
        cy.get(`[data-p-index="1"] [data-cy="userIdForDisplay"]`).contains('user2')
        cy.get(`[data-p-index="1"] [data-cy="quizAttempts"]`).should('contain.text', '4')
        cy.get(`[data-p-index="1"] [data-cy="quizAttemptsPassed"]`).should('have.text', '1')
        cy.get(`[data-p-index="1"] [data-cy="quizAttemptsFailed"]`).should('have.text', '1')
        cy.get(`[data-p-index="1"] [data-cy="quizAttemptsInProgress"]`).should('not.exist')
        cy.get(`[data-p-index="1"] [data-cy="quizAttemptsNeedsGrading"]`).should('have.text', '2')
    });

});
