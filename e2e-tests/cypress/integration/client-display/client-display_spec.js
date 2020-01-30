describe('Client Display Tests', () => {

    before(() => {
        cy.disableUILogin();
    });

    after(function () {
        cy.enableUILogin();
    });

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: 'Subject 2'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj3', {
            projectId: 'proj1',
            subjectId: 'subj3',
            name: 'Subject 3'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 10,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: 'This skill a skill!',
            version: 0,
        });
    });

    it('visit home page', () => {
        cy.cdVisit('/');
        cy.contains('Overall Points');
    });

    it.only('back button', () => {
        cy.cdVisit('/');
        cy.contains('User Skills');
        cy.get('[data-cy=back]').should('not.exist');

        // to ranking page and back
        cy.contains('Ranking Stats').click()
        cy.contains('Rank Overview');
        cy.cdBack();

        // to subject page and back
        cy.cdClickSubj(1, 'Subject 2');
        cy.cdBack();

        // to subject page (2nd subject card), then to skill page, back, back to home page
        cy.cdClickSubj(0, 'Subject 1');
        cy.cdClickSkill(0);
        // cy.get('.user-skill-progress-layers:nth-child(1)').click()
        // cy.contains('Skill Overview')
        cy.cdBack('Subject 1');
        cy.cdBack();
    });

});
