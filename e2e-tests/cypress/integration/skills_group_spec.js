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

            cy.get('[data-cy="groupName"]').type(groupName);
            if (description) {
                cy.get('[data-cy="groupDescription"]').type(description);
            }
            cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

            cy.get('[data-cy="saveGroupButton"]').click();
            cy.get('[data-cy="EditSkillGroupModal"]').should('not.exist');
        });

        Cypress.Commands.add("addSkillToGroupViaUI", (groupId, skillNum, expandGroup = true) => {
            const skillName = `Skill ${skillNum}`;
            if (expandGroup) {
                cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`)
                    .click();
            }
            cy.get(`[data-cy="addSkillToGroupBtn-${groupId}"]`).click();
            cy.get('[data-cy="skillName"]').type(skillName);
            cy.get('[data-cy="saveSkillButton"]').click();
            cy.get('[data-cy="saveSkillButton"]').should('not.exist');
            cy.contains(`Skill${skillNum}Skill`);
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

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="expandDetailsBtn_BlahGroup"]').click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_BlahGroup"] [data-cy="description"]').contains('Description for this group!');
    });

    it('group\'s description supports markdown', () => {
        const markdown = "# Title1\n## Title2\n### Title 3\n#### Title 4\n##### Title 5\nTitle 6\n\n";
        cy.createSkillsGroup(1, 1, 1, { description : markdown });

        cy.viewport(1200, 1200)
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="expandDetailsBtn_group1"]').click();
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="description"]');
        cy.matchSnapshotImageForElement('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="description"]', 'Groups Description Markdown');
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
        cy.acceptRemovalSafetyCheck();

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

    it('change display order for skills under a group', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 4);
        cy.addSkillToGroup(1, 1, 1, 5);
        cy.addSkillToGroup(1, 1, 1, 6);

        const groupId = 'group1'
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        const skillsTableSelector = '[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="skillsTable"]'
        cy.get(`${skillsTableSelector} th`).contains('Display Order').click()

        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 0,  value: 'skill4' }],
            [{ colIndex: 0,  value: 'skill5' }],
            [{ colIndex: 0,  value: 'skill6' }],
        ], 5, true, null, false);

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill4"]').click()
        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 0,  value: 'skill5' }],
            [{ colIndex: 0,  value: 'skill4' }],
            [{ colIndex: 0,  value: 'skill6' }],
        ], 5, true, null, false);

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill4"]').click()
        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 0,  value: 'skill5' }],
            [{ colIndex: 0,  value: 'skill6' }],
            [{ colIndex: 0,  value: 'skill4' }],
        ], 5, true, null, false);

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill6"]').click()
        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 0,  value: 'skill6' }],
            [{ colIndex: 0,  value: 'skill5' }],
            [{ colIndex: 0,  value: 'skill4' }],
        ], 5, true, null, false);
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill6"]').should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill6"]').should('be.disabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill5"]').should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill5"]').should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill4"]').should('be.disabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill4"]').should('be.enabled');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();
        cy.get(`${skillsTableSelector} th`).contains('Display Order').click()
        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 0,  value: 'skill6' }],
            [{ colIndex: 0,  value: 'skill5' }],
            [{ colIndex: 0,  value: 'skill4' }],
        ], 5, true, null, false);
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill6"]').should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill6"]').should('be.disabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill5"]').should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill5"]').should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill4"]').should('be.disabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill4"]').should('be.enabled');

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

    it('go live', () => {
        Cypress.Commands.add("verifyGoLiveDisabled", (groupId) => {
            cy.get(`[data-cy="nameCell_${groupId}"]`).contains('Disabled');
            cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="skillGroupStatus"]`).contains('Disabled');
            cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).should('be.disabled');
        });

        const groupId = 'group1'

        cy.createSkillsGroup(1, 1, 1);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();
        cy.verifyGoLiveDisabled(groupId)

        // 1 skill in the group
        cy.addSkillToGroupViaUI(groupId, 1, false);
        cy.verifyGoLiveDisabled(groupId)

        // 2 skills in the group
        cy.addSkillToGroupViaUI(groupId, 2, false);
        cy.get(`[data-cy="nameCell_${groupId}"]`).contains('Disabled');
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="skillGroupStatus"]`).contains('Disabled');
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).should('be.enabled');

        // go live
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).click();
        cy.contains('While this Group is disabled, user\'s cannot see the group or achieve it');
        cy.contains('Yes, Go Live').click();

        cy.get(`[data-cy="nameCell_${groupId}"]`).contains('Disabled').should('not.exist');
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="skillGroupStatus"]`).contains('Live')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="skillGroupStatus"]`).contains('Disabled').should('not.exist');
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).should('not.exist');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="nameCell_${groupId}"]`).contains('Disabled').should('not.exist');
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="skillGroupStatus"]`).contains('Live')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="skillGroupStatus"]`).contains('Disabled').should('not.exist');
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).should('not.exist');
    });

    it('once the group enabled must have at least 2 skills - delete buttons should be disabled if there are only 2 skills', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_group1"]').should('be.enabled');

        // go live
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).click();
        cy.contains('While this Group is disabled, user\'s cannot see the group or achieve it');
        cy.contains('Yes, Go Live').click();

        // now should be disabled
        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.disabled');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('be.disabled');
        cy.get('[data-cy="deleteSkillButton_group1"]').should('be.enabled');

        // add third skills and delete button should be enabled
        cy.addSkillToGroupViaUI(groupId, 3, false);
        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_Skill3Skill"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_group1"]').should('be.enabled');
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();
        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_Skill3Skill"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_group1"]').should('be.enabled');

        // delete 1 skill, remaining buttons should be disabled
        cy.get('[data-cy="deleteSkillButton_skill2"]').click();
        cy.acceptRemovalSafetyCheck();
        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.disabled');
        cy.get('[data-cy="deleteSkillButton_Skill3Skill"]').should('be.disabled');
        cy.get('[data-cy="deleteSkillButton_group1"]').should('be.enabled');
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();
        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.disabled');
        cy.get('[data-cy="deleteSkillButton_Skill3Skill"]').should('be.disabled');
        cy.get('[data-cy="deleteSkillButton_group1"]').should('be.enabled');
    });

    it('all skills can be deleted from a disabled group', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('be.enabled');

        cy.get('[data-cy="deleteSkillButton_skill2"]').click();
        cy.acceptRemovalSafetyCheck();

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('not.exist');

        cy.get('[data-cy="deleteSkillButton_skill1"]').click();
        cy.acceptRemovalSafetyCheck();

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('not.exist');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('not.exist');

        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        // refresh and verify
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('not.exist');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('not.exist');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');
    });

    it('all skills can be deleted from a disabled group even after numRequiredSkills were updated', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        cy.createSkillsGroup(1, 1, 1, { numSkillsRequired: 1 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('1')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('2')

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('be.enabled');

        cy.get('[data-cy="deleteSkillButton_skill2"]').click();
        cy.acceptRemovalSafetyCheck();

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('be.enabled');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('not.exist');

        cy.get('[data-cy="deleteSkillButton_skill1"]').click();
        cy.acceptRemovalSafetyCheck();

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('not.exist');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('not.exist');

        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        // refresh and verify
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('not.exist');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('not.exist');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');
    });

    it('total points are updated when skill is removed', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 4, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Points').click();
        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('200');

        cy.get('[data-cy="deleteSkillButton_skill2"]').click();
        cy.contains('ID: skill2');
        cy.acceptRemovalSafetyCheck();;

        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('150');
    })

    it('edit skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="editSkillButton_skill2"]').click();
        cy.get('[data-cy="skillName"]').clear().type('other');
        cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();
        cy.get('[data-cy="idInputValue"]').clear().type('newId');
        cy.get('[data-cy="skillPointIncrement"]').clear().type(50);
        cy.get('button').contains('Save').click();

        cy.get('[data-cy="editSkillButton_skill2"]').should('not.exist');
        cy.get('[data-cy="editSkillButton_newId"]')
        cy.get('[data-cy="nameCell_newId"]').contains('newId')
        cy.get('[data-cy="nameCell_newId"]').contains('other')

        cy.get('[data-cy="skillsTable-additionalColumns"]').first().contains('Points').click();
        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('350');
    });

    it('copy skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="copySkillButton_skill2"]').click();
        cy.get('button').contains('Save').click();

        cy.get('[data-cy="nameCell_copy_of_skill2"]').contains('Copy of Very Great Skill 2')
        cy.get('[data-cy="nameCell_copy_of_skill2"]').contains('ID: copy_of_skill2')

        // navigate away and back then verify
        cy.clickNav('Levels')
        cy.clickNav('Skills')

        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();
        cy.get('[data-cy="nameCell_copy_of_skill2"]').contains('Copy of Very Great Skill 2')
        cy.get('[data-cy="nameCell_copy_of_skill2"]').contains('ID: copy_of_skill2')

        // refresh and verify
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="nameCell_copy_of_skill2"]').contains('Copy of Very Great Skill 2')
        cy.get('[data-cy="nameCell_copy_of_skill2"]').contains('ID: copy_of_skill2')
    });

    it('"Go Live" button must enabled after copying operation creates 2nd skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="copySkillButton_skill1"]').click();
        cy.get('button').contains('Save').click();

        cy.get('[data-cy="nameCell_copy_of_skill1"]').contains('Copy of Very Great Skill 1')
        cy.get('[data-cy="nameCell_copy_of_skill1"]').contains('ID: copy_of_skill1')

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).should('be.enabled');

        // refresh and verify
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).should('be.enabled');
    });

    it('nav to skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="manageSkillBtn_skill2"]').click();
        cy.get('[data-cy="pageHeader"]').contains('SKILL: Very Great Skill 2');
    });

    it('Report Skill Events: ability to report skill events after group is enabled', () => {
        cy.intercept('POST', '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=ONE').as('userSuggest');
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).click();
        cy.contains('Yes, Go Live').click();

        cy.get('[data-cy="manageSkillBtn_skill2"]').click();
        cy.get('[data-cy="pageHeader"]').contains('SKILL: Very Great Skill 2');
        cy.get('[data-cy="pageHeader"]').contains('Group ID: group1');
        cy.get('[data-cy="disabledGroupBadge-group1"]').should('not.exist');

        cy.get('[data-cy="nav-Add Event"] .fa-exclamation-circle').should('not.exist');

        // nav directly to the page and nav item is disabled
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2/addSkillEvent');
        cy.get('[data-cy="nav-Add Event"] .fa-exclamation-circle').should('not.exist');
        cy.get('[data-cy="userIdInput"]').type('user1{enter}')
        cy.wait('@userSuggest');
        cy.get('[data-cy="userIdInput"]').contains('user1')
        cy.get('[data-cy="addSkillEventButton"]').should('be.enabled');
    });

    it('Report Skill Events: do not allow to report skill if the group is disabled', () => {
        cy.intercept('POST', '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=ONE').as('userSuggest');
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });

        // create just a skill to fulfill project and subject minimum points requirement
        cy.createSkill(1, 1, 3, { pointIncrement: 100, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="manageSkillBtn_skill2"]').click();
        cy.get('[data-cy="pageHeader"]').contains('SKILL: Very Great Skill 2');
        cy.get('[data-cy="pageHeader"]').contains('Group ID: group1');
        cy.get('[data-cy="disabledGroupBadge-group1"]');
        cy.get('[data-cy="nav-Add Event"] .fa-exclamation-circle').should('exist');

        // nav directly to the page and nav item is disabled
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2/addSkillEvent');
        cy.get('[data-cy="userIdInput"]').type('user1{enter}')
        cy.wait('@userSuggest');
        cy.get('[data-cy="userIdInput"]').contains('user1')
        cy.get('[data-cy="addSkillEventButton"]').should('be.disabled');
    });

    it('Report Skill Events:  must not be able to report skill events if there is not enough points because group is not enabled', () => {
        cy.intercept('POST', '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=ONE').as('userSuggest');

        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="manageSkillBtn_skill2"]').click();
        cy.get('[data-cy="pageHeader"]').contains('SKILL: Very Great Skill 2');
        cy.get('[data-cy="pageHeader"]').contains('Group ID: group1');
        cy.get('[data-cy="disabledGroupBadge-group1"]');
        cy.get('[data-cy="nav-Add Event"] .fa-exclamation-circle').should('exist');

        // nav directly to the page and nav item is disabled
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2/addSkillEvent');
        cy.get('[data-cy="userIdInput"]').type('user1{enter}')
        cy.wait('@userSuggest');
        cy.get('[data-cy="userIdInput"]').contains('user1')
        cy.get('[data-cy="addSkillEventButton"]').should('be.disabled');
    });

    it('modify number of required skills is enabled once there are 2 skills', () => {
        cy.createSkillsGroup(1, 1, 1);
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.disabled');

        cy.addSkillToGroupViaUI(groupId, 1, false);

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.disabled');

        cy.addSkillToGroupViaUI(groupId, 2, false);

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');
    });

    it('modify number of required skills after copying skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.disabled');

        cy.addSkillToGroupViaUI(groupId, 1, false);
        cy.get('[data-cy="copySkillButton_Skill1Skill"]').click();
        cy.get('[data-cy="saveSkillButton"]').click();
        cy.get('[data-cy="saveSkillButton"]').should('not.exist');
        cy.contains(`copy_of_Skill1Skill`);

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).should('have.value','-1');
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).select('1 out of 2');
        cy.get('.modal-content').contains('Save').click();


        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('1')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('2')
    });

    it('"Group skills points must be the same" after points were aligned by editing a skill - numPerformToCompletion attribute is different', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 50 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.contains('Group\'s skills points must be the same');
        cy.get('.modal-footer').contains('Cancel').click();

        cy.get('[data-cy="editSkillButton_skill2"]').click();
        cy.get('[data-cy="numPerformToCompletion"]').clear().type('5');
        cy.get('[data-cy="saveSkillButton"]').click();
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).should('have.value','-1');
        cy.contains('Group\'s skills points must be the same').should('not.exist');

        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).select('1 out of 2');
        cy.get('.modal-content').contains('Save').click();
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('1')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('2')
    });

    it('"Group skills points must be the same" after points were aligned by editing a skill - pointIncrement attribute is different', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 15, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.contains('Group\'s skills points must be the same');
        cy.get('.modal-footer').contains('Cancel').click();

        cy.get('[data-cy="editSkillButton_skill2"]').click();
        cy.get('[data-cy="skillPointIncrement"]').clear().type('10');
        cy.get('[data-cy="saveSkillButton"]').click();
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).should('have.value','-1');
        cy.contains('Group\'s skills points must be the same').should('not.exist');

        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).select('1 out of 2');
        cy.get('.modal-content').contains('Save').click();
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('1')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('2')
    });


    it('modify number of required skills', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        // -1 === all skills
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).should('have.value','-1');
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).select('1 out of 2');

        cy.get('.modal-content').contains('Save').click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('1')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('2')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).should('have.value', '1');

        //refresh and validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('1')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('2')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).should('have.value', '1');
    });

    it('when the skill is removed and numSkillsRequired==skills.size then display "All Skills" tag', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 4, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        // -1 == all skills
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).should('have.value', -1)
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).select('2 out of 4');

        cy.get('.modal-content').contains('Save').click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('2')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('4')

        cy.get('[data-cy="deleteSkillButton_skill2"]').click();
        cy.contains('This will remove Very Great Skill 2 (ID: skill2)');
        cy.acceptRemovalSafetyCheck();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('2')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('3')

        cy.get('[data-cy="deleteSkillButton_skill1"]').click();
        cy.contains('This will remove Very Great Skill 1 (ID: skill1)');
        cy.acceptRemovalSafetyCheck();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).should('not.exist')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).should('not.exist')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        // -1 === all skills
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).should('have.value','-1');

        // refresh and re-verify
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).should('not.exist')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).should('not.exist')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        // -1 === all skills
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).should('have.value','-1');
    });

    it('when the required skills numSkillsRequired is set edit skills modal must not allow users to change point values', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 20, numPerformToCompletion: 3 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 20, numPerformToCompletion: 3 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 20, numPerformToCompletion: 3 });
        cy.addSkillToGroup(1, 1, 1, 4, { pointIncrement: 20, numPerformToCompletion: 3 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        // set numSkillsRequired
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).contains(3);
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).select('2 out of 4');
        cy.get('.modal-content').contains('Save').click();

        // verify new skill modal validation
        cy.get(`[data-cy="addSkillToGroupBtn-${groupId}"]`).click();
        cy.get('[data-cy="skillPointIncrement"]').should('have.value', 20);
        cy.get('[data-cy="numPerformToCompletion"]').should('have.value', 3);
        cy.get('[data-cy="skillPointIncrement"]').should('be.disabled');
        cy.get('[data-cy="numPerformToCompletion"]').should('be.disabled');
        cy.get('[data-cy="skillPointIncrementDisabledWarning"]')
        cy.get('[data-cy="numPerformToCompletionDisabledWarning"]')
        cy.get('[data-cy="closeSkillButton"]').click();

        // verify edit skill modal
        cy.get('[data-cy="editSkillButton_skill3"').click();
        cy.get('[data-cy="skillPointIncrement"]').should('have.value', 20);
        cy.get('[data-cy="numPerformToCompletion"]').should('have.value', 3);
        cy.get('[data-cy="skillPointIncrement"]').should('be.disabled');
        cy.get('[data-cy="numPerformToCompletion"]').should('be.disabled');
        cy.get('[data-cy="skillPointIncrementDisabledWarning"]')
        cy.get('[data-cy="numPerformToCompletionDisabledWarning"]')

        // refresh and verify again
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        // verify new skill modal validation
        cy.get(`[data-cy="addSkillToGroupBtn-${groupId}"]`).click();
        cy.get('[data-cy="skillPointIncrement"]').should('have.value', 20);
        cy.get('[data-cy="numPerformToCompletion"]').should('have.value', 3);
        cy.get('[data-cy="skillPointIncrement"]').should('be.disabled');
        cy.get('[data-cy="numPerformToCompletion"]').should('be.disabled');
        cy.get('[data-cy="skillPointIncrementDisabledWarning"]')
        cy.get('[data-cy="numPerformToCompletionDisabledWarning"]')
        cy.get('[data-cy="closeSkillButton"]').click();

        // verify edit skill modal
        cy.get('[data-cy="editSkillButton_skill3"').click();
        cy.get('[data-cy="skillPointIncrement"]').should('have.value', 20);
        cy.get('[data-cy="numPerformToCompletion"]').should('have.value', 3);
        cy.get('[data-cy="skillPointIncrement"]').should('be.disabled');
        cy.get('[data-cy="numPerformToCompletion"]').should('be.disabled');
        cy.get('[data-cy="skillPointIncrementDisabledWarning"]')
        cy.get('[data-cy="numPerformToCompletionDisabledWarning"]')

    });

    it('required skills can only be modified when all of the underlying skills\' points match', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 12, numPerformToCompletion: 7 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 50, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 11, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();

        // verify defaults
        cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="pointIncrement"]').should('have.value', 12)
        cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="numPerformToCompletion"]').should('have.value', 7)
        cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="totalPoints"]').contains('84')

        // can not add num required
        cy.get('[data-cy="requiredSkillsNumSelect"]').should('be.disabled')

        // set all skills to
        cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="pointIncrement"]').clear().type(3);
        // cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="numPerformToCompletion"]').clear().type(4);

        // save button should be disabled while cancel enabled
        cy.get('.modal-footer button').first().should('be.enabled');
        cy.get('.modal-footer button').last().should('be.disabled');

        cy.get('[data-cy="syncBtn"]').click();

        cy.get('[data-cy="requiredSkillsNumSelect"]').should('be.enabled')
        cy.get('.modal-footer button').first().should('be.enabled');
        cy.get('.modal-footer button').last().should('be.enabled');
        cy.get('[data-cy="syncSkillsPointsSection"]').should('not.exist');

        // save and validate points are reflected
        cy.get('.modal-footer button').last().click();
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_group1"] ${tableSelector} th`).contains('Display Order').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="skillsTable-additionalColumns"]').contains('Points').click();

        cy.validateTable(`[data-cy="ChildRowSkillGroupDisplay_group1"] ${tableSelector}`, [
            [{ colIndex: 3,  value: '213 pts x 7 repetitions' } ],
            [{ colIndex: 3,  value: '213 pts x 7 repetitions' } ],
            [{ colIndex: 3,  value: '213 pts x 7 repetitions' } ],
        ], 5, true, null, false);

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get('[data-cy="requiredSkillsNumSelect"]').should('be.enabled')
        // -1 == all skills
        cy.get('[data-cy="requiredSkillsNumSelect"]').should('have.value', -1);
        cy.get('.modal-footer button').first().should('be.enabled');
        cy.get('.modal-footer button').last().should('be.enabled');
        cy.get('[data-cy="syncSkillsPointsSection"]').should('not.exist');

        // refresh and re-test
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_group1"] ${tableSelector} th`).contains('Display Order').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="skillsTable-additionalColumns"]').contains('Points').click();

        cy.validateTable(`[data-cy="ChildRowSkillGroupDisplay_group1"] ${tableSelector}`, [
            [{ colIndex: 3,  value: '213 pts x 7 repetitions' } ],
            [{ colIndex: 3,  value: '213 pts x 7 repetitions' } ],
            [{ colIndex: 3,  value: '213 pts x 7 repetitions' } ],
        ], 5, true, null, false);

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get('[data-cy="requiredSkillsNumSelect"]').should('be.enabled')
        // -1 == all skills
        cy.get('[data-cy="requiredSkillsNumSelect"]').should('have.value', -1);
        cy.get('.modal-footer button').first().should('be.enabled');
        cy.get('.modal-footer button').last().should('be.enabled');
        cy.get('[data-cy="syncSkillsPointsSection"]').should('not.exist');
    });

    it('subject overview cards are updated when group is enabled', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');

        // go live
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).click();
        cy.contains('While this Group is disabled, user\'s cannot see the group or achieve it');
        cy.contains('Yes, Go Live').click();

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '150');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');
    })

    it('subject overview cards are updated when skills added, deleted or modified for group that is enabled', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '150');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');

        // delete
        cy.get('[data-cy="deleteSkillButton_skill1"]').click();
        cy.contains('Delete Action CANNOT be undone');
        cy.acceptRemovalSafetyCheck();

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '100');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');

        // modify
        cy.get('[data-cy="editSkillButton_skill2"]').click();
        cy.get('[data-cy="skillPointIncrement"]').clear().type(50);
        cy.get('button').contains('Save').click();

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '300');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');

        // add
        cy.addSkillToGroupViaUI('group1', 4, false)
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '350');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');
    })

    it('subject overview cards are updated when group skill is copied', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '150');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');

        // copy
        cy.get('[data-cy="copySkillButton_skill1"]').click();
        cy.get('button').contains('Save').click();

        cy.get('[data-cy="nameCell_copy_of_skill1"]').contains('Copy of Very Great Skill 1')
        cy.get('[data-cy="nameCell_copy_of_skill1"]').contains('ID: copy_of_skill1')

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '200');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '4');
    })

    it('subject overview cards are updated after skill sync', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 12, numPerformToCompletion: 7 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 50, numPerformToCompletion: 2 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 11, numPerformToCompletion: 5 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '239');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();

        // verify defaults
        cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="pointIncrement"]').should('have.value', 12)
        cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="numPerformToCompletion"]').should('have.value', 7)
        cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="totalPoints"]').contains('84')

        // can not add num required
        cy.get('[data-cy="requiredSkillsNumSelect"]').should('be.disabled')

        // set all skills to
        cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="pointIncrement"]').clear().type(50);
        cy.get('[data-cy="syncSkillsPointsSection"] [data-cy="numPerformToCompletion"]').clear().type(4);

        // save button should be disabled while cancel enabled
        cy.get('.modal-footer button').first().should('be.enabled');
        cy.get('.modal-footer button').last().should('be.disabled');

        cy.get('[data-cy="syncBtn"]').click();

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '600');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '600');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');
    });

    it('search and navigate to group\'s skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="skillsSelector"]').click();
        cy.get('[data-cy="skillsSelector"]').contains('Type to search for skills').should('be.visible')
        cy.get('[data-cy="skillsSelector"]').type('skill')

        cy.get('[data-cy="skillsSelector"] [data-cy="skillsSelector-skillId"]').should('have.length', 2).as('skillIds');
        cy.get('@skillIds').eq(0).contains('skill1');
        cy.get('@skillIds').eq(1).contains('skill2');
        cy.get('@skillIds').eq(1).click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill2')
    });

    it('add group\'s skill as a dependency', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2/dependencies');
        cy.contains('No Dependencies Yet');

        cy.get('[data-cy="depsSelector"]').click();
        cy.get('[data-cy="skillsSelector"] [data-cy="skillsSelector-skillId"]').should('have.length', 1).as('skillIds');
        cy.get('@skillIds').eq(0).contains('skill1');
        cy.get('@skillIds').eq(0).click();
        cy.get('[data-cy="simpleSkillsTable"]').contains('Very Great Skill 1');
    });

    it('go live should not change groups display order', () => {
        cy.createSkill(1, 1, 1)
        cy.wait(1000)
        cy.createSkill(1, 1, 2)
        cy.wait(1000)
        cy.createSkill(1, 1, 3)
        cy.wait(1000)
        cy.createSkillsGroup(1, 1, 4);
        cy.addSkillToGroup(1, 1, 4, 5);
        cy.addSkillToGroup(1, 1, 4, 6);

        const groupId = 'group4';
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="expandDetailsBtn_${groupId}"]`).click();
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="goLiveBtn"]`).click();
        cy.contains('Yes, Go Live').click();

        cy.get('[data-label="Skill"]').should('have.length', 6).as('cells');
        cy.get('@cells').eq(0).contains('ID: group4');
        cy.get('@cells').eq(1).contains('ID: skill6');
        cy.get('@cells').eq(2).contains('ID: skill5');
        cy.get('@cells').eq(3).contains('ID: skill3');
        cy.get('@cells').eq(4).contains('ID: skill2');
        cy.get('@cells').eq(5).contains('ID: skill1');

    });

});

