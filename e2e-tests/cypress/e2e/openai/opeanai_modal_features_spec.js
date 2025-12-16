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
    errMsg,
    stopMsg,
    gotStartedMsg,
    catchAllResponse,
    completedMsg
}
    from './openai_helper_commands'

describe('AI Features Tests', () => {

    it('handle if generation never starts and eventually times out', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.addPrefixToInvalidParagraphsOptions = '(A) ,(B) ,(C) ,(D) ';
                conf.openaiTakingLongerThanExpectedTimeoutPerMsg = 5000
                res.send(conf);
            });
        }).as('getConfig');
        cy.intercept('POST', '/openai/chat', (req) => {
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
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        // cy.wait('@openaiStream', { timeout: 40000 })

        const messageToWaitFor = [
            "Just a moment while I get everything ready.",
            "Hang tight! Still processing your request.",
            "Still working on generating the best response for you.",
            "I am still trying, sorry for the delay!",
            "This is taking longer than expected but I am still working on it.",
            "I am trying but unfortunately it is still taking way longer than expected.",
        ]
        messageToWaitFor.forEach((message) => {
            cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(message)
        })
        cy.wait('@openaiStream', { timeout: 40000 })
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(errMsg)
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
        cy.intercept('POST', '/openai/chat', (req) => {
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
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.wait('@openaiStream')
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(errMsg)
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
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)

        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains('Paragraph 1')
        cy.get('[data-cy="sendAndStopBtn"]').click()
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(stopMsg)

        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')
        cy.get(`[data-cy="addPrefixBtn"]`).should('not.exist')
        cy.get('[data-cy="instructionsInput"]').should('have.focus')
    });

    it('send button is enabled only after 1 character is entered', () => {
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
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="sendAndStopBtn"]').should('be.disabled')
        cy.get('[data-cy="instructionsInput"]').type('a')
        cy.get('[data-cy="sendAndStopBtn"]').should('be.enabled')
        cy.get('[data-cy="instructionsInput"]').type('{backspace}')
        cy.get('[data-cy="sendAndStopBtn"]').should('be.disabled')
    });

    it('pressing enter in the empty instructions input field does nothing', () => {
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
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="sendAndStopBtn"]').should('be.disabled')

        cy.get('[data-cy="instructionsInput"]').type('{enter}')
        cy.wait(3000)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="aiMsg-2"]').should('not.exist')
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')

        cy.get('[data-cy="instructionsInput"]').type('a{enter}')

        cy.get('[data-cy="userMsg-1"]').contains('a')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(catchAllResponse)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')
    });

});



