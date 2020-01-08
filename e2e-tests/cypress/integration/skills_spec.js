describe('Skills Tests', () => {

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
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        })
    });

    it('create skill with special chars', () => {
        const expectedId = 'LotsofspecialPcharsSkill';
        const providedName = "!L@o#t$s of %s^p&e*c(i)a_l++_|}{P c'ha'rs";
        cy.server().route('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill');

        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('button:contains(\'Skill\')').click()

        cy.get('#skillName').type(providedName)

        cy.get('#idInput').should('have.value', expectedId)

        cy.get("button:contains('Save')").click()
        cy.wait('@postNewSkill');

        cy.contains('ID: Lotsofspecial')
    });

})
