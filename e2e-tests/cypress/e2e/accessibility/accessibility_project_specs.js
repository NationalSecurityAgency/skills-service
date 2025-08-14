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

const moment = require('moment-timezone');

describe('Accessibility Tests', () => {

    before(() => {
        cy.beforeTestSuiteThatReusesData()

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createProject(2)

        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 4)

        cy.createBadge(1)
        cy.assignSkillToBadge(1, 1, 1);
        cy.enableBadge(1, 1);

        cy.addLearningPathItem(1, 1, 2)

        const m = moment('2020-05-12 11', 'YYYY-MM-DD HH');
        cy.reportSkill(1, 1, 'u1',  m);
        cy.reportSkill(1, 1, 'u2',  m.subtract(4, 'day'));
        cy.reportSkill(1, 1, 'u3',  m.subtract(3, 'day'));
        cy.reportSkill(1, 1, 'u4',  m.subtract(2, 'day'));
        cy.reportSkill(1, 1, 'u5',  m.subtract(1, 'day'));
        cy.reportSkill(1, 3, 'u5',  m.subtract(1, 'day'));
        cy.reportSkill(1, 3, 'u6',  m.subtract(1, 'day'));
        cy.reportSkill(1, 3, 'u7',  m.subtract(1, 'day'));
        cy.reportSkill(1, 4, 'u5',  m.subtract(1, 'day'));
        cy.reportSkill(1, 4, 'u8',  m.subtract(2, 'day'));
        cy.reportSkill(1, 4, 'u8',  m.subtract(3, 'day'));
        cy.reportSkill(1, 4, 'u8',  m.subtract(4, 'day'));
        cy.reportSkill(1, 4, 'u8',  m.subtract(5, 'day'));

        //report skills that dont' exist
        cy.reportSkill('proj1', 42, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('proj1', 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('proj1', 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('proj1', 75, 'user@skills.org', '2021-02-24 10:00', false);
        cy.reportSkill('proj1', 13, 'user@skills.org', '2021-02-24 10:00', false);

        cy.reuseSkillIntoAnotherSubject(1, 3, 2);
        cy.reuseSkillIntoAnotherSubject(1, 4, 2);
    });

    after(() => {
        cy.afterTestSuiteThatReusesData()
    });

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {

        it(`project - subject page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/');
            cy.get('[data-cy=projCard_proj1_manageBtn]').click();
            cy.get('[data-cy=manageBtn_subj1]');

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`project and subject reused skills${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator');
            cy.get('[data-cy="projCard_proj1_manageBtn"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()

            cy.visit('/administrator/projects/proj1');
            cy.get('[data-cy="manageBtn_subj1"]')
            cy.get('[data-cy="projectLastReportedSkill"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`Edit Project - clicking on a description label puts focus in the description input${darkMode}`, () => {
            cy.viewport(1200, 1400)
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator');
            cy.get('[data-cy="projCard_proj1_manageBtn"]')
            cy.get('[data-cy="newProjectButton"]').click()
            cy.get('[data-cy="projectName"]').should('have.focus')
            cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents').should('be.visible')
            cy.wait(500)
            cy.get('[data-cy="markdownEditorLabel"]').contains('Description').click()
            cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents').should('have.focus')
        });

        it(`project - new subject modal${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1');

            cy.get('[data-cy="btn_Subjects"]').click();
            cy.get('[data-cy=subjectName]').type('a');
            cy.get('[data-cy="markdownEditorInput"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`project - self-report page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/self-report');
            cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="skillsBTableTotalRows"]').should("have.text", '3')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()


            // looks like AXE and PrimeVue lib disagree where `aria-selected="true"` can be applied
            // TODO: not really an issue but look into this further so this validation can be added back
            // cy.get('[data-pc-name="pcheadercheckbox"]').click();
            // cy.get('[data-cy="rejectBtn"]').click();
            // cy.get('[data-cy="rejectionTitle"]').contains('This will reject user\'s request(s) to get points');
            // cy.wait(500); // wait for modal to continue loading, if background doesn't load the contract checks will fail
            // cy.customA11y();
        });

        it(`project - learning path${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/learning-path');
            cy.contains('Legend');

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`project - levels${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/levels');
            cy.get('[data-cy="levelsTable"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`project - users${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/users');
            cy.get('[data-cy="pageHeader"]').contains('ID: proj1');
            cy.get('[data-cy="usersTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '6');

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`project - users archive${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/users');
            cy.get('[data-cy="pageHeader"]').contains('ID: proj1');
            cy.get('[data-cy="exportUsersTableBtn"]')
            cy.get('[data-cy="usersTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '6');

            cy.get('[data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()
            cy.get('[data-cy="archiveUsersTableBtn"]').should('be.enabled');
            cy.get('[data-cy="archiveUsersTableBtn"]').click()
            cy.get('[data-cy="userArchiveBtn"]').should('be.enabled');
            cy.get('[data-cy="userArchiveBtn"]').click()

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()

            cy.get('[data-cy="restoreUser-u4"]').should('be.enabled');
            cy.get('[data-cy="restoreUser-u4"]').click()

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`project - access page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/access');

            const tableSelector = '[data-cy="roleManagerTable"]';
            cy.get(`${tableSelector} tbody tr`).should('have.length', 1);
            cy.get('[data-cy="trusted-client-props-panel"]').contains('proj1')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`project - settings page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/settings');

            cy.get('[data-cy="helpUrlHostTextInput"]')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`project - issues page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator/projects/proj1/issues');

            cy.get('[data-cy="projectErrorsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '3')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y()
        });

        it(`create restricted community project${darkMode}`, () => {
            cy.intercept('GET', '/public/config', (req) => {
                req.reply((res) => {
                    const conf = res.body;
                    conf.userCommunityDocsLabel = 'User Community Docs';
                    conf.userCommunityDocsLink = 'https://somedocs.com';
                    res.send(conf);
                });
            })

            const allDragonsUser = 'allDragons@email.org'
            cy.fixture('vars.json').then((vars) => {
                cy.logout();
                cy.login(vars.rootUser, vars.defaultPass, true);
                cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
                cy.logout();

                cy.register(allDragonsUser, vars.defaultPass);
                cy.logout();

                cy.login(vars.defaultUser, vars.defaultPass);
            });
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator')
            cy.get('[data-cy="inception-button"]').contains('Level');
            cy.get('[data-cy="newProjectButton"]').click()
            cy.injectAxe();
            cy.get('[data-cy="projectName"]').type('one')
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
            cy.get('[data-cy="userCommunityDocsLink"] a').contains('User Community Docs')

            cy.customLighthouse();
            cy.customA11y()
        });

        it(`blockquote minimum contrast ratio${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.visit('/administrator');
            cy.get('[data-cy="projCard_proj1_manageBtn"]')
            cy.get('[data-cy="newProjectButton"]').click()
            cy.get('[data-cy="projectName"]').should('have.focus')
            cy.injectAxe();
            cy.get('[aria-label="Blockquote"]').click()
            cy.get('[data-cy="markdownEditorInput"]').type('this is a quote')
            cy.customLighthouse();
            cy.customA11y()
        });

    })


});
