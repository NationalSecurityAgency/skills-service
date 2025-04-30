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

describe('Skills Display Disabled Skills Tests', () => {

    beforeEach(() => {
    });

    it('disabled skills are not displayed in the client display', () => {
        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2, { enabled: false });

        cy.cdVisit('/');

        cy.get('[data-cy="totalPoints"]').should('have.text', '200');
        cy.get('[data-cy="pointsTillNextLevel"]').should('have.text', '20')
        cy.get('[data-cy="numTotalSkills"]').should('have.text', '1')
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="pointsProgress"]').should('have.text', '0 / 200')
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="levelProgress"]').should('have.text', '0 / 20')

        cy.cdClickSubj(0);
        cy.get('[data-cy="totalPoints"]').should('have.text', '200');
        cy.get('[data-cy="pointsTillNextLevel"]').should('have.text', '20')
        cy.get('[data-cy="numTotalSkills"]').should('have.text', '1')
        cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]').should('not.exist')

        // now enable skill
        cy.createSkill(1, 1, 2, { enabled: true })
        cy.cdVisit('/');

        cy.get('[data-cy="totalPoints"]').should('have.text', '400');
        cy.get('[data-cy="pointsTillNextLevel"]').should('have.text', '40')
        cy.get('[data-cy="numTotalSkills"]').should('have.text', '2')
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="pointsProgress"]').should('have.text', '0 / 400')
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="levelProgress"]').should('have.text', '0 / 40')

        cy.cdClickSubj(0);
        cy.get('[data-cy="totalPoints"]').should('have.text', '400');
        cy.get('[data-cy="pointsTillNextLevel"]').should('have.text', '40')
        cy.get('[data-cy="numTotalSkills"]').should('have.text', '2')
        cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
        cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2');
    });

});


