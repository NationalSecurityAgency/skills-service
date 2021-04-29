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

describe('Client Display Leaderboard Tests', () => {
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
    cy.createSubject(1,2)
    for (let i = 1; i <= 10; i += 1) {
      cy.createSkill(1,1,i)
    }
    for (let i = 11; i <= 20; i += 1) {
      cy.createSkill(1,2,i)
    }

    for (let i = 1; i <= 12; i += 1) {
      for (let reportCounter = 1; reportCounter <= i; reportCounter += 1){
        cy.reportSkill(1,reportCounter, `user${i}@skills.org1`, '2021-02-24 10:00');
      }
    }
    cy.cdVisit('/');
    cy.cdClickRank();
  });

})
