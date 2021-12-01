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

    cy.intercept('GET', '/app/projects').as('getProjects')
      .intercept('GET', '/api/icons/customIconCss').as('getProjectsCustomIcons')
      .intercept('GET', '/app/userInfo').as('getUserInfo')
      .intercept('GET', '/app/oAuthProviders').as('getOAuthProviders')
      .intercept('POST', '/performLogin').as('postPerformLogin');
  });

  it('form: successful dashboard login', () => {
    cy.visit('/administrator/');

    cy.get('#username').type('root@skills.org');
    cy.get('#inputPassword').type('password');
    cy.contains('Login').click();

    cy.wait('@getProjects')
      .then(({ request, response}) => {
        expect(response.statusCode).to.eq(200)
    })
    cy.wait('@getUserInfo')
      .then(({ request, response}) => {
      expect(response.statusCode).to.eq(200)
    })

    cy.contains('Project');
    cy.get('[data-cy=subPageHeader]').contains('Projects');
  });

  it('form: successful dashboard login from link', () => {
    cy.fixture('vars.json').then((vars) => {
      if (!Cypress.env('oauthMode')) {
        cy.log('NOT in oauthMode, using form login')
        cy.login(vars.defaultUser, vars.defaultPass);
      } else {
        cy.log('oauthMode, using loginBySingleSignOn')
        cy.loginBySingleSignOn()
      }

      // setup existing project
      cy.createProject(1);
      cy.enableProdMode(1);
      cy.createSubject(1, 1);
      cy.createSkill(1, 1, 1);
      cy.logout();
    });

    cy.visit('/administrator/projects/proj1/');

    cy.get('#username').type('root@skills.org');
    cy.get('#inputPassword').type('password');
    cy.contains('Login').click();

    cy.wait('@getProjects')
      .then(({ request, response}) => {
        expect(response.statusCode).to.eq(200)
      })
    cy.wait('@getUserInfo')
      .then(({ request, response}) => {
        expect(response.statusCode).to.eq(200)
      })

    cy.contains('PROJECT: This is project 1').should('be.visible');
    cy.contains('ID: proj1').should('be.visible');
    cy.get('[data-cy=subPageHeader]').contains('Subjects');
  });

  it('form: bad password', () => {
    cy.visit('/administrator/');

    cy.get('#username').type('root@skills.org');
    cy.get('#inputPassword').type('password1');
    cy.contains('Login').click();
    cy.wait('@postPerformLogin');

    cy.contains('Invalid');
  });

  it('form: bad user', () => {
    cy.visit('/administrator/');

    cy.get('#username').type('root1@skills.org');
    cy.get('#inputPassword').type('password');
    cy.contains('Login').click();
    cy.wait('@postPerformLogin');

    cy.contains('Invalid');
  });

  it('disabled login - password must be at least 8 characters', () => {
    cy.visit('/administrator/');
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
    cy.visit('/administrator/');
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
    cy.visit('/administrator/');
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

  it('disabled login - valid email format', () => {
    cy.visit('/administrator/');
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

  if (!Cypress.env('oauthMode')) {
    it('OAuth login is not enabled', () => {
      cy.visit('/administrator/');
      cy.contains('Login').should('be.disabled');

      cy.wait('@getOAuthProviders')
        .then(({request, response}) => {
          expect(response.statusCode).to.eq(200)
        })
      cy.get('[data-cy=oAuthProviders]').should('not.exist');
    })

    it('no login form for oAuthOnly mode', () => {
      cy.intercept('GET', '/public/config', {oAuthOnly: true}).as('loadConfig');
      cy.intercept('GET', '/app/oAuthProviders', [{"registrationId":"gitlab","clientName":"GitLab","iconClass":"fab fa-gitlab"}]).as('getOauthProviders')

      cy.visit('/administrator/');

      cy.wait('@loadConfig');
      cy.wait('@getOauthProviders');
      cy.get('#username').should('not.exist')
      cy.get('#inputPassword').should('not.exist')
      cy.get('[data-cy=oAuthProviders]').should('exist');
      cy.contains('Login via GitLab')
    });
  }

});
