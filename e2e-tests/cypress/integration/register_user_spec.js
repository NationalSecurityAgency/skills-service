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
describe('Register Dashboard Users', () => {

  beforeEach(() => {
    cy.logout();
  });

  it('navigate between login and sign up page', () => {
    cy.visit('/');
    cy.contains('Don\'t have a SkillTree account')
    cy.contains('Sign up').click()
    cy.contains('New Account')
    cy.contains('Sign in').click()
    cy.contains('Don\'t have a SkillTree account')
  });

  it('register dashboard user', () => {
    cy.visit('/request-account');
    cy.contains('New Account')
    cy.get('#firstName').type("Robert")
    cy.get('#lastName').type("Smith")
    cy.get('#email').type("rob.smith@madeup.org")
    cy.get('#password').type("password")
    cy.get('#password_confirmation').type("password")
    cy.contains('Create Account').click()

    cy.get('[data-cy="breadcrumb-Progress And Rankings"]').should('be.visible');
  });

  it('register dashboard validation', () => {
    cy.visit('/request-account');
    cy.contains('New Account')
    cy.get('#firstName').type("Robert")
    cy.get('#lastName').type("Smith")
    cy.get('#email').type("rob.smith@madeup.org")
    cy.get('#password').type("password")
    cy.get('#password_confirmation').type("password")


    // password mismatch via confirmation pass
    cy.get('#password_confirmation').clear().type("password1")
    cy.contains('Create Account').should('be.disabled');
    cy.contains('Password confirmation does not match')
    cy.get('#password_confirmation').clear().type("password")
    cy.contains('Create Account').should('be.enabled');
    cy.contains('Password confirmation does not match').should('not.exist')

    // password mismatch via main pass
    cy.get('#password').clear().type("password1")
    cy.contains('Create Account').should('be.disabled');
    cy.contains('Password confirmation does not match')
    cy.get('#password').clear().type("password")
    cy.contains('Create Account').should('be.enabled');
    cy.contains('Password confirmation does not match').should('not.exist')

    // password must be at least 8 chars
    cy.get('#password').clear().type("passwor")
    cy.contains('Create Account').should('be.disabled');
    cy.contains('Password cannot be less than 8 characters')
    cy.get('#password').clear().type("password")
    cy.contains('Create Account').should('be.enabled');
    cy.contains('Password cannot be less than 8 characters').should('not.exist')

    // password must not exceed 40 characters
    const invalidPassword = Array(41).fill('a').join('');
    const validPassword = Array(40).fill('a').join('');
    cy.get('#password').clear().type(invalidPassword)
    cy.get('#password_confirmation').clear().type(invalidPassword)
    cy.contains('Password cannot exceed 40 characters')
    cy.contains('Create Account').should('be.disabled');
    cy.get('#password').type('{backspace}')
    cy.get('#password_confirmation').type('{backspace}')
    cy.contains('Password cannot exceed 40 characters').should('not.exist')
    cy.contains('Create Account').should('be.enabled');

    // email must be at least 5 chars
    cy.get('#email').clear().type("1234")
    cy.contains('Email must be a valid email')
    cy.contains('Create Account').should('be.disabled');
    cy.get('#email').clear().type("rob.smith@madeup.org")
    cy.contains('Create Account').should('be.enabled');
    cy.contains('Email must be a valid email').should('not.exist')

    /*
    emails over 73 characters are considered valid in vee-validate 3+
    // email must not exceed 73 chars
    const invalidEmail = Array(74-9).fill('a').join('');
    const validEmail = Array(73-9).fill('a').join('');
    cy.get('#email').clear().type(`${invalidEmail}@mail.org`)
    cy.contains('The Email field must be a valid email').should('be.visible')
    cy.contains('Create Account').should('be.disabled');
    cy.get('#email').clear().type(`${validEmail}@mail.org`)
    cy.contains('Create Account').should('be.enabled');
    cy.contains('The Email field must be a valid email').should('not.exist')
     */

    // email already taken
    cy.get('#email').clear().type('skills@skills.org')
    cy.contains('The email address is already used for another account')
    cy.contains('Create Account').should('be.disabled');

    cy.get('#email').clear().type('skills1@skills.org')
    cy.contains('Create Account').should('be.enabled');
    cy.contains('The email address is already used for another account').should('not.exist')

    // valid email
    cy.get('#email').clear().type("rob.smithmadeup.org")
    cy.contains('Email must be a valid email')
    cy.contains('Create Account').should('be.disabled');
    cy.get('#email').clear().type("rob.smith@madeup.org")
    cy.contains('Email must be a valid email').should('not.exist')
    cy.contains('Create Account').should('be.enabled');

    // first name must  not be empty
    cy.get('#firstName').clear()
    cy.contains('First Name is required')
    cy.contains('Create Account').should('be.disabled');
    cy.get('#firstName').type('Robert')
    cy.contains('Create Account').should('be.enabled');
    cy.contains('First Name is required').should('not.exist')

    // first name must not exceed 30 characters
    const thirtyOneChars = Array(31).fill('a').join('');
    cy.get('#firstName').clear().type(thirtyOneChars)
    cy.contains('First Name cannot exceed 30 characters')
    cy.contains('Create Account').should('be.disabled');
    cy.get('#firstName').type('{backspace}')
    cy.contains('First Name exceed 30 characters').should('not.exist')
    cy.contains('Create Account').should('be.enabled');

    // last name must  not be empty
    cy.get('#lastName').clear()
    cy.contains('Last Name is required')
    cy.contains('Create Account').should('be.disabled');
    cy.get('#lastName').type('Smith')
    cy.contains('Create Account').should('be.enabled');
    cy.contains('Last Name is required').should('not.exist')

    // last name must not exceed 30 characters
    cy.get('#lastName').clear().type(thirtyOneChars)
    cy.contains('Last Name cannot exceed 30 characters')
    cy.contains('Create Account').should('be.disabled');
    cy.get('#lastName').type('{backspace}')
    cy.contains('Last Name cannot exceed 30 characters').should('not.exist')
    cy.contains('Create Account').should('be.enabled');
  });

});
