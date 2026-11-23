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

describe('Configure Flash Cards Tests', () => {

    beforeEach(() => {
        cy.intercept('GET', '/admin/projects/proj1/skills/skill1/flashCards').as('getFlashCards')
        cy.intercept('POST', '/admin/projects/proj1/skills/skill1/flashCards').as('saveFlashCards')
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
    });

    it('add, edit, reorder and remove flash cards', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('[data-cy="nav-Flash Cards"]').click()
        cy.wait('@getFlashCards')
        cy.get('[data-cy="noFlashCards"]').should('be.visible')
        cy.get('[data-cy="saveFlashCardsBtn"]').should('be.disabled')

        cy.get('[data-cy="addFlashCardBtn"]').click()
        cy.get('[data-cy="flashCardFront-0"]').type('What is SkillTree?')
        cy.get('[data-cy="flashCardBack-0"]').type('A micro-learning gamification platform')
        cy.get('[data-cy="flashCardHint-0"]').type('Think training')
        cy.get('[data-cy="addFlashCardBtn"]').click()
        cy.get('[data-cy="flashCardFront-1"]').type('Second front')
        cy.get('[data-cy="flashCardBack-1"]').type('Second back')
        cy.get('[data-cy="unsavedChangesAlert"]').should('be.visible')
        cy.get('[data-cy="flashCardsPreview"] [data-cy="flashCardText"]').should('have.text', 'What is SkillTree?')

        cy.get('[data-cy="saveFlashCardsBtn"]').click()
        cy.wait('@saveFlashCards').its('request.body').should('deep.equal', {
            version: 0,
            cards: [
                { front: 'What is SkillTree?', back: 'A micro-learning gamification platform', hint: 'Think training' },
                { front: 'Second front', back: 'Second back', hint: null },
            ]
        })
        cy.get('[data-cy="savedMsg"]').should('be.visible')
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')

        cy.get('[data-cy="moveFlashCardUp-1"]').click()
        cy.get('textarea[data-cy="flashCardFront-0"]').should('have.value', 'Second front')
        cy.get('[data-cy="removeFlashCard-1"]').click()
        cy.get('[data-cy="flashCard-1"]').should('not.exist')
        cy.get('[data-cy="saveFlashCardsBtn"]').click()
        cy.wait('@saveFlashCards').its('request.body.version').should('eq', 1)

        cy.reload()
        cy.wait('@getFlashCards')
        cy.get('[data-cy="flashCard-0"]').should('exist')
        cy.get('[data-cy="flashCard-1"]').should('not.exist')
        cy.get('textarea[data-cy="flashCardFront-0"]').should('have.value', 'Second front')
    });

    it('front and back are required', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-flash-cards')
        cy.wait('@getFlashCards')
        cy.get('[data-cy="addFlashCardBtn"]').click()
        cy.get('[data-cy="flashCardFront-0"]').type('Front')
        cy.get('[data-cy="flashCardFront-0"]').clear()
        cy.get('[data-cy="cards[0].frontError"]').contains('Front is a required field')
        cy.get('[data-cy="flashCardBack-0"]').type('b')
        cy.get('[data-cy="flashCardBack-0"]').clear()
        cy.get('[data-cy="cards[0].backError"]').contains('Back is a required field')
        cy.get('[data-cy="saveFlashCardsBtn"]').should('be.disabled')
        cy.get('[data-cy="flashCardFront-0"]').type('f')
        cy.get('[data-cy="flashCardBack-0"]').type('b')
        cy.get('[data-cy="saveFlashCardsBtn"]').should('be.enabled')
    });

    it('number of cards is limited', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.maxFlashCardsPerSkill = 2
                return res
            })
        }).as('getConfig')
        saveFlashCards(1, 1, cards(1))
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-flash-cards')
        cy.wait('@getFlashCards')
        cy.get('[data-cy="addFlashCardBtn"]').click()
        cy.get('[data-cy="addFlashCardBtn"]').should('be.disabled')
        cy.get('[data-cy="maxFlashCardsMsg"]').contains('A skill can have at most 2 flash cards')
    });

    it('a save based on outdated cards shows the error and reloads', () => {
        saveFlashCards(1, 1, cards(1))
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-flash-cards')
        cy.wait('@getFlashCards')
        saveFlashCards(1, 1, cards(3), 1)

        cy.get('[data-cy="flashCardBack-0"]').type(' changed')
        cy.get('[data-cy="saveFlashCardsBtn"]').click()
        cy.wait('@saveFlashCards')
        cy.get('[data-cy="flashCardsSaveError"]').contains('Flash cards were changed after they were loaded')
        cy.get('[data-cy="reloadFlashCardsBtn"]').click()
        cy.wait('@getFlashCards')
        cy.get('[data-cy="flashCard-2"]').should('exist')
        cy.get('[data-cy="flashCardsSaveError"]').should('not.exist')
    });

    it('flash cards of reused skills are read-only', () => {
        saveFlashCards(1, 1, cards(2))
        cy.createSubject(1, 2)
        cy.reuseSkillIntoAnotherSubject(1, 1, 2)
        cy.visit('/administrator/projects/proj1/subjects/subj2/skills/skill1STREUSESKILLST0/config-flash-cards')
        cy.get('[data-cy="readOnlyAlert"]').contains('Reused')
        cy.get('textarea[data-cy="flashCardFront-0"]').should('be.disabled').and('have.value', 'Front 1')
        cy.get('[data-cy="addFlashCardBtn"]').should('not.exist')
        cy.get('[data-cy="saveFlashCardsBtn"]').should('not.exist')
        cy.get('[data-cy="removeFlashCard-0"]').should('not.exist')
    });

    it('flash cards of imported skills are read-only', () => {
        saveFlashCards(1, 1, cards(2))
        cy.exportSkillToCatalog(1, 1, 1)
        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.importSkillFromCatalog(2, 1, 1, 1)
        cy.finalizeCatalogImport(2)
        cy.visit('/administrator/projects/proj2/subjects/subj1/skills/skill1/config-flash-cards')
        cy.get('[data-cy="readOnlyAlert"]').contains('Imported')
        cy.get('[data-cy="addFlashCardBtn"]').should('not.exist')
        cy.get('[data-cy="saveFlashCardsBtn"]').should('not.exist')
    });
});
