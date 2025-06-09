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

describe('Training Keyboard Shortcuts Tests', () => {

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
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]').should('have.text', 'Project: This is project 1');

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
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]').should('have.text', 'Project: This is project 1');

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.get('li.p-listbox-empty-message').should('not.exist')
        cy.get('input.p-listbox-filter').type('xyz')
        cy.get('li.p-listbox-empty-message').contains('No results found').should('be.visible')
    })

    it('no results for empty project in training-wide search dialog', () => {
        cy.createProject(1)

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]').should('have.text', 'Project: This is project 1');

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
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]').should('have.text', 'Project: This is project 1');

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
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillsTitle"]').should('have.text', 'Project: This is project 1');

        cy.get('[data-cy="trainingSearchDialog"]').should('not.exist')
        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
        cy.realPress('Escape');
        cy.get('[data-cy="trainingSearchDialog"]').should('not.exist')

        cy.get('[data-cy="skillsDisplaySearchBtn"]').click()
        cy.get('[data-cy="trainingSearchDialog"]').should('be.visible')
    })

})