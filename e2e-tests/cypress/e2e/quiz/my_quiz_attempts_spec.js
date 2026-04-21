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
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Display History of My quiz attempts Tests', () => {

    const tableSelector = '[data-cy="myQuizAttemptsTable"]'
    let defaultUser
    beforeEach(() => {
        defaultUser = Cypress.env('proxyUser')
    })

    it('No Attempts', () => {
        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get('[data-cy="noQuizzesOrSurveys"]')
    })

    it('one quiz', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}], true, 'My Answer')

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'This is quiz 1'
            }, {
                colIndex: 1,
                value: 'Quiz'
            }, {
                colIndex: 2,
                value: 'Needs Grading'
            }, {
                colIndex: 3,
                value: 'N/A'
            }],
        ], 5);

    });

    it('quiz and surveys with associated skills', () => {
        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});
        cy.createQuizQuestionDef(1, 1)

        cy.createQuizDef(2, {name: 'Science Knowledge'});
        cy.createQuizQuestionDef(2, 1)

        cy.createQuizDef(3, {name: 'Lonely Duck'});
        cy.createQuizQuestionDef(3, 1)

        cy.createQuizDef(4, {name: 'Natural Sciences'});
        cy.createQuizQuestionDef(4, 1)

        cy.createProject(1, {name: 'Learn lots of Trivia'})
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            name: 'Fun Facts', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz1',
        });
        cy.createSkill(1, 1, 2, {
            name: 'Research Skills', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz2',
        });
        cy.createSkill(1, 1, 3, {
            name: 'Data Analysis', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz2',
        });
        cy.createSkill(1, 1, 4, {
            name: 'Logical Reasoning', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });

        cy.createProject(2, {name: 'Critical Thinking'})
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 1, {
            name: 'Hypothesis Testing', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 2, {
            name: 'Research Skills', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 3, {
            name: 'Experimental Design', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 4, {
            name: 'Applied Science', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 5, {
            name: 'Natural Sciences', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 6, {
            name: 'Empirical Analysis', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });

        cy.runQuizForUser(4, defaultUser, [{selectedIndex: [1]}], true)
        cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}], true)
        cy.runQuizForUser(2, defaultUser, [{selectedIndex: [0]}], true)
        cy.runQuizForUser(3, defaultUser, [{selectedIndex: [1]}], true)
        cy.runQuizForUser(4, defaultUser, [{selectedIndex: [0]}], true)

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: 'Natural Sciences'},
                { colIndex: 3, value: 'Applied Science'}, { colIndex: 3, value: 'in Critical Thinking'},
                { colIndex: 3, value: 'Empirical Analysis'}, { colIndex: 3, value: 'View 5 More'}],
            [{ colIndex: 0, value: 'Lonely Duck'},  { colIndex: 3, value: 'N/A'}],
            [{ colIndex: 0, value: 'Science Knowledge'},
                { colIndex: 3, value: 'Data Analysis'}, { colIndex: 3, value: 'in Learn lots of Trivia'},
                { colIndex: 3, value: 'Research Skills'}],
            [{ colIndex: 0, value: 'Test Your Trivia Knowledge'},
                { colIndex: 3, value: 'Fun Facts'}, { colIndex: 3, value: 'in Learn lots of Trivia'}, ],
            [{ colIndex: 0, value: 'Natural Sciences'},
                { colIndex: 3, value: 'Applied Science'}, { colIndex: 3, value: 'in Critical Thinking'},
                { colIndex: 3, value: 'Empirical Analysis'}, { colIndex: 3, value: 'View 5 More'}],
        ], 5);

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]')
        cy.get('[data-p-index="1"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').should('not.exist')
        cy.get('[data-p-index="2"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').should('not.exist')
        cy.get('[data-p-index="3"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').should('not.exist')
        cy.get('[data-p-index="4"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').contains('View 5 More')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill4"] [data-cy="viewSkillLink"]').should( 'have.text','Applied Science')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill6"] [data-cy="viewSkillLink"]').should( 'have.text','Empirical Analysis')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill3"]').should('not.exist')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill1"]').should('not.exist')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill2"]').should('not.exist')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill5"]').should('not.exist')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill4"]').should('not.exist')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').click()
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').contains('Show Less')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill4"] [data-cy="viewSkillLink"]').should( 'have.text', 'Applied Science')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill4"] [data-cy="projectName"]').should( 'have.text','Critical Thinking')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill6"] [data-cy="viewSkillLink"]').should('have.text', 'Empirical Analysis')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill6"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill3"] [data-cy="viewSkillLink"]').should('have.text', 'Experimental Design')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill3"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill1"] [data-cy="viewSkillLink"]').should('have.text', 'Hypothesis Testing')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill1"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill4"] [data-cy="viewSkillLink"]').should('have.text', 'Logical Reasoning')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill4"] [data-cy="projectName"]').should('have.text','Learn lots of Trivia')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill5"] [data-cy="viewSkillLink"]').should('have.text', 'Natural Sciences')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill5"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill2"] [data-cy="viewSkillLink"]').should('have.text', 'Research Skills')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill2"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').click()
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').contains('View 5 More')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill3"]').should('not.exist')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill1"]').should('not.exist')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill2"]').should('not.exist')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill5"]').should('not.exist')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill4"]').should('not.exist')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill4"]  [data-cy="viewSkillLink"]').click()
        cy.get('[data-cy="title"]').contains('Skill Overview')
        cy.get('[data-cy="skillProgressTitle"]').contains('Applied Science')
        cy.go('back')

        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').click()
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="expandOrCollapseSkills"]').contains('Show Less')
        cy.get('[data-p-index="0"] [data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill4"] [data-cy="viewSkillLink"]').click()
        cy.get('[data-cy="title"]').contains('Skill Overview')
        cy.get('[data-cy="skillProgressTitle"]').contains('Logical Reasoning')
    });

    it('paging with quizzes and surveys', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}], true, 'My Answer')

        for (let i = 0; i < 12; i++) {
            const quizNum = i + 2
            if (i % 3 === 0) {
                cy.createSurveyDef(quizNum);
                cy.createSurveyMultipleChoiceQuestionDef(quizNum, 1, { questionType: 'SingleChoice' });
            } else {
                cy.createQuizDef(quizNum);
                cy.createQuizQuestionDef(quizNum, 1)
            }

            cy.runQuizForUser(quizNum, defaultUser, [{selectedIndex: [i%2===0?0:1]}], true, 'My Answer')
        }

        cy.visit('/progress-and-rankings/my-quiz-attempts');

        const expectedRows = [
            [{ colIndex: 0, value: 'This is quiz 13'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'}],
            [{ colIndex: 0, value: 'This is quiz 12'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
            [{ colIndex: 0, value: 'This is survey 11'}, { colIndex: 1, value: 'Survey'}, { colIndex: 2, value: 'Completed'}],
            [{ colIndex: 0, value: 'This is quiz 10'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
            [{ colIndex: 0, value: 'This is quiz 9'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'}],
            [{ colIndex: 0, value: 'This is survey 8'}, { colIndex: 1, value: 'Survey'}, { colIndex: 2, value: 'Completed'}],
            [{ colIndex: 0, value: 'This is quiz 7'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'}],
            [{ colIndex: 0, value: 'This is quiz 6'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
            [{ colIndex: 0, value: 'This is survey 5'}, { colIndex: 1, value: 'Survey'}, { colIndex: 2, value: 'Completed'}],
            [{ colIndex: 0, value: 'This is quiz 4'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
            [{ colIndex: 0, value: 'This is quiz 3'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'}],
            [{ colIndex: 0, value: 'This is survey 2'}, { colIndex: 1, value: 'Survey'}, { colIndex: 2, value: 'Completed'}],
            [{ colIndex: 0, value: 'This is quiz 1'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Needs Grading'}],
        ]
        cy.validateTable(tableSelector, expectedRows, 10);

        cy.get(`${tableSelector} [data-pc-name="pcrowperpagedropdown"]`).click();
        cy.get('[data-pc-section="list"] [aria-label="20"]').click()
        cy.validateTable(tableSelector, expectedRows, 20);

    });

    it('filter by quiz name', () => {

        cy.createQuizDef(1, {name: `Find me If you Can`});
        cy.createQuizDef(2, {name: `CAN you find me?`});
        cy.createQuizDef(3, {name: `Third quiz I am`});
        for (let i = 0; i < 3; i++) {
            const quizNum = i + 1
            cy.createQuizQuestionDef(quizNum, 1)
            cy.runQuizForUser(quizNum, defaultUser, [{selectedIndex: [i % 2 === 0 ? 0 : 1]}], true, 'My Answer')
        }

        cy.visit('/progress-and-rankings/my-quiz-attempts');

        const expectedRows = [
            [{ colIndex: 0, value: 'Third quiz I am'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
            [{ colIndex: 0, value: 'CAN you find me?'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Failed'}],
            [{ colIndex: 0, value: 'Find me If you Can'}, { colIndex: 1, value: 'Quiz'}, { colIndex: 2, value: 'Passed'}],
        ]
        cy.validateTable(tableSelector, expectedRows, 10);

        cy.get('[data-cy="quizNameFilter"]').type('cAn')
        cy.get('[data-cy="userFilterBtn"]').click()
        cy.validateTable(tableSelector, [expectedRows[1], expectedRows[2]], 10);

        cy.get('[data-cy="filterResetBtn"]').click()
        cy.validateTable(tableSelector, expectedRows, 10);
    });

    it('navigate to single quiz and back', () => {
        for (let i = 0; i < 3; i++) {
            const quizNum = i + 1
            cy.createQuizDef(quizNum);
            cy.createQuizQuestionDef(quizNum, 1)
            cy.runQuizForUser(quizNum, defaultUser, [{selectedIndex: [i % 2 === 0 ? 0 : 1]}], true, 'My Answer')
        }

        cy.visit('/progress-and-rankings/my-quiz-attempts');

        cy.get(`${tableSelector} [data-p-index="1"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 2')
        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="backToQuizzesBtn"]').click()
        cy.get(`${tableSelector} [data-p-index="1"] [data-cy="viewQuizAttempt"]`)

        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 3')
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')
        cy.get('[data-cy="backToQuizzesBtn"]').click()
        cy.get(`${tableSelector} [data-p-index="1"] [data-cy="viewQuizAttempt"]`)

    });

    it('failed quiz attempt will show "try again" button on latest attempt', () => {
      for (let i = 0; i < 3; i++) {
        const quizNum = i + 1
        cy.createQuizDef(quizNum)
        cy.createQuizQuestionDef(quizNum, 1)
        cy.runQuizForUser(quizNum, defaultUser, [{ selectedIndex: [i % 2 === 0 ? 0 : 1] }], true, 'My Answer')
      }

      cy.visit('/progress-and-rankings/my-quiz-attempts')

      cy.get(`${tableSelector} [data-p-index="1"] [data-cy="viewQuizAttempt"]`).first().click()
      cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 2')
      cy.get('[data-cy="quizRunStatus"]').contains('Failed')

      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('be.visible')
      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('be.enabled')
      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('have.text', 'Try Again')

      cy.get('[data-cy="runQuizAgainBtn"]').should('be.visible')
      cy.get('[data-cy="runQuizAgainBtn"]').should('be.enabled')
      cy.get('[data-cy="runQuizAgainBtn"]').should('have.text', 'Try Again')

      // Get initial window count
      cy.window().then((win) => {
        const initialWindowCount = win.length;

        cy.get('[data-cy="runQuizAgainBtn"]').click()
        cy.get('[data-cy="quizSplashScreen"]').should('contain.text', 'This is quiz 2')

        // Assert no new window was opened
        cy.window().should((win) => {
          expect(win.length).to.equal(initialWindowCount);
        });
      });
    });

    it('failed quiz will show "try again" button on earlier attempt if quiz has NOT already passed', () => {
      cy.createQuizDef(1)
      cy.createQuizQuestionDef(1, 1)
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [1] }], true, 'My Answer')
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [1] }], true, 'My Answer')

      cy.visit('/progress-and-rankings/my-quiz-attempts')

      cy.get(`${tableSelector} [data-p-index="1"] [data-cy="viewQuizAttempt"]`).first().click()
      cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
      cy.get('[data-cy="quizRunStatus"]').contains('Failed')

      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('be.visible')
      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('be.enabled')
      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('have.text', 'Try Again')
      cy.get('[data-cy="runQuizAgainBtn"]').should('be.visible')
      cy.get('[data-cy="runQuizAgainBtn"]').should('be.enabled')
      cy.get('[data-cy="runQuizAgainBtn"]').should('have.text', 'Try Again')
    });

    it('failed quiz will NOT show "try again" button on earlier attempt if quiz has already passed', () => {
      cy.createQuizDef(1)
      cy.createQuizQuestionDef(1, 1)
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [1] }], true, 'My Answer')
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [0] }], true, 'My Answer')

      cy.visit('/progress-and-rankings/my-quiz-attempts')

      cy.get(`${tableSelector} [data-p-index="1"] [data-cy="viewQuizAttempt"]`).first().click()
      cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
      cy.get('[data-cy="quizRunStatus"]').contains('Failed')

      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('not.exist')
      cy.get('[data-cy="runQuizAgainBtn"]').should('not.exist')
    });

    it('failed quiz will NOT show "try again" button when max attempts have been exhausted', () => {
      cy.createQuizDef(1)
      cy.createQuizQuestionDef(1, 1)
      cy.setQuizMaxNumAttempts(1, 2)
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [1] }], true, 'My Answer')
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [1] }], true, 'My Answer')

      cy.visit('/progress-and-rankings/my-quiz-attempts')

      cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
      cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
      cy.get('[data-cy="quizRunStatus"]').contains('Failed')
      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('not.exist')
      cy.get('[data-cy="runQuizAgainBtn"]').should('not.exist')

      cy.get('[data-cy="backToQuizzesBtn"]').click()

      cy.get(`${tableSelector} [data-p-index="1"] [data-cy="viewQuizAttempt"]`).first().click()
      cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
      cy.get('[data-cy="quizRunStatus"]').contains('Failed')
      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('not.exist')
      cy.get('[data-cy="runQuizAgainBtn"]').should('not.exist')

    });

    it('failed quiz will NOT show "try again" button on for surveys', () => {
      cy.createSurveyDef(1)
      cy.createSurveyMultipleChoiceQuestionDef(1, 1, { questionType: 'SingleChoice' });
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [1] }], true, 'My Answer')

      cy.visit('/progress-and-rankings/my-quiz-attempts')

      cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
      cy.get('[data-cy="quizName"]').should('have.text', 'This is survey 1')
      cy.get('[data-cy="quizRunStatus"]').contains('Completed')
      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('not.exist')
      cy.get('[data-cy="runQuizAgainBtn"]').should('not.exist')
    });

    it('passed quiz will NOT show "try again" button', () => {
      cy.createQuizDef(1)
      cy.createQuizQuestionDef(1, 1)
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [0] }], true, 'My Answer')

      cy.visit('/progress-and-rankings/my-quiz-attempts')

      cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
      cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
      cy.get('[data-cy="quizRunStatus"]').contains('Passed')

      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('not.exist')
      cy.get('[data-cy="runQuizAgainBtn"]').should('not.exist')
    });

    it('passed quiz will show "try again" button when multipleTakes setting is enabled and more attempts are left', () => {
      cy.createQuizDef(1)
      cy.createQuizQuestionDef(1, 1)
      cy.setQuizMaxNumAttempts(1, 2)
      cy.setQuizMultipleTakes(1, true)
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [0] }], true, 'My Answer')

      cy.visit('/progress-and-rankings/my-quiz-attempts')

      cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
      cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
      cy.get('[data-cy="quizRunStatus"]').contains('Passed')

      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('be.visible')
      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('be.enabled')
      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('have.text', 'Retake')
      cy.get('[data-cy="runQuizAgainBtn"]').should('be.visible')
      cy.get('[data-cy="runQuizAgainBtn"]').should('be.enabled')
      cy.get('[data-cy="runQuizAgainBtn"]').should('have.text', 'Retake')
    });

    it('passed quiz will NOT show "try again" button when multipleTakes setting is enabled and no more attempts are left', () => {
      cy.createQuizDef(1)
      cy.createQuizQuestionDef(1, 1)
      cy.setQuizMaxNumAttempts(1, 2)
      cy.setQuizMultipleTakes(1, true)
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [0] }], true, 'My Answer')
      cy.runQuizForUser(1, defaultUser, [{ selectedIndex: [0] }], true, 'My Answer')

      cy.visit('/progress-and-rankings/my-quiz-attempts')

      cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
      cy.get('[data-cy="quizName"]').should('have.text', 'This is quiz 1')
      cy.get('[data-cy="quizRunStatus"]').contains('Passed')

      cy.get('[data-cy="runQuizAgainBtnInCard"]').should('not.exist')
      cy.get('[data-cy="runQuizAgainBtn"]').should('not.exist')
    });

    it('single quiz run with associated skills', () => {
        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});
        cy.createQuizQuestionDef(1, 1)

        cy.createQuizDef(2, {name: 'Science Knowledge'});
        cy.createQuizQuestionDef(2, 1)

        cy.createQuizDef(3, {name: 'Lonely Duck'});
        cy.createQuizQuestionDef(3, 1)

        cy.createQuizDef(4, {name: 'Natural Sciences'});
        cy.createQuizQuestionDef(4, 1)

        cy.createProject(1, {name: 'Learn lots of Trivia'})
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            name: 'Fun Facts', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz1',
        });
        cy.createSkill(1, 1, 2, {
            name: 'Research Skills', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz2',
        });
        cy.createSkill(1, 1, 3, {
            name: 'Data Analysis', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz2',
        });
        cy.createSkill(1, 1, 4, {
            name: 'Logical Reasoning', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });

        cy.createProject(2, {name: 'Critical Thinking'})
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 1, {
            name: 'Hypothesis Testing', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 2, {
            name: 'Research Skills', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 3, {
            name: 'Experimental Design', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 4, {
            name: 'Applied Science', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 5, {
            name: 'Natural Sciences', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });
        cy.createSkill(2, 1, 6, {
            name: 'Empirical Analysis', pointIncrement: '150', numPerformToCompletion: 1,
            selfReportingType: 'Quiz', quizId: 'quiz4',
        });

        cy.runQuizForUser(4, defaultUser, [{selectedIndex: [1]}], true)
        cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}], true)
        cy.runQuizForUser(2, defaultUser, [{selectedIndex: [0]}], true)
        cy.runQuizForUser(3, defaultUser, [{selectedIndex: [1]}], true)

        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get(`${tableSelector} [data-p-index="1"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'Science Knowledge')
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')

        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill2"] [data-cy="viewSkillLink"]')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill3"] [data-cy="viewSkillLink"]')
        cy.get('[data-cy="associatedSkills"] a').should('have.length', 2)
        cy.get('[data-cy="expandOrCollapseSkills"]').should('not.exist')

        cy.go('back')
        cy.get(`${tableSelector} [data-p-index="0"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'Lonely Duck')
        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="associatedSkills"]').should('not.exist')
        cy.get('[data-cy="expandOrCollapseSkills"]').should('not.exist')

        cy.go('back')
        cy.get(`${tableSelector} [data-p-index="2"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'Test Your Trivia Knowledge')
        cy.get('[data-cy="quizRunStatus"]').contains('Passed')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill1"]')
        cy.get('[data-cy="associatedSkills"] a').should('have.length', 1)
        cy.get('[data-cy="expandOrCollapseSkills"]').should('not.exist')

        cy.go('back')
        cy.get(`${tableSelector} [data-p-index="3"] [data-cy="viewQuizAttempt"]`).first().click()
        cy.get('[data-cy="quizName"]').should('have.text', 'Natural Sciences')
        cy.get('[data-cy="quizRunStatus"]').contains('Failed')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill4"]')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill6"]')
        cy.get('[data-cy="associatedSkills"] a').should('have.length', 2)
        cy.get('[data-cy="expandOrCollapseSkills"]').contains('View 5 More')

        cy.get('[data-cy="expandOrCollapseSkills"]').click()
        cy.get('[data-cy="expandOrCollapseSkills"]').contains('Show Less')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill4"] [data-cy="viewSkillLink"]').should( 'have.text', 'Applied Science')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill4"] [data-cy="projectName"]').should( 'have.text','Critical Thinking')

        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill6"] [data-cy="viewSkillLink"]').should('have.text', 'Empirical Analysis')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill6"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill3"] [data-cy="viewSkillLink"]').should('have.text', 'Experimental Design')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill3"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill1"] [data-cy="viewSkillLink"]').should('have.text', 'Hypothesis Testing')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill1"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill4"] [data-cy="viewSkillLink"]').should('have.text', 'Logical Reasoning')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill4"] [data-cy="projectName"]').should('have.text','Learn lots of Trivia')

        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill5"] [data-cy="viewSkillLink"]').should('have.text', 'Natural Sciences')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill5"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill2"] [data-cy="viewSkillLink"]').should('have.text', 'Research Skills')
        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill2"] [data-cy="projectName"]').should('have.text','Critical Thinking')

        cy.get('[data-cy="associatedSkills"] a').should('have.length', 7)

        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj1-skill4"] [data-cy="viewSkillLink"]').click()
        cy.get('[data-cy="title"]').contains('Skill Overview')
        cy.get('[data-cy="skillProgressTitle"]').contains('Logical Reasoning')
        cy.go('back')

        cy.get('[data-cy="associatedSkills"] [data-cy="associatedSkill-proj2-skill6"] [data-cy="viewSkillLink"]').click()
        cy.get('[data-cy="title"]').contains('Skill Overview')
        cy.get('[data-cy="skillProgressTitle"]').contains('Empirical Analysis')
    });

});


