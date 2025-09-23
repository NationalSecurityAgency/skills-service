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
import './community_warnings_help_commands.js'

describe('Project - Community Attachment Warning Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    beforeEach(() => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });

        const msg = 'Friendly Reminder: Only safe files please for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.attachmentWarningMessage = msg;
                res.send(conf);
            });
        }).as('loadConfig');


        cy.viewport(1400, 1000)
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.createProject(2, {enableProtectedUserCommunity: true});
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
    });

    it('edit project', () => {
        cy.visit('/administrator/')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]', 'Editing Existing Project')
        cy.validateAllDragonsWarning()

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.openDescModalAndAttachFile('[data-cy="projectCard_proj2"] [data-cy="editProjBtn"]', 'Editing Existing Project')
        cy.validateDivineDragonWarning()
    })

    it('edit project - from project page', () => {
        cy.visit('/administrator/projects/proj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_edit-project"]', 'Editing Existing Project')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_edit-project"]', 'Editing Existing Project')
        cy.validateDivineDragonWarning()
    })

    it('subject creation', () => {
        cy.visit('/administrator/projects/proj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_Subjects"]', 'New Subject')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_Subjects"]', 'New Subject')
        cy.validateDivineDragonWarning()
    });

    it('subject edit', () => {
        cy.visit('/administrator/projects/proj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editBtn"]', 'Editing Existing Subject')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editBtn"]', 'Editing Existing Subject')
        cy.validateDivineDragonWarning()
    });

    it('subject edit - subj page', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_edit-subject"]', 'Editing Existing Subject')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_edit-subject"]', 'Editing Existing Subject')
        cy.validateDivineDragonWarning()
    });

    it('skill creation', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="newSkillButton"]', 'New Skill')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="newSkillButton"]', 'New Skill')
        cy.validateDivineDragonWarning()
    });

    it('edit skill', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editSkillButton_skill1"]', 'Edit Skill')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editSkillButton_skill1"]', 'Edit Skill')
        cy.validateDivineDragonWarning()
    });

    it('edit skill - from skill page', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editSkillButton_skill1"]', 'Edit Skill')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editSkillButton_skill1"]', 'Edit Skill')
        cy.validateDivineDragonWarning()
    });

    it('badge creation', () => {
        cy.visit('/administrator/projects/proj1/badges')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_Badges"]', 'New Badge')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2/badges')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_Badges"]', 'New Badge')
        cy.validateDivineDragonWarning()
    });

    it('edit badge', () => {
        cy.createBadge(1, 1)
        cy.createBadge(2, 1)
        cy.visit('/administrator/projects/proj1/badges')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editBtn"]', 'Editing Existing Badge')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2/badges')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="editBtn"]', 'Editing Existing Badge')
        cy.validateDivineDragonWarning()
    });

    it('edit badge - from badge page', () => {
        cy.createBadge(1, 1)
        cy.createBadge(2, 1)
        cy.visit('/administrator/projects/proj1/badges/badge1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_edit-badge"]', 'Editing Existing Badge')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2/badges/badge1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_edit-badge"]', 'Editing Existing Badge')
        cy.validateDivineDragonWarning()
    });

    it('skill group creation', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="newGroupButton"]', 'New Skills Group')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="newGroupButton"]', 'New Skills Group')
        cy.validateDivineDragonWarning()
    });

    it('edit skill group', () => {
        cy.createSkillsGroup(1,1,1)
        cy.createSkillsGroup(2,1,1)
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.openDescModalAndAttachFile('[data-cy="editSkillButton_group1"]', 'Edit Skills Group')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/projects/proj2/subjects/subj1')
        cy.openDescModalAndAttachFile('[data-cy="editSkillButton_group1"]', 'Edit Skills Group')
        cy.validateDivineDragonWarning()
    });

    it('skill justification', () => {
        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(2, 1, 2, { selfReportingType: 'Approval' });
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill2')
        cy.wait('@loadConfig')
        cy.get('[data-cy="requestApprovalBtn"]').click()

        const attachmentBtnSelector = '[data-cy="markdownEditorInput"] button.attachment-button'
        const markdownEditorToolbarIconsSelector = '[data-cy="markdownEditorInput"] button.toastui-editor-toolbar-icons'
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');
        cy.addAttachment(attachmentBtnSelector)
        cy.validateAllDragonsWarning()

        cy.visit('/progress-and-rankings/projects/proj2/subjects/subj1/skills/skill2')
        cy.wait('@loadConfig')
        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.get(markdownEditorToolbarIconsSelector).should('be.visible')
        cy.get(attachmentBtnSelector).should('be.visible');
        cy.addAttachment(attachmentBtnSelector)
        cy.validateDivineDragonWarning()
    });

    it('skill justification from subject page', () => {
        const attachmentBtnSelector = '[data-cy="markdownEditorInput"] button.attachment-button'
        const markdownEditorToolbarIconsSelector = '[data-cy="markdownEditorInput"] button.toastui-editor-toolbar-icons'

        cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });

        cy.createSkill(2, 1, 2, { selfReportingType: 'Approval' });
        cy.createSkill(2, 1, 3, { selfReportingType: 'Approval' });
        cy.createSkill(2, 1, 4, { selfReportingType: 'Approval' });
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1')
        cy.wait('@loadConfig')
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Great Skill 2')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="skillProgressTitle"]').contains('Great Skill 4')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]').click()
        cy.get(`[data-cy="skillProgress_index-1"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="skillProgress_index-1"] ${attachmentBtnSelector}`).should('be.visible');
        cy.addAttachment(`[data-cy="skillProgress_index-1"] ${attachmentBtnSelector}`)

        cy.get('[data-cy="skillProgress_index-1"]  [data-cy="attachmentWarningMessage"]').contains('Friendly Reminder: Only safe files please for All Dragons')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requestApprovalBtn"]').click()
        cy.get(`[data-cy="skillProgress_index-2"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="skillProgress_index-2"] ${attachmentBtnSelector}`).should('be.visible');
        cy.addAttachment(`[data-cy="skillProgress_index-2"] ${attachmentBtnSelector}`, `[data-cy="skillProgress_index-2"] `)

        cy.get('[data-cy="skillProgress_index-2"]  [data-cy="attachmentWarningMessage"]').contains('Friendly Reminder: Only safe files please for All Dragons')

        cy.visit('/progress-and-rankings/projects/proj2/subjects/subj1')
        cy.wait('@loadConfig')
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Great Skill 2')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="skillProgressTitle"]').contains('Great Skill 4')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]').click()
        cy.get(`[data-cy="skillProgress_index-1"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="skillProgress_index-1"] ${attachmentBtnSelector}`).should('be.visible');
        cy.addAttachment(`[data-cy="skillProgress_index-1"] ${attachmentBtnSelector}`)

        cy.get('[data-cy="skillProgress_index-1"]  [data-cy="attachmentWarningMessage"]').contains('Friendly Reminder: Only safe files please for Divine Dragon')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requestApprovalBtn"]').click()
        cy.get(`[data-cy="skillProgress_index-2"] ${markdownEditorToolbarIconsSelector}`).should('be.visible')
        cy.get(`[data-cy="skillProgress_index-2"] ${attachmentBtnSelector}`).should('be.visible');
        cy.addAttachment(`[data-cy="skillProgress_index-2"] ${attachmentBtnSelector}`, `[data-cy="skillProgress_index-2"] `)

        cy.get('[data-cy="skillProgress_index-2"]  [data-cy="attachmentWarningMessage"]').contains('Friendly Reminder: Only safe files please for Divine Dragon')

    });

});