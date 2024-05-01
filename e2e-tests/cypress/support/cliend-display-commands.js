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

Cypress.Commands.add("validatePoweredBy", () => {
    cy.url().then(url => {
        cy.log(`url: ${url}`);
        if (!url.includes('disableSkillTreeBrand|true') && !url.includes('disableSkillTreeBrand%7Ctrue')) {
            cy.get('[data-cy="skillsTitle"]').contains('powered by');
        }
    });
});

Cypress.Commands.add('cdVisit', (url = '', expectPointHistoryData = false) => {
    cy.visit(`/test-skills-display/proj1${url}`)
    cy.validatePoweredBy()

    if (!url || url === '' || url === '/' || url === '/?internalBackButton=true') {
        cy.get(expectPointHistoryData ? '[data-cy="pointHistoryChartWithData"]' : '[data-cy="pointHistoryChartNoData"]')
    }
})

Cypress.Commands.add("cdBack", (expectedTitle = 'User Skills') => {
    cy.get('[data-cy=back]').click()
    cy.validatePoweredBy();
    cy.contains(expectedTitle);

    // back button should not exist on the home page, whose title is the default value
    if (expectedTitle === 'User Skills'){
        cy.get('[data-cy=back]').should('not.exist');
    }
});

Cypress.Commands.add('cdClickSubj', (subjIndex, expectedTitle = null, expectPointHistoryData = false) => {
  cy.get(`[data-cy="subjectTile"] [data-cy="subjectTileBtn"]`).eq(subjIndex).click()
  cy.validatePoweredBy()
  if (expectedTitle) {
    cy.get('[data-cy="skillsTitle"]').contains(expectedTitle)
  }

  cy.get(expectPointHistoryData ? '[data-cy="pointHistoryChartWithData"]' : '[data-cy="pointHistoryChartNoData"]')
})

Cypress.Commands.add("cdClickSkill", (skillIndex, useProgressBar = true, skillLabel = 'Skill') => {
    if (useProgressBar) {
        cy.get(`[data-cy="skillProgress_index-${skillIndex}"] [data-cy="skillProgressBar"]`).click();
    } else {
        cy.get(`[data-cy="skillProgress_index-${skillIndex}"] [data-cy="skillProgressTitle"]`).click();
    }
    cy.contains(`${skillLabel} Overview`)
    cy.validatePoweredBy();
});

Cypress.Commands.add("cdClickRank", () => {
    cy.get('[data-cy=myRank]').click();
    cy.contains('My Rank');
    cy.validatePoweredBy();
});

Cypress.Commands.add("cdClickBadges", () => {
    cy.get('[data-cy="myBadgesBtn"]').click()
    cy.get('[data-cy="title"]').contains('My Badges');
    cy.validatePoweredBy();
});

Cypress.Commands.add("cdClickBadge", (badgeId) => {
    cy.get(`[data-cy=badgeDetailsLink_badge${badgeId}]`).click();
    cy.contains('Badge Details');
    cy.validatePoweredBy();
});



Cypress.Commands.add("dashboardCd", (firstVisit=false, project='proj1') => {
    cy.intercept(`/api/projects/${project}/rank`).as(`getRank${project}`)
    cy.intercept(`/api/projects/${project}/pointHistory`).as(`getPointsHistory${project}`)
    if (firstVisit) {
        cy.wait(`@getRank${project}`)
        cy.wait(`@getPointsHistory${project}`)
    }
    return cy.wrapIframe();
});

Cypress.Commands.add("dashboardCdClickSubj", (subjIndex, expectedTitle) => {
    cy.wrapIframe().find(`.user-skill-subject-tile:nth-child(${subjIndex + 1})`).first().click();
    if (expectedTitle) {
        cy.wrapIframe().contains(expectedTitle);
    }
});

