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
    from '../openai/openai_helper_commands'

describe('Accessibility AI Modal Tests', () => {

    const runWithDarkMode = ['', ' - dark mode']
    runWithDarkMode.forEach((darkMode) => {
        it(`generate a new description for a skill${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
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

            cy.get('[data-cy="aiModelsSelector"]  [data-cy="modelSettingsButton"]').click()
            cy.get('[data-cy="aiModelsSelector"] [data-cy="modelSettings"]').should('be.visible')

            cy.get('[data-cy="instructionsInput"]').type('Learn chess{enter}')
            cy.get('[data-cy="userMsg-1"]').contains('Learn chess')
            cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
            cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(chessGenValue)
            cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
            cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')

            cy.get('[data-cy="instructionsInput"]').should('have.focus')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });
    })

});


