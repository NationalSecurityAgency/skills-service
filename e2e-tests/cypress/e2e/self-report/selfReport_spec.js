/*
 * Copyright 2026 SkillTree
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

describe('Approval Requests Management Tests', () => {

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

    it('skill overview - display self reporting card', () => {
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill1`,
            name: `Very Great Skill # 1`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
            selfReportingType: 'Approval'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill2`,
            name: `Very Great Skill # 2`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
            selfReportingType: 'HonorSystem'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill3`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill3`,
            name: `Very Great Skill # 3`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('Self Report: Approval');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardSubTitle"]')
            .contains('Users can self report this skill and will go into an approval queue');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('Self Report: Honor System');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardSubTitle"]')
            .contains('Users can self report this skill and will apply immediately');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill3');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('Self Report: Disabled');
        cy.get('[data-cy="selfReportMediaCard"] [data-cy="mediaInfoCardSubTitle"]')
            .contains('Self reporting is disabled for this skill');
    });

    it('sorting and paging of the approval table', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user5Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user4Good@skills.org', '2020-09-14 11:00');
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1Good@skills.org', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0Good@skills.org', '2020-09-18 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        const expected = [
            [{
                colIndex: 2,
                value: 'user0Good@skills.org'
            }, {
                colIndex: 3,
                value: '2020-09-18 11:00'
            }],
            [{
                colIndex: 2,
                value: 'user1Good@skills.org'
            }, {
                colIndex: 3,
                value: '2020-09-17 11:00'
            }],
            [{
                colIndex: 2,
                value: 'user2Good@skills.org'
            }, {
                colIndex: 3,
                value: '2020-09-16 11:00'
            }],
            [{
                colIndex: 2,
                value: 'user3Good@skills.org'
            }, {
                colIndex: 3,
                value: '2020-09-15 11:00'
            }],
            [{
                colIndex: 2,
                value: 'user4Good@skills.org'
            }, {
                colIndex: 3,
                value: '2020-09-14 11:00'
            }],
            [{
                colIndex: 2,
                value: 'user5Good@skills.org'
            }, {
                colIndex: 3,
                value: '2020-09-13 11:00'
            }],
            [{
                colIndex: 2,
                value: 'user6Good@skills.org'
            }, {
                colIndex: 3,
                value: '2020-09-12 11:00'
            }],
        ];
        const expectedReversed = [...expected].reverse();

        cy.validateTable(tableSelector, expected);

        cy.get(`${tableSelector} th`)
            .contains('Requested On')
            .click();
        cy.validateTable(tableSelector, expectedReversed);

        cy.get(`${tableSelector} th`)
            .contains('For User')
            .click();
        cy.validateTable(tableSelector, expected);
        cy.get(`${tableSelector} th`)
            .contains('For User')
            .click();
        cy.validateTable(tableSelector, expectedReversed);

        // Very sorting persists
        cy.visit('/administrator/projects/proj1/self-report');
        cy.validateTable(tableSelector, expectedReversed);

        cy.get(`${tableSelector} th`)
            .contains('For User')
            .click();
        cy.validateTable(tableSelector, expected);
        cy.visit('/administrator/projects/proj1/self-report');
        cy.validateTable(tableSelector, expected);
    });

    it('change page size of the approval table', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user5Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user4Good@skills.org', '2020-09-14 11:00');
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1Good@skills.org', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0Good@skills.org', '2020-09-18 11:00');

        cy.visit('/administrator/projects/proj1/self-report');
        const rowSelector = '[data-cy="skillsReportApprovalTable"] tbody tr';
        cy.get(rowSelector)
            .should('have.length', 5);

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.get('[data-cy="skillsReportApprovalTable"] [data-pc-name="pcrowperpagedropdown"]')
          .click().get('[data-pc-section="option"]').contains('10').click();
        cy.get(rowSelector)
            .should('have.length', 7);
    });

    it('refresh button should pull from server', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user1', '2020-09-12 11:00');

        cy.visit('/administrator/projects/proj1/self-report');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user1'
            }],
        ]);

        cy.reportSkill(1, 2, 'user2', '2020-09-11 11:00');

        cy.get('[data-cy="syncApprovalsBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user1'
            }],
            [{
                colIndex: 2,
                value: 'user2'
            }],
        ]);
    });

    it('self report stats', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 4, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReportInfoCardCount_Disabled"]')
            .contains('1');
        cy.get('[data-cy="selfReportInfoCardCount_Approval"]')
            .contains('3');
        cy.get('[data-cy="selfReportInfoCardCount_HonorSystem"]')
            .contains('2');
    });

    it('do not display approval table if no approval configured', () => {
        cy.createSkill(1, 1, 4, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReportInfoCardCount_Disabled"]')
            .contains('1');
        cy.get('[data-cy="selfReportInfoCardCount_Approval"]')
            .contains('0');
        cy.get('[data-cy="selfReportInfoCardCount_HonorSystem"]')
            .contains('2');

        cy.get('[data-cy="noApprovalTableMsg"]')
            .contains('No Skills Require Approval');
        cy.get('[data-cy="skillsReportApprovalTable"]')
            .should('not.exist');
    });

    it('filter users by id', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('featureSupported');
        cy.intercept('GET', '/admin/projects/proj1/approvals?*').as('loadApprovals');
        cy.intercept('GET', '/admin/projects/proj1/approvals/history?*').as('loadApprovalHistory');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@featureSupported');
        cy.wait('@loadApprovalHistory');
        cy.wait('@loadApprovals');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';

        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user0'
            }],
            [{
                colIndex: 2,
                value: 'user1'
            }],
            [{
                colIndex: 2,
                value: 'user2'
            }],
            [{
                colIndex: 2,
                value: 'user3'
            }],
            [{
                colIndex: 2,
                value: 'user4'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="selfReportApproval-userIdFilter"]').type('user2');
        cy.get('[data-cy="selfReportApproval-filterBtn"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user2'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="selfReportApproval-resetBtn"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user0'
            }],
            [{
                colIndex: 2,
                value: 'user1'
            }],
            [{
                colIndex: 2,
                value: 'user2'
            }],
            [{
                colIndex: 2,
                value: 'user3'
            }],
            [{
                colIndex: 2,
                value: 'user4'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="selfReportApproval-userIdFilter"]').type('user6');
        cy.get('[data-cy="selfReportApproval-filterBtn"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'user6'
            }],
        ], 5, true, null, false);
    });

    it('filter skills by name', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('featureSupported');
        cy.intercept('GET', '/admin/projects/proj1/approvals?*').as('loadApprovals');
        cy.intercept('GET', '/admin/projects/proj1/approvals/history?*').as('loadApprovalHistory');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@featureSupported');
        cy.wait('@loadApprovalHistory');
        cy.wait('@loadApprovals');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="selfReportApproval-skillNameFilter"]').type('Very Great Skill 1');
        cy.get('[data-cy="selfReportApproval-filterBtn"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="selfReportApproval-resetBtn"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="selfReportApproval-skillNameFilter"]').type('Very Great Skill 3');
        cy.get('[data-cy="selfReportApproval-filterBtn"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            }],
        ], 5, true, null, false);
    });

    it('filter skills by name and user id', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
        cy.reportSkill(1, 2, 'user5', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user4', '2020-09-13 11:00');
        cy.reportSkill(1, 2, 'user3', '2020-09-14 11:00');
        cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
        cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
        cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('featureSupported');
        cy.intercept('GET', '/admin/projects/proj1/approvals?*').as('loadApprovals');
        cy.intercept('GET', '/admin/projects/proj1/approvals/history?*').as('loadApprovalHistory');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@featureSupported');
        cy.wait('@loadApprovalHistory');
        cy.wait('@loadApprovals');

        const tableSelector = '[data-cy="skillsReportApprovalTable"]';

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            },
            {
                colIndex: 2,
                value: 'user0'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            },
            {
                colIndex: 2,
                value: 'user1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            },
            {
                colIndex: 2,
                value: 'user2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            },
            {
                colIndex: 2,
                value: 'user3'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            },
            {
                colIndex: 2,
                value: 'user4'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="selfReportApproval-skillNameFilter"]').type('Very Great Skill 2');
        cy.get('[data-cy="selfReportApproval-userIdFilter"]').type('user6');
        cy.get('[data-cy="selfReportApproval-filterBtn"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            },
            {
                colIndex: 2,
                value: 'user6'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="selfReportApproval-resetBtn"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 1'
            },
            {
                colIndex: 2,
                value: 'user0'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 3'
            },
            {
                colIndex: 2,
                value: 'user1'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            },
            {
                colIndex: 2,
                value: 'user2'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            },
            {
                colIndex: 2,
                value: 'user3'
            }],
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            },
            {
                colIndex: 2,
                value: 'user4'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy="selfReportApproval-skillNameFilter"]').type('Very Great Skill 2');
        cy.get('[data-cy="selfReportApproval-userIdFilter"]').type('user4');
        cy.get('[data-cy="selfReportApproval-filterBtn"]').click()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill 2'
            },
            {
                colIndex: 2,
                value: 'user4'
            }],
        ], 5, true, null, false);
    });

    it('show all requests with assigned approvers', () => {
        cy.createSkill(1, 1, 1, {selfReportingType: 'Approval'});
        cy.createSkill(1, 1, 2, {selfReportingType: 'Approval'});
        cy.createSkill(1, 1, 3, {selfReportingType: 'Approval'});

        const createUsers = (alias, num) => {
            const res = [];
            for (let i = 0; i < num; i++) {
                const uName = `${alias}${i}@email.org`
                res.push(uName);
                cy.register(uName, 'password')
            }
            return res
        }

        const approvers = createUsers('approver', 8)
        const users = createUsers('user', 3)
        cy.loginAsRootUser()
        cy.request('POST', `/root/users/${users[0]}/tags/org`, { tags: ['ABCDE'] });
        cy.loginAsAdminUser()
        approvers.forEach((a) => {
            cy.request('POST', `/admin/projects/proj1/users/${a}/roles/ROLE_PROJECT_APPROVER`);
        })

        cy.configureApproverForSkillId(1, approvers[0], 1)
        cy.configureApproverForUser(1, approvers[0], users[0])
        cy.configureApproverForUserTag(1, approvers[0], 'Org', 'ABC')

        cy.configureApproverForSkillId(1, approvers[1], 1)
        cy.configureApproverForSkillId(1, approvers[1], 2)

        cy.configureApproverForSkillId(1, approvers[2], 1)

        cy.reportSkill(1, 2, users[2], '2020-09-16 11:00');
        cy.reportSkill(1, 3, users[1], '2020-09-17 11:00');
        cy.reportSkill(1, 1, users[0], '2020-09-18 11:00');

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('featureSupported');
        cy.intercept('GET', '/admin/projects/proj1/approvals?*').as('loadApprovals');
        cy.intercept('GET', '/admin/projects/proj1/approvals/history?*').as('loadApprovalHistory');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@featureSupported');
        cy.wait('@loadApprovalHistory');
        cy.wait('@loadApprovals');
        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-pc-section="columnheadercontent"]').should('not.contain', 'Assigned Approvers')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="userId"]').should('have.text', 'user1@email.org')

        cy.get('[data-cy="requestOptionSelect"] [data-pc-name="pctogglebutton"][aria-pressed="false"]').click()
        cy.wait('@loadApprovals');
        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '3')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-pc-section="columnheadercontent"]').contains( 'Assigned Approvers')

        // row 1
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="userId"]').should('have.text', 'user0@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="assignedApprovers"] [data-cy="approver-0"] [data-cy="approverId"]').should('have.text', 'approver0@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="assignedApprovers"] [data-cy="approver-0"] [data-cy="approverConfTypes"]').should('have.text', 'Org, Skill, User')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="assignedApprovers"] [data-cy="approver-1"] [data-cy="approverId"]').should('have.text', 'approver1@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="assignedApprovers"] [data-cy="approver-1"] [data-cy="approverConfTypes"]').should('have.text', 'Skill')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="assignedApprovers"] [data-cy="approver-2"] [data-cy="approverId"]').should('have.text', 'approver2@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="assignedApprovers"] [data-cy="approver-2"] [data-cy="approverConfTypes"]').should('have.text', 'Skill')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="assignedApprovers"] [data-cy="approver-3"]').should('not.exist')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="assignedApprovers"] [data-cy="expandOrCollapse"]').should('not.exist')

        // row 2
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="userId"]').should('have.text', 'user1@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-0"] [data-cy="approverId"]').should('have.text', 'approver3@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-0"] [data-cy="approverConfTypes"]').should('have.text', 'Fallback')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-1"] [data-cy="approverId"]').should('have.text', 'approver4@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-1"] [data-cy="approverConfTypes"]').should('have.text', 'Fallback')

        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-2"]').should('not.exist')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="expandOrCollapse"]').contains('View 4 More')

        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="expandOrCollapse"]').click()
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="expandOrCollapse"]').contains('Show Less')

        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-2"] [data-cy="approverId"]').should('have.text', 'approver5@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-2"] [data-cy="approverConfTypes"]').should('have.text', 'Fallback')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-3"] [data-cy="approverId"]').should('have.text', 'approver6@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-3"] [data-cy="approverConfTypes"]').should('have.text', 'Fallback')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-4"] [data-cy="approverId"]').should('have.text', 'approver7@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-4"] [data-cy="approverConfTypes"]').should('have.text', 'Fallback')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-5"] [data-cy="approverId"]').should('have.text', 'skills@skills.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-5"] [data-cy="approverConfTypes"]').should('have.text', 'Fallback')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-6"]').should('not.exist')

        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="expandOrCollapse"]').click()
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="expandOrCollapse"]').contains('View 4 More')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-2"]').should('not.exist')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-3"]').should('not.exist')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-4"]').should('not.exist')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-cy="assignedApprovers"] [data-cy="approver-5"]').should('not.exist')

        // row 3
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="2"] [data-cy="userId"]').should('have.text', 'user2@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="2"] [data-cy="assignedApprovers"] [data-cy="approver-0"] [data-cy="approverId"]').should('have.text', 'approver1@email.org')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="2"] [data-cy="assignedApprovers"] [data-cy="approver-0"] [data-cy="approverConfTypes"]').should('have.text', 'Skill')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="2"] [data-cy="assignedApprovers"] [data-cy="approver-1"]').should('not.exist')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="2"] [data-cy="assignedApprovers"] [data-cy="expandOrCollapse"]').should('not.exist')
    })

    it('all requests select button is not shown when approval workload is not configured', () => {
        cy.createSkill(1, 1, 1, {selfReportingType: 'Approval'});
        cy.createSkill(1, 1, 2, {selfReportingType: 'Approval'});
        cy.createSkill(1, 1, 3, {selfReportingType: 'Approval'});

        cy.reportSkill(1, 2, 'user1', '2020-09-16 11:00');

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('featureSupported');
        cy.intercept('GET', '/admin/projects/proj1/approvals?*').as('loadApprovals');
        cy.intercept('GET', '/admin/projects/proj1/approvals/history?*').as('loadApprovalHistory');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@featureSupported');
        cy.wait('@loadApprovalHistory');
        cy.wait('@loadApprovals');
        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-pc-section="columnheadercontent"]').should('not.contain', 'Assigned Approvers')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="userId"]').should('have.text', 'user1')
        cy.get('[data-cy="requestOptionSelect"]').should('not.exist')
    })

    it('all requests select button is not shown for the approver role', () => {
        cy.createSkill(1, 1, 1, {selfReportingType: 'Approval'});
        cy.createSkill(1, 1, 2, {selfReportingType: 'Approval'});
        cy.createSkill(1, 1, 3, {selfReportingType: 'Approval'});

        cy.reportSkill(1, 1, 'user1', '2020-09-16 11:00');

        const approverId = 'approver@skills.org'
        cy.register(approverId, 'password', false)
        cy.loginAsAdminUser()
        cy.request('POST', `/admin/projects/proj1/users/${approverId}/roles/ROLE_PROJECT_APPROVER`);
        cy.configureApproverForSkillId(1, approverId, 1)

        cy.logout()
        cy.login(approverId)

        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice').as('featureSupported');
        cy.intercept('GET', '/admin/projects/proj1/approvals?*').as('loadApprovals');
        cy.intercept('GET', '/admin/projects/proj1/approvals/history?*').as('loadApprovalHistory');
        cy.visit('/administrator/projects/proj1/self-report');
        cy.wait('@featureSupported');
        cy.wait('@loadApprovalHistory');
        cy.wait('@loadApprovals');
        cy.get('[data-cy="skillsReportApprovalTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-pc-section="columnheadercontent"]').should('not.contain', 'Assigned Approvers')
        cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="0"] [data-cy="userId"]').should('have.text', 'user1')
        cy.get('[data-cy="requestOptionSelect"]').should('not.exist')
    })
});

