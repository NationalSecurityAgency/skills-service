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

    const visitLearningPath = () => {
        cy.visit('/administrator/projects/proj1/learning-path')
        cy.wait('@loadSharedSkills')
    }

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)

        cy.intercept('GET', '/admin/projects/proj1/sharedWithMe').as('loadSharedSkills');
    });

    Cypress.Commands.add('selectSkill', (selector, skillId, searchString = '', projId='proj1') => {
        cy.get(selector).blur({force: true})
        cy.get(selector).click()
        if (searchString) {
            cy.get(selector).type(searchString)
        }
        cy.get(`[data-cy="skillsSelectionItem-${projId}-${skillId}"]`).click()
    })

    it('Simple Skill Circular Path', () => {
        cy.createSkill(1, 1, 4)
        cy.addLearningPathItem(1, 1, 2)
        cy.addLearningPathItem(1, 2, 3)
        visitLearningPath()

        cy.selectSkill('[data-cy="learningPathFromSkillSelector"]', 'skill3')

        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'skill1');
        cy.get('[data-cy="learningPathError"]').contains('Very Great Skill 1 already exists in the learning path and adding it again will cause a circular/infinite learning path')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.disabled')

        cy.get('[data-cy="learningPathToSkillSelector"]').type('{backspace}')
        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'skill4');
        cy.get('[data-cy="learningPathError"]').should('not.exist')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.enabled')
    })

    it('Cannot add a skill to the learning path if the skill already exist with in a badge in that same learning path', () => {
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)
        cy.createSkill(1, 1, 6)
        cy.createSkill(1, 1, 7)
        cy.createSkill(1, 1, 8)
        cy.createSkill(1, 1, 9)

        cy.createSkill(1, 1, 11)
        cy.createSkill(1, 1, 12)
        cy.createSkill(1, 1, 13)
        cy.createSkill(1, 1, 14)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 11);
        cy.assignSkillToBadge(1, 1, 12);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 13);
        cy.assignSkillToBadge(1, 2, 14);
        cy.createBadge(1, 2, { enabled: true });

        cy.addLearningPathItem(1, 1, 2)
        cy.addLearningPathItem(1, 2, 1, false, true)
        cy.addLearningPathItem(1, 1, 4, true, false)
        cy.addLearningPathItem(1, 1, 5, true, false)
        cy.addLearningPathItem(1, 1, 6, true, false)
        cy.addLearningPathItem(1, 4, 7)
        cy.addLearningPathItem(1, 4, 8)
        cy.addLearningPathItem(1, 8, 2, false, true)
        cy.addLearningPathItem(1, 2, 9, true, false)

        visitLearningPath()

        cy.selectSkill('[data-cy="learningPathFromSkillSelector"]', 'skill9')
        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'skill11');
        cy.get('[data-cy="learningPathError"]').contains('Very Great Skill 11 already exists in the learning path under the badge Badge 1 and adding it again will cause a circular/infinite learning path')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.disabled')
    });

    it('error is reset when from or to skill is cleared', () => {
        cy.addLearningPathItem(1, 1, 2)
        visitLearningPath()

        cy.selectSkill('[data-cy="learningPathFromSkillSelector"]', 'skill2')
        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'skill1');
        cy.get('[data-cy="learningPathError"]').contains('Very Great Skill 1 already exists in the learning path and adding it again will cause a circular/infinite learning path')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.disabled')

        cy.get('[data-cy="learningPathToSkillSelector"]').type('{selectall}{backspace}')
        cy.get('[data-cy="learningPathError"]').should('not.exist')

        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'skill1', '1');
        cy.get('[data-cy="learningPathError"]').contains('Very Great Skill 1 already exists in the learning path and adding it again will cause a circular/infinite learning path')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.disabled')

        cy.get('[data-cy="learningPathFromSkillSelector"]').type('{selectall}{backspace}')
        cy.get('[data-cy="learningPathError"]').should('not.exist')
    })

    it('Cannot add a badge that has overlapping skills of a badge in the learning path', () => {
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)
        cy.createSkill(1, 1, 6)
        cy.createSkill(1, 1, 7)
        cy.createSkill(1, 1, 8)
        cy.createSkill(1, 1, 9)

        cy.createSkill(1, 1, 11)
        cy.createSkill(1, 1, 12)
        cy.createSkill(1, 1, 13)
        cy.createSkill(1, 1, 14)
        cy.createSkill(1, 1, 15)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 11);
        cy.assignSkillToBadge(1, 1, 12);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 13);
        cy.assignSkillToBadge(1, 2, 14);
        cy.createBadge(1, 2, { enabled: true });

        cy.createBadge(1, 3);
        cy.assignSkillToBadge(1, 3, 12);
        cy.assignSkillToBadge(1, 3, 15);
        cy.createBadge(1, 3, { enabled: true });

        cy.addLearningPathItem(1, 1, 2)
        cy.addLearningPathItem(1, 2, 1, false, true)
        cy.addLearningPathItem(1, 1, 4, true, false)
        cy.addLearningPathItem(1, 1, 5, true, false)
        cy.addLearningPathItem(1, 1, 6, true, false)
        cy.addLearningPathItem(1, 4, 7)
        cy.addLearningPathItem(1, 4, 8)
        cy.addLearningPathItem(1, 8, 2, false, true)
        cy.addLearningPathItem(1, 2, 9, true, false)

        visitLearningPath()

        cy.selectSkill('[data-cy="learningPathFromSkillSelector"]', 'skill9')
        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'badge3');
        cy.get('[data-cy="learningPathError"]').contains('Multiple badges on the same Learning path cannot have overlapping skills. Both Badge 1 badge and Badge 3 badge have Very Great Skill 12 skill')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.disabled')
    });

    it('Cannot add a badge that already has one of its skills on the learning path', () => {
        cy.createSkill(1, 1, 4)
        cy.createSkill(1, 1, 5)
        cy.createSkill(1, 1, 6)
        cy.createSkill(1, 1, 7)
        cy.createSkill(1, 1, 8)
        cy.createSkill(1, 1, 9)

        cy.createSkill(1, 1, 11)
        cy.createSkill(1, 1, 12)
        cy.createSkill(1, 1, 13)
        cy.createSkill(1, 1, 14)
        cy.createSkill(1, 1, 15)

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 11);
        cy.assignSkillToBadge(1, 1, 12);
        cy.createBadge(1, 1, { enabled: true });

        cy.createBadge(1, 2);
        cy.assignSkillToBadge(1, 2, 13);
        cy.assignSkillToBadge(1, 2, 14);
        cy.createBadge(1, 2, { enabled: true });

        cy.createBadge(1, 3);
        cy.assignSkillToBadge(1, 3, 2);
        cy.assignSkillToBadge(1, 3, 15);
        cy.createBadge(1, 3, { enabled: true });

        cy.addLearningPathItem(1, 1, 2)
        cy.addLearningPathItem(1, 2, 1, false, true)
        cy.addLearningPathItem(1, 1, 4, true, false)
        cy.addLearningPathItem(1, 1, 5, true, false)
        cy.addLearningPathItem(1, 1, 6, true, false)
        cy.addLearningPathItem(1, 4, 7)
        cy.addLearningPathItem(1, 4, 8)
        cy.addLearningPathItem(1, 8, 2, false, true)
        cy.addLearningPathItem(1, 2, 9, true, false)

        visitLearningPath()

        cy.selectSkill('[data-cy="learningPathFromSkillSelector"]', 'skill9')
        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'badge3');

        cy.get('[data-cy="learningPathError"]').contains('Provided badge Badge 3 has skill Very Great Skill 2 which already exists on the learning path')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.disabled')
    });

    it('Cannot add path item which is already present on the path', () => {
        cy.createSkill(1, 1, 4)
        cy.addLearningPathItem(1, 1, 2)
        cy.addLearningPathItem(1, 2, 3)
        visitLearningPath()

        cy.selectSkill('[data-cy="learningPathFromSkillSelector"]', 'skill1')
        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'skill2');
        cy.get('[data-cy="learningPathError"]').contains('Learning path from Very Great Skill 1 to Very Great Skill 2 already exists')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.disabled')
    })

    it('exported skills cannot be added learning path', () => {
        cy.exportSkillToCatalog(1, 1, 1);
        visitLearningPath()

        cy.selectSkill('[data-cy="learningPathFromSkillSelector"]', 'skill2')
        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'skill1');
        cy.get('[data-cy="learningPathError"]').contains('Skill Very Great Skill 1 was exported to the Skills Catalog. A skill in the catalog cannot have prerequisites on the learning path')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.disabled')
    })

    it('reused skills cannot be added learning path', () => {
        cy.createSubject(1,2)
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        visitLearningPath()

        cy.selectSkill('[data-cy="learningPathFromSkillSelector"]', 'skill2')
        cy.selectSkill('[data-cy="learningPathToSkillSelector"]', 'skill1');
        cy.get('[data-cy="learningPathError"]').contains('Skill Very Great Skill 1 was reused in another subject or group and cannot have prerequisites in the learning path')
        cy.get('[data-cy="addLearningPathItemBtn"]').should('be.disabled')
    })

    it('adding a skill to a badge violates Learning Path', () => {
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.assignSkillToBadge(1, 1, 2);
        cy.createBadge(1, 1, { enabled: true });

        cy.addLearningPathItem(1, 3, 1, false, true)
        cy.visit('/administrator/projects/proj1/badges/badge1/')
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill3')
        cy.get('[data-cy="learningPathErrMsg"]').contains(' Failed to add Very Great Skill 3 skill to the badge. Adding this skill would result in a circular/infinite learning path')
        cy.get('[data-cy="learningPathErrMsg"] [data-cy="learningPathLink"]').click()
        cy.get('[data-cy="learningPathTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '1')
    });

});
