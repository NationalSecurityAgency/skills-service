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

describe('Client Display Skills Filtering Tests', () => {

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1',
            helpUrl: 'http://doHelpOnThisSubject.com',
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        });

        Cypress.Commands.add('validateCounts', (withoutProgress, complete, inProgress, pendingApproval, belongsToBadge = null, approval, honorSystem, quiz, survey, video) => {
            cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
                .click();
            cy.get('[data-cy="filter_withoutProgress"] [data-cy="filterCount"]')
                .should('have.text', withoutProgress);
            cy.get('[data-cy="filter_complete"] [data-cy="filterCount"]')
                .should('have.text', complete);
            cy.get('[data-cy="filter_inProgress"] [data-cy="filterCount"]')
                .should('have.text', inProgress);
            cy.get('[data-cy="filter_pendingApproval"] [data-cy="filterCount"]')
                .should('have.text', pendingApproval);
            cy.get('[data-cy="filter_approval"] [data-cy="filterCount"]')
                .should('have.text', approval);
            cy.get('[data-cy="filter_honorSystem"] [data-cy="filterCount"]')
                .should('have.text', honorSystem);
            cy.get('[data-cy="filter_quiz"] [data-cy="filterCount"]')
                .should('have.text', quiz);
            cy.get('[data-cy="filter_survey"] [data-cy="filterCount"]')
                .should('have.text', survey);
            cy.get('[data-cy="filter_video"] [data-cy="filterCount"]')
                .should('have.text', video);
            if (belongsToBadge !== null) {
                cy.get('[data-cy="filter_belongsToBadge"] [data-cy="filterCount"]')
                    .should('have.text', belongsToBadge);
            } else {
                cy.get('[data-cy="filter_belongsToBadge"] [data-cy="filterCount"]').should('not.exist');
            }
        });

        cy.intercept('GET', '/api/projects/proj1/subjects/subj1/summary?includeSkills=true')
            .as('getSkills');
    });

    it('counts in the filter', () => {
        Cypress.Commands.add('refreshCounts', () => {
            cy.get('[data-cy="skillProgressTitle-skill1"] [data-cy="skillProgressTitle"]').click();
            cy.cdBack('Subject 1');
        });

        cy.createSkill(1, 1, 1);

        cy.cdVisit('/?internalBackButton=true', false);
        cy.cdClickSubj(0);
        cy.validateCounts(1, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.refreshCounts();
        cy.validateCounts(4, 0, 0, 0, 0, 0, 0, 0, 0, 0);

        cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });
        cy.refreshCounts();
        cy.validateCounts(6, 0, 0, 0, 0, 1, 1, 0, 0, 0);

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.refreshCounts();
        cy.validateCounts(5, 0, 1, 0, 0, 1, 1, 0, 0, 0);

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.refreshCounts();
        cy.validateCounts(4, 0, 2, 0, 0, 1, 1, 0, 0, 0);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.refreshCounts();
        cy.validateCounts(3, 1, 2, 0, 0, 1, 1, 0, 0, 0);

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.refreshCounts();
        cy.validateCounts(3, 2, 1, 0, 0, 1, 1, 0, 0, 0);

        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'now');
        cy.refreshCounts();
        cy.validateCounts(3, 2, 1, 1, 0, 1, 1, 0, 0, 0);

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.createBadge(1, 1, { enabled: true });
        cy.refreshCounts();
        cy.validateCounts(3, 2, 1, 1, 2, 1, 1, 0, 0, 0);

        cy.createQuizDef(1);
        cy.createSkill(1, 1, 7, {
            name: 'g some quiz skill',
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            numPerformToCompletion: 1
        });
        cy.refreshCounts();
        cy.validateCounts(4, 2, 1, 1, 2, 1, 1, 1, 0, 0);

        cy.createSurveyDef(2, { name: 'Test survey' });
        cy.createSkill(1, 1, 8, {
            name: 'h some survey skill',
            selfReportingType: 'Quiz',
            quizId: 'quiz2',
            numPerformToCompletion: 1
        });
        cy.refreshCounts();
        cy.validateCounts(5, 2, 1, 1, 2, 1, 1, 1, 1, 0);

        const testVideo = '/static/videos/create-quiz.mp4'
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1, description: 'blah blah'})
        const vidAttr = { videoUrl: testVideo, captions: 'some', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, description: 'blah blah', selfReportingType: 'Video' });

        cy.refreshCounts();
        cy.validateCounts(5, 2, 1, 1, 2, 1, 1, 1, 1, 1);
    });

    it('filter skills', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'now');
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.createBadge(1, 1, { enabled: true });

        cy.cdVisit('/', true);
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_withoutProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Without Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 5');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 6');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_attributeGroups"]').click()
        cy.get('[data-cy="filter_complete"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Completed');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 4');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_pendingApproval"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Pending Approval');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 5');
        cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_belongsToBadge"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Belongs to a Badge');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-2"]').should('not.exist')

    });

    it('filter expanded skills', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_withoutProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Without Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 5');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 6');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('Very Great Skill 4');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-4"]')
            .should('not.exist');
    });

    it('expand skills after filtering', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_withoutProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Without Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 5');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Overall Points Earned')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 6');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Overall Points Earned')
            .should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        // expand
        cy.get('[data-cy=toggleSkillDetails]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 5');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 6');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('Very Great Skill 4');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('Overall Points Earned');
        cy.get('[data-cy="skillProgress_index-4"]')
            .should('not.exist');

    });

    it('clear filter', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 6, { selfReportingType: 'HonorSystem' });

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/', true);
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('Very Great Skill 4');
        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('Very Great Skill 5');
        cy.get('[data-cy="skillProgress_index-5"]')
            .contains('Very Great Skill 6');
        cy.get('[data-cy="skillProgress_index-6"]')
            .should('not.exist');

        // filter
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_withoutProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Without Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 5');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 6');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        // clear
        cy.get('[data-cy="selectedFilter"] [data-pc-section="removeicon"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .should('not.exist');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('Very Great Skill 4');
        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('Very Great Skill 5');
        cy.get('[data-cy="skillProgress_index-5"]')
            .contains('Very Great Skill 6');
        cy.get('[data-cy="skillProgress_index-6"]')
            .should('not.exist');
    });

    it('search skills', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, { name: 'sEEk bLaH skill 5' });
        cy.createSkill(1, 1, 6, { name: 'some other skill 6' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');
        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-5"]')
            .contains('skill 6');
        cy.get('[data-cy="skillProgress_index-6"]')
            .should('not.exist');

        cy.get('[data-cy="skillsSearchInput"]')
            .type('bLaH ');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-3"]')
            .should('not.exist');

        cy.get('[data-cy="skillsSearchInput"]')
            .type('O');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-1"]')
            .should('not.exist');

        cy.get('[data-cy="skillsSearchInput"]')
            .clear()
            .type('se');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 4');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-3"]')
            .should('not.exist');

        cy.get('[data-cy="skillsSearchInput"]')
            .type('e');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-1"]')
            .should('not.exist');
    });

    it('search skills no results', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, { name: 'sEEk bLaH skill 5' });
        cy.createSkill(1, 1, 6, { name: 'some other skill 6' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');
        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-5"]')
            .contains('skill 6');
        cy.get('[data-cy="skillProgress_index-6"]')
            .should('not.exist');

        cy.get('[data-cy="skillsSearchInput"]')
            .type('bbbbbb ');
        cy.get('[data-cy=noContent]')
            .should('be.visible')
            .contains('No results');
        cy.get('[data-cy=noContent]')
          .contains('Please refine [bbbbbb ] search');
    });

    it('ability to clear search string', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, { name: 'sEEk bLaH skill 5' });
        cy.createSkill(1, 1, 6, { name: 'some other skill 6' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillsSearchInput"]')
            .type('bLaH ');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-3"]')
            .should('not.exist');

        cy.get('[data-cy="clearSkillsSearchInput"]')
            .click();
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');
        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-5"]')
            .contains('skill 6');
        cy.get('[data-cy="skillProgress_index-6"]')
            .should('not.exist');
    });

    it('search produces no results', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillsSearchInput"]')
            .type('bLaH1 ');
        cy.get('[data-cy="skillProgress_index-0"]')
            .should('not.exist');

        cy.get('[ data-cy="noContent"]')
            .contains('No results');
        cy.get('[ data-cy="noContent"]')
            .contains('Please refine [bLaH1 ] search');
    });

    it('search and filter produces no results', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillsSearchInput"]')
            .type('bLaH1 ');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_withoutProgress"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .should('not.exist');
        cy.get('[ data-cy="noContent"]')
            .contains('No results');
        cy.get('[ data-cy="noContent"]')
            .contains('Please refine [bLaH1 ] search and/or clear the selected filter');
    });

    it('filter then search', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');
        cy.get('[data-cy="skillProgress_index-4"]')
            .should('not.exist');

        cy.get('[data-cy="skillsSearchInput"]')
            .type('b');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');
    });

    it('search then filter', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillsSearchInput"]')
            .type('b');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-3"]')
            .should('not.exist');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');
    });

    it('search should still apply after filter is cleared', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillsSearchInput"]')
            .type('b');
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');

        cy.get('[data-cy="selectedFilter"] [data-pc-section="removeicon"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .should('not.exist');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-3"]')
            .should('not.exist');
    });

    it('filter should still apply after search is cleared', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillsSearchInput"]')
            .type('b');
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');

        cy.get('[data-cy="clearSkillsSearchInput"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');
        cy.get('[data-cy="skillProgress_index-4"]')
            .should('not.exist');
    });

    it('change filter with search', () => {
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillsSearchInput"]')
            .type('b');
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_selfReportGroups"]').click()
        cy.get('[data-cy="filter_approval"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-1"]')
            .should('not.exist');
    });

    it('filter skills on badge catalog page', () => {
        cy.createSkill(1, 1, 1, { name: 'a Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'b is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'c find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'd Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'e sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'f some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.assignSkillToBadge(1, 1, 3);
        cy.assignSkillToBadge(1, 1, 4);
        cy.assignSkillToBadge(1, 1, 5);
        cy.enableBadge(1, 1);

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/', true);
        cy.cdClickBadges();
        cy.get('[data-cy="badgeDetailsLink_badge1"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');
        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-5"]')
            .should('not.exist');

        cy.validateCounts(1, 2, 2, 0, null, 1, 0, 0, 0, 0);

        cy.get('[data-cy="filter_complete"]')
            .click();
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        cy.get('[data-cy="skillsSearchInput"]')
            .type('other');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-1"]')
            .should('not.exist');
    });

    it('filter skills on completed badge page', () => {
        cy.createSkill(1, 1, 1, { name: 'a Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'b is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'c find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'd Search nothing skill 4' });

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.assignSkillToBadge(1, 1, 3);
        cy.assignSkillToBadge(1, 1, 4);
        cy.enableBadge(1, 1);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/', true);
        cy.cdClickBadges();
        cy.contains('Badges');
        cy.get('[data-cy="earnedBadgeLink_badge1"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');

        cy.validateCounts(0, 4, 0, 0, null, 0, 0, 0, 0, 0);

        cy.get('[data-cy="filter_complete"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');

        cy.get('[data-cy="skillsSearchInput"]')
            .type('blah');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');
    });

    it('filter skills on global badge page', () => {
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                }
            });
        cy.loginAsProxyUser();
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'a Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'b is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'c find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'd Search nothing skill 4' });

        cy.loginAsRootUser();

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1);
        cy.assignSkillToGlobalBadge(1, 2);
        cy.assignSkillToGlobalBadge(1, 3);
        cy.assignSkillToGlobalBadge(1, 4);
        cy.enableGlobalBadge();

        cy.loginAsProxyUser();

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/', true);
        cy.cdClickBadges();
        cy.get('[data-cy="earnedBadgeLink_globalBadge1"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');

        cy.validateCounts(0, 4, 0, 0, null, 0, 0, 0, 0, 0);

        cy.get('[data-cy="filter_complete"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');

        cy.get('[data-cy="skillsSearchInput"]')
            .type('blah');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');
    });

    it('Last Viewed button must be disabled if last visited skill was filtered out', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="skillProgressTitle"]').should('exist')
        cy.cdBack('Subject 1');
        cy.wait('@getSkills');

        cy.get('[data-cy="jumpToLastViewedButton"]').should('be.enabled')

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_approval"]')
            .click({force: true});
        cy.get('[data-cy="selectedFilter"]')
            .contains('Approval');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-11"]')
            .should('not.exist');

        cy.get('[data-cy="jumpToLastViewedButton"]').should('be.disabled')

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_attributeGroups"]').click()
        cy.get('[data-cy="filter_belongsToBadge"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('Belongs to a Badge');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')

        cy.get('[data-cy="jumpToLastViewedButton"]').should('be.enabled')
    });

    it('Visual Test skills search and skills filter selected', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.assignSkillToBadge(1, 1, 3);
        cy.assignSkillToBadge(1, 1, 4);
        cy.assignSkillToBadge(1, 1, 5);

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillsSearchInput"]')
            .type('blah');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();
        cy.get('[data-cy="filter_inProgress"]')
            .click();
        cy.get('[data-cy="selectedFilter"]')
            .contains('In Progress');

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('100 / 200');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('100 / 200');
        cy.get('[data-cy="skillProgress_index-2"]')
            .should('not.exist');

        cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"]', { errorThreshold: 0.05 });
    });

    it('Visual Test skills filter open', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { name: 'Search blah skill 1' });
        cy.createSkill(1, 1, 2, { name: 'is a skill 2' });
        cy.createSkill(1, 1, 3, { name: 'find Blah other skill 3' });
        cy.createSkill(1, 1, 4, { name: 'Search nothing skill 4' });
        cy.createSkill(1, 1, 5, {
            name: 'sEEk bLaH skill 5',
            selfReportingType: 'Approval'
        });
        cy.createSkill(1, 1, 6, {
            name: 'some other skill 6',
            selfReportingType: 'HonorSystem'
        });

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.assignSkillToBadge(1, 1, 3);
        cy.assignSkillToBadge(1, 1, 4);
        cy.assignSkillToBadge(1, 1, 5);

        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday');

        cy.cdVisit('/subjects/subj1');
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
            .click();

        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('skill 1');
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('200 / 200');

        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('skill 2');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('100 / 200');

        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('skill 3');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('100 / 200');

        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('skill 4');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('0 / 200');

        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('skill 5');
        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('0 / 200');

        cy.get('[data-cy="skillProgress_index-5"]')
            .contains('skill 6');
        cy.get('[data-cy="skillProgress_index-5"]')
            .contains('0 / 200');

        cy.get('[data-cy="skillProgress_index-6"]')
            .should('not.exist');
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', { blackout: '[data-cy=pointHistoryChart]', errorThreshold: 0.05 });
    });

    it('Visual Tests filter selected and last viewed button present', () => {

        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="skillProgressTitle"]').should('exist')
        cy.cdBack('Subject 1');
        cy.wait('@getSkills');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
        cy.get('[data-cy="filter_approval"]').click({force: true});
        cy.get('[data-cy="selectedFilter"]').contains('Approval');
        cy.matchSnapshotImageForElement('[data-cy="skillsProgressList"] [data-pc-section="header"]');
    });

    it('filter on has tag', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.addTagToSkills(1, ['skill1', 'skill3'], 1);

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3');

        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
        cy.get('[data-cy="filter_attributeGroups"]').click();
        cy.get('[data-cy="filter_hasTag"] [data-cy="filterCount"]').should('have.text', '2');
        cy.get('[data-cy="filter_hasTag"]').click();
        cy.get('[data-cy="selectedFilter"]').contains('Has a Tag');

        cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 3');
        cy.get('[data-cy="skillProgress_index-2"]').should('not.exist');
    });

    it('no results because of the filters', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.createBadge(1, 1, { enabled: true});

        cy.addTagToSkills(1, ['skill1'], 1);

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3');

        cy.get('[data-cy="skillTag-0"] [data-cy="addTagBtn"]').click()
        cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
        cy.get('[data-cy="filter_attributeGroups"]').click();
        cy.get('[data-cy="filter_belongsToBadge"] [data-cy="filterCount"]').should('have.text', '1')
        cy.get('[data-cy="filter_belongsToBadge"]').click();

        cy.get('[data-cy="selectedFilter"]').contains('Belongs to a Badge');

        cy.get('[data-cy="skillProgress_index-0"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-1"]').should('not.exist');
        cy.get('[data-cy="skillProgress_index-2"]').should('not.exist');
    });

});
