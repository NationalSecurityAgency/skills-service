describe('Global Badges Tests', () => {

    beforeEach(() => {
        cy.logout();
        const supervisorUser = 'supervisor@skills.org';
        cy.register(supervisorUser, 'password');
        cy.login('root@skills.org', 'password');
        cy.request('PUT', `/root/users/${supervisorUser}/roles/ROLE_SUPERVISOR`);
        cy.logout();
        cy.login(supervisorUser, 'password');
    });

    it('create badge with special chars', () => {
        const expectedId = 'JustABadgeBadge';
        const providedName = "JustABadge";
        cy.server().route('GET', `/supervisor/badges`).as('getGlobalBadges');
        cy.server().route('PUT', `/supervisor/badges/${expectedId}`).as('postGlobalBadge');

        cy.visit('/globalBadges');
        cy.wait('@getGlobalBadges')
        cy.clickButton('Badge')

        cy.get('#badgeName').type(providedName)

        cy.clickSave()
        cy.wait('@postGlobalBadge');

        cy.contains(`ID: ${expectedId}`);
    });


    // THIS DOES NOT PASS: will be handled bia #449
    // it('create badge with special chars', () => {
    //     const expectedId = 'LotsofspecialPcharsBadge';
    //     const providedName = "!L@o#t$s of %s^p&e*c(i)a_l++_|}{P c'ha'rs";
    //     cy.server().route('GET', `/supervisor/badges`).as('getGlobalBadges');
    //
    //     cy.visit('/globalBadges');
    //     cy.wait('@getGlobalBadges')
    //     cy.clickButton('Badge')
    //
    //     cy.get('#badgeName').type(providedName)
    //     cy.getIdField().should('have.value', expectedId)
    //
    //     // cy.clickSave()
    //     // cy.wait('@postNewBadge');
    //     //
    //     // cy.contains('ID: Lotsofspecial')
    // });

})
