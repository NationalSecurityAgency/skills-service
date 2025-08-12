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

    beforeEach(() => {
        cy.on('uncaught:exception', (err, runnable) => {
            return false
        })
    });

    it('Project Does Not Exist', () => {
        cy.visit('/administrator/projects/fake');
        cy.url().should('include', '/error')
        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.visit('/administrator/projects/fake/subjects/fake');
        cy.url().should('include', '/error')
        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.visit('/administrator/projects/fake/subjects/fake/skills/fake');
        cy.url().should('include', '/error')
        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.visit('/administrator/projects/fake/badges/fake');
        cy.url().should('include', '/error')
        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('You do not have permission to view/manage this Project OR this Project does not exist');
    });

    it('User Not Authorized For Project', () => {
        cy.register('user1', 'password1', false);
        cy.register('user2', 'password2', false);
        cy.logout();
        cy.login('user1', 'password1');
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            enabled: false,
            projectId: 'proj1',
            name: 'Badge1',
            badgeId: 'badge1',
            description: '',
            iconClass: 'fas fa-award'
        });
        cy.logout();
        cy.login('user2', 'password2');
        cy.visit('/administrator/projects/proj1');

        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('You do not have permission to view/manage this Project OR this Project does not exist');

        cy.visit('/administrator/projects/proj1/badges/badge1');
        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('You do not have permission to view/manage this Project OR this Project does not exist');
    });

    it('Subject Not Found', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/fake'
        })
            .as('loadSubject');

        cy.visit('/administrator/projects/proj1/subjects/fake');
        cy.wait('@loadSubject');

        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('Subject [fake] doesn\'t exist');
    });

    it('Skill Not Found', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        })
            .as('loadSkill');
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill');

        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('Skill [skill1] doesn\'t exist.');
    });

    it('Badge Not Found', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/badges/fake'
        })
            .as('loadBadge');

        cy.visit('/administrator/projects/proj1/badges/fake');
        cy.wait('@loadBadge');

        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('Badge [fake] doesn\'t exist');
    });

    it('Global Badge Not Found', () => {
        cy.intercept({
            method: 'GET',
            url: '/admin/badges/fake'
        })
            .as('loadGlobalBadge');
        cy.visit('/administrator/globalBadges/fake');
        cy.wait('@loadGlobalBadge');

        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
          .contains('You do not have permission to view/manage this Global Badge OR this Global Badge does not exist');
    });

    it('Global Badge Not Authorized', () => {
        cy.request('POST', '/app/badges/globalBadge1', {
            'enabled': false,
            'originalBadgeId': '',
            'name': 'globalBadge1',
            'badgeId': 'globalBadge1',
            'description': '',
            'iconClass': 'fas fa-award'
        });
        cy.register('user1', 'password1', false);
        cy.logout();
        cy.login('user1', 'password1');

        cy.intercept('GET', '/admin/badges/globalBadge1')
            .as('loadGlobalBadge');
        cy.visit('/administrator/globalBadges/globalBadge1');
        cy.wait('@loadGlobalBadge');

        cy.get('[data-cy=errExplanation]')
            .should('be.visible');
        cy.get('[data-cy=errExplanation]')
            .contains('You do not have permission to view/manage this Global Badge OR this Global Badge does not exist');
    });

    it('User Not Authorized For Quiz', () => {
        cy.register('user1', 'password1', false);
        cy.register('user2', 'password2', false);
        cy.logout();
        cy.login('user1', 'password1');
        cy.createQuizDef(1)
        cy.logout();
        cy.login('user2', 'password2');
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy=errorTitle]').should('have.text', 'User Not Authorized');
        cy.get('[data-cy=errExplanation]').should('have.text','You do not have permission to view/manage this Quiz OR this Quiz does not exist');
    });

    it('User Not Authorized For Admin Group', () => {
        cy.register('user1', 'password1', false);
        cy.register('user2', 'password2', false);
        cy.logout();
        cy.login('user1', 'password1');
        cy.createAdminGroupDef(1)
        cy.logout();
        cy.login('user2', 'password2');
        cy.visit('/administrator/adminGroups/adminGroup1');

        cy.get('[data-cy=errorTitle]').should('have.text', 'User Not Authorized');
        cy.get('[data-cy=errExplanation]').should('have.text','You do not have permission to view/manage this Admin Group OR this Admin Group does not exist');
    });

    it('Default Message for Not Authorized', () => {
        cy.intercept('GET', '/app/projects', { statusCode: 403 })
            .as('getUserInfo');
        cy.visit('/administrator');
        cy.wait('@getUserInfo');

        cy.get('[data-cy=errorTitle]').should('have.text', 'User Not Authorized');
        cy.get('[data-cy=errExplanation]').should('have.text','You do not have permission to view this page');
    });

});
