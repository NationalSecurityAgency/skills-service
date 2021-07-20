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
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';
dayjs.extend(advancedFormatPlugin);

describe('Breadcrumb Navigation Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1',
        }).as('createProject');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1',
        }).as('createSubject');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        }).as('createSkill');

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        }).as('createBadge');

        cy.intercept('GET', '/admin/projects/proj1/badges').as('loadBadges');
        cy.intercept('GET', '/admin/projects/proj1/badges/badge1').as('loadBadge');
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1'
        }).as('loadProject');
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');
    });

    it('Skill Navigation', () => {
        const now = dayjs();
        cy.intercept({method: 'GET', url: '/admin/projects/proj1/subjects/subj1/skills'}).as('loadSkills');
        cy.intercept({method: 'GET', url: '/admin/projects/proj1/subjects/subj1/skills/skill1'}).as('loadSkill');
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/users').as('loadSkillUsers');
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/dependency/graph').as('loadSkillGraph');
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: `user0@skills.org`, timestamp: now.format('x')})

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.wait('@loadSubject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');

        //skill dependency menu
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy=nav-Dependencies]').click();
        cy.wait('@loadSkillGraph');
        cy.get('[data-cy=breadcrumb-skill1]').click();
        cy.wait('@loadSkill');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.wait('@loadSubject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');

        //skill users
        cy.intercept('/admin/projects/movies/token/**').as('userToken');
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/users');
        cy.wait('@loadSkillUsers');
        cy.get('[data-cy=usersTable_viewDetailsBtn]').click();
        cy.get('.client-display-iframe-1').should('be.visible');
        cy.get('[data-cy=breadcrumb-Users]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-skill1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/users/user0@skills.org/skillEvents');
        cy.get('[data-cy="breadcrumb-user0@skills.org"]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Users]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-skill1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/addSkillEvent');
        cy.get('[data-cy=breadcrumb-skill1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/metrics');
        cy.get('[data-cy=breadcrumb-skill1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

    it('Subject Navigation', ()=> {
        cy.intercept({method: 'GET', url: '/admin/projects/proj1/subjects/subj1/skills'}).as('loadSkills');
        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.wait('@loadSubject');
        cy.wait('@loadSkills');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');

        cy.visit('/administrator/projects/proj1/subjects/subj1/levels');
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');

        cy.visit('/administrator/projects/proj1/subjects/subj1/users');
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');

        cy.visit('/administrator/projects/proj1/subjects/subj1/metrics');
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

    it('Badge Navigation', () => {
        cy.visit('/administrator/projects/proj1/badges/badge1/');
        cy.wait('@loadBadge');
        cy.get('[data-cy=breadcrumb-Badges]').click();
        cy.wait('@loadBadges');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');

        cy.visit('/administrator/projects/proj1/badges/badge1/users');
        cy.get('[data-cy=breadcrumb-badge1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

    it('Self Report', () => {
        cy.visit('/administrator/projects/proj1/self-report');
        cy.contains('No Skills Require Approval');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

    it('Skill Dependencies', () => {
        cy.visit('/administrator/projects/proj1/dependencies');
        cy.intercept('GET', '/admin/projects/proj1/dependency/graph').as('loadGraph');
        cy.wait('@loadGraph');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

    it('Cross Project', () => {
        cy.intercept('GET', '/admin/projects/proj1/shared').as('loadSharedSkills');
        cy.visit('/administrator/projects/proj1/cross%20Project');
        cy.wait('@loadSharedSkills');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

    it('Levels', () => {
        cy.intercept('GET', '/admin/projects/proj1/levels').as('loadLevels');
        cy.visit('/administrator/projects/proj1/levels');
        cy.wait('@loadLevels');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

    it('Users', () => {
        cy.intercept('GET', '/administrator/projects/proj1/users').as('loadUsers');
        cy.visit('/administrator/projects/proj1/users');
        cy.wait('@loadUsers');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    })

    it('Metrics', () => {
        cy.intercept('GET', '/admin/projects/proj1/metrics/distinctUsersOverTimeForProject').as('loadMetrics');
        cy.visit('/administrator/projects/proj1/metrics');
        cy.wait('@loadMetrics');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    })

    it('Issues', () => {
        cy.intercept('GET', '/admin/projects/proj1/errors').as('loadErrors');
        cy.visit('/administrator/projects/proj1/issues');
        cy.wait('@loadErrors');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

    it('Access', () => {
        cy.intercept('GET', '/admin/projects/proj1/userRoles').as('loadUserRoles');
        cy.visit('/administrator/projects/proj1/access');
        cy.wait('@loadUserRoles');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

    it('Settings', () => {
       cy.intercept('GET', '/admin/projects/proj1/settings').as('loadSettings');
        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@loadSettings');
        cy.get('[data-cy=breadcrumb-proj1]').click();
        cy.wait('@loadProject');
        cy.get('[data-cy=errorPage]').should('not.exist');
        cy.get('[data-cy=breadcrumb-Projects]').click();
        cy.get('[data-cy=errorPage]').should('not.exist');
    });

});
