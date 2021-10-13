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

        Cypress.Commands.add("createGroupViaUI", (groupName, description) => {
            cy.get('[data-cy="newGroupButton"]').click();
            cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

            cy.get('[data-cy="groupName"]').type(groupName);
            if (description) {
                cy.get('[data-cy="groupDescription"]').type(description);
            }
            cy.get('[data-cy="saveGroupButton"]').click();
            cy.get('[data-cy="EditSkillGroupModal"]').should('not.exist');
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


    it('Skills Group modal - id is auto generated based on name - special chars', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="groupName"]').type('Great !@#$% Name %^&*(+_)(');
        // cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();
        cy.get('[data-cy="idInputValue"]').should('have.value','GreatNameGroup');

        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');
    });

    it('Skills Group modal - id must not be auto generated based on name when id input is enabled', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="idInputValue"]').should('be.disabled');
        cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();
        cy.get('[data-cy="idInputValue"]').should('be.enabled');

        cy.get('[data-cy="groupName"]').type('Great');
        cy.get('[data-cy="idInputValue"]').should('have.value','');

        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
    });

    it('Skills Group modal - input validation - min/max length', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="groupName"]').type('12');
        cy.get('[data-cy="groupNameError"]').contains('Group Name cannot be less than 3 characters.');
        cy.get('[data-cy="groupName"]').type('3');
        cy.get('[data-cy="groupNameError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        // so id doesn't change anymore
        cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();

        // max value
        // Group Name cannot exceed 100 characters.
        const invalidName = Array(101).fill('a').join('');
        cy.get('[data-cy=groupName]').clear()
        cy.get('[data-cy=groupName]').fill(invalidName);
        cy.get('[data-cy=groupNameError]').contains('Group Name cannot exceed 100 characters.').should('be.visible');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');

        cy.get('[data-cy=groupName]').type('{backspace}');
        cy.get('[data-cy="groupNameError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        // now let's validate id
        // min value
        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').type('12');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');
        cy.get('[data-cy="idError"]').contains('Group ID cannot be less than 3 characters.');
        cy.get('[data-cy="idInputValue"]').type('3');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        // max value
        const invalidId = Array(51).fill('a').join('');
        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').fill(invalidId);
        cy.get('[data-cy="idError"]').contains('Group ID cannot exceed 50 characters.');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');

        cy.get('[data-cy=idInputValue]').type('{backspace}');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');
    });

    it('Skills Group modal - input validation - name or id already exist', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');
        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');

        // validate against skill
        cy.get('[data-cy="groupName"]').type('Very Great Skill 1');
        cy.get('[data-cy=groupNameError]').contains('The value for the Group Name is already taken.').should('be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="groupName"]').type('a');
        cy.get('[data-cy="groupNameError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        cy.get('[data-cy="groupName"]').clear()
        // validate against group
        cy.get('[data-cy="groupName"]').type('Awesome Group 1');
        cy.get('[data-cy=groupNameError]').contains('The value for the Group Name is already taken.').should('be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.disabled');
        cy.get('[data-cy="groupName"]').type('a');
        cy.get('[data-cy="groupNameError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        // now let's test id field
        cy.get('[data-cy="idInputEnableControl"]').contains('Enable').click();
        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').type('skill1');
        cy.get('[data-cy="idError"]').contains('The value for the Group ID is already taken.');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');

        cy.get('[data-cy="idInputValue"]').type('a');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');

        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').type('group1');
        cy.get('[data-cy="idError"]').contains('The value for the Group ID is already taken.');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');

        cy.get('[data-cy="idInputValue"]').type('a');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveGroupButton"]').should('be.enabled');
    });

    it('Skills Group modal - input validation - description custom validation', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newGroupButton"]').click();

        cy.get('[data-cy="groupName"]').type('Awesome Group 1');
        cy.get('[data-cy="groupDescription"]').type('ldkj aljdl aj\n\njabberwocky');

        cy.get('[data-cy="groupDescriptionError"]').contains('Group Description - paragraphs may not contain jabberwocky');
        cy.get('[data-cy=saveGroupButton]').should('be.disabled');
    });

});