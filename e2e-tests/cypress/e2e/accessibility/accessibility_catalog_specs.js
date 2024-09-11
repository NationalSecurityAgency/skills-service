/*
 * Copyright 2024 SkillTree
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
import dayjs from 'dayjs';

const moment = require('moment-timezone');

describe('Accessibility Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/MyNewtestProject', {
            projectId: 'MyNewtestProject',
            name: 'My New test Project'
        });

        cy.request('POST', '/app/projects/MyNewtestProject2', {
            projectId: 'MyNewtestProject2',
            name: 'My New test Project2'
        });

        cy.request('POST', '/admin/projects/MyNewtestProject/subjects/subj1', {
            projectId: 'MyNewtestProject',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        cy.request('POST', '/admin/projects/MyNewtestProject/badges/badge1', {
            projectId: 'MyNewtestProject',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill1`, {
            projectId: 'MyNewtestProject',
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

        cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill2`, {
            projectId: 'MyNewtestProject',
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

        cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill3`, {
            projectId: 'MyNewtestProject',
            subjectId: 'subj1',
            skillId: 'skill3',
            name: `This is 3`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com',
            selfReportingType: 'Approval'
        });
        cy.request('POST', `/admin/projects/MyNewtestProject/subjects/subj1/skills/skill4`, {
            projectId: 'MyNewtestProject',
            subjectId: 'subj1',
            skillId: 'skill4',
            name: `This is 4`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com',
        });

        cy.request('POST', '/admin/projects/MyNewtestProject/badge/badge1/skills/skill2');

        cy.request('POST', `/admin/projects/MyNewtestProject/skill2/prerequisite/MyNewtestProject/skill1`);

        const m = moment('2020-05-12 11', 'YYYY-MM-DD HH');
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u1',
            timestamp: m.format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u2',
            timestamp: m.subtract(4, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u3',
            timestamp: m.subtract(3, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u4',
            timestamp: m.subtract(2, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill1`, {
            userId: 'u5',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill3`, {
            userId: 'u5',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill3`, {
            userId: 'u6',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill3`, {
            userId: 'u7',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });

        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(1, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(2, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(3, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(4, 'day')
                .format('x')
        });
        cy.request('POST', `/api/projects/MyNewtestProject/skills/skill4`, {
            userId: 'u8',
            timestamp: m.subtract(5, 'day')
                .format('x')
        });
    });

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {
        it(`catalog page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createProject(1);
            cy.createSubject(1, 1);

            cy.createSkill(1, 1, 1);
            cy.createSkill(1, 1, 2);
            cy.createSkill(1, 1, 3);

            cy.exportSkillToCatalog(1, 1, 1);
            cy.exportSkillToCatalog(1, 1, 2);
            cy.exportSkillToCatalog(1, 1, 3);

            cy.visit('/administrator/projects/proj1/skills-catalog');
            cy.injectAxe();
            cy.validateTable('[data-cy="exportedSkillsTable"]', [
                [{
                    colIndex: 1,
                    value: 'Very Great Skill 3'
                }],
                [{
                    colIndex: 1,
                    value: 'Very Great Skill 2'
                }],
                [{
                    colIndex: 1,
                    value: 'Very Great Skill 1'
                }],
            ], 5);
            cy.customLighthouse();
            cy.customA11y();

            // Delete from Catalog modal
            cy.get('[data-cy="deleteSkillButton_skill2"]')
              .click();
            cy.contains('This will PERMANENTLY remove [Very Great Skill 2] Skill');
            cy.customLighthouse();
            cy.customA11y();
        });

        it(`import from catalog modal${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createProject(1);
            cy.createSubject(1, 1);

            cy.createSkill(1, 1, 1, { description: '# This is where description goes\n\ntest test' });
            cy.createSkill(1, 1, 2);
            cy.createSkill(1, 1, 3);

            cy.exportSkillToCatalog(1, 1, 1);
            cy.exportSkillToCatalog(1, 1, 2);
            cy.exportSkillToCatalog(1, 1, 3);

            cy.createProject(2);
            cy.createSubject(2, 1);

            cy.visit('/administrator/projects/proj2/subjects/subj1');
            cy.injectAxe();
            cy.get('[data-cy="importFromCatalogBtn"]')
              .click();
            // cy.get('[data-cy="expandDetailsBtn_proj1_skill1"]')
            //     .click();
            cy.get(`[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
            cy.contains('This is where description goes');

            cy.customLighthouse();
            cy.customA11y();
        });

        it(`export to catalog modal${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createProject(1);
            cy.createSubject(1, 1);

            cy.createSkill(1, 1, 1);
            cy.createSkill(1, 1, 2);
            cy.createSkill(1, 1, 3);

            cy.exportSkillToCatalog(1, 1, 1);
            cy.exportSkillToCatalog(1, 1, 2);
            cy.exportSkillToCatalog(1, 1, 3);

            cy.createProject(2);
            cy.createSubject(2, 1);
            cy.createSkill(2, 1, 1);
            cy.createSkill(2, 1, 2, { name: 'Something Else' });
            cy.createSkill(2, 1, 3, { skillId: 'diffId' });
            cy.createSkill(2, 1, 4);
            cy.createSkill(2, 1, 5);
            cy.createSkill(2, 1, 6);
            cy.addLearningPathItem(2, 6, 5)
            cy.createSkill(2, 1, 7);
            cy.exportSkillToCatalog(2, 1, 7);

            cy.visit('/administrator/projects/proj2/subjects/subj1');
            cy.get('[data-cy="skillActionsBtn"]')
            cy.injectAxe();

            cy.customLighthouse();
            cy.customA11y();

            // looks like AXE and PrimeVue lib disagree where `aria-selected="true"` can be applied
            // TODO: not really an issue but look into this further so this validation can be added back
            // cy.get('[data-cy="skillsTable"]  [data-pc-name="headercheckbox"]').click()
            //
            // cy.get('[data-cy="skillActionsBtn"]')
            //     .click();
            // cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
            // cy.contains('Note: The are already 1 skill(s) in the Skill Catalog from the provided selection.');
            // cy.contains('This will export 2 Skills');
            // cy.get('[data-cy="dupSkill-skill1"]')
            //     .contains('ID Conflict');
            // cy.get('[data-cy="dupSkill-skill1"]')
            //     .contains('Name Conflict');
            // cy.get('[data-cy="dupSkill-skill2"]')
            //     .contains('ID Conflict');
            // cy.get('[data-cy="dupSkill-diffId"]')
            //     .contains('Name Conflict');
            // cy.get('[data-cy="dupSkill-skill5"]')
            //     .contains('Has Prerequisites');
            //
            // cy.customLighthouse();
            // cy.customA11y();
        });
    })
});
