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

describe('Flash Cards Accessibility Tests', () => {

    const runWithDarkMode = ['', ' - dark mode']
    runWithDarkMode.forEach((darkMode) => {
        it(`skill page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1)
            saveFlashCards(1, 1, cards(2))

            cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill1')
            cy.get('[data-cy="flashCardPosition"]').should('have.text', 'Card 1 of 2')
            cy.get('[data-cy="showFlashCardHintBtn"]').click()
            cy.get('[data-cy="flashCardHint"]').should('be.visible')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`admin config page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createProject(1)
            cy.createSubject(1, 1)
            cy.createSkill(1, 1, 1)
            saveFlashCards(1, 1, cards(2))

            cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-flash-cards')
            cy.get('textarea[data-cy="flashCardFront-1"]').should('have.value', 'Front 2')
            cy.get('[data-cy="flashCardsPreview"]').should('be.visible')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });
    });
});
