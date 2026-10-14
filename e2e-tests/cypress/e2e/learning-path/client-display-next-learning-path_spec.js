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

describe('Next learning path items on skill details', () => {
  beforeEach(() => {
    Cypress.env('disabledUILoginProp', true)
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4)
  })

  it('shows immediate branches and updates navigation as the user follows a path', () => {
    cy.addLearningPathItem(1, 1, 2)
    cy.addLearningPathItem(1, 1, 3)
    cy.addLearningPathItem(1, 2, 4)
    cy.cdVisit('/subjects/subj1/skills/skill1')
    cy.get('[data-cy="nextLearningPathItems"] li a').should('have.length', 2)
    cy.get('[data-cy="nextLearningPathItem_skill3"]').should('contain.text', 'Very Great Skill 3')
    cy.get('[data-cy="nextLearningPathItem_skill4"]').should('not.exist')
    cy.get('[data-cy="nextLearningPathItem_skill2"]').click()
    cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 2')
    cy.get('[data-cy="prerequisitesCard"]').should('be.visible').and('contain.text', 'Very Great Skill 1')
    cy.get('[data-cy="nextLearningPathItems"] li a').should('have.length', 1)
    cy.get('[data-cy="nextLearningPathItem_skill4"]').click()
    cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 4')
    cy.get('[data-cy="nextLearningPathItems"]').should('not.exist')
  })

  ;['/test-skills-display/proj1', '/progress-and-rankings/projects/proj1'].forEach((projectPath) => {
    it(`links the heading to the current project's learning path in ${projectPath}`, () => {
      cy.addLearningPathItem(1, 1, 2)
      cy.visit(`${projectPath}/subjects/subj1/skills/skill1`)
      cy.get('[data-cy="nextLearningPathLink"]')
        .should('be.visible').and('have.text', 'Learning Path')
        .and('have.attr', 'href', `${projectPath}/learning-path`).click()
      cy.location('pathname').should('equal', `${projectPath}/learning-path`)
      cy.get('[data-cy="learningPathTable"] [data-cy="fromNodeLink_skill1"]').should('be.visible')
      cy.get('[data-cy="learningPathTable"] [data-cy="toNodeLink_skill2"]').should('be.visible')
    })

    it(`links the shared prerequisite heading to the viewing project's learning path in ${projectPath}`, () => {
      cy.createProject(2)
      cy.createSubject(2, 1)
      cy.createSkill(2, 1, 1)
      cy.addCrossProjectLearningPathItem(2, 1, 1, 2)
      cy.visit(`${projectPath}/learning-path`)
      cy.get('[data-cy="learningPathTable"] [data-cy="fromNodeLink_skill1"]').click()
      cy.get('[data-cy="crossProjAlert"]').should('be.visible')
      cy.get('[data-cy="nextLearningPathLink"]')
        .should('have.attr', 'href', `${projectPath}/learning-path`).click()
      cy.location('pathname').should('equal', `${projectPath}/learning-path`)
      cy.get('[data-cy="learningPathTable"] [data-cy="toNodeLink_skill2"]').should('be.visible')
      cy.get('[data-cy="crossProjAlert"]').should('not.exist')
    })
  })

  it('hides navigation for skills outside the learning path', () => {
    cy.addLearningPathItem(1, 1, 2)
    cy.cdVisit('/subjects/subj1/skills/skill3')
    cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 3')
    cy.get('[data-cy="nextLearningPathItems"]').should('not.exist')
  })

  it('links to a successor badge', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.createBadge(1, 1, { enabled: true })
    cy.addLearningPathItem(1, 1, 1, false, true)
    cy.cdVisit('/subjects/subj1/skills/skill1')
    cy.get('[data-cy="nextLearningPathItem_badge1"]').should('contain.text', 'Badge 1').click()
    cy.get('[data-cy="badge_badge1"] [data-cy="badgeTitle"]').should('contain.text', 'Badge 1')
  })

  ;['/test-skills-display/proj1', '/progress-and-rankings/projects/proj1'].forEach((projectPath) => {
    it(`updates badge successors and links back to the learning path in ${projectPath}`, () => {
      cy.createBadge(1, 1)
      cy.assignSkillToBadge(1, 1, 2)
      cy.createBadge(1, 1, { enabled: true })
      cy.createBadge(1, 2)
      cy.assignSkillToBadge(1, 2, 3)
      cy.createBadge(1, 2, { enabled: true })
      cy.addLearningPathItem(1, 1, 1, false, true)
      cy.addLearningPathItem(1, 1, 2, true, true)
      cy.addLearningPathItem(1, 2, 4, true, false)
      cy.visit(`${projectPath}/subjects/subj1/skills/skill1`)
      cy.get('[data-cy="nextLearningPathItem_badge1"]').click()
      cy.get('[data-cy="badge_badge1"] [data-cy="badgeTitle"]').should('contain.text', 'Badge 1')
      cy.get('[data-cy="prerequisitesCard"]').should('contain.text', 'Very Great Skill 1')
      cy.get('[data-cy="nextLearningPathItems"] li a').should('have.length', 1)
      cy.get('[data-cy="nextLearningPathItem_badge2"]').click()
      cy.get('[data-cy="badge_badge2"] [data-cy="badgeTitle"]').should('contain.text', 'Badge 2')
      cy.get('[data-cy="prerequisitesCard"]').should('contain.text', 'Badge 1')
      cy.get('[data-cy="nextLearningPathItems"] li a').should('have.length', 1)
      cy.get('[data-cy="nextLearningPathItem_badge2"]').should('not.exist')
      cy.get('[data-cy="nextLearningPathLink"]').should('have.attr', 'href', `${projectPath}/learning-path`).click()
      cy.get('[data-cy="learningPathTable"] [data-cy="toNodeLink_badge2"]').should('be.visible').click()
      cy.get('[data-cy="nextLearningPathItem_skill4"]').click()
      cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 4')
      cy.get('[data-cy="nextLearningPathItems"]').should('not.exist')
    })
  })

  it('hides successor navigation on a terminal badge', () => {
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 2)
    cy.createBadge(1, 1, { enabled: true })
    cy.addLearningPathItem(1, 1, 1, false, true)
    cy.cdVisit('/subjects/subj1/skills/skill1')
    cy.get('[data-cy="nextLearningPathItem_badge1"]').click()
    cy.get('[data-cy="badge_badge1"] [data-cy="badgeTitle"]').should('contain.text', 'Badge 1')
    cy.get('[data-cy="prerequisitesCard"]').should('contain.text', 'Very Great Skill 1')
    cy.get('[data-cy="nextLearningPathItems"]').should('not.exist')
  })

  it('links from a shared prerequisite back to the current project', () => {
    cy.createProject(2)
    cy.createSubject(2, 1)
    cy.createSkill(2, 1, 1)
    cy.addCrossProjectLearningPathItem(2, 1, 1, 2)
    cy.cdVisit('/learning-path')
    cy.get('[data-cy="fromNodeLink_skill1"]').click()
    cy.get('[data-cy="crossProjAlert"]').should('be.visible')
    cy.get('[data-cy="nextLearningPathItem_skill2"]').click()
    cy.get('[data-cy="skillProgressTitle"]').should('contain.text', 'Very Great Skill 2')
    cy.get('[data-cy="crossProjAlert"]').should('not.exist')
  })
})
