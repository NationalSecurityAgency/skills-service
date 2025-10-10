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
import './community-commands'

describe('Community and Desc Prefix Project Pages Tests', () => {

    const allDragonsUser = 'allDragons@email.org'
    beforeEach( () => {
        const descMsg = 'Friendly Reminder: Only safe descriptions for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.addPrefixToInvalidParagraphsOptions = 'All Dragons:(A) ,(B) |Divine Dragon:(A) ,(B) ,(C) ,(D) ';
                conf.descriptionWarningMessage = descMsg;
                res.send(conf);
            });
        }).as('getConfig');

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, "password");
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);


        });

        cy.viewport(1400, 1400)
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2)
        cy.createBadge(1, 1, {description: null})

        cy.createProject(2, {enableProtectedUserCommunity: true});
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkillsGroup(2, 1, 2)
        cy.createBadge(2, 1, {description: null})
    });

    it('paragraph prefix - switch between projects', () => {
        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.validateDivineDragonOptions('[data-cy="pageHeader"] [data-cy="editSkillButton_skill1"]')

        cy.get('[data-cy="breadcrumb-subj1"]').click()
        cy.validateDivineDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_skill1"]')

        cy.get('[data-cy="breadcrumb-proj2"]').click()
        cy.validateDivineDragonOptions('[data-cy="btn_Subjects"]')

        // Project 1
        cy.get('[data-cy="breadcrumb-Projects"]').click()
        cy.get('[data-cy="projCard_proj1_manageBtn"]').click()
        cy.validateAllDragonOptions('[data-cy="btn_Subjects"]')
        cy.get('[data-cy="manageBtn_subj1"]').click()
        cy.validateAllDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_skill1"]')
        cy.get('[data-cy="manageSkillLink_skill1"]').click()
        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="editSkillButton_skill1"]')
    });

    it('project pages - divine dragon - skill page', () => {
        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.validateDivineDragonOptions('[data-cy="pageHeader"] [data-cy="editSkillButton_skill1"]')
    })

    it('project pages - divine dragon - subject page', () => {
        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.get('@getConfig')
        cy.validateDivineDragonOptions('[data-cy="pageHeader"] [data-cy="btn_edit-subject"]')
        cy.validateDivineDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_skill1"]')
        cy.validateDivineDragonOptions('[data-cy="newSkillButton"]')
        cy.validateDivineDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_group2"]')
        cy.validateDivineDragonOptions('[data-cy="newGroupButton"]')
    })

    it('project pages - divine dragon - project page', () => {
        cy.visit('/administrator/projects/proj2')
        cy.get('@getConfig')
        cy.validateDivineDragonOptions('[data-cy="btn_Subjects"]')
        cy.validateDivineDragonOptions('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]')
    })

    it('project pages - divine dragon - contact page', () => {
        cy.visit('/administrator/projects/proj2/contact-users')
        cy.get('@getConfig')
        cy.get('[data-cy="nav-Contact Users"]').click()
        cy.validateAllDragonOptions(null)
    })

    it('project pages - divine dragon - badges page', () => {
        cy.visit('/administrator/projects/proj2/badges')
        cy.get('@getConfig')
        cy.validateDivineDragonOptions('[data-cy="badgeCard-badge1"] [data-cy="editBtn"]')
        cy.validateDivineDragonOptions('[data-cy="btn_Badges"]')
        cy.get('[data-cy="manageBtn_badge1"]').click()
        cy.validateDivineDragonOptions('[data-cy="pageHeader"] [data-cy="btn_edit-badge"]')
    });

    it('project pages - all dragons - skill page', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')
        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="editSkillButton_skill1"]')
    })

    it('project pages - all dragons - subject page', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('@getConfig')

        cy.get('[data-cy="breadcrumb-subj1"]').click()
        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="btn_edit-subject"]')
        cy.validateAllDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_skill1"]')
        cy.validateAllDragonOptions('[data-cy="newSkillButton"]')
        cy.validateAllDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_group2"]')
        cy.validateAllDragonOptions('[data-cy="newGroupButton"]')
    })

    it('project pages - all dragons - project page', () => {
        cy.visit('/administrator/projects/proj1')
        cy.get('@getConfig')
        cy.validateAllDragonOptions('[data-cy="btn_Subjects"]')
        cy.validateAllDragonOptions('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]')
    })

    it('project pages - all dragons - contact page', () => {
        cy.visit('/administrator/projects/proj1/contact-users')
        cy.get('@getConfig')
        cy.validateAllDragonOptions(null)
    })

    it('project pages - all dragons - badges', () => {
        cy.visit('/administrator/projects/proj1/badges')
        cy.get('@getConfig')
        cy.get('[data-cy="nav-Badges"]').click()
        cy.validateAllDragonOptions('[data-cy="badgeCard-badge1"] [data-cy="editBtn"]')
        cy.validateAllDragonOptions('[data-cy="btn_Badges"]')
        cy.get('[data-cy="manageBtn_badge1"]').click()
        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="btn_edit-badge"]')
    });

    it('project pages - all dragons proj as all dragons user - skill page', () => {
        cy.assignUserAsAdmin('proj1', allDragonsUser)

        cy.logout()
        cy.login(allDragonsUser)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')
        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="editSkillButton_skill1"]')
    })

    it('project pages - all dragons proj as all dragons user - subject page', () => {
        cy.assignUserAsAdmin('proj1', allDragonsUser)

        cy.logout()
        cy.login(allDragonsUser)
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('@getConfig')

        cy.get('[data-cy="breadcrumb-subj1"]').click()
        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="btn_edit-subject"]')
        cy.validateAllDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_skill1"]')
        cy.validateAllDragonOptions('[data-cy="newSkillButton"]')
        cy.validateAllDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_group2"]')
        cy.validateAllDragonOptions('[data-cy="newGroupButton"]')
    })

    it('project pages - all dragons proj as all dragons user - project page', () => {
        cy.assignUserAsAdmin('proj1', allDragonsUser)

        cy.logout()
        cy.login(allDragonsUser)
        cy.visit('/administrator/projects/proj1')
        cy.get('@getConfig')
        cy.validateAllDragonOptions('[data-cy="btn_Subjects"]')
        cy.validateAllDragonOptions('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]')
    })

    it('project pages - all dragons proj as all dragons user - contact page', () => {
        cy.assignUserAsAdmin('proj1', allDragonsUser)

        cy.logout()
        cy.login(allDragonsUser)
        cy.visit('/administrator/projects/proj1/contact-users')
        cy.get('@getConfig')
        cy.validateAllDragonOptions(null)
    })

    it('project pages - all dragons proj as all dragons user - badges', () => {
        cy.assignUserAsAdmin('proj1', allDragonsUser)

        cy.logout()
        cy.login(allDragonsUser)
        cy.visit('/administrator/projects/proj1/badges')
        cy.get('@getConfig')
        cy.get('[data-cy="nav-Badges"]').click()
        cy.validateAllDragonOptions('[data-cy="badgeCard-badge1"] [data-cy="editBtn"]')
        cy.validateAllDragonOptions('[data-cy="btn_Badges"]')
        cy.get('[data-cy="manageBtn_badge1"]').click()
        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="btn_edit-badge"]')
    });
});
