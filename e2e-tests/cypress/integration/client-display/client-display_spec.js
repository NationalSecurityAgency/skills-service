describe('Client Display Tests', () => {

    const cssAttachedToNavigableCards = 'skills-navigable-item';

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

        // some basic default theme validation
        cy.get("#app").should('have.css', 'background-color')
            .and('equal', 'rgba(0, 0, 0, 0)');
    });

    it('back button', () => {
        cy.cdVisit('/');
        cy.contains('User Skills');
        cy.get('[data-cy=back]').should('not.exist');

        // to ranking page and back
        cy.get('[data-cy=myRank]').click()
        cy.contains('Rank Overview');
        cy.cdBack();

        // to subject page and back
        cy.cdClickSubj(1, 'Subject 2');
        cy.cdBack();

        // to subject page (2nd subject card), then to skill page, back, back to home page
        cy.cdClickSubj(0, 'Subject 1');
        cy.cdClickSkill(0);
        cy.cdBack('Subject 1');
        cy.cdBack();
    });

    it('clearly represent navigable components', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.cdVisit('/');

        cy.get('[data-cy=myRank]').should('have.class', 'skills-navigable-item');
        cy.get('[data-cy=myBadges]').should('have.class', 'skills-navigable-item');
        cy.get('[data-cy=subjectTile]').eq(0).should('have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=subjectTile]').eq(1).should('have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=subjectTile]').eq(2).should('have.class', cssAttachedToNavigableCards);
    });

    it.only('test theming', () => {
        cy.cdVisit('/?enableTheme=true')
        // hex #626d7d = rgb(98, 109, 125)
        cy.get("#app").should('have.css', 'background-color')
            .and('equal', 'rgb(98, 109, 125)');

    });

    it('components should not be clickable in the summary only option', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        cy.cdVisit('/?isSummaryOnly=true');

        cy.get('[data-cy=myRank]').contains("1")
        cy.get('[data-cy=myBadges]').contains("0 Badges")

        // make sure click doesn't take us anywhere
        cy.get('[data-cy=myRank]').click()
        cy.contains("User Skills")

        cy.get('[data-cy=myBadges]').click()
        cy.contains("User Skills")

        // make sure css is not attached
        cy.get('[data-cy=myRank]').should('not.have.class', cssAttachedToNavigableCards);
        cy.get('[data-cy=myBadges]').should('not.have.class', cssAttachedToNavigableCards);

        // summaries should not be displayed at all
        cy.get('[data-cy=subjectTile]').should('not.exist');
    });

});

