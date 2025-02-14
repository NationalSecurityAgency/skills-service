describe('Skill Form Tests', () => {

    it('Form content saved on back button press', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.openNewSkillDialog();
        cy.get('[data-cy="skillName"]').type('testname')

        cy.go('back');
        cy.go('forward');

        cy.openNewSkillDialog();
        cy.get('[data-cy="contentRestoredMessage"]').contains('Form\'s values have been restored from backup.');
        cy.get('[data-cy="skillName"]').should('have.value','testname');
    })
})