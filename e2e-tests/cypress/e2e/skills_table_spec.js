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
const moment = require('moment-timezone');
describe('Skills Table Tests', () => {
  const tableSelector = '[data-cy="skillsTable"]'

  beforeEach(() => {
    cy.request('POST', '/app/projects/proj1', {
      projectId: 'proj1',
      name: 'proj1'
    })
    cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      name: 'Subject 1'
    })

    cy.cleanupDownloadsDir()
  })

  it('create first skill then remove it', () => {
    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.contains('No Skills Yet')

    const skillName = 'This is a Skill'
    cy.get('[data-cy="newSkillButton"').click()
    cy.get('[data-cy="skillName"]').type(skillName)
    cy.clickSave()

    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: skillName }, { colIndex: 3, value: 1 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 1)

    cy.get('[data-cy="deleteSkillButton_ThisisaSkillSkill"]').click()
    cy.acceptRemovalSafetyCheck()
    cy.contains('No Skills Yet')
  })

  it('cancelling delete confirmation should return focus to delete button', () => {
    const numSkills = 3
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: `Very Great Skill # ${skillsCounter}`,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter < 3 ? '1' : '200'
      })
    }


    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills').as('getSkills')
    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@getSkills')

    cy.get('[data-cy=deleteSkillButton_skill2]').click()
    cy.contains('Removal Safety Check').should('exist')
    cy.get('[data-cy=closeDialogBtn]').click()
    cy.contains('Removal Safety Check').should('not.exist')
    cy.wait(200)
    cy.get('[data-cy=deleteSkillButton_skill2]').should('have.focus')
  })

  it('copy existing skill', () => {
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/copy_of_skill2').as('saveSkill')
    cy.intercept('POST', '/api/validation/description*').as('validateDescription')

    const numSkills = 3
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        description: 'generic description',
        name: `Very Great Skill # ${skillsCounter}`,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter < 3 ? '1' : '200'
      })
    }


    cy.visit('/administrator/projects/proj1/subjects/subj1')

    // force the order
    cy.get('[data-pc-section="headercontent"]').contains('Display').click()

    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Very Great Skill # 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Very Great Skill # 2' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Very Great Skill # 3' }, { colIndex: 3, value: 3 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)

    const makdownDivSelector = '#markdown-editor div.toastui-editor-main.toastui-editor-ww-mode > div > div.toastui-editor-ww-container > div > div'
    cy.get('[data-cy="copySkillButton_skill2"]').click()
    cy.get(`${makdownDivSelector}`).should('have.text', 'generic description')
    cy.get('[data-cy=skillName]').should('have.value', 'Copy of Very Great Skill # 2')
    cy.get('#idInput').should('have.value', 'copy_of_skill2')
    cy.get('[data-cy=numPerformToCompletion] [data-pc-name="input"]').should('have.value', '1')
    cy.get('[data-cy=pointIncrement] [data-pc-name="input"]').should('have.value', '150')
    cy.get(makdownDivSelector).type('{selectall}copy description edit')
    cy.wait('@validateDescription')
    cy.get('[data-cy=numPerformToCompletion]').type('5')
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click()
    cy.wait('@saveSkill')

    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Very Great Skill # 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Very Great Skill # 2' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Very Great Skill # 3' }, { colIndex: 3, value: 3 }],
      [{ colIndex: 2, value: 'Copy of Very Great Skill # 2' }, { colIndex: 3, value: 4 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 4)
  })

  it('edit existing skill', () => {

    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/skill2').as('saveSkill')

    const numSkills = 3
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: `Very Great Skill # ${skillsCounter}`,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter < 3 ? '1' : '200'
      })
    }

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    // force the order
    cy.get('[data-pc-section="headercontent"]').contains('Display').click()

    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Very Great Skill # 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Very Great Skill # 2' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Very Great Skill # 3' }, { colIndex: 3, value: 3 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)

    cy.get('[data-cy="editSkillButton_skill2"]').click()
    const otherSkillName = 'Other Skill'
    cy.get('[data-cy="skillName"]').clear().type(otherSkillName)
    cy.clickSave()
    cy.wait('@saveSkill')

    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Very Great Skill # 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: otherSkillName }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Very Great Skill # 3' }, { colIndex: 3, value: 3 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)
  })

  it('sort by skill and order', () => {
    const numSkills = 13
    const expected = []
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      expected.push([{ colIndex: 2, value: skillName }, { colIndex: 3, value: skillsCounter }])
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter < 3 ? '1' : '200'
      })
    }

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    // test skill name sorting
    cy.get(`${tableSelector} th`).contains('Skill').click()
    cy.validateTable(tableSelector, expected, 10)

    cy.get(`${tableSelector} th`).contains('Skill').click()
    cy.validateTable(tableSelector, expected.map((item) => item).reverse(), 10)

    cy.get(`${tableSelector} th`).contains('Display').click()
    cy.validateTable(tableSelector, expected, 10)


    cy.get(`${tableSelector} th`).contains('Display').click()
    cy.validateTable(tableSelector, expected.map((item) => item).reverse(), 10)

    // export skill metrics and verify that the file exists
    const exportedFileName = `cypress/downloads/proj1-skills-${moment.utc().format('YYYY-MM-DD')}.xlsx`;
    cy.readFile(exportedFileName).should('not.exist');
    cy.get('[data-cy="exportSkillsTableBtn"]').click();
    cy.readFile(exportedFileName).should('exist');
  })

  it('skills table - exporting shows loading indicator', () => {
    const numSkills = 13
    const expected = []
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      expected.push([{ colIndex: 2, value: skillName }, { colIndex: 3, value: skillsCounter }])
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter < 3 ? '1' : '200'
      })
    }

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    // delay export and verify loading indicator is showing
    cy.intercept('GET', 'admin/projects/proj1/subjects/subj1/skills/export/excel', {
      delay: 1000
    }).as('exportSkillsTable');
    const exportedFileName = `cypress/downloads/proj1-skills-${moment.utc().format('YYYY-MM-DD')}.xlsx`;
    cy.get('[data-cy="exportSkillsTableBtn"]').click();
    cy.get('[data-cy="skillsTable-loading"]').should('exist');
    cy.wait('@exportSkillsTable');
    cy.get('[data-cy="skillsTable-loading"]').should('not.exist');
  });


  it('sort by created date', () => {

    const numSkills = 3
    const expected = []
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      expected.push([{ colIndex: 2, value: skillName }, { colIndex: 3, value: skillsCounter }])
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter
      })
      cy.wait(1001)
    }


    cy.visit('/administrator/projects/proj1/subjects/subj1')

    // test created column
    cy.get(`${tableSelector} th`).contains('Created').click()
    cy.validateTable(tableSelector, expected, 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)

    cy.get(`${tableSelector} th`).contains('Created').click()
    cy.validateTable(tableSelector, expected.map((item) => item).reverse(), 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)
  })

  it('sort by additional fields', () => {

    const numSkills = 3
    const expected = []
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      expected.push([{ colIndex: 2, value: skillName }, { colIndex: 3, value: skillsCounter }])
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter,
        version: skillsCounter
      })
    }


    cy.visit('/administrator/projects/proj1/subjects/subj1')

    // test points column
    cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
    cy.get('[data-pc-section="panel"] [aria-label="Points"]').click()
    cy.get(`${tableSelector} th`).contains('Points').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 5, value: 150 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 5, value: 300 }],
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 5, value: 450 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)

    // test points column
    cy.get(`${tableSelector} th`).contains('Points').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 5, value: 450 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 5, value: 300 }],
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 5, value: 150 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)

    // test version column
    cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
    cy.get('[data-pc-section="panel"] [aria-label="Version"]').click()
    cy.get(`${tableSelector} th`).contains('Version').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 6, value: 1 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 6, value: 2 }],
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 6, value: 3 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)

    cy.get(`${tableSelector} th`).contains('Version').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 6, value: 3 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 6, value: 2 }],
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 6, value: 1 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)
  })

  it('Time Window field formatting', () => {

    const numSkills = 4
    const expected = []
    for (let skillsCounter = 0; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      expected.push([{ colIndex: 2, value: skillName }, { colIndex: 3, value: skillsCounter }])
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        pointIncrementInterval: 30 * (skillsCounter),
        numPerformToCompletion: skillsCounter <= 1 ? 1 : skillsCounter,
        version: skillsCounter + 1
      })
    }


    cy.visit('/administrator/projects/proj1/subjects/subj1')

    // test points column
    // cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Time Window').click();
    cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
    cy.get('[data-pc-section="panel"] [aria-label="Time Window"]').click()
    cy.get(`${tableSelector} th`).contains('Display').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 0' }, { colIndex: 5, value: 'Time Window Disabled' }],
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 5, value: 'Time Window N/A' }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 5, value: '1 Hour' }],
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 5, value: '1 Hour 30 Minutes' }],
      [{ colIndex: 2, value: 'Skill # 4' }, { colIndex: 5, value: '2 Hours' }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 5)

  })

  it('Self Reporting Type additional field', () => {
    cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem' })
    cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' })
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' })
    cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' })

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
    cy.get('[data-pc-section="panel"] [aria-label="Self Report"]').click()
    cy.get(`${tableSelector} th`).contains('Self Report').click()

    cy.validateTable(tableSelector, [
      [{ colIndex: 5, value: 'Approval' }],
      [{ colIndex: 5, value: 'Approval' }],
      [{ colIndex: 2, value: 'Very Great Skill 3' }, { colIndex: 5, value: 'Disabled' }],
      [{ colIndex: 5, value: 'Honor System' }],
      [{ colIndex: 5, value: 'Honor System' }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 5)
  })

  it('display Disabled for self reporting type for a new (non self-reporting) skill', () => {
    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get('[data-cy="newSkillButton"]').click()
    cy.get('[data-cy="skillName"]').type('Disabled Test')
    cy.clickSave()

    cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
    cy.get('[data-pc-section="panel"] [aria-label="Self Report"]').click()
    cy.get(`${tableSelector} th`).contains('Self Report').click()

    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Disabled Test' }, { colIndex: 5, value: 'Disabled' }]
    ], 10, false, null, false)
  })

  it('change display order', () => {
    const numSkills = 4
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter,
        version: skillsCounter
      })
    }


    cy.visit('/administrator/projects/proj1/subjects/subj1')

    // sort by name in descending order
    cy.get(`${tableSelector} th`).contains('Skill').click()
    cy.get(`${tableSelector} th`).contains('Skill').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 4' }, { colIndex: 3, value: 4 }],
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 3, value: 3 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 3, value: 1 }]
    ], 10, false, null, false)

    // sort does not exist by default
    for (let i = 1; i <=4 ; i++) {
      cy.get(`[data-cy="orderMoveUp_skill${i}"]`).should('not.exist')
      cy.get(`[data-cy="orderMoveDown_skill${i}"]`).should('not.exist')
    }

    // enable reorder should add buttons and sort by display order
    cy.get('[data-cy="enableDisplayOrderSort"]').click()

    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 3, value: 3 }],
      [{ colIndex: 2, value: 'Skill # 4' }, { colIndex: 3, value: 4 }]
    ], 10, false, null, false)

    cy.get('[data-cy="orderMoveUp_skill1"]').should('be.disabled')
    cy.get('[data-cy="orderMoveUp_skill2"]').should('be.enabled')
    cy.get('[data-cy="orderMoveUp_skill3"]').should('be.enabled')
    cy.get('[data-cy="orderMoveUp_skill4"]').should('be.enabled')
    cy.get('[data-cy="orderMoveDown_skill1"]').should('be.enabled')
    cy.get('[data-cy="orderMoveDown_skill2"]').should('be.enabled')
    cy.get('[data-cy="orderMoveDown_skill3"]').should('be.enabled')
    cy.get('[data-cy="orderMoveDown_skill4"]').should('be.disabled')

    cy.get('[data-cy="orderMoveUp_skill3"]').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 3, value: 3 }],
      [{ colIndex: 2, value: 'Skill # 4' }, { colIndex: 3, value: 4 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 4)

    cy.get('[data-cy="orderMoveDown_skill1"]').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 3, value: 3 }],
      [{ colIndex: 2, value: 'Skill # 4' }, { colIndex: 3, value: 4 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 4)
    cy.get('[data-cy="orderMoveUp_skill1"]').should('be.enabled')
    cy.get('[data-cy="orderMoveUp_skill2"]').should('be.enabled')
    cy.get('[data-cy="orderMoveUp_skill3"]').should('be.disabled')
    cy.get('[data-cy="orderMoveUp_skill4"]').should('be.enabled')
    cy.get('[data-cy="orderMoveDown_skill1"]').should('be.enabled')
    cy.get('[data-cy="orderMoveDown_skill2"]').should('be.enabled')
    cy.get('[data-cy="orderMoveDown_skill3"]').should('be.enabled')
    cy.get('[data-cy="orderMoveDown_skill4"]').should('be.disabled')
  })

  it('sorting, filtering, additional fields and paging disables reorder', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get('[data-cy="enableDisplayOrderSort"]').click()
    cy.get('[data-cy="orderMoveUp_skill1"]').should('exist')
    cy.get('[data-cy="orderMoveUp_skill2"]').should('exist')
    cy.get('[data-cy="orderMoveDown_skill1"]').should('exist')
    cy.get('[data-cy="orderMoveDown_skill2"]').should('exist')

    // sorting disables
    cy.get(`${tableSelector} th`).contains('Skill').click()
    cy.get('[data-cy="orderMoveUp_skill1"]').should('not.exist')
    cy.get('[data-cy="orderMoveUp_skill2"]').should('not.exist')
    cy.get('[data-cy="orderMoveDown_skill1"]').should('not.exist')
    cy.get('[data-cy="orderMoveDown_skill2"]').should('not.exist')

    cy.get('[data-cy="enableDisplayOrderSort"]').click();
    cy.get('[data-cy="orderMoveUp_skill1"]').should('exist')
    cy.get('[data-cy="orderMoveUp_skill2"]').should('exist')
    cy.get('[data-cy="orderMoveDown_skill1"]').should('exist')
    cy.get('[data-cy="orderMoveDown_skill2"]').should('exist')

    // filtering disables
    cy.get('[data-cy="skillsTable-skillFilter"]').type('s')
    cy.get('[data-cy="orderMoveUp_skill1"]').should('not.exist')
    cy.get('[data-cy="orderMoveUp_skill2"]').should('not.exist')
    cy.get('[data-cy="orderMoveDown_skill1"]').should('not.exist')
    cy.get('[data-cy="orderMoveDown_skill2"]').should('not.exist')

    // enabling again remove search
    cy.get('[data-cy="skillsTable-skillFilter"]').should('have.value', 's')
    cy.get('[data-cy="enableDisplayOrderSort"]').click();
    cy.get('[data-cy="orderMoveUp_skill1"]').should('exist')
    cy.get('[data-cy="orderMoveUp_skill2"]').should('exist')
    cy.get('[data-cy="orderMoveDown_skill1"]').should('exist')
    cy.get('[data-cy="orderMoveDown_skill2"]').should('exist')
    cy.get('[data-cy="skillsTable-skillFilter"]').should('not.have.value', 's')
  })

  it('enabling reorder clears search', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get('[data-cy="skillsTable-skillFilter"]').type('s')
    cy.get('[data-cy="skillsTable-skillFilter"]').should('have.value', 's')
    cy.get('[data-cy="enableDisplayOrderSort"]').click();
    cy.get('[data-cy="skillsTable-skillFilter"]').should('not.have.value', 's')
  })

  it('change display order and validate that manage skill navigation still works', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get('[data-cy="enableDisplayOrderSort"]').click()
    cy.get('[data-cy="orderMoveUp_skill3"]').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Very Great Skill 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Very Great Skill 3' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Very Great Skill 2' }, { colIndex: 3, value: 3 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 3)

    cy.get('[data-cy="manageSkillLink_skill3"]').click()
    cy.get('[data-cy="pageHeader"]').contains('ID: skill3')
  })

  it('change display order with the last item on the current page', () => {
    const numSkills = 12
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter,
        version: skillsCounter
      })
    }


    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get('[data-cy="enableDisplayOrderSort"]').click()
    cy.get('[data-cy="orderMoveDown_skill10"]').click()

    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 3, value: 3 }],
      [{ colIndex: 2, value: 'Skill # 4' }, { colIndex: 3, value: 4 }],
      [{ colIndex: 2, value: 'Skill # 5' }, { colIndex: 3, value: 5 }],
      [{ colIndex: 2, value: 'Skill # 6' }, { colIndex: 3, value: 6 }],
      [{ colIndex: 2, value: 'Skill # 7' }, { colIndex: 3, value: 7 }],
      [{ colIndex: 2, value: 'Skill # 8' }, { colIndex: 3, value: 8 }],
      [{ colIndex: 2, value: 'Skill # 9' }, { colIndex: 3, value: 9 }],
      [{ colIndex: 2, value: 'Skill # 11' }, { colIndex: 3, value: 10 }],
      [{ colIndex: 2, value: 'Skill # 10' }, { colIndex: 3, value: 11 }],
      [{ colIndex: 2, value: 'Skill # 12' }, { colIndex: 3, value: 12 }]
    ], 10)
  })

  it('filter by skill name', () => {
    const numSkills = 12
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter,
        version: skillsCounter
      })
    }

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get(`${tableSelector} th`).contains('Display').click()

    // look for the name
    cy.get('[data-cy="skillsTable-skillFilter"]').type('# 1')

    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Skill # 10' }, { colIndex: 3, value: 10 }],
      [{ colIndex: 2, value: 'Skill # 11' }, { colIndex: 3, value: 11 }],
      [{ colIndex: 2, value: 'Skill # 12' }, { colIndex: 3, value: 12 }]
    ], 10, false, null, false)

    cy.get('[data-cy="skillsTable-skillFilter"]').type('2')
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 12' }, { colIndex: 3, value: 12 }]
    ], 10, false, null, false)
    cy.get(`${tableSelector} tbody tr`).should('have.length', 1)

    cy.get('[data-cy="filterResetBtn"]').click()
    cy.get('[data-cy=skillsBTableTotalRows]').contains(12)

    // should be case insensitive
    cy.get('[data-cy="skillsTable-skillFilter"]').type('sKiLl # 5')
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 5' }, { colIndex: 3, value: 5 }]
    ], 10, false, null, false)

    // filter all records
    cy.get('[data-cy="skillsTable-skillFilter"]').type('a')
    cy.get(tableSelector).contains(`No Skills Found`)

    // reset list by clearing filter when there are no results
    cy.get('[data-cy="skillResetBtnNoFilterRes"]').click()
    cy.get('[data-cy=skillsBTableTotalRows]').contains(12)
  })

  it('paging controls should be present and work when results greater than smallest page size', () => {
    const numSkills = 12
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter,
        version: skillsCounter
      })
    }

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get(`${tableSelector} th`).contains('Display').click()

    // look for the name
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Skill # 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Skill # 2' }, { colIndex: 3, value: 2 }],
      [{ colIndex: 2, value: 'Skill # 3' }, { colIndex: 3, value: 3 }],
      [{ colIndex: 2, value: 'Skill # 4' }, { colIndex: 3, value: 4 }],
      [{ colIndex: 2, value: 'Skill # 5' }, { colIndex: 3, value: 5 }],
      [{ colIndex: 2, value: 'Skill # 6' }, { colIndex: 3, value: 6 }],
      [{ colIndex: 2, value: 'Skill # 7' }, { colIndex: 3, value: 7 }],
      [{ colIndex: 2, value: 'Skill # 8' }, { colIndex: 3, value: 8 }],
      [{ colIndex: 2, value: 'Skill # 9' }, { colIndex: 3, value: 9 }],
      [{ colIndex: 2, value: 'Skill # 10' }, { colIndex: 3, value: 10 }],
      [{ colIndex: 2, value: 'Skill # 11' }, { colIndex: 3, value: 11 }],
      [{ colIndex: 2, value: 'Skill # 12' }, { colIndex: 3, value: 12 }]
    ], 10, false, 12)

  })

  it('expand details', () => {
    const numSkills = 3
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter,
        version: skillsCounter
      })
    }

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.get(`${tableSelector} th`).contains('Skill').click()
    cy.get('[data-p-index="1"] [data-pc-section="rowtoggler"]').click()
    cy.get('[data-cy="childRowDisplay_skill2"]').contains('Minimum Time Window between occurrences')
    cy.get('[data-cy="childRowDisplay_skill2"]').contains('300 Points')

    // cy.get('[data-cy="expandDetailsBtn_skill1"]').click()
    cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
    cy.get('[data-cy="childRowDisplay_skill1"]').contains('150 Points')
    cy.get('[data-cy="childRowDisplay_skill1"]').contains('Time Window N/A')

    cy.get('[data-p-index="1"] [data-pc-section="rowtoggler"]').click()
    cy.get('[data-cy="childRowDisplay_skill2"]').should('not.exist')

    cy.get('[data-cy="childRowDisplay_skill1"]').contains('150 Points')
    cy.get('[data-p-index="0"] [data-pc-section="rowtoggler"]').click()
    cy.get('[data-cy="childRowDisplay_skill1"]').should('not.exist')
  })

  it('navigate to skill details page', () => {
    const numSkills = 3
    for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
      const skillName = `Skill # ${skillsCounter}`
      cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
        projectId: 'proj1',
        subjectId: 'subj1',
        skillId: `skill${skillsCounter}`,
        name: skillName,
        pointIncrement: '150',
        numPerformToCompletion: skillsCounter,
        version: skillsCounter
      })
    }

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.get('[data-cy=manageSkillLink_skill2]').click()
    cy.contains('ID: skill2')
    cy.contains('Overview')
    cy.contains('300 Points')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.get('[data-cy=manageSkillLink_skill3]').click()
    cy.contains('ID: skill3')
    cy.contains('Overview')
    cy.contains('450 Points')

  })

  it('sort column is saved in local storage', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.get(`${tableSelector} th`).contains('Skill').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Very Great Skill 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Very Great Skill 2' }, { colIndex: 3, value: 2 }]
    ], 10, false, null, false)

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Very Great Skill 1' }, { colIndex: 3, value: 1 }],
      [{ colIndex: 2, value: 'Very Great Skill 2' }, { colIndex: 3, value: 2 }]
    ], 10, false, null, false)
  })

  it('when action menu is open tab key is disabled and when closed focus is placed back to the action menu button', () => {
    cy.createSkill(1, 1,1 )
    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
    cy.get('[data-cy="enableDisplayOrderSort"] input').tab().type('{enter}');
    cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]')
    cy.focused().tab()
    cy.get('[data-cy="skillActionsBtn"]').should('have.focus')
    cy.focused().tab()
    cy.get('[data-cy="exportSkillsTableBtn"]').should('have.focus')
  })

})

