describe('Login Tests', () => {

  beforeEach(() => {
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

  it('disabled login - password must be at least 8 characters', () => {
    cy.visit('/');
    cy.contains('Login').should('be.disabled');

    const expectedText = 'Password cannot be less than 8 characters.';

    cy.get('#username').type('validEmail@skills.org');
    cy.get('#inputPassword').type('1234567');
    cy.contains('Login').should('be.disabled');
    cy.contains(expectedText)

    cy.get('#inputPassword').clear();
    cy.get('#inputPassword').type('12345678');
    cy.contains('Login').should('be.enabled');
    cy.contains(expectedText).should('not.exist');
  })

  it('disabled login - password must not exceed 40 characters', () => {
    cy.visit('/');
    cy.contains('Login').should('be.disabled');

    const expectedText = 'Password cannot exceed 40 characters';
    const invalidPassword = Array(41).fill('a').join('');
    const validPassword = Array(40).fill('a').join('');

    cy.get('#username').type('validEmail@skills.org');
    cy.get('#inputPassword').type(invalidPassword);
    cy.contains('Login').should('be.disabled');
    cy.contains(expectedText)

    cy.get('#inputPassword').clear();
    cy.get('#inputPassword').type(validPassword);
    cy.contains('Login').should('be.enabled');
    cy.contains(expectedText).should('not.exist');
  })

  it('disabled login - email must be at least 5 chars', () => {
    cy.visit('/');
    cy.contains('Login').should('be.disabled');

    const expectedText = 'Email cannot be less than 5 characters.';

    cy.get('#username').type('v@s');
    cy.get('#inputPassword').type('12345678');
    cy.contains('Login').should('be.disabled');
    cy.contains(expectedText);

    cy.get('#username').clear();
    cy.get('#username').type('v@s.org');
    cy.contains('Login').should('be.enabled');
    cy.contains(expectedText).should('not.exist');
  })

  it('disabled login - email must not exceed 73 chars', () => {
    cy.visit('/');
    cy.contains('Login').should('be.disabled');

    // valid email must be less than 73 chars
    const invalidEmail = Array(74-9).fill('a').join('');
    const validEmail = Array(73-9).fill('a').join('');

    // will be taken care of by email validator
    const expectedText = 'The Email field must be a valid email';

    cy.get('#username').type(`${invalidEmail}@mail.org`);
    cy.get('#inputPassword').type('12345678');
    cy.contains('Login').should('be.disabled');
    cy.contains(expectedText);

    cy.get('#username').clear();
    cy.get('#username').type(`${validEmail}@mail.org`);
    cy.contains('Login').should('be.enabled');
    cy.contains(expectedText).should('not.exist');
  })

  it('disabled login - valid email format', () => {
    cy.visit('/');
    cy.contains('Login').should('be.disabled');

    const expectedText = 'The Email field must be a valid email';

    cy.get('#username').type('notvalid');
    cy.get('#inputPassword').type('12345678');
    cy.contains('Login').should('be.disabled');
    cy.contains(expectedText);

    cy.get('#username').clear();
    cy.get('#username').type('almost@dkda');
    cy.contains('Login').should('be.disabled');
    cy.contains(expectedText);

    cy.get('#username').clear();
    cy.get('#username').type('almost@dkda.org');
    cy.contains('Login').should('be.enabled');
    cy.contains(expectedText).should('not.exist');
  })
});
