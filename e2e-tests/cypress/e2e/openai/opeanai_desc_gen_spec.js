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


describe('Generate Descriptions via OpenAI API Tests', () => {

    const chessGenValue = 'In order to learn chess you will need to get a chess board!'

    beforeEach(() => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
    })


    it('generate a new description for a skill', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains('Describe the skill, and I\'ll help')
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Learn chess')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains('Got it')
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(chessGenValue)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains('I\'ve prepared a description based on your input')
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')

        cy.get('[data-cy="instructionsInput"]').should('have.focus')
        cy.get('[data-cy="useGenValueBtn-2"]').click()
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get('[data-cy="markdownEditorInput"]').contains(chessGenValue)
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="skillOverviewDescription"]').contains(chessGenValue)
    });
});


