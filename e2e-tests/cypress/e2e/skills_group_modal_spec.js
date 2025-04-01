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

describe('Skills Group Modal Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });
    const tableSelector = '[data-cy="skillsTable"]';

    it('Skills Group modal - id is auto generated based on name - special chars', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="name"]').type('Great !@#$% Name %^&*(+_)(');
        cy.get('[data-cy="idInputValue"]').should('have.value','GreatNameGroup');

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('Skills Group modal - id must not be auto generated based on name when id input is enabled', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="idInputValue"]').should('be.disabled');
        cy.get('[data-cy=enableIdInput]').click();
        cy.get('[data-cy="idInputValue"]').should('be.enabled');

        cy.get('[data-cy="name"]').type('Great');
        cy.get('[data-cy="idInputValue"]').should('have.value','');

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
    });

    it('Skills Group modal - input validation - min/max length', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxIdLength = 50;
                res.send(conf);
            });
        }).as('loadConfig')
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="noContent"]').contains('No Skills Yet');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="name"]').type('12');
        cy.get('[data-cy="nameError"]').contains('Group Name must be at least 3 characters');
        cy.get('[data-cy="name"]').type('3');
        cy.get('[data-cy="nameError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        // so id doesn't change anymore
        cy.get('[data-cy=enableIdInput]').click();

        // max value
        // Group Name cannot exceed 100 characters.
        const invalidName = Array(101).fill('a').join('');
        cy.get('[data-cy=name]').clear()
        cy.get('[data-cy=name]').fill(invalidName);
        cy.get('[data-cy=nameError]').contains('Group Name must be at most 100 characters').should('be.visible');
        cy.get('[data-cy=saveDialogBtn]').should('be.disabled');

        cy.get('[data-cy=name]').type('{backspace}');
        cy.get('[data-cy="nameError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        // now let's validate id
        // min value
        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').type('12');
        cy.get('[data-cy=saveDialogBtn]').should('be.disabled');
        cy.get('[data-cy="idError"]').contains('Group ID must be at least 3 characters');
        cy.get('[data-cy="idInputValue"]').type('3');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        // max value
        const invalidId = Array(51).fill('a').join('');
        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').fill(invalidId);
        cy.get('[data-cy="idError"]').contains('Group ID must be at most 50 characters');
        cy.get('[data-cy=saveDialogBtn]').should('be.disabled');

        cy.get('[data-cy=idInputValue]').type('{backspace}');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('Skills Group modal - input validation - name or id already exist', () => {
        cy.createSkillsGroup(1, 1, 1);
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newGroupButton"]').click();
        cy.get('[data-cy="EditSkillGroupModal"]').contains('New Skills Group');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');

        // validate against skill
        cy.get('[data-cy="name"]').type('Very Great Skill 1a');
        cy.get('[data-cy="nameError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="name"]').type('{backspace}');
        cy.get('[data-cy=nameError]').contains('The value for the Group Name is already taken').should('be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="name"]').type('a');
        cy.get('[data-cy="nameError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        cy.get('[data-cy="name"]').clear()
        // validate against group
        cy.get('[data-cy="name"]').type('Awesome Group 1a');
        cy.get('[data-cy="nameError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="name"]').type('{backspace}');
        cy.get('[data-cy=nameError]').contains('The value for the Group Name is already taken').should('be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="name"]').type('a');
        cy.get('[data-cy="nameError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        // now let's test id field
        cy.get('[data-cy=enableIdInput]').click();
        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').type('skill1');
        cy.get('[data-cy="idError"]').contains('The value for the Group ID is already taken');
        cy.get('[data-cy=saveDialogBtn]').should('be.disabled');

        cy.get('[data-cy="idInputValue"]').type('a');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');

        cy.get('[data-cy="idInputValue"]').clear();
        cy.get('[data-cy="idInputValue"]').type('group1');
        cy.get('[data-cy="idError"]').contains('The value for the Group ID is already taken');
        cy.get('[data-cy=saveDialogBtn]').should('be.disabled');

        cy.get('[data-cy="idInputValue"]').type('a');
        cy.get('[data-cy="idError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('Skills Group modal - input validation - description custom validation', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newGroupButton"]').click();

        cy.get('[data-cy="name"]').type('Awesome Group 1');
        cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky');

        cy.get('[data-cy="descriptionError"]').contains('Group Description - paragraphs may not contain jabberwocky');
        cy.get('[data-cy=saveDialogBtn]').should('be.disabled');
    });

    it('Skills Group modal - edit existing group', () => {
        cy.createSkillsGroup(1, 1, 1, { description: 'first group description' })
        cy.createSkillsGroup(1, 1, 2, { description: 'second group description' })
        cy.createSkillsGroup(1, 1, 3, { description: 'third group description' })
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // cy.get('[data-cy="expandDetailsBtn_group2"]').click();
        cy.get('[data-p-index="1"] [data-pc-section="rowtogglebutton"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group2"] [data-cy="description"]').contains('second group description');

        const makdownDivSelector = '#markdown-editor div.toastui-editor-main.toastui-editor-ww-mode > div > div.toastui-editor-ww-container > div > div'
        cy.get('[data-cy="editSkillButton_group2"]').click();
        cy.get(makdownDivSelector).should('have.text', 'second group description');

        cy.get('[data-cy="descriptionMarkdownEditor"] [data-cy="markdownEditorInput"]').clear().type('another value');
        cy.get('[data-cy="name"]').clear().type('Updated Group Name');
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="EditSkillGroupModal"]').should('not.exist');

        cy.get('[data-cy="nameCell_group2"]').contains('Updated Group Name')
        // cy.get('[data-cy="expandDetailsBtn_group2"]').click();
        // cy.get('[data-p-index="1"] [data-pc-section="rowtogglebutton"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group2"] [data-cy="description"]').contains('another value');

        cy.get('[data-cy="editSkillButton_group2"]').click();
        cy.get('[data-cy="name"]').should('have.value', 'Updated Group Name');
        cy.get(makdownDivSelector).should('have.text', 'another value');
    });

    it('Skills Group modal - edit id of an existing group', () => {
        cy.createSkillsGroup(1, 1, 1, { description: 'first group description' })
        cy.createSkillsGroup(1, 1, 2, { description: 'second group description' })
        cy.createSkillsGroup(1, 1, 3, { description: 'third group description' })
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="editSkillButton_group2"]').click();
        cy.get('[data-cy=enableIdInput]').click();
        cy.get('[data-cy="idInputValue"]').should('have.value','group2');
        cy.get('[data-cy="idInputValue"]').clear().type('newId');
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="editSkillButton_newId"]').should('exist');
        cy.get('[data-cy="editSkillButton_group2"]').should('not.exist');

        cy.get('[data-cy="editSkillButton_newId"]').click();
        cy.get('[data-cy=enableIdInput]').click();
        cy.get('[data-cy="idInputValue"]').should('have.value','newId');

    });

});