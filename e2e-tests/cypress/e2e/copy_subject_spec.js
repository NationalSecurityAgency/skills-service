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
describe('Copy Subject from one project to another Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
    });

    it('copy subject to another subject', () => {
        cy.createProject(2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="closeDialogBtn"]').contains('Cancel')

        cy.get('[data-cy="validationPassedMsg"]').should('be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="closeDialogBtn"]').contains('Close')
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="btn_copy-subject"]').should('have.focus')

        // project 2
        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
    });

    it('cancelling dialog should return focus to copy button', () => {
        cy.createProject(2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="btn_copy-subject"]').should('have.focus')

        // use x button
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').should('be.enabled')
        cy.get('[data-pc-name="dialog"]  [aria-label="Close"]').click()
        cy.get('[data-cy="btn_copy-subject"]').should('have.focus')
    })

    it('subject id already exist', () => {
        cy.createProject(2);
        cy.createSubject(2, 1, {name: 'Other Name'});
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="validationFailedMsg"]').contains('Subject ID already exists in the selected project')
    });

    it('subject name already exist', () => {
        cy.createProject(2);
        cy.createSubject(2, 1, {subjectId: 'otherId'});
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="validationFailedMsg"]').contains('Subject Name already exists in the selected project')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('skill ids and names already exist', () => {
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2, { name: 'Something Else' });
        cy.createSubject(2, 1, {subjectId: 'otherId', name: 'Other Name'});

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '4');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="validationFailedMsg"]').contains('The following skill IDs already exist in the selected project: skill1, skill2.')
        cy.get('[data-cy="validationFailedMsg"]').contains('The following skill names already exist in the selected project: Very Great Skill 1.')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('other project has imported and still disabled skills with the same id', () => {
        cy.createProject(3);
        cy.createSubject(3, 1);
        cy.createSkill(3, 1, 1);
        cy.exportSkillToCatalog(3, 1, 1)

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 3, 1)
        cy.createSubject(2, 1, {subjectId: 'otherId', name: 'Other Name'});

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="validationFailedMsg"]').contains('The following skill IDs already exist in the selected project: skill1.')
        cy.get('[data-cy="validationFailedMsg"]').contains('The following skill names already exist in the selected project: Very Great Skill 1.')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('only show first 10 name ids in the error', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        for (let i = 1; i < 15; i++) {
            cy.createSkill(1, 1, i);
            cy.createSkill(2, 1, i, { name: `Something Else ${i}` });
        }
        cy.createSubject(2, 1, { name: 'Other Name', subjectId: 'otherId'});
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill14"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '14');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="validationFailedMsg"]').contains('The following skill IDs already exist in the selected project: skill1, skill10, skill11, skill12, skill13, skill14, skill2, skill3, skill4, skill5.')
        cy.get('[data-cy="validationFailedMsg"]').contains('The following skill names already exist in the selected project:').should('not.exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('only show first 10 skill names in the error', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        for (let i = 1; i < 15; i++) {
            cy.createSkill(1, 1, i);
            cy.createSkill(2, 1, i, { skillId: `otherId${i}` });
        }
        cy.createSubject(2, 1, { name: 'Other Name', subjectId: 'otherId'});
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill14"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '14');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="validationFailedMsg"]').contains('The following skill IDs already exist in the selected project:').should('not.exist')
        cy.get('[data-cy="validationFailedMsg"]').contains('The following skill names already exist in the selected project: Very Great Skill 1, Very Great Skill 10, Very Great Skill 11, Very Great Skill 12, Very Great Skill 13, Very Great Skill 14, Very Great Skill 2, Very Great Skill 3, Very Great Skill 4, Very Great Skill 5.')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('no projects available to copy to', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="noOtherProjectsMsg"]')
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
        cy.get('[data-cy="closeDialogBtn"]').contains('Close')
    })

    it('clear validation errors on changing project', () => {
        cy.createProject(2);
        cy.createSubject(2, 1, {name: 'Other Name'});

        cy.createProject(3);
        cy.createProject(4);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2').click();
        cy.get('[data-cy="validationFailedMsg"]').contains('Subject ID already exists in the selected project')

        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 3').click();
        cy.get('[data-cy="validationPassedMsg"]').should('be.visible');
        cy.get('[data-cy="validationFailedMsg"]').should('not.exist')
    });

    it('project itself should be filtered from the list', () => {
        cy.createProject(2);
        cy.createProject(3);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="manageSkillLink_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill2"]')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2');
        cy.get('[data-cy="btn_copy-subject"]').click()
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="selectAProjectDropdown"]').click();
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 2')
        cy.get('[data-cy="projectSelector-projectName"]').contains('This is project 3')
        cy.get('[data-cy="projectSelector-projectName"]').should('not.contain', 'This is project 1')
    });
});