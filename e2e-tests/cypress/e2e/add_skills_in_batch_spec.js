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
describe('Tag Skills Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 });
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
    });

    it('Add multiple skills for users', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 1')

        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1{enter}user2{enter}user3{enter}');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skill(s) will be added for 3 user(s)');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user2' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user2' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user3' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user3' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Skill event was applied' }],
        ], 10, true, null, false);
    });

    it('Filter out empty users', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 1')

        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1{enter}    {enter}user3{enter}   ');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skill(s) will be added for 2 user(s)');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user3' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user3' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Skill event was applied' }],
        ], 10, true, null, false);
    });

    it('Add some events that fail', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 1')

        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skill(s) will be added for 1 user(s)');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Skill event was applied' }],
        ], 10, true, null, false);

        cy.get('[data-cy="saveDialogBtn"]').click();

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 1')

        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skill(s) will be added for 1 user(s)');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'This skill was already performed within the configured time period (within the last 8 hours)' }],
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'This skill reached its maximum points' }],
        ], 10, true, null, false);
    });

    it('Can not add too many events', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 1')

        cy.get('[data-cy="firstNextButton"]').click();
        for(let i = 0; i < 100; i++) {
            cy.get('[data-cy="batchUserList"]').type('user' + i + '{enter}');
        }
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="batchErrorMessage"]').contains('Your batch exceeds the');
        cy.get('[data-cy="batchErrorMessage"]').contains('request limit (3 skills × 100 users). To proceed, please remove either users or skills to reduce the total number of requests.');
        cy.get('[data-cy="saveBatchSkillEvents"]').should('be.disabled');
    });


    it('User suggestion sent to back end', () => {
        cy.intercept('PUT', '/admin/projects/proj1/skills').as('saveSkills')

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 1')

        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1{enter}user3');

        cy.contains('ONE').click()
        cy.contains('TWO').click()
        cy.get('[data-cy="userSuggestOptionsDropdown"]').contains('TWO')

        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skill(s) will be added for 2 user(s)');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.wait('@saveSkills').then((interception) => {
            expect(interception.request.body.suggestionOption).to.equal('TWO');
        });

    });
});