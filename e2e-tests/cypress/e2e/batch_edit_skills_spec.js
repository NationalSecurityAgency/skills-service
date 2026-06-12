/*
 * Copyright 2026 SkillTree
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

describe('batch edit skills', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 });
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
    });

    it.only('batch edit skills', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        // cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)
        //
        // cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        // cy.get('[data-cy="eventDatePicker"]').click()
        // cy.log(`todayMonth: ${todayMonth}, yesterdayMonth: ${yesterdayMonth}`)
        // if (todayMonth !== yesterdayMonth) {
        //     cy.get('[data-pc-section="calendar"] [data-pc-name="pcprevbutton"]').click()
        // }
        // cy.get(`[data-pc-section="calendar"] [data-pc-section="daycell"][aria-label="${dayToSelect}"] [aria-disabled="false"]`).click()
        // const expectedFieldValue = yesterday.format('MM/DD/YYYY')
        // cy.get('[data-cy="eventDatePicker"] input').should('have.value', expectedFieldValue)
        //
        //
        // cy.get('[data-cy="firstNextButton"]').click();
        // cy.get('[data-cy="batchUserList"]').type('user1');
        // cy.get('[data-cy="secondNextButton"]').click();
        //
        // cy.get('[data-cy="confirmMessage"]').contains('Skill events for 1 skill will be added for 1 user');
        // cy.get('[data-cy="saveBatchSkillEvents"]').click();
        //
        // cy.validateTable('[data-cy="skillEventBatchResult"]', [
        //     [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
        // ], 10, true, null, false);
        //
        // cy.get('[data-cy="saveDialogBtn"]').click();
        // cy.get('[data-cy="nav-Users"]').click()
        // const expectedDateCellValue = yesterday.format('YYYY-MM-DD')
        // cy.validateTable('[data-cy="usersTable"]', [
        //     [{ colIndex: 0,  value: 'user1' }, { colIndex: 4, value: expectedDateCellValue }],
        // ], 10, true, null, false);

    });

});