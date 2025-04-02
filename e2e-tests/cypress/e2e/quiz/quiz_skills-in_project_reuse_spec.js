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
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Quiz-skills in-project reuse Tests', () => {

    it('re-use quiz-skill into a different subject', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSubject(1, 2);
        cy.createSkillsGroup(1, 1, 3);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
          .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 1 skill.');
        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="breadcrumb-proj1"]').click()
        cy.get('[data-cy="manageBtn_subj2"]').click()
    });

    it('re-use quiz-skill into a group', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSubject(1, 2);
        cy.createSkillsGroup(1, 1, 3);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] thead tr th`).contains('Skill').click()
        cy.get(`[data-cy="skillsTable"] thead tr th`).contains('Skill').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();

        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group3"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
          .should('not.be.visible');
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be reused in the [Awesome Group 3 Subj1] group');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 1 skill.');
        cy.get('[data-cy="okButton"]')
            .click();

        cy.get('[data-cy="nameCell_group3"] [data-cy="numSkillsInGroup"]').contains('1 skill')

        cy.get(`[data-p-index="1"] [data-pc-section="rowtogglebutton"]`).click()

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group3"] [data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Self Report"]').click()
        //close the selector
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group3"] [data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group3"] [data-cy="selfReportCell-skill1STREUSESKILLST0-quiz"]').contains('Survey-Based Validation')
    });

    it('client display - reused quiz-based skills - take quiz through group skill', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSkillsGroup(1, 2, 3);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.reuseSkillIntoAnotherGroup(1, 1, 2, 3);

        cy.cdVisit('/subjects/subj2');
        // validate group skill
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="group-group3Subj2_skillProgress-skill1STREUSESKILLST1"] [data-cy="selfReportQuizTag"]')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="group-group3Subj2_skillProgress-skill1STREUSESKILLST1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportQuizTag"]')
        cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 1')

        cy.get('[data-cy=toggleSkillDetails]').click();

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillDescription-skill1STREUSESKILLST0"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillDescription-skill1STREUSESKILLST0"] [data-cy="quizAlert"]').contains('Pass the 1-question This is quiz 1 Quiz and earn 150 points')

        // group
        cy.get('[data-cy="group-group3Subj2_skillProgress-skill1STREUSESKILLST1"] [data-cy="skillDescription-skill1STREUSESKILLST1"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="group-group3Subj2_skillProgress-skill1STREUSESKILLST1"] [data-cy="skillDescription-skill1STREUSESKILLST1"] [data-cy="quizAlert"]').contains('Pass the 1-question This is quiz 1 Quiz and earn 150 points')

        // run quiz
        cy.get('[data-cy="group-group3Subj2_skillProgress-skill1STREUSESKILLST1"] [data-cy="skillDescription-skill1STREUSESKILLST1"] [data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should('have.text', '150 Total')
    });

    it('client display - reused survey-based skills - take survey through subject-reused skill', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSkillsGroup(1, 2, 3);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.reuseSkillIntoAnotherGroup(1, 1, 2, 3);

        cy.cdVisit('/subjects/subj2');
        // validate group skill
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="group-group3Subj2_skillProgress-skill1STREUSESKILLST1"] [data-cy="selfReportSurveyTag"]')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="group-group3Subj2_skillProgress-skill1STREUSESKILLST1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportSurveyTag"]')
        cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 1')

        cy.get('[data-cy=toggleSkillDetails]').click();

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillDescription-skill1STREUSESKILLST0"] [data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillDescription-skill1STREUSESKILLST0"] [data-cy="quizAlert"]').contains('Complete the 1-question This is survey 1 Survey and earn 150 points')

        // group
        cy.get('[data-cy="group-group3Subj2_skillProgress-skill1STREUSESKILLST1"] [data-cy="skillDescription-skill1STREUSESKILLST1"] [data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="group-group3Subj2_skillProgress-skill1STREUSESKILLST1"] [data-cy="skillDescription-skill1STREUSESKILLST1"] [data-cy="quizAlert"]').contains('Complete the 1-question This is survey 1 Survey and earn 150 points')

        // run test
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillDescription-skill1STREUSESKILLST0"] [data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()
        cy.get('[data-cy="surveyCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by completing the survey.')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="closeSurveyBtn"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should('have.text', '150 Total')
    });

});
