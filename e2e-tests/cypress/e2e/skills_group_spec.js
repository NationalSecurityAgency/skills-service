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

            cy.get('[data-cy="name"]').type(groupName);
            if (description) {
                cy.get('[data-cy="markdownEditorInput"]').type(description);
            }
            cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

            cy.get('[data-cy="saveDialogBtn"]').click();
            cy.get('[data-cy="EditSkillGroupModal"]').should('not.exist');
            cy.get('[data-cy="newGroupButton"]').should('have.focus')
        });

        Cypress.Commands.add("addSkillToGroupViaUI", (groupId, skillNum, expandGroupNum = 0) => {
            const skillName = `Skill ${skillNum}`;
            if (expandGroupNum !== null) {
                cy.get(`[data-p-index="${expandGroupNum}"] [data-pc-section="rowtoggler"]`).click()
            }
            cy.get(`[data-cy="addSkillToGroupBtn-${groupId}"]`).click();
            cy.get('[data-cy="skillName"]').type(skillName);
            cy.get('[data-cy="saveDialogBtn"]').click();
            cy.get('[data-cy="saveDialogBtn"]').should('not.exist');
            cy.get(`[data-cy="manageSkillLink_Skill${skillNum}Skill"]`).should('have.text', skillName);
        });

    });
    const tableSelector = '[data-cy="skillsTable"]';

    it('create skills group', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills');
        cy.createGroupViaUI('Blah');
        cy.validateTable(tableSelector, [
            [{ colIndex: 2,  value: 'Blah' }, { colIndex: 3, value: '1' }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`).should('have.length', 1);

        cy.createGroupViaUI('another');
        cy.validateTable(tableSelector, [
            [{ colIndex: 2,  value: 'another' }, { colIndex: 3, value: '2' }],
            [{ colIndex: 2,  value: 'Blah' }, { colIndex: 3, value: '1' }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`).should('have.length', 2);
    });

    it('create group with description', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills');
        cy.createGroupViaUI('Blah', 'Description for this group!');
        cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_BlahGroup"] [data-cy="description"]').contains('Description for this group!');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_BlahGroup"] [data-cy="description"]').contains('Description for this group!');
    })

    it('group\'s description supports markdown', () => {
        const markdown = "# Title1\n## Title2\n### Title 3\n#### Title 4\n##### Title 5\nTitle 6\n\n";
        cy.createSkillsGroup(1, 1, 1, { description : markdown });

        cy.viewport(1200, 1200)
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="description"]');
        cy.matchSnapshotImageForElement('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="description"]');
    });

    it('handle focus on edit', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2);
        cy.createSkill(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // edit group then cancel and verify focus
        cy.get('[data-cy="editSkillButton_group1"]').click();
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="editSkillButton_group1"]').should('have.focus')

        // edit skill then cancel and verify focus
        cy.get('[data-cy="editSkillButton_skill1"]').click();
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="editSkillButton_skill1"]').should('have.focus')
    });

    it('handle focus on new group - cancel', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2);
        cy.createSkill(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="name"]')
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="newGroupButton"]').should('have.focus')

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="name"]')
        cy.get('[data-pc-section="closebuttonicon"]').click();
        cy.get('[data-cy="newGroupButton"]').should('have.focus')

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="name"]')
        cy.realPress('Escape');
        cy.get('[data-cy="newGroupButton"]').should('have.focus')
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

        cy.get('[data-pc-section="headercontent"]').contains('Display').click()
        cy.validateTable(tableSelector, [
            [{ colIndex: 2,  value: 'Awesome Group 1' },  { colIndex: 3, value: '1' }],
            [{ colIndex: 2,  value: 'Awesome Group 2 Subj1' },  { colIndex: 3, value: '2' }],
            [{ colIndex: 2,  value: 'Very Great Skill 1' },  { colIndex: 3, value: '3' }],
            [{ colIndex: 2,  value: 'Awesome Group 3 Subj1' },  { colIndex: 3, value: '4' }],
            [{ colIndex: 2,  value: 'Very Great Skill 2' },  { colIndex: 3, value: '5' }],
        ],  5, false, null, false);
        cy.get(`${tableSelector} tbody tr`).should('have.length', 5);

        cy.get('[data-cy="deleteSkillButton_group2"]').click();
        cy.acceptRemovalSafetyCheck();

        cy.validateTable(tableSelector, [
            [{ colIndex: 2,  value: 'Awesome Group 1' },  { colIndex: 3, value: '1' }],
            [{ colIndex: 2,  value: 'Very Great Skill 1' },  { colIndex: 3, value: '2' }],
            [{ colIndex: 2,  value: 'Awesome Group 3 Subj1' },  { colIndex: 3, value: '3' }],
            [{ colIndex: 2,  value: 'Very Great Skill 2' },  { colIndex: 3, value: '4' }],
        ],  5, false, null, false);
        cy.get(`${tableSelector} tbody tr`).should('have.length', 4);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.validateTable(tableSelector, [
            [{ colIndex: 2,  value: 'Awesome Group 1' },  { colIndex: 3, value: '1' }],
            [{ colIndex: 2,  value: 'Very Great Skill 1' },  { colIndex: 3, value: '2' }],
            [{ colIndex: 2,  value: 'Awesome Group 3 Subj1' },  { colIndex: 3, value: '3' }],
            [{ colIndex: 2,  value: 'Very Great Skill 2' },  { colIndex: 3, value: '4' }],
        ],  5, false, null, false);
        cy.get(`${tableSelector} tbody tr`).should('have.length', 4);
    });

    it('change display order', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 3);
        cy.createSkill(1, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-pc-section="headercontent"]').contains('Display').click()
        cy.get('[data-cy="enableDisplayOrderSort"]').click()
        cy.get('[data-cy="orderMoveDown_group1"]').click();
        cy.get('[data-cy="orderMoveDown_group3"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 2,  value: 'Awesome Group 2' },  { colIndex: 3, value: '1' }],
            [{ colIndex: 2,  value: 'Awesome Group 1' },  { colIndex: 3, value: '2' }],
            [{ colIndex: 2,  value: 'Very Great Skill 1' },  { colIndex: 3, value: '3' }],
            [{ colIndex: 2,  value: 'Very Great Skill 2' },  { colIndex: 3, value: '4' }],
            [{ colIndex: 2,  value: 'Awesome Group 3' },  { colIndex: 3, value: '5' }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`).should('have.length', 5);

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
        cy.validateTable(tableSelector, [
            [{ colIndex: 2,  value: 'Awesome Group 2' },  { colIndex: 3, value: '1' }],
            [{ colIndex: 2,  value: 'Awesome Group 1' },  { colIndex: 3, value: '2' }],
            [{ colIndex: 2,  value: 'Very Great Skill 1' },  { colIndex: 3, value: '3' }],
            [{ colIndex: 2,  value: 'Very Great Skill 2' },  { colIndex: 3, value: '4' }],
            [{ colIndex: 2,  value: 'Awesome Group 3' },  { colIndex: 3, value: '5' }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`).should('have.length', 5);
        cy.get('[data-cy="enableDisplayOrderSort"]').click()

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
        // window.localStorage.setItem('tableState', JSON.stringify({'skillsTable': {'sortDesc': false, 'sortBy': 'displayOrder'}}))
        // window.localStorage.setItem('tableState', JSON.stringify({'groupSkills_group1': {'sortDesc': false, 'sortBy': 'displayOrder'}}))
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()

        const skillsTableSelector = '[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="skillsTable"]'
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="enableDisplayOrderSort"]').click()

        // cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-pc-section="headercontent"]').contains('Display').click()

        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 2,  value: 'Very Great Skill 4' }],
            [{ colIndex: 2,  value: 'Very Great Skill 5' }],
            [{ colIndex: 2,  value: 'Very Great Skill 6' }],
        ], 5, true, null, false);

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill4"]').click()
        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 2,  value: 'Very Great Skill 5' }],
            [{ colIndex: 2,  value: 'Very Great Skill 4' }],
            [{ colIndex: 2,  value: 'Very Great Skill 6' }],
        ], 5, true, null, false);

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill4"]').click()
        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 2,  value: 'Very Great Skill 5' }],
            [{ colIndex: 2,  value: 'Very Great Skill 6' }],
            [{ colIndex: 2,  value: 'Very Great Skill 4' }],
        ], 5, true, null, false);

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill6"]').click()
        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 2,  value: 'Very Great Skill 6' }],
            [{ colIndex: 2,  value: 'Very Great Skill 5' }],
            [{ colIndex: 2,  value: 'Very Great Skill 4' }],
        ], 5, true, null, false);
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill6"]').should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill6"]').should('be.disabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill5"]').should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill5"]').should('be.enabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveDown_skill4"]').should('be.disabled');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="orderMoveUp_skill4"]').should('be.enabled');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="enableDisplayOrderSort"]').click()
        cy.validateTable(skillsTableSelector, [
            [{ colIndex: 2,  value: 'Very Great Skill 6' }],
            [{ colIndex: 2,  value: 'Very Great Skill 5' }],
            [{ colIndex: 2,  value: 'Very Great Skill 4' }],
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
        cy.get('[data-pc-section="headercontent"]').contains('Display').click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Points"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Self Report"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Time Window"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Version"]').click()

        cy.validateTable(tableSelector, [
            [{ colIndex: 2,  value: 'Awesome Group 1' },  { colIndex: 5, value: '400from 2 skills' }, { colIndex: 6, value: 'N/A' }, { colIndex: 7, value: 'N/A' }, { colIndex: 8, value: '0' }],
            [{ colIndex: 2,  value: 'Awesome Group 2 Subj1' },  { colIndex: 5, value: '0from 0 skills' }],
            [{ colIndex: 2,  value: 'Awesome Group 3 Subj1' },  { colIndex: 5, value: '200from 1 skill' }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`).should('have.length', 3);
    });

    it('total points in additional column are incremented when skills are added', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Points"]').click()
        cy.get('[data-pc-section="closebutton"]').click()
        cy.addSkillToGroupViaUI('group1', 1);
        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('100');
    });

    it('all skills can be deleted from a group', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

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

        cy.get('[data-cy="noContent"]').contains('Group has no Skills');

        // refresh and verify
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('not.exist');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('not.exist');
        cy.get('[data-cy="noContent"]').contains('Group has no Skills');
    });

    it('all skills can be deleted from a group even after numRequiredSkills were updated', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        cy.createSkillsGroup(1, 1, 1, { numSkillsRequired: 1 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

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

        cy.get('[data-cy="noContent"]').contains('Group has no Skills');

        // refresh and verify
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="deleteSkillButton_skill1"]').should('not.exist');
        cy.get('[data-cy="deleteSkillButton_skill2"]').should('not.exist');
        cy.get('[data-cy="noContent"]').contains('Group has no Skills');
    });

    it('total points are updated when skill is removed', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 4, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Points"]').click()
        cy.get('[data-pc-section="closebutton"]').click()
        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('200');
        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('from 4 skills');

        cy.get('[data-cy="deleteSkillButton_skill2"]').click();
        cy.acceptRemovalSafetyCheck();

        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('150');
        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('from 3 skills');
    })

    it('edit skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="nameCell_group1"]')

        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Points"]').click()
        cy.get('[data-pc-section="closebutton"]').click()

        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="tokenlabel"]').contains('Points')

        cy.get('[data-cy="editSkillButton_skill2"]').click();
        cy.get('[data-cy="skillName"]').clear().type('other');
        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="idInputValue"]').clear().type('newId');
        cy.get('[data-cy="pointIncrement"]').clear().type(50);
        cy.get('button').contains('Save').click();

        cy.get('[data-cy="editSkillButton_skill2"]').should('not.exist');
        cy.get('[data-cy="editSkillButton_newId"]')
        cy.get('[data-cy="nameCell_newId"]').contains('other')

        cy.get(`${tableSelector} [data-cy="totalPointsCell_group1"]`).contains('350');
    });

    it('copy skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="copySkillButton_skill2"]').click();
        cy.get('button').contains('Save').click();

        cy.get('[data-cy="nameCell_copy_of_skill2"]').contains('Copy of Very Great Skill 2')

        // navigate away and back then verify
        cy.clickNav('Levels')
        cy.clickNav('Skills')

        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
        cy.get('[data-cy="nameCell_copy_of_skill2"]').contains('Copy of Very Great Skill 2')

        // refresh and verify
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="nameCell_copy_of_skill2"]').contains('Copy of Very Great Skill 2')
    });

    it('nav to skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="manageSkillLink_skill2"]').click();
        cy.get('[data-cy="pageHeader"]').contains('SKILL: Very Great Skill 2');
    });

    it('Report Skill Events: ability to report skill events after group is enabled', () => {
        cy.intercept('/admin/projects/proj1/subjects/subj1/skills/skill2').as('getSkill2')
        cy.intercept('POST', '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=ONE').as('userSuggest');
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="manageSkillLink_skill2"]').click();
        cy.get('[data-cy="pageHeader"]').contains('SKILL: Very Great Skill 2');
        cy.get('[data-cy="pageHeader"]').contains('Group ID: group1');
        cy.get('[data-cy="disabledGroupBadge-group1"]').should('not.exist');

        cy.get('[data-cy="nav-Add Event"]').should('be.enabled');

        // nav directly to the page and nav item is disabled
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2/addSkillEvent');

        cy.wait('@getSkill2')
        cy.get('[data-cy="subPageHeader"]').contains('Add Skill Events')
        cy.get('[data-cy="skillId"]').contains('skill2')
        cy.get('[data-cy="addEventDisabledBlockUI"] > [data-pc-section="mask"]').should('not.exist');
        cy.get('[data-cy="addEventDisabledMsg"]').should('not.exist');
        cy.get('[data-cy="userIdInput"]').type('user1{enter}')
        cy.wait('@userSuggest');
        cy.get('[data-cy="userIdInput"]').type('{enter}')
        cy.get('[data-cy="userIdInput"] input').should('have.value', 'user1')

        cy.get('[data-cy="eventDatePicker"]').click()
        cy.get('[data-pc-section="previousbutton"]').first().click()
        cy.get('.p-datepicker-group-container').contains('10').click()

        cy.get('[data-cy="addSkillEventButton"]').should('be.enabled');
    });

    it('Report Skill Events:  must not be able to report skill events if there is not enough points because group is not enabled', () => {
        cy.intercept('/admin/projects/proj1/subjects/subj1/skills/skill2').as('getSkill2')
        cy.intercept('POST', '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=ONE').as('userSuggest');

        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 5, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 5, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="manageSkillLink_skill2"]').click();
        cy.get('[data-cy="pageHeader"]').contains('SKILL: Very Great Skill 2');
        cy.get('[data-cy="pageHeader"]').contains('Group ID: group1');
        cy.get('[data-cy="nav-Add Event"]').should('be.enabled');

        // nav directly to the page and nav item is disabled
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2/addSkillEvent');
        cy.wait('@getSkill2')
        cy.get('[data-cy="subPageHeader"]').contains('Add Skill Events')
        cy.get('[data-cy="skillId"]').contains('skill2')

        cy.get('[data-cy="addSkillEventButton"]').should('be.disabled');
        cy.get('[data-cy="addEventDisabledBlockUI"] > [data-pc-section="mask"]').should('exist');
        cy.get('[data-cy="addEventDisabledMsg"]').contains('Unable to add skill for user. Insufficient available points in project.');
    });

    it('modify number of required skills is enabled once there are 2 skills', () => {
        cy.createSkillsGroup(1, 1, 1);
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.disabled');

        cy.addSkillToGroupViaUI(groupId, 1, null);

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.disabled');

        cy.addSkillToGroupViaUI(groupId, 2, null);

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');
    });

    it('modify number of required skills after copying skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.disabled');

        cy.addSkillToGroupViaUI(groupId, 1, null);
        cy.get('[data-cy="copySkillButton_Skill1Skill"]').click();
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist');
        cy.get('[data-cy="manageSkillLink_copy_of_Skill1Skill"]').should('have.text', 'Copy of Skill 1');

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).contains('All Skills')
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).click()
        cy.get('[data-pc-section="panel"] [data-pc-section="list"] [data-pc-section="item"]').contains('1 out of 2').click()
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('1')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('2')
    });

    it('modify number of required skills', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        // -1 === all skills
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).contains('All Skills')
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).click()
        cy.get('[data-pc-section="panel"] [data-pc-section="list"] [data-pc-section="item"]').contains('1 out of 2').click()
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('1')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('2')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).contains('1 out of 2')

        //refresh and validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('1')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('2')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).should('be.enabled');

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).contains('1 out of 2')
    });

    it('when the skill is removed and numSkillsRequired==skills.size then display "All Skills" tag', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 4, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        // -1 == all skills
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).contains('All Skills')
        // cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"]`).select('2 out of 4');

        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).click()
        cy.get('[data-pc-section="panel"] [data-pc-section="list"] [data-pc-section="item"]').contains('2 out of 4').click()
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('2')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('4')

        cy.get('[data-cy="deleteSkillButton_skill2"]').click();
        cy.contains('This will remove Very Great Skill 2');
        cy.acceptRemovalSafetyCheck();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).contains('2')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).contains('3')

        cy.get('[data-cy="deleteSkillButton_skill1"]').click();
        cy.contains('This will remove Very Great Skill 1');
        cy.acceptRemovalSafetyCheck();

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).should('not.exist')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).should('not.exist')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')

        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        // -1 === all skills
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).contains('All Skills')

        // refresh and re-verify
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="requiredSkillsNum"]`).should('not.exist')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredSkillsSection"] [data-cy="numSkillsInGroup"]`).should('not.exist')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="requiredAllSkills"]`).contains('all skills')
        cy.get(`[data-cy="ChildRowSkillGroupDisplay_${groupId}"] [data-cy="editRequired"]`).click();
        // -1 === all skills
        cy.get(`[data-cy="editRequiredModal-${groupId}"] [data-cy="requiredSkillsNumSelect"] [data-pc-section="input"]`).contains('All Skills')
    });

    it('subject overview cards are updated when group is enabled', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '150');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '150');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');
    })

    it('subject overview cards are updated when skills added, deleted or modified for a group', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 1, 3, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.createSkillsGroup(1, 1, 1, { enabled: true });
        const groupId = 'group1'

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

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
        cy.get('[data-cy="pointIncrement"]').clear().type(50);
        cy.get('button').contains('Save').click();

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '300');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');

        // add
        cy.addSkillToGroupViaUI('group1', 4, null)
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '400');
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
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '150');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');

        // copy
        cy.get('[data-cy="copySkillButton_skill1"]').click();
        cy.get('button').contains('Save').click();

        cy.get('[data-cy="nameCell_copy_of_skill1"]').contains('Copy of Very Great Skill 1')

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '200');
        cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '4');
    })

    it('search and navigate to group\'s skill', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="skillsSelector"]').click();
        cy.get('li.p-autocomplete-empty-message').contains('Type to search for skills').should('be.visible')
        cy.get(`[data-cy="skillsSelector"]`).type('skill')

        cy.get('[data-cy="skillsSelectionItem-skillId"]').should('have.length', 2).as('skillIds');
        cy.get('@skillIds').eq(0).contains('skill1');
        cy.get('@skillIds').eq(1).contains('skill2');
        cy.get('@skillIds').eq(1).click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill2')
    });

    it('More than 10 skills are visible in the group', () => {
        cy.createSkillsGroup(1, 1, 1)
        for (let step = 1; step < 25; step++) {
            cy.addSkillToGroup(1, 1, 1, step, { pointIncrement: 11, numPerformToCompletion: 2 })
        }
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.contains('Awesome Group 1').should('be.visible')
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy=manageSkillLink_skill24]').should('be.visible')
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-pc-section="pages"] [aria-label="Page 2"]').should('be.enabled').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy=manageSkillLink_skill14]').should('be.visible')
        cy.get('[data-pc-section="pages"] [aria-label="Page 3"]').should('be.enabled').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy=manageSkillLink_skill4]').should('be.visible')
    });

});

