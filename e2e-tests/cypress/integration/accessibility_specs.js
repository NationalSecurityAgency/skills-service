describe('Accessibility Tests', () => {
  it('home page', () => {
    cy.visit('/');
    cy.customLighthouse();
    cy.customPa11y();

    cy.contains('Metrics').click();
    cy.customLighthouse();
    cy.customPa11y();
  });

  it('project and subpages', () => {
    cy.request('POST', '/app/projects/MyNewtestProject', {
      projectId: 'MyNewtestProject',
      name: "My New test Project"
    });

    cy.request('POST', '/admin/projects/MyNewtestProject/subjects/subj1', {
      projectId: 'MyNewtestProject',
      subjectId: 'subj1',
      name: "Subject 1"
    });

    cy.visit('/');
    cy.contains('Manage').click();
    cy.customLighthouse();
    cy.customPa11y();

    cy.contains('Manage').click();
    cy.customLighthouse();
    cy.customPa11y();

    cy.contains('Levels').click();
    cy.customLighthouse();
    cy.customPa11y();
    // create subjects, badges, skills
  })
});
