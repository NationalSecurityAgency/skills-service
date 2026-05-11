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

describe('Skill Groups Accessibility Tests', () => {
  beforeEach(() => {
    // Create test project and data
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkillsGroup(1, 1, 1)
    cy.addSkillToGroup(1, 1, 1, 1, {
      pointIncrement: 10,
      numPerformToCompletion: 5
    })
    cy.addSkillToGroup(1, 1, 1, 2, {
      pointIncrement: 10,
      numPerformToCompletion: 5
    })
    cy.addSkillToGroup(1, 1, 1, 3, {
      pointIncrement: 10,
      numPerformToCompletion: 5
    })

    cy.intercept('GET', '/admin/projects/proj1/groups/group1/skills').as('getGroupSkills')
  })

  const runWithDarkMode = ['', ' - dark mode']

  runWithDarkMode.forEach((darkMode) => {
    it(`skill groups page${darkMode}`, () => {
      cy.setDarkModeIfNeeded(darkMode)
      // Test page header and navigation
      cy.visit('/administrator/projects/proj1/subjects/subj1/groups/group1')
      cy.wait('@getGroupSkills')
      cy.injectAxe()

      // Test accessibility
      cy.customLighthouse()
      cy.customA11y()
    })
  })
})
