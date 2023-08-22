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

const dateFormatter = value => moment.utc(value)
    .format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Recurring Expiration Tests', () => {

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 50,
            pointIncrementInterval: 0
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 50,
            pointIncrementInterval: 0
        });

        Cypress.Commands.add('configureExpiration', (skillNum = '1', numDays = 30) => {
            // cy.request('POST', `/api/projects/proj1/skills/${skillId}`)
            const m = moment.utc().add(numDays, 'day');
            cy.request('POST', `/admin/projects/proj1/skills/skill${skillNum}/expiration`, {
                expirationType: 'YEARLY',
                every: 1,
                monthlyDay: m.date(),
                nextExpirationDate: m.format('x')
            });
        });
    });

    it('expiration date shows when it should', () => {
        const expirationDate = moment.utc().add(30, 'day');
        cy.configureExpiration();
        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.cdClickSkill(0);

        cy.get('[data-cy="claimPointsBtn"]')
            .click();

        cy.get('[data-cy="selfReportAlert"]')
            .contains('You just earned 50 points!');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="progressInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="progressInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('50 / 100 Points');

        cy.get(`[data-cy="expirationDate"]`).should('exist');
        cy.get(`[data-cy="expirationDate"]`).contains(expirationDate.format('MMMM D YYYY'))

        cy.get('[data-cy="nextSkill"]').click();

        cy.get('[data-cy="claimPointsBtn"]')
          .click();

        cy.get('[data-cy="selfReportAlert"]')
          .contains('You just earned 50 points!');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="progressInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="progressInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
          .contains('50 / 100 Points');

        cy.get(`[data-cy="expirationDate"]`).should('not.exist');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .should('exist');
        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .contains(expirationDate.format('MMMM D YYYY'))
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`)
          .should('not.exist');
    });

    it('expiration date will show as tomorrow when the actual date is in the past', () => {
        const tomorrow = moment.utc().add(1, 'day');
        cy.configureExpiration(1, -30);
        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.cdClickSkill(0);

        cy.get('[data-cy="claimPointsBtn"]')
          .click();

        cy.get('[data-cy="selfReportAlert"]')
          .contains('You just earned 50 points!');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="progressInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="progressInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="progressInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
          .contains('50 / 100 Points');

        cy.get(`[data-cy="expirationDate"]`).should('exist');
        cy.get(`[data-cy="expirationDate"]`).contains(tomorrow.format('MMMM D YYYY'))

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .should('exist');
        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .contains(tomorrow.format('MMMM D YYYY'))
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`)
          .should('not.exist')
    });
});
