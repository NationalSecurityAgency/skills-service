/*
 * Copyright 2026 SkillTree
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

describe('Client Display Skills Groups Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        Cypress.Commands.add('reportHonorSkill', (skillNum) => {
            cy.get(`[data-cy="group-group1_skillProgress-skill${skillNum}"] [data-cy="claimPointsBtn"]`)
                .click();
            cy.get(`[data-cy="group-group1_skillProgress-skill${skillNum}"] [data-cy="selfReportAlert"]`)
                .contains('Congrats! You just earned 100 points');
        });
    });

    it('group with partial completion requirement', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 3, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.createSkillsGroup(1, 1, 1, { numSkillsRequired: 2 });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]')
            .first()
            .contains('Awesome Group 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]')
            .contains('Requires 2 out of 3 skills');
        cy.get('[data-cy=skillCompletedCheck-group1')
            .should('not.exist'); // completed checkbox should not exist
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .first()
            .contains('0 / 2 Skills');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('0 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('0 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('0 / 200 Points');
    });

    it('group with partial completion requirement - partially completed', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 3, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.createSkillsGroup(1, 1, 1, { numSkillsRequired: 2 });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/subjects/subj1', true);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]')
            .first()
            .contains('Awesome Group 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]')
            .contains('Requires 2 out of 3 skills');
        cy.get('[data-cy=skillCompletedCheck-group1')
            .should('not.exist'); // completed checkbox should not exist
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .first()
            .contains('1 / 2 Skills');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('100 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('0 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 200 Points');
    });

    it('group with partial completion requirement - partially completed - with multiple skills completing for overall points', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 3
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 100,
            numPerformToCompletion: 3
        });
        cy.addSkillToGroup(1, 1, 1, 3, {
            pointIncrement: 100,
            numPerformToCompletion: 3
        });
        cy.createSkillsGroup(1, 1, 1, { numSkillsRequired: 2 });

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2 days ago');

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/subjects/subj1');

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]')
            .first()
            .contains('Awesome Group 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]')
            .contains('Requires 2 out of 3 skills');

        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 300 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('100 / 300 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('300 / 300 Points');

        cy.get('[data-cy=skillCompletedCheck-group1')
            .should('not.exist'); // completed checkbox should not exist
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .first()
            .contains('1 / 2 Skills');
    });

    it('group with partial completion requirement - fully completed', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 3, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.createSkillsGroup(1, 1, 1, { numSkillsRequired: 2 });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/subjects/subj1');

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]')
            .first()
            .contains('Awesome Group 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]')
            .contains('Requires 2 out of 3 skills');
        cy.get('[data-cy=skillCompletedCheck-group1')
            .should('exist'); // completed checkbox should exist
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .first()
            .contains('2 / 2 Skills');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('0 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 200 Points');
    });

    it('group with partial completion requirement - fully completed - with extra points', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 3, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.createSkillsGroup(1, 1, 1, { numSkillsRequired: 2 });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/', true);
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]')
            .first()
            .contains('Awesome Group 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]')
            .contains('Requires 2 out of 3 skills');
        cy.get('[data-cy=skillCompletedCheck-group1')
            .should('exist'); // completed checkbox should exist
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .first()
            .contains('3 / 2 Skills');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 200 Points');
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 200 Points');
    });
})