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

describe('Disabled Subject Tests', () => {

  const tableSelector = '[data-cy="skillsTable"]'
  
  it('create initially disabled subject', () => {
    cy.createProject(1);

    const expectedId = 'InitiallyDisabledSubjectSubject'
    const providedName = 'Initially Disabled Subject'

    cy.intercept('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');
    cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
    cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

    cy.visit('/administrator/projects/proj1');
    cy.wait('@loadSubjects');
    cy.get('[data-cy="btn_Subjects"]').click();

    cy.get('[data-cy="subjectName"]').type(providedName);
    cy.wait('@nameExists');

    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('be.checked')
    cy.get('[data-cy="visibilitySwitch"]').click()
    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('not.be.checked')
    cy.clickSave()
    cy.wait('@postNewSubject')

    cy.get(`[data-cy="subjectCard-${expectedId}"] [data-cy="disabledSubjectBadge"]`).should('be.visible')
  })

  it('enable a disabled subject on the project page', () => {
    cy.createProject(1);
    cy.createSubject(1, 1, { enabled: false })

    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1').as('updateSubject');
    cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');

    cy.visit('/administrator/projects/proj1');
    cy.wait('@loadSubjects');
    cy.get('[data-cy="subjectCard-subj1"] [data-cy="disabledSubjectBadge"]').should('be.visible')
    cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();

    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('not.be.checked')
    cy.get('[data-cy="visibilitySwitch"]').click()
    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('be.checked')
    cy.clickSave()
    cy.wait('@updateSubject')

    cy.get('[data-cy="subjectCard-subj1"] [data-cy="disabledSubjectBadge"]').should('not.exist')
    cy.get('[data-cy="subjectCard-subj1"] [data-cy="editBtn"]').click();

    // no longer show the visibility switch after the subject is enabled
    cy.get('[data-cy="visibilitySwitch"]').should('not.exist')
  })

  it('enable a disabled subject on the subject page', () => {
    cy.createProject(1);
    cy.createSubject(1, 1, { enabled: false })

    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1').as('updateSubject');

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')

    cy.get('[data-cy="disabledSubjectBadge"]').should('be.visible')
    cy.get('[data-cy=btn_edit-subject]').click();
    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('not.be.checked')
    cy.get('[data-cy="visibilitySwitch"]').click()
    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('be.checked')
    cy.clickSave()
    cy.wait('@updateSubject')

    cy.get('[data-cy="disabledSubjectBadge"]').should('not.exist')
    cy.get('[data-cy=btn_edit-subject]').click();

    // no longer show the visibility switch after the subject is enabled
    cy.get('[data-cy="visibilitySwitch"]').should('not.exist')
  })

  it('creating a new skill for a disabled subject, new skill should be disabled', () => {
    cy.createProject(1);
    cy.createSubject(1, 1, { enabled: false })
    const expectedId = 'InitiallyDisabledSkillSkill'
    const providedName = 'Initially Disabled Skill'

    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill')
    cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists')
    cy.intercept('GET', `/admin/projects/proj1/entityIdExists?id=*`).as('skillIdExists')
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.get('[data-cy="disabledSubjectBadge"]').should('be.visible')

    cy.openNewSkillDialog()
    cy.get('[data-cy="visibilitySwitch"]').should('not.exist'); // not shown when subject is disabled
    cy.get('[data-cy="skillName"]').type(providedName)
    cy.wait('@nameExists')
    cy.wait('@skillIdExists')
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

  it('creating a new skill group for a disabled subject, new skill group should be disabled', () => {
    cy.createProject(1);
    cy.createSubject(1, 1, { enabled: false })
    const expectedId = 'InitiallyDisabledSkillGroupGroup'
    const providedName = 'Initially Disabled Skill Group'
    const skillName = 'Child 1'

    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill')
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/groups/InitiallyDisabledSkillGroupGroup/skills/Child1Skill').as('postNewChildSkill')
    cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists')
    cy.intercept('GET', `/admin/projects/proj1/entityIdExists?id=*`).as('skillIdExists')
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.get('[data-cy="disabledSubjectBadge"]').should('be.visible')

    // cy.openNewSkillDialog()
    cy.get('[data-cy="newGroupButton"]').click();
    cy.get('[data-cy="visibilitySwitch"]').should('not.exist'); // not shown when subject is disabled
    cy.get('[data-cy="name"]').type(providedName)
    cy.wait('@nameExists')
    cy.wait('@skillIdExists')
    cy.clickSave()
    cy.wait('@postNewSkill')

    cy.get('[data-pc-section="columnheadercontent"]').contains('Display').click()
    cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
    cy.get('[data-pc-section="overlay"] [aria-label="Points"]').click()
    cy.validateTable(tableSelector, [
      [{ colIndex: 2, value: `${providedName}` }, { colIndex: 5, value: '0from 0 skills' }]
    ], 10, false, null, false)

    cy.get(`[data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
    cy.get(`[data-cy="addSkillToGroupBtn-${expectedId}"]`).click();
    cy.get('[data-cy="skillName"]').type(skillName);
    cy.clickSave()
    cy.wait('@postNewChildSkill')

    cy.get('[data-cy="disabledBadge-InitiallyDisabledSkillGroupGroup"]').should('be.visible')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('have.text', '1');
    cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Groups_disabled"]').should('have.text', '1');

    // cy.get('[data-pc-section="columnheadercontent"]').contains('Display').click()
    cy.validateTable(`[data-cy="ChildRowSkillGroupDisplay_${expectedId}"] [data-cy="skillsTable"]`, [
      [{ colIndex: 2, value: 'Child 1' }, { colIndex: 5, value: 100 }]
    ], 5, true, null, false);


    cy.visit('/administrator/projects/proj1')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('have.text', '1');
  })

})
