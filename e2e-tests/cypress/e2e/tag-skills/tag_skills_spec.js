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
describe('Tag Skills on Subject Page Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
    });


    it('no tags have been creatd message', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-cy="instructionsMsg"]').contains('To tag 2 selected skills, please select an existing tag or create a new one:')
        cy.get('[data-cy="noTagsMessage"]').should('be.visible')
    })

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
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
    });

    it('tag skills with new tag - special characters are removed from tag id', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('  C%^o&*() O^&*L  ')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillTag-skill1-cool"]').should('have.attr', 'href', '/administrator/projects/proj1/skill-tags/cool')
        cy.get('[data-cy="skillTag-skill2-cool"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-cool"]').should('have.attr', 'href', '/administrator/projects/proj1/skill-tags/cool')
        cy.get('[data-cy="skillTag-skill1-cool"]').contains('C%^o&*() O^&*L')
        cy.get('[data-cy="skillTag-skill3-cool"]').contains('C%^o&*() O^&*L')

        cy.get('[data-cy="skillTag-skill1-cool"]').click()
        const skillsTable = '[data-cy="skillTagSkillsTable"]'
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 1'}],
            [{colIndex: 0, value: 'Very Great Skill 3'}],
        ], 25);
        cy.get('[data-cy="title"]').contains('TAG: C%^o&*() O^&*L')
    });

    it('validate in tag create dialog for existing tag names and ids', () => {
        cy.addTagToSkills(1, ['skill1'], 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValueError"]').should('not.be.visible')
        cy.get('[data-cy="tagValue"]').type('tAg ');
        cy.get('[data-cy="tagValueError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="tagValue"]').type('1');
        cy.get('[data-cy="tagValueError"]').contains('Tag already exists')
        cy.get('[data-cy="idError"]').contains('Tag ID already exists')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="tagValue"]').type('{backspace}');
        cy.get('[data-cy="tagValueError"]').should('not.be.visible')
        cy.get('[data-cy="idError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // validation hits on the id since special chars are removed
        cy.get('[data-cy="tagValue"]').type('$%^&*1');
        cy.get('[data-cy="idError"]').contains('Tag ID already exists')
        cy.get('[data-cy="tagValueError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    })

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
        cy.get('[data-cy="noTagsMessage"]').should('be.visible')
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="existingTagDropdown"]').should('not.exist')
        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'NewTag1')
        cy.get('[data-cy="enableIdInput"]').click()
        cy.get('[data-cy="idInputValue"]').should('be.enabled')
        cy.get('[data-p="modal"] [data-cy="idInputValue"]').type('a')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillTag-skill1-newtag1a"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1a"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('New Tag 1').click()
        cy.get('[data-cy="noTagsMessage"]').should('not.exist')

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="skillTag-skill1-newtag1a"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1a"]').should('exist')
        cy.get('[data-cy="skillTag-skill3-newtag1a"]').should('exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillTag-skill1-newtag1a"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1a"]').should('exist')
        cy.get('[data-cy="skillTag-skill3-newtag1a"]').should('exist')
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
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('New Tag 1').click()
        cy.clickSaveDialogBtn()
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
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('New Tag 1').click()
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.visit('/administrator/projects/proj1/subjects/subj1');
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
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')

        cy.get('[data-cy="skillsTable"] [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]').click();
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()
        cy.get('[data-cy="tagValue"]').type('New Tag 2')
        cy.clickSaveDialogBtn()

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
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

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
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

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
        cy.get('[data-pc-section="tablist"] [data-pc-name="tab"]').contains('Create New Tag').click()

        cy.get('[data-cy="tagValue"]').type('New Tag 1')
        cy.clickSaveDialogBtn()

        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('have.length', 1)
        cy.get('[data-cy="skillTag-skill2-newtag1"]').should('not.exist')
        cy.get('[data-cy="skillTag-skill3-newtag1"]').should('exist')
        cy.get('[data-cy="skillTag-skill1-newtag1"]').should('have.length', 1)
    });

    it('tag is linked to the subject page', () => {
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 1)
        cy.addTagToSkills(1, ['skill1', 'skill2'], 2)
        cy.addTagToSkills(1, ['skill1'], 3)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="nameCell_skill1"] [data-cy="skillTag-skill1-tag3"]').click()
        cy.get('[data-cy="title"]').contains('TAG: TAG 3')
        const skillsTable = '[data-cy="skillTagSkillsTable"]'
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);

        cy.go('back')
        cy.get('[data-cy="nameCell_skill1"] [data-cy="skillTag-skill1-tag2"]').click()
        cy.get('[data-cy="title"]').contains('TAG: TAG 2')
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);

    });

    it('tag is linked to the skill page', () => {
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 1)
        cy.addTagToSkills(1, ['skill1', 'skill2'], 2)
        cy.addTagToSkills(1, ['skill1'], 3)

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');

        cy.get('[data-cy="subTitle"] [data-cy="skillTag-skill1-tag3"]').click()
        cy.get('[data-cy="title"]').contains('TAG: TAG 3')
        const skillsTable = '[data-cy="skillTagSkillsTable"]'
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);

        cy.go('back')
        cy.get('[data-cy="subTitle"] [data-cy="skillTag-skill1-tag2"]').click()
        cy.get('[data-cy="title"]').contains('TAG: TAG 2')
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);

    });
})
