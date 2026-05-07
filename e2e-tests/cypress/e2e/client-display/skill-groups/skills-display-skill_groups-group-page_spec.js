/*
 * Copyright 2026 SkillTree
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

describe('Skills Display Skills Groups Page Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    it('skills group page must load and display skills under a group', () => {
        const groupDescription = "This is a test skills group description";
        cy.createSkillsGroup(1, 1, 1, {
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2, { pointIncrement: 450 });
        cy.addSkillToGroup(1, 1, 1, 3);

        // Add progress to several skills
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'yesterday');
        cy.reportSkill(1, 3, Cypress.env('proxyUser'), 'now');

        cy.cdVisit('/subjects/subj1/groups/group1');

        // Verify the page loads and displays group information
        cy.get('[data-cy="skillsTitle"]').contains('Group Overview');
        
        // Verify group name and progress information
        cy.get('[data-cy="skillsGroupName"]').should('contain', 'Awesome Group 1');
        cy.get('[data-cy="skillsGroupProgress"]').contains(/1 \/ 3 Skills/);

        // Verify group description is displayed
        cy.get('[data-cy="skillsGroupDescription"]').contains(groupDescription);

        // Verify skills progress list is displayed
        cy.get('[data-cy="skillsProgressList"]').should('exist');

        // Verify individual skills are displayed under the group with progress
        cy.get('[data-cy="skillProgressTitle-skill1"] [data-cy="skillProgressTitle"]')
            .should('contain', 'Very Great Skill 1');
        cy.get('[data-cy="skillProgressTitle-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .should('contain', '100 / 200 Points');

        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"] [data-cy="skillProgressTitle"]')
            .should('contain', 'Very Great Skill 2');
        cy.get('[data-cy="skillProgressTitle-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .should('contain', '900 / 900 Points');

        cy.get('[data-cy="skillProgressTitle-skill3"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill3"] [data-cy="skillProgressTitle"]')
            .should('contain', 'Very Great Skill 3');
        cy.get('[data-cy="skillProgressTitle-skill3"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .should('contain', '100 / 200 Points');
    });

    it('skills group page handles empty group gracefully', () => {
        // Create a skills group without any skills
        cy.createSkillsGroup(1, 1, 1);

        // Visit the skills group page
        cy.cdVisit('/subjects/subj1/groups/group1', true);

        // Verify the page loads and displays group information
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Group Overview');
        cy.get('[data-cy="skillsGroupProgress"]').contains(/0 \/ 0 Skills/);
        cy.get('[data-cy="skillsGroupDescription"]').should('not.exist')

        // Verify skills progress list exists but shows no skills
        cy.get('[data-cy="skillsProgressList"]').should('exist');
        cy.get('[data-cy="noContent"]').should('be.visible')
            .contains('Skills have not been added yet.');
    });

    it('expand Skill Details toggle and validate skill descriptions are showing', () => {
        const skillDescription1 = "This is the description for skill 1";
        const skillDescription2 = "This is the description for skill 2";
        
        // Create a skills group with skills that have descriptions and different point increments
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1, {
            description: skillDescription1,
            pointIncrement: 25,
            numPerformToCompletion: 4
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 100,
            description: skillDescription2,
            numPerformToCompletion: 2
        });

        // Add progress to skills with different timing
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now'); // Today's points for skill 1
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'yesterday'); // Overall points for skill 1
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now'); // Today's points for skill 2

        cy.cdVisit('/subjects/subj1/groups/group1');

        // Verify skills are displayed initially without descriptions
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');
        cy.get('[data-cy="skillDescription-skill1"]').should('not.exist');
        cy.get('[data-cy="skillDescription-skill2"]').should('not.exist');

        // Click the Skill Details toggle to expand details
        cy.get('[data-cy="toggleSkillDetails"]').click();

        // Verify skill descriptions are now visible
        cy.get('[data-cy="skillDescription-skill1"]').should('be.visible');
        cy.get('[data-cy="skillDescription-skill1"]').should('contain', skillDescription1);
        
        cy.get('[data-cy="skillDescription-skill2"]').should('be.visible');
        cy.get('[data-cy="skillDescription-skill2"]').should('contain', skillDescription2);

        // Validate skill 1 details with progress
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="overallPointsEarnedCard"]').should('be.visible');
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="overallPointsEarnedCard"]').should('contain', '50');
        
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="pointsAchievedTodayCard"]').should('be.visible');
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="pointsAchievedTodayCard"]').should('contain', '25');
        
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="pointsPerOccurrenceCard"]').should('be.visible');
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="pointsPerOccurrenceCard"]').should('contain', '25');
        
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="timeWindowPts"]').should('be.visible');

        // Validate skill 2 details with progress
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="overallPointsEarnedCard"]').should('be.visible');
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="overallPointsEarnedCard"]').should('contain', '100');
        
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="pointsAchievedTodayCard"]').should('be.visible');
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="pointsAchievedTodayCard"]').should('contain', '100');
        
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="pointsPerOccurrenceCard"]').should('be.visible');
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="pointsPerOccurrenceCard"]').should('contain', '100');
        
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="timeWindowPts"]').should('be.visible');
    });

    it('search functionality on skills group page', () => {

        // Create a skills group with multiple skills using default names
        cy.createSkillsGroup(1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 1);
        cy.addSkillToGroup(1, 1, 1, 2);
        cy.addSkillToGroup(1, 1, 1, 3);

        cy.cdVisit('/subjects/subj1/groups/group1');

        // Verify all skills are initially displayed
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill3"]').should('exist');

        // Search for "skill1" - should show only skill 1
        cy.get('[data-cy="skillsSearchInput"]').type('skill 1');
        
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('contain', 'Very Great Skill 1');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('not.exist');
        cy.get('[data-cy="skillProgressTitle-skill3"]').should('not.exist');

        // Clear search and verify all skills return
        cy.get('[data-cy="clearSkillsSearchInput"]').click();

        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill3"]').should('exist');

        // Search for "skill2" - should show only skill 2
        cy.get('[data-cy="skillsSearchInput"]').type('skill 2');

        cy.get('[data-cy="skillProgressTitle-skill1"]').should('not.exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('contain', 'Very Great Skill 2');
        cy.get('[data-cy="skillProgressTitle-skill3"]').should('not.exist');

        // Search for "skill3" - should show only skill 3
        cy.get('[data-cy="skillsSearchInput"]').clear();
        cy.get('[data-cy="skillsSearchInput"]').type('skill 3');

        cy.get('[data-cy="skillProgressTitle-skill1"]').should('not.exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('not.exist');
        cy.get('[data-cy="skillProgressTitle-skill3"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill3"]').should('contain', 'Very Great Skill 3');

        // Search for term that matches multiple skills
        cy.get('[data-cy="skillsSearchInput"]').clear();
        cy.get('[data-cy="skillsSearchInput"]').type('skill');

        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill3"]').should('exist');

        // Search for non-existent term - should show no results
        cy.get('[data-cy="skillsSearchInput"]').clear();
        cy.get('[data-cy="skillsSearchInput"]').type('NonExistentSkill');

        cy.get('[data-cy="skillProgressTitle-skill1"]').should('not.exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('not.exist');
        cy.get('[data-cy="skillProgressTitle-skill3"]').should('not.exist');
        cy.get('[data-cy="noContent"]').should('be.visible');
        cy.get('[data-cy="noContent"]').should('contain', 'No results');
    });

    it('navigate to Skills Group page from subject page via group title and progress bar', () => {
        const groupDescription = "Test group description for navigation";
        
        // Create a skills group with skills
        cy.createSkillsGroup(1, 1, 1, {
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 150,
            numPerformToCompletion: 1
        });

        // Navigate to subject page first
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1');


        // Verify we're on the subject page and can see the group
        cy.get('[data-cy="skillsTitle"]').contains('Subject 1')

        // Test navigation via group title click
        cy.get('[data-cy="skillProgressTitle-group1"] [data-cy="skillProgressTitle"]').click();
        
        // Verify we're on the group page
        cy.url().should('include', '/subjects/subj1/groups/group1');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Group Overview');
        cy.get('[data-cy="skillsGroupName"]').should('contain', 'Awesome Group 1');
        cy.get('[data-cy="skillsGroupDescription"]').should('contain', groupDescription);

        // Verify skills are displayed on the group page
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');

        // Navigate back to subject page
        cy.go('back');
        cy.get('[data-cy="skillsTitle"]').contains('Subject 1')

        // Test navigation via group progress bar click
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressBar"]').first().click();

        // Verify we're on the group page again
        cy.url().should('include', '/subjects/subj1/groups/group1');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Group Overview');
        cy.get('[data-cy="skillsGroupName"]').should('contain', 'Awesome Group 1');

        // Verify skills are displayed on the group page
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');
    });

    it('navigate between skill pages using the next button', () => {
        const groupDescription = "Test group for skill navigation";

        cy.createSkill(1, 1, 1)
        // Create first group with 1 skill
        cy.createSkillsGroup(1, 1, 1, {
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });

        // Create second group with 2 skills
        cy.createSkillsGroup(1, 1, 2, {
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 2, 3, {
            pointIncrement: 150,
            numPerformToCompletion: 1
        });
        cy.addSkillToGroup(1, 1, 2, 4, {
            pointIncrement: 200,
            numPerformToCompletion: 1
        });

        cy.createSkill(1, 1, 5)

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill1"]').contains('Very Great Skill 1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group1]').should('not.exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').should('not.exist');
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 5')

        // Navigate to next skill using next button
        cy.get('[data-cy="prevSkill"]').should('be.disabled')
        cy.get('[data-cy="nextSkill"]').click();
        
        // Verify we're on second skill and breadcrumb updated
        cy.url().should('include', '/subjects/subj1/groups/group1/skills/skill2');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill2"]').contains('Very Great Skill 2');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill2]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').contains('Awesome Group 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 5')

        // Navigate to next skill using previous button
        cy.get('[data-cy="prevSkill"]').should('be.enabled')
        cy.get('[data-cy="nextSkill"]').click();

        // Verify we're back to first skill and breadcrumb updated
        cy.url().should('include', '/subjects/subj1/groups/group2/skills/skill3');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group2]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill3]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').contains('Awesome Group 2')
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 5')

        // Navigate to next skill
        cy.get('[data-cy="prevSkill"]').should('be.enabled')
        cy.get('[data-cy="nextSkill"]').click();

        // verify
        cy.url().should('include', '/subjects/subj1/groups/group2/skills/skill4');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill4"]').contains('Very Great Skill 4');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group2]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill4]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').contains('Awesome Group 2')
        cy.get('[data-cy="skillOrder"]').contains('Skill 4 of 5')

        // Navigate to next skill
        cy.get('[data-cy="prevSkill"]').should('be.enabled')
        cy.get('[data-cy="nextSkill"]').click();

        // verify
        cy.url().should('include', '/subjects/subj1/skills/skill5');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill5"]').contains('Very Great Skill 5');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group2]').should('not.exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill5]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').should('not.exist');
        cy.get('[data-cy="skillOrder"]').contains('Skill 5 of 5')

        cy.get('[data-cy="prevSkill"]').should('be.enabled')
        cy.get('[data-cy="nextSkill"]').should('be.disabled')

    });

    it('navigate between skill pages using the previous button', () => {
        const groupDescription = "Test group for skill navigation";

        cy.createSkill(1, 1, 1)
        // Create first group with 1 skill
        cy.createSkillsGroup(1, 1, 1, {
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });

        // Create second group with 2 skills
        cy.createSkillsGroup(1, 1, 2, {
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 2, 3, {
            pointIncrement: 150,
            numPerformToCompletion: 1
        });
        cy.addSkillToGroup(1, 1, 2, 4, {
            pointIncrement: 200,
            numPerformToCompletion: 1
        });

        cy.createSkill(1, 1, 5)

        // Start from the last skill
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill5');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill5"]').contains('Very Great Skill 5');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group2]').should('not.exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill5]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').should('not.exist');
        cy.get('[data-cy="skillOrder"]').contains('Skill 5 of 5')

        // Navigate to previous skill using previous button
        cy.get('[data-cy="prevSkill"]').should('be.enabled')
        cy.get('[data-cy="nextSkill"]').should('be.disabled')
        cy.get('[data-cy="prevSkill"]').click();
        
        // Verify we're on fourth skill and breadcrumb updated
        cy.url().should('include', '/subjects/subj1/groups/group2/skills/skill4');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill4"]').contains('Very Great Skill 4');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group2]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill4]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').contains('Awesome Group 2')
        cy.get('[data-cy="skillOrder"]').contains('Skill 4 of 5')

        // Navigate to previous skill using previous button
        cy.get('[data-cy="prevSkill"]').should('be.enabled')
        cy.get('[data-cy="nextSkill"]').should('be.enabled')
        cy.get('[data-cy="prevSkill"]').click();
        
        // Verify we're on third skill and breadcrumb updated
        cy.url().should('include', '/subjects/subj1/groups/group2/skills/skill3');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill3"]').contains('Very Great Skill 3');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group2]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill3]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').contains('Awesome Group 2')
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 5')

        // Navigate to previous skill using previous button
        cy.get('[data-cy="prevSkill"]').should('be.enabled')
        cy.get('[data-cy="nextSkill"]').should('be.enabled')
        cy.get('[data-cy="prevSkill"]').click();
        
        // Verify we're on second skill and breadcrumb updated
        cy.url().should('include', '/subjects/subj1/groups/group1/skills/skill2');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill2"]').contains('Very Great Skill 2');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill2]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').contains('Awesome Group 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 5')

        // Navigate to previous skill using previous button
        cy.get('[data-cy="prevSkill"]').should('be.enabled')
        cy.get('[data-cy="nextSkill"]').should('be.enabled')
        cy.get('[data-cy="prevSkill"]').click();
        
        // Verify we're on first skill and breadcrumb updated
        cy.url().should('include', '/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill1"]').contains('Very Great Skill 1');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group1]').should('not.exist');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]').should('be.visible');
        cy.get('[data-cy="groupInformationSection"]').should('not.exist');
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 5')

        // Verify button states at the first skill
        cy.get('[data-cy="prevSkill"]').should('be.disabled')
        cy.get('[data-cy="nextSkill"]').should('be.enabled')
    });

    it('navigate from skill page to parent group by clicking on group name', () => {
        const groupDescription = "Test group for skill-to-group navigation";
        
        // Create a skills group with skills
        cy.createSkillsGroup(1, 1, 1, {
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 150,
            numPerformToCompletion: 1
        });

        // Navigate directly to a skill page within the group
        cy.cdVisit('/subjects/subj1/groups/group1/skills/skill1');

        // Verify we're on the skill page
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('contain', 'Very Great Skill 1');

        // Verify group information is displayed
        cy.get('[data-cy="groupInformationSection"] [data-cy="groupName"]').should('contain', 'Awesome Group 1');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]').should('be.visible');

        // Click on the group name link to navigate to the parent group
        cy.get('[data-cy="groupInformationSection"] [data-cy="groupName"]').click();

        // Verify we're now on the group page
        cy.url().should('include', '/subjects/subj1/groups/group1');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Group Overview');
        cy.get('[data-cy="skillsGroupName"]').should('contain', 'Awesome Group 1');
        cy.get('[data-cy="skillsGroupDescription"]').should('contain', groupDescription);

        // Verify both skills are displayed on the group page
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]').should('not.exist');
    });

    it('navigate from skill page to parent group by clicking on group breadcrumb', () => {
        const groupDescription = "Test group for skill-to-group navigation";

        // Create a skills group with skills
        cy.createSkillsGroup(1, 1, 1, {
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 150,
            numPerformToCompletion: 1
        });

        // Navigate directly to a skill page within the group
        cy.cdVisit('/subjects/subj1/groups/group1/skills/skill1');

        // Verify we're on the skill page
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Skill Overview');
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('contain', 'Very Great Skill 1');

        // Verify group information is displayed
        cy.get('[data-cy="groupInformationSection"] [data-cy="groupName"]').should('contain', 'Awesome Group 1');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]').should('be.visible');

        // Click on the group breadcrumb item
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group1]').click();

        // Verify we're now on the group page
        cy.url().should('include', '/subjects/subj1/groups/group1');
        cy.get('[data-cy="skillsTitle"]').should('contain', 'Group Overview');
        cy.get('[data-cy="skillsGroupName"]').should('contain', 'Awesome Group 1');
        cy.get('[data-cy="skillsGroupDescription"]').should('contain', groupDescription);

        // Verify both skills are displayed on the group page
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');

        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-proj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-subj1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-group1]').should('be.visible');
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumb-skill1]').should('not.exist');
    });

    it('logout, navigate directly to Skills Group page, login and validate page rendering', () => {
        const groupDescription = "This is a test skills group description for logout/login flow";
        
        // Create a skills group with at least 2 skills and a description
        cy.createSkillsGroup(1, 1, 1, {
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 150,
            numPerformToCompletion: 1
        });

        // Add some progress to skills
        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');

        // Logout first
        cy.logout();

        // Navigate directly to the Skills Group page while logged out
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/groups/group1');

        // Should be redirected to login page - wait for redirect to complete
        cy.location('pathname', { timeout: 5000 }).should('include', 'skills-login');
        cy.get('[data-cy="login"]').should('be.disabled')
        
        // Login using the page's form
        cy.fixture('vars.json').then((vars) => {
            cy.get('#username')
                .type(vars.defaultUser);
            cy.get('#inputPassword')
                .type(vars.defaultPass);
            cy.get('[data-cy=login]')
                .click();
        });

        // Should be redirected back to the Skills Group page after login
        cy.url().should('include', '/subjects/subj1/groups/group1');

        // Validate the group page properly rendered
        cy.get('[data-cy="skillsTitle"]').contains('Group Overview');
        
        // Validate group name and progress information
        cy.get('[data-cy="skillsGroupName"]').should('contain', 'Awesome Group 1');
        cy.get('[data-cy="skillsGroupProgress"]').contains(/1 \/ 2 Skills/);

        // Validate group description is displayed
        cy.get('[data-cy="skillsGroupDescription"]').contains(groupDescription);

        // Validate skills progress list is displayed
        cy.get('[data-cy="skillsProgressList"]').should('exist');

        // Validate at least 2 skills are displayed under the group
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill1"] [data-cy="skillProgressTitle"]')
            .should('contain', 'Very Great Skill 1');
        cy.get('[data-cy="skillProgressTitle-skill1"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .should('contain', '100 / 200 Points');

        cy.get('[data-cy="skillProgressTitle-skill2"]').should('exist');
        cy.get('[data-cy="skillProgressTitle-skill2"] [data-cy="skillProgressTitle"]')
            .should('contain', 'Very Great Skill 2');
        cy.get('[data-cy="skillProgressTitle-skill2"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .should('contain', '150 / 150 Points');
    });


})