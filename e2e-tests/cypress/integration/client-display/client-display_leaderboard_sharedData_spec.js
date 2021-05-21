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
import format from 'number-format.js';

const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Leaderboard (with shared data) Tests', () => {
  const snapshotOptions = {
    blackout: ['[data-cy="userFirstSeen"]'],
    failureThreshold: 0.03, // threshold for entire image
    failureThresholdType: 'percent', // percent of image or number of pixels
    customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
    capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
  };
  const tableSelector = '[data-cy="leaderboardTable"]';
  const rowSelector = `${tableSelector} tbody tr`

  after(() => {
    Cypress.env('disableResetDb', false);
  });

  before(() => {
    Cypress.env('disableResetDb', true);
    cy.resetDb();
    cy.fixture('vars.json').then((vars) => {
      cy.logout()

      if (!Cypress.env('oauthMode')) {
        cy.log('NOT in oauthMode, using form login')
        cy.login(vars.defaultUser, vars.defaultPass);
      } else {
        cy.log('oauthMode, using loginBySingleSignOn')
        cy.loginBySingleSignOn()
      }
    });

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
        cy.reportSkill(1,reportCounter, `user${i}@skills.org`, '2021-02-24 10:00');
      }
    }
  })

  beforeEach(() => {
    // so we can see full leaderboard
    cy.viewport(1200, 1600)
  })

  it('leaderboard top 10', () => {
    cy.cdVisit('/');
    cy.cdClickRank();

    cy.get(tableSelector).contains('Loading...').should('not.exist')
    cy.get(rowSelector).should('have.length', 10).as('cyRows');

    const date = moment.utc().format('MM/DD/YYYY')
    for (let i = 1; i <= 10; i += 1) {
      cy.get('@cyRows')
          .eq(i-1)
          .find('td')
          .as('row');
      cy.get('@row')
          .eq(0)
          .should('contain.text', `${i}`);
      cy.get('@row')
          .eq(1)
          .should('contain.text', `user${13-i}@skills.org`);
      cy.get('@row')
          .eq(2)
          .should('contain.text', `${format('#,##0.', 1300-(i*100))} Points`);
      cy.get('@row')
          .eq(3)
          .should('contain.text', `${date}`);
    }

    cy.matchSnapshotImageForElement('[data-cy="leaderboard"]', 'leaderboard', snapshotOptions);
  });

  it('leaderboard top 10 - opt out', () => {
    cy.request('POST', '/app/userInfo/settings', [{
      'settingGroup': 'user.prefs',
      'value': true,
      'setting': 'rank_and_leaderboard_optOut',
      'lastLoadedValue': '',
      'dirty': true
    }]);

    cy.cdVisit('/?loginAsUser=skills@skills.org');
    cy.get('[data-cy="myRank"]').contains('Opted-Out');
    cy.get('[data-cy="myRank"]').contains('Your position would be 13 if you opt-in');
    cy.matchSnapshotImageForElement('[data-cy="myRank"]', 'my-rank-opted-out', snapshotOptions);

    cy.cdClickRank();

    cy.get(tableSelector).contains('Loading...').should('not.exist')
    cy.get(rowSelector).should('have.length', 10).as('cyRows');

    cy.get('[data-cy="myRankPositionStatCard"]').contains('Opted-Out')
    cy.get('[data-cy="leaderboard"]').contains('You selected to opt-out');

    cy.matchSnapshotImageForElement('[data-cy="myRankPositionStatCard"]', 'rank-overview-my-rank-opted-out', snapshotOptions);
    cy.matchSnapshotImageForElement('[data-cy="leaderboard"]', 'rank-overview-leaderboard-opted-out', snapshotOptions);
  });

  if (!Cypress.env('oauthMode')) {
    it('leaderboard 10 Around Me', () => {
      cy.cdVisit('/');
      cy.cdClickRank();

      cy.get('[data-cy="leaderboard"] [data-cy="badge-selector"]')
          .contains('10 Around Me')
          .click();

      cy.get(tableSelector)
          .contains('Loading...')
          .should('not.exist')
      cy.get(rowSelector)
          .should('have.length', 6)
          .as('cyRows');

      const date = moment.utc()
          .format('MM/DD/YYYY')
      for (let i = 0; i < 6; i += 1) {
        cy.get('@cyRows')
            .eq(i)
            .find('td')
            .as('row');
        cy.get('@row')
            .eq(0)
            .should('contain.text', `${i + 8}`);
        cy.get('@row')
            .eq(1)
            .should('contain.text', i === 5 ? `${Cypress.env('proxyUser')}` : `user${5 - i}@skills.org`);
        cy.get('@row')
            .eq(2)
            .should('contain.text', `${format('#,##0.', 500 - (i * 100))} Points`);
        cy.get('@row')
            .eq(3)
            .should('contain.text', `${date}`);
      }

      cy.matchSnapshotImageForElement('[data-cy="leaderboard"]', 'leaderboard-10AroundMe', snapshotOptions);
    });

    it('switch between "top 10" and "10 around me" ', () => {
      cy.cdVisit('/');
      cy.cdClickRank();

      cy.get(tableSelector)
          .contains('Loading...')
          .should('not.exist')
      cy.get(rowSelector)
          .should('have.length', 10)
          .as('cyRows');

      cy.get('[data-cy="leaderboard"] [data-cy="badge-selector"]')
          .contains('10 Around Me')
          .click();
      cy.get(tableSelector)
          .contains('Loading...')
          .should('not.exist')
      cy.get(rowSelector)
          .should('have.length', 6)
          .as('cyRows');

      cy.get('[data-cy="leaderboard"] [data-cy="badge-selector"]')
          .contains('Top 10')
          .click();
      cy.get(tableSelector)
          .contains('Loading...')
          .should('not.exist')
      cy.get(rowSelector)
          .should('have.length', 10)
          .as('cyRows');
    })

    it('leaderboard on subject\'s rank - subject # 1', () => {
      cy.cdVisit('/');
      cy.contains('Overall Points');
      cy.cdClickSubj(0, 'Subject 1');
      cy.cdClickRank();

      cy.get(tableSelector)
          .contains('Loading...')
          .should('not.exist')
      cy.get(rowSelector)
          .should('have.length', 10)
          .as('cyRows');

      const date = moment.utc()
          .format('MM/DD/YYYY')
      const usersNums = [10, 11, 12, 9, 8, 7, 6, 5, 4, 3]
      for (let i = 0; i < 10; i += 1) {
        cy.get('@cyRows')
            .eq(i)
            .find('td')
            .as('row');
        cy.get('@row')
            .eq(0)
            .should('contain.text', `${i+1}`);
        cy.get('@row')
            .eq(1)
            .should('contain.text', `user${usersNums[i]}@skills.org`);
      }
    })

    it('leaderboard on subject\'s rank - subject # 2', () => {
      cy.cdVisit('/');
      cy.contains('Overall Points');
      cy.cdClickSubj(1, 'Subject 2');
      cy.cdClickRank();

      cy.get(tableSelector)
          .contains('Loading...')
          .should('not.exist')
      cy.get(rowSelector)
          .should('have.length', 3)
          .as('cyRows');

      const date = moment.utc()
          .format('MM/DD/YYYY')
      for (let i = 0; i < 2; i += 1) {
        cy.get('@cyRows')
            .eq(i)
            .find('td')
            .as('row');
        cy.get('@row')
            .eq(0)
            .should('contain.text', `${i+1}`);
        cy.get('@row')
            .eq(1)
            .should('contain.text', i === 5 ? `${Cypress.env('proxyUser')}` : `user${12 - i}@skills.org`);
        cy.get('@row')
            .eq(2)
            .should('contain.text', `${format('#,##0.', 200 - (i * 100))} Points`);
        cy.get('@row')
            .eq(3)
            .should('contain.text', `${date}`);
      }
    })
  }
})
