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

  it('project page with multiple subjects with various progress', () => {
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem'})
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4, { selfReportingType: 'Approval'})
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

    cy.createSubject(1, 2)

    cy.createSubject(1, 3)
    cy.createSkill(1, 3, 4, { selfReportingType: 'HonorSystem'})
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'yesterday' })
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'now' })

    cy.createBadge(1, 1);
    cy.assignSkillToBadge(1, 1, 1);
    cy.createBadge(1, 1, { enabled: true });

    cy.visit('/progress-and-rankings/projects/proj1')
    cy.injectAxe();
    cy.get('[data-cy="subjectTile-subj1"]')
    cy.get('[data-cy="pointHistoryChartWithData"]')
    cy.get('[data-cy="myRankBtn"]')

    cy.customLighthouse();
    cy.customA11y();
  })

  it('rank page', () => {
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4)
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

    cy.reportSkill(1, 1, 'user1', 'now');
    cy.reportSkill(1, 2, 'user1', 'now');

    cy.reportSkill(1, 1, 'user2', 'now');

    cy.reportSkill(1, 2, 'user3', 'now');
    cy.reportSkill(1, 3, 'user3', 'yesterday');
    cy.reportSkill(1, 3, 'user3', 'now');

    cy.createSubject(1, 2)

    cy.createSubject(1, 3)
    cy.createSkill(1, 3, 4)
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'yesterday' })
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'now' })


    cy.visit('/progress-and-rankings/projects/proj1/rank')
    cy.injectAxe();
    cy.get('[data-cy="levelBreakdownChart-animationEnded"]')

    cy.customLighthouse();
    cy.customA11y();
  })

  it('badges page', () => {
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem'})
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4, { selfReportingType: 'Approval'})
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

    cy.createSubject(1, 2)

    cy.createSubject(1, 3)
    cy.createSkill(1, 3, 4, { selfReportingType: 'HonorSystem'})
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'yesterday' })
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'now' })

    cy.createBadge(1, 1);
    cy.assignSkillToBadge(1, 1, 1);
    cy.createBadge(1, 1, { enabled: true });

    cy.createBadge(1, 2);
    cy.assignSkillToBadge(1, 2, 2);
    cy.createBadge(1, 2, { enabled: true });

    cy.createBadge(1, 3);
    cy.assignSkillToBadge(1, 3, 3);
    cy.createBadge(1, 3, { enabled: true });

    cy.visit('/progress-and-rankings/projects/proj1/badges')
    cy.injectAxe();
    cy.get('[data-cy="earnedBadgeLink_badge3"]')
    cy.get('[data-cy="badgeDetailsLink_badge1"]')

    cy.customLighthouse();
    cy.customA11y();
  })

  it('single badge page', () => {
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem'})
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4, { selfReportingType: 'Approval'})
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

    cy.createSubject(1, 2)

    cy.createSubject(1, 3)
    cy.createSkill(1, 3, 4, { selfReportingType: 'HonorSystem'})
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'yesterday' })
    cy.doReportSkill({ project: 1, skill: 4, subjNum: 3, userId: Cypress.env('proxyUser'), date: 'now' })

    cy.createBadge(1, 1);
    cy.assignSkillToBadge(1, 1, 1);
    cy.assignSkillToBadge(1, 1, 2);
    cy.assignSkillToBadge(1, 1, 3);

    cy.createBadge(1, 1, { enabled: true });

    cy.visit('/progress-and-rankings/projects/proj1/badges/badge1')
    cy.injectAxe();
    cy.get('[data-cy="skillProgressTitle-skill1"]')

    cy.customLighthouse();
    cy.customA11y();
  })

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

  it('"My Progress" landing page with many skills', () => {
    for (let i = 1; i <= 10; i++) {
      if (i > 1) {
        cy.createProject(i);
      }
      cy.enableProdMode(i);
      cy.addToMyProjects(i);

      cy.createSubject(i, 1);
      cy.createSkill(i, 1, 100, {numPerformToCompletion: 1});
      cy.reportSkill(i, 100, Cypress.env('proxyUser'), 'now')

      cy.createBadge(i, 1);
      cy.assignSkillToBadge(i, 1, 100);
      cy.createBadge(i, 1, { enabled: true });
    }

    cy.createSubject(1, 1);
    const numSkills = 10
    for (let i = 0; i < numSkills; i++) {
      cy.createSkill(1, 1, i, {numPerformToCompletion: 1});
    }
    for (let i = 0; i < numSkills; i++) {
      cy.reportSkill(1, i, Cypress.env('proxyUser'), 'now')
    }

    cy.visit('/progress-and-rankings');
    cy.injectAxe();
    cy.get('[data-cy="numProjectsContributed"]').should('have.text', 10);
    cy.get('[data-cy="numAchievedSkills"]').should('have.text', 20);
    cy.get('[data-cy="numAchievedBadges"]').should('have.text', 10);
    cy.customLighthouse();
    cy.customA11y();
  })


  it('table in a skill description', () => {
    cy.createSubject(1,1)
    cy.createSkill(1, 1, 1, {description: '| Function | Explanation                                                                                                                                                       |\n' +
        '| -------- |-------------------------------------------------------------------------------------------------------------------------------------------------------------------|\n' +
        '| Subjects | Add, edit or remove Subjects                                                                                                                                      |\n' +
        '| Skills | Add, edit or remove Skill definitions                                                                                                                             |'});

    cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
    cy.injectAxe();
    cy.get('[data-cy="skillProgressTitle-skill1"]')

    cy.customLighthouse();
    cy.customA11y();
  })

})

