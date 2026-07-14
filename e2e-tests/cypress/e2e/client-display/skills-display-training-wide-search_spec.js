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

describe('Skills Display Training Wide Search Tests', () => {

    it('navigate to skills, subjects, and badges using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });


        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"] [data-cy="title"]').should('have.text', 'Project: This is project 1');

        // navigate to subject 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('subject 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Subject 1');
        cy.realPress('Enter');
        cy.get('[data-cy="skillsTitle"]').contains('Subject 1')

        // navigate to skill 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('skill 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Very Great Skill 1');
        cy.realPress('Enter');
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')

        // navigate to badge 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('badge 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Badge 1');
        cy.realPress('Enter');
        cy.get('[data-cy="badgeTitle"]').contains('Badge 1')
    })

    it('client-display: navigate to skills, subjects, and badges using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });


        cy.cdVisit('/');
        cy.contains('Overall Point');

        // navigate to subject 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('subject 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Subject 1');
        cy.realPress('Enter');
        cy.get('[data-cy="skillsTitle"]').contains('Subject 1')

        // navigate to skill 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('skill 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Very Great Skill 1');
        cy.realPress('Enter');
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')

        // navigate to badge 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('badge 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Badge 1');
        cy.realPress('Enter');
        cy.get('[data-cy="badgeTitle"]').contains('Badge 1')
    })

    it('navigate from one subject to another using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
          numPerformToCompletion: 1,
          description: 'This is skill1 - Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        })
        cy.createSkill(1, 1, 2, {
          numPerformToCompletion: 1,
          description: 'This is skill2 - Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        })
        cy.createSkill(1, 1, 3, {
          numPerformToCompletion: 1,
          description: 'This is skill3 - Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        })

        cy.createSubject(1, 2)
        cy.createSkill(1, 2, 1)
        cy.createSkill(1, 2, 2)

        cy.addTagToSkills(1, ['skill1', 'skill3'], 1)
        cy.addTagToSkills(1, ['skill2', 'skill3', 'skill1Subj2'], 2)

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"] [data-cy="title"]').should('have.text', 'Project: This is project 1');

        // navigate to subject 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('subject 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Subject 1');
        cy.realPress('Enter');
        cy.get('[data-cy="skillsTitle"]').contains('Subject 1')
        cy.get('[data-cy="skillTags"]')
          .should('be.visible')

        cy.get('[data-cy="tagLink-tag1"]')
          .should('be.visible')
          .and('have.attr', 'href', '/progress-and-rankings/projects/proj1/tags/tag1')
        cy.get('[data-cy="tagLink-tag1"] [data-cy="tagName"]').should('have.text', 'TAG 1')
        cy.get('[data-cy="tagLink-tag1"] [data-cy="numSkills"]').should('have.text', '2')

        cy.get('[data-cy="tagLink-tag2"]')
          .should('be.visible')
          .and('have.attr', 'href', '/progress-and-rankings/projects/proj1/tags/tag2')
        cy.get('[data-cy="tagLink-tag2"] [data-cy="tagName"]').should('have.text', 'TAG 2')
        cy.get('[data-cy="tagLink-tag2"] [data-cy="numSkills"]').should('have.text', '2')

        // navigate to subject 2
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('subject 2')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Subject 2');
        cy.realPress('Enter');
        cy.get('[data-cy="skillsTitle"]').contains('Subject 2')
        cy.get('[data-cy="skillTags"]')
          .should('be.visible')

        cy.get('[data-cy="tagLink-tag1"]')
          .should('not.exist')

        cy.get('[data-cy="tagLink-tag2"]')
          .should('be.visible')
          .and('have.attr', 'href', '/progress-and-rankings/projects/proj1/tags/tag2')
        cy.get('[data-cy="tagLink-tag2"] [data-cy="tagName"]').should('have.text', 'TAG 2')
        cy.get('[data-cy="tagLink-tag2"] [data-cy="numSkills"]').should('have.text', '1')
    })

    it('navigate from one skill to another using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { pointIncrement: 111 })
        cy.createSubject(1, 2)
        cy.createSkill(1, 2, 3, { pointIncrement: 112 })

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillProgressTitle-skill1"]').contains('Very Great Skill 1')
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]').should('have.text', '111 Increment');

        // navigate to another skill
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('skill 3')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Skill 3');
        cy.realPress('Enter');
        cy.get('[data-cy="skillProgressTitle-skill3Subj2"]').contains('Very Great Skill 3')
        cy.get('[data-cy="pointsPerOccurrenceCard"] [data-cy="mediaInfoCardTitle"]').should('have.text', '112 Increment');
    })

    it('navigate from one skills group to another using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkillsGroup(1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 2)

        cy.createSkillsGroup(1, 1, 2)
        cy.addSkillToGroup(1, 1, 2, 3)

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/groups/group1');
        cy.get('[data-cy="skillsGroupName"]').contains('Awesome Group 1')
        cy.get('[data-cy="skillProgressTitle-skill1"]')
        cy.get('[data-cy="skillProgressTitle-skill2"]')
        cy.get('[data-cy="skillProgressTitle-skill3"]').should('not.exist')


        // navigate to badge 2
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('group 2')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Group 2');
        cy.realPress('Enter');
        cy.get('[data-cy="skillsGroupName"]').contains('Awesome Group 2')
        cy.get('[data-cy="skillProgressTitle-skill1"]').should('not.exist')
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('not.exist')
        cy.get('[data-cy="skillProgressTitle-skill3"]')
    })

    it('navigate from one badge to another using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1 , { enabled: true });
        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 2);
        cy.createBadge(1, 2 , { enabled: true });


        cy.visit('/progress-and-rankings/projects/proj1/badges/badge1');
        cy.get('[data-cy="badgeTitle"]').contains('Badge 1')
        cy.get('[data-cy="skillProgressTitle-skill1"]')

        // navigate to badge 2
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('badge 2')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Badge 2');
        cy.realPress('Enter');
        cy.get('[data-cy="badgeTitle"]').contains('Badge 2')
        cy.get('[data-cy="skillProgressTitle-skill2"]')
    })

    it('no results after filtering training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"] [data-cy="title"]').should('have.text', 'Project: This is project 1');

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('li.p-listbox-empty-message').should('not.exist')
        cy.get('input.p-listbox-filter').type('xyz')
        cy.get('li.p-listbox-empty-message').contains('No results found').should('be.visible')
    })

    it('no results for empty project in training-wide search dialog', () => {
        cy.createProject(1)

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"] [data-cy="title"]').should('have.text', 'Project: This is project 1');

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('li.p-listbox-empty-message').contains('No results found').should('be.visible')
    })

    it('navigate using the mouse and keyboard', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"] [data-cy="title"]').should('have.text', 'Project: This is project 1');

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('subject')
        cy.get('[data-cy="searchRes-subj1"]').click()
        cy.get('[data-cy="skillsTitle"]').contains('Subject 1')

        cy.realPress(["Control", "k"]);
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('skill 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Very Great Skill 1');
        cy.realPress('Enter');
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3')
    })

    it('close training-wide search dialog', () => {
        cy.createProject(1)

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"] [data-cy="title"]').should('have.text', 'Project: This is project 1');

        cy.get('[data-cy="trainingSearchDialog"]').should('not.exist')
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.realPress('Escape');
        cy.get('[data-cy="trainingSearchDialog"]').should('not.exist')

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
    })

    it('point and skill counts on search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, { pointIncrement: 111 })
        cy.createSkill(1, 1, 2, { pointIncrement: 112, numPerformToCompletion: 1 })
        cy.createSkill(1, 1, 3, { pointIncrement: 113, numPerformToCompletion: 1 })

        cy.createSubject(1, 2)
        cy.createSkill(1, 2, 4)

        cy.createSubject(1, 3)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 1);
        cy.assignSkillToBadge(1, 2, 2);
        cy.createBadge(1, 2, { enabled: true });

        cy.createBadge(1, 3);

        cy.createSkillsGroup(1, 1, 10)
        cy.addSkillToGroup(1, 1, 10, 11, { numPerformToCompletion: 1})
        cy.addSkillToGroup(1, 1, 10, 12, { pointIncrement: 3333 })

        cy.createSkillsGroup(1, 1, 20)
        cy.addSkillToGroup(1, 1, 20, 13, { numPerformToCompletion: 1})

        cy.createSkillsGroup(1, 1, 30)

        cy.reportSkill(1, 1, Cypress.env('proxyUser'))
        cy.reportSkill(1, 2, Cypress.env('proxyUser'))
        cy.reportSkill(1, 11, Cypress.env('proxyUser'))
        cy.reportSkill(1, 12, Cypress.env('proxyUser'))

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"] [data-cy="title"]').should('have.text', 'Project: This is project 1');

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')

        // subject counts
        cy.get('input.p-listbox-filter').type('subject')
        cy.get('[data-cy="searchRes-subj1"] [data-cy="points"]').should('have.text', '2 / 6 Skills')
        cy.get('[data-cy="searchRes-subj2"] [data-cy="points"]').should('have.text', '0 / 1 Skill')
        cy.get('[data-cy="searchRes-subj3"] [data-cy="points"]').should('have.text', '0 / 0 Skills')

        // badge counts
        cy.get('input.p-listbox-filter').type('{selectAll}adge')
        cy.get('[data-cy="searchRes-badge1"] [data-cy="points"]').should('have.text', '0 / 1 Skill')
        cy.get('[data-cy="searchRes-badge2"] [data-cy="points"]').should('have.text', '1 / 2 Skills')
        cy.get('[data-cy="searchRes-badge3"]').should('not.exist' )

        // group counts
        cy.get('input.p-listbox-filter').type('{selectAll}ROUP')
        cy.get('[data-cy="searchRes-group10"] [data-cy="points"]').should('have.text', '1 / 2 Skills')
        cy.get('[data-cy="searchRes-group20"] [data-cy="points"]').should('have.text', '0 / 1 Skill')
        cy.get('[data-cy="searchRes-group30"] [data-cy="points"]').should('have.text', '0 / 0 Skills')

        // skills
        // group counts
        cy.get('input.p-listbox-filter').type('{selectAll}sKIll 1')
        cy.get('[data-cy="searchRes-skill1"] [data-cy="points"]').should('have.text', '111 / 222 Points')
        cy.get('[data-cy="searchRes-skill11"] [data-cy="points"]').should('have.text', '100 / 100 Points')
        cy.get('[data-cy="searchRes-skill12"] [data-cy="points"]').should('have.text', '3,333 / 6,666 Points')
        cy.get('[data-cy="searchRes-skill13"] [data-cy="points"]').should('have.text', '0 / 100 Points')
    })

    it('search dialog honors custom labels', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'Course',
                setting: 'subject.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Assessment',
                setting: 'skill.displayName',
                projectId: 'proj1',
            },
            {
                value: 'Cluster',
                setting: 'group.displayName',
                projectId: 'proj1',
            },
        ]);

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()

        cy.get('input.p-listbox-filter')
          .invoke('attr', 'placeholder')
          .should('contain', 'Search for Courses, Assessments, Clusters or Badges');
        cy.get('[data-cy="subjectName"]').first().should('have.text', "Course:Subject 1");
    });

    it('client-display: training-wide search dialog is visible and displayed with position=top when in client-display iframe', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/test-skills-client/proj1')
        cy.wrapIframe().contains('Overall Points');

        cy.wrapIframe().find('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.wait(1000)
        cy.wrapIframe().find('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.wrapIframe().find('[data-cy="trainingSearchDialog"]').first().then(($el) => {
            const bounding = $el[0].getBoundingClientRect();
            const windowWidth = Cypress.config('viewportWidth');
            const windowHeight = Cypress.config('viewportHeight');

            expect(bounding.top).to.be.gte(0);
            expect(bounding.left).to.be.gte(0);
            expect(bounding.right).to.be.lte(windowWidth);
            expect(bounding.bottom).to.be.lte(windowHeight);
        });
    })

    it('training-wide search dialog is visible and displayed with position=center when not in client-display iframe', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.createBadge(1, 1, { enabled: true });

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.contains('Overall Points');

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.wait(1000)
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('[data-cy="trainingSearchDialog"]').first().then(($el) => {
            const bounding = $el[0].getBoundingClientRect();
            const windowWidth = Cypress.config('viewportWidth');
            const windowHeight = Cypress.config('viewportHeight');

            expect(bounding.top).to.be.gte(0);
            expect(bounding.left).to.be.gte(0);
            expect(bounding.right).to.be.lte(windowWidth);
            expect(bounding.bottom).to.be.lte(windowHeight);
        });
    })

    it('navigate to skills group using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkillsGroup(1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 2)
        cy.addSkillToGroup(1, 1, 1, 3)

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"] [data-cy="title"]').should('have.text', 'Project: This is project 1');

        // navigate to skills group 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('awesome group 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Awesome Group 1');
        cy.realPress('Enter');
        cy.url().should('include', '/subjects/subj1/groups/group1');
        cy.get('[data-cy="skillsGroupName"]').contains('Awesome Group 1')
        cy.get('[data-cy="skillsGroupProgress"]').contains(/0 \/ 3 Skills/)
    })

    it('client-display: navigate to skills group using training-wide search dialog', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkillsGroup(1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 1)
        cy.addSkillToGroup(1, 1, 1, 2)
        cy.addSkillToGroup(1, 1, 1, 3)

        cy.cdVisit('/');
        cy.contains('Overall Point');

        // navigate to skills group 1
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('input.p-listbox-filter').type('awesome group 1')
        cy.realPress('ArrowDown');
        cy.get('li.p-listbox-option[data-p-focused="true"]').should('contain.text', 'Awesome Group 1');
        cy.realPress('Enter');
        cy.get('[data-cy="skillsGroupName"]').contains('Awesome Group 1')
        cy.get('[data-cy="skillsGroupProgress"]').contains(/0 \/ 3 Skills/)
    })
})