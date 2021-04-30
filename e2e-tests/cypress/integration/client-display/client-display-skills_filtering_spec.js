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

  const snapshotOptions = {
    blackout: ['[data-cy=pointHistoryChart]', '#dependent-skills-network', '[data-cy=achievementDate]'],
    failureThreshold: 0.03, // threshold for entire image
    failureThresholdType: 'percent', // percent of image or number of pixels
    customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
    capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
  };

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

    Cypress.Commands.add("validateCounts", (withoutProgress, withPointsToday, complete, selfReported, inProgress) => {
      cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
      cy.get('[data-cy="skillsFilter_withoutProgress"] [data-cy="filterCount"]').contains(withoutProgress)
      cy.get('[data-cy="skillsFilter_withPointsToday"] [data-cy="filterCount"]').contains(withPointsToday)
      cy.get('[data-cy="skillsFilter_complete"] [data-cy="filterCount"]').contains(complete)
      cy.get('[data-cy="skillsFilter_selfReported"] [data-cy="filterCount"]').contains(selfReported)
      cy.get('[data-cy="skillsFilter_inProgress"] [data-cy="filterCount"]').contains(inProgress)
    });
  })


  it('counts in the filter', () => {
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

  it('search skills', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6'});

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')
    cy.get('[data-cy="skillProgress_index-4"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-5"]').contains('skill 6')
    cy.get('[data-cy="skillProgress_index-6"]').should('not.exist')

    cy.get('[data-cy="skillsSearchInput"]').type('bLaH ');
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-3"]').should('not.exist')

    cy.get('[data-cy="skillsSearchInput"]').type('O');
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')

    cy.get('[data-cy="skillsSearchInput"]').clear().type('se');
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 4')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-3"]').should('not.exist')

    cy.get('[data-cy="skillsSearchInput"]').type('e');
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')
  })


  it('ability to clear search string', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6'});

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="clearSkillsSearchInput"]').should('not.exist')
    cy.get('[data-cy="skillsSearchInput"]').type('bLaH ');
    cy.get('[data-cy="clearSkillsSearchInput"]').should('exist')

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-3"]').should('not.exist')

    cy.get('[data-cy="clearSkillsSearchInput"]').click();
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')
    cy.get('[data-cy="skillProgress_index-4"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-5"]').contains('skill 6')
    cy.get('[data-cy="skillProgress_index-6"]').should('not.exist')
  })

  it('search produces no results', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsSearchInput"]').type('bLaH1 ');
    cy.get('[data-cy="skillProgress_index-0"]').should('not.exist')

    cy.get('[ data-cy="noDataYet"]').contains('No results');
    cy.get('[ data-cy="noDataYet"]').contains('Please refine [bLaH1 ] search');
  })

  it('search and filter produces no results', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsSearchInput"]').type('bLaH1 ');

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withoutProgress"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').should('not.exist')
    cy.get('[ data-cy="noDataYet"]').contains('No results');
    cy.get('[ data-cy="noDataYet"]').contains('Please refine [bLaH1 ] search and/or clear the selected filter');
  })

  it('filter then search', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5', selfReportingType: 'Approval'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6', selfReportingType: 'HonorSystem'});

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withPointsToday"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills with points earned today')

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')
    cy.get('[data-cy="skillProgress_index-4"]').should('not.exist')

    cy.get('[data-cy="skillsSearchInput"]').type('b');

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')
  });

  it('search then filter', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5', selfReportingType: 'Approval'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6', selfReportingType: 'HonorSystem'});

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsSearchInput"]').type('b');

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-3"]').should('not.exist')

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withPointsToday"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills with points earned today')

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')
  });


  it('search should still apply after filter is cleared', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5', selfReportingType: 'Approval'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6', selfReportingType: 'HonorSystem'});

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsSearchInput"]').type('b');
    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withPointsToday"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills with points earned today')

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.get('[data-cy="clearSelectedFilter"]').click()
    cy.get('[data-cy="selectedFilter"]').should('not.exist')

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-3"]').should('not.exist')
  });


  it('filter should still apply after search is cleared', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5', selfReportingType: 'Approval'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6', selfReportingType: 'HonorSystem'});

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsSearchInput"]').type('b');
    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withPointsToday"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills with points earned today')

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.get('[data-cy="clearSkillsSearchInput"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')
    cy.get('[data-cy="skillProgress_index-4"]').should('not.exist')
  });


  it('change filter with search', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5', selfReportingType: 'Approval'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6', selfReportingType: 'HonorSystem'});

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsSearchInput"]').type('b');
    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withPointsToday"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills with points earned today')

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_selfReported"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')
  });


  it('filter skills on badge catalog page', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5', selfReportingType: 'Approval'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6', selfReportingType: 'HonorSystem'});

    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.assignSkillToBadge(1, 1, 3)
    cy.assignSkillToBadge(1, 1, 4)
    cy.assignSkillToBadge(1, 1, 5)

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickBadges();
    cy.contains('Badges');
    cy.get('[data-cy="badgeDetailsLink_badge1"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')
    cy.get('[data-cy="skillProgress_index-4"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-5"]').should('not.exist')

    cy.validateCounts(1, 4, 2, 1, 2);

    cy.get('[data-cy="skillsFilter_complete"]').click();
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.get('[data-cy="skillsSearchInput"]').type('other');
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')
  });

  it('filter skills on completed badge page', () => {
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});

    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.assignSkillToBadge(1, 1, 3)
    cy.assignSkillToBadge(1, 1, 4)

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickBadges();
    cy.contains('Badges');
    cy.get('[data-cy="earnedBadgeLink_badge1"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')

    cy.validateCounts(0, 4, 4, 0, 0);

    cy.get('[data-cy="skillsFilter_complete"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')

    cy.get('[data-cy="skillsSearchInput"]').type('blah');
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')
  });

  it('filter skills on global badge page', () => {
    cy.resetDb();
    cy.fixture('vars.json').then((vars) => {
      if (!Cypress.env('oauthMode')) {
        cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
      }
    })
    cy.loginAsProxyUser()
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});

    cy.loginAsRootUser();

    cy.createGlobalBadge(1)
    cy.assignSkillToGlobalBadge(1, 1)
    cy.assignSkillToGlobalBadge(1, 2)
    cy.assignSkillToGlobalBadge(1, 3)
    cy.assignSkillToGlobalBadge(1, 4)

    cy.loginAsProxyUser();

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickBadges();
    cy.get('[data-cy="badgeDetailsLink_globalBadge1"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')

    cy.validateCounts(0, 4, 4, 0, 0);

    cy.get('[data-cy="skillsFilter_complete"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')

    cy.get('[data-cy="skillsSearchInput"]').type('blah');
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.cdVisit('/');
    cy.cdClickBadges();
    cy.get('[data-cy="earnedBadgeLink_globalBadge1"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')

    cy.validateCounts(0, 4, 4, 0, 0);

    cy.get('[data-cy="skillsFilter_complete"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')

    cy.get('[data-cy="skillsSearchInput"]').type('blah');
    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')
  });

  it('Visual Test: skills search and skills filter selected', () => {
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5', selfReportingType: 'Approval'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6', selfReportingType: 'HonorSystem'});

    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.assignSkillToBadge(1, 1, 3)
    cy.assignSkillToBadge(1, 1, 4)
    cy.assignSkillToBadge(1, 1, 5)

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillsSearchInput"]').type('blah');

    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();
    cy.get('[data-cy="skillsFilter_withPointsToday"]').click();
    cy.get('[data-cy="selectedFilter"]').contains('Skills with points earned today')

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-0"]').contains('200 / 200')
    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-1"]').contains('200 / 200')
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"]');
  });

  it('Visual Test: skills filter open', () => {
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1, {name: 'Search blah skill 1'});
    cy.createSkill(1, 1, 2, {name: 'is a skill 2'});
    cy.createSkill(1, 1, 3, {name: 'find Blah other skill 3'});
    cy.createSkill(1, 1, 4, {name: 'Search nothing skill 4'});
    cy.createSkill(1, 1, 5, {name: 'sEEk bLaH skill 5', selfReportingType: 'Approval'});
    cy.createSkill(1, 1, 6, {name: 'some other skill 6', selfReportingType: 'HonorSystem'});

    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.assignSkillToBadge(1, 1, 3)
    cy.assignSkillToBadge(1, 1, 4)
    cy.assignSkillToBadge(1, 1, 5)

    cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')

    cy.cdVisit('/');
    cy.cdClickSubj(0);
    cy.get('[data-cy="skillsFilter"] [data-cy="skillsFilterBtn"]').click();

    cy.get('[data-cy="skillProgress_index-0"]').contains('skill 1')
    cy.get('[data-cy="skillProgress_index-0"]').contains('200 / 200')

    cy.get('[data-cy="skillProgress_index-1"]').contains('skill 2')
    cy.get('[data-cy="skillProgress_index-1"]').contains('100 / 200')

    cy.get('[data-cy="skillProgress_index-2"]').contains('skill 3')
    cy.get('[data-cy="skillProgress_index-2"]').contains('100 / 200')

    cy.get('[data-cy="skillProgress_index-3"]').contains('skill 4')
    cy.get('[data-cy="skillProgress_index-3"]').contains('0 / 200')

    cy.get('[data-cy="skillProgress_index-4"]').contains('skill 5')
    cy.get('[data-cy="skillProgress_index-4"]').contains('0 / 200')

    cy.get('[data-cy="skillProgress_index-5"]').contains('skill 6')
    cy.get('[data-cy="skillProgress_index-5"]').contains('0 / 200')

    cy.get('[data-cy="skillProgress_index-6"]').should('not.exist')
    cy.matchSnapshotImage(snapshotOptions);
  });

})
