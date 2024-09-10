/*
 * Copyright 2024 SkillTree
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

describe('Accessibility for Root Role Page Tests', () => {

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {
        it(`user activity history page${darkMode}`, () => {
            cy.createProject(1)
            cy.createProject(2)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' })
            cy.createSkill(1, 1, 2)
            cy.createSkill(1, 1, 2)
            cy.createSkill(1, 1, 3)
            cy.createSkill(1, 1, 3)
            cy.createSkill(1, 1, 4)
            cy.createSkill(1, 1, 4)
            cy.createSkill(1, 1, 5)
            cy.createSkill(1, 1, 5)
            cy.fixture('vars.json').then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
            });

            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/userActions')
            cy.get('[data-cy="dashboardActionsForEverything"]').contains('Edit')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });
    })

});
