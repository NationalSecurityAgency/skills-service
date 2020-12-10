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

describe('Client Display Tests', () => {

    const snapshotOptions = {
        blackout: ['[data-cy=pointHistoryChart]', '[data-cy=timePassed]'],
        failureThreshold: 0.03, // threshold for entire image
        failureThresholdType: 'percent', // percent of image or number of pixels
        customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
        capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
    };

    const cssAttachedToNavigableCards = 'skills-navigable-item';

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
    });

    it('visit home page', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.cdVisit('/');
        cy.injectAxe();
        cy.contains('Overall Points');

        // some basic default theme validation
        cy.get("#app").should('have.css', 'background-color')
            .and('equal', 'rgba(0, 0, 0, 0)');
        cy.customA11y();
    });

    it('ability to expand skill details from subject page', () => {
        cy.cdVisit('/')
        cy.injectAxe();
        cy.cdClickSubj(0);
        cy.get('[data-cy=toggleSkillDetails]').click()
        cy.contains('Lorem ipsum dolor sit amet')
        // 1 skill is locked
        cy.contains('Skill has 1 direct dependent(s).')
        cy.customA11y();
    });

    it('back button', () => {
        cy.cdVisit('/');
        cy.injectAxe();
        cy.contains('User Skills');
        cy.get('[data-cy=back]').should('not.exist');

        // to ranking page and back
        cy.cdClickRank();
        cy.cdBack();

        // to subject page and back
        cy.cdClickSubj(1, 'Subject 2');
        cy.cdBack();

        // to subject page (2nd subject card), then to skill page, back, back to home page
        cy.cdClickSubj(0, 'Subject 1');
        cy.cdClickSkill(0);
        cy.cdBack('Subject 1');
        cy.cdBack();
        cy.customA11y();
    });

    it('clearly represent navigable components', () => {
        cy.cdVisit('/');

        cy.get('[data-cy=myRank]').should('have.class', 'skills-navigable-item');
        cy.get('[data-cy=myBadges]').should('have.class', 'skills-navigable-item');
        cy.get('[data-cy=subjectTile]').eq(0).should('have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=subjectTile]').eq(1).should('have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=subjectTile]').eq(2).should('have.class', cssAttachedToNavigableCards);

        cy.cdClickSubj(0);

        // make sure progress bars have proper css attached
        cy.get('[data-cy=skillProgress]:nth-child(1) [data-cy=skillProgressBar]').should('have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=skillProgress]:nth-child(2) [data-cy=skillProgressBar]').should('have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=skillProgress]:nth-child(3) [data-cy=skillProgressBar]').should('have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=skillProgress]:nth-child(4) [data-cy=skillProgressBar]').should('have.class', cssAttachedToNavigableCards);

        // make sure it can navigate into each skill via title
        cy.cdClickSkill(0, false);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(1, false);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(2, false);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(3, false);
        cy.cdBack('Subject 1');

        // make sure it can navigate into each skill via progress bar
        cy.cdClickSkill(0);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.cdBack('Subject 1');
        cy.cdClickSkill(3);
        cy.cdBack('Subject 1');
    });

    it('components should not be clickable in the summary only option', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.cdVisit('/?isSummaryOnly=true');
        cy.injectAxe();

        // cy.get('[data-cy=myRank]').contains("1")
        cy.get('[data-cy=myBadges]').contains("0 Badges")

        // make sure click doesn't take us anywhere
        cy.get('[data-cy=myRank]').click()
        cy.contains("User Skills")

        cy.get('[data-cy=myBadges]').click()
        cy.contains("User Skills")

        // make sure css is not attached
        cy.get('[data-cy=myRank]').should('not.have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=myBadges]').should('not.have.class', cssAttachedToNavigableCards);

        // summaries should not be displayed at all
        cy.get('[data-cy=subjectTile]').should('not.exist');
        cy.customA11y();
    });

    it('display achieved date on skill overview page', () => {
        const m = moment('2020-09-12 11', 'YYYY-MM-DD HH');
        const orig = m.clone()
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.subtract(4, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.subtract(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.subtract(2, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.subtract(1, 'day').format('x')})
        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);

        cy.get('[data-cy=achievementDate]').contains(`Achieved on ${orig.format("MMMM Do YYYY")}`);
        cy.get('[data-cy=achievementDate]').contains(`${orig.fromNow()}`);

        cy.matchImageSnapshot(`Skill-Overview-Achieved`, snapshotOptions);

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);

        cy.get('[data-cy=achievementDate]').contains(`Achieved on ${orig.format("MMMM Do YYYY")}`);
        cy.get('[data-cy=achievementDate]').contains(`${orig.fromNow()}`);

        cy.matchImageSnapshot(`Skill-Overview-Achieved-Themed`, snapshotOptions);

        cy.setResolution('iphone-6');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);

        cy.get('[data-cy=achievementDate]').contains(`Achieved on ${orig.format("MMMM Do YYYY")}`);
        cy.get('[data-cy=achievementDate]').contains(`${orig.fromNow()}`);

        cy.matchImageSnapshot(`Skill-Overview-Achieved-iphone6`, snapshotOptions);

        cy.setResolution('ipad-2');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(1);

        cy.get('[data-cy=achievementDate]').contains(`Achieved on ${orig.format("MMMM Do YYYY")}`);
        cy.get('[data-cy=achievementDate]').contains(`${orig.fromNow()}`);

        cy.matchImageSnapshot(`Skill-Overview-Achieved-ipad2`, snapshotOptions);

    });

    it('display achieved date on subject page when skill details are expanded', () => {
        const m = moment('2020-09-12 11', 'YYYY-MM-DD HH');
        const orig = m.clone()
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.subtract(4, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.subtract(3, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.subtract(2, 'day').format('x')})
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {userId: Cypress.env('proxyUser'), timestamp: m.subtract(1, 'day').format('x')})
        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy=toggleSkillDetails]').click();
        cy.get('[data-cy=skillProgress]:nth-child(2) [data-cy=achievementDate]').contains(`Achieved on ${orig.format("MMMM Do YYYY")}`);
        cy.get('[data-cy=skillProgress]:nth-child(2) [data-cy=achievementDate]').contains(`${orig.fromNow()}`);
    });

});

