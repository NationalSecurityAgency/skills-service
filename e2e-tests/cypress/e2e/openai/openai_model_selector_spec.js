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
    gotStartedMsg
}
    from './openai_helper_commands'

describe('Model Selector Tests', () => {

    it('select a model and change a temperature', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.intercept('/openai/models').as('getModels');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')
        cy.get('@getModels')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').contains('model1')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('not.exist')
        cy.get('[data-cy="aiModelsSelector"]  [data-cy="modelSettingsButton"]').click()

        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('be.visible')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="model2"]').click()
        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').contains('model2')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelTempSlider"] [data-pc-section="handle"]').type('{rightArrow}')

        cy.get('[data-cy="aiModelsSelector"]  [data-cy="modelSettingsButton"]').click()
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('not.exist')

        cy.intercept('POST', '/openai/chat', (req) => {
            expect(req.body.model).to.equal('model2');
            expect(req.body.modelTemperature).to.equal(0.51);
            req.continue();
        }).as('chatCompletion')
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@chatCompletion')

        // remember selected model and temperature
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')


        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('@getModels')
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').contains('model2')

        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@chatCompletion')
    });

    it('model stored in local storage is not available anymore', () => {
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

        window.localStorage.setItem('selectedAiModel', 'model2');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').contains('model2')

        cy.intercept('GET', '/openai/models', (req) => {
            req.reply((res) => {
                const modelsBody = res.body;
                modelsBody.models = modelsBody.models.map((model, index) => ({...model, model: `model${index+11}`}))
                res.send(modelsBody);
            });
        }).as('getModels');
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')


        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('@getModels')
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').contains('model11')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('not.exist')

        cy.intercept('POST', '/openai/chat', (req) => {
            expect(req.body.model).to.equal('model11');
            expect(req.body.modelTemperature).to.equal(0.50);
            req.continue();
        }).as('chatCompletion')
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@chatCompletion')
    });

    it('models fail to load', () => {
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

        cy.intercept('GET', '/openai/models', (req) => {
            req.reply({
                statusCode: 400,
                body: 'Internal Server Error\n',
                delay: 0
            });
        }).as('getModels');
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()

        cy.get('[data-cy="aiModelsSelector"] [data-cy="failedToLoad"]').should('be.visible')
        cy.get('[data-cy="aiMsg-0"]').should('not.exist')
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').should('not.exist')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('not.exist')
    });

    it('ability to default model and temperature if user has not selected any', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.openaiModelDefaultTemperature = 0.23;
                conf.openaiDefaultModel = 'model2';
                res.send(conf);
            });
        }).as('getConfig');
        cy.intercept('/openai/models').as('getModels');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')
        cy.get('@getModels')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').contains('model2')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('not.exist')

        cy.intercept('POST', '/openai/chat', (req) => {
            expect(req.body.model).to.equal('model2');
            expect(req.body.modelTemperature).to.equal(0.23);
            req.continue();
        }).as('chatCompletion')
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@chatCompletion')

        cy.get('[data-cy="aiModelsSelector"]  [data-cy="modelSettingsButton"]').click()

        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('be.visible')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="model1"]').click()
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelTempSlider"] [data-pc-section="handle"]').type('{leftArrow}')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').contains('model1')

        // selected overrides the default
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')


        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('@getModels')
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').contains('model1')

        cy.intercept('POST', '/openai/chat', (req) => {
            expect(req.body.model).to.equal('model1');
            expect(req.body.modelTemperature).to.equal(0.22);
            req.continue();
        }).as('chatCompletion1')
        cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
        cy.get('@chatCompletion1')
    });

    it('configuration that can filter certain models', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.openaiNotSupportedChatModels = 'model2, mOdEl4 '
                res.send(conf);
            });
        }).as('getConfig');
        cy.intercept('/openai/models', {
            "models": [
                { "model": "model1", "created": "1970-01-21T09:55:32.448+00:00" },
                { "model": "MoDeL2", "created": "1970-01-21T09:55:32.448+00:00" },
                { "model": "model3", "created": "1970-01-21T09:55:32.448+00:00" },
                { "model": "model4", "created": "1970-01-21T09:55:32.448+00:00" },
                { "model": "model5", "created": "1970-01-21T09:55:32.448+00:00" },
            ]
        }).as('getModels');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('@getModels')
        cy.get('[data-cy="aiMsg-0"]').contains(newDescWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="aiModelsSelector"] [data-cy="selectedModel"]').contains('model1')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('not.exist')
        cy.get('[data-cy="aiModelsSelector"]  [data-cy="modelSettingsButton"]').click()

        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('be.visible')
        cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="model1"]')
        cy.get('[data-pc-section="overlay"] [aria-label="model2"]').should('not.exist')
        cy.get('[data-pc-section="overlay"] [aria-label="model3"]')
        cy.get('[data-pc-section="overlay"] [aria-label="model4"]').should('not.exist')
        cy.get('[data-pc-section="overlay"] [aria-label="model5"]')
        cy.get('[data-pc-section="overlay"] [aria-label="model6"]').should('not.exist')
    });

});


