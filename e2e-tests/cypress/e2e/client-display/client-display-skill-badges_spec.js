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

describe('Client Display Badges Visible on Skill Summaries', () => {

    beforeEach(() => {

    });

   it('display badge on skill', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(1, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(1, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });

        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 1);
        cy.enableBadge(1, 1);

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillBadges"]').should('exist');
        cy.get('[data-cy="skillBadge-0"]').should('exist');
    });

   it('display global badge on skill', () => {
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
                }
            });
        cy.loginAsProxyUser();
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(1, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(1, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });

        cy.loginAsRootUser();
        cy.createGlobalBadge(1)
        cy.assignSkillToGlobalBadge(1, 1)
        cy.enableGlobalBadge(1)
        cy.loginAsProxyUser();

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.get('[data-cy="skillBadges"]').should('exist');
        cy.get('[data-cy="skillBadge-0"]').should('exist');
    });

   it('display badge and global badge on skill from subject page', () => {
      cy.resetDb()
      cy.fixture('vars.json')
        .then((vars) => {
          if (!Cypress.env('oauthMode')) {
            cy.register(Cypress.env('proxyUser'), vars.defaultPass, false)
          }
        })
      cy.loginAsProxyUser()
      cy.createProject(1)
      cy.createSubject(1, 1)
      cy.createSkill(1, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' })
      cy.createSkill(1, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' })
      cy.createSkill(1, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' })

      cy.createBadge(1, 1)
      cy.assignSkillToBadge(1, 1, 1)
      cy.enableBadge(1, 1)

      cy.createSubject(1, 2)
      cy.reuseSkillIntoAnotherSubject(1, 1, 2)

      cy.loginAsRootUser()
      cy.createGlobalBadge(1)
      cy.assignSkillToGlobalBadge(1, 1)
      cy.enableGlobalBadge(1)
      cy.loginAsProxyUser()

      // check for the badges on the original skill
      cy.cdVisit('/')
      cy.cdClickSubj(0)

      cy.get('[data-cy="skillBadges"]').should('exist')
      cy.get('[data-cy="skillBadge-0"]').should('exist')
      cy.get('[data-cy="skillBadge-1"]').should('exist')

      // now check the reused skill
      cy.cdVisit('/')
      cy.cdClickSubj(1)

      cy.get('[data-cy="skillBadges"]').should('exist')
      cy.get('[data-cy="skillBadge-0"]').should('exist')
      cy.get('[data-cy="skillBadge-1"]').should('exist')
    })

    it('display badge and global badge on skill from skill detail page', () => {
      cy.resetDb();
      cy.fixture('vars.json')
        .then((vars) => {
          if (!Cypress.env('oauthMode')) {
            cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
          }
        });
      cy.loginAsProxyUser();
      cy.createProject(1);
      cy.createSubject(1, 1);
      cy.createSkill(1, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
      cy.createSkill(1, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
      cy.createSkill(1, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });

      cy.createBadge(1, 1);
      cy.assignSkillToBadge(1, 1, 1);
      cy.enableBadge(1, 1);

      cy.createSubject(1, 2)
      cy.reuseSkillIntoAnotherSubject(1, 1, 2)

      cy.loginAsRootUser();
      cy.createGlobalBadge(1)
      cy.assignSkillToGlobalBadge(1, 1)
      cy.enableGlobalBadge(1)
      cy.loginAsProxyUser();

      // check for the badges on the original skill
      cy.cdVisit('/');
      cy.cdClickSubj(0);
      cy.cdClickSkill(0);

      cy.get('[data-cy="skillBadges"]').should('exist');
      cy.get('[data-cy="skillBadge-0"]').should('exist');
      cy.get('[data-cy="skillBadge-1"]').should('exist');

      // now check the reused skill
      cy.cdVisit('/');
      cy.cdClickSubj(1);
      cy.cdClickSkill(0);

      cy.get('[data-cy="skillBadges"]').should('exist');
      cy.get('[data-cy="skillBadge-0"]').should('exist');
      cy.get('[data-cy="skillBadge-1"]').should('exist');
    });
});