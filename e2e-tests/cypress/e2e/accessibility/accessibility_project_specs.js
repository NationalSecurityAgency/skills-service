/*
 * Copyright 2020 SkillTree
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


    it('project - subject page', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=projCard_proj1_manageBtn]').click();
        cy.get('[data-cy=manageBtn_subj1]');

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project and subject reused skills', () => {
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

    it('Edit Project - clicking on a description label puts focus in the description input', () => {
        cy.visit('/administrator');
        cy.get('[data-cy="projCard_proj1_manageBtn"]')
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents')
        cy.get('[data-cy="markdownEditorLabel"]').contains('Description').click()
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents').should('have.focus')
    });

    it('project - new subject modal', () => {
        cy.visit('/administrator/projects/proj1');

        cy.get('[data-cy="btn_Subjects"]').click();
        cy.get('[data-cy=subjectName]').type('a');
        cy.get('[data-cy="markdownEditorInput"]')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - badges page', () => {
        cy.visit('/administrator/projects/proj1/badges');
        cy.get('[data-cy="manageBtn_badge1"]')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - new badge modal', () => {
        cy.visit('/administrator/projects/proj1/badges');
        cy.get('[data-cy="btn_Badges"]').click()
        cy.get('[data-cy=name').type('a');
        cy.get('[data-cy="markdownEditorInput"]')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - self-report page', () => {
        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="skillsBTableTotalRows"]').should("have.text", '3')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()


        // looks like AXE and PrimeVue lib disagree where `aria-selected="true"` can be applied
        // TODO: not really an issue but look into this further so this validation can be added back
        // cy.get('[data-pc-name="headercheckbox"]').click();
        // cy.get('[data-cy="rejectBtn"]').click();
        // cy.get('[data-cy="rejectionTitle"]').contains('This will reject user\'s request(s) to get points');
        // cy.wait(500); // wait for modal to continue loading, if background doesn't load the contract checks will fail
        // cy.customA11y();
    });

    it('project - Deps page', () => {
        cy.visit('/administrator/projects/proj1/learning-path');
        cy.contains('Legend');

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - levels', () => {
        cy.visit('/administrator/projects/proj1/levels');
        cy.get('[data-cy="levelsTable"]')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - users', () => {
        cy.visit('/administrator/projects/proj1/users');
        cy.get('[data-cy="pageHeader"]').contains('ID: proj1');
        cy.get('[data-cy="usersTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '6');

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - metrics', () => {
        cy.visit('/administrator/projects/proj1/metrics');
        cy.get('[data-cy="card-header"]').contains('Users per day');
        cy.get('[data-cy="distinctNumUsersOverTime"]').contains('This chart needs at least 2 days of user activity.');
        cy.get('[data-cy="projectLastReportedSkillValue"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '4')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - achievements metrics', () => {
        cy.visit('/administrator/projects/proj1/metrics/achievements');
        cy.contains('Level 2: 1 users');
        cy.contains('Level 1: 5 users');

        const headerSelector = `[data-cy="achievementsNavigator"] thead tr th`;
        cy.get(headerSelector)
            .contains('Username')
            .click();
        cy.get('[data-cy=achievementsNavigator-table]')
            .contains('u1');
        cy.wait(2000); // wait for charts to finish loading
        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    })

    it('project - subject metrics', () => {
        cy.visit('/administrator/projects/proj1/metrics/subjects');

        cy.get('[data-cy="Subjects-metrics-link"]')
            .click();
        cy.contains('Number of users for each level over time');
        cy.wait(4000); // wait for charts to finish loading

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()

    });

    it('project - skills metrics', () => {
        cy.visit('/administrator/projects/proj1/metrics/skills');
        cy.get('[data-cy="skillsNavigator-table"] [data-cy="skillsBTableTotalRows"]').should('have.text', '4')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - access page', () => {
        cy.visit('/administrator/projects/proj1/access');

        const tableSelector = '[data-cy="roleManagerTable"]';
        cy.get(`${tableSelector} tbody tr`).should('have.length', 1);
        cy.get('[data-cy="trusted-client-props-panel"]').contains('proj1')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - settings page', () => {
        cy.visit('/administrator/projects/proj1/settings');

        cy.get('[data-cy="helpUrlHostTextInput"]')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });

    it('project - issues page', () => {
        cy.visit('/administrator/projects/proj1/issues');

        cy.get('[data-cy="projectErrorsTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '3')

        cy.customLighthouse();
        cy.injectAxe();
        cy.customA11y()
    });


    it('create restricted community project', () => {
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

        cy.visit('/administrator')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.injectAxe();
        cy.get('[data-cy="projectName"]').type('one')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="userCommunityDocsLink"] a').contains( 'User Community Docs')

        cy.customLighthouse();
        cy.customA11y()
    });

});
