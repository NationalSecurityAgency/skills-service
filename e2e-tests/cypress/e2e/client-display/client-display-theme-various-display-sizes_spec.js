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
import { matchSkillsDisplaySnapshot, visitSkillsDisplaySnapshotPage } from '../../support/skills-display-snapshot-helpers';

describe('Client Display Tests', () => {

    // Sample representative layouts at each size rather than repeating every themed page.
    const sizes = [
        'iphone-6',
        'ipad-2',
        'default',
    ];

    const matchThemeSnapshot = (options = {}) => {
        return matchSkillsDisplaySnapshot('[data-cy="testDisplayTheme"]', options);
    };

    before(() => {
        Cypress.Commands.add('cdInitProjWithSkills', () => {
            cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
                projectId: 'proj1',
                subjectId: 'subj1',
                name: 'Subject 1',
                helpUrl: 'http://doHelpOnThisSubject.com',
                iconClass: 'fas fa-jedi',
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
            });
            cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
                projectId: 'proj1',
                subjectId: 'subj2',
                name: 'Subject 2',
                iconClass: 'fas fa-ghost',
            });
            cy.request('POST', '/admin/projects/proj1/subjects/subj3', {
                projectId: 'proj1',
                subjectId: 'subj3',
                name: 'Subject 3'
            });
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: 'skill1',
                name: `This is 1`,
                type: 'Skill',
                pointIncrement: 100,
                numPerformToCompletion: 5,
                pointIncrementInterval: 0,
                numMaxOccurrencesIncrementInterval: -1,
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
                version: 0,
                helpUrl: 'http://doHelpOnThisSkill.com'
            });

            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: 'skill2',
                name: `This is 2`,
                type: 'Skill',
                pointIncrement: 100,
                numPerformToCompletion: 5,
                pointIncrementInterval: 0,
                numMaxOccurrencesIncrementInterval: -1,
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
                version: 0,
                helpUrl: 'http://doHelpOnThisSkill.com'
            });
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill3`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: 'skill3',
                name: `This is 3`,
                type: 'Skill',
                pointIncrement: 100,
                numPerformToCompletion: 2,
                pointIncrementInterval: 0,
                numMaxOccurrencesIncrementInterval: -1,
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
                version: 0,
                helpUrl: 'http://doHelpOnThisSkill.com'
            });

            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill4`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: 'skill4',
                name: `This is 4`,
                type: 'Skill',
                pointIncrement: 100,
                numPerformToCompletion: 2,
                pointIncrementInterval: 0,
                numMaxOccurrencesIncrementInterval: -1,
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
                version: 0,
                helpUrl: 'http://doHelpOnThisSkill.com'
            });
            cy.addLearningPathItem(1, 2, 4)

            cy.request('POST', '/admin/projects/proj1/badges/badge1', {
                projectId: 'proj1',
                badgeId: 'badge1',
                name: 'Badge 1',
                'iconClass': 'fas fa-ghost',
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            });

            cy.request('POST', '/admin/projects/proj1/badges/badge2', {
                projectId: 'proj1',
                badgeId: 'badge2',
                name: 'Badge 2',
                'iconClass': 'fas fa-monument',
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            });

            cy.request('POST', '/admin/projects/proj1/badges/badge3', {
                projectId: 'proj1',
                badgeId: 'badge3',
                name: 'Badge 3',
                'iconClass': 'fas fa-jedi',
                description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            });

            cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1');
            cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill2');
            cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill3');
            cy.enableBadge(1, 1);

            cy.request('POST', '/admin/projects/proj1/badge/badge2/skills/skill3');
            cy.enableBadge(1, 2, { 'iconClass': 'fas fa-monument' });

            cy.request('POST', '/admin/projects/proj1/badge/badge3/skills/skill2');
            cy.enableBadge(1, 3, { 'iconClass': 'fas fa-jedi' });

            const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: Cypress.env('proxyUser'),
                timestamp: m.clone()
                    .add(1, 'day')
                    .format('x')
            });
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: Cypress.env('proxyUser'),
                timestamp: m.clone()
                    .add(2, 'day')
                    .format('x')
            });

            cy.request('POST', `/api/projects/proj1/skills/skill3`, {
                userId: Cypress.env('proxyUser'),
                timestamp: m.clone()
                    .add(1, 'day')
                    .format('x')
            });
            cy.request('POST', `/api/projects/proj1/skills/skill3`, {
                userId: Cypress.env('proxyUser'),
                timestamp: m.clone()
                    .add(2, 'day')
                    .format('x')
            });

            cy.addTagToSkills(1, ['skill1', 'skill3'], 1)
            cy.addTagToSkills(1, ['skill2', 'skill3'], 2)
        });

    });

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
    });

    sizes.forEach((size) => {

        it(`test theming - project overview - ${size}`, () => {
            cy.setResolution(size);

            cy.cdInitProjWithSkills();

            visitSkillsDisplaySnapshotPage('/?enableTheme=true');

            cy.get('[data-cy=pointHistoryChart]').should('be.visible');

            cy.contains('Subject 3');
            cy.get('[data-cy="subjectTile-subj1"]')
                .contains('Subject 1');
            cy.get('[data-cy=myRank]')
                .contains('1');
            cy.get('[data-cy=myBadges]')
                .contains('1');
            matchThemeSnapshot({ blackout: '[data-cy=pointHistoryChart]', errorThreshold: 0.05 });
        });

        it(`test theming - subject overview with skill details - ${size}`, () => {
            cy.setResolution(size);

            cy.cdInitProjWithSkills();

            visitSkillsDisplaySnapshotPage('/subjects/subj1/?enableTheme=true');
            cy.contains('Subject 1');
            cy.get('[data-cy=myRank]')
                .contains('1');
            cy.contains('This is 4');
            cy.get('[data-cy="totalPoints"]').should('have.text', '1,400');

            cy.get('[data-cy=toggleSkillDetails]')
                .click();
            cy.get('[data-cy=myRank]')
                .contains('1');
            cy.contains('Lorem ipsum dolor sit amet');
            cy.contains('Skill has 1 direct prerequisite(s).');
            cy.get('[data-cy="totalPoints"]').should('have.text', '1,400');
            cy.contains('Description');

            cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 500 Points')
            cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 200 Points')
            matchThemeSnapshot({ blackout: '[data-cy=pointHistoryChart]', errorThreshold: 0.05 });
        });

    });

    // sizes came from https://docs.cypress.io/api/commands/viewport#Syntax
    const expandedVerticalSizes = [
        {
            name: 'iphone-6',
            width: 375,
            height: 667 * 3,
        },
        {
            name: 'ipad-2',
            width: 768,
            height: 1024 * 2,
        },
        {
            name: 'default',
            width: 1000,
            height: 660 * 2,
        },
    ];
    expandedVerticalSizes.forEach((size) => {
        it(`test theming - skill details with deps - ${size.name}`, () => {
            // must set viewport to show entire canvas or it will not appear in the screenshot
            cy.viewport(size.width, size.height);

            cy.cdInitProjWithSkills();

            visitSkillsDisplaySnapshotPage('/subjects/subj1/?enableTheme=true');
            cy.contains('Subject 1');

            cy.cdClickSkill(3);
            cy.contains('Skill Overview');
            cy.contains('This is 4');
            cy.contains('Lorem ipsum dolor sit amet');
            cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill2"]').should('be.visible');
            cy.get('[data-cy="prerequisitesGraph"]', { timeout: 10000 })
                .should('have.attr', 'data-render-ready', 'true');
            matchThemeSnapshot({ blackout: '[data-cy=pointHistoryChart]', errorThreshold: 0.05 });
        });
    });

});
