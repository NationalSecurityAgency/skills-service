describe('Skills Display Badge Type Filter', () => {

  beforeEach(() => {
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1);
    cy.createSkill(1, 1, 2, { pointIncrement: 11, numPerformToCompletion: 1 });
    cy.createSkill(1, 1, 3, { pointIncrement: 22, numPerformToCompletion: 1 });
  })

  it('badge type filter', () => {
    cy.createBadge(1, 1);
    cy.assignSkillToBadge(1, 1, 1);
    cy.assignSkillToBadge(1, 1, 2);
    cy.createBadge(1, 1, { enabled: true });

    cy.createBadge(1, 2);
    cy.assignSkillToBadge(1, 2, 1);
    cy.assignSkillToBadge(1, 2, 2);
    cy.createBadge(1, 2, { enabled: true });

    cy.createProject(2);
    cy.loginAsRootUser();

    cy.createGlobalBadge(1);
    cy.assignSkillToGlobalBadge(1, 1, 1);
    cy.enableGlobalBadge();

    cy.loginAsProxyUser();

    cy.cdVisit('/');
    cy.cdClickBadges();

    cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]')
      .click();
    cy.get('[data-cy="filter_projectBadges"] [data-cy="filterCount"]')
      .should('have.text', 2);
    cy.get('[data-cy="filter_gems"] [data-cy="filterCount"]')
      .should('have.text', 0);
    cy.get('[data-cy="filter_globalBadges"] [data-cy="filterCount"]')
      .should('have.text', 1);

    cy.get('[data-cy="badgeDetailsLink_badge1"]')
    cy.get('[data-cy="badgeDetailsLink_badge2"]')
    cy.get('[data-cy="badgeDetailsLink_globalBadge1"]')

    cy.get('[data-cy="filter_globalBadges"]').click()
    cy.get('[data-cy="badgeDetailsLink_badge1"]').should('not.exist')
    cy.get('[data-cy="badgeDetailsLink_badge2"]').should('not.exist')
    cy.get('[data-cy="badgeDetailsLink_globalBadge1"]')

    cy.get('[data-cy="selectedFilter"]').contains('Global Badges');

    cy.get('[data-cy="selectedFilter"] [data-pc-section="removeicon"]')
      .click();
    cy.get('[data-cy="selectedFilter"]').should('not.exist')
    cy.get('[data-cy="badgeDetailsLink_badge1"]')
    cy.get('[data-cy="badgeDetailsLink_badge2"]')
    cy.get('[data-cy="badgeDetailsLink_globalBadge1"]')
  });

  it('search and filter', () => {
    cy.createBadge(1, 1);
    cy.assignSkillToBadge(1, 1, 1);
    cy.assignSkillToBadge(1, 1, 2);
    cy.createBadge(1, 1, { enabled: true });

    cy.createBadge(1, 2);
    cy.assignSkillToBadge(1, 2, 1);
    cy.assignSkillToBadge(1, 2, 2);
    cy.createBadge(1, 2, { enabled: true });

    cy.createProject(2);
    cy.loginAsRootUser();

    cy.createGlobalBadge(1);
    cy.assignSkillToGlobalBadge(1, 1, 1);
    cy.enableGlobalBadge();

    cy.loginAsProxyUser();

    cy.cdVisit('/');
    cy.cdClickBadges();

    cy.get('[data-cy="badgeDetailsLink_badge1"]')
    cy.get('[data-cy="badgeDetailsLink_badge2"]')
    cy.get('[data-cy="badgeDetailsLink_globalBadge1"]')


    cy.get('[data-cy="badgeSearchInput"]').type('1')

    cy.get('[data-cy="badgeDetailsLink_badge1"]')
    cy.get('[data-cy="badgeDetailsLink_badge2"]').should('not.exist')
    cy.get('[data-cy="badgeDetailsLink_globalBadge1"]')

    cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
    cy.get('[data-cy="filter_projectBadges"]').click()
    cy.get('[data-cy="selectedFilter"]').contains('Project Badges');
    cy.get('[data-cy="badgeDetailsLink_badge1"]')
    cy.get('[data-cy="badgeDetailsLink_badge2"]').should('not.exist')
    cy.get('[data-cy="badgeDetailsLink_globalBadge1"]').should('not.exist')
    //
    // cy.get('[data-cy="selectedFilter"]').contains('Global Badges');
    //
    // cy.get('[data-cy="selectedFilter"] [data-pc-section="removeicon"]')
    //   .click();
    // cy.get('[data-cy="selectedFilter"]').should('not.exist')
    // cy.get('[data-cy="badgeDetailsLink_badge1"]')
    // cy.get('[data-cy="badgeDetailsLink_badge2"]')
    // cy.get('[data-cy="badgeDetailsLink_globalBadge1"]')
  });

  it('no results', () => {
    cy.createBadge(1, 1);
    cy.assignSkillToBadge(1, 1, 1);
    cy.assignSkillToBadge(1, 1, 2);
    cy.createBadge(1, 1, { enabled: true });

    cy.cdVisit('/');
    cy.cdClickBadges();

    cy.get('[data-cy="badgeDetailsLink_badge1"]')

    cy.get('[data-cy="badgeSearchInput"]').type('bla')
    cy.get('[data-cy="badgeDetailsLink_badge1"]').should('not.exist')

    cy.get('[data-cy="noContent"]').contains('Please refine [bla] search');
    cy.get('[data-cy="noContent"]').should('not.contain', 'clear the selected')

    cy.get('[data-cy="badgeSearchInput"]').clear()
    cy.get('[data-cy="badgeDetailsLink_badge1"]')

    cy.get('[data-cy="filterMenu"] [data-cy="filterBtn"]').click();
    cy.get('[data-cy="filter_projectBadges"]').click()
    cy.get('[data-cy="badgeSearchInput"]').type('bla')
    cy.get('[data-cy="badgeDetailsLink_badge1"]').should('not.exist')
    cy.get('[data-cy="noContent"]').contains('Please refine [bla] search')


  });

})