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
import dayjs from 'dayjs'

const moment = require('moment-timezone')

describe('Skill Tags Accessibility Tests', () => {
  beforeEach(() => {
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1);
    cy.createSubject(1, 2);
    cy.createSkill(1, 1, 2);
    cy.createSkill(1, 1, 3);

    cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3'], 1)
    cy.addTagToSkills(1, ['skill1', 'skill2'], 2)
    cy.addTagToSkills(1, ['skill1'], 3)

  })

  const runWithDarkMode = ['', ' - dark mode']

  runWithDarkMode.forEach((darkMode) => {
    it(`skill tags page${darkMode}`, () => {
      cy.setDarkModeIfNeeded(darkMode)
      // Test page header and navigation
      cy.visit('/administrator/projects/proj1/skills-tags')
      cy.injectAxe()

      const tagsTableSelector = '[data-cy="skillsTagsTable"]'
      cy.validateTable(tagsTableSelector, [
        [{colIndex: 0, value: 'TAG 3'}],
        [{colIndex: 0, value: 'TAG 2'}],
        [{colIndex: 0, value: 'TAG 1'}],
      ], 25);

      // Test accessibility
      cy.customLighthouse()
      cy.customA11y()
    })

    it(`single skill tag page${darkMode}`, () => {
      cy.setDarkModeIfNeeded(darkMode)
      // Test page header and navigation
      cy.visit('/administrator/projects/proj1/skills-tags/tag1')
      cy.injectAxe()

      const skillsTable = '[data-cy="skillTagSkillsTable"]'
      cy.validateTable(skillsTable, [
        [{colIndex: 0, value: 'Very Great Skill 3'}],
        [{colIndex: 0, value: 'Very Great Skill 2'}],
        [{colIndex: 0, value: 'Very Great Skill 1'}],
      ], 25);

      // Test accessibility
      cy.customLighthouse()
      cy.customA11y()
    })
  })
})
