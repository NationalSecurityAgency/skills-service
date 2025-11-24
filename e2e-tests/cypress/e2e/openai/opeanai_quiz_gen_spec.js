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

import {
    newDescWelcomeMsg,
    chessGenValue,
    existingDescWelcomeMsg,
    completedMsg,
    gotStartedMsg,
    newQuizGeneratingMsg
}
    from './openai_helper_commands'

describe('Generate Quiz Tests', () => {

    it('generate a new quiz for a skill', () => {
        cy.on('uncaught:exception', (err, runnable) => {
            return false
        })
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { description: chessGenValue, numPerformToCompletion: 1 })

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="skillOverviewDescription"]').contains(chessGenValue)

        cy.get('[data-cy="generateQuizBtn"]').click()

        cy.get('[data-cy="instructionsInput"]').should('have.focus')
        cy.get('[data-cy="useGenValueBtn-1"]').click()
        cy.get('[data-cy="useGenValueBtn-1"]').should('not.exist')

        cy.get('[data-cy="selfReportMediaCard"]').contains('Self Report: Quiz')
        cy.get('[data-cy="selfReportMediaCard"]').contains('Users can self report this skill and points will be awarded after the Very Great Skill 1 Quiz Quiz is passed!')
        cy.get('[data-cy="linkToQuiz"]')
          .should('have.attr', 'href')
          .and('include', '/administrator/quizzes/skill1Quiz');
        cy.get('[data-cy="buttonToQuiz"]')
          .should('have.attr', 'href')
          .and('include', '/administrator/quizzes/skill1Quiz');
        cy.get('[data-cy="buttonToQuiz"]').contains('View Quiz')
    });

});


