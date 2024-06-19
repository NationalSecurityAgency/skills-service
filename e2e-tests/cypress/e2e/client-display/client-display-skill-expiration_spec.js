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

describe('Client Display Expiration Tests', () => {

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
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'HonorSystem',
            pointIncrement: 50,
            pointIncrementInterval: 0,
            numPerformToCompletion: 1,
        });
    });

    it('expiration date shows when it should', () => {
        let expirationDate = moment.utc().add(30, 'day');
        if (expirationDate.hour() >= 1) {
            expirationDate = expirationDate.add(1, 'day')
        }
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
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
            .contains('50');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
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
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
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

    it('post achievement expiration date shows when it should', () => {
        const sixtyDaysAgo = moment.utc().subtract(60, 'day')

        cy.configureExpiration(1, 0, 90, 'DAILY');
        cy.configureExpiration(3, 0, 1, 'DAILY');

        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: sixtyDaysAgo.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: sixtyDaysAgo.add(1, 'day').format('YYYY-MM-DD HH:mm') })

        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: Cypress.env('proxyUser'), date: moment.utc().format('YYYY-MM-DD HH:mm') })

        cy.cdVisit('/', true);
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .should('exist');
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.cdClickSkill(0);

        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('100');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('0');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
          .contains('100 / 100 Points');

        cy.get(`[data-cy="expirationDate"]`).should('exist');
        cy.get(`[data-cy="expirationDate"]`).contains('Expires in a month, perform this skill to keep your points!')

        cy.get('[data-cy="claimPointsBtn"]')
          .click();

        cy.get('[data-cy="selfReportAlert"]')
          .contains('You just retained your 100 points!');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('100');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('0');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
          .contains('100 / 100 Points');

        cy.get(`[data-cy="expirationDate"]`).should('not.exist');

        cy.get('[data-cy="nextSkill"]').click();

        cy.get('[data-cy="claimPointsBtn"]')
          .click();

        cy.get('[data-cy="selfReportAlert"]')
          .contains('You just earned 50 points!');
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="pointsAchievedTodayCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]')
          .contains('50');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
          .contains('50 / 100 Points');

        cy.get(`[data-cy="expirationDate"]`).should('not.exist');

        cy.get('[data-cy="nextSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
        cy.get(`[data-cy="expirationDate"]`).should('exist');
        cy.get(`[data-cy="expirationDate"]`).contains('perform this skill to keep your points!')

        cy.cdVisit('/', true);
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.get(`[data-cy="skillProgress_index-2"] [data-cy="expirationDate"]`)
          .should('exist');
    });

    it('post achievement expiration warning message will show if the most recently reported skill was NOT within the grace period', () => {
        cy.configureExpiration(1, 0, 90, 'DAILY');
        const outsideTheGracePeriod = moment.utc().subtract(29, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: outsideTheGracePeriod.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: outsideTheGracePeriod.add(1, 'day').format('YYYY-MM-DD HH:mm') })

        cy.cdVisit('/', true);
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .should('exist');
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.cdClickSkill(0);

        cy.get(`[data-cy="expirationDate"]`).should('exist');
    });

    it('post achievement expiration date will NOT show if the most recently reported skill was within the grace period', () => {
        cy.configureExpiration(1, 0, 90, 'DAILY');
        const withinTheGracePeriod = moment.utc().subtract(28, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: withinTheGracePeriod.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: withinTheGracePeriod.add(1, 'day').format('YYYY-MM-DD HH:mm') })

        cy.cdVisit('/', true);
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`)
          .should('not.exist');
        cy.cdClickSkill(0);

        cy.get(`[data-cy="expirationDate"]`).should('not.exist');
    });

    it('post achievement expiration warning message is formatted properly in line with the scheduled expiration run', () => {
        cy.configureExpiration(1, 0, 1, 'DAILY');
        cy.configureExpiration(2, 0, 2, 'DAILY');
        cy.configureExpiration(3, 0, 3, 'DAILY');
        const yesterday = moment.utc().subtract(1, 'day')
        const twoDaysAgo = moment.utc().subtract(2, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: yesterday.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: twoDaysAgo.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: Cypress.env('proxyUser'), date: yesterday.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: Cypress.env('proxyUser'), date: twoDaysAgo.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: Cypress.env('proxyUser'), date: yesterday.format('YYYY-MM-DD HH:mm') })

        const currentHourOfDay = moment.utc().hour()
        const hourOfRun = 1
        const incrementStartDays = currentHourOfDay < hourOfRun ? 0 : 1;
        const firstRuntime = moment.utc().add(incrementStartDays, 'day').hour(hourOfRun).minute(0).second(0).millisecond(0)
        const secondRuntime = moment.utc().add(incrementStartDays+1, 'day').hour(hourOfRun).minute(0).second(0).millisecond(0)
        const thirdRuntime = moment.utc().add(incrementStartDays+2, 'day').hour(hourOfRun).minute(0).second(0).millisecond(0)
        cy.log(`currentHourOfDay [${currentHourOfDay}], firstRuntime [${firstRuntime}], from now [${firstRuntime.fromNow()}]`);

        cy.cdVisit('/', true);
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`)
          .should('exist');
        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="expirationDate"]`).contains(`Expires ${firstRuntime.fromNow()}`)
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`)
          .should('exist');
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="expirationDate"]`).contains(`Expires ${secondRuntime.fromNow()}`)
        cy.get(`[data-cy="skillProgress_index-2"] [data-cy="expirationDate"]`)
          .should('exist');
        cy.get(`[data-cy="skillProgress_index-2"] [data-cy="expirationDate"]`).contains(`Expires ${thirdRuntime.fromNow()}`)

        cy.cdClickSkill(0);
        cy.get(`[data-cy="expirationDate"]`).should('exist');
        cy.get(`[data-cy="expirationDate"]`).contains(`Expires ${firstRuntime.fromNow()}`)

        cy.get('[data-cy="nextSkill"]').click();
        cy.get(`[data-cy="expirationDate"]`).should('exist');
        cy.get(`[data-cy="expirationDate"]`).contains(`Expires ${secondRuntime.fromNow()}`)

        cy.get('[data-cy="nextSkill"]').click();
        cy.get(`[data-cy="expirationDate"]`).should('exist');
        cy.get(`[data-cy="expirationDate"]`).contains(`Expires ${thirdRuntime.fromNow()}`)

    });

    it('expired achievement shows how long ago in UI', () => {
        cy.configureExpiration(1, 0, 1, 'DAILY');
        cy.configureExpiration(2, 0, 1, 'DAILY');
        cy.configureExpiration(3, 0, 3, 'DAILY');
        const yesterday = moment.utc().subtract(1, 'day')
        const twoDaysAgo = moment.utc().subtract(2, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: yesterday.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: twoDaysAgo.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: Cypress.env('proxyUser'), date: yesterday.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: Cypress.env('proxyUser'), date: twoDaysAgo.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: Cypress.env('proxyUser'), date: yesterday.format('YYYY-MM-DD HH:mm') })

        cy.expireSkills();
        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="hasExpired"]`)
            .should('exist');
        cy.get(`[data-cy="skillProgress_index-0"] [data-cy="hasExpired"]`).contains(`Points expired a few seconds ago`)
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="hasExpired"]`)
            .should('exist');
        cy.get(`[data-cy="skillProgress_index-1"] [data-cy="hasExpired"]`).contains('Points expired a few seconds ago')
        cy.get(`[data-cy="skillProgress_index-2"] [data-cy="hasExpired"]`)
            .should('not.exist');

        cy.cdClickSkill(0);
        cy.get(`[data-cy="hasExpired"]`).should('exist');
        cy.get(`[data-cy="hasExpired"]`).contains(`Points expired a few seconds ago`)

        cy.get('[data-cy="nextSkill"]').click();
        cy.get(`[data-cy="hasExpired"]`).should('exist');
        cy.get(`[data-cy="hasExpired"]`).contains('Points expired a few seconds ago')

        cy.get('[data-cy="nextSkill"]').click();
        cy.get(`[data-cy="hasExpired"]`).should('not.exist');

    });
});
