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

describe('Generate Desc Tests', () => {

    const chessGenValue = 'In order to learn chess you will need to get a chess board!'

    it('generate a new description for a skill', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
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

    it('apply prefix after generating a new description for a skill', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.addPrefixToInvalidParagraphsOptions = '(A) ,(B) ,(C) ,(D) ';
                res.send(conf);
            });
        }).as('getConfig');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains('Describe the skill, and I\'ll help')
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').type('jabberwocky{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('jabberwocky')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains('Got it')
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains('I\'ve prepared a description based on your input')
        cy.validateMarkdownViewerText('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]', [
            'Has jabberwocky',
            'clean line',
            'jabberwocky again',
            'and nothing here'
        ])

        cy.get('[data-cy="aiMsg-2"] [data-cy="useGenValueBtn-2"]').should('be.enabled')
        cy.get(`[data-cy="aiMsg-2"] [data-cy="addPrefixBtn"]`).should('be.enabled')
        cy.get('[data-cy="instructionsInput"]').should('have.focus')

        cy.get(`[data-cy="aiMsg-2"] [data-cy="prefixSelect"]`).click()
        const options = ['A', 'B', 'C', 'D']
        options.forEach((val) => {
            cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${val}) "]`)
        })

        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${options[2]}) "]`).click()
        cy.get(`[data-cy="aiMsg-2"] [data-cy="addPrefixBtn"]`).click()

        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get(`[data-cy="aiMsg-2"] [data-cy="addPrefixBtn"]`).should('not.exist')

        cy.validateMarkdownEditorText('[data-cy="markdownEditorInput"]', [
            '(C) Has jabberwocky',
            'clean line',
            '(C) jabberwocky again',
            'and nothing here'
        ])
    });

    it('ability to customize prefix button', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.addPrefixToInvalidParagraphsOptions = '(A) ,(B) ,(C) ,(D) ';
                conf.addPrefixToGeneratedValueBtnLabel = 'Prefix Custom Label'
                res.send(conf);
            });
        }).as('getConfig');
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
        cy.get(`[data-cy="aiMsg-2"] [data-cy="addPrefixBtn"]`).should('be.enabled')
        cy.get('[data-cy="instructionsInput"]').should('have.focus')
        cy.get(`[data-cy="aiMsg-2"] [data-cy="addPrefixBtn"]`).contains('Prefix Custom Label')
    });

    it('handle if stream pauses periodically', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.addPrefixToInvalidParagraphsOptions = '(A) ,(B) ,(C) ,(D) ';
                res.send(conf);
            });
        }).as('getConfig');


        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        // Set a longer timeout for this test since we're testing a timeout scenario
        Cypress.config('defaultCommandTimeout', 60000); // 40 seconds

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains('Describe the skill, and I\'ll help')
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').type('paragraphs{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('paragraphs')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains('Got it')

        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains('Paragraph 1')
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains('Paragraph 2')
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains('Paragraph 3')
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains('I\'ve prepared a description based on your input')

        const expectedLines = [
            'Paragraph 1',
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            'Paragraph 2',
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            'Paragraph 3',
            'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        ]
        cy.validateMarkdownViewerText('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]', expectedLines)
    });

});


