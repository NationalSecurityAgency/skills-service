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
var moment = require('moment-timezone');

describe('Self Report Approval History Tests', () => {

    const approvalHistoryTableSelector = '[data-cy="selfReportApprovalHistoryTable"]';

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        })

        Cypress.Commands.add("rejectRequest", (requestNum = 0, rejectionMsg = 'Skill was rejected') => {
            cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
                .then((response) => {
                    cy.request('POST', '/admin/projects/proj1/approvals/reject', {
                        skillApprovalIds: [response.body.data[requestNum].id],
                        rejectionMessage: rejectionMsg,
                    });
                });
        });
        Cypress.Commands.add("approveAllRequests", () => {
            cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
                .then((response) => {
                    response.body.data.forEach((item) => {
                        cy.wait(200); // that way sort works properly
                        cy.request('POST', '/admin/projects/proj1/approvals/approve', {
                            skillApprovalIds: [item.id],
                        });
                    })
                });
        });
    });

    it('empty approval history', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(approvalHistoryTableSelector).contains('There are no records to show');
    });

    it('approval history table not shown if there are zero self-approval skills', () => {
        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(approvalHistoryTableSelector).should('not.exist');
    });


    it('approved and rejected requests', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        // cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        // cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        // cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        // cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        // cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.rejectRequest(0)

        cy.visit('/administrator/projects/proj1/self-report');
        cy.validateTable(approvalHistoryTableSelector,  [
            [
                { colIndex: 0,  value: 'Very Great Skill 2' }, { colIndex: 0,  value: 'user2' },
                { colIndex: 1,  value: 'Rejected' }, { colIndex: 1,  value: 'Explanation: Skill was rejected' },
                { colIndex: 2,  value: '2020-09-16 11:00' },
                { colIndex: 3,  value: 'Today' }
            ],
            [
                { colIndex: 0,  value: 'Very Great Skill 3' },  { colIndex: 0,  value: 'user1' },
                { colIndex: 1,  value: 'Approved' },
                { colIndex: 2,  value: '2020-09-17 11:00' },
                { colIndex: 3,  value: 'Today' }
            ],
            [
                { colIndex: 0,  value: 'Very Great Skill 1' },  { colIndex: 0,  value: 'user0' },
                { colIndex: 1,  value: 'Approved' },
                { colIndex: 2,  value: '2020-09-18 11:00' },
                { colIndex: 3,  value: 'Today' }
            ],
        ]);
    });

    it('rejected request without explanation', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.rejectRequest(0, '')

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(approvalHistoryTableSelector).contains('Explanation').should('not.exist');
    });


    it('approved and rejected requests - sorting', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.rejectRequest(0)

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        cy.rejectRequest(0)

        cy.visit('/administrator/projects/proj1/self-report');
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user3' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user1' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user0' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user6' }, { colIndex: 1,  value: 'Approved' }, ],
        ], 5, true, 7)

        const headerSelector = `${approvalHistoryTableSelector} thead tr th`
        cy.get(headerSelector).contains('Response On').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user4' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user5' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user6' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user0' }, { colIndex: 1,  value: 'Approved' }, ],
        ], 5, true, 7)

        cy.get(headerSelector).contains('Requested On').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 2,  value: '2020-09-11 11:00' }, ],
            [ { colIndex: 2,  value: '2020-09-12 11:00' }, ],
            [ { colIndex: 2,  value: '2020-09-13 11:00' }, ],
            [ { colIndex: 2,  value: '2020-09-14 11:00' }, ],
            [ { colIndex: 2,  value: '2020-09-16 11:00' }, ],
        ], 5, true, 7)

        cy.get(headerSelector).contains('Requested On').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 2,  value: '2020-09-18 11:00' }, ],
            [ { colIndex: 2,  value: '2020-09-17 11:00' }, ],
            [ { colIndex: 2,  value: '2020-09-16 11:00' }, ],
            [ { colIndex: 2,  value: '2020-09-14 11:00' }, ],
            [ { colIndex: 2,  value: '2020-09-13 11:00' }, ],
        ], 5, true, 7)

        cy.get(headerSelector).contains('Response').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 1,  value: 'Approved' }, ],
        ], 5, true, 7)

        cy.get(headerSelector).contains('Response').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 1,  value: 'Approved' }, ],
        ], 5, true, 7)

        cy.get(headerSelector).contains('Requested').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'Very Great Skill 1' }],
            [ { colIndex: 0,  value: 'Very Great Skill 2' }],
            [ { colIndex: 0,  value: 'Very Great Skill 2' }],
            [ { colIndex: 0,  value: 'Very Great Skill 2' }],
            [ { colIndex: 0,  value: 'Very Great Skill 2' }],
        ], 5, true, 7)

        cy.get(headerSelector).contains('Requested').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'Very Great Skill 3' }],
            [ { colIndex: 0,  value: 'Very Great Skill 2' }],
            [ { colIndex: 0,  value: 'Very Great Skill 2' }],
            [ { colIndex: 0,  value: 'Very Great Skill 2' }],
            [ { colIndex: 0,  value: 'Very Great Skill 2' }],
        ], 5, true, 7)
    });

    it('sorting on the 2nd page+ should re-set paging', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.rejectRequest(0)

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        cy.rejectRequest(0)

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(approvalHistoryTableSelector).get('[data-cy=skillsBTablePaging]').contains('2').click();
        cy.get(approvalHistoryTableSelector).contains('Loading').should('not.exist');
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user5' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user4' }, { colIndex: 1,  value: 'Approved' }, ],
        ], 5, true, 2, false)

        const headerSelector = `${approvalHistoryTableSelector} thead tr th`
        cy.get(headerSelector).contains('Response On').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user4' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user5' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user6' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user0' }, { colIndex: 1,  value: 'Approved' }, ],
        ], 5, true, 7)
    });

    it('paging', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.rejectRequest(0)

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        cy.rejectRequest(0)

        cy.visit('/administrator/projects/proj1/self-report');
        // validateTable will page if more than 5 records
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user3' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user1' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user0' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user6' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user5' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user4' }, { colIndex: 1,  value: 'Approved' }, ],
        ])
     });


    it('page size', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.rejectRequest(0)

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        cy.rejectRequest(0)

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(`${approvalHistoryTableSelector} [data-cy="skillsBTablePageSize"]`).select('10');
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user3' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user1' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user0' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user6' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user5' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user4' }, { colIndex: 1,  value: 'Approved' }, ],
        ], 10, true, 7)
    });

    it('filter by skill name - by pressing filter button', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.rejectRequest(0)

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        cy.rejectRequest(0)

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="selfReportApprovalHistory-skillNameFilter"]').type('sKilL 1');
        cy.get('[data-cy="selfReportApprovalHistory-filterBtn"]').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user0' }, { colIndex: 1,  value: 'Approved' }, ],
        ])

        cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]').click();
        cy.get(approvalHistoryTableSelector).contains('Total Rows: 7');

        cy.get('[data-cy="selfReportApprovalHistory-userIdFilter"]').type('SeR2');
        cy.get('[data-cy="selfReportApprovalHistory-filterBtn"]').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Rejected' }, ],
        ])

        cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]').click();
        cy.get(approvalHistoryTableSelector).contains('Total Rows: 7');

        cy.get('[data-cy="selfReportApprovalHistory-approverUserIdFilter"]').type('@skills.or');
        cy.get('[data-cy="selfReportApprovalHistory-filterBtn"]').click();
        cy.get(approvalHistoryTableSelector).contains('Total Rows: 7');

        cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]').click();
        cy.get(approvalHistoryTableSelector).contains('Total Rows: 7');

        // no results
        cy.get('[data-cy="selfReportApprovalHistory-skillNameFilter"]').type('sg');
        cy.get('[data-cy="selfReportApprovalHistory-filterBtn"]').click();
        cy.get(approvalHistoryTableSelector).contains(('There are no records to show'));
    });

    it('filter by skill name - by pressing enter', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.rejectRequest(0)

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00')
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00')
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        cy.rejectRequest(0)

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="selfReportApprovalHistory-skillNameFilter"]').type('sKilL 1{enter}');
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user0' }, { colIndex: 1,  value: 'Approved' }, ],
        ])

        cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]').click();
        cy.get(approvalHistoryTableSelector).contains('Total Rows: 7');

        cy.get('[data-cy="selfReportApprovalHistory-userIdFilter"]').type('SeR2{enter}');
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Rejected' }, ],
        ])

        cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]').click();
        cy.get(approvalHistoryTableSelector).contains('Total Rows: 7');

        cy.get('[data-cy="selfReportApprovalHistory-approverUserIdFilter"]').type('@skills.or{enter}');
        cy.get(approvalHistoryTableSelector).contains('Total Rows: 7');

        cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]').click();
        cy.get(approvalHistoryTableSelector).contains('Total Rows: 7');

        // no results
        cy.get('[data-cy="selfReportApprovalHistory-skillNameFilter"]').type('sg{enter}');
        cy.get(approvalHistoryTableSelector).contains(('There are no records to show'));
    });

    it('approval should move the item to the history table', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(approvalHistoryTableSelector).contains(('There are no records to show'));

        cy.get('[data-cy="approvalSelect_user2-skill2"]').click({force: true});
        cy.get('[data-cy="approveBtn"]').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Approved' }, ],
        ])

        cy.get('[data-cy="approvalSelect_user4-skill2"]').click({force: true});
        cy.get('[data-cy="rejectBtn"]').click();
        cy.get('[data-cy="confirmRejectionBtn"]').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user4' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Approved' }, ],
        ])

        cy.get('[data-cy="approvalSelect_user5-skill2"]').click({force: true});
        cy.get('[data-cy="approveBtn"]').click();
        cy.validateTable(approvalHistoryTableSelector,  [
            [ { colIndex: 0,  value: 'user5' }, { colIndex: 1,  value: 'Approved' }, ],
            [ { colIndex: 0,  value: 'user4' }, { colIndex: 1,  value: 'Rejected' }, ],
            [ { colIndex: 0,  value: 'user2' }, { colIndex: 1,  value: 'Approved' }, ],
        ])

    });

})