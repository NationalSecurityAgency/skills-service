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
describe('Client Display Prerequisites Tests', () => {

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

    it('Deps Chart - drill down into deps from another subject via click', () => {
        cy.createSkill(1, 1, 1);

        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 3);
        cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj1/skill3Subj2`);

        cy.createSubject(1, 3);
        cy.createSkill(1, 3, 4);
        cy.request('POST', `/admin/projects/proj1/skill3Subj2/prerequisite/proj1/skill4Subj3`);

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.clickOnNode(600, 250);

        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3 Subj2')

        cy.get('[data-cy="breadcrumb-subj2"]').should('exist')
        cy.get('[data-cy="breadcrumb-skill1"]').should('not.exist')

        cy.clickOnNode(600, 200);
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 4 Subj3')
        cy.get('[data-cy="breadcrumb-subj2"]').should('not.exist')
        cy.get('[data-cy="breadcrumb-subj3"]').should('exist')
    })

    it('Deps Chart - drill down into deps from another subject via click - verify that prev/next works in the dep', () => {
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 3);
        cy.createSkill(1, 2, 4);
        cy.createSkill(1, 2, 5);
        cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj1/skill3Subj2`);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);
        cy.createSkill(1, 1, 8);

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.clickOnNode(600, 200);

        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3 Subj2')
        cy.get('[data-cy="breadcrumb-subj2"]').should('exist')
        cy.get('[data-cy="nextSkill"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 4 Subj2')

        cy.get('[data-cy="prevSkill"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3 Subj2')

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj2"]').click()
        cy.get('[data-cy="title"]').should('have.text', 'Subject 2');
    })

    it('Deps Chart - drill down into deps from another project and subject via click', () => {
        cy.createSkill(1, 1, 1);

        cy.createProject(2)
        cy.createSubject(2, 2);
        cy.createSkill(2, 2, 3);
        cy.request('PUT', `/admin/projects/proj2/skills/skill3Subj2/shared/projects/proj1`);
        cy.request('POST', `/admin/projects/proj1/skill1/prerequisite/proj2/skill3Subj2`);

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.clickOnNode(600, 200);

        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 3 Subj2')
        cy.get('[data-cy="breadcrumb-subj1"]').should('exist')
        cy.get('[data-cy="breadcrumb-skill1"]').should('exist')
        cy.get('[data-cy="breadcrumb-subj2"]').should('not.exist')
        cy.contains('This is a cross-project skill!');
    })

    it('Deps Chart - make sure drill down via click works', () => {
        const numSkills = 9;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, { name: `This is a very long name. yet is it ${i}` });
        }

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.addLearningPathItem(1, 2, 1)
        cy.addLearningPathItem(1, 3, 1)
        cy.addLearningPathItem(1, 4, 1)
        cy.addLearningPathItem(1, 5, 4)
        cy.addLearningPathItem(1, 6, 5)
        cy.addLearningPathItem(1, 7, 6)
        cy.addLearningPathItem(1, 8, 7)

        cy.addCrossProjectLearningPathItem(2, 1, 1, 1)
        cy.addLearningPathItem(2, 2, 1)

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillProgressTitle"]').contains('This is a very long name. yet is it 1')
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').should('have.text', '8')
        cy.clickOnNode(230, 335);
        cy.contains('Project: This is project 2');
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1');
        cy.get('[data-cy="crossProjAlert"]').contains('cross-project skill');

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillProgressTitle"]').contains('This is a very long name. yet is it 1')
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').should('have.text', '8')
        cy.clickOnNode(468, 330);
        cy.get('[data-cy="skillProgressTitle"]').contains('This is a very long name. yet is it 2');

        // make sure that "this skill" node doesn't navigate away to another page
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillProgressTitle"]').contains('This is a very long name. yet is it 1')
        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').should('have.text', '8')
        cy.clickOnNode(580, 435);
        cy.wait(500);
        cy.get('[data-cy="skillProgressTitle"]').contains('This is a very long name. yet is it 1');
    });

    it('Deps Chart - clicking off of a node but inside the dependency graph does not cause an error', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);

        // create dependency from skill2 -> skill1
        cy.request('POST', `/admin/projects/proj1/skill2/prerequisite/proj1/skill1`);

        // Go to parent dependency page
        cy.cdVisit('/subjects/subj1/skills/skill2');

        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 2');
        // should render Prerequisites section
        cy.contains('Prerequisites');

        cy.get('#dependent-skills-network canvas')
            .should('be.visible')
            .click(50, 325, { force: true });

        // should still be on the same page and no errors should have occurred (no errors are checked in afterEach)
        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 2');
        cy.contains('Prerequisites');
    });

    it('clicking off of a node but inside the dependency graph does not cause an error', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);

        // create dependency from skill2 -> skill1
        cy.request('POST', `/admin/projects/proj1/skill2/prerequisite/proj1/skill1`);

        // Go to parent dependency page
        cy.cdVisit('/subjects/subj1/skills/skill2');

        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 2');
        // should render Prerequisites section
        cy.contains('Prerequisites');

        cy.get('#dependent-skills-network canvas')
            .should('be.visible')
            .click(50, 325, { force: true });

        // should still be on the same page and no errors should have occurred (no errors are checked in afterEach)
        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 2');
        cy.contains('Prerequisites');
    });

    it('table paging renders after 8 elements', () => {
        const numSkills = 10;
        for (let i = 0; i < numSkills; i += 1) {
            cy.createSkill(1, 1, i, {
                name: `skill${i}`,
                numPerformToCompletion: 1
            });
        }

        for (let i = 0; i < numSkills - 1; i += 1) {
            cy.addLearningPathItem(1, i, i+1)
        }

        cy.navToTheFirstSkill();
        cy.get('[data-cy="graphLegend"]').contains('Legend');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(9);

        cy.get('[data-cy="skillProgressTitle"]').contains('skill9')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill0"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill1"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill2"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill3"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill4"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill5"]').should('not.exist')

        cy.get('[data-pc-extend="paginator"] [aria-label="Page 1"]')
        cy.get('[data-pc-extend="paginator"] [aria-label="Page 2"]')
        cy.get('[data-pc-extend="paginator"] [aria-label="Page 3"]').should('not.exist')

        // page 2
        cy.get('[data-pc-extend="paginator"] [aria-label="Page 2"]').click()
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill4"]').should('not.exist')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill5"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill6"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill7"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill8"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill9"]').should('not.exist')

        cy.get('[data-cy="skillLink-proj1-skill8"]').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('skill8')
        cy.get('[data-pc-extend="paginator"]').should('not.exist')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill0"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill1"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill2"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill3"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill4"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill5"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill6"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill7"]')
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj1-skill8"]').should('not.exist')
    });

    it('navigate to cross-project prerequisite', function () {
        cy.createSkill(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);

        cy.addCrossProjectLearningPathItem(2, 1, 1, 1)

        // Go to parent dependency page
        cy.cdVisit('/subjects/subj1/skills/skill1');

        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1');
        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj2-skill1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="prereqTable"] [data-p-index="0"]').contains('Shared From This is project 2')

        cy.get('[data-cy="prereqTable"] [data-cy="skillLink-proj2-skill1"]').click()
        cy.get('[data-cy="skillProgress"]').contains('Project: This is project 2')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="crossProjAlert"]')
    });

    it('learning path graph with multiple links to the same prerequisite', function() {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)
        cy.createSkill(1, 1, 6)
        cy.createSkill(1, 1, 7)
        cy.createSkill(1, 1, 8)

        cy.addLearningPathItem(1, 1, 2)
        cy.addLearningPathItem(1, 2, 3)

        cy.addLearningPathItem(1, 4, 5)
        cy.addLearningPathItem(1, 5, 6)

        cy.addLearningPathItem(1, 1, 6)
        cy.addLearningPathItem(1, 5, 3)

        cy.addLearningPathItem(1, 3, 7)
        cy.addLearningPathItem(1, 6, 7)

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday')
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'yesterday')
        cy.reportSkill(1, 4, Cypress.env('proxyUser'), 'now')

        cy.cdVisit('/subjects/subj1/skills/skill7');

        cy.get('[data-pc-section="columntitle"]').contains('Prerequisite Name').click()

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-skill1"]').contains('Skill 1')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-skill2"]').contains('Skill 2')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="skillLink-proj1-skill3"]').contains('Skill 3')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="skillLink-proj1-skill4"]').contains('Skill 4')
        cy.get('[data-cy="prereqTable"] [data-p-index="4"] [data-cy="skillLink-proj1-skill5"]').contains('Skill 5')
        cy.get('[data-cy="prereqTable"] [data-p-index="5"] [data-cy="skillLink-proj1-skill6"]').contains('Skill 6')
        cy.get('[data-cy="prereqTable"] [data-p-index="6"]').should('not.exist')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="achievedCellYes"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="achievedCellNo"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="achievedCellNo"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="achievedCellYes"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="4"] [data-cy="achievedCellNo"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="5"] [data-cy="achievedCellNo"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="6"]').should('not.exist')

        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]')
            .contains('6');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]')
            .contains('33%');
    });

});
