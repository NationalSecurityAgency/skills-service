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
describe('Tag Skills Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
    });

    it('tag skills with new tag', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
    });

    it('tag skills with existing tag', () => {

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="existingTagDropdown"]').should('not.exist')
        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('New Tag 1').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
    });

    it('remove a tag from a skill', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('New Tag 1').click()
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('not.exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
    });

    it('remove a tag from a skills where not all selected skills have the tag being removed', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('New Tag 1').click()
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
    });

    it('selected skills are unchecked after adding and removing tags', () => {

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('New Tag 1').click()
        cy.get('[data-cy="saveDialogBtn"]').click()


        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
    });

    it('tag skills with multiple tags', () => {

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click();
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-cy="newTag"]').type('New Tag 2')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag2"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag2"]').should('exist')
        cy.get('[data-cy="skillTag-skill3-newtag2"]').should('exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag2"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag2"]').should('exist')
        cy.get('[data-cy="skillTag-skill3-newtag2"]').should('exist')
    });

    it('tag values cannot exceed maxSkillTagLength', () => {

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        const invalidName = Array(51).fill('a').join('');
        cy.get('[data-cy="newTag"]').type(invalidName)
        cy.get('[data-cy=newTagError]').contains('Tag Name must be at most 50 characters').should('be.visible');
        cy.get('[data-cy=saveDialogBtn]').should('be.disabled');

        cy.get('[data-cy=newTag]').type('{backspace}');
        cy.get('[data-cy="newTagError"]').should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
    });

    it('attempt to remove a tag from a skill with no tags', () => {

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-pc-name="dialog"]').contains('The selected skills do not have any tags.')
    });

    it('skills tags are displayed on admin skill page', () => {
        cy.addTagToSkills();
        cy.addTagToSkills(1, ['skill1'], 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');

        cy.get('[data-cy="skillTag-skill1-tag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-tag2"]').should('exist')
    });

    it('after tagging skill selection is removed and focus is returned to the new skill button', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        for (let i= 0; i < 3 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }
        cy.get('[data-cy="newSkillButton"]').should('have.focus')
    });

    it('cancel tagging should return focus to the Action button', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-name="dialog"] [aria-label="Close"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
    });

    it('after removing a tag selection is removed and focus is returned to the new skill button', () => {
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('TAG 2').click()
        cy.get('[data-cy="saveDialogBtn"]').click()

        for (let i= 0; i < 3 ; i++) {
            cy.get(`[data-cy="skillsTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }
        cy.get('[data-cy="newSkillButton"]').should('have.focus')
    });

    it('after canceling remove tag dialog return focus to the Action button', () => {
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 2);
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')

        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()

        cy.get('[data-pc-name="dialog"] [aria-label="Close"]').click()
        cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        cy.get(`[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
    });


    it('adding a duplicate tag skill is ignored and not displayed', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('have.length', 1)
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('have.length', 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('have.length', 1)
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('have.length', 1)

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('have.length', 1)
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('have.length', 1)
    });
});
