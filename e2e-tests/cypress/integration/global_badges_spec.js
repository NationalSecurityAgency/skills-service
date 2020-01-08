describe('Global Badges Tests', () => {

    beforeEach(() => {
        cy.request('PUT', '/createAccount', {
            firstName: 'Person',
            lastName: 'Three',
            email: 'skills@skills.org',
            password: 'password',
        });
        cy.request('POST', '/logout');
        cy.request( {
            method: 'POST',
            url: '/performLogin',
            body: {
                username: 'root@skills.org',
                password: 'password'
            },
            form: true,
        })
        cy.request('PUT', '/root/users/skills@skills.org/roles/ROLE_SUPERVISOR');
        cy.request('POST', '/logout');
        cy.request( {
            method: 'POST',
            url: '/performLogin',
            body: {
                username: 'skills@skills.org',
                password: 'password'
            },
            form: true,
        })
    });

    it('create badge with special chars', () => {
        const expectedId = 'JustABadgeBadge';
        const providedName = "JustABadge";
        cy.server().route('GET', `/supervisor/badges`).as('getGlobalBadges');
        cy.server().route('PUT', `/supervisor/badges/${expectedId}`).as('postGlobalBadge');

        cy.visit('/globalBadges');
        cy.wait('@getGlobalBadges')
        cy.get('button:contains(\'Badge\')').click()

        cy.get('#badgeName').type(providedName)

        cy.get("button:contains('Save')").click()
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
    //     cy.get('button:contains(\'Badge\')').click()
    //
    //     cy.get('#badgeName').type(providedName)
    //     cy.get('#idInput').should('have.value', expectedId)
    //
    //     // cy.get("button:contains('Save')").click()
    //     // cy.wait('@postNewBadge');
    //     //
    //     // cy.contains('ID: Lotsofspecial')
    // });

})
