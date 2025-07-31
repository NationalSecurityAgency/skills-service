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
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import utc from 'dayjs/plugin/utc';

dayjs.extend(relativeTimePlugin);
dayjs.extend(utc);

const dateFormatter = value => dayjs(value)
    .utc()
    .format('YYYY-MM-DD[T]HH:mm:ss[Z]');
const timeFromNowFormatter = (value) => dayjs(value)
    .startOf('seconds')
    .fromNow();

const testTime = new Date().getTime();
const yesterday = new Date().getTime() - (1000 * 60 * 60 * 24);

describe('My Progress Tests', () => {

    beforeEach(() => {
        cy.log(`--------> ${testTime}`);
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
        cy.addLearningPathItem(1, 2, 4)

        const badge1 = {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1',
            enabled: 'true',
            iconClass: 'fa fa-question-circle',
        };
        cy.request('POST', '/admin/projects/proj1/badges/badge1', badge1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.enableBadge(1, 1, badge1);

        const gemBadge = {
            projectId: 'proj1',
            badgeId: 'gemBadge',
            name: 'Gem Badge',
            enabled: 'true',
            iconClass: 'mi mi-ac-unit',
            startDate: dateFormatter(dayjs()
                .subtract(5, 'day')),
            endDate: dateFormatter(dayjs()
                .add(7, 'day')),
        };
        cy.request('POST', '/admin/projects/proj1/badges/gemBadge', gemBadge);
        cy.request('POST', `/admin/projects/proj1/badge/gemBadge/skills/skill4`);
        cy.enableBadge(1, 'gem', gemBadge);

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

        cy.loginAsRootUser();
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

    it('visit My Progress page', function () {

        cy.loginAsRootUser();
        cy.request('PUT', `/app/badges/globalBadge1`, {
            badgeId: `globalBadge1`,
            isEdit: false,
            name: `Global Badge 1`,
            originalBadgeId: '',
            iconClass: 'mi mi-ac-unit',
            enabled: true,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
        });
        cy.assignSkillToGlobalBadge(1, 2);
        cy.enableGlobalBadge();

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .contains('Progress And Rankings')
            .should('be.visible');

        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=numProjectsAvailable]')
            .contains(new RegExp(/^\/ 2$/));
        cy.get('[data-cy=info-snap-footer]')
            .contains('You still have 1 project to explore.');

        cy.get('[data-cy=numAchievedSkills]')
            .contains(new RegExp(/^2$/));
        cy.get('[data-cy=numSkillsAvailable]').should('have.text', 10)
        cy.get('[data-cy="quizzes-card-footer"]')
            .contains('Explore quiz and survey history');

        cy.get('[data-cy=mostRecentAchievedSkill]')
            .contains(`Last Achieved skill${timeFromNowFormatter(testTime)}`);
        cy.get('[data-cy=numAchievedSkillsLastWeek]')
            .contains('2 skills in the last week');
        cy.get('[data-cy=numAchievedSkillsLastMonth]')
            .contains('2 skills in the last month');
        cy.get('[data-cy=last-earned-footer]')
            .contains('Keep up the good work!!');

        cy.get('[data-cy=badges-num-footer]')
            .contains('Be proud to earn those badges!!');
        cy.get('[data-cy=numAchievedBadges]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=numBadgesAvailable]')
            .contains(new RegExp(/^\/ 3$/));
        cy.get('[data-cy=numAchievedGlobalBadges]')
            .contains('Global Badges: 0');
        cy.get('[data-cy=numAchievedGemBadges]')
            .contains('Gems: 0');

        cy.get('[data-cy=project-link-card-proj2]')
            .should('be.visible');
        cy.get('[data-cy=project-link-card-proj2]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 2');

        cy.get('[data-cy=project-link-card-proj1]')
            .should('be.visible');
        cy.get('[data-cy=project-link-card-proj1]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 1');
        cy.get('[data-cy=project-link-card-proj1]')
            .find('[data-cy=project-card-project-level]')
            .contains('3');
        cy.get('[data-cy=project-link-card-proj1] [data-cy=project-card-project-rank]')
            .contains('Rank: 1 / 1');
        cy.get('[data-cy=project-link-card-proj1] [data-cy=project-card-project-points]')
            .contains('400 / 800');

        cy.get('[data-cy=inception-button]')
            .should('not.exist');

        cy.get('[data-cy=project-link-proj1]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .contains('Overall Points');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy=skillsTitle]')
            .contains('Project: This is project 1')
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-projects]')
            .should('not.exist');

        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .click();
        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=viewBadges]')
            .click();
        cy.get('[data-cy=earnedBadgeLink_badge1]')
            .should('be.visible');

        cy.get('[data-cy="achievedBadge-badge1"] [data-cy="badgeName"]')
            .should( 'have.text', 'Badge 1');
        cy.get('[data-cy="availableBadges"] [data-cy="badgeTitle"]')
            .eq(0)
            .contains('Gem Badge');
        cy.get('[data-cy="availableBadges"] [data-cy="badgeTitle"]')
            .eq(1)
            .contains('Global Badge 1');

        cy.get('[data-cy=badgeDetailsLink_globalBadge1]')
            .click();
        cy.get('[data-cy=breadcrumb-globalBadge1]')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]')
            .contains('Global Badge Details');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="badgeTitle"]')
            .contains('Global Badge 1')
            .should('be.visible');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillProgressTitle-skill2=globalBadge1"]')
            .contains('Project: This is project 1')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillProgressTitle-skill2=globalBadge1"] [data-cy=skillProgressTitle]')
            .click();
        cy.get('[data-cy="skillsDisplayHome"]')
            .find('[data-cy=title]')
            .contains('Skill Overview');
        cy.get('[data-cy="skillsDisplayHome"]')
            .find('[data-cy=skillProgressTitle]')
            .contains('Very Great Skill 2');
        cy.get('[data-cy="skillsDisplayHome"]')
            .find('[data-cy=overallPointsEarnedCard]')
            .should('be.visible');
        cy.get('[data-cy="breadcrumb-Progress And Rankings"]')
            .click();

        cy.get('[data-cy=viewBadges]')
            .click();
        cy.get('[data-cy=earnedBadgeLink_badge1]')
            .click();
        cy.get('[data-cy=breadcrumb-badge1]')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-proj1]')
            .should('be.visible');
        cy.get('[data-cy="skillsDisplayHome"]')
            .find('.skills-title')
            .contains('Badge Details');
         cy.get('[data-cy="skillsDisplayHome"] [data-cy="badge_badge1"]')
            .contains('Badge 1')
            .should('be.visible');
         cy.get('[data-cy="skillsDisplayHome"]')
            .find('[data-cy=skillProgressTitle]')
            .contains('Very Great Skill 1')
            .click();
         cy.get('[data-cy="skillsDisplayHome"]')
            .find('[data-cy=title]')
            .contains('Skill Overview');
         cy.get('[data-cy="skillsDisplayHome"]')
            .find('[data-cy=skillProgressTitle]')
            .contains('Very Great Skill 1');
         cy.get('[data-cy="skillsDisplayHome"]')
            .find('[data-cy=overallPointsEarnedCard]')
            .should('be.visible');

        cy.loginAsRootUser();
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {
            userId: Cypress.env('proxyUser'),
            timestamp: yesterday
        });
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {
            userId: Cypress.env('proxyUser'),
            timestamp: testTime
        });

        cy.request('POST', `/api/projects/proj1/skills/skill4`, {
            userId: Cypress.env('proxyUser'),
            timestamp: yesterday
        });
        cy.request('POST', `/api/projects/proj1/skills/skill4`, {
            userId: Cypress.env('proxyUser'),
            timestamp: testTime
        });

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');
        cy.get('[data-cy=viewBadges]')
            .click();
        cy.get('[data-cy=badge-catalog_no-badges]')
            .should('be.visible');

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="overallPoints"]')
            .contains('Overall Points');
         cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]')
            .contains('Project: This is project 1')
    });

    it('My Progress page - contributed to all projects', function () {
        // // add a skill to Inception to have contributed to all projects
        cy.loginAsRootUser();
        cy.request('POST', `/api/projects/proj2/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime()
        });

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^2$/));
        cy.get('[data-cy=numProjectsAvailable]')
            .contains(new RegExp(/^\/ 2$/));
        cy.get('[data-cy=info-snap-footer]')
            .contains('Great job, you have contributed to all projects!');
    });

    it('My Progress page - not contributed to more than one project', function () {
        cy.createProject(3);
        cy.enableProdMode(3);
        cy.addToMyProjects(3);

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=numProjectsAvailable]')
            .contains(new RegExp(/^\/ 3$/));
        cy.get('[data-cy=info-snap-footer]')
            .contains('You still have 2 projects to explore.');
    });

    it('mySkills page - projects that are not added to My Projects are not included', function () {
        cy.createProject(3);
        cy.enableProdMode(3);

        cy.createSubject(3, 1);
        cy.createSubject(3, 2);

        cy.createSkill(3, 1, 1);
        cy.createSkill(3, 1, 2);
        cy.createSkill(3, 1, 3);
        cy.createSkill(3, 1, 4);

        cy.createBadge(3, 1);

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        cy.get('[data-cy=numProjectsContributed]')
            .contains(new RegExp(/^1$/));
        cy.get('[data-cy=numProjectsAvailable]')
            .contains(new RegExp(/^\/ 2$/));
        cy.get('[data-cy=info-snap-footer]')
            .contains('You still have 1 project to explore.');

        cy.get('[data-cy=numSkillsAvailable]').should('have.text', '10')
        cy.get('[data-cy=numBadgesAvailable]')
            .contains(new RegExp(/^\/ 2$/));

        cy.get('[data-cy=project-link-card-proj1]')
            .should('be.visible');
        cy.get('[data-cy=project-link-card-proj1]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 1');

        cy.get('[data-cy=project-link-card-proj2]')
            .should('be.visible');
        cy.get('[data-cy=project-link-card-proj2]')
            .find('[data-cy=project-card-project-name]')
            .contains('This is project 2');

        cy.get('[data-cy=project-link-card-proj3]')
            .should('not.exist');
    });

    it('no projects added to My Projects', function () {
        cy.removeFromMyProjects(1);
        cy.removeFromMyProjects(2);
        cy.visit('/progress-and-rankings');

        cy.get('[data-cy="manageMyProjsBtnInNoContent"]')
            .click();
        cy.get('[data-cy="backToProgressAndRankingBtn"]');

        cy.visit('/progress-and-rankings');
        cy.get('[data-cy="manageMyProjsBtnInNoContent"]')
            .click();
        cy.get('[data-cy="backToProgressAndRankingBtn"]');
    });

    it('change sort order using keyboard', function () {
        cy.createProject(3);
        cy.enableProdMode(3);
        cy.addToMyProjects(3);

        cy.viewport(1200, 1000);

        cy.visit('/progress-and-rankings');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 2', 'This is project 1']);

        // move down
        cy.get('[data-cy="project-link-proj3"] button')
          .should('be.visible')
          .tab({ shift: true })
          .tab({ shift: true })
          .type('{downArrow}')
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 3', 'This is project 1']);
        cy.get('[data-cy="project-link-card-proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // move down
        cy.wait(1000)
        cy.get('[data-cy="project-link-proj3"] button')
            .tab({ shift: true })
            .tab({ shift: true })
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);
        cy.get('[data-cy="project-link-card-proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // move down - last item already; no action
        cy.wait(1000)
        cy.get('[data-cy="project-link-proj3"] button')
            .tab({ shift: true })
            .tab({ shift: true })
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);
        cy.get('[data-cy="project-link-card-proj3"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        cy.visit('/progress-and-rankings');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);
        cy.get('[data-cy="project-link-card-proj3"] [data-cy="sortControlHandle"]')
            .should('not.have.focus');

        // move up
        cy.wait(1000)
        cy.get('[data-cy="project-link-proj1"] button')
            .tab({ shift: true })
            .tab({ shift: true })
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 1', 'This is project 2', 'This is project 3']);
        cy.get('[data-cy="project-link-card-proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');

        // move up; first item already - no action
        cy.wait(1000)
        cy.get('[data-cy="project-link-proj1"] button')
            .tab({ shift: true })
            .tab({ shift: true })
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 1', 'This is project 2', 'This is project 3']);
        cy.get('[data-cy="project-link-card-proj1"] [data-cy="sortControlHandle"]')
            .should('have.focus');
    });

    it('sort my projects', function () {
        cy.createProject(3);
        cy.enableProdMode(3);
        cy.addToMyProjects(3);

        cy.viewport(1200, 1000);

        cy.visit('/progress-and-rankings');

        const proj1Selector = '[data-cy=project-link-card-proj1] [data-cy="sortControlHandle"]';
        const proj2Selector = '[data-cy=project-link-card-proj2] [data-cy="sortControlHandle"]';
        const proj3Selector = '[data-cy=project-link-card-proj3] [data-cy="sortControlHandle"]';

        cy.intercept('/api/myprojects/proj1')
            .as('updateMyProj1');
        cy.intercept('/api/myprojects/proj2')
            .as('updateMyProj2');
        cy.intercept('/api/myprojects/proj3')
            .as('updateMyProj3');

        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 2', 'This is project 1']);
        cy.get(proj2Selector)
            .dragAndDrop(proj1Selector);
        cy.wait('@updateMyProj2');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 1', 'This is project 2']);

        // refresh and make sure that sort order is still the same
        cy.visit('/progress-and-rankings');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 1', 'This is project 2']);

        cy.get(proj3Selector)
            .dragAndDrop(proj2Selector);
        cy.wait('@updateMyProj3');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 1', 'This is project 2', 'This is project 3']);

        cy.get(proj1Selector)
            .dragAndDrop(proj2Selector);
        cy.wait('@updateMyProj1');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);

        // navigate to My Projects and then return
        cy.wait(2000)
        cy.get('[data-cy="manageMyProjsBtn"]')
            .click();
        cy.get('[data-cy="backToProgressAndRankingBtn"]')
            .click();
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1', 'This is project 3']);
    });

    it('My Progress page - verify custom level on ProjectLinkCard', function () {
        // set custom level display name for proj1
        cy.loginAsRootUser();
        cy.request('POST', '/admin/projects/proj1/settings', [{
            value: 'Stage',
            setting: 'level.displayName',
            projectId: 'proj1',
        }]);

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        // proj1 has custom level name ("Stage")
        cy.get('[data-cy=project-link-card-proj1]')
            .find('[data-cy=project-card-project-level]')
            .contains('Stage');
        cy.get('[data-cy=project-link-card-proj1]')
            .find('[data-cy=project-card-project-level]')
            .contains('Level')
            .should('not.exist');

        // proj2 has default level name ("Level")
        cy.get('[data-cy=project-link-card-proj2]')
            .find('[data-cy=project-card-project-level]')
            .contains('Level');
    });

    it('verify progress on ProjectLinkCard', function () {
        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');

        // proj 2 has no progress
        const proj2Selector = '[data-cy="project-link-card-proj2"]'
        cy.get(`${proj2Selector} [data-cy="project-card-project-level"]`).should('have.text', 'Level 0')
        cy.get(`${proj2Selector} [data-cy="project-card-project-points"]`).should('have.text', '0 / 1,200')
        cy.get(`${proj2Selector} .apexcharts-text.apexcharts-datalabel-value`).should('have.text', '0%')

        const proj1Selector = '[data-cy="project-link-card-proj1"]'
        cy.get(`${proj1Selector} [data-cy="project-card-project-level"]`).should('have.text', 'Level 3')
        cy.get(`${proj1Selector} [data-cy="project-card-project-points"]`).should('have.text', '400 / 800')
        cy.get(`${proj1Selector} .apexcharts-text.apexcharts-datalabel-value`).should('have.text', '50%')
    });

    it('My Progress page - verify custom project label', function () {
        // set custom level display name for proj1
        cy.loginAsRootUser();
        cy.request('POST', '/admin/projects/proj1/settings', [{
            value: 'Work Role',
            setting: 'project.displayName',
            projectId: 'proj1',
        }]);

        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');
        cy.get('[data-cy=project-link-proj1]')
            .click();

        // proj1 has custom level name ("Work Role")
        cy.get('[data-cy="skillsDisplayHome"]')
            .find('[data-cy=title]')
            .contains('Work Role: This is project 1');
    });

    it('Contact project owner', () => {
        cy.intercept('POST', '/api/projects/*/contact').as('contact');
        cy.intercept('POST', '/api/validation/description*').as('validate');

        const invalidMsg = new Array(3001).fill('a').join('');
        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/');
        cy.get('[data-cy=project-link-proj1]').click();
        cy.get('[data-cy="contactOwnerBtn"]').should('be.visible').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('exist');
        cy.get('[aria-label="Close"]').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');
        cy.get('[data-cy="contactOwnerBtn"]').should('be.visible').click();
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');

        cy.get('[data-cy="contactOwnerBtn"]').should('be.visible').click();
        cy.get('[data-p="modal"]').contains('Send your message to the administrators of This is project 1 training')
        cy.get('[data-cy="saveDialogBtn"]').should('contain.text', 'Submit');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="messageNumCharsRemaining"]').should('contain.text', '2,500 characters remaining');
        cy.get('[data-cy="contactOwnersMsgInput"]').click().fill(invalidMsg);
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="messageNumCharsRemaining"]').should('contain.text', '-500 characters remaining');

        cy.get('[data-cy="contactOwnersMsgInput"]').click().fill('message message jabberwocky jabberwocky message message');
        cy.wait('@validate');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="messageError"]').should('be.visible');
        cy.get('[data-cy="messageError"]').should('contain.text', 'paragraphs may not contain jabberwocky');
        cy.get('[data-cy="contactOwnersMsgInput"]').click().fill('aaa bbb this is a message');
        cy.get('[data-cy="messageNumCharsRemaining"]').should('contain.text', '2,475 characters remaining');
        cy.clickSaveDialogBtn()
        cy.wait('@contact');
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist');
        cy.get('[data-cy="closeDialogBtn"]').should('contain.text', 'OK');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'Message sent!');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'The Project Administrator(s) of This is project 1 will be notified of your question via email.');
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.wait(500); //wait for animations to complete
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');
    });

    it('do not show contact project admins button if email service is diabled', () => {
        cy.intercept('/public/isFeatureSupported?feature=emailservice', 'false').as('isEmailServiceSupported');
        cy.visit('/progress-and-rankings/projects/proj1');
        cy.wait('@isEmailServiceSupported')
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="pointHistoryChartWithData"]')
        cy.wait(3000)
        cy.get('[data-cy="contactOwnerBtn"]').should('not.exist');
    })

    it('Send email to project owner', () => {
        cy.intercept('POST', '/api/projects/*/contact').as('contact');
        cy.intercept('POST', '/api/validation/description*').as('validate');
        cy.loginAsAdminUser();
        cy.createProject(3);
        cy.enableProdMode(3);
        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.get('[data-cy="contactOwnerBtn_proj3"]').should('be.visible').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('exist');
        cy.get('[data-cy="contactOwnersMsgInput"]').click().fill('aaa bbb this is a message');
        cy.get('[data-cy="messageNumCharsRemaining"]').should('contain.text', '2,475 characters remaining');
        cy.clickSaveDialogBtn()
        cy.wait('@contact');
        cy.get('[data-cy="closeDialogBtn"]').should('contain.text', 'OK');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'Message sent!');
        cy.get('[data-cy="contactOwnerSuccessMsg"]').should('contain.text', 'The Project Administrator(s) of This is project 3 will be notified of your question via email.');
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="contactProjectOwnerDialog"]').should('not.exist');
        cy.getEmails().then((emails) => {
                expect(emails[0].html).to.contain('aaa bbb this is a message');
        });
    });

    it('remove project from My Progress view', function () {
        cy.createProject(3);
        cy.enableProdMode(3);
        cy.addToMyProjects(3);

        cy.viewport(1200, 1000);

        cy.visit('/progress-and-rankings');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 2', 'This is project 1']);

        cy.get('[data-cy="remove-proj3Btn"]').click()
        cy.get('[data-pc-name="pcacceptbutton"]').click()

        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1']);
        cy.get('[data-cy="project-link-card-proj3"]').should('not.exist');

        cy.get('[data-cy="remove-proj1Btn"]').click()
        cy.get('[data-pc-name="pcacceptbutton"]').click()

        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2']);
        cy.get('[data-cy="project-link-card-proj1"]').should('not.exist');

        cy.get('[data-cy="remove-proj2Btn"]').click()
        cy.get('[data-pc-name="pcacceptbutton"]').click()

        cy.get('[data-cy="project-link-card-proj2"]').should('not.exist');
        cy.get('[data-cy="manageMyProjsBtnInNoContent"]').should('exist');
    });

    it('remove project from My Progress view updates info card correctly', function () {
        cy.createProject(3);
        cy.enableProdMode(3);
        cy.addToMyProjects(3);

        cy.viewport(1200, 1000);

        cy.visit('/progress-and-rankings');
        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 3', 'This is project 2', 'This is project 1']);

        cy.get('[data-cy="numProjectsContributed"]').contains('1');
        cy.get('[data-cy="numProjectsAvailable"]').contains('3');
        cy.get('.apexcharts-datalabels-group > .apexcharts-datalabel-value').contains('33');

        cy.get('[data-cy="remove-proj3Btn"]').click()
        cy.get('[data-pc-name="pcacceptbutton"]').click()

        cy.get('[data-cy="numProjectsContributed"]').contains('1');
        cy.get('[data-cy="numProjectsAvailable"]').contains('2');
        cy.get('.apexcharts-datalabels-group > .apexcharts-datalabel-value').contains('50');

        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2', 'This is project 1']);
        cy.get('[data-cy="project-link-card-proj3"]').should('not.exist');

        cy.get('[data-cy="remove-proj1Btn"]').click()
        cy.get('[data-pc-name="pcacceptbutton"]').click()

        cy.validateElementsOrder('[data-cy="project-card-project-name"]', ['This is project 2']);
        cy.get('[data-cy="project-link-card-proj1"]').should('not.exist');

        cy.get('[data-cy="numProjectsContributed"]').contains('0');
        cy.get('[data-cy="numProjectsAvailable"]').contains('1');
        cy.get('.apexcharts-datalabels-group > .apexcharts-datalabel-value').contains('0');
    });

    if (!Cypress.env('oauthMode')) {
        it('Progress view is refreshed on login', function() {
            const user2 = 'user2@skills.org'

            const user3 = 'user3@skills.org'
            const pass = 'password';

            cy.reportSkill(1, 1, user3, 'now');

            cy.register(user2, pass);
            cy.register(user3, pass);
            cy.login(user3, pass);

            cy.addToMyProjects(1);

            cy.loginAsDefaultUser();
            cy.visit('/progress-and-rankings');
            cy.get('[data-cy="project-link-proj2"]')
            cy.get('[data-cy="project-link-proj1"]')
            cy.get('[data-cy="numAchievedBadges"]').should('have.text', '1')
            cy.get('[data-cy="numProjectsContributed"]').should('have.text', '1')
            cy.get('[data-cy="numProjectsAvailable"]').should('have.text', '/ 2')

            cy.get('[data-cy="settings-button"] button').click();
            cy.contains('Log Out').click();
            cy.get('#username').type(user2);
            cy.get('#inputPassword').type(pass);
            cy.get('[data-cy=login]').click();

            cy.get('[data-cy="manageMyProjsBtnInNoContent"]')
            cy.get('[data-cy="project-link-proj2"]').should('not.exist')
            cy.get('[data-cy="numAchievedBadges"]').should('not.exist')

            cy.get('[data-cy="settings-button"] button').click();
            cy.contains('Log Out').click();
            cy.get('#username').type(user3);
            cy.get('#inputPassword').type(pass);
            cy.get('[data-cy=login]').click();

            cy.get('[data-cy="project-link-proj1"]')
            cy.get('[data-cy="project-link-proj2"]').should('not.exist')
            cy.get('[data-cy="numAchievedBadges"]').should('have.text', '0')
            cy.get('[data-cy="numProjectsContributed"]').should('have.text', '1')
            cy.get('[data-cy="numProjectsAvailable"]').should('have.text', '/ 1')
        })
    }

    it('custom subject icons are loaded for multiple projects', function () {
        cy.intercept(' /api/projects/proj10/customIconCss').as('proj1CustomIcons')
        cy.intercept(' /api/projects/proj20/customIconCss').as('proj2CustomIcons')

        cy.createProject(10);
        cy.createProject(20);

        cy.enableProdMode(10);
        cy.enableProdMode(20);

        cy.addToMyProjects(10);
        cy.addToMyProjects(20);

        cy.uploadCustomIcon('valid_icon.png', '/admin/projects/proj10/icons/upload')
        cy.uploadCustomIcon('anothervalid_icon.png', '/admin/projects/proj20/icons/upload')

        cy.createSubject(10, 1, { iconClass: 'proj10-validiconpng' })
        cy.createSubject(20, 1, { iconClass: 'proj20-anothervalidiconpng' })

        cy.visit('/progress-and-rankings/');
        cy.get('[data-cy="project-link-proj10"]').click()
        cy.wait('@proj1CustomIcons')
        cy.wait(1000)
        cy.get('[data-cy="subjectTile-subj1"] .proj10-validiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })

        cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click()
        cy.get('[data-cy="project-link-proj20"]').click()
        cy.wait('@proj2CustomIcons')
        cy.wait(1000)
        cy.get('[data-cy="subjectTile-subj1"] .proj20-anothervalidiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })
    });

    it('custom subject icons are loaded by going directly to a project', function () {
        cy.intercept(' /api/projects/proj10/customIconCss').as('proj1CustomIcons')
        cy.intercept(' /api/projects/proj20/customIconCss').as('proj2CustomIcons')

        cy.createProject(10);
        cy.createProject(20);

        cy.enableProdMode(10);
        cy.enableProdMode(20);

        cy.addToMyProjects(10);
        cy.addToMyProjects(20);

        cy.uploadCustomIcon('valid_icon.png', '/admin/projects/proj10/icons/upload')
        cy.uploadCustomIcon('anothervalid_icon.png', '/admin/projects/proj20/icons/upload')

        cy.createSubject(10, 1, { iconClass: 'proj10-validiconpng' })
        cy.createSubject(20, 1, { iconClass: 'proj20-anothervalidiconpng' })

        cy.visit('/progress-and-rankings/projects/proj10');
        cy.wait('@proj1CustomIcons')
        cy.wait(1000)
        cy.get('[data-cy="subjectTile-subj1"] .proj10-validiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })
    })
});