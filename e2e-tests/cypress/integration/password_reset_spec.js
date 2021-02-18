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
describe('Password Reset Tests', () => {

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


    cy.intercept({
      method: 'POST',
      url: '/performPasswordReset'
    }).as('performReset');
    cy.intercept('GET', '/app/projects').as('getProjects')
    cy.intercept('GET', '/app/userInfo').as('getUserInfo')
  });

  it('reset password', () => {
    cy.register("test@skills.org", "apassword", false);
    cy.visit('/ProjectAdministrator/');
    cy.get('[data-cy=forgotPassword]').click();
    cy.get('[data-cy=forgotPasswordEmail]').should('exist');
    cy.get('[data-cy=forgotPasswordEmail]').type('test@skills.org');
    cy.get('[data-cy=resetPassword').click();
    cy.get('[data-cy=resetRequestConfirmation').should('exist');
    cy.wait(11*1000); //request rest page redirects to login after 30 seconds
    cy.get('[data-cy=login]').should('exist');
    cy.getResetLink().then((resetLink) => {
      cy.visit(resetLink);
      cy.get('[data-cy=resetPasswordSubmit]').should('exist');
      cy.get('[data-cy=resetPasswordEmail]').type('test@skills.org');
      cy.get('[data-cy=resetPasswordNewPassword]').type('password2')
      cy.get('[data-cy=resetPasswordConfirm]').type('password2');
      cy.get('[data-cy=resetPasswordSubmit]').click();

      cy.wait('@performReset');
      cy.get('[data-cy=resetConfirmation]').should('exist');
      cy.wait(11*1000) //will redirect to login page after 30 seconds
      cy.get('[data-cy=login]').should('exist');
      cy.get('#username').type('test@skills.org');
      cy.get('#inputPassword').type('password2');
      cy.get('[data-cy=login]').click();
      cy.wait('@getProjects');
      cy.wait('@getUserInfo');

      cy.get('[data-cy=breadcrumb-Home]').should('be.visible');
    });

  });

  it('reset password - wrong user', () => {
    cy.register("test@skills.org", "apassword", false);
    cy.visit('/ProjectAdministrator/');
    cy.get('[data-cy=forgotPassword]').click();
    cy.get('[data-cy=forgotPasswordEmail]').should('exist');
    cy.get('[data-cy=forgotPasswordEmail]').type('test@skills.org');
    cy.get('[data-cy=resetPassword').click();
    cy.get('[data-cy=resetRequestConfirmation').should('exist');
    cy.wait(11*1000); //request rest page redirects to login after 30 seconds
    cy.get('[data-cy=login]').should('exist');
    cy.getResetLink().then((resetLink) => {
      cy.visit(resetLink);
      cy.get('[data-cy=resetPasswordSubmit]').should('exist');
      cy.get('[data-cy=resetPasswordEmail]').type('test2@skills.org');
      cy.get('[data-cy=resetPasswordNewPassword]').type('password2')
      cy.get('[data-cy=resetPasswordConfirm]').type('password2');
      cy.get('[data-cy=resetPasswordSubmit]').click();

      cy.wait('@performReset');
      cy.get('[data-cy=resetError]').should('be.visible');
      cy.get('[data-cy=resetPasswordSubmit]').should('be.disabled');
    });
  });

  it('reset password - password confirmation mismatch', () => {
    cy.register("test@skills.org", "apassword", false);
    cy.visit('/ProjectAdministrator/');
    cy.get('[data-cy=forgotPassword]').click();
    cy.get('[data-cy=forgotPasswordEmail]').should('exist');
    cy.get('[data-cy=forgotPasswordEmail]').type('test@skills.org');
    cy.get('[data-cy=resetPassword').click();
    cy.get('[data-cy=resetRequestConfirmation').should('exist');
    cy.wait(11*1000); //request rest page redirects to login after 30 seconds
    cy.get('[data-cy=login]').should('exist');
    cy.getResetLink().then((resetLink) => {
      cy.visit(resetLink);
      cy.get('[data-cy=resetPasswordSubmit]').should('exist');
      cy.get('[data-cy=resetPasswordEmail]').type('test2@skills.org');
      cy.get('[data-cy=resetPasswordNewPassword]').type('password2')
      cy.get('[data-cy=resetPasswordConfirm]').type('password');
      cy.get('[data-cy=resetPasswordSubmit]').should('be.disabled');
    });
  });

  it('reset password - user does not exist', () => {
    cy.register("test@skills.org", "apassword", false);
    cy.visit('/ProjectAdministrator/');
    cy.get('[data-cy=forgotPassword]').click();
    cy.get('[data-cy=forgotPasswordEmail]').should('exist');
    cy.get('[data-cy=forgotPasswordEmail]').type('fake@skills.org');
    cy.get('[data-cy=resetPassword').click();
    cy.get('[data-cy=resetFailedError]').should('be.visible');
  });

  it('cannot use reset link twice', () => {
    cy.register("test@skills.org", "apassword", false);
    cy.visit('/ProjectAdministrator/');
    cy.get('[data-cy=forgotPassword]').click();
    cy.get('[data-cy=forgotPasswordEmail]').should('exist');
    cy.get('[data-cy=forgotPasswordEmail]').type('test@skills.org');
    cy.get('[data-cy=resetPassword').click();
    cy.get('[data-cy=resetRequestConfirmation').should('exist');
    cy.get('[data-cy=loginPage]').click();
    cy.get('[data-cy=login]').should('exist');
    cy.getResetLink().then((resetLink) => {
      cy.visit(resetLink);
      cy.get('[data-cy=resetPasswordSubmit]').should('exist');
      cy.get('[data-cy=resetPasswordEmail]').type('test@skills.org');
      cy.get('[data-cy=resetPasswordNewPassword]').type('password2')
      cy.get('[data-cy=resetPasswordConfirm]').type('password2');
      cy.get('[data-cy=resetPasswordSubmit]').click();

      cy.wait('@performReset');
      cy.get('[data-cy=resetConfirmation]').should('exist');
      cy.get('[data-cy=loginPage]').click();
      cy.get('[data-cy=login]').should('exist');

      cy.visit(resetLink);
      cy.get('[data-cy=resetPasswordSubmit]').should('exist');
      cy.get('[data-cy=resetPasswordEmail]').type('test@skills.org');
      cy.get('[data-cy=resetPasswordNewPassword]').type('password3')
      cy.get('[data-cy=resetPasswordConfirm]').type('password3');
      cy.get('[data-cy=resetPasswordSubmit]').click();

      cy.wait('@performReset');
      cy.get('[data-cy=resetError]').should('be.visible');
      cy.get('[data-cy=resetPasswordSubmit]').should('be.disabled');
    });
  });

  it('reset not enabled if required configurations not set', ()=>{
    cy.login('root@skills.org', 'password');

    cy.request({
      method: 'POST',
      url: '/root/saveSystemSettings',
      body: {
        publicUrl: '',
      }
    });
    cy.logout();
    cy.intercept('GET', '/public/isFeatureSupported?feature=passwordreset').as('isEnabled');
    cy.visit('/ProjectAdministrator/');
    cy.get('[data-cy=forgotPassword]').click();
    cy.wait('@isEnabled');
    cy.get('[data-cy=resetNotSupported]').should('be.visible');
    cy.get('[data-cy=forgotPasswordEmail').should('have.length.lte', 0);
  });
});
