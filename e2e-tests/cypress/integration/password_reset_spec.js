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
    cy.resetEmail();

    cy.fixture('vars.json').then((vars) => {
      cy.register(vars.rootUser, vars.defaultPass, true);
    });

    cy.login('root@skills.org', 'password');

    cy.request({
      method: 'POST',
      url: '/root/saveEmailSettings',
      body: {
        host: 'localhost',
        port: 1026,
        'protocol': 'smtp'
      },
    });

    cy.request({
      method: 'POST',
      url: '/root/saveSystemSettings',
      body: {
        publicUrl: 'http://localhost:8082/',
        resetTokenExpiration: 'PT2H'
      }
    });

    cy.logout();

    cy.server();
    cy.route({
      method: 'POST',
      url: '/performPasswordReset'
    }).as('performReset');
    cy.route('GET', '/app/projects').as('getProjects')
    cy.route('GET', '/app/userInfo').as('getUserInfo')

  });

  afterEach(() => {
  });

  it('form: reset password', () => {
    cy.register("test@skills.org", "apassword", false);
    cy.visit('/');
    cy.get('[data-cy=forgotPassword]').click();
    cy.get('[data-cy=forgotPasswordEmail]').type('test@skills.org');
    cy.get('[data-cy=resetPassword').click();
    cy.wait(16*1000); //request rest page redirects to login after 15 seconds
    cy.getResetLink().then((resetLink) => {
      cy.visit(resetLink);
      cy.get('[data-cy=resetPasswordSubmit]').should('exist');
      cy.get('[data-cy=resetPasswordEmail]').type('test@skills.org');
      cy.get('[data-cy=resetPasswordNewPassword]').type('password2')
      cy.get('[data-cy=resetPasswordConfirm]').type('password2');
      cy.get('[data-cy=resetPasswordSubmit]').click();

      cy.wait('@performReset');
      cy.visit("/");
      cy.get('#username').type('test@skills.org');
      cy.get('#inputPassword').type('password2');
      cy.get('[data-cy=login]').click();
      cy.wait('@getProjects');
      cy.wait('@getUserInfo');

      cy.contains('Project');
      cy.contains('My Projects');
    });

  });
});
