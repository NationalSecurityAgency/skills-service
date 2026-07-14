/*
 * Copyright 2026 SkillTree
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

describe('Client Display Skill Tag Page Features', () => {

  beforeEach(() => {
    cy.intercept('GET', '/api/projects/proj1/tags/tag1/summary')
      .as('loadSkillTag1Summary')
    cy.intercept('GET', '/api/projects/proj1/tags/tag2/summary')
      .as('loadSkillTag2Summary')
    cy.intercept('GET', `/api/projects/proj1/skills/tags?*`)
      .as('loadSkillTagsForProject')
    cy.intercept('GET', `/api/projects/proj1/tags/summary`)
      .as('loadSkillTagsSummary')
    cy.intercept('GET', `/api/projects/proj1/subjects/subj1/skills/tags?*`)
      .as('loadSkillTagsForSubject1')
    cy.intercept('GET', `/api/projects/proj1/subjects/subj2/skills/tags?*`)
      .as('loadSkillTagsForSubject2')
    cy.intercept('GET', `/api/projects/proj1/tags/*/descriptions?*`)
      .as('loadSkillTagDescriptions')

    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1, {
      numPerformToCompletion: 1,
      description: 'This is skill1 - Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
    })
    cy.createSkill(1, 1, 2, {
      numPerformToCompletion: 1,
      description: 'This is skill2 - Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
    })
    cy.createSkill(1, 1, 3, {
      numPerformToCompletion: 1,
      description: 'This is skill3 - Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
    })

    cy.createSubject(1, 2)
    cy.createSkill(1, 2, 1)
    cy.createSkill(1, 2, 2)

    cy.addTagToSkills(1, ['skill1', 'skill3'], 1)
    cy.addTagToSkills(1, ['skill2', 'skill3', 'skill1Subj2'], 2)

    cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now')
  })

  it('displays all project-level skill tags on the project page', () => {

    cy.cdVisit('/')
    cy.wait('@loadSkillTagsForProject')

    cy.get('[data-cy="skillTags"]')
      .should('be.visible')

    cy.get('[data-cy="tagLink-tag1"]')
      .should('be.visible')
      .and('have.attr', 'href', '/test-skills-display/proj1/tags/tag1')
    cy.get('[data-cy="tagLink-tag1"] [data-cy="tagName"]').should('have.text', 'TAG 1')
    cy.get('[data-cy="tagLink-tag1"] [data-cy="numSkills"]').should('have.text', '2')

    cy.get('[data-cy="tagLink-tag2"]')
      .should('be.visible')
      .and('have.attr', 'href', '/test-skills-display/proj1/tags/tag2')
    cy.get('[data-cy="tagLink-tag2"] [data-cy="tagName"]').should('have.text', 'TAG 2')
    cy.get('[data-cy="tagLink-tag2"] [data-cy="numSkills"]').should('have.text', '3')
  })

  it('displays subject-level skill tags on the subject page', () => {

    cy.cdVisit('/')
    cy.cdClickSubj(0)
    cy.wait('@loadSkillTagsForSubject1')

    cy.get('[data-cy="skillTags"]')
      .should('be.visible')

    cy.get('[data-cy="tagLink-tag1"]')
      .should('be.visible')
      .and('have.attr', 'href', '/test-skills-display/proj1/tags/tag1')
    cy.get('[data-cy="tagLink-tag1"] [data-cy="tagName"]').should('have.text', 'TAG 1')
    cy.get('[data-cy="tagLink-tag1"] [data-cy="numSkills"]').should('have.text', '2')

    cy.get('[data-cy="tagLink-tag2"]')
      .should('be.visible')
      .and('have.attr', 'href', '/test-skills-display/proj1/tags/tag2')
    cy.get('[data-cy="tagLink-tag2"] [data-cy="tagName"]').should('have.text', 'TAG 2')
    cy.get('[data-cy="tagLink-tag2"] [data-cy="numSkills"]').should('have.text', '2')

    cy.cdVisit('/')
    cy.cdClickSubj(1)
    cy.wait('@loadSkillTagsForSubject2')

    cy.get('[data-cy="skillTags"]')
      .should('be.visible')

    cy.get('[data-cy="tagLink-tag1"]')
      .should('not.exist')

    cy.get('[data-cy="tagLink-tag2"]')
      .should('be.visible')
      .and('have.attr', 'href', '/test-skills-display/proj1/tags/tag2')
    cy.get('[data-cy="tagLink-tag2"] [data-cy="tagName"]').should('have.text', 'TAG 2')
    cy.get('[data-cy="tagLink-tag2"] [data-cy="numSkills"]').should('have.text', '1')
  })

  it('navigate to skill tag overview page from the project page', () => {

    cy.cdVisit('/')
    cy.wait('@loadSkillTagsForProject')

    cy.get('[data-cy="skillTags"]')
      .should('be.visible')

    cy.get('[data-cy="tagLink-tag1"]')
      .should('be.visible')
      .and('have.attr', 'href', '/test-skills-display/proj1/tags/tag1')
    cy.get('[data-cy="tagLink-tag1"] [data-cy="tagName"]').should('have.text', 'TAG 1')
    cy.get('[data-cy="tagLink-tag1"] [data-cy="numSkills"]').should('have.text', '2')

    cy.get('[data-cy="tagLink-tag1"]').click()
    cy.wait('@loadSkillTag1Summary')

    cy.get('[data-cy="title"]').should('contain.text', 'Skill Tag Overview')
    cy.get('[data-cy="skillTagName"]').should('contain.text', 'TAG 1')
    cy.get('[data-cy="skillTagProgress"]').should('contain.text', '1 / 2 Skills')

    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100')

    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 100 Points')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '0')

    cy.get('[data-cy=toggleSkillDetails]').click();
    cy.wait('@loadSkillTagDescriptions')
    cy.get('[data-cy="skillDescription-skill1"]').contains('This is skill1');
    cy.get('[data-cy="skillDescription-skill3"]').contains('This is skill3');
  })

  it('navigate to skill tag overview page from the subject page', () => {

    cy.cdVisit('/')
    cy.cdClickSubj(0)
    cy.wait('@loadSkillTagsForSubject1')

    cy.get('[data-cy="skillTags"]')
      .should('be.visible')

    cy.get('[data-cy="tagLink-tag1"]')
      .should('be.visible')
      .and('have.attr', 'href', '/test-skills-display/proj1/tags/tag1')
    cy.get('[data-cy="tagLink-tag1"] [data-cy="tagName"]').should('have.text', 'TAG 1')
    cy.get('[data-cy="tagLink-tag1"] [data-cy="numSkills"]').should('have.text', '2')

    cy.get('[data-cy="tagLink-tag1"]').click()
    cy.wait('@loadSkillTag1Summary')

    cy.get('[data-cy="title"]').should('contain.text', 'Skill Tag Overview')
    cy.get('[data-cy="skillTagName"]').should('contain.text', 'TAG 1')
    cy.get('[data-cy="skillTagProgress"]').should('contain.text', '1 / 2 Skills')

    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100')

    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 100 Points')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '0')

    cy.get('[data-cy=toggleSkillDetails]').click();
    cy.wait('@loadSkillTagDescriptions')
    cy.get('[data-cy="skillDescription-skill1"]').contains('This is skill1');
    cy.get('[data-cy="skillDescription-skill3"]').contains('This is skill3');
  })

  it('navigate to project skill tags page from the skill tag overview page', () => {

    cy.cdVisit('/tags/tag1')
    cy.wait('@loadSkillTag1Summary')

    cy.get('[data-cy="title"]').should('contain.text', 'Skill Tag Overview')
    cy.get('[data-cy="skillTagName"]').should('contain.text', 'TAG 1')
    cy.get('[data-cy="skillTagProgress"]').should('contain.text', '1 / 2 Skills')

    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100')

    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 100 Points')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '0')

    cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy=breadcrumbLink-Tags]').click()
    cy.wait('@loadSkillTagsSummary')

    cy.get('#tagRow-tag2').within(() => {
      cy.get('[data-cy="tagLink-tag2"]')
        .should('be.visible')
        .and('have.text', 'TAG 2')
        .and('have.attr', 'href', '/test-skills-display/proj1/tags/tag2');

      cy.get('[data-cy="skillTagProgress"]')
        .should('be.visible')
        .and('contain.text', '0 / 3 Skills');

      cy.get('[data-cy="thirdProgressBar"]')
        .should('be.visible')
        .and('have.attr', 'aria-valuenow', '0')
        .and('have.attr', 'aria-valuemax', '100');

      cy.get('[data-cy="thirdProgressBar"] .p-progressbar-value')
        .should('have.attr', 'style')
        .and('contain', 'width: 0%');
    });

    cy.get('#tagRow-tag1').within(() => {
      cy.get('[data-cy="tagLink-tag1"]')
        .should('be.visible')
        .and('have.text', 'TAG 1')
        .and('have.attr', 'href', '/test-skills-display/proj1/tags/tag1');

      cy.get('[data-cy="skillTagProgress"]')
        .should('be.visible')
        .and('contain.text', '1 / 2 Skills');

      cy.get('[data-cy="thirdProgressBar"]')
        .should('be.visible')
        .and('have.attr', 'aria-valuenow', '50')
        .and('have.attr', 'aria-valuemax', '100');

      cy.get('[data-cy="thirdProgressBar"] .p-progressbar-value')
        .should('have.attr', 'style')
        .and('contain', 'width: 50%');
    });
  })

  it('navigate to skill tag overview page from the project skill tags page - title link', () => {

    cy.cdVisit('/tags')
    cy.wait('@loadSkillTagsSummary')

    cy.get('[data-cy="tagLink-tag1"]').should('exist')
    cy.get('[data-cy="tagLink-tag1"]').click()
    cy.wait('@loadSkillTag1Summary')

    cy.get('[data-cy="title"]').should('contain.text', 'Skill Tag Overview')
    cy.get('[data-cy="skillTagName"]').should('contain.text', 'TAG 1')
    cy.get('[data-cy="skillTagProgress"]').should('contain.text', '1 / 2 Skills')

    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100')

    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 100 Points')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '0')

    cy.get('[data-cy=toggleSkillDetails]').click();
    cy.wait('@loadSkillTagDescriptions')
    cy.get('[data-cy="skillDescription-skill1"]').contains('This is skill1');
    cy.get('[data-cy="skillDescription-skill3"]').contains('This is skill3');

    // verify the tag title and tag progress bars are not links when on the tag overview page
    cy.get('[data-cy="tagLink-tag1"]').should('not.exist')
    cy.get('[data-cy="tagProgressLink-tag1"]').should('not.exist')
  })

  it('navigate to skill tag overview page from the project skill tags page - progress link', () => {

    cy.cdVisit('/tags')
    cy.wait('@loadSkillTagsSummary')

    cy.get('[data-cy="tagProgressLink-tag1"]').should('exist')
    cy.get('[data-cy="tagProgressLink-tag1"]').click()
    cy.wait('@loadSkillTag1Summary')

    cy.get('[data-cy="title"]').should('contain.text', 'Skill Tag Overview')
    cy.get('[data-cy="skillTagName"]').should('contain.text', 'TAG 1')
    cy.get('[data-cy="skillTagProgress"]').should('contain.text', '1 / 2 Skills')

    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
    cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '100')

    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Very Great Skill 3')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 100 Points')
    cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"] [data-cy="skillPoints"]').should('have.text', '0')

    cy.get('[data-cy=toggleSkillDetails]').click();
    cy.wait('@loadSkillTagDescriptions')
    cy.get('[data-cy="skillDescription-skill1"]').contains('This is skill1');
    cy.get('[data-cy="skillDescription-skill3"]').contains('This is skill3');

    // verify the tag title and tag progress bars are not links when on the tag overview page
    cy.get('[data-cy="tagLink-tag1"]').should('not.exist')
    cy.get('[data-cy="tagProgressLink-tag1"]').should('not.exist')
  })
})