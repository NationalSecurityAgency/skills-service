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
describe('Error Pages Tests', () => {

    it('Project Does Not Exist', () => {
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/fake'
        }).as('loadProject');
        cy.visit('/Administrator/projects/fake');
        cy.wait('@loadProject');
        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
        cy.get('[data-cy=notAuthorizedExplanation]').contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/fake/subjects/fake'
        }).as('loadSubject');
        cy.visit('/Administrator/projects/fake/subjects/fake');
        cy.wait('@loadSubject');
        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
        cy.get('[data-cy=notAuthorizedExplanation]').contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/fake/subjects/fake/skills/fake'
        }).as('loadSkill');

        cy.visit('/Administrator/projects/fake/subjects/fake/skills/fake');
        cy.wait('@loadSkill');
        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
        cy.get('[data-cy=notAuthorizedExplanation]').contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/fake/badges/fake'
        }).as('loadBadge');

        cy.visit('/Administrator/projects/fake/badges/fake');
        cy.wait('@loadBadge');
        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
        cy.get('[data-cy=notAuthorizedExplanation]').contains('You do not have permission to view/manage this Project OR this Project does not exist');
    });

    it( 'User Not Authorized For Project', () => {
        cy.register('user1', 'password1', false);
        cy.register('user2', 'password2', false);
        cy.logout();
        cy.login('user1', 'password1');
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            enabled:false,
            projectId:"proj1",
            name:"Badge1",
            badgeId:"badge1",
            description:"",
            iconClass:"fas fa-award"
        });
        cy.logout();
        cy.login('user2', 'password2');
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1'
        }).as('loadProject');
        cy.visit('/Administrator/projects/proj1');
        cy.wait('@loadProject');

        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
        cy.get('[data-cy=notAuthorizedExplanation]').contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');
        cy.visit('/Administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');
        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
        cy.get('[data-cy=notAuthorizedExplanation]').contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');
        cy.visit('/Administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill');
        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
        cy.get('[data-cy=notAuthorizedExplanation]').contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/badges/badge1'
        }).as('loadBadge');
        cy.visit('/Administrator/projects/proj1/badges/badge1');
        cy.wait('@loadBadge');
        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
        cy.get('[data-cy=notAuthorizedExplanation]').contains('You do not have permission to view/manage this Project OR this Project does not exist');
    });

    it('Subject Not Found', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        });
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/fake'
        }).as('loadSubject');

        cy.visit('/Administrator/projects/proj1/subjects/fake');
        cy.wait('@loadSubject');

        cy.get('[data-cy=notFoundExplanation]').should('be.visible');
        cy.get('[data-cy=notFoundExplanation]').contains('Subject [fake] doesn\'t exist in project [proj1]');
    })

    it('Skill Not Found', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');
        cy.visit('/Administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill');

        cy.get('[data-cy=notFoundExplanation]').should('be.visible');
        cy.get('[data-cy=notFoundExplanation]').contains('Skill [skill1] doesn\'t exist.');
    });

    it('Badge Not Found', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        });
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/badges/fake'
        }).as('loadBadge');

        cy.visit('/Administrator/projects/proj1/badges/fake');
        cy.wait('@loadBadge');

        cy.get('[data-cy=notFoundExplanation]').should('be.visible');
        cy.get('[data-cy=notFoundExplanation]').contains('Badge [fake] doesn\'t exist');
    });

    it('Global Badge Not Found', () => {
        const supervisorUser = 'supervisor@skills.org';
        cy.register(supervisorUser, 'password');
        cy.login('root@skills.org', 'password');
        cy.request('PUT', `/root/users/${supervisorUser}/roles/ROLE_SUPERVISOR`);
        cy.logout();
        cy.login(supervisorUser, 'password');

        cy.intercept({
            method: 'GET',
            url: '/supervisor/badges/fake'
        }).as('loadGlobalBadge');
        cy.visit('/Administrator/globalBadges/fake');
        cy.wait('@loadGlobalBadge');

        cy.get('[data-cy=notFoundExplanation]').should('be.visible');
        cy.get('[data-cy=notFoundExplanation]').contains('GlobalBadge [fake] doesn\'t exist.');
    });

    it('Global Badge Not Authorized', () => {
        const supervisorUser = 'supervisor@skills.org';
        cy.register(supervisorUser, 'password');
        cy.login('root@skills.org', 'password');
        cy.request('PUT', `/root/users/${supervisorUser}/roles/ROLE_SUPERVISOR`);
        cy.logout();
        cy.login(supervisorUser, 'password');
        cy.request('POST', '/supervisor/badges/globalBadge1', {
            "enabled":false,
            "originalBadgeId":"",
            "name":"globalBadge1",
            "badgeId":"globalBadge1",
            "description":"",
            "iconClass":"fas fa-award"
        });
        cy.register('user1', 'password1', false);
        cy.logout();
        cy.login('user1', 'password1');

        cy.intercept('GET', '/supervisor/badges/globalBadge1').as('loadGlobalBadge');
        cy.visit('/Administrator/globalBadges/globalBadge1');
        cy.wait('@loadGlobalBadge');

        cy.get('[data-cy=notAuthorizedExplanation]').should('be.visible');
        cy.get('[data-cy=notAuthorizedExplanation]').contains('You do not have permission to view/manage this Global Badge OR this Global Badge does not exist');
    });


});
