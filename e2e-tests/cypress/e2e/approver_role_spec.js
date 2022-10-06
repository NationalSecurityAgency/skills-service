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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Approver Role Tests', () => {
    const userName = 'approver@skills.org'
    const password = 'password'

    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();
        cy.resetEmail();

        cy.logout();
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

        const createProj = (projNum) => {
            cy.createProject(projNum)
            cy.createSubject(projNum, 1)
            cy.createSubject(projNum, 2)
            cy.createSkill(projNum, 1, 1)
            cy.createSkill(projNum, 1, 2)
            cy.createSkill(projNum, 1, 3)
            cy.createSkill(projNum, 2, 4)
            cy.createSkill(projNum, 2, 5)
            cy.createBadge(projNum, 1)
            cy.createBadge(projNum, 2)
            cy.assignSkillToBadge(projNum, 1, 1);
            cy.assignSkillToBadge(projNum, 1, 2);
            cy.assignSkillToBadge(projNum, 2, 1);
            cy.createBadge(projNum, 1, { enabled: true })
            cy.assignDep(projNum, 3, 1);
            cy.assignDep(projNum, 3, 2);

            cy.reportSkill(projNum, 1, 'user0', 'yesterday');
            cy.reportSkill(projNum, 1, 'user0', 'now');
            cy.reportSkill(projNum, 1, 'user1', 'now');
            cy.reportSkill(projNum, 1, 'user2', 'now');
        };

        createProj(1)

        cy.register(userName, password);
        cy.logout();
        cy.login(userName, password);

        cy.logout();

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
        cy.request('POST', `/admin/projects/proj1/users/${userName}/roles/ROLE_PROJECT_APPROVER`);

        cy.logout();
        cy.login(userName, password);

        createProj(2)
    });

    after(() => {
        Cypress.env('disableResetDb', false);
    });

    beforeEach(() => {
        cy.logout();
        cy.login(userName, password);

        // it's important to simulate slow settings to catch race conditions of components not waiting for settings to appear
        cy.intercept({ url: '/admin/projects/proj1/settings', middleware: true }, (req) => {
            req.on('response', (res) => {
                // Wait 2 seconds before sending the response to the client.
                res.setDelay(2000)
            })
        }).as('getSettingsProj1')
        cy.intercept({ url: '/admin/projects/proj2/settings', middleware: true }, (req) => {
            req.on('response', (res) => {
                // Wait 2 seconds before sending the response to the client.
                res.setDelay(2000)
            })
        }).as('getSettingsProj2')
    });

    it('projects page - approver role has no mutation controls', function () {
        cy.visit(`/administrator`);
        const runCheck = (projNum, assertChainPrepend = null) => {
            const chainerPrepend = assertChainPrepend ? assertChainPrepend : '';

            cy.get(`[data-cy="projectCard_proj${projNum}"] [data-cy="editProjBtn"]`).should(`${chainerPrepend}exist`)
            cy.get(`[data-cy="projectCard_proj${projNum}"] [data-cy="copyProjBtn"]`).should(`${chainerPrepend}exist`)
            cy.get(`[data-cy="projectCard_proj${projNum}"] [data-cy="deleteProjBtn"]`).should(`${chainerPrepend}exist`)
            cy.get(`[data-cy="projectCard_proj${projNum}"] [data-cy="noIssues"]`).should(`${chainerPrepend}exist`)
        }
        runCheck(2)
        runCheck(1,  'not.')

        cy.get('[data-cy="projCard_proj2_manageBtn"]').contains('Manage')
        cy.get('[data-cy="projCard_proj1_manageBtn"]').contains('View')

        cy.get('[data-cy="projectCard_proj2"] [data-cy="userRole"]').should('have.text', 'Admin')
        cy.get('[data-cy="projectCard_proj1"] [data-cy="userRole"]').should('have.text', 'Approver')
    });

    it('project page - approver role has no mutation controls', function () {
        const runCheck = (projNum, manageButtonTxt = 'Manage', assertChainPrepend = null) => {
            const chainerPrepend = assertChainPrepend ? assertChainPrepend : '';
            cy.visit(`/administrator/projects/proj${projNum}`);
            cy.wait(`@getSettingsProj${projNum}`);

            cy.get('[data-cy="btn_edit-project"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="projectPreview"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="pageHeaderStat_Issues"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="btn_Subjects"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="nav-Skill Catalog"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="nav-Levels"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="nav-Contact Users"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="nav-Issues"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="nav-Access"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="nav-Settings"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="subjectCard-subj2"] [data-cy="sortControlHandle"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="subjectCard-subj2"] [data-cy="editBtn"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="subjectCard-subj1"] [data-cy="deleteBtn"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="subjectCard-subj2"] [data-cy="deleteBtn"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="manageBtn_subj1"]').contains(manageButtonTxt)
            cy.get('[data-cy="manageBtn_subj2"]').contains(manageButtonTxt)
        }
        runCheck(2)
        runCheck(1, 'View', 'not.')
    });

    it('/subj page - approver role has no mutation controls', function () {
        const runCheck = (projNum, manageButtonTxt = 'Manage', assertChainPrepend = null) => {
            const chainerPrepend = assertChainPrepend ? assertChainPrepend : '';
            cy.visit(`/administrator/projects/proj${projNum}/subjects/subj1`);
            cy.wait(`@getSettingsProj${projNum}`);
            cy.get('[data-cy="btn_edit-subject"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="nav-Levels"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="importFromCatalogBtn"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="newGroupButton"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="newSkillButton"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="skillActionsBtn"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="clearSelectedSkillsBtn"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="selectAllSkillsBtn"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="editSkillButton_skill1"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="editSkillButton_skill2"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="editSkillButton_skill3"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="copySkillButton_skill1"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="copySkillButton_skill2"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="copySkillButton_skill3"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="deleteSkillButton_skill3"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="deleteSkillButton_skill3"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="deleteSkillButton_skill3"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="manageSkillBtn_skill3"]').contains(manageButtonTxt)
            cy.get('[data-cy="manageSkillBtn_skill3"]').contains(manageButtonTxt)
            cy.get('[data-cy="manageSkillBtn_skill3"]').contains(manageButtonTxt)

            cy.get('[data-cy="skillSelect-skill1"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="skillSelect-skill2"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="skillSelect-skill3"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="orderMoveDown_skill1"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="orderMoveDown_skill2"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="orderMoveDown_skill3"]').should(`${chainerPrepend}exist`)

            cy.get('[data-cy="orderMoveUp_skill1"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="orderMoveUp_skill2"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="orderMoveUp_skill3"]').should(`${chainerPrepend}exist`)
        }
        runCheck(2)
        runCheck(1, 'View','not.')
    });

    it('/subj/users page - approver role has no mutation controls', function () {
        const runCheck = (projNum, chainerPrepend = '') => {
            cy.visit(`/administrator/projects/proj${projNum}/subjects/subj1/users`);
            cy.wait(`@getSettingsProj${projNum}`);
            cy.get('[data-cy="btn_edit-subject"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="nav-Levels"]').should(`${chainerPrepend}exist`)
        }
        runCheck(2)
        runCheck(1, 'not.')
    });

    it('/subj/metrics page - approver role has no mutation controls', function () {
        const runCheck = (projNum, chainerPrepend = '') => {
            cy.visit(`/administrator/projects/proj${projNum}/subjects/subj1/metrics`);
            cy.wait(`@getSettingsProj${projNum}`);
            cy.get('[data-cy="btn_edit-subject"]').should(`${chainerPrepend}exist`)
            cy.get('[data-cy="nav-Levels"]').should(`${chainerPrepend}exist`)
        }
        runCheck(2)
        runCheck(1, 'not.')
    });

});