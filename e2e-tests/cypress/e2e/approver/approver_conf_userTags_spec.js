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

describe('Approver Config User Tags Tests', () => {

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

    it('configure approver for user tags', function () {
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);

        cy.visit('/administrator/projects/proj1/self-report/configure');
        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="tagKeyConfTable"]`
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noTagKeyConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).should('be.disabled')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).type('First');

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noTagKeyConf"]`).should('not.exist')
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'First'
            }],
        ], 5);

        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in Org: First')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).should('be.disabled')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).should('not.have.value')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).type('SeCond');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).should('have.value', 'SeCond')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'SeCond'
            }],
            [{
                colIndex: 0,
                value: 'First'
            }],
        ], 5);
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in Org: First')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in Org: SeCond')

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'SeCond'
            }],
            [{
                colIndex: 0,
                value: 'First'
            }],
        ], 5);
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in Org: First')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in Org: SeCond')

    });

    it('remove user tags conf from an approver', function () {
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
        cy.configureApproverForUserTag(1, 'user1', 'tagKey', 'first')
        cy.configureApproverForUserTag(1, 'user1', 'tagKey', 'second')

        const user1 = 'user1'
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="tagKeyConfTable"]`
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in tagKey: first')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in tagKey: second')

        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '2')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="tagValue_second"] [data-cy="deleteBtn"]`).click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'first'
            }],
        ], 5);
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in tagKey: first')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in tagKey: second').should('not.exist')

        // refresh and revalidate
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'first'
            }],
        ], 5);
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in tagKey: first')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in tagKey: second').should('not.exist')

        // remove last one
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noTagKeyConf"]`).should('not.exist')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="tagValue_first"] [data-cy="deleteBtn"]`).click()
        cy.get(tableSelector).should('not.exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in tagKey: first').should('not.exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Users in tagKey: second').should('not.exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noTagKeyConf"]`).should('exist')

        // refresh and revalidate
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(tableSelector).should('not.exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noTagKeyConf"]`).should('exist')

    });

    it('entering user tag conf validation', function () {
        const user1 = 'user1'

        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
        cy.configureApproverForUserTag(1, 'user1', 'tagKey', 'fiRst')
        cy.configureApproverForUserTag(1, 'user1', 'tagKey', 'second')
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        // no spaces
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).type('s s');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="tagInputError"]`).should('have.text', 'Org may only contain alpha-numeric characters')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).should('not.be.enabled')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).type('{backspace}{backspace}');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="tagInputError"]`).should('not.be.visible')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).should('be.enabled')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).type('{backspace}FIrst');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="tagInputError"]`).should('have.text', 'There is already an entry for this Org value.')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).should('not.be.enabled')
    })

    it('only show user tags if approvalConfUserTagKey conf is provided', function () {
        const user1 = 'user1'

        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const result = res.body
                delete result.approvalConfUserTagKey;
                delete result.approvalConfUserTagLabel;
                res.send(result);
            });
        }).as('getConfig');

        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="tagKeyConfTable"]`).should('not.exist')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).should('not.exist')
        cy.wait('@getConfig')
    })

    it('user tag conf table paging', function () {
        const user1 = 'user1'

        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);
        cy.request('POST', `/admin/projects/proj1/users/user2/roles/ROLE_PROJECT_APPROVER`);
        cy.configureApproverForUserTag(1, 'user1', 'tagKey', '1')
        cy.configureApproverForUserTag(1, 'user1', 'tagKey', '2')
        cy.configureApproverForUserTag(1, 'user1', 'tagKey', '3')
        cy.configureApproverForUserTag(1, 'user1', 'tagKey', '4')
        cy.visit('/administrator/projects/proj1/self-report/configure');
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        const tableSelector = `[data-cy="expandedChild_${user1}"] [data-cy="tagKeyConfTable"]`
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector).contains('Org').click();
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).type('5');
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).click()

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: '1' }],
            [{ colIndex: 0, value: '2' }],
            [{ colIndex: 0, value: '3' }],
            [{ colIndex: 0, value: '4' }],
            [{ colIndex: 0, value: '5' }],
        ], 4);
    })

});
