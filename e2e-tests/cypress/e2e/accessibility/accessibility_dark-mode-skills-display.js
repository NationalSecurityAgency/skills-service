/*
 * Copyright 2024 SkillTree
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


describe('Dark Mode Accessibility Tests for Progress and Rankings pages', () => {

  beforeEach(() => {
    cy.request('POST', '/app/userInfo/settings', [{
      'settingGroup': 'user.prefs',
      'value': true,
      'setting': 'enable_dark_mode',
      'lastLoadedValue': 'true',
      'dirty': true
    }]);
    cy.createProject(1)
  });

  it('subject page with skills of various achievements', () => {
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem'})
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4, { selfReportingType: 'Approval'})
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

    cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1')
    cy.injectAxe();
    cy.get('[data-cy="skillProgressTitle-skill1"]')
    cy.get('[data-cy="pointHistoryChartWithData"]')

    cy.customLighthouse();
    cy.customA11y();
  })


  it('achieved skill page', () => {
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1)
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');

    cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
    cy.injectAxe();
    cy.get('[data-cy="skillProgressTitle-skill1"]')

    cy.customLighthouse();
    cy.customA11y();
  })


  it('request approval-based skill page with justification expanded', () => {
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, { selfReportingType: 'Approval'})

    cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
    cy.injectAxe();
    cy.get('[data-cy="skillProgressTitle-skill1"]')
    cy.get('[data-cy="requestApprovalBtn"]').click()
    cy.get('[data-cy="selfReportSubmitBtn"]').should('be.enabled')

    cy.customLighthouse();
    cy.customA11y();
  })

  it('skill with prerequisites', () => {
    cy.createSubject(1,1)
    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);

    cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj1/skill2`);
    cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj1/skill3`);

    cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
    cy.injectAxe();
    cy.get('[data-cy="skillProgressTitle-skill1"]')
    cy.wait(5000)

    cy.customLighthouse();
    cy.customA11y();
  })



})

