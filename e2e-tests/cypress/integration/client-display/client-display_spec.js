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
        cy.visit('http://localhost:8083/');
        cy.contains('Overall Points');
    });

});
