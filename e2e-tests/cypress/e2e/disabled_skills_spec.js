/*
 * Copyright 2025 SkillTree
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

describe('Disabled Skills Tests', () => {

  const tableSelector = '[data-cy="skillsTable"]'
  
  it('create initially disabled skill', () => {
    cy.createProject(1);
    cy.createSubject(1, 1);

    const expectedId = 'InitiallyDisabledSkillSkill'
    const providedName = 'Initially Disabled Skill'

    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill')
    cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists')
    cy.intercept('GET', `/admin/projects/proj1/entityIdExists?id=*`).as('skillIdExists')

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.openNewSkillDialog()

    cy.get('[data-cy="skillName"]').type(providedName)
    cy.wait('@nameExists')
    cy.wait('@skillIdExists')

    cy.get('[data-cy="visibilitySwitch"]').click()
    cy.clickSave()
    cy.wait('@postNewSkill')

    cy.get('[data-cy="disabledBadge-InitiallyDisabledSkillSkill"]').should('be.visible')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('have.text', '1');

    cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
    cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: `${providedName}` }, { colIndex: 5, value: 100 }]
    ], 10, false, null, false)

    cy.visit('/administrator/projects/proj1')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('have.text', '1');

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/InitiallyDisabledSkillSkill')
    cy.get('[data-cy="pageHeader"] [data-cy="disabledSkillBadge"]');
    cy.get('[data-cy="childRowDisplay_InitiallyDisabledSkillSkill"]').should('contain.text','This skill is disabled');
  })

  it('enable a disabled skill on the subject page', () => {
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1, { enabled: false })
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('loadSkill1')
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('saveSkill1')
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/entirelyNewId').as('afterIdEdit')
    cy.intercept('POST', '/api/validation/description*').as('validateDescription')
    cy.intercept('POST', '/api/validation/url').as('validateUrl')

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')

    cy.get('[data-cy="disabledBadge-skill1"]').should('be.visible')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('have.text', '1');

    cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
    cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: 'Very Great Skill 1' }, { colIndex: 5, value: 100 }]
    ], 10, false, null, false)

    cy.get('[data-cy=editSkillButton_skill1]').click()
    cy.get('[data-cy="visibilitySwitch"]').click()
    cy.clickSave()
    cy.wait('@saveSkill1')

    cy.get('[data-cy="disabledBadge-skill1"]').should('not.exist')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '200');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '1');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('not.exist');

    cy.get('[data-cy=editSkillButton_skill1]').click()
    cy.get('[data-cy="visibilitySwitch"]').should('not.exist');
  })

  it('enable a disabled skill on the skill page', () => {
    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1, { enabled: false })
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('loadSkill1')
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('saveSkill1')
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/entirelyNewId').as('afterIdEdit')
    cy.intercept('POST', '/api/validation/description*').as('validateDescription')
    cy.intercept('POST', '/api/validation/url').as('validateUrl')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
    cy.wait('@loadSkill1')

    cy.get('[data-cy="pageHeader"] [data-cy="disabledSkillBadge"]');
    cy.get('[data-cy="childRowDisplay_skill1"]').should('contain.text','This skill is disabled');

    cy.contains('SKILL: Very Great Skill 1').should('be.visible')
    cy.get('[data-cy=childRowDisplay_skill1]').should('be.visible')
    // skill should now only be loaded once on page load instead of twice, once by SkillPage and another time by SkillOverview
    cy.get('@loadSkill1.all').should('have.length', 1)

    cy.get('[data-cy=editSkillButton_skill1]').click()
    cy.get('[data-cy="visibilitySwitch"]').click()
    cy.clickSave()
    cy.wait('@saveSkill1')

    cy.get('[data-cy="pageHeader"] [data-cy="disabledSkillBadge"]').should('not.exist');
    cy.get('[data-cy="childRowDisplay_skill1"]').should('not.contain.text','This skill is disabled');
  })

  it('cannot add skill events for disabled skills', function () {
    cy.intercept('/admin/projects/proj1/subjects/subj1/skills/skill1').as('getSkill1')

    cy.createProject(1);
    cy.createSubject(1, 1);
    cy.createSkill(1, 1, 1, { enabled: false })
    cy.createSkill(1, 1, 2)

    // don't even show the add event link for imported skills
    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
    cy.get('[data-cy="nav-Add Event"]').should('not.exist');

    // navigate directly to the add skill event page
    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/addSkillEvent');
    cy.wait('@getSkill1')
    cy.get('[data-cy="subPageHeader"]').contains('Add Skill Events')
    cy.get('[data-cy="skillId"]').contains('skill1')

    cy.get('[data-cy="addSkillEventButton"]').should('be.disabled');
    cy.get('[data-cy="addEventDisabledBlockUI"] > [data-pc-section="mask"]').should('exist');
    cy.get('[data-cy="addEventDisabledMsg"]').contains('Unable to add skill for user. Cannot add events to skills that are disabled.');
  })

})
