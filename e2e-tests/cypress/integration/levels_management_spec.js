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

describe('Levels Management Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('default levels', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/projects/proj1/');
        cy.clickNav('Levels');

        const tableSelector = '[data-cy=levelsTable]'
        const expected = [
            [{ colIndex: 0,  value: 1 }, { colIndex: 2,  value: '10' }, { colIndex: 3,  value: 'Please create more rules first' }],
            [{ colIndex: 0,  value: 2 }, { colIndex: 2,  value: '25' }, { colIndex: 3,  value: 'Please create more rules first' }],
            [{ colIndex: 0,  value: 3 }, { colIndex: 2,  value: '45' }, { colIndex: 3,  value: 'Please create more rules first' }],
            [{ colIndex: 0,  value: 4 }, { colIndex: 2,  value: '67' }, { colIndex: 3,  value: 'Please create more rules first' }],
            [{ colIndex: 0,  value: 5 }, { colIndex: 2,  value: '92' }, { colIndex: 3,  value: 'Please create more rules first' }],
        ]
        cy.validateTable(tableSelector, expected, 5, true, null, false);

        cy.visit('/projects/proj1/subjects/subj1');
        cy.clickNav('Levels');
        cy.validateTable(tableSelector, expected, 5, true, null, false);
    })

    it('once max levels are reached add level button should be disabled', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
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

        cy.visit('/projects/proj1/');

        cy.clickNav('Settings');
        cy.get('[data-cy="usePointsForLevelsSwitch"]').check({force: true});
        cy.get('[data-cy="saveSettingsBtn"]').click();

        for (let i = 1; i < 20; i = i + 1) {
            cy.request('POST', '/admin/projects/proj1/levels/next', {
                points: `${i * 1000}`,
                "iconClass":"fas fa-user-ninja",
            });
        }

        cy.clickNav('Levels');
        cy.get('[data-cy="addLevel"]').click();
        cy.get('[data-cy="newLevelPoints"]').type('20000');
        cy.get('[data-cy="saveLevelButton"]').click();

        cy.get('[data-cy="addLevel"]').should('be.disabled');
    });


    it('warn if there are not enough points declared for a level', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
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

        cy.visit('/projects/proj1/');

        cy.clickNav('Settings');
        cy.get('[data-cy="usePointsForLevelsSwitch"]').check({force: true});
        cy.get('[data-cy="saveSettingsBtn"]').click();

        cy.clickNav('Levels');
        cy.get('[data-cy="addLevel"]').click();
        cy.get('[data-cy="newLevelPoints"]').type('2000');
        cy.get('[data-cy="saveLevelButton"]').click();

        const tableSelector = '[data-cy=levelsTable] tbody tr'
        cy.get(tableSelector).should('have.length', 6).as('cyRows');

        cy.get('@cyRows').eq(4).find('td').as('row5');
        cy.get('@row5').find('.icon-warning').should('not.exist');

        cy.get('@cyRows').eq(5).find('td').as('row6');
        cy.get('@row6').eq(0).contains('6');
        cy.get('@row6').find('.icon-warning').click();
        cy.contains('Level is unachievable. Insufficient available points in project');

        // do the same for a subject
        cy.visit('/projects/proj1/subjects/subj1');
        cy.clickNav('Levels');
        cy.get('[data-cy="addLevel"]').click();
        cy.get('[data-cy="newLevelPoints"]').type('2000');
        cy.get('[data-cy="saveLevelButton"]').click();

        cy.get(tableSelector).should('have.length', 6).as('cyRows');

        cy.get('@cyRows').eq(4).find('td').as('row5');
        cy.get('@row5').find('.icon-warning').should('not.exist');

        cy.get('@cyRows').eq(5).find('td').as('row6');
        cy.get('@row6').eq(0).contains('6');
        cy.get('@row6').find('.icon-warning').click();
        cy.contains('Level is unachievable. Insufficient available points in project');
    });

    it('subject: once max levels are reached add level button should be disabled', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
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

        cy.visit('/projects/proj1/');

        cy.clickNav('Settings');
        cy.get('[data-cy="usePointsForLevelsSwitch"]').check({force: true});
        cy.get('[data-cy="saveSettingsBtn"]').click();

        for (let i = 1; i < 20; i = i + 1) {
            cy.request('POST', '/admin/projects/proj1/subjects/subj1/levels/next', {
                points: `${i * 1000}`,
                "iconClass":"fas fa-user-ninja",
            });
        }

        cy.visit('/projects/proj1/subjects/subj1');
        cy.clickNav('Levels');
        cy.get('[data-cy="addLevel"]').click();
        cy.get('[data-cy="newLevelPoints"]').type('20000');
        cy.get('[data-cy="saveLevelButton"]').click();

        cy.get('[data-cy="addLevel"]').should('be.disabled');
    });


    it('remove levels', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
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

        cy.visit('/projects/proj1/');
        cy.clickNav('Levels');

        const tableSelector = '[data-cy=levelsTable]'
        const expected = [
            [{ colIndex: 0,  value: 1 }, { colIndex: 2,  value: '10' }, { colIndex: 3,  value: '100' }],
            [{ colIndex: 0,  value: 2 }, { colIndex: 2,  value: '25' }, { colIndex: 3,  value: '250' }],
            [{ colIndex: 0,  value: 3 }, { colIndex: 2,  value: '45' }, { colIndex: 3,  value: '450' }],
            [{ colIndex: 0,  value: 4 }, { colIndex: 2,  value: '67' }, { colIndex: 3,  value: '670' }],
            [{ colIndex: 0,  value: 5 }, { colIndex: 2,  value: '92' }, { colIndex: 3,  value: '920' }],
        ]
        cy.validateTable(tableSelector, expected, 5, true, null, false);

        cy.get('[data-cy=removeLevel]').click();
        cy.contains('YES, Delete It').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 1 }, { colIndex: 2,  value: '10' }, { colIndex: 3,  value: '100' }],
            [{ colIndex: 0,  value: 2 }, { colIndex: 2,  value: '25' }, { colIndex: 3,  value: '250' }],
            [{ colIndex: 0,  value: 3 }, { colIndex: 2,  value: '45' }, { colIndex: 3,  value: '450' }],
            [{ colIndex: 0,  value: 4 }, { colIndex: 2,  value: '67' }, { colIndex: 3,  value: '670' }],
            ], 5, true, null, false);

        cy.get('[data-cy=removeLevel]').click();
        cy.contains('YES, Delete It').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 1 }, { colIndex: 2,  value: '10' }, { colIndex: 3,  value: '100' }],
            [{ colIndex: 0,  value: 2 }, { colIndex: 2,  value: '25' }, { colIndex: 3,  value: '250' }],
            [{ colIndex: 0,  value: 3 }, { colIndex: 2,  value: '45' }, { colIndex: 3,  value: '450' }],
        ], 5, true, null, false);

        cy.get('[data-cy=removeLevel]').click();
        cy.contains('YES, Delete It').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 1 }, { colIndex: 2,  value: '10' }, { colIndex: 3,  value: '100' }],
            [{ colIndex: 0,  value: 2 }, { colIndex: 2,  value: '25' }, { colIndex: 3,  value: '250' }],
        ], 5, true, null, false);


        cy.get('[data-cy=removeLevel]').click();
        cy.contains('YES, Delete It').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 1 }, { colIndex: 2,  value: '10' }, { colIndex: 3,  value: '100' }],
        ], 5, true, null, false);

        cy.get('[data-cy=removeLevel]').should('be.disabled');
    })


    it('subject: remove levels', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: "Subject 2"
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

        cy.visit('/projects/proj1/subjects/subj1');
        cy.clickNav('Levels');

        const tableSelector = '[data-cy=levelsTable]'
        const expected = [
            [{ colIndex: 0,  value: 1 }, { colIndex: 2,  value: '10' }, { colIndex: 3,  value: '100' }],
            [{ colIndex: 0,  value: 2 }, { colIndex: 2,  value: '25' }, { colIndex: 3,  value: '250' }],
            [{ colIndex: 0,  value: 3 }, { colIndex: 2,  value: '45' }, { colIndex: 3,  value: '450' }],
            [{ colIndex: 0,  value: 4 }, { colIndex: 2,  value: '67' }, { colIndex: 3,  value: '670' }],
            [{ colIndex: 0,  value: 5 }, { colIndex: 2,  value: '92' }, { colIndex: 3,  value: '920' }],
        ]
        cy.validateTable(tableSelector, expected, 5, true, null, false);

        cy.get('[data-cy=removeLevel]').click();
        cy.contains('YES, Delete It').click();

        cy.get('[data-cy=removeLevel]').click();
        cy.contains('YES, Delete It').click();

        cy.get('[data-cy=removeLevel]').click();
        cy.contains('YES, Delete It').click();

        cy.get('[data-cy=removeLevel]').click();
        cy.contains('YES, Delete It').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 1 }, { colIndex: 2,  value: '10' }, { colIndex: 3,  value: '100' }],
        ], 5, true, null, false);

        cy.get('[data-cy=removeLevel]').should('be.disabled');

        // make sure subject 2 is not affected
        cy.visit('/projects/proj1/subjects/subj2');
        cy.clickNav('Levels');
        cy.validateTable(tableSelector, expected, 5, true, null, false);
    })


    it('add new level without name, then add name', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.visit('/projects/proj1/');
        cy.clickNav('Levels');

        const rowSelector = '[data-cy=levelsTable] tbody tr'
        cy.get(rowSelector).should('have.length', 5).as('cyRows');

        cy.get('[data-cy=addLevel]').first().click();

        // add a level with no name initially
        cy.get('[data-cy=levelPercent]').type('95');
        cy.get('[data-cy=levelPercentError]').should('not.be.visible');
        cy.get('[data-cy=levelNameError]').should('not.be.visible');
        cy.get('[data-cy=saveLevelButton]').should('not.be.disabled');
        cy.get('[data-cy=saveLevelButton]').click();

        // verify the new row was added as expected
        cy.get(rowSelector).should('have.length', 6).as('cyRows');
        cy.get('@cyRows')
            .eq(5)
            .find('[data-cy=levelsTable_name]')
            .as('row6NameCol')
        cy.get('@cyRows')
            .eq(5)
            .find('td')
            .eq(2)
            .as('row6PercentCol')
        cy.get('@row6NameCol').should('be.empty')
        cy.get('@row6PercentCol').contains('95')

        // now give the level a name
        cy.get('[data-cy=editLevelButton]').eq(5).click();
        cy.get('[data-cy=levelName]').type('{selectall}Coral Belt');
        cy.get('[data-cy=levelNameError]').should('not.be.visible');
        cy.get('[data-cy=saveLevelButton]').should('not.be.disabled');
        cy.get('[data-cy=levelName]').type('{enter}');

        // verify that the new name is present
        cy.get('@row6NameCol').contains('Coral Belt')
    });

    it('subjects: add new level without name, then add name', () => {
        cy.server();
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.route({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');

        cy.visit('/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');

        cy.clickNav('Levels');

        const rowSelector = '[data-cy=levelsTable] tbody tr'
        cy.get(rowSelector).should('have.length', 5).as('cyRows');

        cy.get('[data-cy=addLevel]').first().click();

        // add a level with no name initially
        cy.get('[data-cy=levelPercent]').type('95');
        cy.get('[data-cy=levelPercentError]').should('not.be.visible');
        cy.get('[data-cy=levelNameError]').should('not.be.visible');
        cy.get('[data-cy=saveLevelButton]').should('not.be.disabled');
        cy.get('[data-cy=saveLevelButton]').click();

        // verify the new row was added as expected
        cy.get(rowSelector).should('have.length', 6).as('cyRows');
        cy.get('@cyRows')
          .eq(5)
          .find('[data-cy=levelsTable_name]')
          .as('row6NameCol')
        cy.get('@cyRows')
          .eq(5)
          .find('td')
          .eq(2)
          .as('row6PercentCol')
        cy.get('@row6NameCol').should('be.empty')
        cy.get('@row6PercentCol').contains('95')

        // now give the level a name
        cy.get('[data-cy=editLevelButton]').eq(5).click();
        cy.get('[data-cy=levelName]').type('{selectall}Coral Belt');
        cy.get('[data-cy=levelNameError]').should('not.be.visible');
        cy.get('[data-cy=saveLevelButton]').should('not.be.disabled');
        cy.get('[data-cy=levelName]').type('{enter}');

        // verify that the new name is present
        cy.get('@row6NameCol').contains('Coral Belt')
    });

    it('subjects: levels validation', () => {
        cy.server();
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.route({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');

        cy.visit('/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');

        cy.contains('Levels').click();
        cy.get('[data-cy=editLevelButton]').first().click();
        cy.get('[data-cy=levelPercent]').type('{selectall}1000');
        cy.get('[data-cy=levelPercentError]').contains('Percent must be 100 or less');
        cy.get('[data-cy=saveLevelButton]').should('be.disabled');
        cy.get('[data-cy=levelPercent]').type('{selectall}-1000');
        cy.get('[data-cy=levelPercentError]').contains('Percent may only contain numeric characters.');
        cy.get('[data-cy=saveLevelButton]').should('be.disabled');
        cy.get('[data-cy=levelPercent').type('{selectall}50')
        cy.get('[data-cy=levelPercentError').contains('Percent must not overlap with other levels');
        cy.get('[data-cy=saveLevelButton]').should('be.disabled');
        cy.get('[data-cy=cancelLevel]').click();

        cy.contains('Add Next').click();
        cy.get('[data-cy=levelName]').type('Black Belt');
        cy.get('[data-cy=levelNameError').contains('Name is already taken.');
        cy.get('[data-cy=saveLevelButton]').should('be.disabled');
        const invalidName = Array(1000).fill('a').join('');
        cy.get('[data-cy=levelName]').invoke('val', invalidName).trigger('input');
        cy.get('[data-cy=levelNameError').contains('Name cannot exceed 50 characters.');
        cy.get('[data-cy=saveLevelButton]').should('be.disabled');

        cy.get('[data-cy=levelName]').type('{selectall}Coral Belt');
        cy.get('[data-cy=levelNameError]').should('not.be.visible');
        cy.get('[data-cy=levelPercent]').type('{selectall}5');
        cy.get('[data-cy=levelPercentError]').contains('Percent % must not overlap with other levels');
        cy.get('[data-cy=saveLevelButton]').should('be.disabled');
    });

    it('new level dialog should return focus to new level button', () => {
        cy.server();
        cy.route({
            method: 'GET',
            url: '/admin/projects/MyNewtestProject'
        }).as('loadProject');

        cy.route({
            method: 'PUT',
            url: '/admin/projects/MyNewtestProject/levels/edit/**'
        }).as('saveLevel');

        cy.route({
            method: 'GET',
            url: '/admin/projects/MyNewtestProject/levels'
        }).as('loadLevels');

        cy.request('POST', '/app/projects/MyNewtestProject', {
            projectId: 'MyNewtestProject',
            name: "My New test Project"
        })

        cy.visit('/projects/MyNewtestProject/');
        cy.wait('@loadProject');

        cy.contains('Levels').click();
        cy.get('[data-cy=addLevel]').click();
        cy.get('[data-cy=cancelLevel]').click();
        cy.get('[data-cy=addLevel]').should('have.focus');

        cy.get('[data-cy=addLevel]').click();
        cy.get('[data-cy=levelName]').type('{esc}');
        cy.get('[data-cy=addLevel]').should('have.focus');

        cy.get('[data-cy=addLevel]').click();
        cy.get('[aria-label=Close]').filter('.text-light').click();
        cy.get('[data-cy=addLevel]').should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(0).click();
        cy.get('[data-cy=cancelLevel]').click();
        cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(0).click();
        cy.get('[data-cy=levelName]').type('{esc}');
        cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(0).click();
        cy.get('[aria-label=Close]').filter('.text-light').click();
        cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(0).click();
        cy.get('[data-cy=levelName]').type('{selectall}Fooooooo');
        cy.get('[data-cy=saveLevelButton]').click();
        cy.wait('@saveLevel');
        cy.wait('@loadLevels');
        cy.get('[data-cy=editLevelButton]').eq(0).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(3).click();
        cy.get('[data-cy=cancelLevel]').click();
        cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(3).click();
        cy.get('[data-cy=levelName]').type('{esc}');
        cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(3).click();
        cy.get('[aria-label=Close]').filter('.text-light').click();
        cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');

        cy.get('[data-cy=editLevelButton]').eq(3).click();
        cy.get('[data-cy=levelName]').type('{selectall}Baaaaar');
        cy.get('[data-cy=saveLevelButton]').click();
        cy.wait('@saveLevel');
        cy.wait('@loadLevels');
        cy.get('[data-cy=editLevelButton]').eq(3).should('have.focus');
    });


});
