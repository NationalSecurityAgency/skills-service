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
var moment = require('moment-timezone');

describe('Skills Group Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        Cypress.Commands.add("createGroupViaUI", (groupName, description = null) => {
            cy.get('[data-cy="newGroupButton"]').click();
            cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

            cy.get('[data-cy="groupName"]').type(groupName);
            if (description) {
                cy.get('[data-cy="groupDescription"]').type(description);
            }
            cy.get('[data-cy="saveGroupButton"]').click();
            cy.get('[data-cy="EditSkillGroupModal"]').should('not.exist');
        });

        Cypress.Commands.add("addSkillToGroupViaUI", (groupId, skillNum) => {
            const skillName = `Skill ${skillNum}`;
            cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();
            cy.get(`[data-cy="addSkillToGroupBtn-${groupId}"]`).click();
            cy.get('[data-cy="skillName"]').type(skillName);
            cy.get('[data-cy="saveSkillButton"]').click();
            cy.get('[data-cy="saveSkillButton"]').should('not.exist');
        });

    });
    const tableSelector = '[data-cy="skillsTable"]';

    it('create skills group', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');
        cy.createGroupViaUI('Blah');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Blah' }, { colIndex: 0,  value: 'ID: BlahGroup' }, { colIndex: 1, value: '1' }],
        ], 5);

        cy.createGroupViaUI('another');
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'another' }, { colIndex: 0,  value: 'ID: anotherGroup' }, { colIndex: 1, value: '2' }],
            [{ colIndex: 0,  value: 'Blah' }, { colIndex: 0,  value: 'ID: BlahGroup' }, { colIndex: 1, value: '1' }],
        ], 5);
    });

    it('create group with description', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');
        cy.createGroupViaUI('Blah', 'Description for this group!');
        cy.get('[data-cy="expandDetailsBtn_BlahGroup"]').click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_BlahGroup"] [data-cy="description"]').contains('Description for this group!');
    });

    it('handle focus', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2);
        cy.createSkill(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // edit group then cancel and verify focus
        cy.get('[data-cy="editSkillButton_group1"]').click();
        cy.get('[data-cy="closeGroupButton"]').click();
        cy.get('[data-cy="editSkillButton_group1"]').should('have.focus')

        // edit skill then cancel and verify focus
        cy.get('[data-cy="editSkillButton_skill1"]').click();
        cy.get('[data-cy="closeSkillButton"]').click();
        cy.get('[data-cy="editSkillButton_skill1"]').should('have.focus')
    });

    it('remove group - display order should be updated', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 3);
        cy.createSkill(1, 1, 2);

        cy.addSkillToGroup(1, 1, 1, 4);
        cy.addSkillToGroup(1, 1, 1, 5);
        cy.createSkillsGroup(1, 1, 1, { enabled: true });

        cy.addSkillToGroup(1, 1, 2, 6);
        cy.addSkillToGroup(1, 1, 2, 7);
        cy.createSkillsGroup(1, 1, 2, { enabled: true });

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`${tableSelector} th`).contains('Display Order').click()

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'group1' },  { colIndex: 1, value: '1' }],
            [{ colIndex: 0,  value: 'group2' },  { colIndex: 1, value: '2' }],
            [{ colIndex: 0,  value: 'skill1' },  { colIndex: 1, value: '3' }],
            [{ colIndex: 0,  value: 'group3' },  { colIndex: 1, value: '4' }],
            [{ colIndex: 0,  value: 'skill2' },  { colIndex: 1, value: '5' }],
        ], 5);

        cy.get('[data-cy="deleteSkillButton_group2"]').click();
        cy.contains('DELETE [group2]?');
        cy.contains('YES, Delete It!').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'group1' },  { colIndex: 1, value: '1' }],
            [{ colIndex: 0,  value: 'skill1' },  { colIndex: 1, value: '2' }],
            [{ colIndex: 0,  value: 'group3' },  { colIndex: 1, value: '3' }],
            [{ colIndex: 0,  value: 'skill2' },  { colIndex: 1, value: '4' }],
        ], 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`${tableSelector} th`).contains('Display Order').click()
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'group1' },  { colIndex: 1, value: '1' }],
            [{ colIndex: 0,  value: 'skill1' },  { colIndex: 1, value: '2' }],
            [{ colIndex: 0,  value: 'group3' },  { colIndex: 1, value: '3' }],
            [{ colIndex: 0,  value: 'skill2' },  { colIndex: 1, value: '4' }],
        ], 5);
    });

    it('change display order', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 3);
        cy.createSkill(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`${tableSelector} th`)
            .contains('Display Order')
            .click()
        cy.get('[data-cy="orderMoveDown_group1"]').click();
        cy.get('[data-cy="orderMoveDown_group3"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Awesome Group 2' },  { colIndex: 1, value: '1' }],
            [{ colIndex: 0,  value: 'Awesome Group 1' },  { colIndex: 1, value: '2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 1' },  { colIndex: 1, value: '3' }],
            [{ colIndex: 0,  value: 'Very Great Skill 2' },  { colIndex: 1, value: '4' }],
            [{ colIndex: 0,  value: 'Awesome Group 3' },  { colIndex: 1, value: '5' }],
        ], 5);

        cy.get('[data-cy="orderMoveDown_group1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_group1"]').should('be.enabled');

        cy.get('[data-cy="orderMoveDown_group2"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_group2"]').should('be.disabled');

        cy.get('[data-cy="orderMoveDown_group3"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_group3"]').should('be.enabled');

        cy.get('[data-cy="orderMoveDown_skill1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_skill1"]').should('be.enabled');

        cy.get('[data-cy="orderMoveDown_skill2"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_skill2"]').should('be.enabled');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`${tableSelector} th`)
            .contains('Display Order')
            .click()
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Awesome Group 2' },  { colIndex: 1, value: '1' }],
            [{ colIndex: 0,  value: 'Awesome Group 1' },  { colIndex: 1, value: '2' }],
            [{ colIndex: 0,  value: 'Very Great Skill 1' },  { colIndex: 1, value: '3' }],
            [{ colIndex: 0,  value: 'Very Great Skill 2' },  { colIndex: 1, value: '4' }],
            [{ colIndex: 0,  value: 'Awesome Group 3' },  { colIndex: 1, value: '5' }],
        ], 5);

        cy.get('[data-cy="orderMoveDown_group1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_group1"]').should('be.enabled');

        cy.get('[data-cy="orderMoveDown_group2"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_group2"]').should('be.disabled');

        cy.get('[data-cy="orderMoveDown_group3"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_group3"]').should('be.enabled');

        cy.get('[data-cy="orderMoveDown_skill1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_skill1"]').should('be.enabled');

        cy.get('[data-cy="orderMoveDown_skill2"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_skill2"]').should('be.enabled');
    })

    it('additional columns', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2);
        cy.createSkillsGroup(1, 1, 3);

        cy.addSkillToGroup(1, 1, 1, 4);
        cy.addSkillToGroup(1, 1, 1, 5);
        cy.createSkillsGroup(1, 1, 1, { enabled: true });

        cy.addSkillToGroup(1, 1, 3, 6);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`${tableSelector} th`)
            .contains('Display Order')
            .click()
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Points').click();
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Self Report').click();
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Time Window').click();
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Version').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'group1' },  { colIndex: 3, value: '400from 2 skills' }, { colIndex: 4, value: 'N/A' }, { colIndex: 5, value: 'N/A' }, { colIndex: 6, value: '0' }],
            [{ colIndex: 0,  value: 'group2' },  { colIndex: 3, value: '0from 0 skills' }],
            [{ colIndex: 0,  value: 'group3' },  { colIndex: 3, value: '200from 1 skill' }],
        ], 5);
    });

    it('total points in additional column are incremented when skills are added', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Points').click();
        cy.addSkillToGroupViaUI('group1', 1);
        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('50');
    });

    // it('go live', () => {
    //     cy.createSkillsGroup(1, 1, 1);
    //     cy.visit('/administrator/projects/proj1/subjects/subj1');
    //
    //     cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Points').click();
    //     cy.addSkillToGroupViaUI('group1', 1);
    //     cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('50');
    //
    //     cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="skillGroupStatus"]').contains('Disabled');
    // });

});