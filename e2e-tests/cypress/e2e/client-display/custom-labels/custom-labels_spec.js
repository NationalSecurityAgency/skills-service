/*
 * Copyright 2025 SkillTree
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

const moment = require("moment-timezone");
describe('Custom Label Tests', () => {


    beforeEach(() => {
        cy.createProject(1);

        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'Work Role',
                setting: 'project.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Competency',
                setting: 'subject.displayName',
                projectId: 'proj1',
            },
            {
                value: 'KSA',
                setting: 'group.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Course',
                setting: 'skill.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Stage',
                setting: 'level.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Unit',
                setting: 'point.displayName',
                projectId: 'proj1',
            },
        ]);
        cy.createSubject(1, 1);
    })

    it('quiz on skill messages', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createSurveyDef(2);
        cy.createSurveyMultipleChoiceQuestionDef(2, 1);
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });

        cy.configureExpiration(1, 0, 1, 'DAILY');

        cy.visit('/test-skills-display/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="quizRequirementCard"] [data-cy="mediaInfoCardSubTitle"]').contains('Pass This is quiz 1 quiz to earn the course');
        cy.get('[data-cy="quizAlert"]').contains('Pass the 1-question This is quiz 1 Quiz and earn 150 units!');

        cy.get('[data-cy="takeQuizBtn"]').click()

        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 units for Very Great Skill 1 course by passing this quiz.');

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="completionSummaryTitle"]').contains('You just earned 150 units for Very Great Skill 1 course by passing the quiz.');

        cy.get('[data-cy="closeQuizBtn"]').click()

        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('perform this course to keep your units!');
        cy.get('[data-cy="takeQuizMsg"]').contains('but your 150 units can be retained by completing the Quiz again')
    });

    it('survey on skill messages', () => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });

        cy.configureExpiration(1, 0, 1, 'DAILY');

        cy.visit('/test-skills-display/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="quizRequirementCard"] [data-cy="mediaInfoCardSubTitle"]').contains('Complete This is survey 1 survey to earn the course');
        cy.get('[data-cy="quizAlert"]').contains('Complete the 1-question This is survey 1 Survey and earn 150 units!');

        cy.get('[data-cy="takeQuizBtn"]').click()

        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 units for Very Great Skill 1 course by completing this survey');

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="surveyCompletion"]').contains('You just earned 150 units for Very Great Skill 1 course by completing the survey.');

        cy.get('[data-cy="closeSurveyBtn"]').click()

        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('perform this course to keep your units!');
        cy.get('[data-cy="takeQuizMsg"]').contains('but your 150 units can be retained by completing the Survey again')
    });

});