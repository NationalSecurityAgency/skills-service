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
describe('Single Skill Tag Management Tests', () => {

    const skillsTable = '[data-cy="skillTagSkillsTable"]'

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
    });

    it('edit existing tag', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)
        cy.addTagToSkills(1, ['skill1'], 3)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.get('[data-cy="title"]').contains('TAG: TAG 1')
        cy.get('[data-cy="editTag"]').click()
        cy.get('[data-cy="newTag"]').should('have.value', 'TAG 1')
        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('a')
        cy.get('[data-cy="saveDialogBtn"]').click();

        cy.get('[data-cy="title"]').contains('TAG: TAG 1a')

        cy.get('[data-cy="editTag"]').click()
        cy.get('[data-cy="newTag"]').should('have.value', 'TAG 1a')
        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('b')
        cy.get('[data-cy="saveDialogBtn"]').click();

        cy.get('[data-cy="title"]').contains('TAG: TAG 1ab')

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.get('[data-cy="title"]').contains('TAG: TAG 1ab')
    });

    it('tag edit in the dialog must validate against saving the same tag name', () => {
        cy.addTagToSkills(1, ['skill1'], 1)
        cy.addTagToSkills(1, ['skill1'], 2)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.get('[data-cy="editTag"]').click()
        cy.get('[data-cy="newTag"]').should('have.value', 'TAG 1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="newTagError"]').should('not.be.visible')

        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('a')
        cy.get('[data-cy="newTagError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('{backspace}')
        cy.get('[data-cy="newTagError"]').contains('Tag Name needs to be different')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        // id will remove special chars but it for the edit is not validated
        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('$')
        cy.get('[data-cy="newTagError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        // still validates against other tags
        cy.get('[data-cy="newTag"]').click()
        cy.get('[data-cy="newTag"]').type('{backspace}{backspace}2')
        cy.get('[data-cy="newTagError"]').contains('Tag Name already exist')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('add skills to tag', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 4);
        cy.createSkillsGroup(1, 1, 20)
        cy.addSkillToGroup(1, 1, 20, 21)
        cy.addTagToSkills(1, [], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.get('[data-cy="noContent"]').contains('No Skills Added Yet...')
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.get('[data-cy="skillsSelector"] button').should('have.focus');

        cy.get('[data-cy="pageHeaderStat_Tagged Skills"] [data-cy="statValue"]').should('have.text', '1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 1'}, {colIndex: 1, value: 'Subject 1'}],
        ], 25);
        cy.get('[data-cy="noContent"]').should('not.exist')

        cy.selectSkill('[data-cy="skillsSelector"]', 'skill4Subj2');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 4 Subj2'}, {colIndex: 1, value: 'Subject 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}, {colIndex: 1, value: 'Subject 1'}],
        ], 25);
        cy.get('[data-cy="pageHeaderStat_Tagged Skills"] [data-cy="statValue"]').should('have.text', '2');

        cy.selectSkill('[data-cy="skillsSelector"]', 'skill21');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 21'}, {colIndex: 1, value: 'Subject 1'}, {colIndex: 2, value: 'Group 20 Subj1'}],
            [{colIndex: 0, value: 'Very Great Skill 4 Subj2'}, {colIndex: 1, value: 'Subject 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}, {colIndex: 1, value: 'Subject 1'}],
        ], 25);
        cy.get('[data-cy="pageHeaderStat_Tagged Skills"] [data-cy="statValue"]').should('have.text', '3');

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');

        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 21'}, {colIndex: 1, value: 'Subject 1'}, {colIndex: 2, value: 'Group 20 Subj1'}],
            [{colIndex: 0, value: 'Very Great Skill 4 Subj2'}, {colIndex: 1, value: 'Subject 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}, {colIndex: 1, value: 'Subject 1'}],
        ], 25);
        cy.get('[data-cy="pageHeaderStat_Tagged Skills"] [data-cy="statValue"]').should('have.text', '3');
    });

    it('remove skills', () => {
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 3'}],
            [{colIndex: 0, value: 'Very Great Skill 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);

        cy.get('[data-cy="deleteSkill_skill2"]').click()
        cy.get('[data-p="modal"]').contains('Are you sure you want to remove Skill "Very Great Skill 2" from Tag "TAG 1"?')
        cy.get('[data-pc-name="pcacceptbutton"]').click()

        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 3'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);
        cy.get('[data-cy="skillsSelector"] button').should('have.focus');

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 3'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);

        cy.get('[data-cy="deleteSkill_skill1"]').click()
        cy.get('[data-p="modal"]').contains('Are you sure you want to remove Skill "Very Great Skill 1" from Tag "TAG 1"?')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 3'}],
        ], 25);

        cy.get('[data-cy="deleteSkill_skill3"]').click()
        cy.get('[data-p="modal"]').contains('Are you sure you want to remove Skill "Very Great Skill 3" from Tag "TAG 1"?')
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get(skillsTable).should('not.exist')
        cy.get('[data-cy="noContent"]').contains('No Skills Added Yet...')
    });

    it('cancelling remove returns focus to the return button', () => {
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 3'}],
            [{colIndex: 0, value: 'Very Great Skill 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);

        cy.get('[data-cy="deleteSkill_skill2"]').click()
        cy.get('[data-pc-name="pcrejectbutton"]').click()
        cy.get('[data-cy="deleteSkill_skill2"]').should('have.focus')
    })

    it('adding skill returns focus to the skill selector', () => {
        cy.addTagToSkills(1, [], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.get('[data-cy="noContent"]').contains('No Skills Added Yet...')
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.get('[data-cy="skillsSelector"] button').should('have.focus');
    });

    it('skill links to skill page', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 4);
        cy.createSkillsGroup(1, 1, 20)
        cy.addSkillToGroup(1, 1, 20, 21)
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill4Subj2', 'skill21'], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 21'}],
            [{colIndex: 0, value: 'Skill 4'}],
            [{colIndex: 0, value: 'Skill 2'}],
            [{colIndex: 0, value: 'Skill 1'}],
        ], 25);

        cy.get('[data-cy="manage_skill21"]').click()
        cy.get('[data-cy="subTitle"] [data-cy="skillId"]').contains('ID: skill21')
        cy.get('[data-cy="breadcrumbItemValue"]').contains('subj1')
        cy.get('[data-cy="breadcrumbItemValue"]').contains('group20')
        cy.get('[data-cy="breadcrumbItemValue"]').contains('skill21')
        cy.get('[data-cy="subTitle"]').contains('Group: Awesome Group 20 Subj1')
        cy.get('[data-cy="skillOverviewTotalpoints"]')

        cy.go('back')
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 21'}],
            [{colIndex: 0, value: 'Skill 4'}],
            [{colIndex: 0, value: 'Skill 2'}],
            [{colIndex: 0, value: 'Skill 1'}],
        ], 25);

        cy.get('[data-cy="manage_skill4Subj2"]').click()
        cy.get('[data-cy="subTitle"] [data-cy="skillId"]').contains('ID: skill4')
        cy.get('[data-cy="breadcrumbItemValue"]').contains('subj2')
        cy.get('[data-cy="breadcrumbItemValue"]').contains('skill4')
        cy.get('[data-cy="skillOverviewTotalpoints"]')

        cy.go('back')
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 21'}],
            [{colIndex: 0, value: 'Skill 4'}],
            [{colIndex: 0, value: 'Skill 2'}],
            [{colIndex: 0, value: 'Skill 1'}],
        ], 25);

        cy.get('[data-cy="manage_skill1"]').click()
        cy.get('[data-cy="subTitle"] [data-cy="skillId"]').contains('ID: skill1')
        cy.get('[data-cy="breadcrumbItemValue"]').contains('subj1')
        cy.get('[data-cy="breadcrumbItemValue"]').contains('skill1')
        cy.get('[data-cy="skillOverviewTotalpoints"]')
    })

    it('subject links to subject page', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 4);
        cy.createSkillsGroup(1, 1, 20)
        cy.addSkillToGroup(1, 1, 20, 21)
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill4Subj2', 'skill21'], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 21'}],
            [{colIndex: 0, value: 'Skill 4'}],
            [{colIndex: 0, value: 'Skill 2'}],
            [{colIndex: 0, value: 'Skill 1'}],
        ], 25);

        cy.get('[data-p-index="0"] [data-cy="manage_subj1"]').click()
        cy.get('[data-cy="subTitle"]').contains('ID: subj1')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill1"]')

        cy.go('back')
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 21'}],
            [{colIndex: 0, value: 'Skill 4'}],
            [{colIndex: 0, value: 'Skill 2'}],
            [{colIndex: 0, value: 'Skill 1'}],
        ], 25);

        cy.get('[data-p-index="1"] [data-cy="manage_subj2"]').click()
        cy.get('[data-cy="subTitle"]').contains('ID: subj2')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill4Subj2"]')
    })

    it('group links to group page', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 4);
        cy.createSkillsGroup(1, 1, 20)
        cy.addSkillToGroup(1, 1, 20, 21)
        cy.createSkillsGroup(1, 2, 30)
        cy.addSkillToGroup(1, 2, 30, 31)

        cy.addTagToSkills(1, ['skill21', 'skill31Subj2'], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 31'}],
            [{colIndex: 0, value: 'Skill 21'}],
        ], 25);

        cy.get('[data-p-index="0"] [data-cy="manage_group30Subj2"]').click()
        cy.get('[data-cy="subTitle"]').contains('ID: group30Subj2')
        cy.get('[data-cy="breadcrumbItemValue"]').contains('subj2')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill31Subj2"]')

        cy.go('back')
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 31'}],
            [{colIndex: 0, value: 'Skill 21'}],
        ], 25);

        cy.get('[data-p-index="1"] [data-cy="manage_group20"]').click()
        cy.get('[data-cy="subTitle"]').contains('ID: group20')
        cy.get('[data-cy="breadcrumbItemValue"]').contains('subj1')
        cy.get('[data-cy="skillsTable"] [data-cy="manageSkillLink_skill21"]')
    })

    it('paging and sorting - skill name', () => {
        cy.createSubject(1, 2);
        const expected = []
        const skillIds = []
        for (let i = 0; i < 27; i++) {
            cy.createSkill(1, 2, i);
            skillIds.push(`skill${i}Subj2`)
            expected.push([{colIndex: 0, value: `Very Great Skill ${i} Subj2`}],)
        }
        const expectedReversed = expected.toReversed();
        cy.addTagToSkills(1, skillIds, 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.get(`${skillsTable} [data-pc-section="columntitle"]`).contains('Skill').click()
        cy.validateTable(skillsTable, expected, 25);

        cy.get(`${skillsTable} [data-pc-section="columntitle"]`).contains('Skill').click()
        cy.validateTable(skillsTable, expectedReversed, 25);

        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('10').click();
        cy.get(`${skillsTable} [data-pc-section="columntitle"]`).contains('Skill').click()
        cy.validateTable(skillsTable, expected, 10);
    })

    it('sorting preference is preserved after page refresh', () => {
        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 3'}],
            [{colIndex: 0, value: 'Very Great Skill 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);

        cy.get(`${skillsTable} [data-pc-section="columntitle"]`).contains('Skill').click()
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 1'}],
            [{colIndex: 0, value: 'Very Great Skill 2'}],
            [{colIndex: 0, value: 'Very Great Skill 3'}],
        ], 25);

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 1'}],
            [{colIndex: 0, value: 'Very Great Skill 2'}],
            [{colIndex: 0, value: 'Very Great Skill 3'}],
        ], 25);

        cy.get(`${skillsTable} [data-pc-section="columntitle"]`).contains('Skill').click()
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 3'}],
            [{colIndex: 0, value: 'Very Great Skill 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Very Great Skill 3'}],
            [{colIndex: 0, value: 'Very Great Skill 2'}],
            [{colIndex: 0, value: 'Very Great Skill 1'}],
        ], 25);
    })

    it('sort by subject', () => {
        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 4);
        cy.createSubject(1, 3);
        cy.createSkill(1, 3, 5);
        cy.addTagToSkills(1, ['skill1', 'skill4Subj2', 'skill5Subj3'], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 1, value: 'Subject 3'}],
            [{colIndex: 1, value: 'Subject 2'}],
            [{colIndex: 1, value: 'Subject 1'}],
        ], 25);

        cy.get(`${skillsTable} [data-pc-section="columntitle"]`).contains('Subject').click()
        cy.validateTable(skillsTable, [
            [{colIndex: 1, value: 'Subject 1'}],
            [{colIndex: 1, value: 'Subject 2'}],
            [{colIndex: 1, value: 'Subject 3'}],
        ], 25);

        cy.get(`${skillsTable} [data-pc-section="columntitle"]`).contains('Subject').click()
        cy.validateTable(skillsTable, [
            [{colIndex: 1, value: 'Subject 3'}],
            [{colIndex: 1, value: 'Subject 2'}],
            [{colIndex: 1, value: 'Subject 1'}],
        ], 25);
    })

    it('sort by group', () => {
        cy.createSkillsGroup(1, 1, 4);
        cy.addSkillToGroup(1, 1, 4, 11)
        cy.createSkillsGroup(1, 1, 5);
        cy.addSkillToGroup(1, 1, 5, 12)
        cy.createSkillsGroup(1, 1, 6);
        cy.addSkillToGroup(1, 1, 6, 13)
        cy.addTagToSkills(1, ['skill2', 'skill11', 'skill12', 'skill13'], 1)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1');
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 13'}, {colIndex: 2, value: 'Group 6'}],
            [{colIndex: 0, value: 'Skill 12'}, {colIndex: 2, value: 'Group 5'}],
            [{colIndex: 0, value: 'Skill 11'}, {colIndex: 2, value: 'Group 4'}],
            [{colIndex: 0, value: 'Skill 2'}],
        ], 25);

        cy.get(`${skillsTable} [data-pc-section="columntitle"]`).contains('Group').click()
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 11'}, {colIndex: 2, value: 'Group 4'}],
            [{colIndex: 0, value: 'Skill 12'}, {colIndex: 2, value: 'Group 5'}],
            [{colIndex: 0, value: 'Skill 13'}, {colIndex: 2, value: 'Group 6'}],
            [{colIndex: 0, value: 'Skill 2'}],
        ], 25);

        cy.get(`${skillsTable} [data-pc-section="columntitle"]`).contains('Group').click()
        cy.validateTable(skillsTable, [
            [{colIndex: 0, value: 'Skill 13'}, {colIndex: 2, value: 'Group 6'}],
            [{colIndex: 0, value: 'Skill 12'}, {colIndex: 2, value: 'Group 5'}],
            [{colIndex: 0, value: 'Skill 11'}, {colIndex: 2, value: 'Group 4'}],
            [{colIndex: 0, value: 'Skill 2'}],
        ], 25);
    })
})
