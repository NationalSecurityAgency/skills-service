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

describe('Client Display Leaderboard Tests', () => {
    const tableSelector = '[data-cy="leaderboardTable"]';
    const rowSelector = `${tableSelector} tbody tr`;

    it('no users', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.cdVisit('/');
        cy.cdClickRank();

        cy.get('[data-cy="noContent"]')
            .contains('No Users');
    });

    it('no users - subject', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.cdVisit('/');
        cy.contains('Overall Points');
        cy.cdClickSubj(0, 'Subject 1');
        cy.cdClickRank();

        cy.get('[data-cy="noContent"]')
            .contains('No Users');
        cy.matchSnapshotImageForElement('[data-cy="leaderboard"]', {
            blackout: '[data-cy="userFirstSeen"]'
        });
    });

    it('just me with points', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const proxyUser = Cypress.env('proxyUser');
        const userToValidate = Cypress.env('oauthMode') ? 'foo' : proxyUser;
        cy.log(`user is: [${proxyUser}]`);

        cy.reportSkill(1, 1, proxyUser, '2021-02-24 10:00');

        cy.cdVisit('/');
        cy.contains('Overall Points');
        cy.cdClickRank();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: '#1' }, { colIndex: 1, value: userToValidate }, { colIndex: 2, value: '100 Points' }],
        ], 5, true, 1, false);

    });

    it('nickname displayed in leaderboard', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        const proxyUser = Cypress.env('proxyUser');
        const userToValidate = Cypress.env('oauthMode') ? 'foo' : proxyUser;
        cy.log(`user is: [${proxyUser}]`);

        cy.intercept('GET', '/api/projects/proj1/leaderboard?type=topTen', (req) => {
            req.reply((res) => {
                const leaderboard = res.body;
                leaderboard.rankedUsers.find(el => el.userId === userToValidate).nickname = 'John Doe';
                res.send(leaderboard);
            });
        })
            .as('getLeaderboard');

        cy.reportSkill(1, 1, proxyUser, '2021-02-24 10:00');

        cy.cdVisit('/');
        cy.contains('Overall Points');
        cy.cdClickRank();
        cy.wait('@getLeaderboard');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0, value: '#1' }, { colIndex: 1, value: `John Doe (${userToValidate})` }, { colIndex: 2, value: '100 Points' }],
        ], 5, true, 1, false);
    });

});

