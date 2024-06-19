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
describe('Client Display Skills Navigation', () => {

    it('navigate between skills', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillProgress_index-0"]').should('exist');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"').click();
        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 1');
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3');
        cy.get('[data-cy="prevSkill"]').should('not.exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 2');
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 3');
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('not.exist');
        cy.get('[data-cy="prevSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 2');
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
        cy.get('[data-cy="prevSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 1');
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3');
        cy.get('[data-cy="prevSkill"]').should('not.exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
    });

    it('navigate between skills, starting from a skill', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.cdVisit('/subjects/subj1/skills/skill2');

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 2');
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 3');
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('not.exist');
        cy.get('[data-cy="prevSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 2');
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
        cy.get('[data-cy="prevSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 1');
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3');
        cy.get('[data-cy="prevSkill"]').should('not.exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
    });

    it('navigate between skills, starting from the last skill', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.cdVisit('/subjects/subj1/skills/skill3');

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 3');
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('not.exist');
        cy.get('[data-cy="prevSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 2');
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
        cy.get('[data-cy="prevSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 1');
        cy.get('[data-cy="skillOrder"]').contains('Skill 1 of 3');
        cy.get('[data-cy="prevSkill"]').should('not.exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 2');
        cy.get('[data-cy="skillOrder"]').contains('Skill 2 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').click();

        cy.get('[data-cy="skillProgressTitle"').contains('Very Great Skill 3');
        cy.get('[data-cy="skillOrder"]').contains('Skill 3 of 3');
        cy.get('[data-cy="prevSkill"]').should('exist');
        cy.get('[data-cy="nextSkill"]').should('not.exist');
    });

    it('no paging on cross project dependency skill pages', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.addCrossProjectLearningPathItem(2, 1, 1, 1)
        cy.addLearningPathItem(2, 2, 1)

        // Go to cross-project dependency page
        cy.cdVisit('/subjects/subj1/skills/skill1/crossProject/proj2/skill1');
        cy.get('[data-cy="prevSkill"]').should('not.exist');
        cy.get('[data-cy="nextSkill"]').should('not.exist');
    });
});
