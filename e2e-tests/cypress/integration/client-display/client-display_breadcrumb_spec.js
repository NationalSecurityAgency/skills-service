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

describe('Client Display Breadcrumb Tests', () => {

    beforeEach(() => {
        cy.logout()
        Cypress.env('disabledUILoginProp', true);
        cy.fixture('vars.json').then((vars) => {
            if (!Cypress.env('oauthMode')) {
                cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
            }
            // log back in as the project owner
            cy.loginAsProxyUser();
        })

        cy.intercept('GET', '/api/projects/proj1/pointHistory').as('pointHistoryChart');
    });

    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();
        Cypress.env('disabledUILoginProp', true);
        cy.fixture('vars.json').then((vars) => {
            if (!Cypress.env('oauthMode')) {
                cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
            }
        })
        cy.log('lo')
        cy.loginAsProxyUser()

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1',
            helpUrl: 'http://doHelpOnThisSubject.com',
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });
        cy.request('POST', `/admin/projects/proj1/skills/skill4/dependency/skill2`)

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: Cypress.env('proxyUser'), timestamp: new Date().getTime()})
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: Cypress.env('proxyUser'), timestamp: new Date().getTime() - 1000*60*60*24})

        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: Cypress.env('proxyUser'), timestamp: new Date().getTime()})
        cy.request('POST', `/api/projects/proj1/skills/skill3`, {userId: Cypress.env('proxyUser'), timestamp: new Date().getTime() - 1000*60*60*24})

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1')

        // setup cross-project dependency
        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 1, {name: 'Shared skill 1'});

        // share skill1 from proj2 with proj1
        cy.request('POST', '/admin/projects/proj2/skills/skill1/shared/projects/proj1');

        // assigned proj2/skill1 as a dependency of proj1/skill3
        cy.request('POST', '/admin/projects/proj1/skills/skill3/dependency/projects/proj2/skills/skill1');


        // create global badge as root user
        cy.loginAsRootUser();

        cy.createGlobalBadge(1)
        cy.assignSkillToGlobalBadge(1, 1)
        cy.assignSkillToGlobalBadge(1, 2)
        cy.assignSkillToGlobalBadge(1, 3)
        cy.assignSkillToGlobalBadge(1, 4)

        // log back in as the project owner
        cy.loginAsProxyUser();
    });


    after(() => {
        Cypress.env('disableResetDb', false);
    });


    it('test breadcrumbs starting on Overview page', () => {

        cy.cdVisit('/');
        cy.wait('@pointHistoryChart');

        // Overview page
        cy.contains('Overall Points');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains('Overview').should('be.visible');
    });

    it('test breadcrumbs starting on Rank page', () => {
        // Go to Project Rank page
        cy.cdVisit('/rank');
        cy.contains('Rank Overview');
        cy.get('[data-cy=breadcrumb-Rank]').should('exist');
        cy.get('[data-cy=breadcrumb-Rank]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Rank.*$/)).should('be.visible');

        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 2);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Rank');

        // back to home page
        cy.get('[data-cy=breadcrumb-Overview]').click();
        cy.contains('Overall Points');
    });


    it('test breadcrumbs starting on Subject page', () => {
        // Go to Subject Page
        cy.cdVisit('/subjects/subj1');
        cy.contains('Subject 1');
        cy.get('[data-cy=breadcrumb-subj1]').should('exist');
        cy.get('[data-cy=breadcrumb-subj1]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Subject:\s+subj1.*$/)).should('be.visible');
        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 2);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Subject: subj1');

        // back to Overview page
        cy.get('[data-cy=breadcrumb-Overview]').click();
        cy.contains('Overall Points');
    });


    it('test breadcrumbs starting on Skill page', () => {
        // Go to skill1 page
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.contains('This is 1');
        cy.get('[data-cy=breadcrumb-skill1]').should('exist');
        cy.get('[data-cy=breadcrumb-skill1]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-subj1]').should('exist');
        cy.get('[data-cy=breadcrumb-subj1]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Subject:\s+subj1.*Skill:\s+skill1$/)).should('be.visible');
        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 3);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Subject: subj1');
        cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Skill: skill1');

        // back to subject page
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.contains('Subject 1');

        // back to Overview page
        cy.get('[data-cy=breadcrumb-Overview]').click();
        cy.contains('Overall Points');
    });


    it('test breadcrumbs starting on Subject Rank page', () => {
        // Go to Subject Rank page
        cy.cdVisit('/subjects/subj1/rank');
        cy.contains('Rank Overview');
        cy.get('[data-cy=breadcrumb-Rank]').should('exist');
        cy.get('[data-cy=breadcrumb-Rank]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Subject:\s+subj1.*Rank.*$/)).should('be.visible');

        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 3);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Subject: subj1');
        cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Rank');

        // back to subject page
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.contains('Subject 1');

        // back to Overview page
        cy.get('[data-cy=breadcrumb-Overview]').click();
        cy.contains('Overall Points');

    });


    it('test breadcrumbs with internal dependency', () => {
        // Go to in-project dependency page
        cy.cdVisit('/subjects/subj1/skills/skill4/dependency/skill2');

        cy.get('[data-cy=breadcrumb-skill2]').should('exist');
        cy.get('[data-cy=breadcrumb-skill2]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-subj1]').should('exist');
        cy.get('[data-cy=breadcrumb-subj1]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Subject:\s+subj1.*Skill:\s+skill4.*Dependency:\s+skill2$/)).should('be.visible');
        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 4);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Subject: subj1');
        cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Skill: skill4');
        cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Dependency: skill2');

        // back to skill page
        cy.get('[data-cy=breadcrumb-skill4]').click();
        cy.contains('This is 4');

        // back to subject page
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.contains('Subject 1');

        // back to Overview page
        cy.get('[data-cy=breadcrumb-Overview]').click();
        cy.contains('Overall Points');
    });


    it('test breadcrumbs with cross-project dependency', () => {
        // Go to cross-project dependency page
        cy.cdVisit('/subjects/subj1/skills/skill3/crossProject/proj2/skill1');

        cy.get('[data-cy=breadcrumb-skill1]').should('exist');
        cy.get('[data-cy=breadcrumb-skill1]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-subj1]').should('exist');
        cy.get('[data-cy=breadcrumb-subj1]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Subject:\s+subj1.*Skill:\s+skill3.*Dependency:\s+skill1$/)).should('be.visible');
        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 4);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Subject: subj1');
        cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Skill: skill3');
        cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Dependency: skill1');
        cy.contains('Cross-project Skill');

        // back to skill page
        cy.get('[data-cy=breadcrumb-skill3]').click();
        cy.contains('This is 3');

        // back to subject page
        cy.get('[data-cy=breadcrumb-subj1]').click();
        cy.contains('Subject 1');

        // back to Overview page
        cy.get('[data-cy=breadcrumb-Overview]').click();
        cy.contains('Overall Points');
    });


    it('test breadcrumbs with badge', () => {
        // Go to Badges page
        cy.cdVisit('/badges');
        cy.contains('Badges');
        cy.get('[data-cy=breadcrumb-Badges]').should('exist');
        cy.get('[data-cy=breadcrumb-Badges]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Badges.*$/)).should('be.visible');

        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 2);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Badges');

        // Go to regular badge page
        cy.cdVisit('/badges/badge1');
        cy.contains('Badge 1');
        cy.get('[data-cy=breadcrumb-badge1]').should('exist');
        cy.get('[data-cy=breadcrumb-badge1]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Badges]').should('exist');
        cy.get('[data-cy=breadcrumb-Badges]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Badges.*Badge:\s+badge1$/)).should('be.visible');
        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 3);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Badges');
        cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Badge: badge1');

        // Go to skill1 page
        cy.cdClickSkill(0);
        cy.contains('This is 1');
        cy.get('[data-cy=breadcrumb-skill1]').should('exist');
        cy.get('[data-cy=breadcrumb-skill1]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-badge1]').should('exist');
        cy.get('[data-cy=breadcrumb-badge1]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Badges]').should('exist');
        cy.get('[data-cy=breadcrumb-Badges]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Badges.*Badge:\s+badge1.*Skill:\s+skill1$/)).should('be.visible');
        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 4);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Badges');
        cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Badge: badge1');
        cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Skill: skill1');

        // back to badge page
        cy.get('[data-cy=breadcrumb-badge1]').click();
        cy.contains('Badge 1');

        // back to badges page
        cy.get('[data-cy=breadcrumb-Badges]').click();
        cy.contains('Badges');

        // back to Overview page
        cy.get('[data-cy=breadcrumb-Overview]').click();
        cy.contains('Overall Points');
    });


    it('test breadcrumbs with global badge', () => {
        // Go to global badge page
        cy.cdVisit('/badges/global/globalBadge1');
        cy.contains('Global Badge 1');
        cy.get('[data-cy=breadcrumb-globalBadge1]').should('exist');
        cy.get('[data-cy=breadcrumb-globalBadge1]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Badges]').should('exist');
        cy.get('[data-cy=breadcrumb-Badges]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Badges.*Badge:\s+globalBadge1$/)).should('be.visible');
        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 3);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Badges');
        cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Badge: globalBadge1');

        // Go to skill1 page
        cy.cdClickSkill(0);
        cy.contains('This is 1');
        cy.get('[data-cy=breadcrumb-skill1]').should('exist');
        cy.get('[data-cy=breadcrumb-skill1]').should('not.have.attr', 'href');
        cy.get('[data-cy=breadcrumb-globalBadge1]').should('exist');
        cy.get('[data-cy=breadcrumb-globalBadge1]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Badges]').should('exist');
        cy.get('[data-cy=breadcrumb-Badges]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-Overview]').should('exist');
        cy.get('[data-cy=breadcrumb-Overview]').should('have.attr', 'href');
        cy.get('[data-cy=breadcrumb-bar]').contains(new RegExp(/^Overview.*Badges.*Badge:\s+globalBadge1.*Skill:\s+skill1$/)).should('be.visible');
        cy.get('[data-cy=breadcrumb-item]').its('length').should('eq', 4);
        cy.get('[data-cy=breadcrumb-item]').eq(0).should('contain.text', 'Overview');
        cy.get('[data-cy=breadcrumb-item]').eq(1).should('contain.text', 'Badges');
        cy.get('[data-cy=breadcrumb-item]').eq(2).should('contain.text', 'Badge: globalBadge1');
        cy.get('[data-cy=breadcrumb-item]').eq(3).should('contain.text', 'Skill: skill1');

        // back to badge page
        cy.get('[data-cy=breadcrumb-globalBadge1]').click();
        cy.contains('Badge 1');

        // back to badges page
        cy.get('[data-cy=breadcrumb-Badges]').click();
        cy.contains('Badges');

        // back to Overview page
        cy.get('[data-cy=breadcrumb-Overview]').click();
        cy.contains('Overall Points');
    });

});