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
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        Cypress.Commands.add('rejectRequest', (requestNum = 0, rejectionMsg = 'Skill was rejected') => {
            cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
                .then((response) => {
                    cy.request('POST', '/admin/projects/proj1/approvals/reject', {
                        skillApprovalIds: [response.body.data[requestNum].id],
                        rejectionMessage: rejectionMsg,
                    });
                });
        });
    });

    it('empty approval history', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(approvalHistoryTableSelector)
            .contains('There are no records to show');
    });

    it('approval history table not shown if there are zero self-approval skills', () => {
        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(approvalHistoryTableSelector)
            .should('not.exist');
    });

    it('approved and rejected requests', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        // cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00')
        // cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00')
        // cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00')
        // cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00')
        // cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00')
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.rejectRequest(0);
        cy.reportSkill(1, 4, 'user2', '2024-09-16 11:00');
        cy.approveRequest(1, 0, 'I approve this message!');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.validateTable(approvalHistoryTableSelector, [
            [
                {
                    colIndex: 0,
                    value: 'Very Great Skill 4'
                }, {
                colIndex: 0,
                value: 'user2'
            },
                {
                    colIndex: 1,
                    value: 'Approved'
                }, {
                colIndex: 1,
                value: 'Explanation: I approve this message!'
            },
                {
                    colIndex: 2,
                    value: '2024-09-16 11:00'
                },
                {
                    colIndex: 3,
                    value: 'Today'
                }
            ],
            [
                {
                    colIndex: 0,
                    value: 'Very Great Skill 2'
                }, {
                colIndex: 0,
                value: 'user2'
            },
                {
                    colIndex: 1,
                    value: 'Rejected'
                }, {
                colIndex: 1,
                value: 'Explanation: Skill was rejected'
            },
                {
                    colIndex: 2,
                    value: '2020-09-16 11:00'
                },
                {
                    colIndex: 3,
                    value: 'Today'
                }
            ],
            [
                {
                    colIndex: 0,
                    value: 'Very Great Skill 3'
                }, {
                colIndex: 0,
                value: 'user1'
            },
                {
                    colIndex: 1,
                    value: 'Approved'
                },
                {
                    colIndex: 2,
                    value: '2020-09-17 11:00'
                },
                {
                    colIndex: 3,
                    value: 'Today'
                }
            ],
            [
                {
                    colIndex: 0,
                    value: 'Very Great Skill 1'
                }, {
                colIndex: 0,
                value: 'user0'
            },
                {
                    colIndex: 1,
                    value: 'Approved'
                },
                {
                    colIndex: 2,
                    value: '2020-09-18 11:00'
                },
                {
                    colIndex: 3,
                    value: 'Today'
                }
            ],
        ]);
    });

    it('approval history notes and rejection message should display truncated', () => {
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        const requestMsg = new Array(40).join('lorem ');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00', true, requestMsg);
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        const rejectMsg = new Array(250).join('A');
        cy.rejectRequest(0, rejectMsg);

        cy.intercept('/admin/projects/proj1/approvals/history*')
            .as('loadHistory');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@loadHistory');
        cy.get('[data-cy=showMore]')
            .should('have.length', 1);
        cy.get('[data-cy="selfReportApprovalHistoryTable"] [data-pc-section="bodyrow"]').first().find('[data-cy="showMoreText"] [data-cy="showMore"]').should('be.visible');
        cy.get('[data-cy=smtText]')
            .eq(0)
            .should('have.text', `${rejectMsg.substr(0, 50)}`);
        cy.get('[data-cy="selfReportApprovalHistoryTable"] [data-pc-section="bodyrow"]').first().find('[data-cy="showMoreText"] [data-cy="showMore"]').click();
        cy.get('[data-cy=showMore]').should('have.length', 0);
        cy.get('[data-cy=showLess]').should('have.length', 1);
        cy.get('[data-cy=smtText]').eq(0).should('have.text', `${rejectMsg}`);
        cy.get('[data-cy="selfReportApprovalHistoryTable"] [data-pc-section="bodyrow"]').first().find('[data-cy="showMoreText"] [data-cy="showLess"]').click();
        cy.get('[data-cy="selfReportApprovalHistoryTable"] [data-pc-section="bodyrow"]').first().find('[data-cy="showMoreText"] [data-cy="showMore"]').should('be.visible');
        cy.get('[data-cy=smtText]')
            .eq(0)
            .should('have.text', `${rejectMsg.substr(0, 50)}`);

        cy.log('validating second row');

        cy.get('[data-cy="expandDetailsBtn_skill2"]')
          .click();
        cy.get('[data-cy="selfReportApprovalHistoryTable"]')
          .contains('No Justification supplied')
          .should('exist');
        cy.get('[data-cy="approvalMessage"]')
          .should('not.exist');
        cy.get('[data-cy="expandDetailsBtn_skill3"]')
          .click();
        cy.get('[data-cy="selfReportApprovalHistoryTable"]')
          .contains('Requested points with the following justification:')
          .should('exist');
        cy.get('[data-cy="approvalMessage"]')
          .should('have.length', 1)
          .eq(0)
          .should('contain.text', requestMsg.trim());

    });

    it('rejected request without explanation', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.rejectRequest(0, '');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(approvalHistoryTableSelector)
            .contains('Explanation')
            .should('not.exist');
    });

    it('approved and rejected requests - sorting', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.rejectRequest(0);

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.rejectRequest(0);

        cy.visit('/administrator/projects/proj1/self-report');
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'user3'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user1'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user0'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user2'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user6'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
        ], 5, true, 7);

        const headerSelector = `${approvalHistoryTableSelector} thead tr th`;
        cy.get(headerSelector)
          .contains('Response On')
          .click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'user4'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user5'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user6'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user2'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user0'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
        ], 5, true, 7);

        cy.get(headerSelector)
          .contains('Requested On')
          .click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 2,
                value: '2020-09-11 11:00'
            },],
            [{
                colIndex: 2,
                value: '2020-09-12 11:00'
            },],
            [{
                colIndex: 2,
                value: '2020-09-13 11:00'
            },],
            [{
                colIndex: 2,
                value: '2020-09-14 11:00'
            },],
            [{
                colIndex: 2,
                value: '2020-09-16 11:00'
            },],
        ], 5, true, 7);

        cy.get(headerSelector)
          .contains('Requested On')
          .click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 2,
                value: '2020-09-18 11:00'
            },],
            [{
                colIndex: 2,
                value: '2020-09-17 11:00'
            },],
            [{
                colIndex: 2,
                value: '2020-09-16 11:00'
            },],
            [{
                colIndex: 2,
                value: '2020-09-14 11:00'
            },],
            [{
                colIndex: 2,
                value: '2020-09-13 11:00'
            },],
        ], 5, true, 7);

        cy.get(headerSelector)
          .contains('Response')
          .click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 1,
                value: 'Approved'
            },],
        ], 5, true, 7);

        cy.get(headerSelector)
          .contains('Response')
          .click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 1,
                value: 'Approved'
            },],
        ], 5, true, 7);

        cy.get(headerSelector)
          .contains('Requested')
          .click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
        ], 5, true, 7);

        cy.get(headerSelector)
          .contains('Requested')
          .click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 3'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
        ], 5, true, 7);

        cy.visit('/administrator/projects/proj1/self-report');
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 3'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
        ], 5, true, 7);

    });

    it('sorting on the 2nd page+ should re-set paging', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.rejectRequest(0);

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.rejectRequest(0);

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(`${approvalHistoryTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', '7')
        cy.get(approvalHistoryTableSelector)
            .get('[data-pc-name="pcpaginator"]')
            .contains('2')
            .click();
        cy.get(approvalHistoryTableSelector)
            .contains('Loading')
            .should('not.exist');
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'user5'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user4'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
        ], 5, true, 2, false);

        const headerSelector = `${approvalHistoryTableSelector} thead tr th`;
        cy.get(headerSelector)
            .contains('Response On')
            .click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'user4'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user5'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user6'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user2'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user0'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
        ], 5, true, 7);
    });

    it('paging', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.rejectRequest(0);

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.rejectRequest(0);

        cy.visit('/administrator/projects/proj1/self-report');
        // validateTable will page if more than 5 records
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'user3'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user1'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user0'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user2'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user6'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user5'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user4'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
        ]);
    });

    it('page size', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.approveAllRequests();
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.rejectRequest(0);

        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');
        cy.approveAllRequests();

        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.rejectRequest(0);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get(approvalHistoryTableSelector).find('[data-pc-name="rowperpagedropdown"]').click();

        cy.get('.p-dropdown-item-label').contains(10).click();

        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'user3'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user1'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user0'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user2'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user6'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user5'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user4'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
        ], 10, true, 7);
    });

    if (!Cypress.env('oauthMode')) {
        it('filter fields - by pressing filter button', () => {
            cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
            cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
            cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
            cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
            cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
            cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
            cy.approveAllRequests();
            cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
            cy.rejectRequest(0);

            cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
            cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');
            cy.approveAllRequests();

            cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
            cy.rejectRequest(0);

            cy.visit('/administrator/projects/proj1/self-report');
            cy.get('[data-cy="selfReportApprovalHistory-skillNameFilter"]')
                .type('sKilL 1');
            cy.get('[data-cy="selfReportApprovalHistory-filterBtn"]')
                .click();
            cy.validateTable(approvalHistoryTableSelector, [
                [{
                    colIndex: 0,
                    value: 'user0'
                }, {
                    colIndex: 1,
                    value: 'Approved'
                },],
            ]);

            cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]')
                .click();
            cy.get(approvalHistoryTableSelector)
                .contains('Total Rows: 7');

            cy.get('[data-cy="selfReportApprovalHistory-userIdFilter"]')
                .type('SeR2');
            cy.get('[data-cy="selfReportApprovalHistory-filterBtn"]')
                .click();
            cy.validateTable(approvalHistoryTableSelector, [
                [{
                    colIndex: 0,
                    value: 'user2'
                }, {
                    colIndex: 1,
                    value: 'Rejected'
                },],
            ]);

            cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]')
                .click();
            cy.get(approvalHistoryTableSelector)
                .contains('Total Rows: 7');

            cy.get('[data-cy="selfReportApprovalHistory-approverUserIdFilter"]')
                .type('@skills.or');
            cy.get('[data-cy="selfReportApprovalHistory-filterBtn"]')
                .click();
            cy.get(approvalHistoryTableSelector)
                .contains('Total Rows: 7');

            cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]')
                .click();
            cy.get(approvalHistoryTableSelector)
                .contains('Total Rows: 7');

            // no results
            cy.get('[data-cy="selfReportApprovalHistory-skillNameFilter"]')
                .type('sg');
            cy.get('[data-cy="selfReportApprovalHistory-filterBtn"]')
                .click();
            cy.get(approvalHistoryTableSelector)
                .contains(('There are no records to show'));
        });

        it('filter fields - by pressing enter', () => {
            cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
            cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
            cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
            cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
            cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
            cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
            cy.approveAllRequests();
            cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
            cy.rejectRequest(0);

            cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
            cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');
            cy.approveAllRequests();

            cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
            cy.rejectRequest(0);

            cy.visit('/administrator/projects/proj1/self-report');
            cy.get('[data-cy="selfReportApprovalHistory-skillNameFilter"]')
                .type('sKilL 1{enter}');
            cy.validateTable(approvalHistoryTableSelector, [
                [{
                    colIndex: 0,
                    value: 'user0'
                }, {
                    colIndex: 1,
                    value: 'Approved'
                },],
            ]);

            cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]')
                .click();
            cy.get(approvalHistoryTableSelector)
                .contains('Total Rows: 7');

            cy.get('[data-cy="selfReportApprovalHistory-userIdFilter"]')
                .type('SeR2{enter}');
            cy.validateTable(approvalHistoryTableSelector, [
                [{
                    colIndex: 0,
                    value: 'user2'
                }, {
                    colIndex: 1,
                    value: 'Rejected'
                },],
            ]);

            cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]')
                .click();
            cy.get(approvalHistoryTableSelector)
                .contains('Total Rows: 7');

            cy.get('[data-cy="selfReportApprovalHistory-approverUserIdFilter"]')
                .type('@skills.or{enter}');
            cy.get(approvalHistoryTableSelector)
                .contains('Total Rows: 7');

            cy.get('[data-cy="selfReportApprovalHistory-resetBtn"]')
                .click();
            cy.get(approvalHistoryTableSelector)
                .contains('Total Rows: 7');

            // no results
            cy.get('[data-cy="selfReportApprovalHistory-skillNameFilter"]')
                .type('sg{enter}');
            cy.get(approvalHistoryTableSelector)
                .contains(('There are no records to show'));
        });
    }

    it('approval should move the item to the history table', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get(approvalHistoryTableSelector)
            .contains(('There are no records to show'));

        cy.get('[data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="approveBtn"]').click();
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'user2'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
        ]);

        cy.get('[data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="rejectBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .click();
        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'user4'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user2'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
        ]);

        cy.wait(1000);

        cy.get('[data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="approveBtn"]').click();
        cy.get('[data-cy="saveDialogBtn"]').click();

        cy.validateTable(approvalHistoryTableSelector, [
            [{
                colIndex: 0,
                value: 'user5'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
            [{
                colIndex: 0,
                value: 'user4'
            }, {
                colIndex: 1,
                value: 'Rejected'
            },],
            [{
                colIndex: 0,
                value: 'user2'
            }, {
                colIndex: 1,
                value: 'Approved'
            },],
        ]);

    });

    it('pending approval table - expand multiple justifications', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'user1', '2020-09-17 11:00', true, 'please approve request 1');
        // cy.rejectRequest();
        cy.reportSkill(1, 1, 'user2', moment.utc(), true, 'please approve request 2');
        // cy.approveAllRequests();

        cy.intercept('/admin/projects/proj1/approvals/history*')
            .as('loadHistory');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@loadHistory');

        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="expandDetailsBtn_skill1"]')
            .should('have.length', 2)
            .eq(0)
            .click();

        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="expandDetailsBtn_skill1"]')
            .should('have.length', 2)
            .eq(1)
            .click();

        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="approvalMessage"]')
            .should('have.length', 2)
            .eq(0)
            .should('contain.text', 'please approve request 2')

        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="approvalMessage"]')
            .should('have.length', 2)
            .eq(1)
            .should('contain.text', 'please approve request 1');
    });

    it('approval history table - expand multiple justifications', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'user1', '2020-09-17 11:00', true, 'please approve request 1');
        cy.rejectRequest();
        cy.reportSkill(1, 1, 'user1', moment.utc(), true, 'please approve request 2');
        cy.approveAllRequests();

        cy.intercept('/admin/projects/proj1/approvals/history*')
            .as('loadHistory');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@loadHistory');

        cy.get('[data-cy="selfReportApprovalHistoryTable"] [data-cy="expandDetailsBtn_skill1"]')
            .should('have.length', 2)
            .eq(0)
            .click();

        cy.get('[data-cy="selfReportApprovalHistoryTable"] [data-cy="expandDetailsBtn_skill1"]')
            .should('have.length', 2)
            .eq(1)
            .click();

        cy.get('[data-cy="selfReportApprovalHistoryTable"] [data-cy="approvalMessage"]')
            .should('have.length', 2)
            .eq(0)
            .should('contain.text', 'please approve request 2')

        cy.get('[data-cy="selfReportApprovalHistoryTable"] [data-cy="approvalMessage"]')
            .should('have.length', 2)
            .eq(1)
            .should('contain.text', 'please approve request 1');
    });

});
