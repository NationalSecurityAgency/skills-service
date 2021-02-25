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
describe('Register Root Users', () => {

  beforeEach(() => {
    cy.logout();
    cy.clearDb();
  });

  afterEach(() => {
    cy.logout();
    cy.clearDb();

    cy.fixture('vars.json').then((vars) => {
      cy.register(vars.rootUser, vars.defaultPass, true);
      cy.register(vars.defaultUser, vars.defaultPass);
    })
  });

  it('register root user', () => {
    cy.visit('/administrator/');
    cy.contains('New Root Account')
    cy.get('#firstName').type("Robert")
    cy.get('#lastName').type("Smith")
    cy.get('#email').type("rob.smith@madeup.org")
    cy.get('#password').type("password")
    cy.get('#password_confirmation').type("password")
    cy.contains('Create Account').click()
  });
});
