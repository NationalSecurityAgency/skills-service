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

describe('Community Project Email Header/Footer Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    beforeEach(() => {
        Cypress.Commands.add('rejectRequest', (requestNum = 0, rejectionMsg = 'Skill was rejected') => {
            cy.request('/admin/projects/proj1/approvals?limit=10&ascending=true&page=1&orderBy=userId')
              .then((response) => {
                  cy.request('POST', '/admin/projects/proj1/approvals/reject', {
                      skillApprovalIds: [response.body.data[requestNum].id],
                      rejectionMessage: rejectionMsg,
                  });
              });
        });
        Cypress.Commands.add('navToSettings', () => {
            cy.get('[data-cy="settings-button"] button')
              .click();
            cy.get('[data-cy="settingsButton-navToSettings"]')
              .should('not.be.disabled');
            cy.get('[data-cy="settingsButton-navToSettings"]')
              .click({force: true});
        });
        cy.intercept({
            method: 'GET',
            url: '/root/global/settings/GLOBAL.EMAIL'
        }).as('loadTemplateSettings');

        cy.fixture('vars.json').then((vars) => {
            cy.register('user1', vars.defaultPass);
            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${allDragonsUser}/tags/dragons`, { tags: ['DivineDragon'] });

            cy.visit('/settings/email');
            cy.get('[data-cy="nav-Email"]').click();
            cy.wait('@loadTemplateSettings');
            cy.get('[data-cy="htmlHeader"]').type('For {{}{{} community.descriptor {}}{}} Only');
            cy.get('[data-cy=plainTextHeader]').click().type('For {{}{{}community.descriptor {}}{}} Only');
            cy.get('[data-cy=htmlFooter]').click().type('For {{}{{} community.descriptor {}}{}} Only');
            cy.get('[data-cy=plainTextFooter]').click().type('For {{}{{}community.descriptor {}}{}} Only');
            cy.get('[data-cy=emailTemplateSettingsSave]').click();
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });
    });

    it('UC protected project has UC default communityHeaderDescriptor value replaced in header/footer in invite email', () => {
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

        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
          .should('be.visible')
          .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="acceptbutton"]').click()
        cy.get('[data-cy="saveSettingsBtn"').click();
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="nav-Access"')
          .click();
        cy.wait('@emailSupported');
        cy.get('[data-cy="inviteExpirationSelect"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="30 minutes"]').click();
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

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });
    });

    it('Non-UC protected project has default communityHeaderDescriptor value replaced in header/footer in invite email', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})
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

        cy.get('[data-cy="projectVisibilitySelector"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Private Invite Only"]').click();
        cy.get('[data-pc-name="dialog"] [data-pc-section="message"]')
          .should('be.visible')
          .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users')
        cy.get('[data-pc-name="dialog"] [data-pc-name="acceptbutton"]').click()

        cy.get('[data-cy="saveSettingsBtn"').click()
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="nav-Access"')
          .click();
        cy.wait('@emailSupported');
        cy.get('[data-cy="inviteExpirationSelect"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="30 minutes"]').click()
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

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });
    });

    it('UC protected project has UC communityHeaderDescriptor value replaced in header/footer in contact users email', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
          .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/contactUsersCount')
          .as('updateCount');
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/levels')
          .as('getSubjectLevels');

        cy.visit('/administrator/projects/proj1/contact-users');

        cy.get('[data-cy="nav-Contact Users"]')
          .click();
        cy.wait('@emailSupported');

        cy.get('[data-cy=previewUsersEmail]')
          .should('be.disabled');
        cy.get('[data-cy=emailUsers_subject]')
          .type('Test Subject');
        cy.get('[data-cy=previewUsersEmail]')
          .should('be.disabled');
        cy.get('[data-cy="markdownEditorInput"]')
          .type('Test Body');
        cy.get('[data-cy=previewUsersEmail]')
          .should('be.enabled');

        cy.get('[data-cy=previewUsersEmail]')
          .click();
        cy.get('[data-cy=emailSent]')
          .should('be.visible');

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For Divine Dragon Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For Divine Dragon Only')
          });
    });

    it('Non-UC protected project has default communityHeaderDescriptor value replaced in header/footer in contact users email', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
          .as('emailSupported');
        cy.intercept('POST', '/admin/projects/proj1/contactUsersCount')
          .as('updateCount');
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/levels')
          .as('getSubjectLevels');

        cy.visit('/administrator/projects/proj1/contact-users');

        cy.get('[data-cy="nav-Contact Users"]')
          .click();
        cy.wait('@emailSupported');

        cy.get('[data-cy=previewUsersEmail]')
          .should('be.disabled');
        cy.get('[data-cy=emailUsers_subject]')
          .type('Test Subject');
        cy.get('[data-cy=previewUsersEmail]')
          .should('be.disabled');
        cy.get('[data-cy="markdownEditorInput"]')
          .type('Test Body');
        cy.get('[data-cy=previewUsersEmail]')
          .should('be.enabled');

        cy.get('[data-cy=previewUsersEmail]')
          .click();
        cy.get('[data-cy=emailSent]')
          .should('be.visible');

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });
    });

    it('UC protected project has UC communityHeaderDescriptor value replaced in header/footer in self report request email', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject();
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'user1', '2020-09-17 11:00', true, 'Please Approve');

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For Divine Dragon Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For Divine Dragon Only')
          });
    });

    it('Non-UC protected project has default communityHeaderDescriptor value replaced in header/footer in self report request email', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})
        cy.createSubject();
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'user1', '2020-09-17 11:00', true, 'Please Approve');

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });
    });

    it('UC protected project has UC communityHeaderDescriptor value replaced in header/footer in self report reject email', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject();
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'skills@skills.org', '2020-09-17 11:00', true, 'Please Approve');

        // remove request email
        cy.getEmails();
        cy.resetEmail();

        cy.rejectRequest(0, 'Sorry, try again!');

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For Divine Dragon Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For Divine Dragon Only')
          });
    });

    it('Non-UC protected project has default communityHeaderDescriptor value replaced in header/footer in self report reject email', () => {
        cy.createProject(1, {enableProtectedUserCommunity: false})
        cy.createSubject();
        cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
        cy.reportSkill(1, 1, 'skills@skills.org', '2020-09-17 11:00', true, 'Please Approve');

        // remove request email
        cy.getEmails();
        cy.resetEmail();

        cy.rejectRequest(0, 'Sorry, try again!');

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });
    });

    it('UC protected project has UC communityHeaderDescriptor value replaced in header/footer for contact project owner', () => {
        cy.intercept('POST', '/api/projects/*/contact').as('contact');
        cy.intercept('POST', '/api/validation/description*').as('validate');
        cy.createProject(3, {enableProtectedUserCommunity: true})
        cy.enableProdMode(3);
        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.get('[data-cy="contactOwnerBtn_proj3"]').should('be.visible').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('exist');
        cy.get('[data-cy="contactOwnersMsgInput"]').click().fill('aaa bbb this is a message');
        cy.get('[data-cy="messageNumCharsRemaining"]').should('contain.text', '2,475 characters remaining');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.wait('@contact');
        cy.get('[data-cy="closeDialogBtn"]').should('contain.text', 'OK');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'Message sent!');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'The Project Administrator(s) of This is project 3 will be notified of your question via email.');
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');
        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For Divine Dragon Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For Divine Dragon Only')
          });
    });

    it('Non-UC protected project has default communityHeaderDescriptor value replaced in header/footer for contact project owner', () => {
        cy.intercept('POST', '/api/projects/*/contact').as('contact');
        cy.intercept('POST', '/api/validation/description*').as('validate');
        cy.createProject(3, {enableProtectedUserCommunity: false})
        cy.enableProdMode(3);
        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.get('[data-cy="contactOwnerBtn_proj3"]').should('be.visible').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('exist');
        cy.get('[data-cy="contactOwnersMsgInput"]').click().fill('aaa bbb this is a message');
        cy.get('[data-cy="messageNumCharsRemaining"]').should('contain.text', '2,475 characters remaining');
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.wait('@contact');
        cy.get('[data-cy="closeDialogBtn"]').should('contain.text', 'OK');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'Message sent!');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'The Project Administrator(s) of This is project 3 will be notified of your question via email.');
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');
        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });
    });

    it('UC protected project has default communityHeaderDescriptor value replaced in header/footer for admin/approver added to project email', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.visit('/administrator/projects/proj1/access');
        cy.intercept('PUT', '/admin/projects/proj1/users/alldragons@email.org/roles/ROLE_PROJECT_ADMIN')
          .as('addAdminAttempt');

        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');
        cy.intercept('GET', '/app/userInfo').as('loadUserInfo');
        cy.intercept('GET', '/admin/projects/proj1').as('loadProject');

        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserInfo');
        cy.wait('@loadProject');

        // cy.get('[data-cy="existingUserInput"]').type('all');
        // cy.wait('@suggest');
        // cy.wait(500);
        // cy.get('#existingUserInput_0').contains(allDragonsUser).click();
        // cy.get('[data-cy="userRoleSelector"]').click()
        // cy.get('[data-pc-section="panel"] [aria-label="Administrator"]').click();
        //
        // cy.get('[data-cy="addUserBtn"]').click();
        // cy.getHeaderFromEmail()
        //   .then((header) => {
        //       expect(header).to.equal('For All Dragons Only')
        //   });
        // cy.getFooterFromEmail(false)
        //   .then((footer) => {
        //       expect(footer).to.equal('For All Dragons Only')
        //   });
        //
        // cy.resetEmail();
        //
        // cy.get('[data-cy="existingUserInput"]').type('root');
        // cy.wait('@suggest');
        // cy.wait(500);
        // cy.get('#existingUserInput_0').contains('root').click();
        // cy.get('[data-cy="userRoleSelector"]').click()
        // cy.get('[data-pc-section="panel"] [aria-label="Approver"]').click();
        // cy.get('[data-cy="addUserBtn"]').click();
        //
        // cy.getHeaderFromEmail()
        //   .then((header) => {
        //       expect(header).to.equal('For All Dragons Only')
        //   });
        // cy.getFooterFromEmail(false)
        //   .then((footer) => {
        //       expect(footer).to.equal('For All Dragons Only')
        //   });
    });

    it('default communityHeaderDescriptor value replaced in header/footer for add root user email', () => {
        cy.loginAsRootUser()
        cy.intercept('POST', '/root/users/without/role/ROLE_SUPER_DUPER_USER?userSuggestOption=ONE')
          .as('getEligibleForRoot');
        cy.intercept('GET', '/root/users/roles/ROLE_SUPER_DUPER_USER**')
          .as('loadRootUsers');
        cy.visit('/settings/security');
        cy.wait('@loadRootUsers');

        cy.get('[data-cy="existingUserInput"]')
          .first()
          .click()
          .type('sk{enter}');
        cy.wait('@getEligibleForRoot');
        cy.contains('skills@skills.org')
          .click({ force: true });
        cy.contains('Add')
          .first()
          .click();

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });
    });

    it.skip('default communityHeaderDescriptor value replaced in header/footer for contact all admins email', () => {
        cy.createProject(1, {enableProtectedUserCommunity: true})
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.loginAsRootUser()
        cy.intercept('GET', '/public/isFeatureSupported?feature=emailservice')
          .as('emailSupported');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPER_DUPER_USER')
          .as('isRoot');

        cy.visit('/administrator/');

        cy.get('[data-cy="nav-Contact Admins"]').click();
        cy.wait('@isRoot');
        cy.get('[data-cy="projectAdminCount"]').should('have.text', '2')

        cy.get('[data-cy=emailUsers_subject]')
          .type('Test Subject');
        cy.get('[data-cy="markdownEditorInput"]')
          .type('Test Body');
        cy.get('[data-cy=previewAdminEmail]')
          .click();

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });

        cy.resetEmail();
        cy.get('[data-cy=emailUsers-submitBtn]')
          .click();
        cy.get('[data-cy=emailSent]')
          .should('be.visible');

        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });
    });

    it.skip('default communityHeaderDescriptor value replaced in header/footer for reset password email', () => {
        cy.register('test@skills.org', 'apassword', false);
        cy.visit('/administrator/');
        cy.get('[data-cy=forgotPassword]')
          .click();
        cy.get('[data-cy=forgotPasswordEmail]')
          .should('exist');
        cy.get('[data-cy=forgotPasswordEmail]')
          .type('test@skills.org');
        cy.get('[data-cy=resetPassword')
          .click();
        cy.getHeaderFromEmail()
          .then((header) => {
              expect(header).to.equal('For All Dragons Only')
          });
        cy.getFooterFromEmail(false)
          .then((footer) => {
              expect(footer).to.equal('For All Dragons Only')
          });
    });

});
