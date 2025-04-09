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
import moment from 'moment-timezone';

describe('Client Display Prerequisites Snapshot Tests', () => {

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.createProject(1);
        cy.createSubject(1, 1);

        Cypress.Commands.add('clickOnNode', (x, y) => {
            cy.contains('Prerequisites');
            cy.get('[data-cy="graphLegend"]').contains('Legend');
            cy.wait(2000); // wait for chart
            // have to click twice to it to work...
            cy.get('#dependent-skills-network canvas')
                .should('be.visible')
                .click(x, y);
        });

        Cypress.Commands.add('navToTheFirstSkill', (x, y) => {
            cy.cdVisit('/');
            cy.cdClickSubj(0);
            cy.cdClickSkill(1);
        });

        // must set viewport to show entire canvas or it will not appear in the screenshot
        cy.viewport(1280, 1280);
    });

    it('Deps Chart - partially completed deps - 1 out 4', () => {
        const numSkills = 6;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, {
                name: `This is a very long name. yet is it ${i}`,
                numPerformToCompletion: 1
            });
        }

        cy.addLearningPathItem(1, 2, 1)
        cy.addLearningPathItem(1, 3, 1)
        cy.addLearningPathItem(1, 4, 2)
        cy.addLearningPathItem(1, 5, 2)

        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');
        cy.wait(1000);

        cy.navToTheFirstSkill();
        cy.contains('Prerequisites');
        cy.get('[data-cy="graphLegend"]').contains('Legend');
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]')
            .contains('4');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]')
            .contains('25%');

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-skill2"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-skill3"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="skillLink-proj1-skill4"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="skillLink-proj1-skill5"]')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="isAchievedCell"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="isAchievedCell"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="isAchievedCell"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="isAchievedCell"]').contains('Not Yet')

        cy.wait(1000); // wait for chart
        cy.matchSnapshotImageForElement('[data-cy="prerequisitesCard"]');
    });

    it('Deps Chart - partially completed deps - 3 out 4', () => {
        const numSkills = 6;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, {
                name: `This is a very long name. yet is it ${i}`,
                numPerformToCompletion: 1
            });
        }

        cy.addLearningPathItem(1, 2, 1)
        cy.addLearningPathItem(1, 3, 1)
        cy.addLearningPathItem(1, 4, 2)
        cy.addLearningPathItem(1, 5, 2)

        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.wait(1000);

        cy.navToTheFirstSkill();
        cy.contains('Prerequisites');
        cy.get('[data-cy="graphLegend"]').contains('Legend');
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]')
            .contains('4');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]')
            .contains('75%');

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-skill2"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-skill3"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="skillLink-proj1-skill4"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="skillLink-proj1-skill5"]')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="isAchievedCell"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="isAchievedCell"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="isAchievedCell"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="isAchievedCell"]').contains('Yes')

        cy.wait(1000); // wait for chart
        cy.matchSnapshotImageForElement('[data-cy="prerequisitesCard"]');
    });

    it('Deps Chart - fully satisfied deps', () => {
        const numSkills = 6;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, {
                name: `This is a very long name. yet is it ${i}`,
                numPerformToCompletion: 1
            });
        }

        cy.addLearningPathItem(1, 2, 1)
        cy.addLearningPathItem(1, 3, 1)
        cy.addLearningPathItem(1, 4, 2)
        cy.addLearningPathItem(1, 5, 2)

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.wait(1000);

        cy.navToTheFirstSkill();
        cy.contains('Prerequisites');
        cy.get('[data-cy="graphLegend"]').contains('Legend');
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]')
            .contains('4');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]')
            .contains('100%');

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-skill2"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-skill3"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="skillLink-proj1-skill4"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="skillLink-proj1-skill5"]')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="isAchievedCell"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="isAchievedCell"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="isAchievedCell"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="isAchievedCell"]').contains('Yes')

        cy.wait(1000); // wait for chart
        cy.matchSnapshotImageForElement('[data-cy="prerequisitesCard"]');
    });

    it('Deps Chart - fully satisfied deps with cross-project dep', () => {
        const numSkills = 9;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, { numPerformToCompletion: 1 });
        }

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 2, { numPerformToCompletion: 1 });

        cy.addLearningPathItem(1, 2, 1)
        cy.addLearningPathItem(1, 3, 2)
        cy.addCrossProjectLearningPathItem(2, 1, 1, 1)
        cy.addLearningPathItem(2, 2, 1)


        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(2, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(2, 1, Cypress.env('proxyUser'), 'now');

        cy.navToTheFirstSkill();
        cy.contains('Prerequisites');
        cy.get('[data-cy="graphLegend"]').contains('Legend');
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]')
            .contains('3');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]')
            .contains('100%');


        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj2-skill1"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="0"]').contains('Shared From This is project 2')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-skill2"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="skillLink-proj1-skill3"]')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="isAchievedCell"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="isAchievedCell"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="isAchievedCell"]').contains('Yes')

        cy.matchSnapshotImageForElement('[data-cy="prerequisitesCard"]');
    });

    it('cross-project dependency properly displayed', () => {
        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'proj2'
        });
        cy.request('POST', '/admin/projects/proj2/subjects/subj1', {
            projectId: 'proj2',
            subjectId: 'subj1',
            name: 'Subject 1',
            helpUrl: 'http://doHelpOnThisSubject.com',
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        });
        cy.request('POST', `/admin/projects/proj2/subjects/subj1/skills/skill42`, {
            projectId: 'proj2',
            subjectId: 'subj1',
            skillId: `skill42`,
            name: `This is 42`,
            type: 'Skill',
            pointIncrement: 50,
            numPerformToCompletion: 2,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });
        // share skill42 to proj1
        cy.request('PUT', '/admin/projects/proj2/skills/skill42/shared/projects/proj1');
        cy.createSkill(1);
        // issue #659 was evidenced in the specific situation of a skill having only a single dependency which was a skill
        // shared from another project
        cy.request('POST', '/admin/projects/proj1/skill1/prerequisite/proj2/skill42');

        cy.viewport(1200, 2000);
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.contains('Very Great Skill 1');
        // should render Prerequisites section
        cy.contains('Prerequisites');
        cy.get('[data-cy="skillLink-proj2-skill42"]')

        cy.wait(4000);
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]',
          {
              name: `LockedSkill-CrossProjectDependency`,
              blackout: '[data-cy="skillTreePoweredBy"]',
              errorThreshold: 0.05
          });
    });

    it('parent and child Prerequisites are properly displayed', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        // create dependency from skill3 -> skill2 -> skill1
        cy.request('POST', `/admin/projects/proj1/skill3/prerequisite/proj1/skill2`);
        cy.request('POST', `/admin/projects/proj1/skill2/prerequisite/proj1/skill1`);

        // Go to parent dependency page
        cy.cdVisit('/subjects/subj1/skills/skill3');

        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').should('have.text', '2')
        cy.wait(4000);
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"] [data-cy="prerequisitesCard"]', `InProjectDependency-parent`);

        // Go to child dependency page
        cy.clickOnNode(600, 262);
        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 2');
        // should render Prerequisites section
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').should('have.text', '1')
        cy.wait(4000);
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"] [data-cy="prerequisitesCard"]', `InProjectDependency-child`);
    });

    it('cross-project skill dep is shown when skill ids match', function () {
        cy.createSkill(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);

        cy.addCrossProjectLearningPathItem(2, 1, 1, 1)

        // Go to parent dependency page
        cy.viewport(1200, 2000);
        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 1');

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj2-skill1"]')
        cy.get('[data-cy="prereqTable"] [data-p-index="0"]').contains('Shared From This is project 2')

        // should render Prerequisites section
        cy.contains('Prerequisites');
        cy.wait(4000);
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]',
          {
              name: `CrossProject Dep with the same skillId`,
              blackout: '[data-cy="skillTreePoweredBy"]',
              errorThreshold: 0.05
          });

    });

    it('deps are added to partially achieved skill', () => {
        cy.createSkill(1, 1, 1);
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime()
        });
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj1/skill2`);
        cy.request('POST', `/admin/projects/proj1/skill2/prerequisite/proj1/skill3`);

        cy.viewport(1200, 2000);
        cy.cdVisit('/?internalBackButton=true');

        cy.cdClickSubj(0, 'Subject 1');

        cy.wait(4000);
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
            name: 'Subject-WithLockedSkills-ThatWerePartiallyAchieved',
            blackout: '[data-cy="pointHistoryChart"], [data-cy="skillTreePoweredBy"]',
            errorThreshold: 0.05
        });

        cy.cdClickSkill(0);
        cy.contains('Very Great Skill 1');
        const expectedMsg = 'You were able to earn partial points before the prerequisites were added';
        cy.contains(expectedMsg);
        // should render Prerequisites section
        cy.contains('Prerequisites');

        cy.wait(4000);
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
            name: 'LockedSkill-ThatWasPartiallyAchieved',
            blackout: '[data-cy="skillTreePoweredBy"]',
            errorThreshold: 0.05
        });

        // make sure the other locked skill doesn't contain the same message
        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.contains('Very Great Skill 2');
        cy.contains(expectedMsg)
            .should('not.exist');

        // make sure the skill without deps doesn't have the message
        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.contains('Very Great Skill 3');
        cy.contains(expectedMsg)
            .should('not.exist');
    });

    it('deps are added to fully achieved skill', () => {
        cy.createSkill(1, 1, 1);
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), '8 days ago');
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), '10 days ago');
        cy.createSkill(1, 1, 2);
        cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj1/skill2`);

        cy.viewport(1200, 2000);
        cy.cdVisit('/?internalBackButton=true', true);
        cy.cdClickSubj(0, 'Subject 1', true);

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
            name: 'Subject-WithLockedSkills-ThatWereFullyAchieved',
            blackout: '[data-cy="pointHistoryChart"], [data-cy="skillTreePoweredBy"]',
            errorThreshold: 0.05
        });

        cy.cdClickSkill(0);
        cy.contains('Very Great Skill 1');
        const msg = 'Congrats! You completed this skill before the prerequisites were added';
        cy.contains(msg);
        cy.get('[data-cy="progressBarWithLock"]').should('not.exist')

        cy.wait(4000);
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
            name: 'LockedSkill-ThatWasFullyAchieved',
            blackout: '[data-cy="achievementOn"], [data-cy="skillTreePoweredBy"]',
            errorThreshold: 0.05
        });

        // other skill should not have the message
        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.contains('Very Great Skill 2');
        cy.contains(msg)
            .should('not.exist');

        // now let's achieve the dependent skill
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), '8 days ago');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), '10 days ago');
        cy.cdBack('Subject 1');
        cy.cdClickSkill(0);
        cy.contains('Very Great Skill 1');
        cy.contains(msg)
            .should('not.exist');
    });

    it('badge skill with badge dependency displays correctly', () => {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 2);
        cy.createBadge(1, 2, { enabled: true });

        cy.addLearningPathItem(1, 1, 2, true, true)
        cy.addLearningPathItem(1, 3, 2, false, true)
        cy.addLearningPathItem(1, 4, 3)

        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday')
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

        cy.viewport(1200, 2000);
        cy.cdVisit('/subjects/subj1/skills/skill2');

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', {
            name: 'BadgeSkill-WithBadgeDependencies',
            blackout: '[data-cy="skillTreePoweredBy"]',
            errorThreshold: 0.05
        });
    });

});