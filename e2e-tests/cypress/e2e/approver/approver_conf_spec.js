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

describe('Approver Config Tests', () => {

    beforeEach(() => {
        cy.createProject(1)
        cy.enableProdMode(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' })
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' })

        cy.reportSkill(1, 1, 'userA', 'yesterday');
        cy.reportSkill(1, 1, 'userA', 'now');
        cy.reportSkill(1, 1, 'userB', 'now');
        cy.reportSkill(1, 1, 'userC', 'now');

        const pass = 'password';
        cy.register('user1', pass);
        cy.register('user2', pass);
        cy.register('user3', pass);
        cy.register('user4', pass);
        cy.register('user5', pass);

        cy.register('testuser', pass);
        cy.register('abcd', pass);
        cy.register('ghij', pass);
        cy.register('zed', pass);

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
    });


    it('must have at least 1 admin to configure the project', function () {
        cy.visit('/administrator/projects/proj1/self-report/configure');

        cy.get('[data-cy="approvalConfNotAvailable"]')
        cy.get('[data-cy="approvalConfNotAvailable"] [data-cy="navToAccessPage"]').click();
        cy.get('[data-cy="projectAdmins"]')
    });

    it('assign approver to handle all of the approval requests', function () {
        cy.viewport(1200, 1200);
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.visit('/administrator/projects/proj1/self-report/configure');

            const user1 = 'user1'
            cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).should('be.enabled')

            const defaultUser = Cypress.env('oauthMode') ? 'foo-hydra': vars.defaultUser
            cy.get(`[data-cy="workloadCell_${defaultUser}"]`).contains('Default Fallback - All Unmatched Requests')
            cy.get(`[data-cy="workloadCell_${defaultUser}"] [data-cy="editApprovalBtn"]`).should('be.enabled')

            // switch to fallback
            cy.get('[data-cy="workloadCell_user1"] [data-cy="fallbackSwitch"]').click()

            // validate
            cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Assigned Fallback - All Unmatched Requests')
            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).should('be.disabled')

            cy.get(`[data-cy="workloadCell_${defaultUser}"]`).contains('Not Handling Approval Workload')
            cy.get(`[data-cy="workloadCell_${defaultUser}"] [data-cy="editApprovalBtn"]`).should('be.enabled')

            // refresh and re-validate
            cy.visit('/administrator/projects/proj1/self-report/configure');

            cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Assigned Fallback - All Unmatched Requests')
            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).should('be.disabled')

            cy.get(`[data-cy="workloadCell_${defaultUser}"]`).contains('Not Handling Approval Workload')
            cy.get(`[data-cy="workloadCell_${defaultUser}"] [data-cy="editApprovalBtn"]`).should('be.enabled')
        });
    });

    it('switching to the fallback should close the expanded child', function () {
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.visit('/administrator/projects/proj1/self-report/configure');

        const user1 = 'user1'
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"]`).should('exist')
        cy.get('[data-cy="workloadCell_user1"] [data-cy="fallbackSwitch"]').click()
        cy.get(`[data-cy="expandedChild_${user1}"]`).should('not.exist')
    });

    it('must always have 1 fallback approver - disable edit button if only 1 fallback approver is left', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);

            cy.configureApproverForSkillId(1, 'user2', 1)

            const defaultUser = Cypress.env('oauthMode') ? 'foo-hydra': vars.defaultUser
            const user1 = 'user1'

            cy.visit('/administrator/projects/proj1/self-report/configure');
            cy.get(`[data-cy="workloadCell_${defaultUser}"] [data-cy="editApprovalBtn"]`).should('be.enabled')
            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).should('be.enabled')
            cy.get('[data-cy="workloadCell_user2"] [data-cy="editApprovalBtn"]').should('be.enabled')

            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
            cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"]`).click();
            // cy.selectItem(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] #existingUserInput`, 'userb', true, true);
            cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]`).click()
            cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="existingUserInputDropdown"]`).type('userb')
            cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('userb').click();

            cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).click()
            cy.get(`[data-cy="workloadCell_${user1}"]`).contains('1 Specific User')

            cy.get(`[data-cy="workloadCell_${defaultUser}"] [data-cy="editApprovalBtn"]`).should('not.be.enabled')
            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).should('be.enabled')
            cy.get('[data-cy="workloadCell_user2"] [data-cy="editApprovalBtn"]').should('be.enabled')

            cy.visit('/administrator/projects/proj1/self-report/configure');

            cy.get(`[data-cy="workloadCell_${defaultUser}"] [data-cy="editApprovalBtn"]`).should('not.be.enabled')
            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).should('be.enabled')
            cy.get('[data-cy="workloadCell_user2"] [data-cy="editApprovalBtn"]').should('be.enabled')
        });
    });

    it('when 2nd approver is changed to fallback edit buttons must be re-enabled', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);

            cy.configureApproverForSkillId(1, 'user1', 1)
            cy.configureApproverForSkillId(1, 'user2', 1)

            const defaultUser = Cypress.env('oauthMode') ? 'foo-hydra': vars.defaultUser
            const user1 = 'user1'

            cy.visit('/administrator/projects/proj1/self-report/configure');
            cy.get(`[data-cy="workloadCell_${defaultUser}"] [data-cy="editApprovalBtn"]`).should('not.be.enabled')
            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).should('be.enabled')
            cy.get('[data-cy="workloadCell_user2"] [data-cy="editApprovalBtn"]').should('be.enabled')

            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
            const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalSkillConfTable"]`
            cy.get(`${tableSelector} [data-cy="skillCell-skill1"] [data-cy="deleteBtn"]`).click()
            cy.get(`${tableSelector} [data-cy="skillCell-skill1"]`).should('not.exist')


            cy.get(`[data-cy="workloadCell_${defaultUser}"] [data-cy="editApprovalBtn"]`).should('be.enabled')
            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).should('be.enabled')
            cy.get('[data-cy="workloadCell_user2"] [data-cy="editApprovalBtn"]').should('be.enabled')
        });
    });

    it('paging works appropriately and does not display incorrect result component', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user3/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user4/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user5/roles/ROLE_PROJECT_APPROVER`);

            cy.visit('/administrator/projects/proj1/self-report/configure');

            cy.get('[data-pc-section="page"]').contains('2').click();
            cy.get('[data-cy="approvalConfNotAvailable"]').should('not.exist');

            cy.get('[data-pc-section="page"]').contains('1').click();

            const defaultUser = Cypress.env('oauthMode') ? 'foo': vars.defaultUser

            const tableSelector = '[data-cy="skillApprovalConfTable"]'
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user5'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user4'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user3'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user2'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user1'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: defaultUser
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
            ], 5);
        });
    });

    it('page with a single user does not disable edit button', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user3/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user4/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user5/roles/ROLE_PROJECT_APPROVER`);

            cy.visit('/administrator/projects/proj1/self-report/configure');

            cy.get('[data-pc-section="page"]').contains('2').click();
            cy.get('[data-cy="approvalConfNotAvailable"]').should('not.exist');

            cy.get('[data-pc-section="page"]').contains('1').click();

            const defaultUser = Cypress.env('oauthMode') ? 'foo': vars.defaultUser

            const tableSelector = '[data-cy="skillApprovalConfTable"]'
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user5'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user4'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user3'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user2'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user1'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: defaultUser
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
            ], 5);

            cy.get('[data-cy="editApprovalBtn"]').should('be.enabled');
        });
    });

    it('last remaining user has edit button disabled', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user3/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user4/roles/ROLE_PROJECT_APPROVER`);

            cy.configureApproverForSkillId(1, 'user1', 1)
            cy.configureApproverForSkillId(1, 'user2', 1)
            cy.configureApproverForSkillId(1, 'user3', 1)
            cy.configureApproverForSkillId(1, 'user4', 1)

            cy.visit('/administrator/projects/proj1/self-report/configure');

            const defaultUser = Cypress.env('oauthMode') ? 'foo': vars.defaultUser

            const tableSelector = '[data-cy="skillApprovalConfTable"]'
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user4'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user3'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user2'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user1'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: defaultUser
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
            ], 5);

            cy.get('[data-cy="editApprovalBtn"]').eq(4).should('not.be.enabled');
        });
    });

    it('page with a single user disables edit button appropriately', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user3/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user4/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user5/roles/ROLE_PROJECT_APPROVER`);

            cy.configureApproverForSkillId(1, 'user1', 1)
            cy.configureApproverForSkillId(1, 'user2', 1)
            cy.configureApproverForSkillId(1, 'user3', 1)
            cy.configureApproverForSkillId(1, 'user4', 1)
            cy.configureApproverForSkillId(1, 'user5', 1)

            cy.visit('/administrator/projects/proj1/self-report/configure');

            cy.get('[data-pc-section="page"]').contains('2').click();
            cy.get('[data-cy="approvalConfNotAvailable"]').should('not.exist');

            cy.get('[data-pc-section="page"]').contains('1').click();

            const defaultUser = Cypress.env('oauthMode') ? 'foo': vars.defaultUser

            const tableSelector = '[data-cy="skillApprovalConfTable"]'
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user5'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user4'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user3'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user2'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user1'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: defaultUser
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
            ], 5);

            cy.get('[data-cy="editApprovalBtn"]').should('not.be.enabled');
        });
    });

    it('item on first page is disabled when item on second page has config', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user3/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user4/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user5/roles/ROLE_PROJECT_APPROVER`);

            const defaultUser = Cypress.env('oauthMode') ? 'foo': vars.defaultUser

            cy.configureApproverForSkillId(1, 'user5', 1)
            cy.configureApproverForSkillId(1, 'user2', 1)
            cy.configureApproverForSkillId(1, 'user3', 1)
            cy.configureApproverForSkillId(1, 'user4', 1)
            cy.configureApproverForSkillId(1, Cypress.env('proxyUser'), 1)

            cy.visit('/administrator/projects/proj1/self-report/configure');

            cy.get('[data-pc-section="page"]').contains('2').click();
            cy.get('[data-cy="approvalConfNotAvailable"]').should('not.exist');

            cy.get('[data-pc-section="page"]').contains('1').click();

            const tableSelector = '[data-cy="skillApprovalConfTable"]'
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user5'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user4'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user3'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user2'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: 'user1'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: defaultUser
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
            ], 5);

            cy.get('[data-pc-section="page"]').contains('1').click();

            cy.get('[data-cy="editApprovalBtn"]').eq(4).should('not.be.enabled');
        });
    });

    it('local admins, local approvers and group admins are de-duplicated in the approver table', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);

            cy.createAdminGroupDef(1, { name: 'Admin Group 1' });
            cy.request('POST', `/admin/admin-group-definitions/adminGroup1/users/user2/roles/ROLE_ADMIN_GROUP_OWNER`);
            cy.request('POST', `/admin/admin-group-definitions/adminGroup1/users/user3/roles/ROLE_ADMIN_GROUP_OWNER`);
            cy.request('POST', `/admin/admin-group-definitions/adminGroup1/users/user4/roles/ROLE_ADMIN_GROUP_OWNER`);
            cy.addProjectToAdminGroupDef(1, 1)

            cy.createAdminGroupDef(2, { name: 'Admin Group 2' });
            cy.request('POST', `/admin/admin-group-definitions/adminGroup2/users/user3/roles/ROLE_ADMIN_GROUP_OWNER`);
            cy.request('POST', `/admin/admin-group-definitions/adminGroup2/users/user4/roles/ROLE_ADMIN_GROUP_OWNER`);
            cy.request('POST', `/admin/admin-group-definitions/adminGroup2/users/user5/roles/ROLE_ADMIN_GROUP_OWNER`);
            cy.addProjectToAdminGroupDef(2, 1)

            cy.visit('/administrator/projects/proj1/self-report/configure');

            cy.get('[data-pc-section="page"]').contains('2').click();
            cy.get('[data-cy="approvalConfNotAvailable"]').should('not.exist');

            cy.get('[data-pc-section="page"]').contains('1').click();

            const defaultUser = Cypress.env('oauthMode') ? 'foo': vars.defaultUser

            const tableSelector = '[data-cy="skillApprovalConfTable"]'
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user5'
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
                [{
                    colIndex: 0,
                    value: 'user4'
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
                [{
                    colIndex: 0,
                    value: 'user3'
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
                [{
                    colIndex: 0,
                    value: 'user2'
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
                [{
                    colIndex: 0,
                    value: 'user1'
                }, {
                    colIndex: 1,
                    value: 'Approver'
                }],
                [{
                    colIndex: 0,
                    value: defaultUser
                }, {
                    colIndex: 1,
                    value: 'Admin'
                }],
            ], 5);
        });
    });

    it('sorting works by userIdForDisplay', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user3/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user4/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/user5/roles/ROLE_PROJECT_APPROVER`);

            cy.request('POST', `/admin/projects/proj1/users/testuser/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/abcd/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/ghij/roles/ROLE_PROJECT_APPROVER`);
            cy.request('POST', `/admin/projects/proj1/users/zed/roles/ROLE_PROJECT_APPROVER`);

            cy.visit('/administrator/projects/proj1/self-report/configure');

            const defaultUser = Cypress.env('oauthMode') ? 'foo': vars.defaultUser
            cy.get('[data-pc-section="columnheadercontent"]').contains('Approver').click();

            const tableSelector = '[data-cy="skillApprovalConfTable"]'
            const tableContents = [
                [{
                    colIndex: 0,
                    value: 'abcd'
                }],
                [{
                    colIndex: 0,
                    value: 'user1'
                }],
                [{
                    colIndex: 0,
                    value: 'user2'
                }],
                [{
                    colIndex: 0,
                    value: 'ghij'
                }],
                [{
                    colIndex: 0,
                    value: 'testuser'
                }],
                [{
                    colIndex: 0,
                    value: 'user3'
                }],
                [{
                    colIndex: 0,
                    value: 'user4'
                }],
                [{
                    colIndex: 0,
                    value: 'user5'
                }],
                [{
                    colIndex: 0,
                    value: 'zed'
                }],
                [{
                    colIndex: 0,
                    value: defaultUser
                }],
            ]
            tableContents.sort((a, b) => {
                if(a[0].value < b[0].value) {
                    return -1;
                } else if (a[0].value > b[0].value) {
                    return 1;
                } else {
                    return 0;
                }
            })
            cy.validateTable(tableSelector, tableContents, 5);

            cy.get('[data-pc-section="columnheadercontent"]').contains('Approver').click();

            cy.validateTable(tableSelector, tableContents.toReversed(), 5);
        });

    });
});
