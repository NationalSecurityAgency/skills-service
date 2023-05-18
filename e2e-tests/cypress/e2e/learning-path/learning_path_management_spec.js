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
describe('Learning Path Management Validation Tests', () => {

    const tableSelector = '[data-cy="learningPathTable"]';
    const headerSelector = `${tableSelector} thead tr th`;

    beforeEach(() => {
        Cypress.Commands.add('clickOnNode', (x, y) => {
            cy.contains('Legend');
            cy.wait(2000); // wait for chart
            // have to click twice to it to work...
            cy.get('#dependency-graph canvas')
                .should('be.visible')
                .click(x, y);
        });

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 2, 5)
        cy.createSkill(1, 2, 6)
        cy.createSkill(1, 2, 7)
        cy.createSkill(1, 2, 8)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 3);
        cy.assignSkillToBadge(1, 2, 4);
        cy.createBadge(1, 2, { enabled: true });
    });

    it('Create a simple learning path', () => {
        cy.visit('/administrator/projects/proj1/learning-path')

        // Add Badge1 as a prerequisite for Badge2
        cy.get('[data-cy="learningPathFromSkillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-badge1"]').click();
        cy.get('[data-cy="learningPathToSkillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-badge2"]').click();
        cy.get('[data-cy="addLearningPathItemBtn"]').click();

        // Add Skill5 as a prerequisite for Badge1
        cy.get('[data-cy="learningPathFromSkillSelector"]')
            .click();

        cy.get('[data-cy="skillsSelectionItem-proj1-skill5Subj2"]').click();
        cy.get('[data-cy="learningPathToSkillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-badge1"]').click();
        cy.get('[data-cy="addLearningPathItemBtn"]').click();

        // Add Skill6 as a prerequisite for Badge2
        cy.get('[data-cy="learningPathFromSkillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-skill6Subj2"]').click();
        cy.get('[data-cy="learningPathToSkillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-badge2"]').click();
        cy.get('[data-cy="addLearningPathItemBtn"]').click();

        // Add Skill7 as a prerequisite for Skill5
        cy.get('[data-cy="learningPathFromSkillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-skill7Subj2"]').click();
        cy.get('[data-cy="learningPathToSkillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-skill5Subj2"]').click();
        cy.get('[data-cy="addLearningPathItemBtn"]').click();

        // Add Skill8 as a prerequisite for Badge1
        cy.get('[data-cy="learningPathFromSkillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-skill8Subj2"]').click();
        cy.get('[data-cy="learningPathToSkillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-badge1"]').click();
        cy.get('[data-cy="addLearningPathItemBtn"]').click();

        cy.get(headerSelector).contains('From').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Badge 1'
            }, {
                colIndex: 1,
                value: 'Badge 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 5 Subj2'
            }, {
                colIndex: 1,
                value: 'Badge 1'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 6 Subj2'
            }, {
                colIndex: 1,
                value: 'Badge 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 7 Subj2'
            }, {
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 8 Subj2'
            }, {
                colIndex: 1,
                value: 'Badge 1'
            }],
        ], 5, false, null, false);

        // Remove the connection from Badge 1 and Badge 2
        cy.get('[data-cy="sharedSkillsTable-removeBtn"]').first().click();
        cy.get('button').contains('Remove').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 5 Subj2'
            }, {
                colIndex: 1,
                value: 'Badge 1'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 6 Subj2'
            }, {
                colIndex: 1,
                value: 'Badge 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 7 Subj2'
            }, {
                colIndex: 1,
                value: 'Very Great Skill 5 Subj2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 8 Subj2'
            }, {
                colIndex: 1,
                value: 'Badge 1'
            }],
        ], 5, false, null, false);

    })
});