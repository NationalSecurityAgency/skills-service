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

describe('Project and Quiz Filtering for Global Users Progress', () => {

    const tableSelector = '[data-cy="userOverallProgressTable"]'

    before(() => {
        cy.beforeTestSuiteThatReusesData()

        const createProj = (num) => {
            cy.createProject(num)
            cy.createSubject(num, 1)
            cy.createSkill(num, 1, 1)
            cy.createSkill(num, 1, 2)
        }

        createProj(1)
        createProj(2)
        createProj(3)

        const creatQuiz = (num) => {
            cy.createQuizDef(num)
            cy.createQuizQuestionDef(num, 1)
        }
        const creatSurvey = (num) => {
            cy.createSurveyDef(num)
            cy.createSurveyMultipleChoiceQuestionDef(num, 1)
        }

        creatQuiz(1)
        creatQuiz(2)
        creatSurvey(3)

        const users = ['user1', 'user2', 'user3', 'user4', 'user5', 'user6']
        cy.reportSkill(1, 1, users[0], 'now')
        cy.reportSkill(2, 1, users[1], 'now')
        cy.reportSkill(3, 1, users[2], 'now')

        cy.runQuizForUser(1, users[3], [{selectedIndex: [0]}], true)
        cy.runQuizForUser(2, users[4], [{selectedIndex: [0]}], true)
        cy.runQuizForUser(3, users[5], [{selectedIndex: [0]}], true)
    })

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    beforeEach(() => {
        cy.execSql('delete from settings where setting = \'globalMetricsExcludedItem\'', true)
        cy.execSql('delete from quiz_settings where setting = \'globalMetricsExcludedItem\'', true)
    })

    it('exclude a project', () => {
        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user3' }],
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user5' }],
            [{ colIndex: 1,  value: 'user6' }],
        ], 10);
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').should('not.exist')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Projects')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('6 Skills')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Quizzes and 1 Survey')

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()

        cy.get('[data-pc-name="dialog"] [data-pc-section="title"]').contains('Configure Included Projects')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="header"]').contains("Included Projects")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="header"]').contains("Excluded Projects")

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is project 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').contains("This is project 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="4"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').should('not.exist')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovetotargetbutton"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is project 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="2"]').should('not.exist')

        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').contains('1 Project Excluded')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('2 Projects')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('4 Skills')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Quizzes and 1 Survey')
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user3' }],
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user5' }],
            [{ colIndex: 1,  value: 'user6' }],
        ], 10);

        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').contains('1 Project Excluded')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('2 Projects')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('4 Skills')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Quizzes and 1 Survey')
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user3' }],
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user5' }],
            [{ colIndex: 1,  value: 'user6' }],
        ], 10);
    })

    it('exclude quizzes', () => {
        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user3' }],
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user5' }],
            [{ colIndex: 1,  value: 'user6' }],
        ], 10);
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedAssessments"]').should('not.exist')
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').should('not.exist')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Projects')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('6 Skills')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Quizzes and 1 Survey')

        cy.get('[data-cy="confQuizExclusionBtn"]').click()

        cy.get('[data-pc-name="dialog"] [data-pc-section="title"]').contains('Configure Included Assessments')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="header"]').contains("Included Assessments")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="header"]').contains("Excluded Assessments")


        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("(Survey)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="4"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').should('not.exist')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovetotargetbutton"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="3"]').should('not.exist')

        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedAssessments"]').contains('2 Assessments Excluded')
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').should('not.exist')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Projects')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('6 Skills')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('1 Assessment')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('1 Quiz and 0 Surveys')
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user3' }],
            [{ colIndex: 1,  value: 'user4' }],
        ], 10);

        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedAssessments"]').contains('2 Assessments Excluded')
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').should('not.exist')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('3 Projects')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('6 Skills')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('1 Assessment')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('1 Quiz and 0 Surveys')
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user3' }],
            [{ colIndex: 1,  value: 'user4' }],
        ], 10);
    })

    it('exclude projects and a quiz', () => {
        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user2' }],
            [{ colIndex: 1,  value: 'user3' }],
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user5' }],
            [{ colIndex: 1,  value: 'user6' }],
        ], 10);

        cy.get('[data-cy="confQuizExclusionBtn"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovetotargetbutton"]').click()
        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedAssessments"]').contains('1 Assessment Excluded')

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovetotargetbutton"]').click()

        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').contains('2 Projects Excluded')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user1' }],
            [{ colIndex: 1,  value: 'user4' }],
            [{ colIndex: 1,  value: 'user5' }],
        ], 10);
    })

    it('filter all projects and quizzes', () => {
        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-pc-name="headercell"] [data-pc-section="columntitle"]').contains('User').click();
        cy.wait('@progressMetrics')

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '6')

        cy.get('[data-cy="confQuizExclusionBtn"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovealltotargetbutton"]').click()
        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedAssessments"]').contains('3 Assessments Excluded')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('0 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('0 Quizzes and 0 Surveys')

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovealltotargetbutton"]').click()
        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').contains('3 Projects Excluded')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('0 Projects')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('0 Skills')

        cy.get('[data-cy="noUserOverallProgress"]').should('be.visible')
        cy.get(tableSelector).should('not.be.visible')
    })

    it('project exclusion dialog state', () => {
        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is project 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').contains("This is project 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="4"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').should('not.exist')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovetotargetbutton"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is project 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').should('not.exist')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="2"]').should('not.exist')

        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is project 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="2"]').should('not.exist')

        cy.get('[data-pc-name="dialog"] [data-cy="closeDialogBtn"]').click()

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is project 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is project 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="2"]').should('not.exist')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovealltotargetbutton"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').should('have.length', 3).and(($options) => {
            const optionTexts = $options.map((_, el) => el.textContent.trim()).get();
            expect(optionTexts).to.include.members(["This is project 2", "This is project 1", "This is project 3"]);
        });
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="4"]').should('not.exist')

        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').should('have.length', 3).and(($options) => {
            const optionTexts = $options.map((_, el) => el.textContent.trim()).get();
            expect(optionTexts).to.include.members(["This is project 2", "This is project 1", "This is project 3"]);
        });
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="4"]').should('not.exist')
    })

    it('quiz exclusion dialog state', () => {
        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get('[data-cy="confQuizExclusionBtn"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("(Survey)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="4"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').should('not.exist')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovetotargetbutton"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("(Survey)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="2"]').should('not.exist')

        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="confQuizExclusionBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("(Survey)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="2"]').should('not.exist')

        cy.get('[data-pc-name="dialog"] [data-cy="closeDialogBtn"]').click()

        cy.get('[data-cy="confQuizExclusionBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("(Survey)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="1"]').contains("(Quiz)")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="2"]').should('not.exist')

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovealltotargetbutton"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').should('have.length', 3)
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="4"]').should('not.exist')

        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="confQuizExclusionBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').should('not.exist')
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').should('have.length', 3)
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').contains("This is survey 3")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').contains("This is quiz 2")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"]').contains("This is quiz 1")
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="targetlistcontainer"] [data-pc-section="option"][aria-posinset="4"]').should('not.exist')
    })

    it('configured filter applies to the global metrics page', () => {
        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '6')

        cy.get('[data-cy="confQuizExclusionBtn"]').click()

        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="1"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovetotargetbutton"]').click()
        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="2"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-section="sourcelistcontainer"] [data-pc-section="option"][aria-posinset="3"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"] [data-pc-name="pcmovetotargetbutton"]').click()

        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedAssessments"]').contains('1 Assessment Excluded')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('2 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Quizzes and 0 Surveys')
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').contains('2 Projects Excluded')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('1 Project')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Skills')

        cy.get('[data-cy="nav-Metrics"]').click()
        cy.get('[data-cy="distinctNumUsersOverTime"]').should('be.visible')

        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedAssessments"]').contains('1 Assessment Excluded')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardTitle"]').contains('2 Assessments')
        cy.get('[data-cy="overallMetricsQuizzesAndSurveysCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Quizzes and 0 Surveys')
        cy.get('[data-cy="overallMetricsCards"] [data-cy="numExcludedProjects"]').contains('2 Projects Excluded')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardTitle"]').contains('1 Project')
        cy.get('[data-cy="overallMetricsProjectsCard"] [data-cy="mediaInfoCardSubTitle"]').contains('2 Skills')
    })

    it('canceling config dialog returns focus to the configure button', () => {
        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '6')

        cy.get('[data-cy="confQuizExclusionBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="confQuizExclusionBtn"]').should("have.focus")

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="confIncludedProjectsBtn"]').should("have.focus")

        cy.get('[data-cy="confQuizExclusionBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcclosebutton"]').click()
        cy.get('[data-cy="confQuizExclusionBtn"]').should("have.focus")

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcclosebutton"]').click()
        cy.get('[data-cy="confIncludedProjectsBtn"]').should("have.focus")
    })

    it('saving config dialog returns focus to the configure button', () => {
        cy.intercept('/app/progress-metrics**').as('progressMetrics')
        cy.visit('/administrator/users-progress');
        cy.wait('@progressMetrics')

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '6')

        cy.get('[data-cy="confQuizExclusionBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="confQuizExclusionBtn"]').should("have.focus")

        cy.get('[data-cy="confIncludedProjectsBtn"]').click()
        cy.get('[data-cy="confIncludedMetricsDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="confIncludedProjectsBtn"]').should("have.focus")
    })
});

