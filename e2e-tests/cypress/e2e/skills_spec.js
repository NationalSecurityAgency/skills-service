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
var moment = require('moment-timezone')

import dayjs from 'dayjs'

describe('Skills Tests', () => {

  const addButtonSelector = '[data-cy=addSkillEventButton]';
  
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
  })

  it('name causes id to fail validation', () => {

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')

    cy.get('[data-cy=newSkillButton]').click()

    // name causes id to be too long
    const msg = 'Skill ID cannot exceed 100 characters.'
    // 96 becuase 'Skill' is appended to id
    const validNameButInvalidId = Array(96).fill('a').join('')
    cy.get('[data-cy=skillName]').type(validNameButInvalidId)
    cy.get('[data-cy=idError]').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=skillName]').type('{backspace}')
    cy.get('[data-cy=idError]').should('not.be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')
  })

  it('close skill dialog', () => {

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')

    cy.openNewSkillDialog()
    cy.get('[data-cy=closeDialogBtn]').click()
    cy.get('[data-cy=closeDialogBtn]').should('not.exist')
  })

  it('validation', () => {
    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('postNewSkill')
    cy.intercept('GET', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('getSkill')
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject')

    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/duplicate', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'duplicate',
      name: 'Duplicate',
      pointIncrement: '50',
      numPerformToCompletion: '5'
    })

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')

    cy.openNewSkillDialog()
    cy.get('[data-cy=skillName]').type('Skill123')
    cy.get('[data-cy=markdownEditorInput]').type('loremipsum')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')
    cy.get('[data-cy=skillName]').type('{selectall}Sk')
    cy.get('[data-cy=skillNameError]').contains('Skill Name must be at least 3 characters').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    const invalidName = Array(101).fill('a').join('')
    cy.get('[data-cy=skillName]').fill(invalidName)
    cy.get('[data-cy=skillNameError]').contains('Skill Name must be at most 100 characters').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=skillName]').type('{selectall}Duplicate')
    cy.get('[data-cy=skillNameError]').contains('The value for the Skill Name is already taken').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=skillName]').type('{selectall}Skill123')
    cy.get('[data-cy=skillNameError]').should('not.be.visible')

    cy.get('[data-cy=version]').type('{selectall}1000')
    cy.get('[data-cy=versionError]').contains('Version must be less than or equal to 999').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=version]').type('{selectall}2')
    cy.get('[data-cy=versionError]').contains('Version 0 is the latest; max supported version is 1 (latest + 1)').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=version]').type('{selectall}1')
    cy.get('[data-cy=versionError]').should('not.exist')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')

    cy.get('[data-cy="pointIncrement"]').type('{selectall}11111111111')
    cy.get('[data-cy=pointIncrementError]').contains('Point Increment must be less than or equal to 10000').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy="pointIncrement"]').type('{selectall}11')
    cy.get('[data-cy=pointIncrementError]').should('not.exist')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')

    cy.get('[data-cy=numPerformToCompletion]').type('{selectall}{del}')
    cy.get('[data-cy=numPerformToCompletionError]').contains('Occurrences is a required field').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=numPerformToCompletion]').type('{selectall}1000000')
    cy.get('[data-cy=numPerformToCompletionError]').contains('Occurrences must be less than or equal to 10000').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')

    cy.get('[data-cy="timeWindowInput"] [data-pc-section="togglebutton"]').click()
    cy.get('[data-cy=timeWindowCheckbox').click()
    cy.get('[data-cy=numPointIncrementMaxOccurrences]').type('{selectall}{del}')
    cy.get('[data-cy=numPointIncrementMaxOccurrencesError]').contains('Occurrences is a required field').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=numPointIncrementMaxOccurrences]').type('{selectall}5')
    cy.get('[data-cy=numPerformToCompletion]').type('{selectall}3')
    cy.get('[data-cy=numPerformToCompletionError]')
      .contains('Occurrences must be >= Window\'s Max Occurrences').should('be.visible')
    cy.get('[data-cy=numPointIncrementMaxOccurrencesError]')
      .contains('Max Occurrences must be <= total Occurrences to Completion').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=numPointIncrementMaxOccurrences]').type('{selectall}2')
    cy.get('[data-cy=numPerformToCompletionError]').should('not.be.visible')
    cy.get('[data-cy=numPointIncrementMaxOccurrencesError]').should('not.exist')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')
    cy.get('[data-cy=numPerformToCompletion]').type('{selectall}1200')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')
    cy.get('[data-cy=numPointIncrementMaxOccurrences]').type('{selectall}1000')
    cy.get('[data-cy=numPointIncrementMaxOccurrencesError]').contains('Max Occurrences must be less than or equal to 999')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=numPointIncrementMaxOccurrences]').type('{selectall}999')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')

    cy.get('[data-cy=pointIncrementIntervalHrs]').type('{selectall}0')
    cy.get('[data-cy=pointIncrementIntervalHrsError]').contains('Hours must be > 0 if Minutes = 0')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=pointIncrementIntervalMins]').type('{selectall}90')
    cy.get('[data-cy=pointIncrementIntervalHrsError]').should('not.exist')
    cy.get('[data-cy=pointIncrementIntervalMinsError]')
      .contains('Minutes must be less than or equal to 60')
      .should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=pointIncrementIntervalMins]').type('{selectall}0')
    cy.get('[data-cy=pointIncrementIntervalMinsError]').contains('Minutes must be > 0 if Hours = 0')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=pointIncrementIntervalHrs]').type('{selectall}1')
    cy.get('[data-cy=pointIncrementIntervalHrsError]').should('not.exist')
    cy.get('[data-cy=pointIncrementIntervalMinsError]').should('not.exist')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')

    cy.get('[data-cy=pointIncrementIntervalHrs]').type('{selectall}721')
    cy.get('[data-cy=pointIncrementIntervalHrsError]').contains('Hours must be less than or equal to 720')
    cy.get('[data-cy=pointIncrementIntervalMinsError]').should('not.exist')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=pointIncrementIntervalHrs]').type('{selectall}0')
    cy.get('[data-cy=pointIncrementIntervalMins]').type('{selectall}43201')
    cy.get('[data-cy=pointIncrementIntervalMinsError').contains('Minutes must be less than or equal to 60')
    cy.get('[data-cy=pointIncrementIntervalHrsError]').should('not.exist')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=pointIncrementIntervalMins]').type('{selectall}59')
  })

  it('help url validation', () => {
    cy.intercept('POST', '/api/validation/url').as('customUrlValidation')
    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.openNewSkillDialog()

    cy.get('[data-cy=skillName]').type('name')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')

    cy.get('[data-cy=skillHelpUrl]').clear().type('javascript:alert("uh oh");')
    cy.get('[data-cy=skillHelpUrlError]')
      .should('be.visible')
      .contains('Help URL/Path must start with \"/\" or')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    cy.get('[data-cy=skillHelpUrl]').clear().type('/foo?p1=v1&p2=v2')
    cy.get('[data-cy=skillHelpUrlError]').should('not.be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')
    cy.get('[data-cy=skillHelpUrl]').clear().type('http://foo.bar?p1=v1&p2=v2')
    cy.get('[data-cy=skillHelpUrlError]').should('not.be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')
    cy.get('[data-cy=skillHelpUrl]').clear().type('https://foo.bar?p1=v1&p2=v2')
    cy.get('[data-cy=skillHelpUrlError]').should('not.be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')

    cy.get('[data-cy=skillHelpUrl]').clear().type('https://')
    cy.wait('@customUrlValidation')
    cy.get('[data-cy=skillHelpUrlError]').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')

    cy.get('[data-cy=skillHelpUrl]').clear().type('https://---??..??##')
    cy.wait('@customUrlValidation')
    cy.get('[data-cy=skillHelpUrlError]').should('be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.disabled')
    // trailing space should work now
    cy.get('[data-cy=skillHelpUrl]').clear().type('https://foo.bar?p1=v1&p2=v2 ')
    cy.wait('@customUrlValidation')
    cy.get('[data-cy=skillHelpUrlError]').should('not.be.visible')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled')
  })

  it('edit number of occurrences', () => {
    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('postNewSkill')
    cy.intercept('GET', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('getSkill')

    const selectorOccurrencesToCompletion = '[data-cy="numPerformToCompletion"] [data-pc-name="pcinputtext"]'
    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.get('[data-cy="noContent"]')
    cy.openNewSkillDialog()
    cy.get(selectorOccurrencesToCompletion).should('have.value', '1')
    cy.get('[data-cy=skillName]').type('Skill 1')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled').click()

    cy.get('[data-p-index="0"] [data-pc-section="rowtogglebutton"]').click()
    cy.get('[data-cy="childRowDisplay_Skill1Skill"]').contains('100 Points')

    cy.get('[data-cy="editSkillButton_Skill1Skill"]').click()
    cy.get(selectorOccurrencesToCompletion).should('have.value', '1')
    cy.get(selectorOccurrencesToCompletion).type('{backspace}10')
    cy.get(selectorOccurrencesToCompletion).should('have.value', '10')

    cy.get('[data-cy=saveDialogBtn]').should('be.enabled').click()

    // cy.get('[data-pc-section="rowtogglebutton"]').first().click();
    cy.get('[ data-cy="childRowDisplay_Skill1Skill"]').contains('1,000 Points')
  })

  it('create skill with special chars', () => {
    const expectedId = 'LotsofspecialPcharsSkill'
    const providedName = '!L@o#t$s of %s^p&e*c(i)/#?a_l++_|}{P c\'ha\'rs'

    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill')
    cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists')

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.openNewSkillDialog()

    cy.get('#skillName').type(providedName)

    cy.getIdField().should('have.value', expectedId)
    cy.wait('@nameExists')

    cy.clickSave()
    cy.wait('@postNewSkill')
  })

  it('create skill using enter key', () => {
    const expectedId = 'LotsofspecialPcharsSkill'
    const providedName = '!L@o#t$s of %s^p&e*c(i)/#?a_l++_|}{P c\'ha\'rs'

    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill')
    cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists')

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.openNewSkillDialog()

    cy.get('[data-cy="skillName"]').type(providedName)

    cy.getIdField().should('have.value', expectedId)
    cy.wait('@nameExists')

    cy.get('[data-cy="skillName"]').type('{enter}')
    cy.wait('@postNewSkill')
  })

  it('Open new skill dialog with enter key', () => {
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')

    cy.get('[data-cy=newSkillButton]').focus().realPress('Enter')
    cy.get('[data-cy="skillName"]').should('have.value', '')
    cy.get('[data-cy="skillNameError"]').should('have.value', '')
    cy.get('[data-cy=closeDialogBtn]').click()
    cy.get('[data-cy="skillName"]').should('not.exist')
  })

  it('Open edit skill dialog using enter key', function() {
    const expectedId = 'testSkill'
    const providedName = 'test'
    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill')
    cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists')
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.get('[data-cy=newSkillButton]').click()
    cy.get('#skillName').type(providedName)

    cy.getIdField().should('have.value', expectedId)
    cy.wait('@nameExists')

    cy.get('#skillName').type('{enter}')
    cy.wait('@postNewSkill')
    cy.wait('@loadSubject')

    cy.get('[data-cy=editSkillButton_testSkill]').focus()
    cy.realPress('Enter')

    cy.get('[data-cy="skillName"]').should('have.value', 'test')
    cy.get('[data-cy="skillNameError"]').should('have.value', '')
    cy.get('[data-cy=closeDialogBtn]').click()
    cy.get('[data-cy="manageSkillLink_testSkill"]')
  })

  it('Add Skill Event', () => {
    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: 'Skill 1',
      pointIncrement: '50',
      numPerformToCompletion: '5'
    })

    cy.intercept({
      method: 'POST',
      url: '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=TWO'
    }).as('suggestUsers')
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('loadSkill')
    cy.intercept({
      method: 'POST',
      url: '/api/projects/Inception/skills/ManuallyAddSkillEvent'
    }).as('addSkillEvent')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
    cy.wait('@loadSkill')
    cy.contains('Add Event').click()

    cy.contains('ONE').click()
    cy.contains('TWO').click()
    cy.get('[data-cy="userSuggestOptionsDropdown"]').contains('TWO')

    cy.get('[data-cy="userIdInput"]').click().type('foo{enter}')
    cy.wait('@suggestUsers')
    cy.get(addButtonSelector).click();
    cy.wait('@addSkillEvent')
    cy.get('[data-cy="addedUserEventsInfo"]', { timeout: 5 * 1000 }).contains('Added points for')
    cy.get('[data-cy="addedUserEventsInfo"]', { timeout: 5 * 1000 }).contains('[foo]')

    cy.get('[data-cy="userIdInput"]').click().type('{selectall}bar{enter}')
    cy.wait('@suggestUsers')
    cy.get(addButtonSelector).click();
    cy.wait('@addSkillEvent')
    cy.get('[data-cy="addedUserEventsInfo"]', { timeout: 5 * 1000 }).contains('Added points for')
    cy.get('[data-cy="addedUserEventsInfo"]', { timeout: 5 * 1000 }).contains('[bar]')

    cy.get('[data-cy="userIdInput"]').click().type('{selectall}baz{enter}')
    cy.wait('@suggestUsers')
    cy.get(addButtonSelector).click();
    cy.wait('@addSkillEvent')
    cy.get('[data-cy="addedUserEventsInfo"]', { timeout: 5 * 1000 }).contains('Added points for')
    cy.get('[data-cy="addedUserEventsInfo"]', { timeout: 5 * 1000 }).contains('[baz]')

    cy.get('[data-cy="userIdInput"]').click().type('{selectall}fo{enter}')
    cy.wait('@suggestUsers')
    cy.get('[data-cy="userIdInput"]').click()
    cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('foo').click({ force: true })
  })

  it('Cannot Add Skill Event if project does not have enough points', () => {
    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: 'Skill 1',
      pointIncrement: '10',
      numPerformToCompletion: '5'
    })

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('loadSkill')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
    cy.wait('@loadSkill')
    cy.get('[data-cy="nav-Add Event"]').click()

    cy.get('[data-cy="subPageHeader"]').contains('Add Skill Events')
    cy.get('[data-cy="skillId"]').contains('skill1')

    cy.get('[data-cy="addSkillEventButton"]').should('be.disabled');
    cy.get('[data-cy="addEventDisabledBlockUI"] > [data-pc-section="mask"]').should('exist');
    cy.get('[data-cy="addEventDisabledMsg"]').contains('Unable to add skill for user. Insufficient available points in project.');

    // increase the points and make sure the warning is gone
    cy.get('[data-cy="editSkillButton_skill1"]').click()
    cy.get('[data-cy="numPerformToCompletion"] [data-pc-name="pcinputtext"]').type('{selectall}10')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled').click()

    // decrease the points and make sure the warning returns
    cy.get('[data-cy="editSkillButton_skill1"]').click()
    cy.get('[data-cy="numPerformToCompletion"] [data-pc-name="pcinputtext"]').type('{selectall}5')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled').click()

    cy.get('[data-cy="addSkillEventButton"]').should('not.be.enabled');
    cy.get('[data-cy="addEventDisabledBlockUI"] > [data-pc-section="mask"]').should('exist');
    cy.get('[data-cy="addEventDisabledMsg"]').contains('Unable to add skill for user. Insufficient available points in project.');
  })

  it('Cannot Add Skill Event if subject does not have enough points', () => {
    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: 'Skill 1',
      pointIncrement: '10',
      numPerformToCompletion: '5'
    })

    cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
      projectId: 'proj1',
      subjectId: 'subj2',
      name: 'Subject 2'
    })

    cy.request('POST', '/admin/projects/proj1/subjects/subj2/skills/skill2', {
      projectId: 'proj1',
      subjectId: 'subj2',
      skillId: 'skill2',
      name: 'Skill 2',
      pointIncrement: '10',
      numPerformToCompletion: '5'
    })

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('loadSkill')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
    cy.wait('@loadSkill')
    cy.get('[data-cy="nav-Add Event"]').click()

    cy.get('[data-cy="subPageHeader"]').contains('Add Skill Events')
    cy.get('[data-cy="skillId"]').contains('skill1')

    cy.get('[data-cy="addSkillEventButton"]').should('be.disabled');
    cy.get('[data-cy="addEventDisabledBlockUI"] > [data-pc-section="mask"]').should('exist');
    cy.get('[data-cy="addEventDisabledMsg"]').contains('Unable to add skill for user. Insufficient available points in subject.');

    // increase the points and make sure the warning is gone
    cy.get('[data-cy="editSkillButton_skill1"]').click()
    cy.get('[data-cy="numPerformToCompletion"] [data-pc-name="pcinputtext"]').type('{selectall}10')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled').click()

    // decrease the points and make sure the warning returns
    cy.get('[data-cy="editSkillButton_skill1"]').click()
    cy.get('[data-cy="numPerformToCompletion"] [data-pc-name="pcinputtext"]').type('{selectall}5')
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled').click()

    cy.get('[data-cy="addSkillEventButton"]').should('not.be.enabled');
    cy.get('[data-cy="addEventDisabledBlockUI"] > [data-pc-section="mask"]').should('exist');
    cy.get('[data-cy="addEventDisabledMsg"]').contains('Unable to add skill for user. Insufficient available points in subject.');
  })

  it('Add Skill Event for days in past correctly does not subtract one day from selected date', () => {
    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: 'Skill 1',
      pointIncrement: '50',
      numPerformToCompletion: '5'
    })

    cy.intercept({
      method: 'POST',
      url: '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=TWO'
    }).as('suggestUsers')
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('loadSkill')
    cy.intercept({
      method: 'POST',
      url: '/api/projects/Inception/skills/ManuallyAddSkillEvent'
    }).as('addSkillEvent')
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/skills/skill1/users**'
    }).as('loadUsers')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
    cy.wait('@loadSkill')
    cy.contains('Add Event').click()

    cy.contains('ONE').click()
    cy.get('[data-cy="userIdInput"]').click().type('foo{enter}')
    // cy.wait('@suggestUsers');

    const date = dayjs().subtract(1, 'month').date(10)
    const formattedStr = dayjs(date).format('YYYY-MM-DD')
    //need to format year/month/day to match users screen

    cy.get('[data-cy="eventDatePicker"]').click()
    cy.get('[data-pc-name="pcprevbutton"]').first().click()
    cy.get('[data-pc-section="day"]').contains('10').click()

    cy.get(addButtonSelector).click();
    cy.wait('@addSkillEvent')
    cy.get('[data-cy="addedUserEventsInfo"]', { timeout: 5 * 1000 }).contains('Added points for')
    cy.get('[data-cy="addedUserEventsInfo"]', { timeout: 5 * 1000 }).contains('[foo]')
    cy.get('[data-cy=nav-Users]').click()
    cy.validateTable('[data-cy=usersTable]', [
      [{ colIndex: 0,  value: 'foo' }, { colIndex: 3,  value: formattedStr }],
    ], 5);
  })

  it('Add Skill Event - suggest user with slash character does not cause error', () => {
    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: 'Skill 1',
      pointIncrement: '50',
      numPerformToCompletion: '5'
    })

    cy.intercept({
      method: 'POST',
      url: '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=TWO'
    }).as('suggestUsers')
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('loadSkill')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
    cy.wait('@loadSkill')
    cy.contains('Add Event').click()

    cy.contains('ONE').click()
    cy.contains('TWO').click()
    cy.get('[data-cy="userSuggestOptionsDropdown"]').contains('TWO')

    cy.get('[data-cy="userIdInput"]').click().type('foo/bar{enter}')
    cy.wait('@suggestUsers')
  })

  it('Add Skill Event User Not Found', () => {
    cy.intercept({
      method: 'PUT',
      path: '/api/projects/*/skills/*'
    }, {
      statusCode: 400,
      body: { errorCode: 'UserNotFound', explanation: 'Some Error Occurred' }
    }).as('addUser')

    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: 'Skill 1',
      pointIncrement: '50',
      numPerformToCompletion: '5'
    })

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('loadSkill')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
    cy.wait('@loadSkill')


    cy.contains('Add Event').click()

    cy.get('[data-cy="userIdInput"]').click().type('foo{enter}')

    cy.get(addButtonSelector).click();
    cy.wait('@addUser')
    cy.get('[data-cy="addedUserEventsInfo"]').contains('Unable to add points for')
  })

  it('Add Skill Event - user names cannot have spaces', () => {
    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: 'Skill 1',
      pointIncrement: '50',
      numPerformToCompletion: '5'
    })

    cy.intercept('/admin/projects/proj1/subjects/subj1/skills/skill1').as('loadSkill')
    cy.intercept('/admin/projects/proj1').as('loadProject')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/addSkillEvent')
    cy.wait('@loadSkill')
    cy.wait('@loadProject')
    cy.get('[data-cy="subPageHeader"]').contains('Add Skill Events')
    cy.get('[data-cy="skillId"]').contains('skill1')

    const expectedErrMsg = 'User Id may not contain spaces'
    const userIdSelector = '[data-cy=userIdInput]'
    const addButtonSelector = '[data-cy=addSkillEventButton]'

    cy.get(userIdSelector).type('user a{enter}')
    cy.contains(expectedErrMsg)
    cy.get(addButtonSelector).should('be.disabled')

    cy.get(userIdSelector).type('{selectall}userd{enter}')
    cy.contains(expectedErrMsg).should('not.exist')
    cy.get(addButtonSelector).should('not.be.disabled')

    cy.get(userIdSelector).type('{selectall}user d{enter}')
    cy.contains(expectedErrMsg)
    cy.get(addButtonSelector).should('be.disabled')

    cy.get(userIdSelector).type('{selectall}userOK{enter}')
    cy.contains(expectedErrMsg).should('not.exist')
    cy.get(addButtonSelector).should('not.be.disabled')
    cy.get(addButtonSelector).click()
    cy.contains('userOK')

    cy.get(userIdSelector).type('{selectall}user@#$&*{enter}')
    cy.contains(expectedErrMsg).should('not.exist')
    cy.get(addButtonSelector).should('not.be.disabled')
    cy.get(addButtonSelector).click()
    cy.contains('user@#$&*')
  })

  it('create skill and then update skillId', () => {
    const initialId = 'myid1Skill'
    const newId = 'MyId1Skill'
    const providedName = 'my id 1'

    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${initialId}`).as('postNewSkill')
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

    cy.getIdField().should('have.value', initialId)
    cy.wait('@nameExists')
    cy.wait('@skillIdExists')

    cy.clickSave()
    cy.wait('@postNewSkill')

    const editButtonSelector = `[data-cy=editSkillButton_${initialId}]`
    cy.get(editButtonSelector).click()

    cy.get('[data-cy="enableIdInput"]').click()
    cy.getIdField().clear().type(newId)
    cy.wait('@skillIdExists')

    cy.clickSaveDialogBtn()
    cy.wait('@postNewSkill')
  })

  it('new skill button should retain focus after dialog closes', () => {
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')

    cy.openNewSkillDialog()
    cy.get('[data-cy=closeDialogBtn]').click()
    cy.get('[data-cy="newSkillButton"]').should('have.focus')

    cy.openNewSkillDialog()
    cy.get('[data-cy=skillName]').type('{esc}')
    cy.get('[data-cy="newSkillButton"]').should('have.focus')

    cy.openNewSkillDialog()
    cy.get('[aria-label=Close]').click()
    cy.get('[data-cy="newSkillButton"]').should('have.focus')

    cy.openNewSkillDialog()
    cy.get('[data-cy=skillName]').type('foobarbaz')
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.get('[data-cy="newSkillButton"]').should('have.focus')
  })

  it('focus should be returned to skill edit button', () => {
    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: 'Skill 1',
      pointIncrement: '50',
      numPerformToCompletion: '5'
    })

    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill2', {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill2',
      name: 'Skill 2',
      pointIncrement: '50',
      numPerformToCompletion: '5'
    })

    cy.intercept({
      method: 'POST',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('saveSkill')
    cy.intercept({
      method: 'POST',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill2'
    }).as('saveSkill2')

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('loadSkill')
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill2'
    }).as('loadSkill2')
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    //skill 2
    cy.get('[data-cy=editSkillButton_skill2]').click()
    cy.get('[data-cy=skillName]').should('be.visible')
    cy.get('[data-cy=skillName]').type('{esc}')
    cy.get('[data-cy=editSkillButton_skill2]').first().should('have.focus')

    cy.get('[data-cy=editSkillButton_skill2]').click()
    cy.get('[data-cy=closeDialogBtn]').click()
    cy.get('[data-cy=editSkillButton_skill2]').should('have.focus')

    cy.get('[data-cy=editSkillButton_skill2]').click()
    cy.get('[data-cy=skillName]').type('test 123')
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@saveSkill2')
    cy.wait('@loadSkill2')
    cy.get('[data-cy=editSkillButton_skill2]').should('have.focus')

    cy.get('[data-cy=editSkillButton_skill2]').click()
    cy.get('[data-pc-name="dialog"] [data-pc-name="pcclosebutton"]').click()
    cy.get('[data-cy=editSkillButton_skill2]').should('have.focus')
    cy.contains('Skill 2test 123')

    //skill 1
    cy.get('[data-cy=editSkillButton_skill1]').click()
    cy.get('[data-cy=skillName]').should('be.visible')
    cy.get('[data-cy=skillName]').type('{esc}')
    cy.get('[data-cy=editSkillButton_skill1]').should('have.focus')

    cy.get('[data-cy=editSkillButton_skill1]').click()
    cy.get('[data-cy=closeDialogBtn]').click()
    cy.get('[data-cy=editSkillButton_skill1]').should('have.focus')

    cy.get('[data-cy=editSkillButton_skill1]').click()
    cy.get('[data-cy=skillName]').type('test 123')
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@saveSkill')
    cy.wait('@loadSkill')
    cy.get('[data-cy=editSkillButton_skill1]').should('have.focus')

    cy.get('[data-cy=editSkillButton_skill1]').click()
    cy.get('[data-pc-name="dialog"] [data-pc-name="pcclosebutton"]').click()
    cy.get('[data-cy=editSkillButton_skill1]').should('have.focus')
  })


  it('description is validated against custom validators', () => {
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.openNewSkillDialog()

    cy.get('[data-cy="skillName"]').type('Great Name')
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

    cy.get('[data-cy="markdownEditorInput"]').type('ldkj aljdl aj\n\njabberwocky')
    cy.get('[data-cy="descriptionError"]').contains('Skill Description - paragraphs may not contain jabberwocky')
    cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

    cy.get('[data-cy="markdownEditorInput"]').type('{backspace}')
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
  })

  it('name is validated against custom validators', () => {
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.openNewSkillDialog()

    cy.get('[data-cy="skillName"]').type('Great Name')
    cy.get('[data-cy="skillNameError"]')
      .should('not.be.visible')
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

    cy.get('[data-cy="skillName"]')
      .type('{selectall}(A) Updated Skill Name')
    cy.get('[data-cy="skillNameError"]').contains('Skill Name - names may not contain (A)')
    cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

    cy.get('[data-cy="skillName"]')
      .type('{selectall}(B) A Updated Skill Name')
    cy.get('[data-cy="skillNameError"]')
      .should('not.be.visible')
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
  })

  it('skill id must only contain alpha number characters or underscore', () => {
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1')
      .as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.openNewSkillDialog()

    cy.get('[data-cy="skillName"]').type('Great Name 1 2 33')
    cy.get('[data-cy=enableIdInput]').click();

    const errMsg = 'Skill ID may only contain alpha-numeric, underscore or percent characters'

    const invalidChars = '$!#$^&*()+=-`~'
    for (let i = 0; i < invalidChars.length; i++) {
      const charToCheck = invalidChars.charAt(i)
      cy.get('[data-cy="idError"]').contains(errMsg).should('not.exist')
      cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

      cy.get('[data-cy="idInputValue"]').type(charToCheck)
      cy.get('[data-cy="idError"]').contains(errMsg)
      cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')

      cy.get('[data-cy="idInputValue"]').type('{backspace}')
    }

    cy.get('[data-cy="idError"]').contains(errMsg).should('not.exist')
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
    cy.get('[data-cy="idInputValue"]').type('_blah')
    cy.get('[data-cy="idError"]').contains(errMsg).should('not.exist')
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')

    cy.clickSaveDialogBtn()
  })

  it('skill id validation special characters can be URL encoded', () => {

    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1')
      .as('loadSubject')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.get('[data-cy=newSkillButton]').click()

    cy.get('[data-cy="skillName"]').type('Great Name 1 2 33')

    const errMsg = 'Skill ID may only contain alpha-numeric, underscore or percent characters'

    // id cannot contain special chars
    const providedName = '@#$^&i_l+|}{/\\'
    cy.get('[data-cy=enableIdInput]').click()
    cy.getIdField().clear().type(providedName)
    cy.get('[data-cy="idError"]').contains(errMsg).should('be.visible')
    cy.get('[data-cy="saveDialogBtn"]').should('not.be.enabled')

    // id can URL encode special chars
    cy.get('[data-cy="idInputValue"]').clear().type(`${encodeURIComponent(providedName)}_blah`)
    cy.get('[data-cy="idError"]').contains(errMsg).should('not.exist')
    cy.get('[data-cy=idError]').should('not.be.visible')

    cy.clickSaveDialogBtn()
  })

  it('edit skill on page', () => {
    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'skill1',
      name: `This is 1`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 5,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: 1,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
      version: 1,
      helpUrl: 'http://doHelpOnThisSkill.com'
    })
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('loadSkill1')
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('saveSkill1')
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/entirelyNewId').as('afterIdEdit')
    cy.intercept('POST', '/api/validation/description*').as('validateDescription')
    cy.intercept('POST', '/api/validation/url').as('validateUrl')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
    cy.wait('@loadSkill1')

    cy.contains('SKILL: This is 1').should('be.visible')
    cy.get('[data-cy=childRowDisplay_skill1]').should('be.visible')
    // skill should now only be loaded once on page load instead of twice, once by SkillPage and another time by SkillOverview
    cy.get('@loadSkill1.all').should('have.length', 1)
    cy.get('[data-cy=editSkillButton_skill1]').click()
    cy.get('[data-cy=skillName]').type('{selectall}Edited Skill Name')
    cy.get('[data-cy=pointIncrement]').click()
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@saveSkill1')
    cy.get('[data-cy=editSkillButton_skill1]').should('have.focus')
    cy.contains('SKILL: Edited Skill Name').should('be.visible')
    cy.contains('SKILL: This is 1').should('not.exist')

    cy.get('[data-cy=breadcrumb-skill1]').should('be.visible')
    cy.get('[data-cy=editSkillButton_skill1]').click()
    cy.get('[data-cy=enableIdInput]').click()
    cy.get('[data-cy=idInputValue]').type('{selectall}entirelyNewId')
    cy.get('[data-cy=pointIncrement]').click()
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@validateDescription')
    cy.wait('@validateUrl')
    cy.wait('@afterIdEdit')
    cy.get('[data-cy=editSkillButton_entirelyNewId]').should('have.focus')
    cy.contains('ID: entirelyNewId').should('be.visible')
    cy.get('[data-cy=breadcrumb-skill1]').should('not.exist')
    cy.get('[data-cy=breadcrumb-entirelyNewId]').should('be.visible')
    cy.get('[data-cy=editSkillButton_entirelyNewId]').should('be.visible')
    cy.get('[data-cy=editSkillButton_skill1]').should('not.exist')

    //edit version, point increment, occurrences, time window, description, helpurl and confirm that the updates are reflected in the overview section
    cy.get('[data-cy=editSkillButton_entirelyNewId]').click()
    cy.get('[data-cy=pointIncrement]').type('{selectall}20')
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@validateDescription')
    cy.wait('@validateUrl')
    cy.wait('@afterIdEdit')
    cy.get('[data-cy=skillOverviewTotalpoints]').contains('100 Points').should('be.visible')
    cy.get('[data-cy=skillOverviewTotalpoints]').contains('20 points').should('be.visible')
    cy.get('[data-cy=skillOverviewTotalpoints]').contains('5 repetitions to Completion').should('be.visible')
    cy.get('[data-cy=editSkillButton_entirelyNewId]').click()
    cy.get('[data-cy=numPerformToCompletion]').type('{selectall}10')
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@validateDescription')
    cy.wait('@validateUrl')
    cy.wait('@afterIdEdit')
    cy.get('[data-cy=childRowDisplay_entirelyNewId]').contains('200 Points').should('be.visible')
    cy.get('[data-cy=skillOverviewTotalpoints]').contains('20 points').should('be.visible')
    cy.get('[data-cy=skillOverviewTotalpoints]').contains('10 repetitions to Completion').should('be.visible')
    cy.get('[data-cy=editSkillButton_entirelyNewId]').click()
    cy.get('[data-cy="timeWindowInput"] [data-pc-section="togglebutton"]').click()
    cy.get('[data-cy=timeWindowCheckbox').click()
    cy.get('[data-cy=pointIncrementIntervalMins]').type('{selectall}59')
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@afterIdEdit')
    cy.contains('8 Hours 59 Minutes').should('be.visible')
    cy.get('[data-cy=editSkillButton_entirelyNewId]').click()
    cy.get('[data-cy=selfReportEnableCheckbox]').click()
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@validateDescription')
    cy.wait('@validateUrl')
    cy.wait('@afterIdEdit')
    cy.contains('Self Report: Approval').should('be.visible')
    cy.get('[data-cy=editSkillButton_entirelyNewId]').click()
    cy.get('[data-cy=markdownEditorInput]').type('{selectall}LOREM')
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@validateDescription')
    cy.wait('@validateUrl')
    cy.wait('@afterIdEdit')
    cy.contains('Editing Existing Skill').should('not.exist')
    cy.get('[data-cy="skillOverviewDescription"').contains('LOREM')
    cy.get('[data-cy=editSkillButton_entirelyNewId]').click()
    cy.get('[data-cy=skillHelpUrl]').type('{selectall}http://fake/fake/fake.fake')
    cy.get('[data-cy=saveDialogBtn]').click()
    cy.wait('@validateDescription')
    cy.wait('@validateUrl')
    cy.wait('@afterIdEdit')
    cy.contains('http://fake/fake/fake.fake').should('be.visible')
  })

  it('skill help url with %20 in path retains %20 on edit', () => {
    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/dummy`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'dummy',
      name: `dummy`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 5,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: 1,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
      version: 1,
      helpUrl: 'http://doHelpOnThisSkill.com/i%20have%20spaces'
    })
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/dummy').as('loadSkill')

    // resolutions over 1280 are ignored in headless mode so we can only test at this resolution
    cy.setResolution([1280, 900])
    cy.wait(200)
    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/dummy/')
    cy.wait('@loadSkill')

    cy.get('[data-cy=editSkillButton_dummy]').click()
    cy.wait('@loadSkill')
    cy.get('[data-cy=skillHelpUrl]').should('have.value', 'http://doHelpOnThisSkill.com/i%20have%20spaces')

  })

  it('skill modal allows Help Url to have spaces', () => {
    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.openNewSkillDialog()
    cy.get('[data-cy="skillName"]').type('skill1')
    cy.get('[data-cy="skillHelpUrl"]').type('https://someCoolWebsite.com/some url with spaces')
    cy.get('[data-cy="skillHelpUrlError"]').should('not.be.visible')
    cy.clickSaveDialogBtn()
    cy.get('[data-cy="editSkillButton_skill1Skill"]').click()
    cy.get('[data-cy="skillHelpUrl"]').should('have.value', 'https://someCoolWebsite.com/some%20url%20with%20spaces')
    cy.get('[data-cy="closeDialogBtn"]').click()
    cy.get('[data-p-index="0"] [data-pc-section="rowtogglebutton"]').click()
    cy.get('[data-cy="childRowDisplay_skill1Skill"] [data-cy="skillOverviewHelpUrl"]').contains('https://someCoolWebsite.com/some%20url%20with%20spaces')
  })

  it('append "Root Help URL" to the "Help Url" if configured', () => {
    cy.request('POST', '/admin/projects/proj1/settings/help.url.root', {
      projectId: 'proj1',
      setting: 'help.url.root',
      value: 'https://SomeArticleRepo.com/'
    })
    cy.createSkill(1, 1, 1, { helpUrl: '/some/path' })
    cy.createSkill(1, 1, 2, { helpUrl: 'https://www.OverrideHelpUrl.com/other/path' })

    const runHelpUrlValidation = () => {
      cy.visit('/administrator/projects/proj1/subjects/subj1')
      cy.get('[data-p-index="1"] [data-pc-section="rowtogglebutton"]').click()
      cy.get('[data-cy="childRowDisplay_skill1"] [data-cy="skillOverviewHelpUrl"]').should('have.attr', 'href', 'https://SomeArticleRepo.com/some/path')
      cy.get('[data-cy="childRowDisplay_skill1"] [data-cy="skillOverviewHelpUrl"]').contains('https://SomeArticleRepo.com/some/path')

      cy.get('[data-p-index="0"] [data-pc-section="rowtogglebutton"]').click()
      cy.get('[data-cy="childRowDisplay_skill2"] [data-cy="skillOverviewHelpUrl"]').should('have.attr', 'href', 'https://www.OverrideHelpUrl.com/other/path')
      cy.get('[data-cy="childRowDisplay_skill2"] [data-cy="skillOverviewHelpUrl"]').contains('https://www.OverrideHelpUrl.com/other/path')

      // navigate to each skill and validate help url
      cy.get('[data-cy="manageSkillLink_skill1"]').click()
      cy.get('[data-cy="skillOverviewHelpUrl"]').should('have.attr', 'href', 'https://SomeArticleRepo.com/some/path')
      cy.get('[data-cy="skillOverviewHelpUrl"]').contains('https://SomeArticleRepo.com/some/path')
      // refresh and re-validate
      cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
      cy.get('[data-cy="skillOverviewHelpUrl"]').should('have.attr', 'href', 'https://SomeArticleRepo.com/some/path')
      cy.get('[data-cy="skillOverviewHelpUrl"]').contains('https://SomeArticleRepo.com/some/path')

      // now let's do the same for the 2nd skill
      cy.visit('/administrator/projects/proj1/subjects/subj1')
      cy.get('[data-cy="manageSkillLink_skill2"]').click()
      cy.get('[data-cy="skillOverviewHelpUrl"]').should('have.attr', 'href', 'https://www.OverrideHelpUrl.com/other/path')
      cy.get('[data-cy="skillOverviewHelpUrl"]').contains('https://www.OverrideHelpUrl.com/other/path')
      cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2')
      cy.get('[data-cy="skillOverviewHelpUrl"]').should('have.attr', 'href', 'https://www.OverrideHelpUrl.com/other/path')
      cy.get('[data-cy="skillOverviewHelpUrl"]').contains('https://www.OverrideHelpUrl.com/other/path')
    }
    runHelpUrlValidation()

    // run same tests but root help url value does NOT end with '/'
    cy.request('POST', '/admin/projects/proj1/settings/help.url.root', {
      projectId: 'proj1',
      setting: 'help.url.root',
      value: 'https://SomeArticleRepo.com'
    })
    runHelpUrlValidation()
  })

  it('skill modal shows Root Help Url when configured', () => {
    cy.request('POST', '/admin/projects/proj1/settings/help.url.root', {
      projectId: 'proj1',
      setting: 'help.url.root',
      value: 'https://SomeArticleRepo.com/'
    })
    cy.createSkill(1, 1, 1, { helpUrl: '/some/path' })
    cy.createSkill(1, 1, 2, { helpUrl: 'https://www.OverrideHelpUrl.com/other/path' })

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.openNewSkillDialog()
    cy.get('[data-cy="rootHelpUrlSetting"]').contains('https://SomeArticleRepo.com')

    const textDecorationMatch = 'line-through solid color(srgb 0.0862745 0.396078 0.203922)'

    // strike-through when url starts with http:// or https://
    cy.get('[data-cy="skillHelpUrl"]').type('https:/')
    cy.get('[data-cy="rootHelpUrlSetting"]').should('not.have.css', 'text-decoration', textDecorationMatch)
    cy.get('[data-cy="skillHelpUrl"]').type('/')
    cy.get('[data-cy="rootHelpUrlSetting"]').should('have.css', 'text-decoration', textDecorationMatch)

    cy.get('[data-cy="skillHelpUrl"]').clear().type('http:/')
    cy.get('[data-cy="rootHelpUrlSetting"]').should('not.have.css', 'text-decoration', textDecorationMatch)
    cy.get('[data-cy="skillHelpUrl"]').type('/')
    cy.get('[data-cy="rootHelpUrlSetting"]').should('have.css', 'text-decoration', textDecorationMatch)

    // now test edit
    cy.get('[data-cy="closeDialogBtn"]').click()
    cy.get('[data-cy="editSkillButton_skill1"]').click()
    cy.get('[data-cy="rootHelpUrlSetting"]').contains('https://SomeArticleRepo.com')
    cy.get('[data-cy="rootHelpUrlSetting"]').should('not.have.css', 'text-decoration', textDecorationMatch)

    // edit again - anything that starts with https or http must not use Root Help Url
    cy.get('[data-cy="closeDialogBtn"]').click()
    cy.get('[data-cy="editSkillButton_skill2"]').click()
    cy.get('[data-cy="rootHelpUrlSetting"]').contains('https://SomeArticleRepo.com')
    cy.get('[data-cy="rootHelpUrlSetting"]').should('have.css', 'text-decoration', textDecorationMatch)

    // do not show Root Help Url if it's not configured
    cy.request('POST', '/admin/projects/proj1/settings/help.url.root', {
      projectId: 'proj1',
      setting: 'help.url.root',
      value: ''
    })
    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.openNewSkillDialog()
    cy.get('[data-cy="skillHelpUrl"]')
    cy.get('[data-cy="rootHelpUrlSetting"]').should('not.exist')
    cy.get('[data-cy="closeDialogBtn"]').click()
    cy.get('[data-cy="editSkillButton_skill1"]').click()
    cy.get('[data-cy="skillHelpUrl"]')
    cy.get('[data-cy="rootHelpUrlSetting"]').should('not.exist')
  })

  it('skill help url with %20 in host retains %20 on edit', () => {
    cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/dummy`, {
      projectId: 'proj1',
      subjectId: 'subj1',
      skillId: 'dummy',
      name: `dummy`,
      type: 'Skill',
      pointIncrement: 100,
      numPerformToCompletion: 5,
      pointIncrementInterval: 0,
      numMaxOccurrencesIncrementInterval: 1,
      description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.',
      version: 1,
      helpUrl: 'http://doHelp%20On%20This%20Skill.com/i%20have%20spaces'
    })
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills/dummy').as('loadSkill')

    // resolutions over 1280 are ignored in headless mode so we can only test at this resolution
    cy.setResolution([1280, 900])
    cy.wait(200)
    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/dummy/')
    cy.wait('@loadSkill')

    cy.get('[data-cy=editSkillButton_dummy]').click()
    cy.wait('@loadSkill')
    cy.get('[data-cy=skillHelpUrl]').should('have.value', 'http://doHelp%20On%20This%20Skill.com/i%20have%20spaces')

  })

  it('load page with apex charts directly and repeatedly', () => {
    // apex charts and dynamic imports had a race condition, this test verifies that charts load successfully

    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4)
    cy.createSkill(1, 1, 5)

    const m = moment.utc().subtract(10, 'days')
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: 'someuser1',
      timestamp: m.clone().add(1, 'day').format('x')
    })
    cy.request('POST', `/api/projects/proj1/skills/skill1`, {
      userId: 'someuser1',
      timestamp: m.clone().add(2, 'day').format('x')
    })
    cy.request('POST', `/api/projects/proj1/skills/skill1`, { userId: 'someuser2', timestamp: new Date().getTime() })
    cy.request('POST', `/api/projects/proj1/skills/skill1`, { userId: 'someuser3', timestamp: new Date().getTime() })
    cy.request('POST', `/api/projects/proj1/skills/skill1`, { userId: 'someuser4', timestamp: new Date().getTime() })

    cy.intercept('/admin/projects/proj1/metrics/numUserAchievedOverTimeChartBuilder?skillId=skill1').as('numUserAchievedOverTimeChartBuilder')
    cy.intercept('/admin/projects/proj1/metrics/binnedUsagePostAchievementMetricsBuilder?skillId=skill1').as('binnedUsagePostAchievementMetricsBuilder')
    cy.intercept('/admin/projects/proj1/metrics/usagePostAchievementMetricsBuilder?skillId=skill1').as('usagePostAchievementMetricsBuilder')
    cy.intercept('/admin/projects/proj1/metrics/skillAchievementsByTagBuilder?skillId=skill1&userTagKey=adminOrganization').as('skillAchievementsByTagBuilder')
    cy.intercept('/admin/projects/proj1/metrics/skillAchievementsByTagBuilder?skillId=skill1&userTagKey=dutyOrganization').as('skillAchievementsByTagBuilder2')
    cy.intercept('/admin/projects/proj1/metrics/skillEventsOverTimeChartBuilder**').as('skillEventsOverTimeChartBuilder')
    cy.intercept('/admin/projects/proj1/metrics/usagePostAchievementUsersBuilder**').as('usagePostAchievementUsersBuilder')
    const numTries = 5
    for (let i = 1; i <= numTries; i += 1) {
      cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/metrics')
      cy.wait('@binnedUsagePostAchievementMetricsBuilder')
      cy.wait('@numUserAchievedOverTimeChartBuilder')
      cy.wait('@usagePostAchievementMetricsBuilder')
      cy.wait('@skillAchievementsByTagBuilder')
      cy.wait('@skillAchievementsByTagBuilder2')
      cy.wait('@skillEventsOverTimeChartBuilder')
      cy.wait('@usagePostAchievementUsersBuilder')
      cy.wait(2000)
      cy.contains('# Users')
      cy.contains('stopped after achieving')
    }
  })

  it('search for skills across subjects', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4)
    cy.createSkill(1, 1, 5)

    cy.createSubject(1, 2)
    cy.createSkill(1, 2, 6)
    cy.createSkill(1, 2, 7)
    cy.createSkill(1, 2, 8)

    cy.createSubject(1, 3)
    cy.createSkill(1, 3, 9)
    cy.createSkill(1, 3, 10)


    cy.visit('/administrator/projects/proj1/')
    cy.get('[data-cy="skillsSelector"] input')
      .invoke('attr', 'placeholder')
      .should('contain', 'Search and Navigate directly to a skill');
    cy.get('[data-cy="skillsSelector"]').click()
    cy.get('li.p-autocomplete-empty-message').contains('Type to search for skills').should('be.visible')
    cy.get(`[data-cy="skillsSelector"]`).type('sUbJ2')

    cy.get('[data-cy="skillsSelector-skillId"]').should('have.length', 3).as('skillIds')
    cy.get('@skillIds').eq(0).contains('skill6Subj2')
    cy.get('@skillIds').eq(1).contains('skill7Subj2')
    cy.get('@skillIds').eq(2).contains('skill8Subj2')

    cy.get('[data-cy="skillsSelector-skillName"]').should('have.length', 3).as('names')
    cy.get('@names').eq(0).contains('Very Great Skill 6 Subj2')
    cy.get('@names').eq(1).contains('Very Great Skill 7 Subj2')
    cy.get('@names').eq(2).contains('Very Great Skill 8 Subj2')

    cy.get('[data-cy="skillsSelector-subjectName"]').should('have.length', 3).as('subjects')
    cy.get('@subjects').eq(0).contains('Subject 2')
    cy.get('@subjects').eq(1).contains('Subject 2')
    cy.get('@subjects').eq(2).contains('Subject 2')

    // navigate to a skill
    cy.get('@skillIds').eq(1).click()
    cy.get('[data-cy="pageHeader"]').contains('ID: skill7Subj2')

    // search produces no results
    cy.visit('/administrator/projects/proj1/')
    // cy.get('[data-cy="skillsSelector"]').contains('Search and Navigate directly to a skill').should('be.visible')
    cy.get('[data-cy="skillsSelector"]').type('sUbJ2*kd')
    cy.get('li.p-autocomplete-empty-message').contains('No results found').should('be.visible')

    // special chars don't break anything
    cy.get(`[data-cy="skillsSelector"]`).type('!@#$%^&*()')
    cy.get('li.p-autocomplete-empty-message').contains('No results found').should('be.visible')
  })

  it('add skill and copy skill buttons disabled if max skills for subject reached', () => {
    cy.intercept('/public/config', {
      body: {
        artifactBuildTimestamp: '2022-01-17T14:39:38Z',
        authMode: 'FORM',
        buildTimestamp: '2022-01-17T14:39:38Z',
        dashboardVersion: '1.9.0-SNAPSHOT',
        defaultLandingPage: 'progress',
        descriptionMaxLength: '2000',
        docsHost: 'https://code.nsa.gov/skills-docs',
        expirationGracePeriod: 7,
        expireUnusedProjectsOlderThan: 180,
        maxBadgeNameLength: '50',
        maxBadgesPerProject: '25',
        maxDailyUserEvents: '30',
        maxFirstNameLength: '30',
        maxIdLength: '50',
        maxLastNameLength: '30',
        maxLevelNameLength: '50',
        maxNicknameLength: '70',
        maxNumPerformToCompletion: '10000',
        maxNumPointIncrementMaxOccurrences: '999',
        maxPasswordLength: '40',
        maxPointIncrement: '10000',
        maxProjectNameLength: '50',
        maxProjectsPerAdmin: '25',
        maxSelfReportMessageLength: '250',
        maxSelfReportRejectionMessageLength: '250',
        maxSkillNameLength: '100',
        maxSkillVersion: '999',
        maxSkillsPerSubject: '5',
        maxSubjectNameLength: '50',
        maxSubjectsPerProject: '25',
        maxTimeWindowInMinutes: '43200',
        maxBadgeBonusInMinutes: '525600',
        minIdLength: '3',
        minNameLength: '3',
        minPasswordLength: '8',
        minUsernameLength: '5',
        minimumProjectPoints: '100',
        minimumSubjectPoints: '100',
        nameValidationMessage: '',
        nameValidationRegex: '',
        needToBootstrap: false,
        numProjectsForTableView: '10',
        oAuthOnly: false,
        paragraphValidationMessage: 'paragraphs may not contain jabberwocky',
        paragraphValidationRegex: '^(?i)(?s)((?!jabberwocky).)*$',
        pointHistoryInDays: '1825',
        projectMetricsTagCharts: '[{"key":"dutyOrganization","type":"pie","title":"Users by Org"},{"key":"adminOrganization","type":"bar","title":"Users by Agency"}]',
        rankingAndProgressViewsEnabled: 'true',
        userSuggestOptions: 'ONE,TWO,THREE',
        verifyEmailAddresses: false
      }
    })

    cy.intercept('DELETE', '/admin/projects/proj1/subjects/subj1/skills/skill1').as('deleteSkill')

    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4)
    cy.createSkill(1, 1, 5)

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get('[data-cy=newGroupButton]').should('be.disabled')
    cy.get('[data-cy=newSkillButton]').should('be.disabled')
    cy.get('[data-cy=addSkillDisabledWarning]').contains('The maximum number of Skills allowed is 5')

    cy.get('[data-cy*=copySkillButton]').should('have.length', 5)
    cy.get('[data-cy*=copySkillButton]').should('be.disabled')

    cy.openDialog('[data-cy=deleteSkillButton_skill1]')
    cy.get('[data-cy=currentValidationText]').type('Delete Me', {delay: 0})
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled').click()
    cy.wait('@deleteSkill')

    cy.get('[data-cy=newGroupButton]').should('be.enabled')
    cy.get('[data-cy=newSkillButton]').should('be.enabled')
    cy.get('[data-cy=addSkillDisabledWarning]').should('not.exist')

    cy.get('[data-cy*=copySkillButton]').should('have.length', 4)
    cy.get('[data-cy*=copySkillButton]').should('be.enabled')
  })

  it('deleting a skill causes subject skill count to be updated', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createSkill(1, 1, 3)
    cy.createSkill(1, 1, 4)
    cy.createSkill(1, 1, 5)

    cy.intercept('DELETE', '/admin/projects/proj1/subjects/subj1/skills/skill5').as('deleteSkill')

    cy.visit('/administrator/projects/proj1/subjects/subj1')

    cy.get('[data-cy=pageHeaderStat]').eq(1).should('contain.text', '5')
    cy.openDialog('[data-cy=deleteSkillButton_skill5]')
    cy.get('[data-cy=currentValidationText]').type('Delete Me', {delay: 0})
    cy.get('[data-cy=saveDialogBtn]').should('be.enabled').click()
    cy.wait('@deleteSkill')
    cy.get('[data-cy=pageHeaderStat]').eq(1).should('contain.text', '4')
  })

  it('edit skill with version greater than 1', () => {
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2, { version: 1 })
    cy.createSkill(1, 1, 3, { version: 2 })

    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills').as('loadSkills')
    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSkills')
    cy.get('[data-cy="editSkillButton_skill3"]').should('be.visible')
    cy.get('[data-cy="editSkillButton_skill3"]').click()
    cy.get('[data-cy="markdownEditorInput"').type('AABBCCDDEEFFGG')
    cy.wait(500) //wait for validation debounce
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
  })

  it('edit skill - run validation on load in case validation improved and existing values fail to validate', () => {
    cy.intercept('POST', '/api/validation/description*', {
      valid: false,
      msg: 'Mocked up validation failure'
    }).as('validateDesc')

    cy.createSkill(1, 1, 1, { description: 'Very cool project' })
    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.get('[data-cy="editSkillButton_skill1"]').click()
    cy.wait('@validateDesc')
    cy.get('[data-cy="descriptionError"]').contains('Mocked up validation failure')
  })

  it('copy skill - run validation on load in case validation improved and existing values fail to validate', () => {
    cy.intercept('POST', '/api/validation/description*', {
      valid: false,
      msg: 'Mocked up validation failure'
    }).as('validateDesc')

    cy.createSkill(1, 1, 1, { description: 'Very cool project' })
    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.get('[data-cy="copySkillButton_skill1"]').click()
    cy.wait('@validateDesc')
    cy.get('[data-cy="descriptionError"]').contains('Mocked up validation failure')
  })


})
