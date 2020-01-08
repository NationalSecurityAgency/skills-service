describe('Badges Tests', () => {

    beforeEach(() => {
        cy.request('PUT', '/createAccount', {
            firstName: 'Person',
            lastName: 'Three',
            email: 'skills@skills.org',
            password: 'password',
        });
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('create badge with special chars', () => {
        const expectedId = 'LotsofspecialPcharsBadge';
        const providedName = "!L@o#t$s of %s^p&e*c(i)a_l++_|}{P c'ha'rs";
        cy.server().route('POST', `/admin/projects/proj1/badges/${expectedId}`).as('postNewBadge');

        cy.visit('/projects/proj1/badges');
        cy.get('button:contains(\'Badge\')').click()

        cy.get('#badgeName').type(providedName)

        cy.get('#idInput').should('have.value', expectedId)

        cy.get("button:contains('Save')").click()
        cy.wait('@postNewBadge');

        cy.contains('ID: Lotsofspecial')
    });

})
