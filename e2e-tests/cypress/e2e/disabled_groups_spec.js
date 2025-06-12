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

describe('Disabled Group Tests', () => {

  it('create initially disabled group', () => {
    cy.createProject(1);
    cy.createSubject(1, 1)

    const expectedId = 'InitiallyDisabledSkillGroupGroup'
    const providedName = 'Initially Disabled Skill Group'

    cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill')
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/groups/InitiallyDisabledSkillGroupGroup/skills/Child1Skill').as('postNewChildSkill')
    cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists')
    cy.intercept('GET', `/admin/projects/proj1/entityIdExists?id=*`).as('skillIdExists')
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.get('[data-cy="subjectCard-subj1"] [data-cy="disabledSubjectBadge"]').should('not.exist')

    cy.get('[data-cy="newGroupButton"]').click();
    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('be.checked')
    cy.get('[data-cy="visibilitySwitch"]').click()
    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('not.be.checked')
    cy.get('[data-cy="name"]').type(providedName)
    cy.wait('@nameExists')
    cy.wait('@skillIdExists')
    cy.clickSave()
    cy.wait('@postNewSkill')

    cy.get('[data-cy="disabledBadge-InitiallyDisabledSkillGroupGroup"]').should('be.visible')
  })

  it('enable a disabled group on the subject page', () => {
    cy.createProject(1);
    cy.createSubject(1, 1)
    cy.createSkillsGroup(1, 1, 1, { enabled: false });
    cy.addSkillToGroup(1, 1, 1, 1, { enabled: false })

    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/group1').as('updateGroup')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.get('[data-cy="subjectCard-subj1"] [data-cy="disabledSubjectBadge"]').should('not.exist')
    cy.get('[data-cy="disabledBadge-group1"]').should('be.visible')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('have.text', '1');
    cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Groups_disabled"]').should('have.text', '1');

    cy.get(`[data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
    cy.validateTable('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="skillsTable"]', [
      [{ colIndex: 2, value: 'Very Great Skill 1 Disabled' }]
    ], 5, true, null, false);
    cy.get('[data-cy="disabledBadge-skill1"]').should('be.visible')

    cy.get('[data-cy="editSkillButton_group1"]').click();

    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('not.be.checked')
    cy.get('[data-cy="visibilitySwitch"]').click()
    cy.get('[data-cy="visibilitySwitch"] [role="switch"]').should('be.checked')
    cy.clickSave()
    cy.wait('@updateGroup')

    cy.get('[data-cy="disabledBadge-group1"]').should('not.exist')
    cy.get('[data-cy="disabledBadge-skill1"]').should('not.exist')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '200');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '1');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('not.exist')
    cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '1');
    cy.get('[data-cy="pageHeaderStats_Groups_disabled"]').should('not.exist')

    cy.validateTable('[data-cy="ChildRowSkillGroupDisplay_group1"] [data-cy="skillsTable"]', [
      [{ colIndex: 2, value: 'Very Great Skill 1' }]
    ], 5, true, null, false);
    cy.get('[data-cy="editSkillButton_group1"]').click();

    // no longer show the visibility switch after the group is enabled
    cy.get('[data-cy="visibilitySwitch"]').should('not.exist')
  })

  it('creating a new skill for a disabled group, new child skill should be disabled', () => {
    cy.createProject(1);
    cy.createSubject(1, 1)
    cy.createSkillsGroup(1, 1, 20, { enabled: false });

    const childSkillName = 'Child 1'

    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/groups/group20/skills/Child1Skill').as('postNewChildSkill')
    cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists')
    cy.intercept('GET', `/admin/projects/proj1/entityIdExists?id=*`).as('skillIdExists')
    cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadSubject')
    cy.get('[data-cy="subjectCard-subj1"] [data-cy="disabledSubjectBadge"]').should('not.exist')

    cy.get('[data-cy="disabledBadge-group20"]').should('be.visible')

    cy.get(`[data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
    cy.get(`[data-cy="addSkillToGroupBtn-group20"]`).click();
    cy.get('[data-cy="visibilitySwitch"]').should('not.exist'); // not shown when group is disabled
    cy.get('[data-cy="skillName"]').type(childSkillName);
    cy.clickSave()
    cy.wait('@postNewChildSkill')

    cy.get('[data-cy="disabledBadge-group20"]').should('be.visible')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('have.text', '1');
    cy.get('[data-cy="pageHeaderStat_Groups"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Groups_disabled"]').should('have.text', '1');

    cy.validateTable(`[data-cy="ChildRowSkillGroupDisplay_group20"] [data-cy="skillsTable"]`, [
      [{ colIndex: 2, value: 'Child 1' }]
    ], 5, true, null, false);

    cy.visit('/administrator/projects/proj1')
    cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');
    cy.get('[data-cy="pageHeaderStats_Skills_disabled"]').should('have.text', '1');
  })

})
