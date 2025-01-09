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

  it('approve one', () => {
    cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
    cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
    cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
    cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

    cy.visit('/administrator/projects/proj1/self-report');

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
    ]);

    cy.get('[data-cy="approveBtn"]')
      .should('be.disabled');
    cy.get('[data-cy="rejectBtn"]')
      .should('be.disabled');
    cy.get('[data-p-index="1"] [data-pc-name="rowcheckbox"]').click()
    cy.get('[data-cy="approveBtn"]')
      .should('be.enabled');
    cy.get('[data-cy="rejectBtn"]')
      .should('be.enabled');

    cy.get('[data-cy="approveBtn"]')
      .click();
    cy.get('[data-cy="approvalTitle"]')
        .contains('This will approve user\'s request(s) to get points');
    cy.get('[data-cy="approvalInputMsg"]')
        .type('Approval message!');
    cy.get('[data-cy="saveDialogBtn"]')
        .click();

    cy.validateTable(tableSelector, [
      [{
        colIndex: 2,
        value: 'user0'
      }],
      [{
        colIndex: 2,
        value: 'user2'
      }],
    ]);

    cy.visit('/administrator/projects/proj1/users/user1/skillEvents');
    cy.validateTable('[data-cy="performedSkillsTable"]', [
      [{
        colIndex: 1,
        value: 'skill3'
      }],
    ]);
  });

  it('reject one', () => {
    cy.intercept('POST', '/admin/projects/proj1/approvals/reject', (req) => {
      expect(req.body.rejectionMessage)
        .to
        .include('Rejection message!');
    })
      .as('reject');

    cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
    cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
    cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
    cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

    cy.visit('/administrator/projects/proj1/self-report');

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
    ]);

    cy.get('[data-cy="approveBtn"]')
      .should('be.disabled');
    cy.get('[data-cy="rejectBtn"]')
      .should('be.disabled');
    cy.get('[data-p-index="1"] [data-pc-name="rowcheckbox"]').click()
    cy.get('[data-cy="approveBtn"]')
      .should('be.enabled');
    cy.get('[data-cy="rejectBtn"]')
      .should('be.enabled');

    cy.get('[data-cy="rejectBtn"]')
      .click();
    cy.get('[data-cy="rejectionTitle"]')
      .contains('This will reject user\'s request(s) to get points');
    cy.get('[data-cy="rejectionInputMsg"]')
      .type('Rejection message!');
    cy.get('[data-cy="saveDialogBtn"]')
      .click();

    cy.wait('@reject');

    cy.validateTable(tableSelector, [
      [{
        colIndex: 2,
        value: 'user0'
      }],
      [{
        colIndex: 2,
        value: 'user2'
      }],
    ]);

    cy.visit('/administrator/projects/proj1/users/user1/skillEvents');
    cy.get('[data-cy="performedSkillsTable"] tbody tr')
      .should('have.length', 0);
  });

  it('custom validation for rejection message', () => {
    cy.intercept('POST', '/admin/projects/proj1/approvals/reject', (req) => {
      expect(req.body.rejectionMessage)
        .to
        .include('Rejection jabberwoc');
    })
      .as('reject');

    cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
    cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
    cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
    cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

    cy.visit('/administrator/projects/proj1/self-report');

    cy.get('[data-p-index="1"] [data-pc-name="rowcheckbox"]').click()
    cy.get('[data-cy="rejectBtn"]')
      .click();
    cy.get('[data-cy="rejectionTitle"]')
      .contains('This will reject user\'s request(s) to get points');

    cy.get('[data-cy="approvalRequiredMsgError"]')
      .should('not.be.visible');
    cy.get('[data-cy="saveDialogBtn"]')
      .should('be.enabled');

    cy.get('[data-cy="rejectionInputMsg"]')
      .type('Rejection jabber');
    cy.get('[data-cy="approvalRequiredMsgError"]')
      .should('not.be.visible');
    cy.get('[data-cy="saveDialogBtn"]')
      .should('be.enabled');

    cy.get('[data-cy="rejectionInputMsg"]')
      .type('wock');
    cy.get('[data-cy="approvalRequiredMsgError"]')
      .should('not.be.visible');
    cy.get('[data-cy="saveDialogBtn"]')
      .should('be.enabled');

    cy.get('[data-cy="rejectionInputMsg"]')
      .type('y');
    cy.get('[data-cy="approvalRequiredMsgError"]')
      .contains('Rejection Message - paragraphs may not contain jabberwocky');
    cy.get('[data-cy="saveDialogBtn"]')
      .should('be.disabled');

    cy.get('[data-cy="rejectionInputMsg"]')
      .type(' ok');
    cy.get('[data-cy="approvalRequiredMsgError"]')
      .contains('Rejection Message - paragraphs may not contain jabberwocky');
    cy.get('[data-cy="saveDialogBtn"]')
      .should('be.disabled');

    cy.get('[data-cy="rejectionInputMsg"]')
      .type('{backspace}{backspace}{backspace}');
    cy.get('[data-cy="approvalRequiredMsgError"]')
      .contains('Rejection Message - paragraphs may not contain jabberwocky');
    cy.get('[data-cy="saveDialogBtn"]')
      .should('be.disabled');

    cy.get('[data-cy="rejectionInputMsg"]')
      .type('{backspace}{backspace}');
    cy.get('[data-cy="approvalRequiredMsgError"]')
      .contains('Rejection Message - paragraphs may not contain jabberwocky')
      .should('not.exist');
    cy.get('[data-cy="saveDialogBtn"]')
      .should('be.enabled');

    cy.get('[data-cy="saveDialogBtn"]')
      .click();
    cy.wait('@reject');
  });

  it('approve 1 page worth of records', () => {
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
    cy.get(`${tableSelector} [data-pc-name="headercheckbox"] [data-pc-section="input"]`).click();
    cy.get('[data-cy="approveBtn"]')
      .click();
    cy.get('[data-cy="saveDialogBtn"]').click();

    cy.validateTable(tableSelector, [
      [{
        colIndex: 2,
        value: 'user5'
      }],
      [{
        colIndex: 2,
        value: 'user6'
      }],
    ]);

    cy.get('[data-cy="approveBtn"]')
      .should('be.disabled');
    cy.get('[data-cy="rejectBtn"]')
      .should('be.disabled');

    cy.visit('/administrator/projects/proj1/users');
    cy.validateTable('[data-cy="usersTable"]', [
      [{
        colIndex: 1,
        value: 'user0'
      }, {
        colIndex: 3,
        value: '100'
      }],
      [{
        colIndex: 1,
        value: 'user1'
      }, {
        colIndex: 3,
        value: '100'
      }],
      [{
        colIndex: 1,
        value: 'user2'
      }, {
        colIndex: 3,
        value: '100'
      }],
      [{
        colIndex: 1,
        value: 'user3'
      }, {
        colIndex: 3,
        value: '100'
      }],
      [{
        colIndex: 1,
        value: 'user4'
      }, {
        colIndex: 3,
        value: '100'
      }],
    ]);
  });

  it('reject 1 page worth of records', () => {
    cy.intercept({
      method: 'POST',
      url: '/admin/projects/proj1/approvals/reject',
    })
      .as('reject');

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
    cy.get(`${tableSelector} [data-pc-name="headercheckbox"] [data-pc-section="input"]`).click();

    cy.get('[data-cy="rejectBtn"]')
      .click();
    cy.get('[data-cy="rejectionTitle"]')
      .contains('This will reject user\'s request(s) to get points');
    cy.get('[data-cy="rejectionInputMsg"]')
      .type('Rejection message!');
    cy.get('[data-cy="saveDialogBtn"]')
      .click();


    cy.validateTable(tableSelector, [
      [{
        colIndex: 2,
        value: 'user5'
      }],
      [{
        colIndex: 2,
        value: 'user6'
      }],
    ]);

    cy.get('[data-cy="approveBtn"]')
      .should('be.disabled');
    cy.get('[data-cy="rejectBtn"]')
      .should('be.disabled');

    cy.visit('/administrator/projects/proj1/users');
    cy.get('[data-cy="usersTable"] tbody tr')
      .should('have.length', 0);
  });

  it('rejection message is limited to configure max size', () => {
    cy.createSkill(1, 1, 1, {
      selfReportingType: 'Approval',
      description: 'This is skill 1'
    });
    cy.createSkill(1, 1, 2, {
      selfReportingType: 'Approval',
      description: 'very cool skill 2'
    });
    cy.createSkill(1, 1, 3, {
      selfReportingType: 'Approval',
      description: 'last but not least'
    });
    const msgNoExpandBtn = new Array(60).join('A');
    cy.doReportSkill({
      project: 1,
      skill: 2,
      userId: 'user6',
      date: '2020-09-11 11:00',
      approvalRequestedMsg: msgNoExpandBtn
    });

    cy.intercept('/admin/projects/proj1/approvals*')
      .as('loadApprovals');
    cy.visit('/administrator/projects/proj1/self-report');
    cy.wait('@loadApprovals');
    const tableSelector = '[data-cy="skillsReportApprovalTable"]';
    cy.get(`${tableSelector} [data-pc-name="headercheckbox"] [data-pc-section="input"]`).click();
    cy.get('[data-cy=rejectBtn]')
      .click();
    cy.contains('Reject Skills');
    cy.get('[data-cy=rejectionInputMsg]')
      .fill(new Array(500).join('A'));
    cy.get('[data-cy=approvalRequiredMsgError]')
      .contains('Rejection Message must be at most 250 characters')
      .should('be.visible');
    cy.get('[data-cy=saveDialogBtn]')
      .should('be.disabled');
    cy.get('[data-cy=rejectionInputMsg]')
      .clear();
    cy.get('[data-cy=saveDialogBtn]')
      .should('be.enabled');
    cy.get('[data-cy=approvalRequiredMsgError]')
      .should('not.be.visible');
    cy.get('[data-cy=rejectionInputMsg]')
      .type(new Array(50).join('B'));
    cy.get('[data-cy=saveDialogBtn]')
      .should('be.enabled');
    cy.get('[data-cy=approvalRequiredMsgError]')
      .should('not.be.visible');
  });

  it('approval request skill should be a link to skill details', () => {
    cy.createSkill(1, 1, 1, {
      selfReportingType: 'Approval',
      description: 'This is skill 1'
    });
    cy.createSkill(1, 1, 2, {
      selfReportingType: 'Approval',
      description: 'very cool skill 2'
    });
    cy.createSkill(1, 1, 3, {
      selfReportingType: 'Approval',
      description: 'last but not least'
    });
    cy.reportSkill(1, 2, 'user6', '2020-09-11 11:00');
    cy.reportSkill(1, 1, 'user5', '2020-09-12 11:00');
    cy.reportSkill(1, 3, 'user4', '2020-09-13 11:00');

    cy.visit('/administrator/projects/proj1/self-report');

    cy.get('[data-cy="viewSkillLink_skill1"]')
      .should('have.attr', 'href', '/administrator/projects/proj1/subjects/subj1/skills/skill1');
    cy.get('[data-cy="viewSkillLink_skill2"]')
      .should('have.attr', 'href', '/administrator/projects/proj1/subjects/subj1/skills/skill2');
    cy.get('[data-cy="viewSkillLink_skill3"]')
      .should('have.attr', 'href', '/administrator/projects/proj1/subjects/subj1/skills/skill3');
  });

  it('show how many points are requested', () => {
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

    cy.visit('/administrator/projects/proj1/self-report');

    const tableSelector = '[data-cy="skillsReportApprovalTable"]';
    const expected = [
      [{
        colIndex: 2,
        value: 'user3Good@skills.org'
      }, {
        colIndex: 1,
        value: '180'
      }],
      [{
        colIndex: 2,
        value: 'user2Good@skills.org'
      }, {
        colIndex: 1,
        value: '220'
      }],
      [{
        colIndex: 2,
        value: 'user1Good@skills.org'
      }, {
        colIndex: 1,
        value: '100'
      }],
    ];
    cy.validateTable(tableSelector, expected);
  });
});