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
import moment from 'moment';

const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Self Report Skills Tests', () => {

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

    Cypress.Commands.add("createSkill", (num, selfReportType) => {
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${num}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${num}`,
        name: `This is ${num}`,
        type: 'Skill',
        pointIncrement: 50,
        numPerformToCompletion: 2,
        pointIncrementInterval: 0,
        numMaxOccurrencesIncrementInterval: -1,
        description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        version: 0,
        helpUrl: 'http://doHelpOnThisSkill.com',
        selfReportType
      });
    });
  })


  it.only('only show self-report button if enabled', () => {
    cy.createSkill(1, 'Approval');
    cy.createSkill(2, 'HonorSystem');
    cy.createSkill(3, null);

    cy.cdVisit('/');
    cy.cdClickSubj(0);

  });


  it('self report skill', () => {
    cy.createSkill(1, 'Approval');
    cy.cdVisit('/');
    cy.cdClickSubj(0);
    cy.cdClickSkill(0);

    cy.get('[data-cy="selfReportBtn"]').click();
    cy.get('[data-cy="selfReportAlert"]').contains("You just earned 50 points!")
    cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]').contains('50');
    cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="progressInfoCardTitle"]').contains('50');
    cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="progressInfoCardTitle"]').contains('50');
    cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('50 / 100 Points')

    cy.get('[data-cy="skillProgressBar"]').matchImageSnapshot('Halfway_Progress');

    cy.get('[data-cy="selfReportBtn"]').click();
    cy.get('[data-cy="selfReportAlert"]').contains("You just earned 50 points!")
    cy.get('[data-cy="selfReportBtn"]').should('be.disabled');
    cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]').contains('100');
    cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="progressInfoCardTitle"]').contains('100');
    cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="progressInfoCardTitle"]').contains('50');
    cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')

    cy.get('[data-cy="skillProgressBar"]').matchImageSnapshot('Full_Progress');
  });


})
