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
describe('Client Display Prerequisites Badges Tests', () => {

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

    it('display skills and badges in the table', () => {
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

        cy.cdVisit('/subjects/subj1');
        cy.cdClickSkill(1);

        cy.get('[data-pc-section="headertitle"]').contains('Prerequisite Name').click()

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-badge1"]').contains('Badge 1')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-skill3"]').contains('Skill 3')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="skillLink-proj1-skill4"]').contains('Skill 4')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="prereqType"]').contains('Badge')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="prereqType"]').contains('Skill')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="prereqType"]').contains('Skill')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="isAchievedCell"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="isAchievedCell"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="isAchievedCell"]').contains('Yes')
    });

    it('navigate to a badge via a table', () => {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.addLearningPathItem(1, 1, 2, true)

        cy.cdVisit('/subjects/subj1/skills/skill2');

        cy.get('[data-pc-section="headertitle"]').contains('Prerequisite Name').click()

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-badge1"]').contains('Badge 1')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"]').should('not.exist')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-badge1"]').click()

        cy.get('[data-cy="breadcrumb-badge1"]') // breadcrumb
        cy.get('[data-cy="skillsTitle"]').contains('Badge Details') // title

        cy.get('[data-cy="badge_badge1"] [data-cy="badgeTitle"]').contains('Badge 1')
        cy.get('#dependent-skills-network canvas').should('not.exist')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')

    });

    it('navigate to a badge via graph node', () => {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.addLearningPathItem(1, 1, 2, true)

        cy.cdVisit('/subjects/subj1/skills/skill2');

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-badge1"]').contains('Badge 1')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"]').should('not.exist')

        cy.clickOnNode(590, 205);
        cy.get('[data-cy="breadcrumb-badge1"]') // breadcrumb
        cy.get('[data-cy="skillsTitle"]').contains('Badge Details') // title

        cy.get('[data-cy="badge_badge1"] [data-cy="badgeTitle"]').contains('Badge 1')
        cy.get('#dependent-skills-network canvas').should('not.exist')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')
    });

    it('view prerequisites on badge page', function() {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 11)
        cy.createSkill(1, 1, 12)
        cy.createSkill(1, 1, 13)
        cy.createSkill(1, 1, 14)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 11);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 12);
        cy.createBadge(1, 2, { enabled: true });

        cy.createBadge(1, 3);
        cy.assignSkillToBadge(1, 3, 13);
        cy.createBadge(1, 3, { enabled: true })

        cy.addLearningPathItem(1, 2, 1, true, true)
        cy.addLearningPathItem(1, 3, 1, true, true)
        cy.addLearningPathItem(1, 1, 1, false, true)
        cy.addLearningPathItem(1, 2, 2, false, true)
        cy.addLearningPathItem(1, 3, 2)

        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'yesterday')
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now')
        cy.reportSkill(1, 13, Cypress.env('proxyUser'), 'yesterday')
        cy.reportSkill(1, 13, Cypress.env('proxyUser'), 'now')

        cy.cdVisit('/badges/badge1');

        cy.get('[data-pc-section="headertitle"]').contains('Prerequisite Name').click()

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-badge2"]').contains('Badge 2')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-badge3"]').contains('Badge 3')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="skillLink-proj1-skill1"]').contains('Skill 1')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="skillLink-proj1-skill2"]').contains('Skill 2')
        cy.get('[data-cy="prereqTable"] [data-p-index="4"] [data-cy="skillLink-proj1-skill3"]').contains('Skill 3')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="prereqType"]').contains('Badge')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="prereqType"]').contains('Badge')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="prereqType"]').contains('Skill')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="prereqType"]').contains('Skill')
        cy.get('[data-cy="prereqTable"] [data-p-index="4"] [data-cy="prereqType"]').contains('Skill')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="isAchievedCell"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="isAchievedCell"]').contains('Yes')
        cy.get('[data-cy="prereqTable"] [data-p-index="2"] [data-cy="isAchievedCell"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="3"] [data-cy="isAchievedCell"]').contains('Not Yet')
        cy.get('[data-cy="prereqTable"] [data-p-index="4"] [data-cy="isAchievedCell"]').contains('Yes')

        cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]')
            .contains('5');
        cy.get('[data-cy="depsProgress"] [data-cy="depsPercentComplete"]')
            .contains('40%');
    });

    it('navigate to badge from badge via table', function() {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 11)
        cy.createSkill(1, 1, 12)

        cy.createBadge(1, 1 );
        cy.assignSkillToBadge(1, 1, 11);
        cy.createBadge(1, 1, { enabled: true, description: "Badge #1" });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 12);
        cy.createBadge(1, 2, { enabled: true, description: "Badge #2" });


        cy.addLearningPathItem(1, 2, 1, true, true)
        cy.addLearningPathItem(1, 1, 1, false, true)

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.cdClickBadge(1);

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-badge2"]').contains('Badge 2')
        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-badge2"]').click()

        cy.get('[data-cy="breadcrumb-badge2"]') // breadcrumb
        cy.get('[data-cy="skillsTitle"]').contains('Badge Details') // title

        cy.get('[data-cy="badge_badge2"] [data-cy="badgeTitle"]').contains('Badge 2')
        // cy.get('[data-cy="badge_badge2"] [data-cy="markdownViewer"]').contains('Badge #2') // description
        //
        // cy.get('#dependent-skills-network canvas').should('not.exist')
        // cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 12')
        // cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')
    });

    it('navigate to badge from badge via graph node click', function() {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 11)
        cy.createSkill(1, 1, 12)

        cy.createBadge(1, 1 );
        cy.assignSkillToBadge(1, 1, 11);
        cy.createBadge(1, 1, { enabled: true, description: "Badge #1" });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 12);
        cy.createBadge(1, 2, { enabled: true, description: "Badge #2" });

        cy.addLearningPathItem(1, 2, 1, true, true)
        cy.addLearningPathItem(1, 1, 1, false, true)

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.cdClickBadge(1);

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj1-badge2"]').contains('Badge 2')
        cy.clickOnNode(415, 180);

        cy.get('[data-cy="breadcrumb-badge2"]') // breadcrumb
        cy.get('[data-cy="skillsTitle"]').contains('Badge Details') // title

        cy.get('[data-cy="badge_badge2"] [data-cy="badgeTitle"]').contains('Badge 2')
        cy.get('[data-cy="badge_badge2"] [data-cy="markdownViewer"]').contains('Badge #2') // description

        cy.get('#dependent-skills-network canvas').should('not.exist')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 12')
        cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')
    });

    it('navigate to skill from badge via table', function() {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 11)
        cy.createSkill(1, 1, 12)

        cy.createBadge(1, 1 );
        cy.assignSkillToBadge(1, 1, 11);
        cy.createBadge(1, 1, { enabled: true, description: "Badge #1" });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 12);
        cy.createBadge(1, 2, { enabled: true, description: "Badge #2" });

        cy.addLearningPathItem(1, 2, 1, true, true)
        cy.addLearningPathItem(1, 1, 1, false, true)

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.cdClickBadge(1);

        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-skill1"]').contains('Skill 1')
        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-skill1"]').click()

        cy.get('[data-cy="breadcrumb-skill1"]') // breadcrumb
        cy.get('[data-cy="skillsTitle"]').contains('Skill Overview') // title
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    });

    it('navigate to skill from badge via graph node click', function() {
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 11)
        cy.createSkill(1, 1, 12)

        cy.createBadge(1, 1 );
        cy.assignSkillToBadge(1, 1, 11);
        cy.createBadge(1, 1, { enabled: true, description: "Badge #1" });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 12);
        cy.createBadge(1, 2, { enabled: true, description: "Badge #2" });

        cy.addLearningPathItem(1, 2, 1, true, true)
        cy.addLearningPathItem(1, 1, 1, false, true)

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.cdClickBadge(1);

        cy.get('[data-cy="prereqTable"] [data-p-index="1"] [data-cy="skillLink-proj1-skill1"]').contains('Skill 1')
        cy.clickOnNode(770, 180);

        cy.get('[data-cy="breadcrumb-skill1"]') // breadcrumb
        cy.get('[data-cy="skillsTitle"]').contains('Skill Overview') // title
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    });

    it('navigate to cross-project skill from badge via table', function() {
        cy.createSkill(1, 1, 11)

        cy.createBadge(1, 1 );
        cy.assignSkillToBadge(1, 1, 11);
        cy.createBadge(1, 1, { enabled: true, description: "Badge #1" });

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);

        cy.request('PUT', `/admin/projects/proj2/skills/skill1/shared/projects/proj1`);
        cy.request('POST', `/admin/projects/proj1/badge1/prerequisite/proj2/skill1`);

        cy.cdVisit('/');
        cy.cdClickBadges();
        cy.cdClickBadge(1);

        cy.get('[data-cy="prereqTable"] [data-p-index="0"]').contains('Shared From This is project 2')

        cy.get('[data-cy="prereqTable"] [data-p-index="0"] [data-cy="skillLink-proj2-skill1"]').click()
        cy.get('[data-cy="skillProgress"]').contains('Project: This is project 2')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="crossProjAlert"]')
    });

    it('navigate to cross-project skill from badge via graph node click', function() {
        cy.createSkill(1, 1, 11)

        cy.createBadge(1, 1 );
        cy.assignSkillToBadge(1, 1, 11);
        cy.createBadge(1, 1, { enabled: true, description: "Badge #1" });

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);

        cy.request('PUT', `/admin/projects/proj2/skills/skill1/shared/projects/proj1`);
        cy.request('POST', `/admin/projects/proj1/badge1/prerequisite/proj2/skill1`);

        cy.cdVisit('/badges/badge1');

        cy.get('[data-cy="prereqTable"] [data-p-index="0"]').contains('Shared From This is project 2')

        cy.clickOnNode(590, 205);
        cy.get('[data-cy="skillProgress"]').contains('Project: This is project 2')
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="crossProjAlert"]')
    });
});
