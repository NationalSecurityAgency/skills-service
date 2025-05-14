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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Projects Invite-Only Tests', () => {
    beforeEach(() => {
        cy.intercept('GET', '/app/projects')
            .as('getProjects');
        cy.intercept('GET', '/api/icons/customIconCss')
            .as('getProjectsCustomIcons');
        cy.intercept('GET', '/app/userInfo')
            .as('getUserInfo');
        cy.intercept('/admin/projects/proj1/users/root@skills.org/roles*')
            .as('getRolesForRoot');
    });

    it('Extend expired invite', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = ['abc@cba.org'];
                result.unsuccessful = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');
        cy.intercept('GET', '/admin/projects/proj1/errors*')
            .as('loadIssues');
        cy.intercept('POST', '/admin/projects/proj1/invites/extend').as('extendInviteExpiration');

        cy.intercept('GET', '/admin/projects/proj1/invites/status**').as('loadInviteStatus');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
            .should('be.visible')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcacceptbutton"]').click()

        cy.get('[data-cy="saveSettingsBtn"').click()
        cy.wait('@saveSettings');
        cy.wait('@getSettings');

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc@abc.org'],
            validityDuration: 'PT1S'
        });

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc1@abc.org', 'cba1@cba.org', 'foo1@foo.org'],
            validityDuration: 'PT55M'
        });

        cy.wait(1000); //wait for invite to expire

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.wait('@loadInviteStatus');
        cy.contains('abc@abc.org').should('be.visible');
        cy.get(`[data-cy="projectInviteStatusTable"] [data-pc-section="columntitle"]`).contains('Recipient').click()
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(0).should('contain.text', 'abc@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(2).should('contain.text', 'expired');

        cy.get('[data-cy="projectInviteStatusTable"] [data-cy="extendInvite-abc@abc.org"]').eq(0).click()
        cy.get('[data-pc-name="menu"] [aria-label="30 minutes"]').click()
        cy.wait('@extendInviteExpiration');
        cy.wait('@loadInviteStatus');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(0).should('contain.text', 'abc@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(2).should('contain.text', 'in 30 minutes');
    });

    it('Extend un-expired invite', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = ['abc@cba.org'];
                result.unsuccessful = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');
        cy.intercept('GET', '/admin/projects/proj1/errors*')
            .as('loadIssues');
        cy.intercept('POST', '/admin/projects/proj1/invites/extend').as('extendInviteExpiration');

        cy.intercept('GET', '/admin/projects/proj1/invites/status**').as('loadInviteStatus');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
            .should('be.visible')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcacceptbutton"]').click()
        cy.get('[data-cy="saveSettingsBtn"').click()
        cy.wait('@saveSettings');
        cy.wait('@getSettings');

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc@abc.org'],
            validityDuration: 'P1D'
        });

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc1@abc.org', 'cba1@cba.org', 'foo1@foo.org'],
            validityDuration: 'P90D'
        });

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.wait('@loadInviteStatus');
        cy.contains('abc@abc.org').should('be.visible');
        cy.get(`[data-cy="projectInviteStatusTable"] [data-pc-section="columntitle"]`).contains('Recipient').click()
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(0).should('contain.text', 'abc@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(2).should('contain.text', 'in a day');

        cy.get('[data-cy="projectInviteStatusTable"] [data-cy="extendInvite-abc@abc.org"]').eq(0).click()
        cy.get('[data-pc-name="menu"] [aria-label="7 days"] [data-cy="invite-3-extension"]').click()
        //
        cy.wait('@extendInviteExpiration');
        cy.wait('@loadInviteStatus');
        cy.contains('abc1@abc.org').should('be.visible');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(0).should('contain.text', 'abc@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(2).should('contain.text', 'in 8 days');
    });

    it('delete expired invite', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = ['abc@cba.org'];
                result.unsuccessful = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');
        cy.intercept('GET', '/admin/projects/proj1/errors*')
            .as('loadIssues');
        cy.intercept('POST', '/admin/projects/proj1/invites/extend').as('extendInviteExpiration');

        cy.intercept('GET', '/admin/projects/proj1/invites/status**').as('loadInviteStatus');
        cy.intercept('DELETE', '/admin/projects/proj1/invites/*').as('deleteInvite');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
            .should('be.visible')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcacceptbutton"]').click()
        cy.get('[data-cy="saveSettingsBtn"').click()
        cy.wait('@saveSettings');
        cy.wait('@getSettings');

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc@abc.org'],
            validityDuration: 'PT1S'
        });

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc1@abc.org', 'cba1@cba.org', 'foo1@foo.org'],
            validityDuration: 'P90D'
        });

        cy.wait(1000);

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.wait('@loadInviteStatus');
        cy.contains('abc@abc.org').should('be.visible');
        cy.get('[data-cy="projectInviteStatusTable"] tr').should('have.length', 5); //account for header row
        cy.get(`[data-cy="projectInviteStatusTable"] [data-pc-section="columntitle"]`).contains('Recipient').click()
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(0).should('contain.text', 'abc@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(2).should('contain.text', 'expired');
        cy.openDialog('[data-cy="controls_abc@abc.org"] [data-cy="deleteInvite"]')
        cy.contains('Removal Safety Check').should('be.visible');
        cy.get('[data-cy="currentValidationText"]').type('Delete Me');
        cy.clickSaveDialogBtn()
        cy.wait('@deleteInvite');
        cy.wait('@loadInviteStatus');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(1).children('td').eq(0).should('contain.text', 'abc1@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(1).children('td').eq(2).should('not.contain.text', 'expired');
        cy.get('[data-cy="projectInviteStatusTable"] tr').should('have.length', 4);
    });

    it('delete unexpired invite', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = ['abc@cba.org'];
                result.unsuccessful = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');
        cy.intercept('GET', '/admin/projects/proj1/errors*')
            .as('loadIssues');
        cy.intercept('POST', '/admin/projects/proj1/invites/extend').as('extendInviteExpiration');

        cy.intercept('GET', '/admin/projects/proj1/invites/status**').as('loadInviteStatus');
        cy.intercept('DELETE', '/admin/projects/proj1/invites/*').as('deleteInvite');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
            .should('be.visible')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcacceptbutton"]').click()
        cy.get('[data-cy="saveSettingsBtn"').click()
        cy.wait('@saveSettings');
        cy.wait('@getSettings');

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc@abc.org'],
            validityDuration: 'PT1H'
        });

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc1@abc.org', 'cba1@cba.org', 'foo1@foo.org'],
            validityDuration: 'P90D'
        });

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.wait('@loadInviteStatus');
        cy.contains('abc@abc.org').should('be.visible');
        cy.get('[data-cy="projectInviteStatusTable"] tr').should('have.length', 5); //account for header row
        cy.get(`[data-cy="projectInviteStatusTable"] [data-pc-section="columntitle"]`).contains('Recipient').click()
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(0).should('contain.text', 'abc@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(2).should('not.contain.text', 'expired');
        cy.openDialog('[data-cy="controls_abc@abc.org"] [data-cy="deleteInvite"]')
        cy.contains('Removal Safety Check').should('be.visible');
        cy.get('[data-cy="currentValidationText"]').type('Delete Me');
        cy.clickSaveDialogBtn()
        cy.wait('@deleteInvite');
        cy.wait('@loadInviteStatus');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(1).children('td').eq(0).should('contain.text', 'abc1@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(1).children('td').eq(2).should('not.contain.text', 'expired');
        cy.get('[data-cy="projectInviteStatusTable"] tr').should('have.length', 4);
    });

    it('remind user of invite', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = ['abc@cba.org'];
                result.unsuccessful = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');
        cy.intercept('GET', '/admin/projects/proj1/errors*')
            .as('loadIssues');
        cy.intercept('POST', '/admin/projects/proj1/invites/extend').as('extendInviteExpiration');

        cy.intercept('GET', '/admin/projects/proj1/invites/status**').as('loadInviteStatus');
        cy.intercept('POST', '/admin/projects/proj1/invites/*/remind').as('remindUser');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
            .should('be.visible')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcacceptbutton"]').click()
        cy.get('[data-cy="saveSettingsBtn"').click()
        cy.wait('@saveSettings');
        cy.wait('@getSettings');

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc@abc.org'],
            validityDuration: 'PT1H'
        });

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc1@abc.org', 'cba1@cba.org', 'foo1@foo.org'],
            validityDuration: 'P90D'
        });

        cy.getEmails(4).then(() => {
            cy.resetEmail();
        });

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.wait('@loadInviteStatus');
        cy.get(`[data-cy="projectInviteStatusTable"] [data-pc-section="columntitle"]`).contains('Recipient').click()
        cy.contains('abc@abc.org').should('be.visible');
        cy.get('[data-cy="remindUser-abc@abc.org"]').click();
        cy.wait('@remindUser');
        cy.get('[id="accessNotificationPanel"]').should('be.visible');
        cy.get('[id=accessNotificationPanel]').should('contain.text', 'Invite reminder sent!');

        cy.getEmails().then((emails) => {
            const reminderEmail = emails.find((e) => e.subject === 'SkillTree Project Invitation Reminder')
            expect(reminderEmail.to[0].address).to.equal('abc@abc.org');
            expect(reminderEmail.text).to.contain('This is a friendly reminder that you have been invited to join');
        });
    });

    it('cannot remind user if invite is expired without first extending', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = ['abc@cba.org'];
                result.unsuccessful = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');
        cy.intercept('GET', '/admin/projects/proj1/errors*')
            .as('loadIssues');
        cy.intercept('POST', '/admin/projects/proj1/invites/extend').as('extendInviteExpiration');

        cy.intercept('GET', '/admin/projects/proj1/invites/status**').as('loadInviteStatus');
        cy.intercept('POST', '/admin/projects/proj1/invites/*/remind').as('remindUser');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
            .should('be.visible')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcacceptbutton"]').click()
        cy.get('[data-cy="saveSettingsBtn"').click()
        cy.wait('@saveSettings');
        cy.wait('@getSettings');

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc@abc.org'],
            validityDuration: 'PT1S'
        });

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc1@abc.org', 'cba1@cba.org', 'foo1@foo.org'],
            validityDuration: 'P90D'
        });

        cy.wait(1000);

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.wait('@loadInviteStatus');
        cy.contains('abc@abc.org').should('be.visible');
        cy.get(`[data-cy="projectInviteStatusTable"] [data-pc-section="columntitle"]`).contains('Recipient').click()
        cy.get('[data-cy="remindUser-abc@abc.org"]').should('be.disabled');
        cy.get('[data-cy="extendInvite-abc@abc.org"]').click();
        cy.get('[data-pc-name="menu"] [aria-label="30 minutes"] [data-cy="invite-3-extension"]').click()
        // cy.get('[data-cy="invite-0-extension"]').eq(0).click();
        cy.wait('@loadInviteStatus')
        cy.contains('abc@abc.org').should('be.visible');
        cy.get('[data-cy="remindUser-abc@abc.org"]').should('be.enabled').click();
        cy.wait('@remindUser');
        cy.get('[id="accessNotificationPanel"]').should('be.visible');
        cy.get('[id=accessNotificationPanel]').should('contain.text', 'Invite reminder sent!');

        cy.getEmails(5).then((emails) => {
            const reminderEmail = emails.find((e) => e.subject === 'SkillTree Project Invitation Reminder')
            expect(reminderEmail.to[0].address).to.equal('abc@abc.org');
            expect(reminderEmail.html).to.contain('This is a friendly reminder that you have been invited to join');
        });
    });

    it('cannot send reminder if invite expires after status table is loaded', () => {
        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
            .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = ['abc@cba.org'];
                result.unsuccessful = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');
        cy.intercept('GET', '/admin/projects/proj1/errors*')
            .as('loadIssues');
        cy.intercept('POST', '/admin/projects/proj1/invites/extend').as('extendInviteExpiration');

        cy.intercept('GET', '/admin/projects/proj1/invites/status**').as('loadInviteStatus');
        cy.intercept('POST', '/admin/projects/proj1/invites/*/remind').as('remindUser');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
            .should('be.visible')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="pcacceptbutton"]').click()
        cy.get('[data-cy="saveSettingsBtn"').click()
        cy.wait('@saveSettings');
        cy.wait('@getSettings');

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc@abc.org'],
            validityDuration: 'PT5S'
        });

        cy.request('POST', '/admin/projects/proj1/invite', {
            recipients: ['abc1@abc.org', 'cba1@cba.org', 'foo1@foo.org'],
            validityDuration: 'P90D'
        });

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.get(`[data-cy="projectInviteStatusTable"] [data-pc-section="columntitle"]`).contains('Recipient').click()
        cy.wait('@emailSupported');
        cy.wait('@loadInviteStatus');
        cy.contains('abc@abc.org').should('be.visible');
        cy.wait(5000);

        cy.get('[data-cy="projectInviteStatusTable"] tr').should('have.length', 5); //account for header row
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(0).should('contain.text', 'abc@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(2).should('not.contain.text', 'expired');
        cy.get('[data-cy="remindUser-abc@abc.org"]').eq(0).should('be.enabled');
        cy.get('[data-cy="remindUser-abc@abc.org"]').eq(0).should('be.enabled').click();
        cy.get('[data-pc-section="title"]').should('contain.text', 'Expired Invite');
        cy.wait('@loadInviteStatus');
        cy.get('[data-pc-name="pcacceptbutton"]').click()
        cy.get('[data-cy="projectInviteStatusTable"] tr').should('have.length', 5); //account for header row
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(0).should('contain.text', 'abc@abc.org');
        cy.get('[data-cy="projectInviteStatusTable"] tr').eq(2).children('td').eq(2).should('contain.text', 'expired');
    });

})