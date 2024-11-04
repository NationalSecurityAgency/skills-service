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

describe('Accessibility Admin Group Tests', () => {

    beforeEach(() => {
    });
    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {
        it(`empty admin group definitions page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/adminGroups/')
            cy.get('[data-cy="noAdminGroupsYet"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`new admin group modal${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/adminGroups/')
            cy.get('[data-cy="btn_Admin Groups"]').click()
            cy.get('[data-cy="adminGroupName"]').type('hello')
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`admin group definitions page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });
            cy.createAdminGroupDef(2);
            cy.createAdminGroupDef(3);
            cy.createAdminGroupDef(4);
            cy.createAdminGroupDef(5);
            cy.createAdminGroupDef(6);

            cy.visit('/administrator/adminGroups/')
            cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '6')
            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`admin group members page${darkMode}`, () => {
            const anotherUser = 'user1@skills.org';
            cy.register(anotherUser, 'password');
            cy.fixture('vars.json')
                .then((vars) => {
                    if (!Cypress.env('oauthMode')) {
                        cy.log('NOT in oauthMode, using form login');
                        cy.login(vars.defaultUser, vars.defaultPass);
                    } else {
                        cy.log('oauthMode, using loginBySingleSignOn');
                        cy.loginBySingleSignOn();
                    }
                });

            cy.setDarkModeIfNeeded(darkMode)
            cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

            cy.request('PUT', `/admin/admin-group-definitions/adminGroup1/users/${anotherUser}/roles/ROLE_ADMIN_GROUP_MEMBER`);

            cy.visit('/administrator/adminGroups/adminGroup1')
            cy.get('[data-cy="adminGroupMemberRoleManager"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`admin group empty projects page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

            cy.visit('/administrator/adminGroups/adminGroup1/group-projects')
            cy.get('[data-cy="noContent"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`admin group with projects page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });
            cy.createProject(1)
            cy.addProjectToAdminGroupDef(1, 1)

            cy.visit('/administrator/adminGroups/adminGroup1/group-projects')
            cy.get('[data-cy="adminGroupProjectsTable"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`admin group empty quizzes page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });

            cy.visit('/administrator/adminGroups/adminGroup1/group-quizzes')
            cy.get('[data-cy="noContent"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`admin group with quizzes page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createAdminGroupDef(1, { name: 'My Awesome Admin Group' });
            cy.createQuizDef(1)
            cy.addQuizToAdminGroupDef(1, 1)

            cy.visit('/administrator/adminGroups/adminGroup1/group-quizzes')
            cy.get('[data-cy="adminGroupQuizzesTable"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });


    })
});
