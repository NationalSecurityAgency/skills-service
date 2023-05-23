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
import moment from 'moment';
import dayjs from 'dayjs';
import relativeTimePlugin from 'dayjs/plugin/relativeTime';

dayjs.extend(relativeTimePlugin);

const dateFormatter = value => moment.utc(value)
    .format('YYYY-MM-DD[T]HH:mm:ss[Z]');
const testTime = new Date().getTime();
const yesterday = new Date().getTime() - (1000 * 60 * 60 * 24);

describe('My Progress Breadcrumb Tests', () => {

    beforeEach(() => {
        cy.createProject(1);

        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSubject(1, 3);

        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.request('POST', `/admin/projects/proj1/skill4/prerequisite/proj1/skill2`);

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: yesterday
        });
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: testTime
        });

        cy.request('POST', `/api/projects/proj1/skills/skill3`, {
            userId: Cypress.env('proxyUser'),
            timestamp: yesterday
        });
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {
            userId: Cypress.env('proxyUser'),
            timestamp: testTime
        });

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.request('POST', '/admin/projects/proj1/badges/gemBadge', {
            projectId: 'proj1',
            badgeId: 'gemBadge',
            name: 'Gem Badge',
            startDate: dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 7),
            endDate: dateFormatter(new Date() + 1000 * 60 * 60 * 24 * 5),
        });

        cy.request('POST', '/admin/projects/proj1/badge/gemBadge/skills/skill1');

        cy.createProject(2);
        cy.enableProdMode(2);
        cy.createSubject(2, 1);
        cy.createSubject(2, 2);
        cy.createSubject(2, 3);

        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 4);
        cy.createSkill(2, 1, 5);
        cy.createSkill(2, 1, 6);

        cy.createSkill(2, 1, 1, { name: 'Shared skill 1' });

        // share skill1 from proj2 with proj1
        cy.request('POST', '/admin/projects/proj2/skills/skill1/shared/projects/proj1');

        // assigned proj2/skill1 as a dependency of proj1/skill3
        cy.request('POST', '/admin/projects/proj1/skill3/prerequisite/proj2/skill1');

        cy.loginAsRootUser();

        // create global badge as root user
        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1);
        cy.assignSkillToGlobalBadge(1, 2);
        cy.assignSkillToGlobalBadge(1, 3);
        cy.assignSkillToGlobalBadge(1, 4);

        cy.loginAsDefaultUser();
    });

    it('test breadcrumbs starting on Admin page', function () {
        cy.visit('/administrator');
        cy.get('[data-cy=inception-button]')
            .click();
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .contains('Dashboard Skills')
            .should('be.visible');

        cy.dashboardCd()
            .contains('Overall Points');
        cy.get('[data-cy="breadcrumb-Projects"]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains('Dashboard Skills')
            .should('be.visible');
    });

    it('test breadcrumbs starting on Project Overview page', function () {
        cy.visit('/administrator/skills?skillsClientDisplayPath=/');

        cy.dashboardCd()
            .contains('Overall Points');
        cy.get('[data-cy="breadcrumb-Projects"]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains('Dashboard Skills')
            .should('be.visible');
    });

    it('test breadcrumbs starting on Rank page', function () {
        cy.visit('/administrator/skills?skillsClientDisplayPath=/rank');

        cy.dashboardCd()
            .contains('My Rank');
        cy.get('[data-cy="breadcrumb-Projects"]')
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-Rank]')
            .should('exist');
        cy.get('[data-cy=breadcrumb-Rank]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Projects.*Dashboard Skills.*Rank.*$/))
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 3);
        cy.get('[data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Projects');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'Dashboard Skills');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Rank');

        // back to home page
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .click();
        cy.dashboardCd()
            .contains('Overall Points');
    });

    it('test breadcrumbs starting on Subject page', function () {
        cy.visit('/administrator/skills?skillsClientDisplayPath=/subjects/Dashboard');

        cy.dashboardCd()
            .contains('Dashboard');
        cy.get('[data-cy="breadcrumb-Projects"]')
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-Dashboard]')
            .should('exist');
        cy.get('[data-cy=breadcrumb-Dashboard]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Projects.*Dashboard Skills.*Subject:\s+Dashboard.*$/))
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 3);
        cy.get('[data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Projects');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'Dashboard Skills');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Subject: Dashboard');

        // back to home page
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .click();
        cy.dashboardCd()
            .contains('Overall Points');
    });

    it('test breadcrumbs starting on Skill page', function () {
        cy.visit('/administrator/skills?skillsClientDisplayPath=/subjects/Dashboard/skills/VisitDashboardSkills');

        cy.dashboardCd()
            .contains('Visit Dashboard Skills');
        cy.get('[data-cy="breadcrumb-Projects"]')
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-VisitDashboardSkills]')
            .should('exist');
        cy.get('[data-cy=breadcrumb-VisitDashboardSkills]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Dashboard]')
            .should('exist');
        cy.get('[data-cy=breadcrumb-Dashboard]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Projects.*Dashboard Skills.*Subject:\s+Dashboard.*Skill:\s+VisitDashboardSkills$/))
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Projects');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'Dashboard Skills');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Subject: Dashboard');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'Skill: VisitDashboardSkills');

        // back to home page
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .click();
        cy.dashboardCd()
            .contains('Overall Points');
    });

    it('test breadcrumbs starting on Skill page', function () {
        cy.visit('/administrator/skills?skillsClientDisplayPath=/subjects/Dashboard/skills/VisitDashboardSkills');

        cy.dashboardCd()
            .contains('Visit Dashboard Skill');
        cy.get('[data-cy="breadcrumb-Projects"]')
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-VisitDashboardSkills]')
            .should('exist');
        cy.get('[data-cy=breadcrumb-VisitDashboardSkills]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Dashboard]')
            .should('exist');
        cy.get('[data-cy=breadcrumb-Dashboard]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Projects.*Dashboard Skills.*Subject:\s+Dashboard.*Skill:\s+VisitDashboardSkills$/))
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Projects');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'Dashboard Skills');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Subject: Dashboard');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'Skill: VisitDashboardSkills');

        // back to home page
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .click();
        cy.dashboardCd()
            .contains('Overall Points');
    });

    it('test breadcrumbs starting on Subject Rank page', function () {
        cy.visit('/administrator/skills?skillsClientDisplayPath=/subjects/Dashboard/rank');

        cy.dashboardCd()
            .contains('My Rank');
        cy.get('[data-cy="breadcrumb-Projects"]')
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-Rank]')
            .should('exist');
        cy.get('[data-cy=breadcrumb-Rank]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Dashboard]')
            .should('exist');
        cy.get('[data-cy=breadcrumb-Dashboard]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Projects.*Dashboard Skills.*Subject:\s+Dashboard.*Rank$/))
            .should('be.visible');

        cy.get('[data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Projects');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'Dashboard Skills');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Subject: Dashboard');
        cy.get('[data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'Rank');

        // back to home page
        cy.get('[data-cy="breadcrumb-Dashboard Skills"]')
            .click();
        cy.dashboardCd()
            .contains('Overall Points');
    });

});

