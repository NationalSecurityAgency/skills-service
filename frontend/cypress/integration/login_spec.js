describe('Login Tests', () => {

  beforeEach(() => {
    cy.exec('npm version', {failOnNonZeroExit: false})
    cy.exec('npm run backend:clearDb')
    cy.request('PUT', '/createAccount', {
      firstName: 'Person',
      lastName : 'OneTwo',
      email    : 'root@skills.org',
      password : 'password',
    })
    cy.request('POST', '/grantFirstRoot');
    cy.request('POST', '/logout');

    cy.server()
      .route('GET', '/app/projects').as('getProjects')
      .route('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
      .route('GET', '/app/userInfo').as('getUserInfo')
      .route('POST', '/performLogin').as('postPerformLogin');
  });

  it('form: successful dashboard login', () => {
    cy.visit('/');

    cy.get('#username').type('root@skills.org');
    cy.get('#inputPassword').type('password');
    cy.contains('Login').click();

    cy.wait('@getProjects').its('status').should('be', 200)
      .wait('@getUserInfo').its('status').should('be', 200);

    cy.contains('Project');
    cy.contains('My Projects');
    cy.contains('Inception');
  });

  it('form: bad password', () => {
    cy.visit('/');

    cy.get('#username').type('root@skills.org');
    cy.get('#inputPassword').type('password1');
    cy.contains('Login').click();
    cy.wait('@postPerformLogin');

    cy.contains('Invalid');
  });

  it('form: bad user', () => {
    cy.visit('/');

    cy.get('#username').type('root1@skills.org');
    cy.get('#inputPassword').type('password');
    cy.contains('Login').click();
    cy.wait('@postPerformLogin');

    cy.contains('Invalid');
  });
});
