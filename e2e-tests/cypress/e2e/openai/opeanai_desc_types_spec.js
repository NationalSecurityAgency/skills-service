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
    gotStartedMsg
}
    from './openai_helper_commands'

describe('Generate Description For Various Items Tests', () => {

    beforeEach(() => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
    })

    it('new skill description', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('@getConfig')

        cy.get('[data-cy="newSkillButton"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(`${newDescWelcomeMsg} Skill`)

        let requestBody
        cy.intercept('POST', '/openai/chat', (req) => {
            requestBody = req.body;
            req.reply();
        }).as('genDescription1');
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@genDescription1').then(() => {
            expect(requestBody.messages).to.have.length(1);
            expect(requestBody.messages[0].role).to.equal('User')
            expect(requestBody.messages[0].content).to.contain('Generate a detailed description for a skill based on this information:');
            expect(requestBody.messages[0].content).to.contain('Learn chess');
        });
    });

    it('new skill group description', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('@getConfig')

        cy.get('[data-cy="newGroupButton"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(`${newDescWelcomeMsg} Skill Group`)

        let requestBody
        cy.intercept('POST', '/openai/chat', (req) => {
            requestBody = req.body;
            req.reply();
        }).as('genDescription1');
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@genDescription1').then(() => {
            expect(requestBody.messages).to.have.length(1);
            expect(requestBody.messages[0].role).to.equal('User')
            expect(requestBody.messages[0].content).to.contain('Generate a detailed description for a Skill Group based on this information:');
            expect(requestBody.messages[0].content).to.contain('Learn chess');
        });
    });

    it('new subject description', () => {
        cy.createProject(1);

        cy.visit('/administrator/projects/proj1')
        cy.get('@getConfig')

        cy.get('[data-cy="btn_Subjects"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(`${newDescWelcomeMsg} Subject`)
        let requestBody
        cy.intercept('POST', '/openai/chat', (req) => {
            requestBody = req.body;
            req.reply();
        }).as('genDescription1');
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@genDescription1').then(() => {
            expect(requestBody.messages).to.have.length(1);
            expect(requestBody.messages[0].role).to.equal('User')
            expect(requestBody.messages[0].content).to.contain('Generate a detailed description for a training subject based on this information:');
            expect(requestBody.messages[0].content).to.contain('Learn chess');
        });
    });

    it('new project description', () => {
        cy.visit('/administrator')
        cy.get('@getConfig')

        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(`${newDescWelcomeMsg} Project`)

        let requestBody;
        cy.intercept('POST', '/openai/chat', (req) => {
            requestBody = req.body;
            req.reply();
        }).as('genDescription1');

        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.wait('@genDescription1').then(() => {
            expect(requestBody.messages).to.have.length(1);
            expect(requestBody.messages[0].role).to.equal('User')
            expect(requestBody.messages[0].content).to.contain('Generate a detailed description for a training program based on this information:');
            expect(requestBody.messages[0].content).to.contain('Learn chess');
        });
    });

    it('new badge description', () => {
        cy.createProject(1);

        cy.visit('/administrator/projects/proj1/badges')
        cy.get('@getConfig')

        cy.get('[data-cy="btn_Badges"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(`${newDescWelcomeMsg} Badge`)
        let requestBody
        cy.intercept('POST', '/openai/chat', (req) => {
            requestBody = req.body;
            req.reply();
        }).as('genDescription1');
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@genDescription1').then(() => {
            expect(requestBody.messages).to.have.length(1);
            expect(requestBody.messages[0].role).to.equal('User')
            expect(requestBody.messages[0].content).to.contain('Generate a detailed description for a badge that will be part of a training program based on this information:');
            expect(requestBody.messages[0].content).to.contain('Learn chess');
        });
    });

    it('new quiz description', () => {
        cy.visit('/administrator/quizzes')
        cy.get('@getConfig')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(`${newDescWelcomeMsg} Quiz`)
        let requestBody
        cy.intercept('POST', '/openai/chat', (req) => {
            requestBody = req.body;
            req.reply();
        }).as('genDescription1');
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@genDescription1').then(() => {
            expect(requestBody.messages).to.have.length(1);
            expect(requestBody.messages[0].role).to.equal('User')
            expect(requestBody.messages[0].content).to.contain('Generate a detailed description for a Quiz based on this information:');
            expect(requestBody.messages[0].content).to.contain('Learn chess');
        });
    });

    it('new survey description', () => {
        cy.visit('/administrator/quizzes')
        cy.get('@getConfig')

        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-cy="quizTypeSelector"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="optionlabel"]').contains('Survey').click()
        cy.get('[data-cy="quizTypeSelector"]').contains('Survey')
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(`${newDescWelcomeMsg} Survey`)
        let requestBody
        cy.intercept('POST', '/openai/chat', (req) => {
            requestBody = req.body;
            req.reply();
        }).as('genDescription1');
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@genDescription1').then(() => {
            expect(requestBody.messages).to.have.length(1);
            expect(requestBody.messages[0].role).to.equal('User')
            expect(requestBody.messages[0].content).to.contain('Generate a detailed description for a Survey based on this information:');
            expect(requestBody.messages[0].content).to.contain('Learn chess');
        });
    });

});


