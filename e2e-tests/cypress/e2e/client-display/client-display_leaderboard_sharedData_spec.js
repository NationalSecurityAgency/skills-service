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

const dateFormatter = value => moment.utc(value)
    .format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Leaderboard (with shared data) Tests', () => {
    const tableSelector = '[data-cy="leaderboardTable"]';
    const rowSelector = `${tableSelector} tbody tr`;

    after(() => {
        Cypress.env('disableResetDb', false);
    });

    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();

                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        for (let i = 1; i <= 10; i += 1) {
            cy.createSkill(1, 1, i);
        }
        for (let i = 11; i <= 20; i += 1) {
            cy.createSkill(1, 2, i);
        }

        for (let i = 1; i <= 12; i += 1) {
            for (let reportCounter = 1; reportCounter <= i; reportCounter += 1) {
                cy.doReportSkill({
                    skill: reportCounter,
                    userId: `user${i}@skills.org`,
                    date: '2021-02-24 10:00',
                    subjNum: reportCounter <= 10 ? 1 : 2
                });
            }
        }
    });

    beforeEach(() => {
        // so we can see full leaderboard
        cy.viewport(1300, 1600);
    });

    it('leaderboard top 10', () => {
        cy.cdVisit('/');
        cy.cdClickRank();

        cy.get(tableSelector)
            .contains('Loading...')
            .should('not.exist');
        cy.get(rowSelector)
            .should('have.length', 10)
            .as('cyRows');

        const date = moment.utc()
            .format('YYYY-MM-DD');
        for (let i = 1; i <= 10; i += 1) {
            cy.get('@cyRows')
                .eq(i - 1)
                .find('td')
                .as('row');
            cy.get('@row')
                .eq(0)
                .should('contain.text', `${i}`);
            cy.get('@row')
                .eq(1)
                .should('contain.text', `user${13 - i}@skills.org`);
            cy.get('@row')
                .eq(2)
                .should('contain.text', `${format('#,##0.', 1300 - (i * 100))} Points`);
            cy.get('@row')
                .eq(3)
                .should('contain.text', `${date}`);
        }

        // TODO: investigate why snapshots continue to fail on CI with a slight difference in spacing
        // cy.wait(2000)
        // cy.matchSnapshotImageForElement('[data-cy="leaderboard"]', { blackout: '[data-cy="dateCell"]' });
    });

    if (!Cypress.env('oauthMode')) {
        it('leaderboard 10 Around Me', () => {
            cy.cdVisit('/rank');
            cy.get('[data-cy="myRankPositionStatCard"]').contains('13')
            cy.get('[data-cy="leaderboard"] [data-cy="badge-selector"] [data-cy="select-tenAroundMe"]')
                .click();

            cy.get(tableSelector)
                .contains('Loading...')
                .should('not.exist');
            cy.get(rowSelector)
                .should('have.length', 6)
                .as('cyRows');

            const date = moment.utc()
                .format('YYYY-MM-DD');
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

            // TODO: investigate why snapshots continue to fail on CI with a slight difference in spacing
            // cy.wait(2000)
            // cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
            //     blackout: '[data-cy="dateCell"]'
            // });
        });

        it('switch between "top 10" and "10 around me" ', () => {
            cy.cdVisit('/');
            cy.cdClickRank();

            cy.get(tableSelector)
                .contains('Loading...')
                .should('not.exist');
            cy.get(rowSelector)
                .should('have.length', 10)
                .as('cyRows');

            cy.get('[data-cy="leaderboard"] [data-cy="badge-selector"] [data-cy="select-tenAroundMe"]')
                .click();
            cy.get(tableSelector)
                .contains('Loading...')
                .should('not.exist');
            cy.get(rowSelector)
                .should('have.length', 6)
                .as('cyRows');

            cy.get('[data-cy="leaderboard"] [data-cy="badge-selector"] [data-cy="select-topTen"]')
                .click();
            cy.get(tableSelector)
                .contains('Loading...')
                .should('not.exist');
            cy.get(rowSelector)
                .should('have.length', 10)
                .as('cyRows');
        });

        it('leaderboard on subject\'s rank - subject # 1', () => {
            cy.cdVisit('/');
            cy.contains('Overall Points');
            cy.cdClickSubj(0, 'Subject 1');
            cy.cdClickRank();

            cy.get(tableSelector)
                .contains('Loading...')
                .should('not.exist');
            cy.get(rowSelector)
                .should('have.length', 10)
                .as('cyRows');

            const usersNums = [10, 11, 12, 9, 8, 7, 6, 5, 4, 3];
            for (let i = 0; i < 10; i += 1) {
                cy.get('@cyRows')
                    .eq(i)
                    .find('td')
                    .as('row');
                cy.get('@row')
                    .eq(0)
                    .should('contain.text', `${i + 1}`);
                cy.get('@row')
                    .eq(1)
                    .should('contain.text', `user${usersNums[i]}@skills.org`);
            }
        });

        it('leaderboard on subject\'s rank - subject # 2', () => {
            cy.cdVisit('/');
            cy.contains('Overall Points');
            cy.cdClickSubj(1, 'Subject 2');
            cy.cdClickRank();

            cy.get(tableSelector)
                .contains('Loading...')
                .should('not.exist');
            cy.get(rowSelector)
                .should('have.length', 3)
                .as('cyRows');

            const date = moment.utc()
                .format('YYYY-MM-DD');
            for (let i = 0; i < 2; i += 1) {
                cy.get('@cyRows')
                    .eq(i)
                    .find('td')
                    .as('row');
                cy.get('@row')
                    .eq(0)
                    .should('contain.text', `${i + 1}`);
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
        });
    }

    it('all admin leaderboard top 10 - opt out - non admin switch between "top 10" and "10 around me"', () => {
        cy.request('POST', '/admin/projects/proj1/settings', [{
            "value": true,
            "setting": "project-admins_rank_and_leaderboard_optOut",
            "lastLoadedValue": false,
            "dirty": true,
            "projectId": "proj1"
        }]);

        cy.register('user1', 'password1', false);
        cy.logout();
        cy.login('user1', 'password1');

        cy.cdVisit('/');
        cy.cdClickRank();

        cy.get(tableSelector)
          .contains('Loading...')
          .should('not.exist');
        cy.get(rowSelector)
          .should('have.length', 10)
          .as('cyRows');

        cy.get('[data-cy="leaderboard"] [data-cy="badge-selector"] [data-cy="select-tenAroundMe"]')
          .click();
        cy.get(tableSelector)
          .contains('Loading...')
          .should('not.exist');
        cy.get(rowSelector)
          .should('have.length', 6)
          .as('cyRows');

        cy.get('[data-cy="leaderboard"] [data-cy="badge-selector"] [data-cy="select-topTen"]')
          .click();
        cy.get(tableSelector)
          .contains('Loading...')
          .should('not.exist');
        cy.get(rowSelector)
          .should('have.length', 10)
          .as('cyRows');
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
        cy.get('[data-cy="myRank"]')
          .contains('Opted-Out');
        cy.get('[data-cy="myRank"]')
          .contains('Your position would be 13 if you opt-in');
        cy.wait(2000)
        cy.matchSnapshotImageForElement('[data-cy="myRank"]', {
            name: 'my-rank-opted-out',
            blackout: '[data-cy="dateCell"]',
            errorThreshold: 0.05
        });

        cy.cdClickRank();

        cy.get('[data-cy="myRankPositionStatCard"]')
          .contains('Opted-Out');
        cy.get('[data-cy="leaderboard"]')
          .contains('You selected to opt-out');

        cy.wait(2000)
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
            name: 'rank-overview-leaderboard-opted-out',
            blackout: '[data-cy="dateCell"]',
            errorThreshold: 0.05
        });
    });
});
