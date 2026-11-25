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
const cards = (num) => Array.from({ length: num }, (_, i) => ({ front: `Front ${i + 1}`, back: `Back ${i + 1}`, hint: i % 2 === 0 ? `Hint ${i + 1}` : null }))
const saveFlashCards = (projNum, skillNum, toSave, version = 0) => {
    cy.request('POST', `/admin/projects/proj${projNum}/skills/skill${skillNum}/flashCards`, { version, cards: toSave })
}

describe('Flash Cards on Skills Display Tests', () => {

    beforeEach(() => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
    });

    it('flip cards, show hints and move through the deck', () => {
        saveFlashCards(1, 1, cards(3))
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('[data-cy="skillFlashCards"]').should('be.visible')
        cy.get('[data-cy="flashCardPosition"]').should('have.text', 'Card 1 of 3')
        cy.get('[data-cy="flashCardSide"]').should('have.text', 'Front')
        cy.get('[data-cy="flashCardText"]').should('have.text', 'Front 1')
        cy.get('[data-cy="prevFlashCardBtn"]').should('be.disabled')

        cy.get('[data-cy="flashCard"]').click()
        cy.get('[data-cy="flashCardSide"]').should('have.text', 'Back')
        cy.get('[data-cy="flashCardText"]').should('have.text', 'Back 1')
        cy.get('[data-cy="flashCard"]').click()
        cy.get('[data-cy="flashCardText"]').should('have.text', 'Front 1')

        cy.get('[data-cy="flashCardHint"]').should('not.exist')
        cy.get('[data-cy="showFlashCardHintBtn"]').click()
        cy.get('[data-cy="flashCardHint"]').should('have.text', 'Hint 1')

        cy.get('[data-cy="nextFlashCardBtn"]').click()
        cy.get('[data-cy="flashCardPosition"]').should('have.text', 'Card 2 of 3')
        cy.get('[data-cy="flashCardText"]').should('have.text', 'Front 2')
        cy.get('[data-cy="showFlashCardHintBtn"]').should('not.exist')

        cy.get('[data-cy="nextFlashCardBtn"]').click()
        cy.get('[data-cy="flashCardPosition"]').should('have.text', 'Card 3 of 3')
        cy.get('[data-cy="nextFlashCardBtn"]').should('be.disabled')
        cy.get('[data-cy="flashCardHint"]').should('not.exist')
        cy.get('[data-cy="prevFlashCardBtn"]').click()
        cy.get('[data-cy="flashCardPosition"]').should('have.text', 'Card 2 of 3')
    });

    it('keyboard: Enter flips the card, arrow keys move through the deck', () => {
        saveFlashCards(1, 1, cards(2))
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('[data-cy="flashCard"]').focus()
        cy.get('[data-cy="flashCard"]').type('{enter}')
        cy.get('[data-cy="flashCardText"]').should('have.text', 'Back 1')
        cy.get('[data-cy="flashCard"]').type('{rightArrow}')
        cy.get('[data-cy="flashCardPosition"]').should('have.text', 'Card 2 of 2')
        cy.get('[data-cy="flashCardText"]').should('have.text', 'Front 2')
        cy.get('[data-cy="flashCard"]').type('{leftArrow}')
        cy.get('[data-cy="flashCardPosition"]').should('have.text', 'Card 1 of 2')
    });

    it('skills without flash cards do not show the deck', () => {
        saveFlashCards(1, 1, cards(1))
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill2')
        cy.get('[data-cy="skillProgressTitle-skill2"]').should('be.visible')
        cy.get('[data-cy="skillFlashCards"]').should('not.exist')
    });

    it('reused skills show the original flash cards', () => {
        saveFlashCards(1, 1, cards(2))
        cy.createSubject(1, 2)
        cy.reuseSkillIntoAnotherSubject(1, 1, 2)
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj2/skills/skill1STREUSESKILLST0')
        cy.get('[data-cy="flashCardPosition"]').should('have.text', 'Card 1 of 2')
        cy.get('[data-cy="flashCardText"]').should('have.text', 'Front 1')
    });
});
