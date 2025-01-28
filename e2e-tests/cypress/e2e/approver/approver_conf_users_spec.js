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

describe('Approver Config Users Tests', () => {
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

    it('configure approver for a specific user', function () {
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);

        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalConfSpecificUsersTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noUserConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).should('be.disabled')

        // cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"]`).click();
        cy.selectItem(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] #existingUserInput`, 'usera', true, true);
        // cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] .p-dropdown-option`).contains('userA').click();
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).should('be.enabled')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noUserConf"]`).should('not.exist')

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'userA'
            }],
        ], 5);
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('1 Specific User')

        // cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"]`).click();
        cy.selectItem(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] #existingUserInput`, 'userb', true, true);
        // cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] .p-dropdown-option`).contains('userB').click();
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).should('be.enabled')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).click()

        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector).contains('User').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'userA'
            }],
            [{
                colIndex: 0,
                value: 'userB'
            }],
        ], 5);
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('2 Specific Users')

        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'userA'
            }],
            [{
                colIndex: 0,
                value: 'userB'
            }],
        ], 5);
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('2 Specific Users')

    });

    it('remove users conf from an approver', function () {
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
        cy.configureApproverForUser(1, 'user1', 'userA')
        cy.configureApproverForUser(1, 'user1', 'userB')

        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()

        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalConfSpecificUsersTable"]`
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '2')
        cy.get(`${tableSelector} [data-cy="userIdCell-usera"]`)
        cy.get(`${tableSelector} [data-cy="userIdCell-userb"]`)
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('2 Specific Users')

        cy.get(`${tableSelector} [data-cy="userIdCell-userb"] [data-cy="deleteBtn"]`).click()
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '1')
        cy.get(`${tableSelector} [data-cy="userIdCell-usera"]`)
        cy.get(`${tableSelector} [data-cy="userIdCell-userb"]`).should('not.exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('1 Specific User')

        cy.get(`${tableSelector} [data-cy="userIdCell-usera"] [data-cy="deleteBtn"]`).click()
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('not.exist')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noUserConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
    });

    it('duplicate user validation', function () {
        const user1 = 'user1'

        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);

        cy.configureApproverForUser(1, 'user1', 'userA')
        cy.configureApproverForUser(1, 'user1', 'userB')

        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()

        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalConfSpecificUsersTable"]`
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '2')
        cy.get(`${tableSelector} [data-cy="userIdCell-usera"]`)
        cy.get(`${tableSelector} [data-cy="userIdCell-userb"]`)
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('2 Specific Users')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] #existingUserInput [data-pc-name="dropdownbutton"]`).click();
        cy.get('[data-pc-section="option"]').contains('userB').should('not.exist');
        cy.get('[data-pc-section="option"]').contains('userA').should('not.exist');
        cy.get('[data-pc-section="option"]').contains('userc').click();

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).should('be.enabled')
        // cy.get('[data-cy="userIdInputErr"]').should('not.be.visible')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).click()
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '3')
        cy.get(`${tableSelector} [data-cy="userIdCell-usera"]`)
        cy.get(`${tableSelector} [data-cy="userIdCell-userb"]`)
        cy.get(`${tableSelector} [data-cy="userIdCell-userc"]`)
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('3 Specific Users')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] #existingUserInput`).type('usera{enter}');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).should('be.disabled')
        cy.get('[data-cy="userIdInputError"]').should('be.visible')
    });

    it('user conf table paging', function () {
        const user1 = 'user1'
        cy.reportSkill(1, 1, 'userD', 'now');
        cy.reportSkill(1, 1, 'userE', 'now');
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
        cy.configureApproverForUser(1, 'user1', 'userA')
        cy.configureApproverForUser(1, 'user1', 'userB')
        cy.configureApproverForUser(1, 'user1', 'userC')
        cy.configureApproverForUser(1, 'user1', 'userD')
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="skillApprovalConfSpecificUsersTable"]`
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector).contains('User').click();


        cy.fixture('vars.json').then((vars) => {
            const defaultUser = Cypress.env('oauthMode') ? 'foo' : vars.defaultUser
            cy.selectItem(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] #existingUserInput`, defaultUser, true, true);
            cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).click()

            const compare = (a, b) => {
                return a[0].value?.localeCompare(b[0].value)
            }
            const expected = [
                [{ colIndex: 0, value: 'userA' }],
                [{ colIndex: 0, value: 'userB' }],
                [{ colIndex: 0, value: 'userC' }],
                [{ colIndex: 0, value: 'userD' }],
                [{ colIndex: 0, value: defaultUser }],
            ].sort(compare)
            cy.validateTable(tableSelector, expected, 4);
        })
    })
});
