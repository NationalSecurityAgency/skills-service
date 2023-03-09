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

    beforeEach(() => {

    });

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

        cy.get('[data-cy="skillDescription-skill2"] [data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="quizAlert"]').should('not.exist')

        cy.get('[data-cy="skillDescription-skill3"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillDescription-skill3"] [data-cy="quizAlert"]').contains('Pass the 2-question Trivia Knowledge 1 Quiz and earn 150 points')

        cy.get('[data-cy="skillDescription-skill4"] [data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="skillDescription-skill4"] [data-cy="quizAlert"]').contains('Pass the Trivia Knowledge 2 Quiz and earn 150 points')

        cy.cdClickSkill(0);
        cy.get('[data-cy="selfReportQuizTag"]')
        cy.get('[data-cy="takeQuizBtn"]').should('be.enabled')
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="quizAlert"]').contains('Pass the 1-question Trivia Knowledge Quiz and earn 150 points')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.get('[data-cy="selfReportQuizTag"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="quizAlert"]').should('not.exist')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="quizAlert"]').contains('Pass the 2-question Trivia Knowledge 1 Quiz and earn 150 points')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(3);
        cy.get('[data-cy="takeQuizBtn"]').contains('Take Quiz')
        cy.get('[data-cy="quizAlert"]').contains('Pass the Trivia Knowledge 2 Quiz and earn 150 points')
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

        cy.get('[data-cy="skillDescription-skill2"] [data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="quizAlert"]').should('not.exist')

        cy.get('[data-cy="skillDescription-skill3"] [data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="skillDescription-skill3"] [data-cy="quizAlert"]').contains('Complete the 2-question Trivia Knowledge 1 Survey and earn 150 points')

        cy.get('[data-cy="skillDescription-skill4"] [data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="skillDescription-skill4"] [data-cy="quizAlert"]').contains('Complete the Trivia Knowledge 2 Survey and earn 150 points')

        cy.cdClickSkill(0);
        cy.get('[data-cy="selfReportSurveyTag"]')
        cy.get('[data-cy="takeQuizBtn"]').should('be.enabled')
        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="quizAlert"]').contains('Complete the 1-question Trivia Knowledge Survey and earn 150 points')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.get('[data-cy="selfReportQuizTag"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="quizAlert"]').should('not.exist')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="quizAlert"]').contains('Complete the 2-question Trivia Knowledge 1 Survey and earn 150 points')

        cy.cdBack('Subject 1');
        cy.cdClickSkill(3);
        cy.get('[data-cy="takeQuizBtn"]').contains('Complete Survey')
        cy.get('[data-cy="quizAlert"]').contains('Complete the Trivia Knowledge 2 Survey and earn 150 points')
    });
});


