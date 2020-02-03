
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
        cy.get(`[data-cy=skillProgress]:nth-child(${skillIndex+1}) [data-cy=skillProgressBar]`).click();
    } else {
        cy.get(`[data-cy=skillProgress]:nth-child(${skillIndex+1}) [data-cy=skillProgressTitle]`).click();
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



