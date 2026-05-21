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
const dayjs = require('dayjs');

describe('Tag Skills Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 });
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
    });

    it('Add events in the past', () => {
        const today = dayjs();
        const yesterday = dayjs().subtract(22, 'day');

        const todayMonth = today.month();
        const yesterdayDayOfMonth = yesterday.date(); // Day of the month (1–31)
        const yesterdayMonth = yesterday.month();

        const dayToSelect = yesterdayDayOfMonth

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        cy.get('[data-cy="eventDatePicker"]').click()
        cy.log(`todayMonth: ${todayMonth}, yesterdayMonth: ${yesterdayMonth}`)
        if (todayMonth !== yesterdayMonth) {
            cy.get('[data-pc-section="calendar"] [data-pc-name="pcprevbutton"]').click()
        }
        cy.get(`[data-pc-section="calendar"] [data-pc-section="daycell"][aria-label="${dayToSelect}"] [aria-disabled="false"]`).click()
        const expectedFieldValue = yesterday.format('MM/DD/YYYY')
        cy.get('[data-cy="eventDatePicker"] input').should('have.value', expectedFieldValue)


        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 1 skill will be added for 1 user');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
        ], 10, true, null, false);

        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.get('[data-cy="nav-Users"]').click()
        const expectedDateCellValue = yesterday.format('YYYY-MM-DD')
        cy.validateTable('[data-cy="usersTable"]', [
            [{ colIndex: 0,  value: 'user1' }, { colIndex: 4, value: expectedDateCellValue }],
        ], 10, true, null, false);

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
        cy.get('[data-cy="batchUserList"]').type('user1{enter}user2{enter}user3{enter}  USER3   {enter}  uSeR3');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skills will be added for 3 users');
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

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skills will be added for 2 users');
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

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skills will be added for 1 user');
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

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skills will be added for 1 user');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'This skill was already performed within the configured time period (within the last 8 hours)' }],
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'This skill reached its maximum points' }],
        ], 10, true, null, false);
    });

    it('Can not add too many events', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.continue((res) => {
                res.body.maxSkillBatchSize = 10
            })
        }).as('getConfig')
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@getConfig');

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
        for(let i = 0; i < 4; i++) {
            cy.get('[data-cy="batchUserList"]').type('user' + i + '{enter}');
        }
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="batchErrorMessage"]').contains('Your batch exceeds the 10 request limit (3 skills × 4 users).');
        cy.get('[data-cy="saveBatchSkillEvents"]').should('be.disabled');
        cy.get('[data-cy="lastBackButton"]').should('be.enabled')

        cy.get('[data-cy="lastBackButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('{end}{backspace}{backspace}{backspace}{backspace}{backspace}{backspace}{backspace}')

        cy.get('[data-cy="secondNextButton"]').click();
        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 3 skills will be added for 3 users');
        cy.get('[data-cy="saveBatchSkillEvents"]').should('be.enabled');
        cy.get('[data-cy="lastBackButton"]').should('be.enabled')

    });

    it('User suggestion sent to back end', () => {
        cy.intercept('POST', '/admin/projects/proj1/reportSkillEvents').as('saveSkills')

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

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skills will be added for 2 users');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.wait('@saveSkills').then((interception) => {
            expect(interception.request.body.suggestionOption).to.equal('TWO');
        });

    });

    it('spaces are not allowed in user id', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')

        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('us er1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 1 skill will be added for 1 user');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{colIndex: 0, value: 'Rejected' }, { colIndex: 1,  value: 'us er1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Spaces are not allowed in user id. Provided [us er1]' }],
        ], 10, true, null, false);
    });

    it('Insufficient project points - cannot report', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { pointIncrement: 1, numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 2, { pointIncrement: 1, numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 3, { pointIncrement: 1, numPerformToCompletion: 1 });

        cy.visit('/administrator/projects/proj2/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')

        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 1 skill will be added for 1 user');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{colIndex: 0, value: 'Rejected' }, { colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Insufficient project points, skill achievement is disallowed' }],
        ], 10, true, null, false);
    });

    it('Insufficient subject points - cannot report', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { pointIncrement: 1, numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 2, { pointIncrement: 1, numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 3, { pointIncrement: 1, numPerformToCompletion: 1 });

        cy.createSubject(2, 2);
        cy.createSkill(2, 2, 4, { pointIncrement: 500, numPerformToCompletion: 1 });

        cy.visit('/administrator/projects/proj2/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')

        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 1 skill will be added for 1 user');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{colIndex: 0, value: 'Rejected' }, { colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Insufficient Subject points, skill achievement is disallowed' }],
        ], 10, true, null, false);
    });

    it('Project admin bypass approval for approval-based self report skills', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { selfReportingType: 'Approval', numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 2, { selfReportingType: 'Approval', numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 3, { selfReportingType: 'Approval', numPerformToCompletion: 1 });

        cy.visit('/administrator/projects/proj2/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 1 skill will be added for 1 user');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
        ], 10, true, null, false);

        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.get('[data-cy="nav-Users"]').click()
        cy.validateTable('[data-cy="usersTable"]', [
            [{ colIndex: 0,  value: 'user1' }],
        ], 10, true, null, false);

    });

    it('Cannot report quiz-based skills', () => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);

        cy.createSkill(1, 1, 4, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');
        cy.get('[data-cy="manageSkillLink_skill4"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 4')

        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 1 skill will be added for 1 user');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{colIndex: 0, value: 'Rejected' }, { colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill4' }, { colIndex: 3, value: 'Cannot report skill events directly to a quiz-based skill.' }],
        ], 10, true, null, false);
    });

    it('able to report user with slashes', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)

        cy.get('[data-cy="skillsToAdd"]').contains('Very Great Skill 3')
        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('foo/bar');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 1 skill will be added for 1 user');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'foo/bar' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
        ], 10, true, null, false);
    });

    it('cannot report a disable skill', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { enabled: false, numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 2, { numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 3, { enabled: false, numPerformToCompletion: 1 });

        cy.visit('/administrator/projects/proj2/subjects/subj1');

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
        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 3 skills will be added for 1 user');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{colIndex: 0, value: 'Rejected' }, { colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Cannot report skill events for a skill that is disabled.' }],
            [{colIndex: 0, value: 'Applied' }, { colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill2' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{colIndex: 0, value: 'Rejected' }, { colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Cannot report skill events for a skill that is disabled.' }],
        ], 10, true, null, false);

    });

    it('cannot report catalog imported skill', () => {
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.wait(200);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').should('not.be.visible');
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').should('not.be.visible');
    });

    it('progress bar when there are more than 1 request', () => {
        const numMillisPerSkillEventInBatchReporting = 600
        const numSkillEvents = 6
        cy.intercept('GET', '/public/config', (req) => {
            req.continue((res) => {
                res.body.numMillisPerSkillEventInBatchReporting = numMillisPerSkillEventInBatchReporting
            })
        }).as('getConfig')
        cy.intercept('/admin/projects/proj1/reportSkillEvents', (req) => {
            req.reply((res) => {
                res.send({ delay: (numMillisPerSkillEventInBatchReporting * numSkillEvents) + 1000 });
            });
        }).as('saveSkillEvents');
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('@getConfig')

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)
        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1{enter}user2{enter}user3{enter}');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 2 skills will be added for 3 users');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();
        cy.get('[data-cy="batchSaveLoadingMsg"]').contains('Reporting 6 skill events (2 skills × 3 users)')
        cy.get('[data-cy="batchSaveProgressBar"]').should('be.visible');
        cy.get('[data-cy="batchSaveLoader"]').should('not.exist');
        cy.wait('@saveSkillEvents');

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user2' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user2' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user3' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
            [{ colIndex: 1,  value: 'user3' }, { colIndex: 2,  value: 'skill1' }, { colIndex: 3, value: 'Skill event was applied' }],
        ], 10, true, null, false);
    });

    it('loader when there is only 1 request', () => {
        const numMillisPerSkillEventInBatchReporting = 1500
        const numSkillEvents = 1
        cy.intercept('GET', '/public/config', (req) => {
            req.continue((res) => {
                res.body.numMillisPerSkillEventInBatchReporting = numMillisPerSkillEventInBatchReporting
            })
        }).as('getConfig')
        cy.intercept('/admin/projects/proj1/reportSkillEvents', (req) => {
            req.reply((res) => {
                res.send({ delay: (numMillisPerSkillEventInBatchReporting * numSkillEvents) + 1000 });
            });
        }).as('saveSkillEvents');
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('@getConfig')

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Report Skills for Users"]', true)
        cy.get('[data-cy="firstNextButton"]').click();
        cy.get('[data-cy="batchUserList"]').type('user1');
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy="confirmMessage"]').contains('Skill events for 1 skill will be added for 1 user on');
        cy.get('[data-cy="saveBatchSkillEvents"]').click();
        cy.get('[data-cy="batchSaveLoadingMsg"]').contains('Reporting 1 skill event')
        cy.get('[data-cy="batchSaveLoader"]').should('be.visible');
        cy.get('[data-cy="batchSaveProgressBar"]').should('not.exist');
        cy.wait('@saveSkillEvents');

        cy.validateTable('[data-cy="skillEventBatchResult"]', [
            [{ colIndex: 1,  value: 'user1' }, { colIndex: 2,  value: 'skill3' }, { colIndex: 3, value: 'Skill event was applied' }],
        ], 10, true, null, false);
    });

});