describe('Subjects Tests', () => {

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

    it('create subject with special chars', () => {
        const expectedId = 'LotsofspecialPcharsSubject';
        const providedName = "!L@o#t$s of %s^p&e*c(i)a_l++_|}{P c'ha'rs";
        cy.server().route('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');

        cy.visit('/projects/proj1');
        cy.get('button:contains(\'Subject\')').click()

        cy.get('#subjName').type(providedName)
        cy.get('#idInput').should('have.value', expectedId)

        cy.get("button:contains('Save')").click()
        cy.wait('@postNewSubject');

        cy.contains('ID: Lotsofspecial')
    });

})
