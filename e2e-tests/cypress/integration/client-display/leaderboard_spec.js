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

describe('Client Display Features Tests', () => {
  const snapshotOptions = {
    blackout: ['[data-cy=pointHistoryChart]'],
    failureThreshold: 0.03, // threshold for entire image
    failureThresholdType: 'percent', // percent of image or number of pixels
    customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
    capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
  };

  beforeEach(() => {
    Cypress.env('disabledUILoginProp', true);
  })

  it('display leaderboard', () => {
    cy.createProject(1)
    cy.createSubject(1,1)
    cy.createSkill(1,1,1)
    cy.reportSkill(1,1, 'user@skills.org', '2021-02-24 10:00');
    cy.reportSkill(1,1, 'user@skills.org', '2021-02-25 10:00');
    cy.reportSkill(1,1, 'user1@skills.org', '2021-02-24 10:00');
    cy.reportSkill(1,1, 'user0', '2021-02-24 10:00');
    cy.cdVisit('/');
    cy.cdClickRank();
  });

})
