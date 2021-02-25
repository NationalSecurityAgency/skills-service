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

Cypress.Commands.add("cdVisit", (url) => {
    cy.visit(`http://localhost:8083${url}`);
});

Cypress.Commands.add("cdBack", (expectedTitle = 'User Skills') => {
    cy.get('[data-cy=back]').click()
    cy.contains(expectedTitle);

    // back button should not exist on the home page, whose title is the default value
    if (expectedTitle === 'User Skills'){
        cy.get('[data-cy=back]').should('not.exist');
    }
});

Cypress.Commands.add("cdClickSubj", (subjIndex, expectedTitle) => {
    cy.get(`.user-skill-subject-tile:nth-child(${subjIndex+1})`).first().click();
    if (expectedTitle){
        cy.contains(expectedTitle);
    }
});

Cypress.Commands.add("cdClickSkill", (skillIndex, useProgressBar = true) => {
    if (useProgressBar) {
        cy.get(`[data-cy="skillProgress_index-${skillIndex}"] [data-cy="skillProgressBar"]`).click();
    } else {
        cy.get(`[data-cy="skillProgress_index-${skillIndex}"] [data-cy="skillProgressTitle"]`).click();
    }
    cy.contains('Skill Overview')
});

Cypress.Commands.add("cdClickRank", () => {
    cy.get('[data-cy=myRank]').click();
    cy.contains('Rank Overview');
});

Cypress.Commands.add("cdClickBadges", () => {
    cy.get('[data-cy=myBadges]').click()
    cy.contains('Badges');
});



