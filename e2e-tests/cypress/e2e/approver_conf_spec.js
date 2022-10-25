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


    it('must have at least 1 admin to configure the proejct', function () {
        cy.visit('/administrator/projects/proj1/self-report/configure');

        cy.get('[data-cy="approvalConfNotAvailable"]')
        cy.get('[data-cy="approvalConfNotAvailable"] [data-cy="navToAccessPage"]').click();
        cy.get('[data-cy="projectAdmins"]')
    });

    it('assign approver to handle all of the approval requests', function () {
        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
            cy.visit('/administrator/projects/proj1/self-report/configure');

            const user1 = 'user1'
            cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
            cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).should('be.enabled')

            const defaultUser = vars.defaultUser
            cy.get(`[data-cy="workloadCell_${defaultUser}"]`).contains('Default Fallback - All Unmatched Requests')
            cy.get(`[data-cy="workloadCell_${defaultUser}"] [data-cy="editApprovalBtn"]`).should('be.enabled')

            // switch to fallback
            cy.get('[data-cy="workloadCell_user1"] [data-cy="fallbackSwitch"]').click({force: true})

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
        cy.get('[data-cy="workloadCell_user1"] [data-cy="fallbackSwitch"]').click({force: true})
        cy.get(`[data-cy="expandedChild_${user1}"]`).should('not.exist')
    });

    it('configure approver for a specific user', function () {
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);

        cy.configureApproverForSkillId(1, 'user1', 1)
        cy.configureApproverForUserTag(1, 'user1', 'tagKey', 'tagValue')
        cy.configureApproverForUser(1, 'user1', 'userA')
        cy.configureApproverForUser(1, 'user1', 'userB')

        cy.visit('/administrator/projects/proj1/self-report/configure');



    });


});
