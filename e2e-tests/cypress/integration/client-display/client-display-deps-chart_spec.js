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

const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');
describe('Client Display Dependencies Tests', () => {
    const snapshotOptions = {
        blackout: ['[data-cy=pointHistoryChart]'],
        failureThreshold: 0.03, // threshold for entire image
        failureThresholdType: 'percent', // percent of image or number of pixels
        customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
        capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
    };

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.createProject(1)
        cy.createSubject(1,1)

        Cypress.Commands.add("clickOnNode", (x,y) => {
            cy.contains('Dependencies');
            cy.contains('Node Legend');
            cy.wait(2000); // wait for chart
            // have to click twice to it to work...
            cy.get('#dependent-skills-network canvas').should('be.visible').click(x, y)
        });

        Cypress.Commands.add("navToTheFirstSkill", (x,y) => {
            cy.cdVisit('/');
            cy.cdClickSubj(0);
            cy.cdClickSkill(1);
        });


        // must set viewport to show entire canvas or it will not appear in the screenshot
        cy.viewport(1280, 1280)
    })

    it('Deps Chart - make sure drill down via click works', () => {
        const numSkills = 9;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, { name: `This is a very long name. yet is it ${i}`})
        }

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.assignDep(1, 1, 2);
        cy.assignDep(1, 2, 3);
        cy.assignDep(1, 3, 4);
        cy.assignDep(1, 4, 5);
        cy.assignDep(1, 5, 6);
        cy.assignDep(1, 6, 7);
        cy.assignDep(1, 7, 8);

        cy.assignCrossProjectDep(1, 1, 2, 1);
        cy.assignDep(2, 1, 2);

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.clickOnNode(425, 460);
        cy.contains('Project: This is project 2');
        cy.contains('Very Great Skill 1');
        cy.contains('Cross-project Skill');

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.clickOnNode(550, 93);
        cy.contains('This is a very long name. yet is it 2');

        // make sure that "this skill" node doesn't navigate away to another page
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.contains('This is a very long name. yet is it 1');
        cy.clickOnNode(500, 34);
        cy.wait(500)
        cy.contains('This is a very long name. yet is it 1');
    });

    it('Deps Chart - clicking off of a node but inside the dependency graph does not cause an error', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);

        // create dependency from skill2 -> skill1
        cy.request('POST', `/admin/projects/proj1/skills/skill2/dependency/skill1`)

        // Go to parent dependency page
        cy.cdVisit('/subjects/subj1/skills/skill2');

        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        // should render dependencies section
        cy.contains('Dependencies');

        cy.get('#dependent-skills-network canvas').should('be.visible').click(50, 325, { force: true })

        // should still be on the same page and no errors should have occurred (no errors are checked in afterEach)
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        cy.contains('Dependencies');
    });

    it('Deps Chart - partially completed deps - 1 out 4', () => {
        cy.viewport(1280, 1280)
        const numSkills = 6;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, { name: `This is a very long name. yet is it ${i}`, numPerformToCompletion: 1 })
        }

        cy.assignDep(1, 1, 2);
        cy.assignDep(1, 1, 3);
        cy.assignDep(1, 2, 4);
        cy.assignDep(1, 2, 5);

        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');
        cy.wait(1000);

        cy.navToTheFirstSkill();
        cy.contains('Dependencies');
        cy.contains('Node Legend');
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').contains('4');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]').contains('25%');

        cy.wait(1000); // wait for chart
        cy.matchSnapshotImageForElement('#dependent-skills-network', 'Deps Chart - partially completed - 1 out 4');
    })


    it('Deps Chart - partially completed deps - 3 out 4', () => {
        const numSkills = 6;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, { name: `This is a very long name. yet is it ${i}`, numPerformToCompletion: 1 })
        }

        cy.assignDep(1, 1, 2);
        cy.assignDep(1, 1, 3);
        cy.assignDep(1, 2, 4);
        cy.assignDep(1, 2, 5);

        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.wait(1000);

        cy.navToTheFirstSkill();
        cy.contains('Dependencies');
        cy.contains('Node Legend');
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').contains('4');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]').contains('75%');

        cy.wait(1000); // wait for chart
        cy.matchSnapshotImageForElement('#dependent-skills-network', 'Deps Chart - partially completed - 3 out 4');
    })

    it('Deps Chart - fully satisfied deps', () => {
        const numSkills = 6;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, { name: `This is a very long name. yet is it ${i}`, numPerformToCompletion: 1 })
        }

        cy.assignDep(1, 1, 2);
        cy.assignDep(1, 1, 3);
        cy.assignDep(1, 2, 4);
        cy.assignDep(1, 2, 5);

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 5, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.wait(1000);

        cy.navToTheFirstSkill();
        cy.contains('Dependencies');
        cy.contains('Node Legend');
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').contains('4');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]').contains('100%');

        cy.wait(1000); // wait for chart
        cy.matchSnapshotImageForElement('#dependent-skills-network', 'Deps Chart - fully satisfied deps');
    })

    it('Deps Chart - fully satisfied deps with cross-project dep', () => {
        const numSkills = 9;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, { numPerformToCompletion: 1 })
        }

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { numPerformToCompletion: 1 });
        cy.createSkill(2, 1, 2, { numPerformToCompletion: 1 });

        cy.assignDep(1, 1, 2);
        cy.assignDep(1, 2, 3);

        cy.assignCrossProjectDep(1, 1, 2, 1);
        cy.assignDep(2, 1, 2);

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(2, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(2, 1, Cypress.env('proxyUser'), 'now');

        cy.navToTheFirstSkill();
        cy.contains('Dependencies');
        cy.contains('Node Legend');
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').contains('3');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]').contains('100%');

        cy.matchSnapshotImageForElement('#dependent-skills-network', 'Deps Chart - fully satisfied deps with cross-project skill');
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
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
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });
        // share skill42 to proj1
        cy.request('PUT', '/admin/projects/proj2/skills/skill42/shared/projects/proj1');
        cy.createSkill(1);
        // issue #659 was evidenced in the specific situation of a skill having only a single dependency which was a skill
        // shared from another project
        cy.request('POST', '/admin/projects/proj1/skills/skill1/dependency/projects/proj2/skills/skill42');

        cy.cdVisit('/?internalBackButton=true');
        cy.cdClickSubj(0, 'Subject 1');
        cy.wait(4000);

        cy.cdClickSkill(0);
        cy.contains('Very Great Skill 1');
        // should render dependencies section
        cy.contains('Dependencies');

        cy.wait(4000);
        cy.matchSnapshotImage(`LockedSkill-CrossProjectDependency`, snapshotOptions);
    });

    it('parent and child dependencies are properly displayed', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        // create dependency from skill3 -> skill2 -> skill1
        cy.request('POST', `/admin/projects/proj1/skills/skill3/dependency/skill2`)
        cy.request('POST', `/admin/projects/proj1/skills/skill2/dependency/skill1`)

        // Go to parent dependency page
        cy.cdVisit('/subjects/subj1/skills/skill3');

        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3');
        // should render dependencies section
        cy.contains('Dependencies');
        cy.wait(4000);
        cy.matchSnapshotImage(`InProjectDependency-parent`, snapshotOptions);

        // Go to child dependency page
        cy.cdVisit('/subjects/subj1/skills/skill3/dependency/skill2');
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        // should render dependencies section
        cy.contains('Dependencies');
        cy.wait(4000);
        cy.matchSnapshotImage(`InProjectDependency-child`, snapshotOptions);
    });

    it('cross-project skill dep is shown when skill ids match', function () {
        cy.createSkill(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.log('before');
        cy.createSkill(2, 1, 1);
        cy.log('after');

        cy.assignCrossProjectDep(1, 1, 2, 1);

        // Go to parent dependency page
        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 1');
        // should render dependencies section
        cy.contains('Dependencies');
        cy.wait(4000);
        cy.matchSnapshotImage(`CrossProject Dep with the same skillId`, snapshotOptions);

    });

    it('clicking off of a node but inside the dependency graph does not cause an error', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);

        // create dependency from skill2 -> skill1
        cy.request('POST', `/admin/projects/proj1/skills/skill2/dependency/skill1`)

        // Go to parent dependency page
        cy.cdVisit('/subjects/subj1/skills/skill2');

        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        // should render dependencies section
        cy.contains('Dependencies');

        cy.get('#dependent-skills-network canvas').should('be.visible').click(50, 325, { force: true })

        // should still be on the same page and no errors should have occurred (no errors are checked in afterEach)
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        cy.contains('Dependencies');
    });

    it('deps are added to partially achieved skill', () => {
        cy.createSkill(1, 1, 1);
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: new Date().getTime()
        })
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.request('POST', `/admin/projects/proj1/skills/skill1/dependency/skill2`)
        cy.request('POST', `/admin/projects/proj1/skills/skill2/dependency/skill3`)

        cy.cdVisit('/?internalBackButton=true');

        cy.cdClickSubj(0, 'Subject 1');

        cy.wait(4000);
        cy.matchSnapshotImage(`Subject-WithLockedSkills-ThatWerePartiallyAchieved`, snapshotOptions);

        cy.cdClickSkill(0);
        cy.contains('Very Great Skill 1');
        const expectedMsg = 'You were able to earn partial points before the dependencies were added';
        cy.contains(expectedMsg);
        // should render dependencies section
        cy.contains('Dependencies');

        cy.wait(4000);
        cy.matchSnapshotImage(`LockedSkill-ThatWasPartiallyAchieved`, snapshotOptions);

        // make sure the other locked skill doesn't contain the same message
        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.contains('Very Great Skill 2');
        cy.contains(expectedMsg).should('not.exist');

        // make sure the skill without deps doesn't have the message
        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.contains('Very Great Skill 3');
        cy.contains(expectedMsg).should('not.exist');
    });

    it('deps are added to fully achieved skill', () => {
        cy.createSkill(1, 1, 1);
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: moment.utc().format('x')
        })
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: Cypress.env('proxyUser'),
            timestamp: moment.utc().subtract(2, 'day').format('x')
        })
        cy.createSkill(1, 1, 2);
        cy.request('POST', `/admin/projects/proj1/skills/skill1/dependency/skill2`)

        cy.cdVisit('/?internalBackButton=true');
        cy.cdClickSubj(0, 'Subject 1');

        cy.matchSnapshotImage(`Subject-WithLockedSkills-ThatWereFullyAchieved`, snapshotOptions);

        cy.cdClickSkill(0);
        cy.contains('Very Great Skill 1');
        const msg = "Congrats! You completed this skill before the dependencies were added";
        cy.contains(msg);

        cy.wait(4000);
        cy.matchSnapshotImage(`LockedSkill-ThatWasFullyAchieved`, snapshotOptions);

        // other skill should not have the message
        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.contains('Very Great Skill 2');
        cy.contains(msg).should('not.exist');

        // now let's achieve the dependent skill
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {
            userId: Cypress.env('proxyUser'),
            timestamp:  moment.utc().format('x')
        })
        cy.request('POST', `/api/projects/proj1/skills/skill2`, {
            userId: Cypress.env('proxyUser'),
            timestamp:  moment.utc().subtract(2, 'day').format('x')
        })
        cy.cdBack('Subject 1');
        cy.cdClickSkill(0);
        cy.contains('Very Great Skill 1');
        cy.contains(msg).should('not.exist');
    });
})