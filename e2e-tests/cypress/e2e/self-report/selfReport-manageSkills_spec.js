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

  it('manage self reporting settings at project level', () => {
    cy.visit('/administrator/projects/proj1/settings');

    cy.get('[data-cy="selfReportSwitch"]')
      .should('not.be.checked');
    cy.get('[data-cy="saveSettingsBtn"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportSwitch"]').click();
    cy.get('[data-cy="selfReportSwitch"] input')
      .should('be.checked');

    cy.get('[data-cy="saveSettingsBtn"]')
      .should('be.enabled');
    cy.get('[data-cy="unsavedChangesAlert"]')
      .contains('Unsaved Changes');

    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');

    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .click();
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.checked');

    cy.get('[data-cy="saveSettingsBtn"]')
      .should('be.enabled');
    cy.get('[data-cy="unsavedChangesAlert"]')
      .contains('Unsaved Changes');

    cy.get('[data-cy="saveSettingsBtn"]')
      .click();
    cy.get('[data-cy="settingsSavedAlert"]')
      .contains('Settings Updated');
    cy.get('[data-cy="unsavedChangesAlert"]')
      .should('not.exist');
    cy.get('[data-cy="saveSettingsBtn"]')
      .should('be.disabled');

    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').click()
    cy.get('[data-cy="justificationRequiredCheckbox"] input')
      .should('not.be.checked');

    cy.get('[data-cy="justificationRequiredCheckbox"]')
      .click();
    cy.get('[data-cy="justificationRequiredCheckbox"] input')
      .should('be.checked');

    cy.get('[data-cy="saveSettingsBtn"]')
      .should('be.enabled');
    cy.get('[data-cy="unsavedChangesAlert"]')
      .contains('Unsaved Changes');

    cy.get('[data-cy="saveSettingsBtn"]')
      .click();
    cy.get('[data-cy="settingsSavedAlert"]')
      .contains('Settings Updated');
    cy.get('[data-cy="unsavedChangesAlert"]')
      .should('not.exist');
    cy.get('[data-cy="saveSettingsBtn"]')
      .should('be.disabled');

    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').click()
    cy.get('[data-cy="saveSettingsBtn"]')
      .click();
    cy.get('[data-cy="settingsSavedAlert"]')
      .contains('Settings Updated');

    // refresh and check that the values persisted
    cy.visit('/administrator/projects/proj1/settings');
    cy.get('[data-cy="selfReportSwitch"] input')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.checked');
    cy.get('[data-cy="unsavedChangesAlert"]')
      .should('not.exist');
    cy.get('[data-cy="settingsSavedAlert"]')
      .should('not.exist');

    // disable skill, refresh and validate
    cy.get('[data-cy="selfReportSwitch"]').click();
    cy.get('[data-cy="unsavedChangesAlert"]')
      .contains('Unsaved Changes');
    cy.get('[data-cy="settingsSavedAlert"]')
      .should('not.exist');
    cy.get('[data-cy="saveSettingsBtn"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.disabled');
    cy.get('[data-cy="saveSettingsBtn"]')
      .click();

    // cy.visit('/administrator/projects/proj1/settings');
    // cy.get('[data-cy="selfReportSwitch"]')
    //     .should('not.be.checked');
    // cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
    //     .should('be.disabled');
    // cy.get('[data-cy="justificationRequiredCheckbox"] input')
    //     .should('be.disabled');
    // cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
    //     .should('be.disabled');
    // cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
    //     .should('be.checked');
    // cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
    //     .should('not.be.checked');
    // cy.get('[data-cy="justificationRequiredCheckbox"] input')
    //     .should('be.checked');
    // cy.get('[data-cy="unsavedChangesAlert"]')
    //     .should('not.exist');
    // cy.get('[data-cy="settingsSavedAlert"]')
    //     .should('not.exist');
    // cy.get('[data-cy="saveSettingsBtn"]')
    //     .should('be.disabled');
    //
    // // enable then disable should disable save button
    // cy.get('[data-cy="selfReportSwitch"]').click();
    // cy.get('[data-cy="unsavedChangesAlert"]')
    //     .contains('Unsaved Changes');
    // cy.get('[data-cy="saveSettingsBtn"]')
    //     .should('be.enabled');
    // cy.get('[data-cy="selfReportSwitch"]')
    //     .click();
    // cy.get('[data-cy="unsavedChangesAlert"]')
    //     .should('not.exist');
    // cy.get('[data-cy="saveSettingsBtn"]')
    //     .should('be.disabled');
    //
    // // enabled and save
    // cy.get('[data-cy="selfReportSwitch"]').click();
    // cy.get('[data-cy="saveSettingsBtn"]')
    //     .click();
    // cy.get('[data-cy="settingsSavedAlert"]')
    //     .contains('Settings Updated');
    //
    // cy.get('[data-cy="unsavedChangesAlert"]')
    //     .should('not.exist');
    // cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
    //     .click();
    // cy.get('[data-cy="unsavedChangesAlert"]')
    //     .contains('Unsaved Changes');
    // cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
    //     .click();
    // cy.get('[data-cy="unsavedChangesAlert"]')
    //     .should('not.exist');
    //
    // cy.get('[data-cy="selfReportSwitch"]').click();
    // cy.get('[data-cy="unsavedChangesAlert"]')
    //     .contains('Unsaved Changes');
    // cy.get('[data-cy="selfReportSwitch"]').click();
    // cy.get('[data-cy="unsavedChangesAlert"]')
    //     .should('not.exist');
  });

  it('create skills - self reporting disabled - no project level default', () => {
    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.get('[data-cy="newSkillButton"]')
      .click();
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');

    cy.get('[data-cy=skillName]')
      .type('skill1');
    cy.get('[data-cy="saveDialogBtn"]').click()
    cy.get('[data-cy="editSkillButton_skill1Skill"]')
      .click();
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');
  });

  it('create skills - self reporting with approval - no project level default', () => {
    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.get('[data-cy="newSkillButton"]')
      .click();
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');

    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');

    cy.get('[data-cy=skillName]')
      .type('skill1');
    cy.get('[data-cy="saveDialogBtn"]').click()
    cy.get('[data-cy="editSkillButton_skill1Skill"]')
      .click();
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');
  });

  it('create skills - self reporting with Honor System - no project level default', () => {
    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.get('[data-cy="newSkillButton"]')
      .click();
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.disabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');

    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');

    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .click();
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.checked');

    cy.get('[data-cy=skillName]')
      .type('skill1');
    cy.get('[data-cy="saveDialogBtn"]').click()
    cy.get('[data-cy="editSkillButton_skill1Skill"]')
      .click();
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.checked');
  });

  it('create skill - project level default of Honor System', () => {
    cy.visit('/administrator/projects/proj1/settings');
    cy.get('[data-cy="selfReportSwitch"]')
      .click();
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .click();
    cy.get('[data-cy="saveSettingsBtn"]')
      .click();
    cy.get('[data-cy="settingsSavedAlert"]')
      .contains('Settings Updated');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.get('[data-cy="newSkillButton"]')
      .click();
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('not.be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.checked');
  });

  it('create skill - project level default of Approval', () => {
    cy.visit('/administrator/projects/proj1/settings');
    cy.get('[data-cy="selfReportSwitch"]')
      .click();
    cy.get('[data-cy="saveSettingsBtn"]')
      .click();
    cy.get('[data-cy="settingsSavedAlert"]')
      .contains('Settings Updated');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.get('[data-cy="newSkillButton"]')
      .click();
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.enabled');
    cy.get('[data-cy="justificationRequiredCheckbox"] input')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');
    cy.get('[data-cy="justificationRequiredCheckbox"] input')
      .should('not.be.checked');
  });

  it('create skill - project level default of Approval and Require Justification', () => {
    cy.visit('/administrator/projects/proj1/settings');
    cy.get('[data-cy="selfReportSwitch"]')
      .click();
    cy.get('[data-cy="justificationRequiredCheckbox"]')
      .click();
    cy.get('[data-cy="saveSettingsBtn"]')
      .click();
    cy.get('[data-cy="settingsSavedAlert"]')
      .contains('Settings Updated');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.get('[data-cy="newSkillButton"]')
      .click();
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('be.enabled');
    cy.get('[data-cy="justificationRequiredCheckbox"] input')
      .should('be.enabled');
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .should('be.checked');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .should('not.be.checked');
    cy.get('[data-cy="justificationRequiredCheckbox"] input')
      .should('be.checked');
  });

  it('edit skills - approval -> warnings', () => {
    cy.createSkill(1, 1, 1, {
      selfReportingType: 'Approval',
      name: 'Approval 1'
    });
    cy.createSkill(1, 1, 2, {
      selfReportingType: 'Approval',
      name: 'Approval 2'
    });
    cy.createSkill(1, 1, 3, {
      selfReportingType: 'Approval',
      name: 'Approval 3'
    });
    cy.createSkill(1, 1, 4, {
      selfReportingType: 'HonorSystem',
      name: 'Honor System 1'
    });
    cy.createSkill(1, 1, 5, { name: 'Disabled 1' });
    cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
    cy.reportSkill(1, 1, 'user5Good@skills.org', '2020-09-13 11:00');
    cy.reportSkill(1, 1, 'user4Good@skills.org', '2020-09-14 11:00');
    cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
    cy.rejectRequest(3);

    cy.visit('/administrator/projects/proj1/subjects/subj1');

    // approval -> honor
    cy.get('[data-cy="editSkillButton_skill1"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .click();

    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Switching this skill to the Honor System will automatically:');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Approve 3 pending requests');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Remove 1 rejected request');

    // honor -> disabled
    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Disabling Self Reporting will automatically:');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Remove 3 pending requests');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Remove 1 rejected request');

    // disabled -> honor
    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Switching this skill to the Honor System will automatically:');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Approve 3 pending requests');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Remove 1 rejected request');

    // honor -> approval
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');

    // approval -> disable
    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Disabling Self Reporting will automatically:');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Remove 3 pending requests');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Remove 1 rejected request');

    // disable -> approval
    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');
  });

  it('edit skills - approval -> no pending requests then no warning', () => {
    cy.createSkill(1, 1, 1, {
      selfReportingType: 'Approval',
      name: 'Approval 1'
    });
    cy.createSkill(1, 1, 2, {
      selfReportingType: 'Approval',
      name: 'Approval 2'
    });
    cy.createSkill(1, 1, 3, {
      selfReportingType: 'Approval',
      name: 'Approval 3'
    });
    cy.createSkill(1, 1, 4, {
      selfReportingType: 'HonorSystem',
      name: 'Honor System 1'
    });
    cy.createSkill(1, 1, 5, { name: 'Disabled 1' });
    cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
    cy.reportSkill(1, 1, 'user5Good@skills.org', '2020-09-13 11:00');
    cy.reportSkill(1, 1, 'user4Good@skills.org', '2020-09-14 11:00');
    cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
    cy.rejectRequest(3);

    cy.visit('/administrator/projects/proj1/subjects/subj1');

    // approval -> honor
    cy.get('[data-cy="editSkillButton_skill2"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');

    // honor -> disabled
    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');

    // disabled -> honor
    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');

    // honor -> approval
    cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');

    // approval -> disable
    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');

    // disable -> approval
    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');
  });

  it('edit skills - disable self reporting -> warnings -> save', () => {
    cy.createSkill(1, 1, 1, {
      selfReportingType: 'Approval',
      name: 'Approval 1'
    });
    cy.createSkill(1, 1, 2, {
      selfReportingType: 'Approval',
      name: 'Approval 2'
    });
    cy.createSkill(1, 1, 3, {
      selfReportingType: 'Approval',
      name: 'Approval 3'
    });
    cy.createSkill(1, 1, 4, {
      selfReportingType: 'HonorSystem',
      name: 'Honor System 1'
    });
    cy.createSkill(1, 1, 5, { name: 'Disabled 1' });
    cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
    cy.reportSkill(1, 1, 'user5Good@skills.org', '2020-09-13 11:00');
    cy.reportSkill(1, 1, 'user4Good@skills.org', '2020-09-14 11:00');
    cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
    cy.rejectRequest(3);

    cy.visit('/administrator/projects/proj1/subjects/subj1');

    // approval -> disabled
    cy.get('[data-cy="editSkillButton_skill1"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');

    // honor -> disabled
    cy.get('[data-cy="selfReportEnableCheckbox"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Disabling Self Reporting will automatically:');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Remove 3 pending requests');
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .contains('Remove 1 rejected request');

    cy.get('[data-cy="saveDialogBtn"]').click()
    cy.get('[data-cy="editSkillButton_skill1"]')
      .click();
    cy.get('[data-cy="selfReportingTypeWarning"]')
      .should('not.exist');
    cy.get('[data-cy="selfReportEnableCheckbox"] input')
      .should('not.be.checked');
  });

});