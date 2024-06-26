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
const timeFromNowFormatter = (value) => dayjs(value)
    .startOf('seconds')
    .fromNow();

const testTime = new Date().getTime();
const yesterday = new Date().getTime() - (1000 * 60 * 60 * 24);
describe('My Progress Breadcrumb Tests', () => {

    beforeEach(() => {
        cy.intercept('GET', '/api/projects/proj1/pointHistory*')
            .as('pointHistoryChart');
        cy.intercept('/api/metrics/allProjectsSkillEventsOverTimeMetricsBuilder**')
            .as('allSkillEventsForUser');

        cy.createProject(1);
        cy.enableProdMode(1);

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
            name: 'Badge 1',
        });
        cy.request('POST', `/admin/projects/proj1/badge/badge1/skills/skill1`);
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1',
            enabled: 'true',
        });

        cy.request('POST', '/admin/projects/proj1/badges/gemBadge', {
            projectId: 'proj1',
            badgeId: 'gemBadge',
            name: 'Gem Badge',
            startDate: dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 7),
            endDate: dateFormatter(new Date() + 1000 * 60 * 60 * 24 * 5),
        });

        cy.request('POST', '/admin/projects/proj1/badge/gemBadge/skills/skill1');
        cy.request('POST', '/admin/projects/proj1/badges/gemBadge', {
            projectId: 'proj1',
            badgeId: 'gemBadge',
            name: 'Gem Badge',
            startDate: dateFormatter(new Date() - 1000 * 60 * 60 * 24 * 7),
            endDate: dateFormatter(new Date() + 1000 * 60 * 60 * 24 * 5),
            enabled: 'true',
        });

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
        cy.enableGlobalBadge(1);

        cy.request('POST', '/admin/projects/Inception/settings/production.mode.enabled', {
            projectId: 'Inception',
            setting: 'production.mode.enabled',
            value: 'true'
        });

        cy.fixture('vars.json')
            .then((vars) => {
                cy.request('POST', '/logout');
                cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                cy.loginAsProxyUser();
            });
        cy.loginAsProxyUser();

        cy.addToMyProjects(1);
        cy.addToMyProjects(2);
    });

    it('test breadcrumbs starting on Progress and Rankings page', function () {
        cy.visit('/');

        cy.get('[data-cy=project-link-proj1]')
            .click();
        cy.wait('@pointHistoryChart');

        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy="breadcrumb-proj1"] [data-cy="breadcrumbItemLabel"]')
          .contains('Project:')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy="breadcrumb-proj1"] [data-cy="breadcrumbItemValue"]')
            .contains('proj1')
    });

    it('test breadcrumbs starting on Project Overview page', function () {
        cy.visit('/progress-and-rankings/projects/proj1/');

        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy="breadcrumb-proj1"] [data-cy="breadcrumbItemLabel"]')
          .contains('Project:')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy="breadcrumb-proj1"] [data-cy="breadcrumbItemValue"]')
          .contains('proj1')
    });

    it('test breadcrumbs starting on Rank page', function () {
        cy.visit('/progress-and-rankings/projects/proj1/rank');

        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('My Rank');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Rank]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Rank]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"]')
            .contains(new RegExp(/^Progress And Rankings.*Project:\s*proj1.*Rank.*$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 3);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Rank');

        // back to home page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
    });

    it('test breadcrumbs starting on Subject page', function () {
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Subject 1');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"]')
            .contains(new RegExp(/^Progress And Rankings.*Project:\s*proj1.*Subject:\s*subj1.*$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 3);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'subj1');

        // back to home page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
    });

    it('test breadcrumbs starting on Skill page', function () {
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');

        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Project:\s*proj1.*Subject:\s*subj1.*Skill:\s*skill1$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'subj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'skill1');

        // back to home page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
    });

    it('test breadcrumbs starting on Subject Rank page', function () {
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/rank');

        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('My Rank');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Rank]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Rank]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Project:\s*proj1.*Subject:\s*subj1.*Rank$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'subj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'Rank');

        // back to home page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
    });

    it('test breadcrumbs with cross-project dependency', function () {
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill3/crossProject/proj2/skill1');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Project:\s*proj1.*Subject:\s*subj1.*Skill:\s*skill3.*Prerequisite:\s*skill1$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 5);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'subj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'skill3');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(4)
            .should('contain.text', 'skill1');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('cross-project skill');

        // back to skill page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill3]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="depsProgress"]')
        cy.get('[data-cy="skillLink-proj2-skill1"]')

        // back to subject page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Subject 1');
        cy.get('[data-cy="pointHistoryChart-animationEnded"]')

        // back to home page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
        cy.get('[data-cy="pointHistoryChart-animationEnded"]')
    });

    it('test breadcrumbs with badge', function () {
        // Go to Badges page
        cy.visit('/progress-and-rankings/projects/proj1/badges');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Badges');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"]')
            .contains(new RegExp(/^Progress And Rankings.*Project:\s*proj1.*Badges.*$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 3);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Badges');

        // Go to regular badge page
        cy.visit('/progress-and-rankings/projects/proj1/badges/badge1');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Badge 1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-badge1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-badge1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Project:\s*proj1.*Badges.*Badge:\s*badge1$/))
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Badges');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'badge1');

        // back to badges page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Badges');

        // back to Overview page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
    });

    it('test breadcrumbs with global badge', function () {
        // Go to Badges page
        cy.visit('/progress-and-rankings/projects/proj1/badges/global/globalBadge1');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Global Badge 1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-globalBadge1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-globalBadge1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Project:\s*proj1.*Badges.*Badge:\s*globalBadge1$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Badges');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'globalBadge1');

        // back to badges page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Badges');

        // back to Overview page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
    });

    it('test breadcrumbs with custom labels', function () {
        // log in as project admin and set custom labels
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });
        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'Work Role',
                setting: 'project.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Competency',
                setting: 'subject.displayName',
                projectId: 'proj1',
            },
            {
                value: 'KSA',
                setting: 'group.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Course',
                setting: 'skill.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Stage',
                setting: 'level.displayName',
                projectId: 'proj1',
            },
        ]);

        // log back in as the proxy user to test client display breadcrumbs
        cy.fixture('vars.json')
            .then((vars) => {
                cy.request('POST', '/logout');
                cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                cy.loginAsProxyUser();
            });

        // progress and rankings page
        cy.visit('/');
        cy.get('[data-cy=project-link-proj1]')
            .click();
        cy.wait('@pointHistoryChart');

        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] ')
            .contains('proj1')
            .should('be.visible');

        cy.visit('/progress-and-rankings/projects/proj1/rank');

        // overall rank page
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('My Rank');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Rank]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Rank]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Work Role:\s*proj1.*Rank.*$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 3);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Rank');

        // subject page
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Subject 1');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] ')
            .contains(new RegExp(/^Progress And Rankings.*Work Role:\s*proj1.*Competency:\s*subj1.*$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 3);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'subj1');

        // subject rank page
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/rank');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('My Rank');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Rank]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Rank]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Work Role:\s*proj1.*Competency:\s*subj1.*Rank$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'subj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'Rank');

        // skill page
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Work Role:\s*proj1.*Competency:\s*subj1.*Course:\s*skill1$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'subj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'skill1');

        // cross project dependency
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill3/crossProject/proj2/skill1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"]')
            .contains(new RegExp(/^Progress And Rankings.*Work Role:\s*proj1.*Competency:\s*subj1.*Course:\s*skill3.*Prerequisite:\s*skill1$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 5);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'subj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'skill3');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(4)
            .should('contain.text', 'skill1');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Cross-Work Role Course');

        // internal dependency
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill2');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill2]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill2]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Work Role:\s*proj1.*Competency:\s*subj1.*Course:\s*skill2.*$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'subj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'skill2');

        // Go to Badges page
        cy.visit('/progress-and-rankings/projects/proj1/badges');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Badges');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Work Role:\s*proj1.*Badges.*$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 3);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Badges');

        // Go to regular badge page
        cy.visit('/progress-and-rankings/projects/proj1/badges/badge1');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Badge 1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-badge1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-badge1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Work Role:\s*proj1.*Badges.*Badge:\s*badge1$/))
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Badges');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'badge1');

        // global badge page
        cy.visit('/progress-and-rankings/projects/proj1/badges/global/globalBadge1');
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Global Badge 1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-globalBadge1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-globalBadge1]')
            .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-Badges]')
            .should('have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
            .should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]')
            .contains(new RegExp(/^Progress And Rankings.*Work Role:\s*proj1.*Badges.*Badge:\s*globalBadge1$/))
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .its('length')
            .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(0)
            .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(1)
            .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(2)
            .should('contain.text', 'Badges');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
            .eq(3)
            .should('contain.text', 'globalBadge1');

    });

    it('navigate through and validate breadcrumbs', function () {

        cy.visit('/');

        cy.get('[data-cy=project-link-proj1]')
          .click();
        cy.wait('@pointHistoryChart');

        cy.get('[data-cy="skillsDisplayHome"]')
          .contains('Overall Points');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
          .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
          .should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
          .should('exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
          .should('not.have.attr', 'href');
        cy.get('[data-cy="breadcrumb-bar"]')
          .contains('proj1')
          .should('be.visible');

        // click on subject
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTileBtn"]').first()
          .click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy=title]')
          .contains('Subject 1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .its('length')
          .should('eq', 3);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(0)
          .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(1)
          .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(2)
          .should('contain.text', 'subj1');

        // click on skill
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillProgress_index-0"] [data-cy="skillProgressBar"]')
          .click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy=title]')
          .contains('Skill Overview');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .its('length')
          .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(0)
          .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(1)
          .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(2)
          .should('contain.text', 'subj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(3)
          .should('contain.text', 'skill1');

        // back to subject page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]')
          .click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy=title]')
          .contains('Subject 1');
        cy.get('[data-cy="skillProgressTitle-skill1"]')
        cy.get('[data-cy="myRank"] [data-cy="myRankPosition"]').should('have.text', '1')

        // click on rank
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="myRankBtn"] button')
          .should('be.visible')
          .should('be.enabled')
          .click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy=title]')
          .contains('My Rank');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .its('length')
          .should('eq', 4);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(0)
          .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(1)
          .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(2)
          .should('contain.text', 'subj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(3)
          .should('contain.text', 'Rank');


        // back to home page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
          .click();
        cy.get('[data-cy="skillsDisplayHome"]')
          .contains('Overall Points');
        cy.get('[data-cy="myRank"] [data-cy="myRankPosition"]').should('have.text', '1')
        // click on rank
        cy.get('[data-cy="skillsDisplayHome"] [data-cy=myRankBtn]')
          .click();
        cy.get('[data-cy="skillsDisplayHome"] [data-cy=title]')
          .contains('My Rank');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .its('length')
          .should('eq', 3);
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(0)
          .should('contain.text', 'Progress And Rankings');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(1)
          .should('contain.text', 'proj1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
          .eq(2)
          .should('contain.text', 'Rank');

        // back to home page
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]')
          .click();
        cy.get('[data-cy="skillsDisplayHome"]')
          .contains('Overall Points');

        // // click on my badges
        // cy.get('[data-cy="skillsDisplayHome"] [data-cy=myBadgesBtn]')
        //   .click();
        // cy.get('[data-cy="skillsDisplayHome"] [data-cy=title]')
        //   .contains('My Badges');
        // cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
        //   .its('length')
        //   .should('eq', 3);
        // cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
        //   .eq(0)
        //   .should('contain.text', 'Progress And Rankings');
        // cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
        //   .eq(1)
        //   .should('contain.text', 'proj1');
        // cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
        //   .eq(2)
        //   .should('contain.text', 'Badges');
        //
        // // click on badge
        // cy.get('[data-cy="skillsDisplayHome"] [data-cy=earnedBadgeLink_badge1]')
        //   .click();
        // cy.get('[data-cy="skillsDisplayHome"] [data-cy=title]')
        //   .contains('Badge Details');
        // cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
        //   .its('length')
        //   .should('eq', 4);
        // cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
        //   .eq(0)
        //   .should('contain.text', 'Progress And Rankings');
        // cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
        //   .eq(1)
        //   .should('contain.text', 'proj1');
        // cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
        //   .eq(2)
        //   .should('contain.text', 'Badges');
        // cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-item]')
        //   .eq(3)
        //   .should('contain.text', 'badge1');
    });

});

