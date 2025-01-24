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
describe('Move Skills Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
    });

    it('move skills from subject into a subject', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSubject(1, 3);
        cy.createSubject(1, 4);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-p-index="2"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Move Skills"]').click()

        // step 1
        cy.get('[data-cy="reuseSkillsModalStep1"]');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj3"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        // step 2
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be moved to the [Subject 3] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();

        // step 3
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 2 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="manageSkillLink_skill1"]')
            .should('not.exist');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]')
            .should('not.exist');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');

        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj3"]')
            .click();
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]')
            .should('not.exist');
        cy.get('[data-cy="manageSkillLink_skill3"]');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '400');
    });

    it('move skills from group into another subject', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSubject(1, 3);
        cy.createSubject(1, 4);

        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);
        cy.addSkillToGroup(1, 1, 11, 7);
        cy.addSkillToGroup(1, 1, 11, 8);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`[data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        // must exist initially
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill8"]');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '6');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '1,200');

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="2"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Move Skills"]').click()

        // step 1
        cy.get('[data-cy="reuseSkillsModalStep1"]');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj3"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        // step 2
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be moved to the [Subject 3] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();

        // step 3
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 2 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="nameCell_group11"] [data-cy="numSkillsInGroup"]')
            .should('have.text', '1 skill');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]')
            .should('not.exist');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill8"]')
            .should('not.exist');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '4');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '800');

        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj3"]')
            .click();
        cy.get('[data-cy="manageSkillLink_skill6"]');
        cy.get('[data-cy="manageSkillLink_skill7"]')
            .should('not.exist');
        cy.get('[data-cy="manageSkillLink_skill8"]');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '400');
    });

    it('move 1 skill from group into a group under another subject', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSubject(1, 3);
        cy.createSubject(1, 4);

        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);
        cy.addSkillToGroup(1, 1, 11, 7);
        cy.addSkillToGroup(1, 1, 11, 8);

        cy.createSkillsGroup(1, 3, 12);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`[data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        // must exist initially
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill8"]');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '6');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '1,200');

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Move Skills"]').click()

        // step 1
        cy.get('[data-cy="reuseSkillsModalStep1"]');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj3group12Subj3"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        // step 2
        cy.get('[data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be moved to the [Awesome Group 12 Subj3] group');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();

        // step 3
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 1 skill');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="nameCell_group11"] [data-cy="numSkillsInGroup"]')
            .should('have.text', '2 skills');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill8"]')
            .should('not.exist');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '5');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '1,000');

        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj3"]')
            .click();
        cy.get('[data-cy="nameCell_group12Subj3"] [data-cy="numSkillsInGroup"]')
            .should('have.text', '1 skill');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj3"] [data-cy="manageSkillLink_skill6"]')
            .should('not.exist');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj3"] [data-cy="manageSkillLink_skill7"]')
            .should('not.exist');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group12Subj3"] [data-cy="manageSkillLink_skill8"]');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');
    });

    it('move skills from group into the same subject', () => {
        cy.createSkill(1, 1, 2);

        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);
        cy.addSkillToGroup(1, 1, 11, 7);
        cy.addSkillToGroup(1, 1, 11, 8);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
        // must exist initially
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill8"]');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '5');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '1,000');

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="1"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="2"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
          .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Move Skills"]').click()

        // step 1
        cy.get('[data-cy="reuseSkillsModalStep1"]');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be moved to the [Subject 1] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();

        // step 3
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 3 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill6"]');
        cy.get('[data-cy="manageSkillLink_skill7"]');
        cy.get('[data-cy="manageSkillLink_skill8"]');
        cy.get('[data-cy="nameCell_group11"] [data-cy="numSkillsInGroup"]')
            .should('have.text', '0 skills');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"]')
            .contains('Group has no Skills');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '5');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '1,000');
    });

    it('move skills from subject into a group under the same subject', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);

        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 8);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill6"]');
        cy.get('[data-cy="manageSkillLink_skill7"]');

        cy.get('[data-cy="nameCell_group11"] [data-cy="numSkillsInGroup"]')
            .should('have.text', '1 skill');
        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill8"]');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '5');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '1,000');

        cy.get('[data-p-index="1"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-p-index="2"] [data-pc-name="rowcheckbox"]').click()

        cy.get('[data-cy="skillActionsBtn"]').first().click()
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Move Skills"]').click()

        // step 1
        cy.get('[data-cy="reuseSkillsModalStep1"]');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1group11"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be moved to the [Awesome Group 11 Subj1] group.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();

        // step 3
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 2 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]')
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]')
        cy.get('[data-cy="nameCell_group11"] [data-cy="numSkillsInGroup"]')
            .should('have.text', '3 skills');

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill8"]');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '5');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '1,000');

        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).first().click()
        cy.get('[data-cy="manageSkillLink_skill6"]').should('not.exist')
        cy.get('[data-cy="manageSkillLink_skill7"]').should('not.exist')
    });

    it('move skills with deps from subject into a subject', () => {
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.addLearningPathItem(1, 3, 1)
        cy.createSubject(1, 3);
        cy.createSubject(1, 4);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // must exist initially
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-p-index="1"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-p-index="2"] [data-pc-name="rowcheckbox"]').click()

        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Move Skills"]').click()


        // step 1
        cy.get('[ data-cy="reuseSkillsModalStep1"]');
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj3"]')
            .click();
        cy.get('[ data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('2 skills will be moved to the [Subject 3] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();

        // step 3
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 2 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="manageSkillLink_skill1"]')
            .should('not.exist');
        cy.get('[data-cy="manageSkillLink_skill2"]')
            .should('not.exist');
        cy.get('[data-cy="manageSkillLink_skill3"]');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');

        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj3"]')
            .click();
        cy.get('[data-cy="manageSkillLink_skill1"]');
        cy.get('[data-cy="manageSkillLink_skill2"]');
        cy.get('[data-cy="manageSkillLink_skill3"]')
            .should('not.exist');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '400');
    });

    it('move ALL skills from a group with partial requirement then add a skill to a group', () => {
        cy.createSkillsGroup(1, 1, 11);
        cy.addSkillToGroup(1, 1, 11, 6);
        cy.addSkillToGroup(1, 1, 11, 7);
        cy.addSkillToGroup(1, 1, 11, 8);
        cy.createSkillsGroup(1, 1, 11, { numSkillsRequired: 2 });

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`[data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
        // must exist initially
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill6"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill7"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_skill8"]');

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="1"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-p-index="2"] [data-pc-name="rowcheckbox"]').click()

        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Move Skills"]').click()

        // step 1
        cy.get('[data-cy="reuseSkillsModalStep1"]');
        cy.get('[data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj1"]')
            .click();
        cy.get('[data-cy="reuseSkillsModalStep1"]')
            .should('not.be.visible');

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('3 skills will be moved to the [Subject 1] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
            .click();

        // step 3
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 3 skills');
        cy.get('[data-cy="reuseSkillsModalStep3"] [data-cy="okButton"]')
            .click();

        cy.get('[data-cy="nameCell_group11"] [data-cy="numSkillsInGroup"]')
            .should('have.text', '0 skills');
        cy.get('[data-cy="addSkillToGroupBtn-group11"]')
            .click();
        cy.contains('* Skill Name')
        cy.wait(1000)
        cy.get('[data-cy="skillName"]')
            .type('new skill', { delay: 100, waitForAnimations: true });
        cy.get('[data-cy="skillName"]').should('have.value', 'new skill');
        cy.get('[data-cy="saveDialogBtn"]')
            .click();

        // validate skill was created
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="manageSkillLink_newskillSkill"]');
        cy.get('[data-cy="ChildRowSkillGroupDisplay_group11"] [data-cy="requiredAllSkills"]');

    });

});
