describe('Badges Tests', () => {

    beforeEach(() => {
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
        cy.clickButton('Badge')

        cy.get('#badgeName').type(providedName)

        cy.getIdField().should('have.value', expectedId)

        cy.clickSave()
        cy.wait('@postNewBadge');

        cy.contains('ID: Lotsofspecial')
    });


})
