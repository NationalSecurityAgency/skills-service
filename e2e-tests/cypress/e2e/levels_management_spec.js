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
// const attachFiles = require('cypress-form-data-with-file-upload');
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Levels Management Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
    });

    it('default levels', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Levels');

        const tableSelector = '[data-cy=levelsTable]';
        const expected = [
            [{
                colIndex: 0,
                value: 1
            }, {
                colIndex: 1,
                value: '10'
            }, {
                colIndex: 2,
                value: 'Please create more skills first'
            }],
            [{
                colIndex: 0,
                value: 2
            }, {
                colIndex: 1,
                value: '25'
            }, {
                colIndex: 2,
                value: 'Please create more skills first'
            }],
            [{
                colIndex: 0,
                value: 3
            }, {
                colIndex: 1,
                value: '45'
            }, {
                colIndex: 2,
                value: 'Please create more skills first'
            }],
            [{
                colIndex: 0,
                value: 4
            }, {
                colIndex: 1,
                value: '67'
            }, {
                colIndex: 2,
                value: 'Please create more skills first'
            }],
            [{
                colIndex: 0,
                value: 5
            }, {
                colIndex: 1,
                value: '92'
            }, {
                colIndex: 2,
                value: 'Please create more skills first'
            }],
        ];
        cy.validateTable(tableSelector, expected, 5, true, null, false);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.clickNav('Levels');
        cy.validateTable(tableSelector, expected, 5, true, null, false);
    });

    it('once max levels are reached add level button should be disabled', () => {
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('setSettings');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill2',
            name: `This is 2`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.visit('/administrator/projects/proj1/');

        cy.clickNav('Settings');
        cy.get('[data-cy="usePointsForLevelsSwitch"]').click();
        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.wait('@setSettings');

        for (let i = 1; i < 20; i = i + 1) {
            cy.request('POST', '/admin/projects/proj1/levels/next', {
                points: `${i * 1000}`,
                'iconClass': 'fas fa-user-ninja',
            });
        }

        cy.clickNav('Levels');
        cy.get('[data-cy="addLevel"]')
            .click();
        cy.get('[data-cy="pointsInput"]')
            .type('20000');
        cy.get('[data-cy="saveDialogBtn"]')
            .click();

        cy.get('[data-cy="addLevel"]')
            .should('be.disabled');
    });

    it('warn if there are not enough points declared for a level', () => {
        cy.intercept('/admin/projects/proj1/levels').as('getLevels')

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill2',
            name: `This is 2`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.request('POST', '/admin/projects/proj1/settings/level.points.enabled', {
            projectId: 'proj1',
            setting: 'level.points.enabled',
            value: 'true'
        });

        cy.visit('/administrator/projects/proj1/levels');
        cy.wait('@getLevels')
        cy.get('[data-cy="levelsTable"]')

        cy.get('[data-cy="addLevel"]')
            .click();
        cy.get('[data-cy="pointsInput"]')
            .type('2000');
        cy.get('[data-cy="saveDialogBtn"]')
            .click();

        const tableSelector = '[data-cy=levelsTable] tbody tr';
        cy.get(tableSelector)
            .should('have.length', 6)
            .as('cyRows');

        cy.get('@cyRows')
            .eq(4)
            .find('td')
            .as('row5');
        cy.get('@row5')
            .find('.icon-warning')
            .should('not.exist');

        cy.get('@cyRows')
            .eq(5)
            .find('td')
            .as('row6');
        cy.get('@row6')
            .eq(0)
            .contains('6');
        cy.get('@row6')
            .contains('Level is unachievable. Insufficient available points in project');

        // do the same for a subject
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.clickNav('Levels');
        cy.get('[data-cy="addLevel"]')
            .click();
        cy.get('[data-cy="pointsInput"]')
            .type('2000');
        cy.get('[data-cy="saveDialogBtn"]')
            .click();

        cy.get(tableSelector)
            .should('have.length', 6)
            .as('cyRows');

        cy.get('@cyRows')
            .eq(4)
            .find('td')
            .as('row5');
        cy.get('@row5')
            .find('.icon-warning')
            .should('not.exist');

        cy.get('@cyRows')
            .eq(5)
            .find('td')
            .as('row6');
        cy.get('@row6')
            .eq(0)
            .contains('6');
        cy.get('@row6')
            .contains('Level is unachievable. Insufficient available points in project');
    });

    it('subject: once max levels are reached add level button should be disabled', () => {
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('setSettings');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill2',
            name: `This is 2`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.visit('/administrator/projects/proj1/');

        cy.clickNav('Settings');
        cy.get('[data-cy="usePointsForLevelsSwitch"]').click();
        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.wait('@setSettings');

        for (let i = 1; i < 20; i = i + 1) {
            cy.request('POST', '/admin/projects/proj1/subjects/subj1/levels/next', {
                points: `${i * 1000}`,
            });
        }

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.clickNav('Levels');
        cy.get('[data-cy="addLevel"]')
            .click();
        cy.get('[data-cy="pointsInput"]')
            .type('20000');
        cy.get('[data-cy="saveDialogBtn"]')
            .click();

        cy.get('[data-cy="addLevel"]')
            .should('be.disabled');
    });

    it('remove levels', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill2',
            name: `This is 2`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Levels');

        const tableSelector = '[data-cy=levelsTable]';
        const expected = [
            [{
                colIndex: 0,
                value: 1
            }, {
                colIndex: 1,
                value: '10'
            }, {
                colIndex: 2,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 2
            }, {
                colIndex: 1,
                value: '25'
            }, {
                colIndex: 2,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 3
            }, {
                colIndex: 1,
                value: '45'
            }, {
                colIndex: 2,
                value: '450'
            }],
            [{
                colIndex: 0,
                value: 4
            }, {
                colIndex: 1,
                value: '67'
            }, {
                colIndex: 2,
                value: '670'
            }],
            [{
                colIndex: 0,
                value: 5
            }, {
                colIndex: 1,
                value: '92'
            }, {
                colIndex: 2,
                value: '920'
            }],
        ];
        cy.validateTable(tableSelector, expected, 5, true, null, false);

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 1
            }, {
                colIndex: 1,
                value: '10'
            }, {
                colIndex: 2,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 2
            }, {
                colIndex: 1,
                value: '25'
            }, {
                colIndex: 2,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 3
            }, {
                colIndex: 1,
                value: '45'
            }, {
                colIndex: 2,
                value: '450'
            }],
            [{
                colIndex: 0,
                value: 4
            }, {
                colIndex: 1,
                value: '67'
            }, {
                colIndex: 2,
                value: '670'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 1
            }, {
                colIndex: 1,
                value: '10'
            }, {
                colIndex: 2,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 2
            }, {
                colIndex: 1,
                value: '25'
            }, {
                colIndex: 2,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 3
            }, {
                colIndex: 1,
                value: '45'
            }, {
                colIndex: 2,
                value: '450'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 1
            }, {
                colIndex: 1,
                value: '10'
            }, {
                colIndex: 2,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 2
            }, {
                colIndex: 1,
                value: '25'
            }, {
                colIndex: 2,
                value: '250'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 1
            }, {
                colIndex: 1,
                value: '10'
            }, {
                colIndex: 2,
                value: '100'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy=removeLevel]')
            .should('be.disabled');
    });

    it('attempt to remove achieved subject levels', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
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
            version: 0,
        });

        const now = dayjs();
        cy.reportSkill(1, 1, 'user@skills.org', now.format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill(1, 1, 'user@skills.org', now.subtract(1, 'day')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill(1, 1, 'user@skills.org', now.subtract(2, 'day')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill(1, 1, 'user@skills.org', now.subtract(3, 'day')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill(1, 1, 'user@skills.org', now.subtract(4, 'day')
            .format('YYYY-MM-DD HH:mm'), false);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.clickNav('Levels');

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();
        cy.contains('Unable to delete')
            .should('be.visible');
        cy.contains('Unable to delete level 5, 1 user has achieved this level')
            .should('be.visible');
    });

    it('attempt to remove achieved project levels', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
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
            version: 0,
        });

        const now = dayjs();
        cy.reportSkill(1, 1, 'user@skills.org', now.format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill(1, 1, 'user@skills.org', now.subtract(1, 'day')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill(1, 1, 'user@skills.org', now.subtract(2, 'day')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill(1, 1, 'user@skills.org', now.subtract(3, 'day')
            .format('YYYY-MM-DD HH:mm'), false);
        cy.reportSkill(1, 1, 'user@skills.org', now.subtract(4, 'day')
            .format('YYYY-MM-DD HH:mm'), false);

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Levels');

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();
        cy.contains('Unable to delete')
            .should('be.visible');
        cy.contains('Unable to delete level 5, 1 user has achieved this level')
            .should('be.visible');
    });

    it('subject: remove levels', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: 'Subject 2'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill2',
            name: `This is 2`,
            type: 'Skill',
            pointIncrement: 500,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj2/skills/skill3`, {
            projectId: 'proj1',
            subjectId: 'subj2',
            skillId: 'skill3',
            name: `This is 3`,
            type: 'Skill',
            pointIncrement: 1000,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.clickNav('Levels');

        const tableSelector = '[data-cy=levelsTable]';
        const expected = [
            [{
                colIndex: 0,
                value: 1
            }, {
                colIndex: 1,
                value: '10'
            }, {
                colIndex: 2,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 2
            }, {
                colIndex: 1,
                value: '25'
            }, {
                colIndex: 2,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 3
            }, {
                colIndex: 1,
                value: '45'
            }, {
                colIndex: 2,
                value: '450'
            }],
            [{
                colIndex: 0,
                value: 4
            }, {
                colIndex: 1,
                value: '67'
            }, {
                colIndex: 2,
                value: '670'
            }],
            [{
                colIndex: 0,
                value: 5
            }, {
                colIndex: 1,
                value: '92'
            }, {
                colIndex: 2,
                value: '920'
            }],
        ];
        cy.validateTable(tableSelector, expected, 5, true, null, false);

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.get('[data-cy=removeLevel]')
            .click();
        cy.contains('YES, Delete It')
            .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 1
            }, {
                colIndex: 1,
                value: '10'
            }, {
                colIndex: 2,
                value: '100'
            }],
        ], 5, true, null, false);

        cy.get('[data-cy=removeLevel]')
            .should('be.disabled');

        // make sure subject 2 is not affected
        cy.visit('/administrator/projects/proj1/subjects/subj2');
        cy.clickNav('Levels');
        cy.validateTable(tableSelector, expected, 5, true, null, false);
    });

    it('subjects: levels validation', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        })
            .as('loadSubject');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');

        cy.contains('Levels')
            .click();
        cy.get('[data-cy=editLevelButton]')
            .first()
            .click();
        cy.get('[data-cy=percent]')
            .type('{selectall}1000');
        cy.get('[data-cy=percentError]')
            .contains('Percent must be less than or equal to 100');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');

        cy.get('[data-cy=percent')
            .type('{selectall}50');
        cy.get('[data-cy=percentError')
            .contains('Percent must not overlap with other levels');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.get('[data-cy=closeDialogBtn]')
            .click();

    });

    it('new level dialog should return focus to new level button', () => {
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/MyNewtestProject'
        })
            .as('loadProject');

        cy.intercept({
            method: 'PUT',
            url: '/admin/projects/MyNewtestProject/levels/edit/**'
        })
            .as('saveLevel');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/MyNewtestProject/levels'
        })
            .as('loadLevels');

        cy.request('POST', '/app/projects/MyNewtestProject', {
            projectId: 'MyNewtestProject',
            name: 'My New test Project'
        });

        cy.visit('/administrator/projects/MyNewtestProject/');
        cy.wait('@loadProject');

        cy.contains('Levels')
            .click();
        cy.get('[data-cy=addLevel]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy=addLevel]')
            .should('have.focus');

        // cy.get('[data-cy=addLevel]').click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=addLevel]').should('have.focus');

        cy.get('[data-cy=addLevel]')
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[data-cy=addLevel]')
            .should('have.focus');

        cy.get('[data-cy=editLevelButton]')
            .eq(0)
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy=editLevelButton]')
            .eq(0)
            .should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(0).click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]')
            .eq(0)
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[data-cy=editLevelButton]')
            .eq(0)
            .should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(0).click();
        // cy.get('[data-cy=levelName]').type('{selectall}Fooooooo');
        // cy.get('[data-cy=saveDialogBtn]').click();
        // cy.wait('@saveLevel');
        // cy.wait('@loadLevels');
        // cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]')
            .eq(3)
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy=editLevelButton]')
            .eq(3)
            .should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(3).click();
        // cy.get('[data-cy=levelName]').type('{esc}');
        // cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

        cy.get('[data-cy=editLevelButton]')
            .eq(3)
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[data-cy=editLevelButton]')
            .eq(3)
            .should('have.focus');

        // cy.get('[data-cy=editLevelButton]').eq(3).click();
        // cy.get('[data-cy=levelName]').type('{selectall}Baaaaar');
        // cy.get('[data-cy=saveDialogBtn]').click();
        // cy.wait('@saveLevel');
        // cy.wait('@loadLevels');
        // cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');
    });

});
