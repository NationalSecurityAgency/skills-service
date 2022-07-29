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

const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');
const timeFromNowFormatter = (value) => dayjs(value).startOf('seconds').fromNow();

const testTime = new Date().getTime()
const yesterday = new Date().getTime() - (1000 * 60 * 60 * 24)

describe('My Progress Breadcrumb Tests', () => {

  beforeEach(() => {
    cy.intercept('GET', '/api/projects/proj1/pointHistory').as('pointHistoryChart');
    cy.intercept('/api/metrics/allProjectsSkillEventsOverTimeMetricsBuilder**').as('allSkillEventsForUser');

    cy.createProject(1);
    cy.enableProdMode(1);

    cy.createSubject(1, 1);
    cy.createSubject(1, 2);
    cy.createSubject(1, 3);

    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);
    cy.createSkill(1, 1, 4);
    cy.request('POST', `/admin/projects/proj1/skills/skill4/dependency/skill2`)


    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: yesterday
    })
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: Cypress.env('proxyUser'),
      timestamp: testTime
    })


    cy.request('POST', `/api/projects/proj1/skills/skill3`, {
      userId: Cypress.env('proxyUser'),
      timestamp: yesterday
    })
    cy.request('POST', `/api/projects/proj1/skills/skill3`, {
      userId: Cypress.env('proxyUser'),
      timestamp: testTime
    })


    cy.request('POST', '/admin/projects/proj1/badges/badge1', {
      projectId: 'proj1',
      badgeId: 'badge1',
      name: 'Badge 1',
    });
    cy.request('POST', `/admin/projects/proj1/badge/badge1/skills/skill1`)
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

    cy.request('POST', '/admin/projects/proj1/badge/gemBadge/skills/skill1')
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

    cy.createSkill(2, 1, 1, {name: 'Shared skill 1'});

    // share skill1 from proj2 with proj1
    cy.request('POST', '/admin/projects/proj2/skills/skill1/shared/projects/proj1');

    // assigned proj2/skill1 as a dependency of proj1/skill3
    cy.request('POST', '/admin/projects/proj1/skills/skill3/dependency/projects/proj2/skills/skill1');

    cy.loginAsRootUser();

    // create global badge as root user
    cy.createGlobalBadge(1)
    cy.assignSkillToGlobalBadge(1, 1)
    cy.assignSkillToGlobalBadge(1, 2)
    cy.assignSkillToGlobalBadge(1, 3)
    cy.assignSkillToGlobalBadge(1, 4)
    cy.enableGlobalBadge(1)

    cy.request('POST', '/admin/projects/Inception/settings/production.mode.enabled', {
      projectId: 'Inception',
      setting: 'production.mode.enabled',
      value: 'true'
    });

    cy.fixture('vars.json').then((vars) => {
      cy.request('POST', '/logout');
      cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
      cy.loginAsProxyUser()
    });
    cy.loginAsProxyUser()

    cy.addToMyProjects(1);
    cy.addToMyProjects(2);
  });

  it('test breadcrumbs starting on Progress and Rankings page', function () {
    cy.visit('/');
    cy.title().should('eq', 'SkillTree Dashboard - Progress And Rankings');

    cy.get('[data-cy=project-link-proj1]').click()
    cy.wait('@pointHistoryChart');
    cy.title().should('eq', 'SkillTree Dashboard - Progress And Rankings Project');

    cy.dashboardCd().contains('Overall Points');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');
    cy.get('[data-cy=breadcrumb-proj1]').should('be.visible');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains('Project: proj1').should('be.visible');
  });

  it('test breadcrumbs starting on Project Overview page', function () {
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/');

    cy.dashboardCd().contains('Overall Points');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');
    cy.get('[data-cy=breadcrumb-proj1]').should('be.visible');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains('Project: proj1').should('be.visible');
  });

  it('test breadcrumbs starting on Rank page', function () {
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/rank');

    cy.dashboardCd().contains('My Rank');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');

    cy.get('[data-cy=breadcrumb-Rank]').should('exist');
    cy.get('[data-cy=breadcrumb-Rank]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Rank.*$/)).should('be.visible');

    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 3);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Rank');

    // back to home page
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.dashboardCd().contains('Overall Points');
  });

  it('test breadcrumbs starting on Subject page', function () {
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/subjects/subj1');

    cy.dashboardCd().contains('Subject 1');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');

    cy.get('[data-cy=breadcrumb-subj1]').should('exist');
    cy.get('[data-cy=breadcrumb-subj1]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Subject:\s+subj1.*$/)).should('be.visible');

    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 3);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Subject: subj1');

    // back to home page
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.dashboardCd().contains('Overall Points');
  });

  it('test breadcrumbs starting on Skill page', function () {
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/subjects/subj1/skills/skill1');

    cy.dashboardCd().contains('Very Great Skill 1');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');

    cy.get('[data-cy=breadcrumb-skill1]').should('exist');
    cy.get('[data-cy=breadcrumb-skill1]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-subj1]').should('exist');
    cy.get('[data-cy=breadcrumb-subj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Subject:\s+subj1.*Skill:\s+skill1$/)).should('be.visible');

    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 4);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Subject: subj1');
    cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Skill: skill1');

    // back to home page
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.dashboardCd().contains('Overall Points');
  });

  it('test breadcrumbs starting on Skill page', function () {
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/subjects/subj1/skills/skill1');

    cy.dashboardCd().contains('Very Great Skill 1');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');

    cy.get('[data-cy=breadcrumb-skill1]').should('exist');
    cy.get('[data-cy=breadcrumb-skill1]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-subj1]').should('exist');
    cy.get('[data-cy=breadcrumb-subj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Subject:\s+subj1.*Skill:\s+skill1$/)).should('be.visible');

    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 4);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Subject: subj1');
    cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Skill: skill1');

    // back to home page
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.dashboardCd().contains('Overall Points');
  });

  it('test breadcrumbs starting on Subject Rank page', function () {
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/subjects/subj1/rank');

    cy.dashboardCd().contains('My Rank');
    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');

    cy.get('[data-cy=breadcrumb-Rank]').should('exist');
    cy.get('[data-cy=breadcrumb-Rank]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-subj1]').should('exist');
    cy.get('[data-cy=breadcrumb-subj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Subject:\s+subj1.*Rank$/)).should('be.visible');

    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 4);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Subject: subj1');
    cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Rank');

    // back to home page
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.dashboardCd().contains('Overall Points');
  });

  it('test breadcrumbs with internal dependency', function () {
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/subjects/subj1/skills/skill4/dependency/skill2');

    cy.get('[data-cy=breadcrumb-skill2]').should('exist');
    cy.get('[data-cy=breadcrumb-skill2]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-subj1]').should('exist');
    cy.get('[data-cy=breadcrumb-subj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Subject:\s+subj1.*Skill:\s+skill4.*Dependency:\s+skill2$/)).should('be.visible');

    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 5);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Subject: subj1');
    cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Skill: skill4');
    cy.get('[data-cy=breadcrumb-item]').eq(4).should('contain.text', 'Dependency: skill2');

    // back to skill page
    cy.get('[data-cy=breadcrumb-skill4]').click();
    cy.dashboardCd().contains('Very Great Skill 4');

    // back to subject page
    cy.get('[data-cy=breadcrumb-subj1]').click();
    cy.dashboardCd().contains('Subject 1');

    // back to home page
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.dashboardCd().contains('Overall Points');
  });

  it('test breadcrumbs with cross-project dependency', function () {
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/subjects/subj1/skills/skill3/crossProject/proj2/skill1');

    cy.get('[data-cy=breadcrumb-skill1]').should('exist');
    cy.get('[data-cy=breadcrumb-skill1]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-subj1]').should('exist');
    cy.get('[data-cy=breadcrumb-subj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Subject:\s+subj1.*Skill:\s+skill3.*Dependency:\s+skill1$/)).should('be.visible');

    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 5);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Subject: subj1');
    cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Skill: skill3');
    cy.get('[data-cy=breadcrumb-item]').eq(4).should('contain.text', 'Dependency: skill1');
    cy.dashboardCd().contains('cross-project skill');

    // back to skill page
    cy.get('[data-cy=breadcrumb-skill3]').click();
    cy.dashboardCd().contains('Very Great Skill 3');

    // back to subject page
    cy.get('[data-cy=breadcrumb-subj1]').click();
    cy.dashboardCd().contains('Subject 1');

    // back to home page
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.dashboardCd().contains('Overall Points');
  });

  it('test breadcrumbs with badge', function () {
    // Go to Badges page
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/badges');
    cy.dashboardCd().contains('Badges');
    cy.get('[data-cy=breadcrumb-Badges]').should('exist');
    cy.get('[data-cy=breadcrumb-Badges]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Badges.*$/)).should('be.visible');

    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 3);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Badges');

    // Go to regular badge page
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/badges/badge1');
    cy.dashboardCd().contains('Badge 1');
    cy.get('[data-cy=breadcrumb-badge1]').should('exist');
    cy.get('[data-cy=breadcrumb-badge1]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-Badges]').should('exist');
    cy.get('[data-cy=breadcrumb-Badges]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Badges.*Badge:\s+badge1$/)).should('be.visible');
    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 4);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Badges');
    cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Badge: badge1');

    // back to badges page
    cy.get('[data-cy=breadcrumb-Badges]').click();
    cy.dashboardCd().contains('Badges');

    // back to Overview page
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.dashboardCd().contains('Overall Points');
  });

  it('test breadcrumbs with global badge', function () {
    // Go to Badges page
    cy.visit('/progress-and-rankings/projects/proj1/?skillsClientDisplayPath=/badges/global/globalBadge1');
    cy.dashboardCd().contains('Global Badge 1');
    cy.get('[data-cy=breadcrumb-globalBadge1]').should('exist');
    cy.get('[data-cy=breadcrumb-globalBadge1]').should('not.have.attr', 'href');
    cy.get('[data-cy=breadcrumb-Badges]').should('exist');
    cy.get('[data-cy=breadcrumb-Badges]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-proj1]').should('exist');
    cy.get('[data-cy=breadcrumb-proj1]').should('have.attr', 'href');
    cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Progress And Rankings.*Project: proj1.*Badges.*Badge:\s+globalBadge1$/)).should('be.visible');

    cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 4);
    cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Progress And Rankings');
    cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Project: proj1');
    cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Badges');
    cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Badge: globalBadge1');

    // back to badges page
    cy.get('[data-cy=breadcrumb-Badges]').click();
    cy.dashboardCd().contains('Badges');

    // back to Overview page
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.dashboardCd().contains('Overall Points');
  });

});

