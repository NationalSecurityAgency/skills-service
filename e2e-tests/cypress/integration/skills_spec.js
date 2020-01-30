describe('Skills Tests', () => {

    beforeEach(() => {
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
        cy.clickButton('Skill')

        cy.get('#skillName').type(providedName)

        cy.getIdField().should('have.value', expectedId)

        cy.clickSave()
        cy.wait('@postNewSkill');

        cy.contains('ID: Lotsofspecial')
    });

    it('edit number of occurrences', () => {
        cy.server().route('POST', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('postNewSkill');
        cy.server().route('GET', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('getSkill');

        const selectorOccurrencesToCompletion = '[data-vv-name="numPerformToCompletion"]';
        const selectorSkillsRowToggle = 'table .VueTables__child-row-toggler';
        cy.visit('/projects/proj1/subjects/subj1');
        cy.clickButton('Skill')
        cy.get(selectorOccurrencesToCompletion).should('have.value', '5')
        cy.get('#skillName').type('Skill 1')

        cy.clickSave()
        cy.wait('@postNewSkill');


        cy.get(selectorSkillsRowToggle).click()
        cy.contains('50 Points')

        cy.get('table .control-column .fa-edit').click()
        cy.wait('@getSkill')

        // close toast
        cy.get('.toast-header button').click()
        cy.get(selectorOccurrencesToCompletion).should('have.value', '5')
        cy.get(selectorOccurrencesToCompletion).type('{backspace}10')
        cy.get(selectorOccurrencesToCompletion).should('have.value', '10')

        cy.clickSave()
        cy.wait('@postNewSkill');

        cy.get(selectorSkillsRowToggle).click()
        cy.contains('100 Points')
    });

    it('Add Skill Event', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

       cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
       cy.contains('Add Event').click();

       cy.contains('ONE').click();
       cy.contains('TWO').click();
       cy.get('.existingUserInput button').contains('TWO');

       cy.contains('Enter user id').type('foo{enter}');
       cy.clickButton('Add');
       cy.get('.text-success', {timeout: 5*1000}).contains('Added points for');

    });

})
