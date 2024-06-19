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

describe('Community Project Creation Tests', () => {

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
    });

    it('create restricted community project', () => {
        cy.visit('/administrator')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]').type('one')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"]').should('not.exist')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="projectCard_one"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')
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
        cy.visit('/administrator')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"] a').contains( 'User Community Docs')
        cy.get('[data-cy="userCommunityDocsLink"] a[href="https://somedocs.com"]')
    });

    it('create non-restricted community project', () => {
        cy.visit('/administrator')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]').type('one')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="projectCard_one"] [data-cy="userCommunity"]').contains('For All Dragons Nation')
    });

    it('create restricted community project by editing existing project', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="userCommunity"]').contains('For All Dragons Nation')
        cy.get('[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]').click()

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="projectCard_proj1"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    });

    it('create restricted community project by editing existing project from a project page', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For All Dragons Nation')

        cy.get('[data-cy="btn_edit-project"]').click()

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="pageHeader"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="btn_edit-project"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    });

    it('once restricted community is enabled it cannot be disabled', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})

        cy.visit('/administrator')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    });

    it('community validator should be selected based on the community - projection creation', () => {
        cy.visit('/administrator')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]').type('one')
        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="markdownEditorInput"]').clear().type('divinedragon')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('community validator should be selected based on the community - edit existing project', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false, description: 'some text'})
        cy.visit('/administrator')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="markdownEditorInput"]').clear().type('divinedragon')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('community validator should be selected based on the community - edit existing project from project page', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false, description: 'some text'})
        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_edit-project"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - paragraphs may not contain jabberwocky')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

        cy.get('[data-cy="markdownEditorInput"]').clear().type('divinedragon')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('edit existing community project should use community validator', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'some text '})
        cy.visit('/administrator/')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('edit existing community project should use community validator - project page', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true, description: 'some text '})

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_edit-project"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky')
        cy.wait(1000)
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="markdownEditorInput"]').type('divinedragon')
        cy.get('[data-cy="descriptionError"]').should('have.text', 'Project Description - May not contain divinedragon word')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
    });

    it('do not allow to enable if project does not meet community requirements', () => {
        cy.createProject(1, { enableProtectedUserCommunity: false })
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.request('POST', `/admin/projects/proj1/users/${allDragonsUser}/roles/ROLE_PROJECT_APPROVER`);

        cy.visit('/administrator/')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="communityProtectionErrors"]').should('not.exist')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="communityProtectionErrors"]').contains('Unable to restrict access to Divine Dragon users only')
        cy.get('[data-cy="communityProtectionErrors"]').contains(`Has existing ${allDragonsUser} user that is not authorized`)
        cy.get('[data-cy="communityProtectionErrors"]').contains('Has skill(s) that have been exported to the Skills Catalog')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="communityRestrictionWarning"]').should('not.exist')
    });

    it('attachments are not enabled on project creation when UC protection is available', () => {
        cy.visit('/administrator')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-cy="projectName"]').type('one')
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"]').should('not.exist')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get(`button.attachment-button`).should('not.exist');

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="projectCard_one"] [data-cy="userCommunity"]').contains('For Divine Dragon Nation')

        cy.get('[data-cy="projectCard_one"] [data-cy="editProjBtn"]').click()
        cy.get(`button.attachment-button`).should('exist');

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(allDragonsUser, vars.defaultPass);
            cy.visit('/administrator')
            cy.get('[data-cy="inception-button"]').contains('Level');
            cy.get('[data-cy="newProjectButton"]').click()
            cy.get('[data-cy="restrictCommunityControls"]').should('not.exist');
            cy.get(`button.attachment-button`).should('exist');
        })
    });
});
