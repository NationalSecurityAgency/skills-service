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
describe('Navigation Tests', () => {
  beforeEach(() => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: "My New test Project"
    })
  });

  it('ability to expand and collapse navigation', function () {
    cy.visit('/');
    cy.contains('My New test Project');

    // validate nav i expanded by default
    cy.get('[data-cy=nav-Projects]').contains('Projects');
    cy.get('[data-cy=nav-Metrics]').contains('Metrics');

    // collapse
    cy.get('[data-cy=navCollapseOrExpand]').click();
    cy.get('[data-cy=nav-Projects]').contains('Projects').should('not.exist');
    cy.get('[data-cy=nav-Metrics]').contains('Metrics').should('not.exist');

    // refresh and validate that nav is still collapsed
    cy.visit('/');
    cy.contains('My New test Project');
    cy.get('[data-cy=nav-Projects]').contains('Projects').should('not.exist');
    cy.get('[data-cy=nav-Metrics]').contains('Metrics').should('not.exist');

    // navigate through the collapsed nav
    cy.get('[data-cy=nav-Metrics]').click();
    cy.contains('No Metrics Yet');
    cy.get('[data-cy=nav-Projects]').click();
    cy.contains('My New test Project');
    cy.get('[data-cy=nav-Projects]').contains('Projects').should('not.exist');
    cy.get('[data-cy=nav-Metrics]').contains('Metrics').should('not.exist');

    // drill down to subjects and make sure nav is still collapsed
    cy.get('[data-cy=projCard_proj1_manageBtn]').click();
    cy.contains('No Subjects Yet');
    cy.get('[data-cy=nav-Subjects]').contains('Subjects').should('not.exist');
    cy.get('[data-cy=nav-Badges]').contains('Badges').should('not.exist');

    // refresh and make sure that nav is still collapsed
    cy.visit('/projects/proj1');
    cy.contains('No Subjects Yet');
    cy.get('[data-cy=nav-Subjects]').contains('Subjects').should('not.exist');
    cy.get('[data-cy=nav-Badges]').contains('Badges').should('not.exist');

    // expand nav
    cy.get('[data-cy=navCollapseOrExpand]').click();
    cy.get('[data-cy=nav-Subjects]').contains('Subjects');
    cy.get('[data-cy=nav-Badges]').contains('Badges');

    // navigate back to the home page and make sure nav is expanded
    cy.get('[data-cy=breadcrumb-Home]').click();
    cy.contains('My New test Project');
    cy.get('[data-cy=nav-Projects]').contains('Projects');
    cy.get('[data-cy=nav-Metrics]').contains('Metrics');
  });

  it('selected menu item should be highlighted', function () {
    cy.visit('/');
    cy.contains('My New test Project');
    cy.get('[data-cy=nav-Projects]').should('have.class', 'bg-primary');
    cy.get('[data-cy=nav-Metrics]').should('not.have.class', 'bg-primary');

    cy.get('[data-cy=nav-Metrics]').click();
    cy.contains('No Metrics Yet');
    cy.get('[data-cy=nav-Projects]').should('not.have.class', 'bg-primary');
    cy.get('[data-cy=nav-Metrics]').should('have.class', 'bg-primary');
  });

  it('navigation on a small screen', function () {
    cy.viewport('iphone-6')
    cy.visit('/');
    cy.contains('My New test Project');
    cy.get('[data-cy=navCollapseOrExpand]').should('not.exist');
    cy.get('[data-cy=nav-Projects]').should('not.visible');
    cy.get('[data-cy=nav-Metrics]').should('not.visible');

    // expand menu
    cy.get('[data-cy=navSmallScreenExpandMenu]').click()
    cy.get('[data-cy=nav-Projects]').contains('Projects');
    cy.get('[data-cy=nav-Metrics]').contains('Metrics');

    // navigate and make sure menu is collapsed again
    cy.get('[data-cy=nav-Metrics]').click();
    cy.contains('No Metrics Yet');
    cy.get('[data-cy=nav-Projects]').should('not.visible');
    cy.get('[data-cy=nav-Metrics]').should('not.visible');
  });

});

