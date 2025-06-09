/*
 * Copyright 2025 SkillTree
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

describe('Training Keyboard Shortcuts Tests', () => {

    it('user shortcuts to navigate to next and previous skills', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)

        cy.intercept('/api/projects/proj1/skills/skill1/dependencies').as('skills1Deps')

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.wait('@skills1Deps')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Alt", "n"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3')


        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Alt", "n"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Alt", "n"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Alt", "p"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Alt", "p"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Alt", "p"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')
    })

    it('customize custom previous/next shortcuts', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)

        cy.visit('/settings/preferences')
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="clearShortcut"]').click()
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="shortcutInput"]').focus()
        cy.realPress("Control");
        cy.realPress("Shift");
        cy.realPress("L");

        cy.get('[data-cy="unsavedChangesAlert"]')
        cy.get('[data-cy="userPrefsSettingsSave"]').click()
        cy.get('[data-cy="settingsSavedAlert"]')

        cy.get('[data-cy="Next_SkillShortcut"] [data-cy="clearShortcut"]').click()
        cy.get('[data-cy="Next_SkillShortcut"] [data-cy="shortcutInput"]').focus()
        cy.realPress("Shift");
        cy.realPress("Control");
        cy.realPress("F9");

        cy.get('[data-cy="unsavedChangesAlert"]')
        cy.get('[data-cy="userPrefsSettingsSave"]').click()
        cy.get('[data-cy="settingsSavedAlert"]')

        cy.intercept('/api/projects/proj1/skills/skill1/dependencies').as('skills1Deps')

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.wait('@skills1Deps')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Shift", "Control", "F9"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Shift", "Control", "F9"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Shift", "Control", "F9"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Shift", "l"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Shift", "l"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Shift", "l"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')
    })

    it('customize custom previous shortcuts and navigate p&r without refresh', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.enableProdMode(1);
        cy.addToMyProjects(1);

        cy.visit('/settings/preferences')
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="clearShortcut"]').click()
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="shortcutInput"]').focus()
        cy.realPress("Control");
        cy.realPress("Shift");
        cy.realPress("L");

        cy.get('[data-cy="unsavedChangesAlert"]')
        cy.get('[data-cy="userPrefsSettingsSave"]').click()
        cy.get('[data-cy="settingsSavedAlert"]')

        cy.intercept('/api/projects/proj1/skills/skill3/dependencies').as('skills1Deps')

        cy.get('[data-cy="skillTreeLogo"]').click()
        cy.get('[data-cy="project-link-proj1"]').click()
        cy.get('[data-cy="subjectTileBtn"]').click()
        cy.get('[data-cy="skillProgressTitle-skill3"] [data-cy="skillProgressTitle"]').click()
        cy.wait('@skills1Deps')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "Shift", "l"]);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2')
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3')
    })

    it('user shortcuts to open training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)

        cy.intercept('/api/projects/proj1/skills/skill1/dependencies').as('skills1Deps')

        // initiate training-wide search dialog from the skill page
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.wait('@skills1Deps')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.realPress(["Control", "k"]);
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcclosebutton"]').click();

        // initiate training-wide search dialog from the subject page
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1')
        cy.get('[data-cy="skillsTitle"]').contains('Subject 1')
        cy.realPress(["Control", "k"]);
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcclosebutton"]').click();

        // initiate training-wide search dialog from the project page
        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]').should('have.text', 'Project: This is project 1');
        cy.realPress(["Control", "k"]);
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcclosebutton"]').click();
    })

    it('customize training search shortcuts and navigate p&r without refresh', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.enableProdMode(1);
        cy.addToMyProjects(1);

        cy.visit('/settings/preferences')
        cy.get('[data-cy="Search_TrainingShortcut"] [data-cy="clearShortcut"]').click()
        cy.get('[data-cy="Search_TrainingShortcut"] [data-cy="shortcutInput"]').focus()
        cy.realPress("Control");
        cy.realPress("Shift");
        cy.realPress("L");

        cy.get('[data-cy="unsavedChangesAlert"]')
        cy.get('[data-cy="userPrefsSettingsSave"]').click()
        cy.get('[data-cy="settingsSavedAlert"]')

        cy.intercept('/api/projects/proj1/skills/skill3/dependencies').as('skills1Deps')

        cy.get('[data-cy="skillTreeLogo"]').click()
        cy.get('[data-cy="project-link-proj1"]').click()
        cy.get('[data-cy="subjectTileBtn"]').click()
        cy.get('[data-cy="skillProgressTitle-skill3"] [data-cy="skillProgressTitle"]').click()
        cy.wait('@skills1Deps')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3')

        cy.get('[data-cy="breadcrumb-subj1"]').focus()
        cy.get('[data-cy="trainingSearchDialog"]').should('not.exist')
        cy.realPress(["Control", "Shift", "l"]);
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcclosebutton"]').click();
    })

    it('tab should not be one of the shortcut keys', () => {
        cy.visit('/settings/preferences')
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="shortcutInput"]').focus()
        cy.realPress("Tab");
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="clearShortcut"]').should('have.focus')
    })

    it('saved settings are preserved', () => {
        cy.visit('/settings/preferences')
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="shortcutInput"]').should( 'have.value', 'Ctrl + Alt + p')
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="clearShortcut"]').click()
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="shortcutInput"]').focus()
        cy.realPress("Control");
        cy.realPress("Shift");
        cy.realPress("l");

        cy.get('[data-cy="unsavedChangesAlert"]')
        cy.get('[data-cy="userPrefsSettingsSave"]').click()
        cy.get('[data-cy="settingsSavedAlert"]')

        cy.visit('/settings/preferences')
        cy.get('[data-cy="Previous_SkillShortcut"] [data-cy="shortcutInput"]').should( 'have.value', 'Ctrl + Shift + l')
    })

})