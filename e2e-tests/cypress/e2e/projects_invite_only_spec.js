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

    it('invite only project full flow', () => {
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
                result.unsuccessfulErrors = ['bsmith@fake.email'];
                res.send(result);
            });
        })
            .as('sendInvites');
        cy.intercept('GET', '/api/myprojects/proj1/name')
            .as('getName');
        cy.intercept('GET', '/api/projects/proj1/token')
            .as('getToken');
        const userIdForDisplay = 'highlander';
        cy.intercept('GET', '/admin/projects/proj1/userRoles/ROLE_PRIVATE_PROJECT_USER*', (req) => {
            req.reply((res) => {
                const payload = res.body;
                if (payload.data && payload.data.length > 0) {
                    const userInfo = payload.data.find(el => el.userId === 'uuuuuu');
                    if (userInfo) {
                        userInfo.userIdForDisplay = userIdForDisplay;
                    }
                }
                res.send(payload);
            });
        })
            .as('getApprovedUsers');
        cy.intercept('DELETE', '/admin/projects/proj1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
            .as('removeAccess');

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
        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.get('[data-cy="inviteExpirationSelect"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="30 days"]').click()
        cy.get('[data-cy=addEmails]')
            .should('be.disabled');
        cy.get('[data-cy="sendInvites-btn"]')
            .should('be.disabled');
        cy.get('[data-cy="inviteEmailInput"]')
            .type('foo;@bar;abc@cba.org;Bob Smith <bsmith@fake.email>');
        cy.get('[data-cy=addEmails]')
            .click();
        cy.get('[data-cy=invalidEmails]')
            .should('be.visible')
            .should('include.text', 'Unable to add the following invalid email recipients: foo, @bar');
        cy.get('[data-cy=inviteRecipient]')
            .eq(0)
            .should('include.text', 'abc@cba.org');
        cy.get('[data-cy=inviteRecipient]')
            .eq(1)
            .should('include.text', 'bsmith@fake.email');
        cy.get('[data-cy="sendInvites-btn"]')
            .should('be.enabled')
            .click();
        cy.wait('@sendInvites');
        cy.get('[data-cy=failedEmails]')
            .should('be.visible')
            .should('include.text', 'bsmith@fake.email');
        cy.logout();
        cy.wait(2000); //wait for invite only cache to clear

        cy.getLinkFromEmail()
            .then((inviteLink) => {
                cy.register('uuuuuu', 'password', false);
                cy.logout();
                cy.login('uuuuuu', 'password');

                cy.visit('/progress-and-rankings/projects/proj1');
                cy.contains('Restricted Access')
                    .should('be.visible');
                cy.get('[data-cy="notAuthorizedExplanation"]').should('contain.text', 'Access to this training is currently restricted');
                cy.get('[data-cy="contactOwnerBtn"]').should('be.visible').click();
                cy.wait(500);//give animation time to complete
                cy.get('[data-cy="contactOwnersMsgInput"]').should('be.visible');

                cy.visit(inviteLink);
                cy.get('[data-cy=joinProject]')
                    .should('be.visible');
                cy.get('[data-cy="breadcrumbItemValue"]')
                    .contains('Join Project This is project 1')
                    .should('be.visible');
                cy.get('[data-cy=joinProject]')
                    .click();
                cy.get('[data-cy=project-link-proj1]')
                    .should('be.visible')
                cy.wait(10 * 1000); //wait for countdown timer
                cy.get('[data-pc-name="breadcrumb"] [data-cy="breadcrumbItemValue"]')
                  .eq(0)
                  .should('contain.text', 'Progress And Rankings');
                cy.get('[data-pc-name="breadcrumb"] [data-cy="breadcrumbItemValue"]')
                  .eq(1)
                  .should('contain.text', 'proj1');
                cy.get('[data-cy="skillsDisplayHome"] [data-cy="myRankBtn"]')
                cy.logout();

                cy.fixture('vars.json')
                    .then((vars) => {
                        if (!Cypress.env('oauthMode')) {
                            cy.log('NOT in oauthMode, using form login');
                            cy.login(vars.defaultUser, vars.defaultPass);
                        } else {
                            cy.log('oauthMode, using loginBySingleSignOn');
                            cy.loginBySingleSignOn();
                        }
                        cy.visit('/administrator/projects/proj1/access');
                        cy.wait('@emailSupported');
                        cy.wait('@getApprovedUsers');
                        cy.get('[data-cy=privateProjectUsersTable]')
                            .contains(userIdForDisplay)
                            .should('be.visible');
                        cy.get('[data-cy="privateProjectUsersTable_revokeUserAccessBtn"]')
                            .click();
                        cy.contains(`Are you sure you want to revoke ${userIdForDisplay}'s access to this Project? ${userIdForDisplay}'s achievements will NOT be deleted, however ${userIdForDisplay} will no longer be able to access the training profile.`)
                            .should('be.visible');
                        cy.get('[data-pc-name="pcacceptbutton"]').click()
                        cy.wait('@removeAccess');
                        cy.wait('@getApprovedUsers');
                        cy.get('[data-cy=privateProjectUsersTable] [data-cy="skillsBTableTotalRows"]')
                            .should('have.text', '0');
                        cy.logout();
                        cy.login('uuuuuu', 'password');

                        cy.visit('/progress-and-rankings/projects/proj1');
                        cy.contains('Restricted Access')
                            .should('be.visible');
                        cy.get('[data-cy="notAuthorizedExplanation"]').should('contain.text', 'Access to this training is currently restricted');
                        cy.get('[data-cy="contactOwnerBtn"]').should('be.visible');
                    });
            });

    });

    if (!Cypress.env('oauthMode')) {
        it('revoke access should support paging when users exceed minimum page size', () => {
            cy.createInviteOnly();
            cy.intercept('GET', '/admin/projects/TestInviteOnlyProject1/settings')
                .as('getSettings');
            cy.intercept('POST', '/admin/projects/TestInviteOnlyProject1/settings')
                .as('saveSettings');
            cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
                .as('emailSupported');
            cy.intercept('GET', '/api/myprojects/TestInviteOnlyProject1/name')
                .as('getName');
            cy.intercept('GET', '/api/projects/TestInviteOnlyProject1/token')
                .as('getToken');
            cy.intercept('GET', '/admin/projects/TestInviteOnlyProject1/userRoles/ROLE_PRIVATE_PROJECT_USER*')
                .as('getApprovedUsers');
            cy.intercept('DELETE', '/admin/projects/TestInviteOnlyProject1/users/*/roles/ROLE_PRIVATE_PROJECT_USER')
                .as('revokeUser');

            cy.visit('/administrator/projects/TestInviteOnlyProject1/access');
            cy.wait('@emailSupported');
            cy.wait('@getApprovedUsers');
            const tableSelector = '[data-cy=privateProjectUsersTable]';
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user9@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user8@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user7@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user6@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user55@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user3@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user1@skills.org'
                }],
            ], 5);

            cy.get('[data-cy="privateProjectUsers-userIdFilter"]')
                .type('user2');
            cy.wait('@getApprovedUsers');
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
            ], 5, true, null, false);
            cy.get(`${tableSelector} tbody [role="row"]`)
                .should('have.length', '2');

            cy.get(`${tableSelector} [data-pc-section="filterclearicon"]`).click()
            cy.wait('@getApprovedUsers');
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user9@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user8@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user7@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user6@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user55@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user3@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user1@skills.org'
                }],
            ], 5);

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.get('[data-pc-name="pcacceptbutton"]').click();
            cy.wait('@revokeUser');
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user1@skills.org'
                }],
            ], 5, true, null, false);
            cy.get(`${tableSelector} [data-cy=skillsBTableTotalRows]`)
                .should('have.text', '8');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.get('[data-pc-name="pcrejectbutton"]').click();
            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user2@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user22@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user1@skills.org'
                }],
            ], 5, true, null, false);
            cy.get(`${tableSelector} [data-cy=skillsBTableTotalRows]`)
                .should('have.text', '8');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.get('[data-pc-name="pcacceptbutton"]').click();
            cy.wait('@revokeUser');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.get('[data-pc-name="pcacceptbutton"]').click();
            cy.wait('@revokeUser');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.get('[data-pc-name="pcacceptbutton"]').click();
            cy.wait('@revokeUser');

            cy.validateTable(tableSelector, [
                [{
                    colIndex: 0,
                    value: 'user9@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user8@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user7@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user6@skills.org'
                }],
                [{
                    colIndex: 0,
                    value: 'user55@skills.org'
                }],
            ], 5, false, null, false);
        });
    }

    it('cannot manage expirations when email is disabled', () => {
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false');
        cy.createProject(1)
        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'true',
                setting: 'invite_only',
                projectId: 'proj1',
            },
        ]);
        cy.visit('/administrator/projects/proj1/access')
        cy.get('[data-cy=contactUsers_emailServiceWarning]')
          .should('be.visible');
        cy.contains('Please note that email notifications are currently disabled. Email configuration has not been performed on this instance of SkillTree. Please contact the root administrator.')
          .should('be.visible');
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

    it('warn users when admins are added', () => {
        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.createProject(1)
        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'true',
                setting: 'invite_only',
                projectId: 'proj1',
            },
        ]);
        cy.visit('/administrator/projects/proj1/access')

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Administrator"]').click();
        cy.get('[data-cy="addUserBtn"]').click();

        cy.contains('Add Project Administrator?')
        cy.contains('The selected user will be added as an Administrator for this project and will be able to edit/add/delete all aspects of the Project.')
        cy.get('[data-pc-name="pcacceptbutton"]').contains('Add Administrator!').click()
        const tableSelector = '[data-cy=roleManagerTable]';
        cy.get(`${tableSelector} [data-cy="userCell_root@skills.org"]`);

    } )

    it('warn users when approvers are added', () => {
        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.createProject(1)
        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'true',
                setting: 'invite_only',
                projectId: 'proj1',
            },
        ]);
        cy.visit('/administrator/projects/proj1/access')

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Approver"]').click();
        cy.get('[data-cy="addUserBtn"]').click();


        cy.contains('Add Project Approver?')
        cy.contains('The selected user will be added as an Approver for this project and will be able to view all aspects of the Project as well as approve and deny self reporting requests.')
        cy.get('[data-pc-name="pcacceptbutton"]').contains('Add Approver!').click()
        const tableSelector = '[data-cy=roleManagerTable]';
        cy.get(`${tableSelector} [data-cy="userCell_root@skills.org"]`);

    } )

    it('user is warned but then cancelled', () => {
        cy.intercept('POST', '*suggestDashboardUsers*')
            .as('suggest');
        cy.createProject(1)
        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'true',
                setting: 'invite_only',
                projectId: 'proj1',
            },
        ]);
        cy.visit('/administrator/projects/proj1/access')

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Approver"]').click();
        cy.get('[data-cy="addUserBtn"]').click();

        cy.contains('Add Project Approver?')
        cy.contains('The selected user will be added as an Approver for this project and will be able to view all aspects of the Project as well as approve and deny self reporting requests.')
        cy.get('[data-pc-name="pcrejectbutton"]').contains('Cancel').click()
        const tableSelector = '[data-cy=roleManagerTable]';
        cy.get(`${tableSelector} [data-cy="userCell_root@skills.org"]`).should('not.exist');

    } )

    it('must not allow invite submission for the same email twice ', () => {
        cy.createProject(1)
        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'true',
                setting: 'invite_only',
                projectId: 'proj1',
            },
        ]);
        cy.visit('/administrator/projects/proj1/access')
        cy.get('[data-cy="inviteEmailInput"]').type('email1@email.com, email2@email.com')
        cy.get('[data-cy="addEmails"]').click()
        cy.get('[data-cy="inviteRecipient"]').contains('email1@email.com')
        cy.get('[data-cy="sendInvites-btn"]').click()
        cy.get('[data-cy="projectInviteStatusTable"]').contains('email1@email.com')
        cy.get('[data-cy="failedEmails"]').should('not.exist')
        cy.get('[data-cy="inviteEmailInput"]').should('have.value', '')

        // try to add again
        cy.get('[data-cy="inviteEmailInput"]').type('email1@email.com, email2@email.com')
        cy.get('[data-cy="addEmails"]').click()
        cy.get('[data-cy="inviteRecipient"]').contains('email1@email.com')
        cy.get('[data-cy="sendInvites-btn"]').click()
        cy.get('[data-cy="projectInviteStatusTable"]').contains('email1@email.com')

        cy.get('[data-cy="failedEmails"]').contains('email1@email.com already has a pending invite')
        cy.get('[data-cy="failedEmails"]').contains('email2@email.com already has a pending invite')
        cy.get('[data-cy="inviteEmailInput"]').should('have.value', "email1@email.com\nemail2@email.com")
    });

    it('Paging works appropriately', () => {
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
            recipients: ['abc1@abc.org', 'cba1@cba.org', 'foo1@foo.org', 'abc@abc.org', 'abc2@abc.org', 'abc3@abc.org', 'abc4@abc.org', 'abc5@abc.org', 'abc6@abc.org', 'abc7@abc.org'],
            validityDuration: 'P90D'
        });

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.wait('@loadInviteStatus');

        cy.get(`[data-cy="projectInviteStatusTable"] [data-pc-section="columntitle"]`).contains('Recipient').click()

        const tableSelector = '[data-cy=projectInviteStatusTable]';
        const expected = [
            [{
                colIndex: 0,
                value: 'abc1@abc.org'
            }],
            [{
                colIndex: 0,
                value: 'abc2@abc.org'
            }],
            [{
                colIndex: 0,
                value: 'abc3@abc.org'
            }],
            [{
                colIndex: 0,
                value: 'abc4@abc.org'
            }],
            [{
                colIndex: 0,
                value: 'abc5@abc.org'
            }],
            [{
                colIndex: 0,
                value: 'abc6@abc.org'
            }],
            [{
                colIndex: 0,
                value: 'abc7@abc.org'
            }],
            [{
                colIndex: 0,
                value: 'abc@abc.org'
            }],
            [{
                colIndex: 0,
                value: 'cba1@cba.org'
            }],
            [{
                colIndex: 0,
                value: 'foo1@foo.org'
            }],
        ];
        cy.validateTable(tableSelector, expected, 5, false);
    });
});