/*
 * Copyright 2020 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
describe('Login Tests', () => {

  beforeEach(() => {
    cy.logout();

    cy.server()
      .route('GET', '/app/projects').as('getProjects')
      .route('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
      .route('GET', '/app/userInfo').as('getUserInfo')
      .route('GET', '/app/oAuthProviders').as('getOAuthProviders')
      .route('POST', '/performLogin').as('postPerformLogin');
  });

  it('form: successful dashboard login', () => {
    cy.visit('/');

    cy.get('#username').type('root@skills.org');
    cy.get('#inputPassword').type('password');
    cy.contains('Login').click();

    cy.wait('@getProjects').its('status').should('equal', 200)
      .wait('@getUserInfo').its('status').should('equal', 200);

    cy.contains('Project');
    cy.contains('My Projects');
    cy.get('[data-cy=projectSearch]').should('be.visible');
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

    const expectedText = 'Email Address cannot be less than 5 characters.';

    cy.get('#username').type('v@s');
    cy.get('#inputPassword').type('12345678');
    cy.contains('Login').should('be.disabled');
    cy.contains(expectedText);

    cy.get('#username').clear();
    cy.get('#username').type('v@s.org');
    cy.contains('Login').should('be.enabled');
    cy.contains(expectedText).should('not.exist');
  })

  /*
  vee-validate 3.x permits email addresses greater than 73 characters
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
  })*/

  it('disabled login - valid email format', () => {
    cy.visit('/');
    cy.contains('Login').should('be.disabled');

    const expectedText = 'Email Address must be a valid email';

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

  it('OAuth login is not enabled', () => {
    cy.visit('/');
    cy.contains('Login').should('be.disabled');

    cy.wait('@getOAuthProviders').its('status').should('equal', 200)
    cy.get('[data-cy=oAuthProviders]').should('not.exist');
  })
});
