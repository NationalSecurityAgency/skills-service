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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Admin Group CRUD Tests', () => {

    const adminGroupTableSelector = '[data-cy="adminGroupDefinitionsTable"]';
    beforeEach(() => {

    });

    it('create a an admin group', function () {
        cy.visit('/administrator/adminGroups/')
        cy.get('[data-cy="noAdminGroupsYet"]')

        cy.get('[data-cy="btn_Admin Groups"]').click()
        cy.get('.p-dialog-header').contains('New Admin Group')

        cy.get('[data-cy="adminGroupName"]').type('My First Admin Group')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'MyFirstAdminGroup')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="adminGroupName"]').should('not.exist')
        cy.get('[data-cy="btn_Admin Groups"]').should('have.focus')
        cy.validateTable(adminGroupTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Admin Group'
            }],
        ], 5);

        // refresh and revalidate
        cy.visit('/administrator/adminGroups/')
        cy.validateTable(adminGroupTableSelector, [
            [{
                colIndex: 0,
                value: 'My First Admin Group'
            }],
        ], 5);
    });

    it('Admin Group Modal Validation: Name', function () {
        cy.createAdminGroupDef(1, { name: 'Already Exist' });

        cy.visit('/administrator/adminGroups/')

        cy.get('[data-cy="btn_Admin Groups"]').click()
        cy.get('.p-dialog-header').contains('New Admin Group')

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        // name is not taken
        cy.get('[data-cy="adminGroupName"]').type('Already Exist')
        cy.get('[data-cy="adminGroupNameError"]').contains('The value for the Admin Group Name is already taken')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        // min 3 chars
        cy.get('[data-cy="adminGroupName"]').clear().type('ab')
        cy.get('[data-cy="adminGroupNameError"]').contains('Admin Group Name must be at least 3 characters')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="adminGroupName"]').type('c')
        cy.get('[data-cy="adminGroupNameError"]').invoke('text').invoke('trim').should('equal', '')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // max 75 chars
        const longName = new Array(100).join('A');
        cy.get('[data-cy="adminGroupName"]').clear().fill(longName)
        cy.get('[data-cy="adminGroupName"]').type('AA')
        cy.get('[data-cy="adminGroupNameError"]').contains('Admin Group Name must be at most 100 characters')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="adminGroupName"]').type('{backspace}')
        cy.get('[data-cy="adminGroupNameError"]').invoke('text').invoke('trim').should('equal', '')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // required field
        cy.get('[data-cy="adminGroupName"]').clear()
        cy.get('[data-cy="adminGroupNameError"]').contains('Admin Group Name is a required field')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        
        // custom validators
        cy.get('[data-cy="adminGroupName"]')
          .type('{selectall}(A) Updated Admin Group Name');
        cy.get('[data-cy="adminGroupNameError"]')
          .contains('names may not contain (A)');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.disabled');

        cy.get('[data-cy="adminGroupName"]')
          .type('{selectall}(B) A Updated Admin Group Name');
        cy.get('[data-cy="adminGroupNameError"]')
          .invoke('text').invoke('trim').should('equal', '');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.enabled');
    });

    it('admin group name search / filtering', function () {
        cy.createAdminGroupDef(1, { name: 'a Admin Group 1' });
        cy.createAdminGroupDef(2, { name: 'b Admin Group Even 1' });
        cy.createAdminGroupDef(3, { name: 'c Admin Group 2' });
        cy.createAdminGroupDef(4, { name: 'd Admin Group  Even 2' });
        cy.createAdminGroupDef(5, { name: 'e Admin Group 3' });
        cy.createAdminGroupDef(6, { name: 'f Admin Group  Even 3' });

        cy.visit('/administrator/adminGroups/')

        // sort by name
        const headerSelector = `${adminGroupTableSelector} thead tr th`;
        cy.get(headerSelector)
            .contains('Name')
            .click();

        cy.get(`${adminGroupTableSelector}`).find('[data-cy="skillsBTableTotalRows"]').should('have.text', 6)
        cy.get('[data-cy="adminGroupNameFilter"]').type('3')
        cy.get(`${adminGroupTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 2)

        cy.get(`${adminGroupTableSelector} [data-p-index="0"] [data-cy="managesAdminGroupLink_adminGroup5"]`)
        cy.get(`${adminGroupTableSelector} [data-p-index="1"] [data-cy="managesAdminGroupLink_adminGroup6"]`)
        cy.get(`${adminGroupTableSelector} [data-p-index="3"]`).should('not.exist')

        cy.get('[data-cy="filterResetBtn"]').click()
        cy.get(`${adminGroupTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 6)
        cy.get('[data-cy="adminGroupNameFilter"]').should('have.value', '')
        cy.get('[data-cy="adminGroupNameFilter"]').type('eVeN{enter}')

        cy.get(`${adminGroupTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 3)

        cy.get(`${adminGroupTableSelector} [data-p-index="0"] [data-cy="managesAdminGroupLink_adminGroup2"]`)
        cy.get(`${adminGroupTableSelector} [data-p-index="1"] [data-cy="managesAdminGroupLink_adminGroup4"]`)
        cy.get(`${adminGroupTableSelector} [data-p-index="2"] [data-cy="managesAdminGroupLink_adminGroup6"]`)
        cy.get(`${adminGroupTableSelector} [data-p-index="3"]`).should('not.exist')

        cy.get('[data-cy="adminGroupNameFilter"]').type('{backspace}{backspace}{backspace}{backspace}{backspace}{backspace}{enter}')
        cy.get(`${adminGroupTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 6)
    });

    it('edit existing admin group', function () {
        cy.createAdminGroupDef(1);
        cy.createAdminGroupDef(2);

        cy.visit('/administrator/adminGroups/')
        cy.get('[data-cy="editAdminGroupButton_adminGroup1"]').click()
        cy.get('[data-cy="adminGroupName"]').should('have.value','This is admin group 1')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'adminGroup1')

        cy.get('[data-cy="adminGroupName"]').type('A')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="adminGroupName"]').should('not.exist')

        cy.get(`${adminGroupTableSelector} [data-p-index="0"]`).contains('This is admin group 1A')
        cy.get('[data-cy="editAdminGroupButton_adminGroup1"]').should('have.focus')

        cy.visit('/administrator/adminGroups/')
        cy.get('[data-cy="editAdminGroupButton_adminGroup1"]').click()
        cy.get('[data-cy="adminGroupName"]').should('have.value','This is admin group 1A')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'adminGroup1')
    });

    it('cannot edit an existing admin group\'s id', function () {
        cy.createAdminGroupDef(1);
        cy.createAdminGroupDef(2);

        cy.visit('/administrator/adminGroups/')
        cy.get('[data-cy="editAdminGroupButton_adminGroup2"]').click()
        cy.get('[data-cy="adminGroupName"]').should('have.value','This is admin group 2')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'adminGroup2')

        cy.get('[data-cy="managesAdminGroupLink_adminGroup2"]').should('exist')
        cy.get('[data-cy="managesAdminGroupBtn_adminGroup2"]').should('exist')

        cy.get('[data-cy="enableIdInput"]').should('not.be.visible')
        cy.get('[data-cy="idInputValue"]').should('not.be.visible')
    });

    it('edit admin group on the admin group page', function () {
        cy.createAdminGroupDef(1);

        cy.visit('/administrator/adminGroups/adminGroup1')
        cy.get('[data-cy="editAdminGroupButton"]').click()
        cy.get('[data-cy="adminGroupName"]').should('have.value','This is admin group 1')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'adminGroup1')

        cy.get('[data-cy="adminGroupName"]').type('A')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="adminGroupName"]').should('not.exist')

        cy.get('[data-cy="pageHeader"]').contains('This is admin group 1A')
        cy.get('[data-cy="editAdminGroupButton"]').should('have.focus')

        cy.visit('/administrator/adminGroups/adminGroup1')
        cy.get('[data-cy="editAdminGroupButton"]').click()
        cy.get('[data-cy="adminGroupName"]').should('have.value','This is admin group 1A')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'adminGroup1')
    });

    it('admin group id is derived from name', function () {
        const expectedId = 'LotsofspecialPchars';
        const providedName = "!L@o#t$s of %s^p&e*c(i)/?#a_l++_|}{P c'ha'rs";

        cy.visit('/administrator/adminGroups')
        cy.get('[data-cy="btn_Admin Groups"]').click()

        cy.get('[data-cy="adminGroupName"]').type(providedName, { delay: 100 })
        cy.get('[data-cy="idInputValue"]').should('have.value', expectedId)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="adminGroupName"]').should('not.exist')

        // id is not derived from name during edit
        cy.get(`[data-cy="editAdminGroupButton_${expectedId}"]`).click()
        cy.get('[data-cy="idInputValue"]').should('have.value', expectedId)
        cy.get('[data-cy="adminGroupName"]').type('More', { delay: 100 })
        cy.get('[data-cy="idInputValue"]').should('have.value', expectedId)
    });

    it('edit admin group modal close and cancel focus on the edit button', function () {
        cy.createAdminGroupDef(1);

        cy.visit('/administrator/adminGroups/')
        cy.get('[data-cy="editAdminGroupButton_adminGroup1"]').click()
        cy.get('[data-cy="adminGroupName"]').should('exist')

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="adminGroupName"]').should('not.exist')
        cy.get('[data-cy="editAdminGroupButton_adminGroup1"]').should('have.focus')

        cy.get('[data-cy="editAdminGroupButton_adminGroup1"]').click()
        cy.get('[data-cy="adminGroupName"]').should('exist')

        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('[data-cy="adminGroupName"]').should('not.exist')
        cy.get('[data-cy="editAdminGroupButton_adminGroup1"]').should('have.focus')
    });

    it('delete admin group', function () {
        cy.createAdminGroupDef(1);
        cy.createAdminGroupDef(2);
        cy.createAdminGroupDef(3);
        cy.createAdminGroupDef(4);

        cy.visit('/administrator/adminGroups/')

        cy.get(`${adminGroupTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 4)
        cy.get('[data-cy="deleteAdminGroupButton_adminGroup1"]').should('exist')
        cy.get('[data-cy="deleteAdminGroupButton_adminGroup2"]').should('exist')
        cy.get('[data-cy="deleteAdminGroupButton_adminGroup3"]').should('exist')
        cy.get('[data-cy="deleteAdminGroupButton_adminGroup4"]').should('exist')

        cy.openDialog('[data-cy="deleteAdminGroupButton_adminGroup2"]')
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove This is admin group 2 Admin Group')
        cy.get('[data-cy="currentValidationText"]').type('Delete Me')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get(`${adminGroupTableSelector} [data-cy="skillsBTableTotalRows"]`).should('have.text', 3)
        cy.get('[data-cy="deleteAdminGroupButton_adminGroup1"]').should('exist')
        cy.get('[data-cy="deleteAdminGroupButton_adminGroup2"]').should('not.exist')
        cy.get('[data-cy="deleteAdminGroupButton_adminGroup3"]').should('exist')
        cy.get('[data-cy="deleteAdminGroupButton_adminGroup4"]').should('exist')

        cy.get('[data-cy="deleteAdminGroupButton_adminGroup1"]').click()
        cy.get('[data-cy="removalSafetyCheckMsg"]').contains('This will remove This is admin group 1 Admin Group')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="deleteAdminGroupButton_adminGroup1"]').should('have.focus')
    });

    it('closing modal returns focus on the "New Admin Group button"', function () {
        cy.visit('/administrator/adminGroups/')
        cy.get('[data-cy="noAdminGroupsYet"]')

        cy.get('[data-cy="btn_Admin Groups"]').click()
        cy.get('.p-dialog-header').contains('New Admin Group')

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="btn_Admin Groups"]').should("have.focus")

        cy.get('[data-cy="btn_Admin Groups"]').click()
        cy.get('.p-dialog-header').contains('New Admin Group')

        cy.get('.p-dialog-header [aria-label="Close"]').click()
        cy.get('[data-cy="btn_Admin Groups"]').should("have.focus")
    });

    it('sort column and order is saved in local storage', () => {
        cy.createAdminGroupDef(1, { name: 'a Admin Group 1' });
        cy.createAdminGroupDef(2, { name: 'b Even 1' });
        cy.createAdminGroupDef(3, { name: 'c Admin Group 2' });
        cy.createAdminGroupDef(4, { name: 'd Even 2' });
        cy.createAdminGroupDef(5, { name: 'e Admin Group 3' });

        cy.visit('/administrator/adminGroups/')

        // initial sort order
        cy.validateTable(adminGroupTableSelector, [
            [{ colIndex: 0, value: 'a Admin Group 1' }],
            [{ colIndex: 0, value: 'b Even 1' }],
            [{ colIndex: 0, value: 'c Admin Group 2' }],
            [{ colIndex: 0, value: 'd Even 2' }],
            [{ colIndex: 0, value: 'e Admin Group 3' }],
        ], 5);

        // sort by name
        const headerSelector = `${adminGroupTableSelector} thead tr th`;
        cy.get(headerSelector)
          .contains('Name')
          .click();

        cy.validateTable(adminGroupTableSelector, [
            [{ colIndex: 0, value: 'a Admin Group 1' }],
            [{ colIndex: 0, value: 'b Even 1' }],
            [{ colIndex: 0, value: 'c Admin Group 2' }],
            [{ colIndex: 0, value: 'd Even 2' }],
            [{ colIndex: 0, value: 'e Admin Group 3' }],
        ], 5);

        cy.get(headerSelector)
          .contains('Name')
          .click();

        cy.validateTable(adminGroupTableSelector, [
            [{ colIndex: 0, value: 'e Admin Group 3' }],
            [{ colIndex: 0, value: 'd Even 2' }],
            [{ colIndex: 0, value: 'c Admin Group 2' }],
            [{ colIndex: 0, value: 'b Even 1' }],
            [{ colIndex: 0, value: 'a Admin Group 1' }],
        ], 5);

        // refresh and validate
        cy.visit('/administrator/adminGroups/')
        cy.validateTable(adminGroupTableSelector, [
            [{ colIndex: 0, value: 'e Admin Group 3' }],
            [{ colIndex: 0, value: 'd Even 2' }],
            [{ colIndex: 0, value: 'c Admin Group 2' }],
            [{ colIndex: 0, value: 'b Even 1' }],
            [{ colIndex: 0, value: 'a Admin Group 1' }],
        ], 5);
    });

});

