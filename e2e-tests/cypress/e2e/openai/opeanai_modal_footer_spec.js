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

describe('AI Modal Footer Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
    })

    it('footer is configured with messaged and powered by', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.openaiFooterMsg = 'Review AI-generated content for accuracy.'
                conf.openaiFooterPoweredByLinkText = 'COOLAI'
                conf.openaiFooterPoweredByLink = 'https://coolai.com'
                res.send(conf);
            });
        }).as('getConfig');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="aiPromptDialogFooter"]')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterMsg"]').contains('Review AI-generated content for accuracy.')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterPoweredBy"]').contains('Powered By COOLAI')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterPoweredBy"] a').should('have.attr', 'href', 'https://coolai.com')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterPoweredBy"] a').should('have.attr', 'target', '_blank')
    });

    it('no footer', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="aiPromptDialogFooter"]').should('not.exist')
    });

    it('footer just with message', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.openaiFooterMsg = 'Review AI-generated content for accuracy.'
                res.send(conf);
            });
        }).as('getConfig');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="aiPromptDialogFooter"]')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterMsg"]').contains('Review AI-generated content for accuracy.')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterPoweredBy"]').should('not.exist')
    });

    it('footer is configured with only powered by', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.openaiFooterPoweredByLinkText = 'COOLAI'
                conf.openaiFooterPoweredByLink = 'https://coolai.com'
                res.send(conf);
            });
        }).as('getConfig');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="aiPromptDialogFooter"]')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterMsg"]').should('not.exist')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterPoweredBy"]').contains('Powered By COOLAI')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterPoweredBy"] a').should('have.attr', 'href', 'https://coolai.com')
        cy.get('[data-cy="aiPromptDialogFooter"] [data-cy="aiPromptDialogFooterPoweredBy"] a').should('have.attr', 'target', '_blank')
    });
});



