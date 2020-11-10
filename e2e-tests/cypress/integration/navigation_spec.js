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
    cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
      projectId: 'proj1', subjectId: 'subj1', name: 'Subject 1',
    });
  });

  it('ability to expand and collapse navigation', function () {
    cy.visit('/projects/proj1');
    cy.contains('ID: subj1');

    // validate nav i expanded by default
    cy.get('[data-cy=nav-Subjects]').contains('Subjects');
    cy.get('[data-cy=nav-Badges]').contains('Badges');

    // collapse
    cy.get('[data-cy=navCollapseOrExpand]').click();
    cy.get('[data-cy=nav-Subjects]').contains('Projects').should('not.exist');
    cy.get('[data-cy=nav-Badges]').contains('Metrics').should('not.exist');

    // refresh and validate that nav is still collapsed
    cy.visit('/projects/proj1');
    cy.contains('ID: subj1');
    cy.get('[data-cy=nav-Subjects]').contains('Subjects').should('not.exist');
    cy.get('[data-cy=nav-Badges]').contains('Badges').should('not.exist');

    // navigate through the collapsed nav
    cy.get('[data-cy=nav-Badges]').click();
    cy.contains('No Badges Yet');
    cy.get('[data-cy=nav-Subjects]').click();
    cy.contains('ID: subj1');
    cy.get('[data-cy=nav-Subjects]').contains('Subjects').should('not.exist');
    cy.get('[data-cy=nav-Badges]').contains('Badges').should('not.exist');

    // drill down to subject and make sure nav is still collapsed
    cy.clickManageSubject('subj1');
    cy.contains('No Skills Yet');
    cy.get('[data-cy=nav-Skills]').contains('Skills').should('not.exist');
    cy.get('[data-cy=nav-Levels]').contains('Levels').should('not.exist');

    // refresh and make sure that nav is still collapsed
    cy.visit('/projects/proj1/subjects/subj1');
    cy.contains('No Skills Yet');
    cy.get('[data-cy=nav-Skills]').contains('Skills').should('not.exist');
    cy.get('[data-cy=nav-Levels]').contains('Levels').should('not.exist');

    // expand nav
    cy.get('[data-cy=navCollapseOrExpand]').click();
    cy.get('[data-cy=nav-Skills]').contains('Skills');
    cy.get('[data-cy=nav-Levels]').contains('Levels');

    // navigate back to the home page and make sure nav is expanded
    cy.get('[data-cy=breadcrumb-proj1]').click();
    cy.contains('ID: subj1');
    cy.get('[data-cy=nav-Subjects]').contains('Subjects');
    cy.get('[data-cy=nav-Badges]').contains('Badges');
  });

  it('selected menu item should be highlighted', function () {
    cy.visit('/projects/proj1');
    cy.contains('ID: subj1');
    cy.get('[data-cy=nav-Subjects]').should('have.class', 'bg-primary');
    cy.get('[data-cy=nav-Badges]').should('not.have.class', 'bg-primary');

    cy.get('[data-cy=nav-Badges]').click();
    cy.contains('No Badges Yet');
    cy.get('[data-cy=nav-Subjects]').should('not.have.class', 'bg-primary');
    cy.get('[data-cy=nav-Badges]').should('have.class', 'bg-primary');
  });

  it('navigation on a small screen', function () {
    cy.viewport('iphone-6')
    cy.visit('/projects/proj1');
    cy.contains('ID: subj1');
    cy.get('[data-cy=navCollapseOrExpand]').should('not.exist');
    cy.get('[data-cy=nav-Subjects]').should('not.visible');
    cy.get('[data-cy=nav-Badges]').should('not.visible');

    // expand menu
    cy.get('[data-cy=navSmallScreenExpandMenu]').click()
    cy.get('[data-cy=nav-Subjects]').contains('Subjects');
    cy.get('[data-cy=nav-Badges]').contains('Badges');

    // navigate and make sure menu is collapsed again
    cy.get('[data-cy=nav-Badges]').click();
    cy.contains('No Badges Yet');
    cy.get('[data-cy=nav-Subjects]').should('not.visible');
    cy.get('[data-cy=nav-Badges]').should('not.visible');
  });

});

