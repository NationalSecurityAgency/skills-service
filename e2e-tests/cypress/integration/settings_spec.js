describe('Settings Tests', () => {

    beforeEach(() => {
        cy.logout();
        cy.fixture('vars.json').then((vars) => {
            cy.login(vars.rootUser, vars.defaultPass);
        });
    })

    it('Add Root User', () => {
        cy.visit('/');
        cy.get('button.dropdown-toggle').first().click({force: true});
        cy.contains('Settings').click();
        cy.contains('Security').click();
        cy.contains('Enter user id').first().type('sk{enter}');
        cy.contains('skills@skills.org').click();
        cy.contains('Add').first().click();
        cy.get('div.table-responsive').contains('Firstname LastName (skills@skills.org)');


    })
});
