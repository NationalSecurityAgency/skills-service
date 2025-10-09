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

describe('Community and Desc Prefix Project Tests', () => {

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

        cy.viewport(1400, 1000)
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2)
        cy.createBadge(1, 1)

        cy.createProject(2, {enableProtectedUserCommunity: true});
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkillsGroup(2, 1, 2)
        cy.createBadge(2, 1)
    });

    it('load proper options when switching between projects', () => {
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

    it('offer proper community options on project pages - divine dragon', () => {
        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.validateDivineDragonOptions('[data-cy="pageHeader"] [data-cy="editSkillButton_skill1"]')

        cy.get('[data-cy="breadcrumb-subj1"]').click()
        cy.validateDivineDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_skill1"]')
        cy.validateDivineDragonOptions('[data-cy="newSkillButton"]')
        cy.validateDivineDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_group2"]')
        cy.validateDivineDragonOptions('[data-cy="newGroupButton"]')

        cy.get('[data-cy="breadcrumb-proj2"]').click()
        cy.validateDivineDragonOptions('[data-cy="btn_Subjects"]')
        cy.validateDivineDragonOptions('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]')
        cy.get('[data-cy="nav-Contact Users"]').click()
        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="nav-Badges"]').click()
        cy.validateDivineDragonOptions('[data-cy="badgeCard-badge1"] [data-cy="editBtn"]')
        cy.validateDivineDragonOptions('[data-cy="btn_Badges"]')
        cy.get('[data-cy="manageBtn_badge1"]').click()
        cy.validateDivineDragonOptions('[data-cy="pageHeader"] [data-cy="btn_edit-badge"]')
    });

    it('offer proper community options on project pages - all dragons', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="editSkillButton_skill1"]')

        cy.get('[data-cy="breadcrumb-subj1"]').click()
        cy.validateAllDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_skill1"]')
        cy.validateAllDragonOptions('[data-cy="newSkillButton"]')
        cy.validateAllDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_group2"]')
        cy.validateAllDragonOptions('[data-cy="newGroupButton"]')

        cy.get('[data-cy="breadcrumb-proj1"]').click()
        cy.validateAllDragonOptions('[data-cy="btn_Subjects"]')
        cy.validateAllDragonOptions('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]')
        cy.get('[data-cy="nav-Contact Users"]').click()
        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="nav-Badges"]').click()
        cy.validateAllDragonOptions('[data-cy="badgeCard-badge1"] [data-cy="editBtn"]')
        cy.validateAllDragonOptions('[data-cy="btn_Badges"]')
        cy.get('[data-cy="manageBtn_badge1"]').click()
        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="btn_edit-badge"]')
    });

    it('offer proper community options on project pages - all dragons - as all dragon user', () => {
        cy.assignUserAsAdmin('proj1', allDragonsUser)

        cy.logout()
        cy.login(allDragonsUser)

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="editSkillButton_skill1"]')

        cy.get('[data-cy="breadcrumb-subj1"]').click()
        cy.validateAllDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_skill1"]')
        cy.validateAllDragonOptions('[data-cy="newSkillButton"]')
        cy.validateAllDragonOptions('[data-cy="skillsTable"] [data-cy="editSkillButton_group2"]')
        cy.validateAllDragonOptions('[data-cy="newGroupButton"]')

        cy.get('[data-cy="breadcrumb-proj1"]').click()
        cy.validateAllDragonOptions('[data-cy="btn_Subjects"]')
        cy.validateAllDragonOptions('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]')
        cy.get('[data-cy="nav-Contact Users"]').click()
        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="nav-Badges"]').click()
        cy.validateAllDragonOptions('[data-cy="badgeCard-badge1"] [data-cy="editBtn"]')
        cy.validateAllDragonOptions('[data-cy="btn_Badges"]')
        cy.get('[data-cy="manageBtn_badge1"]').click()
        cy.validateAllDragonOptions('[data-cy="pageHeader"] [data-cy="btn_edit-badge"]')
    });

    it('new project', () => {
        cy.visit('/administrator/')
        cy.wait('@getConfig')
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Project')

        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`).should("not.exist")
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`).should("not.exist")

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`).should("not.exist")
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`).should("not.exist")
    })

    it('edit a project - all dragons', () => {
        cy.visit('/administrator')
        cy.get('@getConfig')
        cy.get('[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`).should("not.exist")
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`).should("not.exist")

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`)
    })

    it('edit a project - all dragons - project page', () => {
        cy.visit('/administrator/projects/proj1')
        cy.get('@getConfig')
        cy.get('[data-cy="btn_edit-project"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`).should("not.exist")
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`).should("not.exist")

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]`)
        cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]`)
    })

    it('edit a project - divine dragons', () => {
        cy.visit('/administrator')
        cy.get('@getConfig')
        cy.validateDivineDragonOptions('[data-cy="projectCard_proj2"] [data-cy="editProjBtn"]')
    })

    it('edit a project - divine dragons - project page', () => {
        cy.visit('/administrator/projects/proj2')
        cy.get('@getConfig')
        cy.validateDivineDragonOptions('[data-cy="btn_edit-project"]')
    })

    it('skill justification', () => {
        cy.createSkill(1, 1, 3, {selfReportingType: 'Approval'});
        cy.createSkill(2, 1, 3, {selfReportingType: 'Approval'});
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill3')
        cy.wait('@getConfig')
        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.validateAllDragonOptions(null)

        cy.visit('/progress-and-rankings/projects/proj2/subjects/subj1/skills/skill3')
        cy.wait('@getConfig')
        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.validateDivineDragonOptions(null)
    })

    it('skill justification as all dragons user', () => {
        cy.createSkill(1, 1, 3, {selfReportingType: 'Approval'});
        cy.createSkill(2, 1, 3, {selfReportingType: 'Approval'});

        cy.logout()
        cy.login(allDragonsUser)

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill3')
        cy.wait('@getConfig')
        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.validateAllDragonOptions(null)
    })

    it('skill justification from subject page', () => {
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });

        cy.createSkill(2, 1, 3, { selfReportingType: 'Approval' });
        cy.createSkill(2, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(2, 1, 5, { selfReportingType: 'Approval' });
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1')
        cy.wait('@getConfig')
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="skillProgressTitle"]').contains('Great Skill 5')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="skillProgress_index-1"]')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requestApprovalBtn"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="skillProgress_index-2"]')


        cy.visit('/progress-and-rankings/projects/proj2/subjects/subj1')
        cy.wait('@getConfig')
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="skillProgressTitle"]').contains('Great Skill 5')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]').click()
        cy.validateDivineDragonOptions(null, '[data-cy="skillProgress_index-1"]')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requestApprovalBtn"]').click()
        cy.validateDivineDragonOptions(null, '[data-cy="skillProgress_index-2"]')
    });
});
