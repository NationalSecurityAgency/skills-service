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

        cy.get('[data-cy="projectVisibilitySelector"]')
            .select('pio');
        cy.get('.modal-content')
            .should('be.visible')
            .should('include.text', 'Changing to Invite Only')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users.');
        cy.clickButton('Ok');
        cy.get('[data-cy="saveSettingsBtn"')
            .click({ force: true });
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.get('[data-cy="inviteExpirationSelect"]')
            .select('PT30M');
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
            .should('include.text', 'Unable to send invites to: bsmith@fake.email');
        cy.logout();
        cy.wait(2000); //wait for invite only cache to clear

        cy.getLinkFromEmail()
            .then((inviteLink) => {
                cy.register('uuuuuu', 'password', false);
                cy.logout();
                cy.login('uuuuuu', 'password');

                cy.visit('/progress-and-rankings/projects/proj1');
                cy.contains('User Not Authorized')
                    .should('be.visible');

                cy.visit(inviteLink);
                cy.get('[data-cy=joinProject]')
                    .should('be.visible');
                cy.get('[data-cy=breadcrumb-item]')
                    .contains('Join Project This is project 1')
                    .should('be.visible');
                cy.get('[data-cy=joinProject]')
                    .click();
                cy.get('[data-cy=project-link-proj1]')
                    .should('be.visible')
                    .should('have.attr', 'href', '/progress-and-rankings/projects/proj1')
                    .contains('This is project 1')
                    .should('be.visible');
                cy.wait(10 * 1000); //wait for countdown timer
                cy.wait('@getToken');
                cy.dashboardCd()
                    .contains('My Level');
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
                        cy.clickButton('Yes, revoke access!');
                        cy.wait('@removeAccess');
                        cy.wait('@getApprovedUsers');
                        cy.get('[data-cy=privateProjectUsersTable]')
                            .contains(userIdForDisplay)
                            .should('not.exist');
                        cy.logout();
                        cy.login('uuuuuu', 'password');

                        cy.visit('/progress-and-rankings/projects/proj1');
                        cy.contains('User Not Authorized')
                            .should('be.visible');
                    });
            });

    });

    it('Invite Only duplicate input validation', () => {
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

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');

        cy.get('[data-cy="projectVisibilitySelector"]')
            .select('pio');
        cy.get('.modal-content')
            .should('be.visible')
            .should('include.text', 'Changing to Invite Only')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users.');
        cy.clickButton('Ok');
        cy.get('[data-cy="saveSettingsBtn"')
            .click({ force: true });
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait('@emailSupported');
        cy.get('[data-cy="inviteExpirationSelect"]')
            .select('PT30M');
        cy.get('[data-cy=addEmails]')
            .should('be.disabled');
        cy.get('[data-cy="sendInvites-btn"]')
            .should('be.disabled');
        cy.get('[data-cy="inviteEmailInput"]')
            .type('abc@cba.org;abc@cba.org,abc@cba.org\nabc@cba.org\nBob Smith <abc@cba.org>');
        // try to navigate away while the input form has content
        cy.get('[data-cy="nav-Settings"')
            .click();
        cy.get('[data-cy="nav-Settings"]')
            .should('not.have.class', 'bg-primary');
        cy.get('[data-cy="nav-Access"]')
            .should('have.class', 'bg-primary');
        cy.contains('Discard Emails?')
            .should('be.visible');
        cy.contains('Cancel')
            .click();
        //navigation should be cancelled
        cy.get('[data-cy="nav-Settings"]')
            .should('not.have.class', 'bg-primary');
        cy.get('[data-cy="nav-Access"]')
            .should('have.class', 'bg-primary');
        cy.get('[data-cy="inviteEmailInput"]')
            .should('be.visible');
        cy.get('[data-cy=addEmails]')
            .click();
        cy.get('[data-cy=inviteRecipient]')
            .should('have.length', 1);
        cy.get('[data-cy="inviteEmailInput"]')
            .should('be.empty');
        cy.get('[data-cy=inviteRecipient]')
            .should('include.text', 'abc@cba.org');
        //try navigate away with recipients that have not yet been sent
        cy.get('[data-cy="nav-Issues"')
            .click();
        cy.get('[data-cy="nav-Issues"]')
            .should('not.have.class', 'bg-primary');
        cy.get('[data-cy="nav-Access"]')
            .should('have.class', 'bg-primary');
        cy.contains('Discard Recipients?')
            .should('be.visible');
        cy.contains('Let\'s Go!')
            .click();
        //navigation should proceed
        cy.wait('@loadIssues');
        cy.contains('Project Issues')
            .should('be.visible');
        cy.get('[data-cy="nav-Access"]')
            .should('not.have.class', 'bg-primary');
        cy.get('[data-cy="nav-Issues"]')
            .should('have.class', 'bg-primary');
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
            cy.get('[data-cy="privateProjectUsers-filterBtn"]')
                .click();
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
            cy.get(`${tableSelector} [data-cy=skillsBTableTotalRows]`)
                .should('not.exist');
            cy.get(`${tableSelector} tbody [role="row"]`)
                .should('have.length', '2');

            cy.get('[data-cy=privateProjectUsers-resetBtn]')
                .click();
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
            cy.contains('Yes, revoke access!')
                .click();
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
            cy.get('[data-cy=skillsBTableTotalRows]')
                .should('have.text', '8');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.contains('Cancel')
                .click();
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
            cy.get('[data-cy=skillsBTableTotalRows]')
                .should('have.text', '8');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.contains('Yes, revoke access!')
                .click();
            cy.wait('@revokeUser');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.contains('Yes, revoke access!')
                .click();
            cy.wait('@revokeUser');

            cy.get('[data-cy=privateProjectUsersTable_revokeUserAccessBtn]')
                .eq(0)
                .click();
            cy.contains('Yes, revoke access!')
                .click();
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
            cy.get('[data-cy=skillsBTableTotalRows]')
                .should('not.exist');

        });
    }
});