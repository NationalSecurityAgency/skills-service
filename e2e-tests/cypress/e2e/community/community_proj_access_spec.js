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

    it('community protected project cannot assign non community member admin access', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        // cy.visit('/administrator/projects/proj1/access');
        cy.intercept('PUT', '/admin/projects/proj1/users/alldragons@email.org/roles/ROLE_PROJECT_ADMIN')
          .as('addAdminAttempt');

        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1').as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]').type('all');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains(allDragonsUser).click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Administrator"]').click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addAdminAttempt').its('response.statusCode').should('eq', 400);
        cy.get('[data-cy=error-msg]')
          .contains('Error! Request could not be completed! User [allDragons@email.org] is not allowed to be assigned [Admin] user role');
    });

    it('community protected project cannot assign non community member approver access', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.intercept('PUT', '/admin/projects/proj1/users/alldragons@email.org/roles/ROLE_PROJECT_APPROVER')
          .as('addApproverAttempt');

        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1').as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        cy.get('[data-cy="existingUserInput"]').type('all');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('#existingUserInput_0').contains(allDragonsUser).click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Approver"]').click();
        cy.get('[data-cy="addUserBtn"]').click();
        cy.wait('@addApproverAttempt').its('response.statusCode').should('eq', 400);
        cy.get('[data-cy=error-msg]')
          .contains('Error! Request could not be completed! User [allDragons@email.org] is not allowed to be assigned [Approver] user role');
    });

    it.skip('cannot join invite only community protected project if user non community member', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.intercept('GET', '/admin/projects/proj1/settings')
          .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
          .as('saveSettings');
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
          .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/invite', (req) => {
            req.reply((res) => {
                const result = res.body;
                result.successful = [allDragonsUser];
                // result.unsuccessful = ['bsmith@fake.email'];
                // result.unsuccessfulErrors = ['bsmith@fake.email'];
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
                    const userInfo = payload.data.find(el => el.userId === allDragonsUser);
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

        // cy.get('[data-cy="projectVisibilitySelector"]')
        //   .select('pio');
        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
          .should('be.visible')
          .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="acceptbutton"]').click()
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
          .type(allDragonsUser);
        cy.get('[data-cy=addEmails]')
          .click();
        cy.get('[data-cy=inviteRecipient]')
          .eq(0)
          .should('include.text', allDragonsUser);
        cy.get('[data-cy="sendInvites-btn"]')
          .should('be.enabled')
          .click();
        cy.wait('@sendInvites');
        cy.logout();
        cy.wait(2000); //wait for invite only cache to clear

        cy.getLinkFromEmail()
          .then((inviteLink) => {
              cy.logout();
              cy.login(allDragonsUser, 'password');

              cy.visit('/progress-and-rankings/projects/proj1');
              cy.contains('Invite Only Project')
                .should('be.visible');
              cy.get('[data-cy="notAuthorizedExplanation"]').should('contain.text', 'This Project is configured for Invite Only access.');
              cy.get('[data-cy="contactOwnerBtn"]').should('be.visible').click();
              cy.wait(500);//give animation time to complete
              cy.get('[data-cy="contactOwnersMsgInput"]').should('be.visible');

              cy.visit(inviteLink);
              cy.url().should('include', '/not-authorized');
              cy.contains('User Not Authorized').should('be.visible');
          });

    });
});
