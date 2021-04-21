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
import moment from 'moment-timezone';

const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Skills Filtering Tests', () => {

  beforeEach(() => {
    Cypress.env('disabledUILoginProp', true);
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: 'proj1'
    });
    cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      name: 'Subject 1',
      helpUrl: 'http://doHelpOnThisSubject.com',
      description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
    });
  })


  it('counts in the filter', () => {
    Cypress.Commands.add("validateCounts", (withoutProgress, withPointsToday, complete, selfReported, inProgress) => {
      cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
      cy.get('[data-cy="skillsFilter_withoutProgress"] [data-cy="filterCount"]').contains(withoutProgress)
      cy.get('[data-cy="skillsFilter_withPointsToday"] [data-cy="filterCount"]').contains(withPointsToday)
      cy.get('[data-cy="skillsFilter_complete"] [data-cy="filterCount"]').contains(complete)
      cy.get('[data-cy="skillsFilter_selfReported"] [data-cy="filterCount"]').contains(selfReported)
      cy.get('[data-cy="skillsFilter_inProgress"] [data-cy="filterCount"]').contains(inProgress)
    });

    Cypress.Commands.add("refreshCounts", () => {
      cy.cdClickRank();
      cy.cdBack('Subject 1');
    });

    cy.createSkill(1, 1, 1);

    cy.cdVisit('/');
    cy.cdClickSubj(0);
    cy.validateCounts(1, 0, 0, 0, 0)

    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);
    cy.createSkill(1, 1, 4);
    cy.refreshCounts();
    cy.validateCounts(4, 0, 0, 0, 0)

    cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });
    cy.refreshCounts();
    cy.validateCounts(6, 0, 0, 2, 0)

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.refreshCounts();
    cy.validateCounts(5, 1, 0, 2, 1)

    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.refreshCounts();
    cy.validateCounts(4, 1, 0, 2, 2)

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.refreshCounts();
    cy.validateCounts(3, 2, 1, 2, 2)

    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.refreshCounts();
    cy.validateCounts(3, 3, 2, 2, 1)
  });

  it('filter skills', () => {
    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);
    cy.createSkill(1, 1, 4);
    cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withoutProgress"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills without progress')

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 5')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 6')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withPointsToday"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills with points earned today')

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('Very Great Skill 4')
    cy.get('[data-cy="skillProgress_index-4"]').should('not.exist')

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_complete"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Completed skills')
    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_selfReported"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Self Reported Skills')
    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 5')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 6')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_inProgress"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills in progress')
    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 2')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 4')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')
  });

  it('filter expanded skills', () => {
    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);
    cy.createSkill(1, 1, 4);
    cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy=toggleSkillDetails]').click()

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withoutProgress"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills without progress')

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 5')
    cy.get('[data-cy="skillProgress_index-0"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 6')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withPointsToday"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills with points earned today')

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-0"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-3"]').contains('Very Great Skill 4')
    cy.get('[data-cy="skillProgress_index-3"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-4"]').should('not.exist')
  });

  it('expand skills after filtering', () => {
    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);
    cy.createSkill(1, 1, 4);
    cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withoutProgress"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills without progress')

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 5')
    cy.get('[data-cy="skillProgress_index-0"]').contains('Overall Points Earned').should('not.exist')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 6')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Overall Points Earned').should('not.exist')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    // expand
    cy.get('[data-cy=toggleSkillDetails]').click()

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 5')
    cy.get('[data-cy="skillProgress_index-0"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 6')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withPointsToday"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills with points earned today')

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-0"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-3"]').contains('Very Great Skill 4')
    cy.get('[data-cy="skillProgress_index-3"]').contains('Overall Points Earned')
    cy.get('[data-cy="skillProgress_index-4"]').should('not.exist')

  });

  it('clear filter', () => {
    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);
    cy.createSkill(1, 1, 4);
    cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('Very Great Skill 4')
    cy.get('[data-cy="skillProgress_index-4"]').contains('Very Great Skill 5')
    cy.get('[data-cy="skillProgress_index-5"]').contains('Very Great Skill 6')
    cy.get('[data-cy="skillProgress_index-6"]').should('not.exist')

    // filter
    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withoutProgress"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills without progress')

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 5')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 6')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    // clear
    cy.get('[data-cy="clearSelectedFilter"]').click()
    cy.get('[data-cy="selectedFilter"]').should('not.exist')

    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('Very Great Skill 4')
    cy.get('[data-cy="skillProgress_index-4"]').contains('Very Great Skill 5')
    cy.get('[data-cy="skillProgress_index-5"]').contains('Very Great Skill 6')
    cy.get('[data-cy="skillProgress_index-6"]').should('not.exist')
  })




})
