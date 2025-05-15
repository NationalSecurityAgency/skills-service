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

    it('do not show request new invite button if email service is not configured', () => {
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false').as('emailFeature')
        cy.createProject(1);
        cy.setProjToInviteOnly(1)
        const invalidInvite = '/join-project/proj1/51a1bfd875bb1781e887728f03e3cc700271f1d52a2452a71c5df820824ad28b?pn=Proj'
        cy.visit(invalidInvite)
        cy.wait('@emailFeature')

        cy.get('[data-cy="invalidInvite"]').contains('Unfortunately, this invite to Proj training is no longer valid')
        cy.get('[data-cy="requestNewInvite"]').should('not.exist')
    })
})