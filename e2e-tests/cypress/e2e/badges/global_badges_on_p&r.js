/*
 * Copyright 2025 SkillTree
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
describe('Global Badge on P&R pages', () => {


    beforeEach(() => {
    })

    it('global badge with only levels', function () {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createProject(2);

        cy.createGlobalBadge(1);
        cy.assignProjectToGlobalBadge(1, 1, 2);
        cy.assignProjectToGlobalBadge(1, 2, 1);
        cy.enableGlobalBadge(1);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'))

        cy.addToMyProjects(1);
        cy.visit('/progress-and-rankings/my-badges');

        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('50% Complete')

        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgeDetailsLink_globalBadge1"]').click();
        cy.get('[data-cy="skillsTitle"]').contains('Global Badge Details');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('50% Complete')

        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="projectName"]').contains('This is project 1');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="projectLevel"]').contains('Level 2');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="percentComplete"]').should('have.text', '100');

        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="projectName"]').contains('This is project 2');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="projectLevel"]').contains('Level 1');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="percentComplete"]').should('have.text', '0');
    })

    it('global badge only with skills', function () {
        cy.createProject()
        cy.createSubject()
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1});
        cy.createSkill(1, 1, 3);
        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1, )
        cy.assignSkillToGlobalBadge(1, 2, 1, )
        cy.enableGlobalBadge(1);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'))

        cy.addToMyProjects(1);
        cy.visit('/progress-and-rankings/my-badges');

        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('50% Complete')

        cy.get('[data-cy="availableBadges"] [data-cy="badge_globalBadge1"] [data-cy="badgeDetailsLink_globalBadge1"]').click();
        cy.get('[data-cy="skillsTitle"]').contains('Global Badge Details');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('50% Complete')

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProjectName"]').contains('This is project 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProjectName"]').contains('This is project 1');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 100 Points');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '0');
    })

    it('achieved global badge with only levels', function () {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);

        cy.createGlobalBadge(1);
        cy.assignProjectToGlobalBadge(1, 1, 2);
        cy.assignProjectToGlobalBadge(1, 2, 1);
        cy.enableGlobalBadge(1);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'))
        cy.reportSkill(2, 1, Cypress.env('proxyUser'))

        cy.addToMyProjects(1);
        cy.visit('/progress-and-rankings/my-badges');

        cy.get('[data-cy="achievedBadges"] [data-cy="achievedBadge-globalBadge1"] [data-cy="badgeName"]').contains('Global Badge 1');
        cy.get('[data-cy="achievedBadges"] [data-cy="earnedBadgeLink_globalBadge1"]').click();

        cy.get('[data-cy="skillsTitle"]').contains('Global Badge Details');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('100% Complete')

        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="projectName"]').contains('This is project 1');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="projectLevel"]').contains('Level 2');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj1"] [data-cy="percentComplete"]').should('have.text', '100');

        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="projectName"]').contains('This is project 2');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="projectLevel"]').contains('Level 1');
        cy.get('[data-cy="globalBadgeProjectLevels"] [data-cy="gb_proj2"] [data-cy="percentComplete"]').should('have.text', '100');
    })

    it('earned global badge only with skills', function () {
        cy.createProject()
        cy.createSubject()
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1});
        cy.createSkill(1, 1, 2, { numPerformToCompletion: 1});
        cy.createSkill(1, 1, 3);
        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1, )
        cy.assignSkillToGlobalBadge(1, 2, 1, )
        cy.enableGlobalBadge(1);

        cy.reportSkill(1, 1, Cypress.env('proxyUser'))
        cy.reportSkill(1, 2, Cypress.env('proxyUser'))

        cy.addToMyProjects(1);
        cy.visit('/progress-and-rankings/my-badges');

        cy.get('[data-cy="achievedBadges"] [data-cy="achievedBadge-globalBadge1"] [data-cy="badgeName"]').contains('Global Badge 1');
        cy.get('[data-cy="achievedBadges"] [data-cy="earnedBadgeLink_globalBadge1"]').click();
        cy.get('[data-cy="skillsTitle"]').contains('Global Badge Details');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgeTitle"]').contains('Global Badge 1');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badge_globalBadge1"]').contains('Global Badge');
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="markdownViewer"]').contains('Lorem ipsum dolor sit am')
        cy.get('[data-cy="badge_globalBadge1"] [data-cy="badgePercentCompleted"]').contains('100% Complete')

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProjectName"]').contains('This is project 1');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100');

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 2');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProjectName"]').contains('This is project 1');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points');
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100');
    })

})