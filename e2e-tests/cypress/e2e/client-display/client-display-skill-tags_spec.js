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

describe('Client Display Skill Tags Visible on Skill Summaries', () => {

  beforeEach(() => {

  });

 it('display tag on skill', () => {
      cy.createProject(1);
      cy.createSubject(1, 1);
      cy.createSkill(1, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
      cy.createSkill(1, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
      cy.createSkill(1, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });

      cy.addTagToSkills();

      cy.cdVisit('/');
      cy.cdClickSubj(0);

      cy.get('[data-cy="skillTags"]').should('exist');
      cy.get('[data-cy="skillTag-0"]').should('exist');
  });

  it('filter on skill tag', () => {
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
    cy.createSkill(1, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
    cy.createSkill(1, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });

    cy.addTagToSkills();

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillTags"]').should('exist');
    cy.get('[data-cy="skillTag-0"]').should('exist');


    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2');
    cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3');

    cy.get('[data-cy="skillTag-0"]').click()
    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
    cy.get('[data-cy="skillProgress_index-1"]').should('not.exist');
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist');

    cy.get('[data-cy="clearSelectedTagFilter-tag1"]').click()
    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2');
    cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3');
  });

  it('filter on multiple skill tags', () => {
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
    cy.createSkill(1, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
    cy.createSkill(1, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });

    cy.addTagToSkills(1, ['skill1', 'skill3'], 1);
    cy.addTagToSkills(1, ['skill2', 'skill3'], 2);

    cy.cdVisit('/');
    cy.cdClickSubj(0);

    cy.get('[data-cy="skillTags"]').should('exist');
    cy.get('[data-cy="skillTag-0"]').should('exist');


    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2');
    cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3');

    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillTag-0"]').click()
    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 3');
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist');

    cy.get('[data-cy="skillTag-1"]').click()
    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 3');
    cy.get('[data-cy="skillProgress_index-1"]').should('not.exist');
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist');

    cy.get('[data-cy="clearSelectedTagFilter-tag1"]').click()
    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 2');
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 3');
    cy.get('[data-cy="skillProgress_index-2"]').should('not.exist');

    cy.get('[data-cy="clearSelectedTagFilter-tag2"]').click()
    cy.get('[data-cy="skillProgress_index-0"]').contains('Very Great Skill 1');
    cy.get('[data-cy="skillProgress_index-1"]').contains('Very Great Skill 2');
    cy.get('[data-cy="skillProgress_index-2"]').contains('Very Great Skill 3');
  });
});