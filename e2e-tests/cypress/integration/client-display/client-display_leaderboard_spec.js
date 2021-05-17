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
    const snapshotOptions = {
        blackout: ['[data-cy="userFirstSeen"]'],
        failureThreshold: 0.03, // threshold for entire image
        failureThresholdType: 'percent', // percent of image or number of pixels
        customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
        capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
    };
    const tableSelector = '[data-cy="leaderboardTable"]';
    const rowSelector = `${tableSelector} tbody tr`

    it('no users', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.cdVisit('/');
        cy.cdClickRank();

        cy.get('[data-cy="noDataYet"]')
            .contains('No Users');
    });

    it('no users', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.cdVisit('/');
        cy.contains('Overall Points');
        cy.cdClickSubj(0, 'Subject 1');
        cy.cdClickRank();

        cy.get('[data-cy="noDataYet"]')
            .contains('No Users');
        cy.matchSnapshotImageForElement('[data-cy="leaderboard"]', 'leaderboard-empty', snapshotOptions);
    });

    it('just me with points', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        const user = Cypress.env('proxyUser');
        cy.log(`user is: [${user}]`)
        cy.reportSkill(1, 1, user, '2021-02-24 10:00');

        cy.cdVisit('/');
        cy.contains('Overall Points');
        cy.cdClickRank();

        cy.get(tableSelector)
            .contains('Loading...')
            .should('not.exist');
        cy.get(rowSelector)
            .should('have.length', 1)
            .as('cyRows');

        cy.get('@cyRows')
            .eq(0)
            .find('td')
            .as('row');
        cy.get('@row')
            .eq(0)
            .should('contain.text', `1`);
        const userToValidate = Cypress.env('oauthMode') ? 'foo' : user;
        cy.get('@row')
            .eq(1)
            .should('contain.text', userToValidate);
        cy.get('@row')
            .eq(2)
            .should('contain.text', '100 Points');
    });

});

