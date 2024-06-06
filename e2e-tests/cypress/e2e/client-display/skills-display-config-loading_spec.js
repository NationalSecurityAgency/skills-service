describe('Skills Display Config Loading in various scenarios', () => {

  beforeEach(() => {

  })

  it('project settings are loaded when navigating through multiple projects via p&r pages', () => {
    cy.intercept('/public/clientDisplay/config?projectId=proj1').as('proj1Config');
    cy.intercept('/public/clientDisplay/config?projectId=proj2').as('proj2Config');
    cy.intercept('/public/clientDisplay/config?projectId=proj3').as('proj3Config');

    for (let i = 1; i <= 3; i++) {
      cy.createProject(i);
      cy.enableProdMode(i);
      cy.addToMyProjects(i);
    }
    cy.request('POST', '/admin/projects/proj1/settings', [
      {
        value: 'Stage',
        setting: 'level.displayName',
        projectId: 'proj1',
      },
    ]);
    cy.request('POST', '/admin/projects/proj2/settings', [
      {
        value: 'Achievement',
        setting: 'level.displayName',
        projectId: 'proj2',
      },
    ]);
    cy.request('POST', '/admin/projects/proj3/settings', [
      {
        value: 'Life',
        setting: 'level.displayName',
        projectId: 'proj3',
      },
    ]);

    cy.visit('/progress-and-rankings');

    cy.get('[data-cy="project-link-proj1"]').click();
    cy.wait('@proj1Config')
    cy.get('[data-cy="overallLevelTitle"]').contains('My Stage')

    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click()
    cy.get('[data-cy="project-link-proj2"]').click();
    cy.wait('@proj2Config')
    cy.get('[data-cy="overallLevelTitle"]').contains('My Achievement')

    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').click()
    cy.get('[data-cy="project-link-proj3"]').click();
    cy.wait('@proj3Config')
    cy.get('[data-cy="overallLevelTitle"]').contains('My Life')
  })

  it('project settings are loaded when navigating directly to each project', () => {
    cy.intercept('/public/clientDisplay/config?projectId=proj1').as('proj1Config');
    cy.intercept('/public/clientDisplay/config?projectId=proj2').as('proj2Config');
    cy.intercept('/public/clientDisplay/config?projectId=proj3').as('proj3Config');

    for (let i = 1; i <= 3; i++) {
      cy.createProject(i);
      cy.enableProdMode(i);
      cy.addToMyProjects(i);
    }
    cy.request('POST', '/admin/projects/proj1/settings', [
      {
        value: 'Stage',
        setting: 'level.displayName',
        projectId: 'proj1',
      },
    ]);
    cy.request('POST', '/admin/projects/proj2/settings', [
      {
        value: 'Achievement',
        setting: 'level.displayName',
        projectId: 'proj2',
      },
    ]);
    cy.request('POST', '/admin/projects/proj3/settings', [
      {
        value: 'Life',
        setting: 'level.displayName',
        projectId: 'proj3',
      },
    ]);

    cy.visit('/progress-and-rankings/projects/proj1');
    cy.wait('@proj1Config')
    cy.get('[data-cy="overallLevelTitle"]').contains('My Stage')

    cy.visit('/progress-and-rankings/projects/proj2');
    cy.wait('@proj2Config')
    cy.get('[data-cy="overallLevelTitle"]').contains('My Achievement')

    cy.visit('/progress-and-rankings/projects/proj3');
    cy.wait('@proj3Config')
    cy.get('[data-cy="overallLevelTitle"]').contains('My Life')
  })

  it.only('project settings are loaded in iframe', () => {
    for (let i = 1; i <= 3; i++) {
      cy.createProject(i);
      cy.enableProdMode(i);
      cy.addToMyProjects(i);
    }
    cy.request('POST', '/admin/projects/proj1/settings', [
      {
        value: 'Stage',
        setting: 'level.displayName',
        projectId: 'proj1',
      },
    ]);
    cy.request('POST', '/admin/projects/proj2/settings', [
      {
        value: 'Achievement',
        setting: 'level.displayName',
        projectId: 'proj2',
      },
    ]);
    cy.request('POST', '/admin/projects/proj3/settings', [
      {
        value: 'Life',
        setting: 'level.displayName',
        projectId: 'proj3',
      },
    ]);
    cy.on('uncaught:exception', (err, runnable) => {
      return false
    })

    cy.intercept('/public/clientDisplay/config?projectId=proj1').as('proj1Config');
    cy.intercept('/static/clientPortal/index.html').as('skillsClient')
    cy.visit('/test-skills-client/proj1');
    cy.wait('@skillsClient')
    cy.wait('@proj1Config')
    cy.wrapIframe().find('[data-cy="overallLevelTitle"]').contains('My Stage')
  })

})
