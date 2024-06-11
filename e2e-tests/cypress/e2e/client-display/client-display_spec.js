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
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Client Display Tests', () => {

    // '[data-cy=timePassed]'

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1',
            helpUrl: 'http://doHelpOnThisSubject.com',
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: 'Subject 2'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj3', {
            projectId: 'proj1',
            subjectId: 'subj3',
            name: 'Subject 3'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill2',
            name: `This is 2`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill3`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill3',
            name: `This is 3`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 2,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill4`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill4',
            name: `This is 4`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 2,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill5`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill5',
            name: `This is 5`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 1,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });
        cy.request('POST', `/admin/projects/proj1/skill4/prerequisite/proj1/skill2`);

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime()
        });
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime() - 1000 * 60 * 60 * 24
        });

        cy.request('POST', `/api/projects/proj1/skills/skill3`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime()
        });
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime() - 1000 * 60 * 60 * 24
        });

        const badge1 = {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        };
        cy.request('POST', '/admin/projects/proj1/badges/badge1', badge1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.enableBadge(1, 1);
    });

    it('visit home page', () => {

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.intercept('GET', '/api/projects/proj1/pointHistory')
            .as('pointHistoryChart');
        cy.cdVisit('/', true);
        cy.injectAxe();
        cy.contains('Overall Points');

        // some basic default theme validation
        cy.get('#app')
            .should('have.css', 'background-color')
            .and('equal', 'rgba(0, 0, 0, 0)');
        cy.wait('@pointHistoryChart');
        cy.wait(5000)
        cy.customA11y();
    });

    it('ability to expand skill details from subject page', () => {
        cy.cdVisit('/', true);
        cy.injectAxe();
        cy.cdClickSubj(0, 'Subject 1', true);
        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.contains('Lorem ipsum dolor sit amet');
        // 1 skill is locked
        cy.get('[data-cy="skillProgress_index-3"]').contains('Skill has 1 direct prerequisite(s).');
        cy.customA11y();
    });

    it('internal back button', () => {

        cy.intercept('GET', '/api/projects/proj1/pointHistory')
            .as('pointHistoryChart');

        cy.cdVisit('/?internalBackButton=true', true);
        cy.injectAxe();
        cy.contains('User Skills');
        cy.get('[data-cy=back]')
            .should('not.exist');

        // to ranking page and back
        cy.cdClickRank();
        cy.cdBack();

        // // to subject page and back
        cy.cdClickSubj(1, 'Subject 2');
        cy.cdBack();

        // to subject page (2nd subject card), then to skill page, back, back to home page
        cy.cdClickSubj(0, 'Subject 1', true);
        cy.cdClickSkill(0);
        cy.cdBack('Subject 1');
        cy.cdBack();

        // TODO: put back
        cy.wait('@pointHistoryChart');
        cy.wait(500); //we have to wait for the chart to load before doing accessibility tests
        cy.customA11y();
    });

    it('clearly represent navigable components', () => {
        cy.cdVisit('?internalBackButton=true', true);
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.cdClickSubj(0, 'Subject 1',true);
        cy.get('[data-cy="pointHistoryChartWithData"]')

        // make sure it can navigate into each skill via title
        cy.cdClickSkill(0, false);
        cy.cdBack('Subject 1');
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.cdClickSkill(1, false);
        cy.cdBack('Subject 1');
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.cdClickSkill(2, false);
        cy.cdBack('Subject 1');
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.cdClickSkill(3, false);
        cy.cdBack('Subject 1');
        cy.get('[data-cy="pointHistoryChartWithData"]')

        // make sure it can navigate into each skill via progress bar
        cy.cdClickSkill(0);
        cy.cdBack('Subject 1');
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.cdClickSkill(1);
        cy.cdBack('Subject 1');
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.cdClickSkill(2);
        cy.cdBack('Subject 1');
        cy.get('[data-cy="pointHistoryChartWithData"]')
        cy.cdClickSkill(3);
        cy.cdBack('Subject 1');
        cy.get('[data-cy="pointHistoryChartWithData"]')
    });

    it('components should not be clickable in the summary only option', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.cdVisit('/?isSummaryOnly=true');

        cy.get('[data-cy="myRank"]')
        cy.get('[data-cy="myRankBtn"]').should('not.exist')

        cy.get('[data-cy="myBadges"]')
        cy.get('[data-cy="myBadgesBtn"]').should('not.exist')

        // summaries should not be displayed at all
        cy.get('[data-cy="subjectTile-subj1"]')
        cy.get('[data-cy="subjectTileBtn"]').should('not.exist');

        cy.get('[data-cy="searchSkillsAcrossSubjects"]').should('not.exist')
    });

    it('skills-client: components should not be clickable in the summary only option', () => {
        cy.on('uncaught:exception', (err, runnable) => {
            // cy.log(err.message)
            if (err.message.includes('Handshake Reply Failed')) {
                return false
            }
            return true
        })

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.visit('/test-skills-client/proj1/?isSummaryOnly=true')

        cy.wrapIframe().find('[data-cy="myRank"]')
        cy.wrapIframe().find('[data-cy="myRankBtn"]').should('not.exist')

        cy.wrapIframe().find('[data-cy="myBadges"]')
        cy.wrapIframe().find('[data-cy="myBadgesBtn"]').should('not.exist')

        // summaries should not be displayed at all
        cy.wrapIframe().find('[data-cy="subjectTile-subj1"]')
        cy.wrapIframe().find('[data-cy="subjectTileBtn"]').should('not.exist')

        cy.wrapIframe().find('[data-cy="searchSkillsAcrossSubjects"]').should('not.exist')
    });

    it('skilltree brand should link to docs', () => {
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        // ensure brand exist
        cy.cdVisit('/', true);
        cy.get('[data-cy="skillTreePoweredBy"]')
            .contains('powered by');
        cy.get('[data-cy="skillTreePoweredBy"] a')
            .should('have.attr', 'href', 'https://skilltreeplatform.dev');
    });

    if (!Cypress.env('oauthMode')) {
        it('verify that authorization header is used in DevMode', () => {
            cy.ignoreSkillsClientError();
            cy.intercept('/api/projects/proj1/token')
              .as('getToken');
            cy.visit('/test-skills-client/proj1');
            cy.wait('@getToken')
              .its('response.body')
              .should('have.property', 'access_token');
            cy.get('@getToken')
              .its('response.body')
              .should('have.property', 'token_type', 'Bearer');
        });

        it.skip('verify that loginAsUser is used when retrieving token in DevMode', () => {
            cy.intercept({ url: 'http://localhost:8083/admin/projects/proj1/token/user7', })
                .as('getToken');
            cy.intercept('GET', '/api/projects/proj1/skills/skill4/dependencies')
                .as('getDependencies');
            cy.cdVisit('/subjects/subj1/skills/skill4?loginAsUser=user7');
            cy.wait('@getToken')
                .its('response.body')
                .should('have.property', 'access_token');
            cy.get('@getToken')
                .its('response.body')
                .should('have.property', 'token_type', 'Bearer')
            cy.wait('@getDependencies')
                .its('request.headers')
                .should('have.property', 'authorization');
        });
    }

    it('verify correct # of stars on subject card', () => {
        cy.request('PUT', '/admin/projects/proj1/subjects/subj1/levels/next', {
            percent: '99',
            'iconClass': 'fas fa-user-ninja',
        });
        cy.cdVisit('/', true);
        cy.contains('Overall Points');
        cy.get('[data-cy=subjectTile]')
            .eq(0)
            .contains('Subject 1');
        cy.get('[data-cy=subjectTile]')
            .eq(0)
            .find('[data-cy="subjectStars"] .p-rating-item')
            .should('have.length', 6);
        cy.get('[data-cy=subjectTile]')
          .eq(1)
          .find('[data-cy="subjectStars"] [data-pc-section="officon"]')
          .should('have.length', 5);
        cy.get('[data-cy=subjectTile]')
          .eq(2)
          .find('[data-cy="subjectStars"] [data-pc-section="officon"]')
          .should('have.length', 5);
    });

    it('description is rendered in client-display if configured on project', () => {
        cy.request('POST', '/admin/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1',
            description: 'I am a description *italic* **bold** foo',
        });
        cy.request('POST', '/admin/projects/proj1/settings', [{
            setting: 'show_project_description_everywhere',
            value: 'true',
            projectId: 'proj1'
        }]);

        cy.cdVisit('/', true);
        cy.get('[data-cy="projectDescription"]').contains('I am a description');
        cy.matchSnapshotImageForElement('[data-cy="projectDescription"]', {
            blackout: '[data-cy=pointHistoryChart]'
        });
    });

});


