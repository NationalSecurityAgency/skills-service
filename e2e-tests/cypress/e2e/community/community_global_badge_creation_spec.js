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

describe('Community Global Badge Creation Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    beforeEach(() => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, {tags: ['DivineDragon']});
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, {tags: ['DivineDragon']});
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });
    });

    it('create restricted community global badge', () => {
        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="noContent"]').contains('No Badges Yet')
        cy.get('[data-cy="btn_Global Badges"]').click()
        cy.get('.p-dialog-header').contains('New Badge')
        cy.get('[data-cy="name"]').type('My First Global Badge')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"]').should('not.exist')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="badgeCard-MyFirstGlobalBadgeBadge"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
    });

    it('Enable (Go Live) a UC protected global badge from the global badge details page', () => {
        cy.intercept('POST', '/admin/badges/globalBadge1/projects/proj1/skills/skill1').as('saveSkill')
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1)
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: true})
        cy.assignSkillToGlobalBadge(1, 1, 1);

        cy.visit('/administrator/globalBadges/globalBadge1')

        cy.get('[data-cy=statPreformatted]')
          .contains('Disabled')
          .should('exist');
        cy.get('[data-cy=goLive]')
          .click();
        cy.contains('Please Confirm!')
          .should('exist');
        cy.contains('Yes, Go Live!')
          .click();

        cy.get('[data-cy=statPreformatted]')
          .contains('Live')
          .should('exist');
    });

    it('Enable (Go Live) a UC protected global badge from the global badges page', () => {
        cy.intercept('POST', '/admin/badges/globalBadge1/projects/proj1/skills/skill1').as('saveSkill')
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1)
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: true})
        cy.assignSkillToGlobalBadge(1, 1, 1);

        cy.visit('/administrator/globalBadges')

        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=badgeStatus]')
          .contains('Status: Disabled')
          .should('exist');
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=goLive]').click()
        cy.contains('Please Confirm!')
          .should('exist');
        cy.contains('Yes, Go Live!')
          .click();

        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=badgeStatus]')
          .contains('Status: Live')
          .should('exist');
    });

    it('show docs links if configured', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.userCommunityDocsLabel = 'User Community Docs';
                conf.userCommunityDocsLink = 'https://somedocs.com';
                res.send(conf);
            });
        })
        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="noContent"]').contains('No Badges Yet')
        cy.get('[data-cy="btn_Global Badges"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"] a').contains('User Community Docs')
        cy.get('[data-cy="userCommunityDocsLink"] a[href="https://somedocs.com"]')
    });

    it('create non-restricted community global badge', () => {
        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="noContent"]').contains('No Badges Yet')
        cy.get('[data-cy="btn_Global Badges"]').click()
        cy.get('.p-dialog-header').contains('New Badge')
        cy.get('[data-cy="name"]').type('My First Global Badge')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="badgeCard-MyFirstGlobalBadgeBadge"] [data-cy="userCommunity"]').contains('All Dragons Nation')
    });

    it('create restricted community global badge by editing existing global badge', () => {
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="userCommunity"]').contains('All Dragons Nation')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=editBtn]').click()

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=editBtn]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    });

    it('create restricted community global badge by editing existing global badge from global badge page', () => {
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For All Dragons Nation')

        cy.get('[data-cy="btn_edit-badge"]').click()

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="btn_edit-badge"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    });

    it('once restricted community is enabled it cannot be disabled', () => {
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: true})

        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=editBtn]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    });

    it('community validator should be selected based on the community - global badge creation', () => {
        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="noContent"]').contains('No Badges Yet')
        cy.get('[data-cy="btn_Global Badges"]').click()
        cy.get('.p-dialog-header').contains('New Badge')
        cy.get('[data-cy="name"]').type('one')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="markdownEditorInput"]').clear().type('divinedragon')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('community validator should be selected based on the community - edit existing project', () => {
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: false, description: 'some text'})
        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="userCommunity"]').contains('All Dragons Nation')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=editBtn]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="markdownEditorInput"]').clear().type('divinedragon')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('community validator should be selected based on the community - edit existing project from global badge page', () => {
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: false, description: 'some text'})

        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For All Dragons Nation')
        cy.get('[data-cy="btn_edit-badge"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="markdownEditorInput"]').clear().type('divinedragon')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('edit existing community project should use community validator', () => {
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: true, description: 'some text'})
        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=editBtn]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('edit existing community project should use community validator - global badge page', () => {
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: true, description: 'some text'})

        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
        cy.get('[data-cy="btn_edit-badge"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Badge Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('do not allow to enable if global badge does not meet community requirements', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.request('POST', `/admin/projects/proj1/users/${allDragonsUser}/roles/ROLE_PROJECT_APPROVER`);

        cy.createGlobalBadge(1, {enableProtectedUserCommunity: false, description: 'some text'})
        cy.request('POST', `/admin/badges/globalBadge1/users/${allDragonsUser}/roles/ROLE_GLOBAL_BADGE_ADMIN`);

        cy.createAdminGroupDef(1, { name: 'Non-UC Protected Admin Group', enableProtectedUserCommunity: false });
        cy.addGlobalBadgeToAdminGroupDef()

        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="userCommunity"]').contains('All Dragons Nation')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=editBtn]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="communityProtectionErrors"]').should('not.exist')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="communityProtectionErrors"]').contains('Unable to restrict access to Divine Dragon users only')
        cy.get('[data-cy="communityProtectionErrors"]').contains(`This global badge has the user ${allDragonsUser} who is not authorized`)
        cy.get('[data-cy="communityProtectionErrors"]').contains('This global badge is part of one or more Admin Groups that do no have Divine Dragon permission')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="communityRestrictionWarning"]').should('not.exist')
    });

    it('Community protection cannot be enabled if global badge is assigned a skill from a non community project - project skill', () => {
        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/abcSkill').as('saveSkill')
        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1)
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: false})
        cy.assignSkillToGlobalBadge(1, 1, 1);

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=editBtn]').click()

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="communityProtectionErrors"]').should('not.exist')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="communityProtectionErrors"]').contains('Unable to restrict access to Divine Dragon users only')
        cy.get('[data-cy="communityProtectionErrors"]').contains('This global badge is linked to the following project(s) that do not have Divine Dragon permission: proj1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="communityRestrictionWarning"]').should('not.exist')
    });

    it('Community protection cannot be enabled if global badge is assigned a skill from a non community project - project level', () => {
        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/abcSkill').as('saveSkill')
        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1)
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: false})
        cy.assignProjectToGlobalBadge(1, 1, 1);

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy=editBtn]').click()

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="communityProtectionErrors"]').should('not.exist')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="communityProtectionErrors"]').contains('Unable to restrict access to Divine Dragon users only')
        cy.get('[data-cy="communityProtectionErrors"]').contains('This global badge is linked to the following project(s) that do not have Divine Dragon permission: proj1')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="communityRestrictionWarning"]').should('not.exist')
    });

    it('A UC protected global badge can be assigned to a skill of a UC protected community project', () => {
        cy.intercept('POST', '/admin/badges/globalBadge1/projects/proj1/skills/skill1').as('saveSkill')
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1)
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: true})

        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.get('[data-cy="noContent"]').contains('No Skills Added Yet');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.wait('@saveSkill')
    });

    it('A non UC protected global badge can be assigned to a skill of a non UC protected community project', () => {
        cy.intercept('POST', '/admin/badges/globalBadge1/projects/proj1/skills/skill1').as('saveSkill')
        cy.createProject(1, {enableProtectedUserCommunity: false})
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1)
        cy.createGlobalBadge(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.get('[data-cy="noContent"]').contains('No Skills Added Yet');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.wait('@saveSkill')
    });

    it('attachments are not enabled on global badge creation when UC protection is available', () => {
        cy.visit('/administrator/globalBadges/')
        cy.get('[data-cy="noContent"]').contains('No Badges Yet')
        cy.get('[data-cy="btn_Global Badges"]').click()
        cy.get('.p-dialog-header').contains('New Badge')
        cy.get('[data-cy="name"]').type('My First Global Badge')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"]').should('not.exist')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get(`button.attachment-button`).should('not.exist');

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="badgeCard-MyFirstGlobalBadgeBadge"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="badgeCard-MyFirstGlobalBadgeBadge"] [data-cy=editBtn]').click()
        cy.get(`button.attachment-button`).should('exist');

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(allDragonsUser, vars.defaultPass);
            cy.visit('/administrator/globalBadges/')
            cy.get('[data-cy="inception-button"]').contains('Level')
            cy.get('[data-cy="btn_Global Badges"]').click()
            cy.get('[data-cy="restrictCommunityControls"]').should('not.exist');
            cy.get(`button.attachment-button`).should('exist');
        })
    });

});
