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

describe('Client Display Quiz on a Skill Tests', () => {

    let defaultUser
    let defaultUserDisplay
    beforeEach(() => {
        defaultUser = Cypress.env('proxyUser')
        defaultUserDisplay = Cypress.env('oauthMode') ? 'foo' : Cypress.env('proxyUser')
    })

    it('display quiz tag and alert ', () => {
        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1);

        cy.createQuizDef(2, { name: 'Trivia Knowledge 1' });
        cy.createQuizQuestionDef(2, 1);
        cy.createQuizQuestionDef(2, 2);

        cy.createQuizDef(3, { name: 'Trivia Knowledge 2' });

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1,
            description: 'Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?',
            helpUrl: 'http://doHelpOnThisSkill.com'
        });
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz2',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 4, { selfReportingType: 'Quiz', quizId: 'quiz3',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="selfReportQuizTag"]')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportQuizTag"]').should('not.exist')

        cy.get('[data-cy=toggleSkillDetails]').click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="takeQuizBtn"]').should('be.enabled')
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="quizAlert"]').contains('Pass the 1-question Trivia Knowledge Quiz and earn 150 points')
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="quizRequirementCard"]').contains('Pass Trivia Knowledge quiz to earn the skill')

        cy.get('[data-cy="skillDescription-skill2"] [data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="quizAlert"]').should('not.exist')
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="quizRequirementCard"]').should('not.exist')

        cy.get('[data-cy="skillDescription-skill3"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillDescription-skill3"] [data-cy="quizAlert"]').contains('Pass the 2-question Trivia Knowledge 1 Quiz and earn 150 points')
        cy.get('[data-cy="skillDescription-skill3"] [data-cy="quizRequirementCard"]').contains('Pass Trivia Knowledge 1 quiz to earn the skill')

        cy.get('[data-cy="skillDescription-skill4"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillDescription-skill4"] [data-cy="quizAlert"]').contains('Pass the Trivia Knowledge 2 Quiz and earn 150 points')
        cy.get('[data-cy="skillDescription-skill4"] [data-cy="quizRequirementCard"]').contains('Pass Trivia Knowledge 2 quiz to earn the skill')

        cy.cdClickSkill(0);
        cy.get('[data-cy="selfReportQuizTag"]')
        cy.get('[data-cy="takeQuizBtn"]').should('be.enabled')
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="quizAlert"]').contains('Pass the 1-question Trivia Knowledge Quiz and earn 150 points')
        cy.get('[data-cy="quizRequirementCard"]').contains('Pass Trivia Knowledge quiz to earn the skill')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.get('[data-cy="selfReportQuizTag"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="quizAlert"]').should('not.exist')
        cy.get('[data-cy="quizRequirementCard"]').should('not.exist')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="quizAlert"]').contains('Pass the 2-question Trivia Knowledge 1 Quiz and earn 150 points')
        cy.get('[data-cy="quizRequirementCard"]').contains('Pass Trivia Knowledge 1 quiz to earn the skill')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(3);
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="quizAlert"]').contains('Pass the Trivia Knowledge 2 Quiz and earn 150 points')
        cy.get('[data-cy="quizRequirementCard"]').contains('Pass Trivia Knowledge 2 quiz to earn the skill')
    });

    it('display survey tag and alert ', () => {
        cy.createSurveyDef(1, { name: 'Trivia Knowledge' });
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createSurveyDef(2, { name: 'Trivia Knowledge 1' });
        cy.createSurveyMultipleChoiceQuestionDef(2, 1);
        cy.createSurveyMultipleChoiceQuestionDef(2, 2);

        cy.createSurveyDef(3, { name: 'Trivia Knowledge 2' });

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1,
            description: 'Sed ut perspiciatis unde omnis iste natus error sit voluptatem accusantium doloremque laudantium, totam rem aperiam, eaque ipsa quae ab illo inventore veritatis et quasi architecto beatae vitae dicta sunt explicabo. Nemo enim ipsam voluptatem quia voluptas sit aspernatur aut odit aut fugit, sed quia consequuntur magni dolores eos qui ratione voluptatem sequi nesciunt. Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat voluptatem. Ut enim ad minima veniam, quis nostrum exercitationem ullam corporis suscipit laboriosam, nisi ut aliquid ex ea commodi consequatur? Quis autem vel eum iure reprehenderit qui in ea voluptate velit esse quam nihil molestiae consequatur, vel illum qui dolorem eum fugiat quo voluptas nulla pariatur?',
            helpUrl: 'http://doHelpOnThisSkill.com'
        });
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz2',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 4, { selfReportingType: 'Quiz', quizId: 'quiz3',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="selfReportSurveyTag"]')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportQuizTag"]').should('not.exist')

        cy.get('[data-cy=toggleSkillDetails]').click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="takeQuizBtn"]').should('be.enabled')
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="quizAlert"]').contains('Complete the 1-question Trivia Knowledge Survey and earn 150 points')
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="quizRequirementCard"]').contains('Complete Trivia Knowledge survey to earn the skill')

        cy.get('[data-cy="skillDescription-skill2"] [data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="quizAlert"]').should('not.exist')
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="quizRequirementCard"]').should('not.exist')

        cy.get('[data-cy="skillDescription-skill3"] [data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="skillDescription-skill3"] [data-cy="quizAlert"]').contains('Complete the 2-question Trivia Knowledge 1 Survey and earn 150 points')
        cy.get('[data-cy="skillDescription-skill3"] [data-cy="quizRequirementCard"]').contains('Complete Trivia Knowledge 1 survey to earn the skill')

        cy.get('[data-cy="skillDescription-skill4"] [data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="skillDescription-skill4"] [data-cy="quizAlert"]').contains('Complete the Trivia Knowledge 2 Survey and earn 150 points')
        cy.get('[data-cy="skillDescription-skill4"] [data-cy="quizRequirementCard"]').contains('Complete Trivia Knowledge 2 survey to earn the skill')

        cy.cdClickSkill(0);
        cy.get('[data-cy="selfReportSurveyTag"]')
        cy.get('[data-cy="takeQuizBtn"]').should('be.enabled')
        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="quizAlert"]').contains('Complete the 1-question Trivia Knowledge Survey and earn 150 points')
        cy.get('[data-cy="quizRequirementCard"]').contains('Complete Trivia Knowledge survey to earn the skill')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.get('[data-cy="selfReportQuizTag"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="quizAlert"]').should('not.exist')
        cy.get('[data-cy="quizRequirementCard"]').should('not.exist')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="quizAlert"]').contains('Complete the 2-question Trivia Knowledge 1 Survey and earn 150 points')
        cy.get('[data-cy="quizRequirementCard"]').contains('Complete Trivia Knowledge 1 survey to earn the skill')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(3);
        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="quizAlert"]').contains('Complete the Trivia Knowledge 2 Survey and earn 150 points')
        cy.get('[data-cy="quizRequirementCard"]').contains('Complete Trivia Knowledge 2 survey to earn the skill')
    });

    it('display quiz results on completed skill', () => {
        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createTextInputQuestionDef(1, 3)
        cy.runQuizForUser(1, defaultUser, [{selectedIndex: [0]}, {selectedIndex: [0, 2]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.gradeQuizAttempt(1, true)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1,
            {
                selfReportingType: 'Quiz',
                quizId: 'quiz1',
                pointIncrement: '150',
                numPerformToCompletion: 1,
        });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="quizCompletedMsg"]').contains('Congratulations! You have passed Trivia Knowledge Quiz')
        cy.get('[data-cy="quizRequirementCard"]').contains('You passed Trivia Knowledge quiz. Well done!')

        cy.get('[data-cy="questionDisplayCard-1"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"]').should('not.exist')
        cy.get('[data-cy="viewQuizAttemptInfo"]').should('be.enabled').click()

        // q1
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').contains('Question 1 - First Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_displayText"]').contains('Question 1 - Second Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').contains('Question 1 - Third Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-1"] [data-cy="notSelected"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-2"] [data-cy="notSelected"]')

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-0_displayText"]').contains('First Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-1_displayText"]').contains('Second Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-2_displayText"]').contains('Third Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-3_displayText"]').contains('Fourth Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-1"] [data-cy="notSelected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-2"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-3"] [data-cy="notSelected"]')

        // q2
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="grader"]').contains(defaultUserDisplay)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"] [data-cy="feedback"]').contains('Good answer')

        // collapse
        cy.get('[data-cy="viewQuizAttemptInfo"]').contains('Hide Quiz Results')
        cy.get('[data-cy="viewQuizAttemptInfo"]').should('be.enabled').click()
        cy.get('[data-cy="questionDisplayCard-1"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"]').should('not.exist')
        cy.get('[data-cy="viewQuizAttemptInfo"]').contains('View Quiz Results')
    })

    it('display survey results on completed skill', () => {
        const quizNum = 1
        cy.createSurveyDef(quizNum);
        cy.createSurveyMultipleChoiceQuestionDef(quizNum, 1, { questionType: 'SingleChoice' });
        cy.createSurveyMultipleChoiceQuestionDef(quizNum, 2);
        cy.createTextInputQuestionDef(quizNum, 3)
        cy.createRatingQuestionDef(quizNum, 4)
        cy.runQuizForUser(1, defaultUser, [
            {selectedIndex: [0]},
            {selectedIndex: [0, 2]},
            {selectedIndex: [0]},
            {selectedIndex: [0]},
        ], true, 'My Answer')

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1,
            {
                selfReportingType: 'Quiz',
                quizId: 'quiz1',
                pointIncrement: '150',
                numPerformToCompletion: 1,
            });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="quizCompletedMsg"]').contains('Congratulations! You have completed This is survey 1 Survey')
        cy.get('[data-cy="quizRequirementCard"]').contains('You completed This is survey 1 survey. Well done!')

        cy.get('[data-cy="questionDisplayCard-1"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"]').should('not.exist')
        cy.get('[data-cy="viewQuizAttemptInfo"]').should('be.enabled').click()

        // q1
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').contains('Question 1 - First Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_displayText"]').contains('Question 1 - Second Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').contains('Question 1 - Third Answer')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-1"] [data-cy="notSelected"]')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answerDisplay-2"] [data-cy="notSelected"]')

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-0_displayText"]').contains('First Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-1_displayText"]').contains('Second Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-2_displayText"]').contains('Third Answer')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-1"] [data-cy="notSelected"]')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answerDisplay-2"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]')

        // q3
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="wrongAnswer"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="TextInputAnswer"]').contains('My Answer')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="manuallyGradedInfo"]').should('not.exist')

        // q4
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 4')
        cy.get('[data-cy="questionDisplayCard-4"] [data-pc-name="rating"] [data-pc-section="item"]')
            .should('have.length', 5).as('ratingItems');

        cy.get('@ratingItems')
            .eq(0)
            .should( 'have.attr', 'data-p-active', 'true')
        cy.get('@ratingItems')
            .eq(1)
            .should( 'have.attr', 'data-p-active', 'false')
        cy.get('@ratingItems')
            .eq(2)
            .should( 'have.attr', 'data-p-active', 'false')
        cy.get('@ratingItems')
            .eq(3)
            .should( 'have.attr', 'data-p-active', 'false')
        cy.get('@ratingItems')
            .eq(4)
            .should( 'have.attr', 'data-p-active', 'false')

        // collapse
        cy.get('[data-cy="viewQuizAttemptInfo"]').contains('Hide Survey Results')
        cy.get('[data-cy="viewQuizAttemptInfo"]').should('be.enabled').click()
        cy.get('[data-cy="questionDisplayCard-1"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"]').should('not.exist')
        cy.get('[data-cy="viewQuizAttemptInfo"]').contains('View Survey Results')
    })

    it('do not show take quiz button if skills has an unfulfilled skill prerequisite', () => {
        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2, { pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 4, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.addLearningPathItem(1, 1, 3)
        cy.addLearningPathItem(1, 2, 4)

        cy.reportSkill(1, 2, Cypress.env('proxyUser'))

        cy.cdVisit('/subjects/subj1/skills/skill3');
        cy.get('[data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')

        cy.cdVisit('/subjects/subj1/skills/skill4');
        cy.get('[data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="takeQuizBtn"]').should('exist')

        // from skill under a subject with skills expanded
        cy.cdVisit('/subjects/subj1');
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="takeQuizBtn"]').should('exist')
    });

    it('do not show take quiz button if skills has an unfulfilled badge prerequisite', () => {
        cy.createQuizDef(1, { name: 'Trivia Knowledge' });
        cy.createQuizQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2, { pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 4, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.enableBadge(1, 1)

        cy.createBadge(1, 2)
        cy.assignSkillToBadge(1, 2, 2)
        cy.enableBadge(1, 2)

        cy.createBadge(1, 3)
        cy.assignSkillToBadge(1, 3, 3)
        cy.enableBadge(1, 3)

        cy.createBadge(1, 4)
        cy.assignSkillToBadge(1, 4, 4)
        cy.enableBadge(1, 4)

        cy.addLearningPathItem(1, 1, 3, true, true)
        cy.addLearningPathItem(1, 2, 4, true, true)

        cy.reportSkill(1, 2, Cypress.env('proxyUser'))

        cy.cdVisit('/subjects/subj1/skills/skill3');
        cy.get('[data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')

        cy.cdVisit('/subjects/subj1/skills/skill4');
        cy.get('[data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="takeQuizBtn"]').should('exist')

        // from skill under a subject with skills expanded
        cy.cdVisit('/subjects/subj1');
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="takeQuizBtn"]').should('exist')

        // from skill under badge
        cy.visit('/progress-and-rankings/projects/proj1/badges/badge3/skills/skill3');
        cy.get('[data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')

        cy.visit('/progress-and-rankings/projects/proj1/badges/badge4/skills/skill4');
        cy.get('[data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="takeQuizBtn"]').should('exist')

        // from skill under a badge with skills expanded
        cy.visit('/progress-and-rankings/projects/proj1/badges/badge3');
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="takeQuizBtn"]').should('not.exist')

        cy.visit('/progress-and-rankings/projects/proj1/badges/badge4');
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="takeQuizBtn"]').should('exist')

    });
});


