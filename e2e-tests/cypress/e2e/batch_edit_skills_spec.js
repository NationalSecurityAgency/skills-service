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
    });

    it('batch edit skills', () => {
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 2, { enabled: false});
        cy.createSkill(1, 1, 3);
        cy.viewport(1800, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Self Report"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Time Window"]').click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()

        cy.validateTable('[data-cy="skillsTable"]', [
            [{ colIndex: 2,  value: "Skill 3" }, { colIndex: 5,  value: "100 pts x 2" }, { colIndex: 6,  value: "Disabled" },
                { colIndex: 7,  value: "8 Hours" }, { colIndex: 7,  value: "Minimum Time Window between occurrences to receive"}],
            [{ colIndex: 2,  value: "Skill 2" }, { colIndex: 5,  value: "100 pts x 2" }, { colIndex: 6,  value: "Disabled" },
                { colIndex: 7,  value: "8 Hours" }, { colIndex: 7,  value: "Minimum Time Window between occurrences to receive"}],
            [{ colIndex: 2,  value: "Skill 1" }, { colIndex: 5,  value: "100 pts x 1" }, { colIndex: 6,  value: "Disabled" }, { colIndex: 7,  value: "N/A" }],
        ])

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Batch Edit"]', true)

        cy.get('[data-cy="batchUpdateMsg"]').contains('Update 2 Skills at once. Provide at least one attribute below to apply changes.')

        cy.get('[data-cy="pointIncrement"]').type('111')
        cy.get('[data-cy="numPerformToCompletion"]').type('3')
        cy.get('[data-cy="calculatedTotalPoints"] [data-pc-name="pcinputtext"]').should('have.value', '333')
        cy.get('[data-cy="selfReportingType"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Approval Queue"]').click()

        cy.get('[data-cy="timeWindowInput"] [data-pc-section="togglebutton"]').click()
        cy.get('[data-cy="pointIncrementIntervalHrs"] [data-pc-name="pcinputtext"]').should('be.disabled')
        cy.get('[data-cy="pointIncrementIntervalMins"] [data-pc-name="pcinputtext"]').should('be.disabled')
        cy.get('[data-cy="numPointIncrementMaxOccurrences"] [data-pc-name="pcinputtext"]').should('be.disabled')
        cy.get('[data-cy="timeWindowCheckbox"]').click()
        cy.get('[data-cy="pointIncrementIntervalHrs"] [data-pc-name="pcinputtext"]').should('be.enabled')
        cy.get('[data-cy="pointIncrementIntervalMins"] [data-pc-name="pcinputtext"]').should('be.enabled')
        cy.get('[data-cy="numPointIncrementMaxOccurrences"] [data-pc-name="pcinputtext"]').should('be.enabled')

        cy.get('[data-cy="pointIncrementIntervalHrs"]').type('24')
        cy.get('[data-cy="pointIncrementIntervalMins"]').type('13')
        cy.get('[data-cy="numPointIncrementMaxOccurrences"]').type('2')

        cy.intercept('POST', '/admin/projects/proj1/batchUpdateSkills').as('batchUpdateSkills')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@batchUpdateSkills')

        cy.validateTable('[data-cy="skillsTable"]', [
            [{ colIndex: 2,  value: "Skill 3" }, { colIndex: 5,  value: "111 pts x 3" }, { colIndex: 6,  value: "Approval" },
                { colIndex: 7,  value: "24 Hours 13 Minutes" }, { colIndex: 7,  value: "Up to 2 occurrences" }],
            [{ colIndex: 2,  value: "Skill 2" }, { colIndex: 5,  value: "111 pts x 3" }, { colIndex: 6,  value: "Approval" },
                { colIndex: 7,  value: "24 Hours 13 Minutes" }, { colIndex: 7,  value: "Up to 2 occurrences" }],
            [{ colIndex: 2,  value: "Skill 1" }, { colIndex: 5,  value: "100 pts x 1" }, { colIndex: 6,  value: "Disabled" }, { colIndex: 7,  value: "N/A" }],
        ])
    });

    it('able to update a single attribute for batch edit', () => {
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 });
        cy.createSkill(1, 1, 2, { enabled: false});
        cy.createSkill(1, 1, 3);
        cy.viewport(1800, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Self Report"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Time Window"]').click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()

        cy.validateTable('[data-cy="skillsTable"]', [
            [{ colIndex: 2,  value: "Skill 3" }, { colIndex: 5,  value: "100 pts x 2" }, { colIndex: 6,  value: "Disabled" },
                { colIndex: 7,  value: "8 Hours" }, { colIndex: 7,  value: "Minimum Time Window between occurrences to receive"}],
            [{ colIndex: 2,  value: "Skill 2" }, { colIndex: 5,  value: "100 pts x 2" }, { colIndex: 6,  value: "Disabled" },
                { colIndex: 7,  value: "8 Hours" }, { colIndex: 7,  value: "Minimum Time Window between occurrences to receive"}],
            [{ colIndex: 2,  value: "Skill 1" }, { colIndex: 5,  value: "100 pts x 1" }, { colIndex: 6,  value: "Disabled" },
                { colIndex: 7,  value: "N/A" }],
        ])

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Batch Edit"]', true)

        cy.get('[data-cy="pointIncrement"]').type('111')

        cy.intercept('POST', '/admin/projects/proj1/batchUpdateSkills').as('batchUpdateSkills')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@batchUpdateSkills')

        cy.validateTable('[data-cy="skillsTable"]', [
            [{ colIndex: 2,  value: "Skill 3" }, { colIndex: 5,  value: "111 pts x 2" }, { colIndex: 6,  value: "Disabled" },
                { colIndex: 7,  value: "8 Hours" }, { colIndex: 7,  value: "Minimum Time Window between occurrences to receive"}],
            [{ colIndex: 2,  value: "Skill 2" }, { colIndex: 5,  value: "111 pts x 2" }, { colIndex: 6,  value: "Disabled" },
                { colIndex: 7,  value: "8 Hours" }, { colIndex: 7,  value: "Minimum Time Window between occurrences to receive"}],
            [{ colIndex: 2,  value: "Skill 1" }, { colIndex: 5,  value: "100 pts x 1" }, { colIndex: 6,  value: "Disabled" },
                { colIndex: 7,  value: "N/A" }],
        ])
    });

    it('batch change skills visibility', () => {
        cy.createSkill(1, 1, 1, { enabled: false });
        cy.createSkill(1, 1, 2, { enabled: false});
        cy.createSkill(1, 1, 3);
        cy.viewport(1800, 1000)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-p-index="1"] [data-cy="disabledBadge-skill2"]')
        cy.get('[data-p-index="2"] [data-cy="disabledBadge-skill1"]')
        cy.get('[data-p-index="0"] [data-cy="disabledBadge-skill3"]').should('not.exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Batch Edit"]', true)
        cy.get('[data-cy="hiddenSkillsMsg"]').contains('2 Skills currently hidden')
        cy.get('[data-cy="checkbox_enabled"]').click()

        cy.intercept('POST', '/admin/projects/proj1/batchUpdateSkills').as('batchUpdateSkills')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@batchUpdateSkills')

        cy.get('[data-p-index="1"] [data-cy="disabledBadge-skill2"]').should('not.exist')
        cy.get('[data-p-index="2"] [data-cy="disabledBadge-skill1"]').should('not.exist')
        cy.get('[data-p-index="0"] [data-cy="disabledBadge-skill3"]').should('not.exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Batch Edit"]', true)
        cy.get('[data-cy="pointIncrement"]').should('be.visible')
        cy.get('[data-cy="hiddenSkillsMsg"]').should('not.exist')
        cy.get('[data-cy="checkbox_enabled"]').should('not.exist')
    });

    it('validate point increment and occurrences', () => {
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 });
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Batch Edit"]', true)

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="pointIncrement"]').type('0')
        cy.get('[data-cy="pointIncrementError"]').contains('Point Increment must be greater than or equal to 1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="pointIncrement"]').type('{backspace}')
        cy.get('[data-cy="pointIncrementError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="pointIncrement"]').type('10001')
        cy.get('[data-cy="pointIncrementError"]').contains('Point Increment must be less than or equal to 10000')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="pointIncrement"]').type('{backspace}0')
        cy.get('[data-cy="pointIncrementError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="numPerformToCompletion"]').type('0')
        cy.get('[data-cy="numPerformToCompletionError"]').contains('Occurrences must be greater than or equal to 1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="numPerformToCompletion"]').type('{backspace}')
        cy.get('[data-cy="numPerformToCompletionError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="numPerformToCompletion"]').type('10001')
        cy.get('[data-cy="numPerformToCompletionError"]').contains('Occurrences must be less than or equal to 10000')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="numPerformToCompletion"]').type('{backspace}0')
        cy.get('[data-cy="numPerformToCompletionError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
    });

    it('validate time window', () => {
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 5, pointIncrementInterval: 0 });
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 5, pointIncrementInterval: 0 });
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Batch Edit"]', true)

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="timeWindowInput"] [data-pc-section="togglebutton"]').click()
        cy.get('[data-cy="pointIncrementIntervalHrs"] [data-pc-name="pcinputtext"]').should('be.disabled')
        cy.get('[data-cy="pointIncrementIntervalMins"] [data-pc-name="pcinputtext"]').should('be.disabled')
        cy.get('[data-cy="numPointIncrementMaxOccurrences"] [data-pc-name="pcinputtext"]').should('be.disabled')
        cy.get('[data-cy="timeWindowCheckbox"]').click()
        cy.get('[data-cy="pointIncrementIntervalHrs"] [data-pc-name="pcinputtext"]').should('be.enabled')
        cy.get('[data-cy="pointIncrementIntervalMins"] [data-pc-name="pcinputtext"]').should('be.enabled')
        cy.get('[data-cy="numPointIncrementMaxOccurrences"] [data-pc-name="pcinputtext"]').should('be.enabled')

        cy.get('[data-cy="pointIncrementIntervalHrs"]').type('0')
        cy.get('[data-cy="pointIncrementIntervalMins"]').type('0')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').contains('Hours must be > 0 if Minutes = 0')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').contains('Minutes must be > 0 if Hours = 0')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="pointIncrementIntervalMins"]').type('{backspace}')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').should('not.exist')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="pointIncrementIntervalMins"]').type('0')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').contains('Hours must be > 0 if Minutes = 0')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').contains('Minutes must be > 0 if Hours = 0')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="pointIncrementIntervalHrs"]').type('{backspace}')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').should('not.exist')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="pointIncrementIntervalHrs"]').type('-1')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').contains('Hours must be greater than or equal to 0')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').contains('Minutes must be > 0 if Hours = 0')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="pointIncrementIntervalHrs"]').type('{backspace}{backspace}')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').should('not.exist')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="pointIncrementIntervalHrs"]').type('721')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').contains('Hours must be less than or equal to 720')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="pointIncrementIntervalHrs"]').type('{backspace}0')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').should('not.exist')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="pointIncrementIntervalMins"]').type('61')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').should('not.exist')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').contains('Minutes must be less than or equal to 60')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="pointIncrementIntervalMins"]').type('{backspace}0')
        cy.get('[data-cy="pointIncrementIntervalHrsError"]').should('not.exist')
        cy.get('[data-cy="pointIncrementIntervalMinsError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="numPointIncrementMaxOccurrences"]').type('0')
        cy.get('[data-cy="numPointIncrementMaxOccurrencesError"]').contains('Max Occurrences must be greater than or equal to 1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="numPointIncrementMaxOccurrences"]').type('{backspace}1')
        cy.get('[data-cy="numPointIncrementMaxOccurrencesError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="numPointIncrementMaxOccurrences"]').type('0')
        cy.get('[data-cy="numPointIncrementMaxOccurrencesError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="numPerformToCompletion"]').type('9')
        cy.get('[data-cy="numPointIncrementMaxOccurrencesError"]').contains('Max Occurrences must be <= total Occurrences to Completion')
        cy.get('[data-cy="numPerformToCompletionError"]').contains('Occurrences must be >= Window\'s Max Occurrences')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="numPerformToCompletion"]').type('{backspace}10')
        cy.get('[data-cy="numPointIncrementMaxOccurrencesError"]').should('not.exist')
        cy.get('[data-cy="numPerformToCompletionError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="numPerformToCompletion"]').type('{backspace}{backspace}')
        cy.get('[data-cy="numPointIncrementMaxOccurrences"]').type('00')
        cy.get('[data-cy="numPointIncrementMaxOccurrencesError"]').contains('Max Occurrences must be less than or equal to 999')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="numPointIncrementMaxOccurrences"]').type('{backspace}')
        cy.get('[data-cy="numPointIncrementMaxOccurrencesError"]').should('not.exist')
        cy.get('[data-cy="numPerformToCompletionError"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
    });

    it('able to reset self report selection', () => {
        cy.createSkill(1, 1, 1);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Self Report"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()

        cy.validateTable('[data-cy="skillsTable"]', [
            [ { colIndex: 5,  value: "100 pts x 2" }, { colIndex: 6,  value: "Disabled" }],
        ])

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Batch Edit"]', true)

        cy.get('[data-cy="batchUpdateMsg"]').contains('Update 1 Skill at once. Provide at least one attribute below to apply changes.')


        cy.get('[data-cy="pointIncrement"]').type('111')

        cy.get('[data-cy="selfReportingType_clearBtn"]').should('not.exist')
        cy.get('[data-cy="selfReportingType"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Approval Queue"]').click()

        cy.get('[data-cy="selfReportingType_clearBtn"]')
        cy.get('[data-cy="selfReportingType"]').contains('Approval Queue')

        cy.get('[data-cy="selfReportingType_clearBtn"]').click()
        cy.get('[data-cy="selfReportingType"]').should('not.contain', 'Approval Queue')

        cy.intercept('POST', '/admin/projects/proj1/batchUpdateSkills').as('batchUpdateSkills')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.wait('@batchUpdateSkills')

        cy.validateTable('[data-cy="skillsTable"]', [
            [ { colIndex: 5,  value: "111 pts x 2" }, { colIndex: 6,  value: "Disabled" }],
        ])
    });

    it('warn if switching self report skills to honor will cause pending approval request to be applied', () => {
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Approval',
            name: 'Approval 1'
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Approval',
            name: 'Approval 2'
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Approval',
            name: 'Approval 3'
        });
        cy.createSkill(1, 1, 4, {
            selfReportingType: 'HonorSystem',
            name: 'Honor System 1'
        });
        cy.createSkill(1, 1, 5, { name: 'Disabled 1' });
        cy.reportSkill(1, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(1, 1, 'user5Good@skills.org', '2020-09-13 11:00');
        cy.reportSkill(1, 1, 'user4Good@skills.org', '2020-09-14 11:00');
        cy.reportSkill(1, 1, 'user3Good@skills.org', '2020-09-15 11:00');
        // cy.rejectRequest(3);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Batch Edit"]', true)

        cy.get('[data-cy="selfReportingType_clearBtn"]').should('not.exist')
        cy.get('[data-cy="selfReportingType"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Honor System"]').click()

        // cy.intercept('POST', '/admin/projects/proj1/batchUpdateSkills').as('batchUpdateSkills')
        // cy.get('[data-cy="saveDialogBtn"]').click()
        // cy.wait('@batchUpdateSkills')
        //
        // cy.validateTable('[data-cy="skillsTable"]', [
        //     [ { colIndex: 5,  value: "111 pts x 2" }, { colIndex: 6,  value: "Disabled" }],
        // ])
    });

});