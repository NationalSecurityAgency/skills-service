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

describe('Client Display Skills Groups Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1,1)
    })

    it('one group', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 15, numPerformToCompletion: 2 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="groupSkillsRequiredBadge"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 1')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('0 / 80 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 50 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 30 Points')
    })

    it('one group - partial progress', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 150, numPerformToCompletion: 2 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="groupSkillsRequiredBadge"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 1')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('250 / 500 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('150 / 300 Points')
    })

    it('one group - fully achieved', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 150, numPerformToCompletion: 2 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="groupSkillsRequiredBadge"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 1')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('500 / 500 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('300 / 300 Points')
    })


    it('group with partial completion requirement', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 2 });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 1')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]').contains('Requires 2 out of 3 skills');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('0 / 400 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 200 Points')
    })

    it('group with partial completion requirement - partially completed', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 2 });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 1')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]').contains('Requires 2 out of 3 skills');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('300 / 400 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 200 Points')
    })

    it('group with partial completion requirement - partially completed - with multiuple skills completing for overall points', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 100, numPerformToCompletion: 3 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 100, numPerformToCompletion: 3 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 100, numPerformToCompletion: 3  });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 2 });

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), '2 days ago');

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 1')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]').contains('Requires 2 out of 3 skills');

        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 300 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 300 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('300 / 300 Points')

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('500 / 600 Points')
    })

    it('group with partial completion requirement - fully completed', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 2 });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 1')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]').contains('Requires 2 out of 3 skills');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('400 / 400 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 200 Points')
    })

    it('group with partial completion requirement - fully completed - with extra points', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 2 });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 1')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]').contains('Requires 2 out of 3 skills');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('400 / 400 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 200 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 200 Points')
    })

    it('multiple groups', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 1 });

        cy.createSkillsGroup(1, 1, 2);
        cy.addSkillToGroup(1, 1, 2, 3, { pointIncrement: 50, numPerformToCompletion: 1 });
        cy.addSkillToGroup(1, 1, 2, 4, { pointIncrement: 100, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 2, 5, { pointIncrement: 150, numPerformToCompletion: 3 });
        cy.addSkillToGroup(1, 1, 2, 6, { pointIncrement: 200, numPerformToCompletion: 4 });
        cy.createSkillsGroup(1, 1, 2, { enabled: true });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');

        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 6, Cypress.env('proxyUser'), '2 days ago');
        cy.reportSkill(1, 6, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 6, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="groupSkillsRequiredBadge"]').contains('Requires 1 out of 2 skills');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('10 / 50 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('10 / 50 Points')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 50 Points')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').first().contains('Awesome Group 2');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="groupSkillsRequiredBadge"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').first().contains('700 / 1,500 Points')
        cy.get('[data-cy="group-group2_skillProgress-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 50 Points')
        cy.get('[data-cy="group-group2_skillProgress-skill4"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 200 Points')
        cy.get('[data-cy="group-group2_skillProgress-skill5"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 450 Points')
        cy.get('[data-cy="group-group2_skillProgress-skill6"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('600 / 800 Points')
    })


    it('search groups and child skills', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { name: 'For SkIll1 Searching' });
        cy.addSkillToGroup(1, 1, 1, 2, { name: 'For skill1 SkIll2  Searching' });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 1, name: 'gRouP1' });

        cy.createSkillsGroup(1, 1, 2);
        cy.addSkillToGroup(1, 1, 2, 3, { name: 'For skill1 SkIll2 skill3  Searching' });
        cy.addSkillToGroup(1, 1, 2, 4, { name: 'For skill1 SkIll2 skill3 skill4  Searching' });
        cy.addSkillToGroup(1, 1, 2, 5, { name: 'For skill1 SkIll2 skill3 skill4 skill5  Searching' });
        cy.createSkillsGroup(1, 1, 2, { enabled: true, name: 'gRouP1 GrOuP2' });

        cy.createSkillsGroup(1, 1, 3);
        cy.addSkillToGroup(1, 1, 3, 6, { name: 'For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7  Searching' });
        cy.addSkillToGroup(1, 1, 3, 7, { name: 'For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7 skill8 Searching' });
        cy.createSkillsGroup(1, 1, 3, { enabled: true, name: 'gRouP1 GrOuP2 group3' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgressTitle"]')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2');
        cy.get('[data-cy="group-group2_skillProgress-skill3"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill4"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill5"] [data-cy="skillProgressTitle"]')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2 group3');
        cy.get('[data-cy="group-group3_skillProgress-skill6"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group3_skillProgress-skill7"] [data-cy="skillProgressTitle"]')

        // search for group - 1 result
        cy.get('[data-cy="skillsSearchInput"]').type('GrOup3');

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2 group3');
        cy.get('[data-cy="group-group3_skillProgress-skill6"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group3_skillProgress-skill7"] [data-cy="skillProgressTitle"]')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');

        // search for group - 2 results
        cy.get('[data-cy="skillsSearchInput"]').clear().type('group2');

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2');
        cy.get('[data-cy="group-group2_skillProgress-skill3"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill4"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill5"] [data-cy="skillProgressTitle"]')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2 group3');
        cy.get('[data-cy="group-group3_skillProgress-skill6"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group3_skillProgress-skill7"] [data-cy="skillProgressTitle"]')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');

        // search for skill - 1 results
        cy.get('[data-cy="skillsSearchInput"]').clear().type('sKiLL7');

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2 group3');
        cy.get('[data-cy="group-group3_skillProgress-skill6"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group3_skillProgress-skill7"] [data-cy="skillProgressTitle"]')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');

        // search for skill - 2 results
        cy.get('[data-cy="skillsSearchInput"]').clear().type('SKILL5');

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2');
        cy.get('[data-cy="group-group2_skillProgress-skill3"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill4"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill5"] [data-cy="skillProgressTitle"]')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2 group3');
        cy.get('[data-cy="group-group3_skillProgress-skill6"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group3_skillProgress-skill7"] [data-cy="skillProgressTitle"]')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');
    })

    it('filter should not show empty groups', () => {
        Cypress.Commands.add("validateFilterCounts", (withoutProgress, withPointsToday, complete, selfReported, inProgress) => {
            cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
            cy.get('[data-cy="filter_withoutProgress"] [data-cy="filterCount"]').contains(withoutProgress)
            cy.get('[data-cy="filter_withPointsToday"] [data-cy="filterCount"]').contains(withPointsToday)
            cy.get('[data-cy="filter_complete"] [data-cy="filterCount"]').contains(complete)
            cy.get('[data-cy="filter_selfReported"] [data-cy="filterCount"]').contains(selfReported)
            cy.get('[data-cy="filter_inProgress"] [data-cy="filterCount"]').contains(inProgress)
        });

        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { name: 'For SkIll1 Searching', selfReportingType: 'HonorSystem'});
        cy.addSkillToGroup(1, 1, 1, 2, { name: 'For skill1 SkIll2  Searching' });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 1, name: 'gRouP1' });

        cy.createSkillsGroup(1, 1, 2);
        cy.addSkillToGroup(1, 1, 2, 3, { name: 'For skill1 SkIll2 skill3  Searching' });
        cy.addSkillToGroup(1, 1, 2, 4, { name: 'For skill1 SkIll2 skill3 skill4  Searching', selfReportingType: 'HonorSystem' });
        cy.addSkillToGroup(1, 1, 2, 5, { name: 'For skill1 SkIll2 skill3 skill4 skill5  Searching' });
        cy.createSkillsGroup(1, 1, 2, { enabled: true, name: 'gRouP1 GrOuP2' });

        cy.createSkillsGroup(1, 1, 3);
        cy.addSkillToGroup(1, 1, 3, 6, { name: 'For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7  Searching' });
        cy.addSkillToGroup(1, 1, 3, 7, { name: 'For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7 skill8 Searching' });
        cy.createSkillsGroup(1, 1, 3, { enabled: true, name: 'gRouP1 GrOuP2 group3' });

        cy.reportSkill(1, 6, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 6, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 7, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.validateFilterCounts(5, 2, 1, 2, 1);
        cy.get('[data-cy="filter_complete"] [data-cy="filterCount"]').click();

        // filter down to 1 skill
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2');
        cy.get('[data-cy="group-group3_skillProgress-skill6"] [data-cy="skillProgressTitle"]').contains('For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7 Searching')
        cy.get('[data-cy="group-group3_skillProgress-skill7"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');

        cy.get('[data-cy="clearSelectedFilter"]').click()
        cy.get('[data-cy="selectedFilter"]').should('not.exist')

        // 2 skills under the same group
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
        cy.get('[data-cy="filter_withPointsToday"] [data-cy="filterCount"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2');
        cy.get('[data-cy="group-group3_skillProgress-skill6"] [data-cy="skillProgressTitle"]').contains('For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7 Searching')
        cy.get('[data-cy="group-group3_skillProgress-skill7"] [data-cy="skillProgressTitle"]').contains('For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7 skill8 Searching')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');

        cy.get('[data-cy="clearSelectedFilter"]').click()
        cy.get('[data-cy="selectedFilter"]').should('not.exist')

        // multiple skills across more than 1 group
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
        cy.get('[data-cy="filter_selfReported"] [data-cy="filterCount"]').click();

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgressTitle"]').should('not.exist');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2');
        cy.get('[data-cy="group-group2_skillProgress-skill4"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill3"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="group-group2_skillProgress-skill5"] [data-cy="skillProgressTitle"]').should('not.exist');

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');
    })

    it('filter and search', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { name: 'For SkIll1 Searching', selfReportingType: 'HonorSystem'});
        cy.addSkillToGroup(1, 1, 1, 2, { name: 'For skill1 SkIll2  Searching' });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 1, name: 'gRouP1' });

        cy.createSkillsGroup(1, 1, 2);
        cy.addSkillToGroup(1, 1, 2, 3, { name: 'For skill1 SkIll2 skill3  Searching' });
        cy.addSkillToGroup(1, 1, 2, 4, { name: 'For skill1 SkIll2 skill3 skill4  Searching', selfReportingType: 'HonorSystem' });
        cy.addSkillToGroup(1, 1, 2, 5, { name: 'For skill1 SkIll2 skill3 skill4 skill5  Searching' });
        cy.createSkillsGroup(1, 1, 2, { enabled: true, name: 'gRouP1 GrOuP2' });

        cy.createSkillsGroup(1, 1, 3);
        cy.addSkillToGroup(1, 1, 3, 6, { name: 'For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7  Searching' });
        cy.addSkillToGroup(1, 1, 3, 7, { name: 'For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7 skill8 Searching' });
        cy.createSkillsGroup(1, 1, 3, { enabled: true, name: 'gRouP1 GrOuP2 group3' });

        cy.reportSkill(1, 6, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 6, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 7, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        // multiple skills across more than 1 group
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
        cy.get('[data-cy="filter_selfReported"] [data-cy="filterCount"]').click();

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1');
        cy.get('[data-cy="group-group1_skillProgress-skill1"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group1_skillProgress-skill2"] [data-cy="skillProgressTitle"]').should('not.exist');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2');
        cy.get('[data-cy="group-group2_skillProgress-skill4"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill3"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="group-group2_skillProgress-skill5"] [data-cy="skillProgressTitle"]').should('not.exist');

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');

        // search for group
        cy.get('[data-cy="skillsSearchInput"]').type('group2');

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2');
        cy.get('[data-cy="group-group2_skillProgress-skill4"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill3"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="group-group2_skillProgress-skill5"] [data-cy="skillProgressTitle"]').should('not.exist');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');

        // search for skill
        cy.get('[data-cy="skillsSearchInput"]').clear().type('skill4');

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').first().contains('gRouP1 GrOuP2');
        cy.get('[data-cy="group-group2_skillProgress-skill4"] [data-cy="skillProgressTitle"]')
        cy.get('[data-cy="group-group2_skillProgress-skill3"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="group-group2_skillProgress-skill5"] [data-cy="skillProgressTitle"]').should('not.exist');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').should('not.exist');

        // no res
        cy.get('[data-cy="skillsSearchInput"]').type('a');
        cy.get('[data-cy=noDataYet]').should('be.visible').contains('No results');
    })

    it('description ', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { name: 'For SkIll1 Searching' });
        cy.addSkillToGroup(1, 1, 1, 2, { name: 'For skill1 SkIll2  Searching' });
        cy.createSkillsGroup(1, 1, 1, { enabled: true, numSkillsRequired: 1, name: 'gRouP1' });

        cy.createSkillsGroup(1, 1, 2);
        cy.addSkillToGroup(1, 1, 2, 3, { name: 'For skill1 SkIll2 skill3  Searching' });
        cy.addSkillToGroup(1, 1, 2, 4, { name: 'For skill1 SkIll2 skill3 skill4  Searching' });
        cy.addSkillToGroup(1, 1, 2, 5, { name: 'For skill1 SkIll2 skill3 skill4 skill5  Searching' });
        cy.createSkillsGroup(1, 1, 2, { enabled: true, name: 'gRouP1 GrOuP2' });

        cy.createSkillsGroup(1, 1, 3);
        cy.addSkillToGroup(1, 1, 3, 6, { name: 'For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7  Searching' });
        cy.addSkillToGroup(1, 1, 3, 7, { name: 'For skill1 SkIll2 skill3 skill4 skill5 skill6 skill7 skill8 Searching' });
        cy.createSkillsGroup(1, 1, 3, { enabled: true, name: 'gRouP1 GrOuP2 group3' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
    })
})

