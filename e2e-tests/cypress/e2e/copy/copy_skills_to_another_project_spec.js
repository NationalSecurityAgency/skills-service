/*
 * Copyright 2024 SkillTree
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
import './copy_commands'

describe('Copy skills from one project to another Tests', () => {
    const tableSelector = '[data-cy="skillsTable"]'

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);

        cy.createProject(2)
        cy.createSubject(2, 1)
    });

    it('copy skills from a a subject to a subject in another project', () => {
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsCopyModal([1,2,4])

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.exist')
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 1. select project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 2. select subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1').click();

        cy.get('[data-cy="validationPassedMsg"]').contains('Validation Passed! 3 skill(s) are eligible to be copied to This is project 2 project')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="closeDialogBtn"]').contains('Close')
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="newSkillButton"]').should('have.focus')
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]').should('have.text', '0')

        // project 2
        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill3"]')
        cy.get('[data-cy="manageSkillLink_skill4"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');
    });

    it('copy skills from a group to a subject in another project', () => {
        cy.createSkillsGroup(1, 1, 10)
        cy.addSkillToGroup(1, 1, 10, 3)
        cy.addSkillToGroup(1, 1, 10, 4)
        cy.addSkillToGroup(1, 1, 10, 5)
        cy.createSkillsGroup(1, 1, 11)
        cy.addSkillToGroup(1, 1, 11, 6)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`${tableSelector} [data-p-index="1"] [data-pc-section="rowtogglebutton"]`).click()
        const expandedGroupSelector = '[data-cy="ChildRowSkillGroupDisplay_group10"]'
        cy.initiateSkillsCopyModal([0,2], expandedGroupSelector)

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.exist')
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 1. select project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 2. select subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1').click();

        cy.get('[data-cy="validationPassedMsg"]').contains('Validation Passed! 2 skill(s) are eligible to be copied to This is project 2 project')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="closeDialogBtn"]').contains('Close')
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get(`${expandedGroupSelector} [data-cy="addSkillToGroupBtn-group10"]`).should('have.focus')
        cy.get(`${expandedGroupSelector} [data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]`).should('have.text', '0')

        // project 2
        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.get('[data-cy="manageSkillLink_skill3"]')
        cy.get('[data-cy="manageSkillLink_skill5"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
    });

    it('copy skills from a subject to a group in another project', () => {
        cy.createSkillsGroup(2, 1, 10)
        cy.createSkillsGroup(2, 1, 11)
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsCopyModal([1,2,4])

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.exist')
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 1. select project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 2. select subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1')
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Awesome Group 10 Subj1')
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Awesome Group 11 Subj1').click();

        cy.get('[data-cy="validationPassedMsg"]').contains('Validation Passed! 3 skill(s) are eligible to be copied to This is project 2 project')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="closeDialogBtn"]').contains('Close')
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="copySuccessMsg"]').contains('Selected skill(s) were copied to This is project 2')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="newSkillButton"]').should('have.focus')
        cy.get('[data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]').should('have.text', '0')

        // project 2
        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.get(`${tableSelector} [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        const expandedGroupSelector = '[data-cy="ChildRowSkillGroupDisplay_group11"]'
        cy.get(`${expandedGroupSelector} [data-cy="manageSkillLink_skill1"]`)
        cy.get(`${expandedGroupSelector} [data-cy="manageSkillLink_skill3"]`)
        cy.get(`${expandedGroupSelector} [data-cy="manageSkillLink_skill4"]`)
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '3');
    });

    it('copy skills from a group to a group in another project', () => {
        cy.createSkillsGroup(1, 1, 10)
        cy.addSkillToGroup(1, 1, 10, 3)
        cy.addSkillToGroup(1, 1, 10, 4)
        cy.addSkillToGroup(1, 1, 10, 5)
        cy.createSkillsGroup(1, 1, 11)
        cy.addSkillToGroup(1, 1, 11, 6)

        cy.createSkillsGroup(2, 1, 10)
        cy.createSkillsGroup(2, 1, 11)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`${tableSelector} [data-p-index="1"] [data-pc-section="rowtogglebutton"]`).click()
        const expandedGroupSelector = '[data-cy="ChildRowSkillGroupDisplay_group10"]'
        cy.initiateSkillsCopyModal([0,2], expandedGroupSelector)

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.exist')
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 1. select project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 2. select subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1')
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Awesome Group 10 Subj1')
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Awesome Group 11 Subj1').click();

        cy.get('[data-cy="validationPassedMsg"]').contains('Validation Passed! 2 skill(s) are eligible to be copied to This is project 2 project')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="closeDialogBtn"]').contains('Close')
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="copySuccessMsg"]').contains('Selected skill(s) were copied to This is project 2')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get(`${expandedGroupSelector} [data-cy="addSkillToGroupBtn-group10"]`).should('have.focus')
        cy.get(`${expandedGroupSelector} [data-cy="skillActionsBtn"] [data-cy="skillActionsNumSelected"]`).should('have.text', '0')

        // project 2
        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.get(`${tableSelector} [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        const expandedGroupSelectorP2 = '[data-cy="ChildRowSkillGroupDisplay_group11"]'
        cy.get(`${expandedGroupSelectorP2} [data-cy="manageSkillLink_skill3"]`)
        cy.get(`${expandedGroupSelectorP2} [data-cy="manageSkillLink_skill5"]`)
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
    });

    it('validation called - skill ids and names already exist', () => {
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 4);
        cy.createSkill(2, 1, 5);

        cy.createProject(3)
        cy.createSubject(3, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsCopyModal([1,2,4])

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.exist')
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 1. select project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        // 2. select subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1').click();

        cy.get('[data-cy="validationFailedMsg"]').contains('The following IDs already exist in the destination project: skill3, skill4.')
        cy.get('[data-cy="validationFailedMsg"]').contains('The following names already exist in the destination project: Very Great Skill 3, Very Great Skill 4.')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
    });

    it('validation errors are cleared when the project is changed', () => {
        cy.createSkill(2, 1, 1);
        cy.createProject(3)
        cy.createSubject(3, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsCopyModal([0,1])
        // 1. select project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');
        // 2. select subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1').click();
        cy.get('[data-cy="validationFailedMsg"]').should('exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('contain.text', 'Subject 1')

        // select same project; error and subject selector should be cleared
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();

        cy.get('[data-cy="validationFailedMsg"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.contain.text', 'Subject 1')

        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1').click();
        cy.get('[data-cy="validationFailedMsg"]').should('exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('contain.text', 'Subject 1')

        // now select another project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 3').click();

        cy.get('[data-cy="validationFailedMsg"]').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.contain.text', 'Subject 1')

        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1').click();
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('contain.text', 'Subject 1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.get('[data-cy="validationFailedMsg"]').should('not.exist')
    });

    it('validation errors are cleared and re-checked when the subject is changed', () => {
        cy.createSkill(2, 1, 1);
        cy.createSubject(2, 2)
        cy.createProject(3)
        cy.createSubject(3, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsCopyModal([0,1])
        // 1. select project
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');
        // 2. select subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 1').click();
        cy.get('[data-cy="validationFailedMsg"]').contains('The following IDs already exist in the destination project: skill1.')
        cy.get('[data-cy="validationFailedMsg"]').contains('The following names already exist in the destination project: Very Great Skill 1.')
        // validate that error is not duplicated
        cy.get('[data-cy="validationFailedMsg"]').invoke('text').then((text) => {
            const occurrences = text.split('Very Great Skill 1').length - 1;
            cy.wrap(occurrences).should('eq', 1);
        });
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('contain.text', 'Subject 1')

        // select subject a different subject
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').click();
        cy.get('[data-cy="subjOrGroupSelector-name"]').contains('Subject 2').click();
        cy.get('[data-cy="validationFailedMsg"]').contains('The following IDs already exist in the destination project: skill1.')
        cy.get('[data-cy="validationFailedMsg"]').contains('The following names already exist in the destination project: Very Great Skill 1.').should('have.length', 1)
        // validate that error is not duplicated
        cy.get('[data-cy="validationFailedMsg"]').invoke('text').then((text) => {
            const occurrences = text.split('Very Great Skill 1').length - 1;
            cy.wrap(occurrences).should('eq', 1);
        });
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('contain.text', 'Subject 2')
    });

    it('cancelling dialog should return focus to the actions button', () => {
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.initiateSkillsCopyModal([1,2,4])

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.exist')
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get(`[data-cy="skillActionsBtn"]`).should('have.focus')

        // use x button to close dialog
        cy.get(`[data-cy="skillActionsBtn"]`).click()
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Copy to another Project"]').click()
        cy.get('[data-pc-name="dialog"] [data-pc-section="title"]').should('have.text', 'Copy Selected Skills To Another Project')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').should('be.enabled')
        cy.get('[data-pc-name="dialog"]  [aria-label="Close"]').click()
        cy.get(`[data-cy="skillActionsBtn"]`).should('have.focus')
    });

    it('cancelling dialog initiated from a group should return focus to the group actions button', () => {
        cy.createSkillsGroup(1, 1, 10)
        cy.addSkillToGroup(1, 1, 10, 3)
        cy.addSkillToGroup(1, 1, 10, 4)
        cy.addSkillToGroup(1, 1, 10, 5)
        cy.createSkillsGroup(1, 1, 11)
        cy.addSkillToGroup(1, 1, 11, 6)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`${tableSelector} [data-p-index="1"] [data-pc-section="rowtogglebutton"]`).click()
        const expandedGroupSelector = '[data-cy="ChildRowSkillGroupDisplay_group10"]'
        cy.initiateSkillsCopyModal([0,2], expandedGroupSelector)

        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectASubjectOrGroupDropdown"]').should('not.exist')
        cy.get('[data-cy="validationPassedMsg"]').should('not.exist');

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get(`${expandedGroupSelector} [data-cy="skillActionsBtn"]`).should('have.focus')

        // use x button to close dialog
        cy.get(`${expandedGroupSelector} [data-cy="skillActionsBtn"]`).click()
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Copy to another Project"]').click()
        cy.get('[data-pc-name="dialog"] [data-pc-section="title"]').should('have.text', 'Copy Selected Skills To Another Project')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').should('be.enabled')
        cy.get('[data-pc-name="dialog"]  [aria-label="Close"]').click()
        cy.get(`${expandedGroupSelector} [data-cy="skillActionsBtn"]`).should('have.focus')

    });
});