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

describe('Quiz Skills Catalog Tests', () => {

    it('show quiz-skill details in the import modal', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.exportSkillToCatalog(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
        cy.get('[data-cy="skillToImportInfo-proj1_skill1"]').contains('Self Report: Quiz/Survey')
    });

    it('dashboard admin skill table - imported skills do not link to the quiz', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.exportSkillToCatalog(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Self Report"]').click()
        cy.get('[data-cy="selfReportCell-skill1-quiz"]').contains('Survey-Based Validation')
        cy.get('[data-cy="selfReportCell-skill1-quiz"]').contains('This is survey 1').should('not.exist')
    });

    it('run survey from an imported skill', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.exportSkillToCatalog(2, 1, 1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.importSkillFromCatalog(1, 1, 2, 1);
        cy.createSkill(1, 1, 2);
        cy.finalizeCatalogImport(1);

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="catalogImportStatus"]').contains('This skill is originally defined in This is project 2 and re-used in this project!')
        cy.get('[data-cy="selfReportSurveyTag"]')
        cy.get('[data-cy="quizAlert"]').contains('Complete the 1-question This is survey 1 Survey and earn 150 points!')
        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="surveyCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by completing the survey')
        cy.get('[data-cy="surveyCompletion"] [data-cy="closeSurveyBtn"]').click()
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should('have.text', '150 Total')
    });

    it('run quiz from an imported skill', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.exportSkillToCatalog(2, 1, 1);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.importSkillFromCatalog(1, 1, 2, 1);
        cy.createSkill(1, 1, 2);
        cy.finalizeCatalogImport(1);

        cy.cdVisit('/subjects/subj1/skills/skill1');
        // cy.get('[data-cy="catalogImportStatus"]').contains('This skill is originally defined in This is project 2 and re-used in this project!')
        cy.get('[data-cy="selfReportQuizTag"]')
        cy.get('[data-cy="quizAlert"]').contains('Pass the 1-question This is quiz 1 Quiz and earn 150 points')
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizPassed"]')
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz')
        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should('have.text', '150 Total')
        cy.get('[data-cy="quizAlert"]').should('not.exist')
    });

    it('display imported quiz-based skills on subject page', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.createSurveyDef(2);
        cy.createSurveyMultipleChoiceQuestionDef(2, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 2, { selfReportingType: 'Quiz', quizId: 'quiz2',  pointIncrement: '150', numPerformToCompletion: 1 });
        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.importSkillFromCatalog(1, 1, 2, 1);
        cy.createSkill(1, 1, 3);
        cy.importSkillFromCatalog(1, 1, 2, 2);
        cy.finalizeCatalogImport(1);

        cy.cdVisit('/subjects/subj1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="selfReportQuizTag"]')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportQuizTag"]').should('not.exist')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportSurveyTag"]').should('not.exist')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="selfReportSurveyTag"]')

        cy.get('[data-cy=toggleSkillDetails]').click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="quizAlert"]').contains('Pass the 1-question This is quiz 1 Quiz and earn 150 points')

        cy.get('[data-cy="skillDescription-skill3"] [data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="skillDescription-skill3"] [data-cy="quizAlert"]').should('not.exist')

        cy.get('[data-cy="skillDescription-skill2"] [data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="quizAlert"]').contains('Complete the 1-question This is survey 2 Survey and earn 150 points')

    });

});
