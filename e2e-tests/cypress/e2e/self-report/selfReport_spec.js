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

describe('Self Report Skills Management Tests', () => {

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
        cy.get('[data-cy="skillsReportApprovalTable"] [data-pc-name="rowperpagedropdown"]')
          .click().get('[data-pc-section="item"]').contains('10').click();
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

    it('warn if email service is not configured', () => {
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');

        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="emailServiceWarning"]')
            .contains('Please note that email notifications are currently disabled');
    });

    it('email service warning should NOT be displayed if there 0 Approval required Self Reporting skills', () => {
        cy.createSkill(1, 1, 4, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReport_emailServiceWarning"]')
            .should('not.exist');
    });

    it('no email warning when email service is configured', () => {
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 6);

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });
        cy.request({
            method: 'POST',
            url: '/root/saveEmailSettings',
            body: {
                host: 'localhost',
                port: 1025,
                'protocol': 'smtp'
            },
        });

        cy.request({
            method: 'POST',
            url: '/root/saveSystemSettings',
            body: {
                publicUrl: 'http://localhost:8082/',
                resetTokenExpiration: 'PT2H',
                fromEmail: 'noreploy@skilltreeemail.org',
            }
        });
        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.defaultUser, vars.defaultPass);
            });

        cy.visit('/administrator/projects/proj1/self-report');

        cy.get('[data-cy="selfReport_emailServiceWarning"]')
            .should('not.exist');
    });

    it('should be able to unsubscribe from approval request emails', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            pointIncrement: '220'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            pointIncrement: '180'
        });
        cy.reportSkill(1, 1, 'user1Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user3Good@skills.org', '2020-09-14 11:00');
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.reportSkill(2, 1, 'user1Good@skills.org', '2020-09-12 11:00');

        cy.intercept('GET', '/admin/projects/**/approvalEmails/isSubscribed')
            .as('isSubscribed');
        cy.intercept('POST', '/admin/projects/proj1/approvalEmails/unsubscribe')
            .as('unsubscribe');
        cy.intercept('POST', '/admin/projects/proj1/approvalEmails/subscribe')
            .as('subscribe');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="btn_edit-project"]')
        cy.get('[data-cy="viewSkillLink_skill3"]')
        cy.wait('@isSubscribed');
        cy.contains('Subscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch] input')
            .should('be.checked');
        cy.get('[data-cy=unsubscribeSwitch]')
            .click();
        cy.wait('@unsubscribe');
        cy.contains('Unsubscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch]')
            .should('not.be.checked');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="btn_edit-project"]')
        cy.get('[data-cy="viewSkillLink_skill3"]')
        cy.wait('@isSubscribed');
        cy.contains('Unsubscribed')
          .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch] input')
          .should('not.be.checked');

        //setting should be per project
        cy.visit('/administrator/projects/proj2/self-report');
        cy.get('[data-cy="btn_edit-project"]')
        cy.get('[data-cy="viewSkillLink_skill1"]')
        cy.wait('@isSubscribed');
        cy.contains('Subscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch] input')
            .should('be.checked');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy="btn_edit-project"]')
        cy.get('[data-cy="viewSkillLink_skill3"]')
        cy.wait('@isSubscribed');
        cy.contains('Unsubscribed')
            .should('be.visible');
        cy.get('[data-cy=unsubscribeSwitch] input')
            .should('not.be.checked');
    });

    it('approval request email subscription toggle should not be visible if email is not configured', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            pointIncrement: '100'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            pointIncrement: '220'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            pointIncrement: '180'
        });
        cy.reportSkill(1, 1, 'user1Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 2, 'user2Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 3, 'user3Good@skills.org', '2020-09-14 11:00');
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');

        cy.visit('/administrator/projects/proj1/self-report');
        cy.get('[data-cy=unsubscribeContainer]')
            .should('not.exist');
    });

});

