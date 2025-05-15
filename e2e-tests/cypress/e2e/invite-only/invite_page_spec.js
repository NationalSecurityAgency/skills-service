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

    it('users must be able to request a new invite from the expiration invitation page', () => {
        cy.createProject(1);
        cy.setProjToInviteOnly(1)
        const invalidInvite = '/join-project/proj1/51a1bfd875bb1781e887728f03e3cc700271f1d52a2452a71c5df820824ad28b?pn=Proj'
        cy.visit(invalidInvite)

        cy.get('[data-cy="invalidInvite"]').contains('Unfortunately, this invite to Proj training is no longer valid')
        cy.get('[data-cy="inviteRequestSent"]').should('not.exist')
        cy.get('[data-cy="requestNewInvite"]').click()

        cy.get('[data-cy="invalidInvite"]').should('not.exist')
        cy.get('[data-cy="requestNewInvite"]').should('not.exist')
        cy.get('[data-cy="inviteRequestSent"]')
        cy.get('[data-cy="takeMeHome"]')
            .should('have.attr', 'href', '/');

        cy.fixture('vars.json')
            .then((vars) => {
                cy.getEmails().then((emails) => {
                    const userId = Cypress.env('oauthMode') ? vars.oauthUser : vars.defaultUser;
                    const reminderEmail = emails.find((e) => e.subject === 'New Invite Request for SkillTree Project')
                    expect(reminderEmail.to[0].address).to.equal(userId);
                    expect(reminderEmail.text).to.contain(`requested a new invite for This is project 1 because the current invite is no longer valid`);
                });
            });
    })

    it('show that invite was accepted with the link to the training - user already accepted the invite, ', () => {
        cy.createProject(1);
        cy.setProjToInviteOnly(1)
        cy.inviteUser(1, 'abc@abc.org')
        cy.getLinkFromEmail()
            .then((inviteLink) => {
                cy.register('otherU@skills.org', 'password', false);
                cy.logout();
                cy.login('otherU@skills.org', 'password');
                cy.visit(inviteLink)
                cy.get('[data-cy="joinProjectContainer"] [data-cy="joinProject"]').click()
                cy.get('[data-cy="joinProjectContainer"]').contains('Congratulations! You\'re now a member of This is project 1!')

                cy.visit(inviteLink)
                cy.get('[data-cy="joinProjectContainer"]').contains('Already Enrolled')
                cy.get('[data-cy="joinProjectContainer"]').contains('Congratulations').should('not.exist')
                cy.get('[data-cy="joinProjectContainer"]').contains('You\'re already a member of This is project 1')
                cy.get('[data-cy="joinProjectContainer"] [data-cy="project-link-proj1"]').contains('View Training')
            })
    })

    it('show that invite was accepted with the link to the training - non-existent invite', () => {
        cy.createProject(1);
        cy.setProjToInviteOnly(1)
        cy.inviteUser(1, 'abc@abc.org')
        cy.getLinkFromEmail()
            .then((inviteLink) => {
                cy.register('otherU@skills.org', 'password', false);
                cy.logout();
                cy.login('otherU@skills.org', 'password');
                cy.visit(inviteLink)
                cy.get('[data-cy="joinProjectContainer"] [data-cy="joinProject"]').click()
                cy.get('[data-cy="joinProjectContainer"]').contains('Congratulations! You\'re now a member of This is project 1!')

                const invalidInvite = '/join-project/proj1/51a1bfd875bb1781e887728f03e3cc700271f1d52a2452a71c5df820824ad28b?pn=Proj'
                cy.visit(invalidInvite)
                cy.get('[data-cy="joinProjectContainer"]').contains('Already Enrolled')
                cy.get('[data-cy="joinProjectContainer"]').contains('Congratulations').should('not.exist')
                cy.get('[data-cy="joinProjectContainer"]').contains('You\'re already a member of Proj')
                cy.get('[data-cy="joinProjectContainer"] [data-cy="project-link-proj1"]').contains('View Training')
            })
    })
})