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
}
    from './openai_helper_commands'

describe('Generate Desc Fail And Cancel Tests', () => {

    it('handle if generation never starts and eventually times out', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.addPrefixToInvalidParagraphsOptions = '(A) ,(B) ,(C) ,(D) ';
                res.send(conf);
            });
        }).as('getConfig');
        cy.intercept('POST', '/openai/stream/description', (req) => {
            // Delay the response by 30 seconds
            return new Promise((resolve) => {
                setTimeout(() => {
                    // After 30 seconds, respond with a timeout error
                    req.reply({
                        statusCode: 504,
                        body: 'Gateway Timeout',
                        delay: 0
                    });
                    resolve();
                }, 40000); // 40 seconds
            });
        }).as('openaiStream');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        // Set a longer timeout for this test since we're testing a timeout scenario
        Cypress.config('defaultCommandTimeout', 60000); // 40 seconds

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Learn chess')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains('Got it')
        // cy.wait('@openaiStream', { timeout: 40000 })

        const messageToWaitFor = [
            "This is taking longer than expected but I am still working on it...",
            "Still working on generating the best response for you...",
            "Hang tight! Still processing your request...",
            "I am trying but unfortunately it is still taking way longer than expected...",
            "I am still trying, sorry for the delay!",
            "I may not be able to generate a description at this time. But I'll try my best to get it done."
        ]
        messageToWaitFor.forEach((message) => {
            cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(message)
        })
        cy.wait('@openaiStream', { timeout: 40000 })
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains('I apologize, but I was unable to generate a description at this time.')
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get(`[data-cy="addPrefixBtn"]`).should('not.exist')
        cy.get('[data-cy="instructionsInput"]').should('have.focus')
    });

    it('handle immediate failure', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.addPrefixToInvalidParagraphsOptions = '(A) ,(B) ,(C) ,(D) ';
                res.send(conf);
            });
        }).as('getConfig');
        cy.intercept('POST', '/openai/stream/description', (req) => {
            req.reply({
                statusCode: 400,
                body: 'Internal Server Error\n',
                delay: 0
            });
        }).as('openaiStream');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        // Set a longer timeout for this test since we're testing a timeout scenario
        Cypress.config('defaultCommandTimeout', 60000); // 40 seconds

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Learn chess')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains('Got it')
        cy.wait('@openaiStream')
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains('I apologize, but I was unable to generate a description at this time.')
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get(`[data-cy="addPrefixBtn"]`).should('not.exist')
        cy.get('[data-cy="instructionsInput"]').should('have.focus')
    });

    it('ability to stop generation', () => {
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
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').type('paragraphs')
        cy.get('[data-cy="sendAndStopBtn"]').click()
        cy.get('[data-cy="userMsg-1"]').contains('paragraphs')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains('Got it')

        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains('Paragraph 1')
        cy.get('[data-cy="sendAndStopBtn"]').click()
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains('Generation stopped.')

        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get(`[data-cy="addPrefixBtn"]`).should('not.exist')
        cy.get('[data-cy="instructionsInput"]').should('have.focus')
    });

});


