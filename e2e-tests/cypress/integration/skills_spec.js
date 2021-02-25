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
describe('Skills Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        })
    });

    it('name causes id to fail validation', () => {

      cy.intercept({
        method: 'GET',
        url: '/admin/projects/proj1/subjects/subj1'
      }).as('loadSubject');

      cy.visit('/administrator/projects/proj1/subjects/subj1');
      cy.wait('@loadSubject');

      cy.clickButton('Skill');

      // name causes id to be too long
      const msg = 'Skill ID cannot exceed 50 characters.';
      const validNameButInvalidId = Array(46).fill('a').join('');
      cy.get('[data-cy=skillName]').click();
      cy.get('[data-cy=skillName]').fill(validNameButInvalidId);
      cy.get('[data-cy=idError]').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=skillName]').type('{backspace}');
      cy.get('[data-cy=idError]').should('not.be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');
    });

    it('close skill dialog', () => {

      cy.intercept({
        method: 'GET',
        url: '/admin/projects/proj1/subjects/subj1'
      }).as('loadSubject');

      cy.visit('/administrator/projects/proj1/subjects/subj1');
      cy.wait('@loadSubject');

      cy.clickButton('Skill');
      cy.get('[data-cy=closeSkillButton]').click();
      cy.get('[data-cy=closeSkillButton]').should('not.exist');
    });

    it('validation', () => {
      cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('postNewSkill');
      cy.intercept('GET', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('getSkill');
      cy.intercept({
        method: 'GET',
        url: '/admin/projects/proj1/subjects/subj1'
      }).as('loadSubject');

      cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/duplicate', {
        projectId: 'proj1',
        subjectId: "subj1",
        skillId: "duplicate",
        name: "Duplicate",
        pointIncrement: '50',
        numPerformToCompletion: '5'
      });

      cy.visit('/administrator/projects/proj1/subjects/subj1');
      cy.wait('@loadSubject');

      cy.clickButton('Skill');
      cy.get('[data-cy=skillName]').type('Skill123');
      cy.get('[data-cy=skillDescription]').type('loremipsum');
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');
      cy.get('[data-cy=skillName]').type('{selectall}Sk');
      cy.get('[data-cy=skillNameError]').contains('Skill Name cannot be less than 3 characters.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      const invalidName = Array(101).fill('a').join('');
      cy.get('[data-cy=skillName]').fill(invalidName);
      cy.get('[data-cy=skillNameError]').contains('Skill Name cannot exceed 100 characters.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=skillName]').type('{selectall}Duplicate');
      cy.get('[data-cy=skillNameError]').contains('The value for the Skill Name is already taken.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=skillName]').type('{selectall}Skill123');
      cy.get('[data-cy=skillNameError]').should('not.be.visible');

      cy.get('[data-cy=skillVersion]').type('{selectall}-5');
      cy.get('[data-cy=skillVersionError]').contains('Version may only contain numeric characters.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=skillVersion]').type('{selectall}1000');
      cy.get('[data-cy=skillVersionError]').contains('Version cannot exceed 999.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=skillVersion]').type('{selectall}2');
      cy.get('[data-cy=skillVersionError]').contains('Version 0 is the latest; max supported version is 1 (latest + 1)').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=skillVersion]').type('{selectall}1');
      cy.get('[data-cy=skillVersionError]').should('not.be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');

      cy.get('[data-cy=skillPointIncrement]').type('{selectall}-42');
      cy.get('[data-cy=skillPointIncrementError]').contains('Point Increment may only contain numeric characters.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=skillPointIncrement]').type('{selectall}11111111111');
      cy.get('[data-cy=skillPointIncrementError]').contains('Point Increment cannot exceed 10000.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=skillPointIncrement]').type('{selectall}11');
      cy.get('[data-cy=skillPointIncrementError]').should('not.be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');

      cy.get('[data-cy=numPerformToCompletion]').type('{selectall}-5');
      cy.get('[data-cy=skillOccurrencesError]').contains('Occurrences to Completion may only contain numeric characters.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=numPerformToCompletion]').type('{selectall}1000000');
      cy.get('[data-cy=skillOccurrencesError]').contains('Occurrences to Completion cannot exceed 10000.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=maxOccurrences]').type('{selectall}5')
      cy.get('[data-cy=numPerformToCompletion]').type('{selectall}3');
      cy.get('[data-cy=skillOccurrencesError]').contains('Must be more than or equals to \'Max Occurrences Within Window\' field').should('be.visible');
      cy.get('[data-cy=skillMaxOccurrencesError]').contains( 'Must be less than or equals to \'Occurrences to Completion\' field');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=maxOccurrences]').type('{selectall}2');
      cy.get('[data-cy=skillOccurrencesError]').should('not.be.visible');
      cy.get('[data-cy=skillMaxOccurrencesError]').should('not.be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');
      cy.get('[data-cy=numPerformToCompletion]').type('{selectall}1200');
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');
      cy.get('[data-cy=maxOccurrences]').type('{selectall}1000')
      cy.get('[data-cy=skillMaxOccurrencesError]').contains('Window\'s Max Occurrences cannot exceed 999.');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=maxOccurrences]').type('{selectall}999')
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');

      cy.get('[data-cy=timeWindowHours]').type('{selectall}0');
      cy.get('[data-cy=skillHoursError]').contains('Hours must be > 0 if Minutes = 0');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=timeWindowMinutes]').type('{selectall}90');
      cy.get('[data-cy=skillHoursError]').should('not.be.visible');
      cy.get('[data-cy=skillMinutesError]').should('be.visible');
      cy.get('[data-cy=skillMinutesError]').contains('Minutes must be 59 or less');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=timeWindowMinutes]').type('{selectall}0');
      cy.get('[data-cy=skillMinutesError]').contains('Minutes must be > 0 if Hours = 0');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=timeWindowHours]').type('{selectall}1');
      cy.get('[data-cy=skillHoursError]').should('not.be.visible');
      cy.get('[data-cy=skillMinutesError]').should('not.be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');

      cy.get('[data-cy=timeWindowHours]').type('{selectall}721');
      cy.get('[data-cy=skillHoursError]').contains('Time Window must be less then 720 hours');
      cy.get('[data-cy=skillMinutesError]').should('not.be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      cy.get('[data-cy=timeWindowHours]').type('{selectall}0');
      cy.get('[data-cy=timeWindowMinutes]').type('{selectall}43201');
      cy.get('[data-cy=skillMinutesError').contains('Minutes must be 59 or less');
      cy.get('[data-cy=skillHoursError]').should('not.be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
    });

    it('edit number of occurrences', () => {
        cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('postNewSkill');
        cy.intercept('GET', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('getSkill');
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');

        const selectorOccurrencesToCompletion = '[data-cy="numPerformToCompletion"]';
        const selectorSkillsRowToggle = '[data-cy="expandDetailsBtn_Skill1Skill"]';
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.wait('@loadSubject');

        cy.clickButton('Skill')
        cy.get(selectorOccurrencesToCompletion).should('have.value', '5')
        cy.get('#skillName').type('Skill 1')

        cy.clickSave()
        cy.wait('@postNewSkill');


        cy.get(selectorSkillsRowToggle).click()
        cy.get('[data-cy="childRowDisplay_Skill1Skill"]').contains('50 Points');

        cy.get('[data-cy="editSkillButton_Skill1Skill"]').click()
        cy.wait('@getSkill')

        cy.get(selectorOccurrencesToCompletion).should('have.value', '5')
        cy.get(selectorOccurrencesToCompletion).type('{backspace}10')
        cy.get(selectorOccurrencesToCompletion).should('have.value', '10')

        cy.clickSave()
        cy.wait('@postNewSkill');

        cy.get(selectorSkillsRowToggle).click()
        cy.get('[ data-cy="childRowDisplay_Skill1Skill"]').contains('100 Points')
    });

    it('create skill with special chars', () => {
        const expectedId = 'LotsofspecialPcharsSkill';
        const providedName = "!L@o#t$s of %s^p&e*c(i)/#?a_l++_|}{P c'ha'rs";

        cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill');
        cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists');

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');
        cy.clickButton('Skill');

        cy.get('#skillName').type(providedName);

        cy.getIdField().should('have.value', expectedId);
        cy.wait('@nameExists');

        cy.clickSave();
        cy.wait('@postNewSkill');

        cy.contains('ID: Lotsofspecial')
    });

    it('create skill using enter key', () => {
      const expectedId = 'LotsofspecialPcharsSkill';
      const providedName = "!L@o#t$s of %s^p&e*c(i)/#?a_l++_|}{P c'ha'rs";

      cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill');
      cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists');

      cy.intercept({
        method: 'GET',
        url: '/admin/projects/proj1/subjects/subj1'
      }).as('loadSubject');

      cy.visit('/administrator/projects/proj1/subjects/subj1');
      cy.wait('@loadSubject');
      cy.clickButton('Skill');

      cy.get('#skillName').type(providedName);

      cy.getIdField().should('have.value', expectedId);
      cy.wait('@nameExists');

      cy.get('#skillName').type('{enter}');
      cy.wait('@postNewSkill');

      cy.contains('ID: Lotsofspecial')
    });

    it('Add Skill Event', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });


        cy.intercept({
            method: 'POST',
            url: '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=TWO'
        }).as('suggestUsers');
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');
        cy.intercept({
            method: 'POST',
            url: '/api/projects/Inception/skills/ManuallyAddSkillEvent'
        }).as('addSkillEvent');

       cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
       cy.wait('@loadSkill');
       cy.contains('Add Event').click();

       cy.contains('ONE').click();
       cy.contains('TWO').click();
       cy.get('.existingUserInput button').contains('TWO');

       cy.contains('Enter user id').type('foo{enter}');
       cy.wait('@suggestUsers');
       cy.clickButton('Add');
       cy.wait('@addSkillEvent');
       cy.get('.text-success', {timeout: 5*1000}).contains('Added points for');
       cy.get('.text-success', {timeout: 5*1000}).contains('[foo]');

        cy.contains('Enter user id').type('bar{enter}');
        cy.wait('@suggestUsers');
        cy.clickButton('Add');
        cy.wait('@addSkillEvent');
        cy.get('.text-success', {timeout: 5*1000}).contains('Added points for');
        cy.get('.text-success', {timeout: 5*1000}).contains('[bar]');

        cy.contains('Enter user id').type('baz{enter}');
        cy.wait('@suggestUsers');
        cy.clickButton('Add');
        cy.wait('@addSkillEvent');
        cy.get('.text-success', {timeout: 5*1000}).contains('Added points for');
        cy.get('.text-success', {timeout: 5*1000}).contains('[baz]');

        cy.contains('Enter user id').type('fo');
        cy.wait('@suggestUsers');
        cy.get('li.multiselect__element').contains('foo').click();
    });

    it('Add Skill Event - suggest user with slash character does not cause error', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });


        cy.intercept({
            method: 'POST',
            url: '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=TWO'
        }).as('suggestUsers');
        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill');
        cy.contains('Add Event').click();

        cy.contains('ONE').click();
        cy.contains('TWO').click();
        cy.get('.existingUserInput button').contains('TWO');

        cy.contains('Enter user id').type('foo/bar{enter}');
        cy.wait('@suggestUsers');
    });

    it('Add Skill Event User Not Found', () => {
       cy.intercept({
         method: 'PUT',
         path: '/api/projects/*/skills/*',
       }, {
         statusCode: 400,
         body: {errorCode: 'UserNotFound', explanation: 'Some Error Occurred'}
       }).as('addUser');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill')

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill')


        cy.contains('Add Event').click();

        cy.contains('Enter user id').type('foo{enter}');

        cy.clickButton('Add');
        cy.wait('@addUser');
        cy.get('.text-danger', {timeout: 5*1000}).contains("Wasn't able to add points for");
    });

    it('Add Skill Event - user names cannot have spaces', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.intercept({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill');
        cy.contains('Add Event').click();

        const expectedErrMsg = 'User Id may not contain spaces';
        const userIdSelector = '[data-cy=userIdInput]';
        const addButtonSelector = '[data-cy=addSkillEventButton]';

        cy.get(userIdSelector).type('user a{enter}');
        cy.contains(expectedErrMsg)
        cy.get(addButtonSelector).should('be.disabled')

        cy.get(userIdSelector).type('userd{enter}');
        cy.contains(expectedErrMsg).should('not.exist');
        cy.get(addButtonSelector).should('not.be.disabled')

        cy.get(userIdSelector).type('user d{enter}');
        cy.contains(expectedErrMsg)
        cy.get(addButtonSelector).should('be.disabled')

        cy.get(userIdSelector).type('userOK{enter}');
        cy.contains(expectedErrMsg).should('not.exist');
        cy.get(addButtonSelector).should('not.be.disabled')
        cy.get(addButtonSelector).click();
        cy.contains('userOK');

        cy.get(userIdSelector).type('user@#$&*{enter}');
        cy.contains(expectedErrMsg).should('not.exist');
        cy.get(addButtonSelector).should('not.be.disabled')
        cy.get(addButtonSelector).click();
        cy.contains('user@#$&*');
    });

    it('create skill and then update skillId', () => {
      const initialId = 'myid1Skill';
      const newId = 'MyId1Skill';
      const providedName = "my id 1";

      cy.intercept('POST', `/admin/projects/proj1/subjects/subj1/skills/${initialId}`).as('postNewSkill');
      cy.intercept('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists');
      cy.intercept('GET', `/admin/projects/proj1/entityIdExists?id=*`).as('skillIdExists');


      cy.intercept({
          method: 'GET',
          url: '/admin/projects/proj1/subjects/subj1'
      }).as('loadSubject');

      cy.visit('/administrator/projects/proj1/subjects/subj1');
      cy.wait('@loadSubject');
      cy.clickButton('Skill');

      cy.get('#skillName').type(providedName);

      cy.getIdField().should('have.value', initialId);
      cy.wait('@nameExists');
      cy.wait('@skillIdExists');

      cy.clickSave();
      cy.wait('@postNewSkill');

      cy.contains(`ID: ${initialId}`)

      const editButtonSelector = `[data-cy=editSkillButton_${initialId}]`;
      cy.get(editButtonSelector).click()

      cy.contains("Enable").click()
      cy.getIdField().clear().type(newId);
      cy.wait('@skillIdExists');

      cy.clickSave();
      cy.wait('@postNewSkill');

      cy.contains(`ID: ${newId}`)
  });

  it('new skill button should retain focus after dialog closes', () => {

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');

    cy.get('[aria-label="new skill"]').click();
    cy.get('[data-cy=closeSkillButton]').click();
    cy.get('[aria-label="new skill"]').should('have.focus');

    cy.get('[aria-label="new skill"]').click();
    cy.get('[data-cy=skillName]').type('{esc}');
    cy.get('[aria-label="new skill"]').should('have.focus');

    cy.get('[aria-label="new skill"]').click();
    cy.get('[aria-label=Close]').click();
    cy.get('[aria-label="new skill"]').should('have.focus');

    cy.get('[aria-label="new skill"]').click();
    cy.get('[data-cy=skillName]').type('foobarbaz');
    cy.get('[data-cy=saveSkillButton]').click();
    cy.get('[aria-label="new skill"]').should('have.focus');
  });

  it('focus should be returned to subject edit button', () => {

    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: "subj1",
      skillId: "skill1",
      name: "Skill 1",
      pointIncrement: '50',
      numPerformToCompletion: '5'
    });

    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill2', {
      projectId: 'proj1',
      subjectId: "subj1",
      skillId: "skill2",
      name: "Skill 2",
      pointIncrement: '50',
      numPerformToCompletion: '5'
    });

    cy.intercept({
      method: 'POST',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('saveSkill');
    cy.intercept({
      method: 'POST',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill2'
    }).as('saveSkill2');

    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
    }).as('loadSkill');
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1/skills/skill2'
    }).as('loadSkill2');
    cy.intercept({
      method: 'GET',
      url: '/admin/projects/proj1/subjects/subj1'
    }).as('loadSubject');

    cy.visit('/administrator/projects/proj1/subjects/subj1');
    cy.wait('@loadSubject');
    //skill 2
    cy.get('[data-cy=editSkillButton_skill2]').click();
    cy.get('[data-cy=skillName]').should('be.visible');
    cy.get('[data-cy=skillName]').type('{esc}');
    cy.get('[data-cy=editSkillButton_skill2]').first().should('have.focus');

    cy.get('[data-cy=editSkillButton_skill2]').click();
    cy.get('[data-cy=closeSkillButton]').click();
    cy.get('[data-cy=editSkillButton_skill2]').should('have.focus');

    cy.get('[data-cy=editSkillButton_skill2]').click();
    cy.get('[data-cy=skillName]').type('test 123');
    cy.get('[data-cy=saveSkillButton]').click();
    cy.wait('@saveSkill2');
    cy.wait('@loadSkill2');
    cy.get('[data-cy=editSkillButton_skill2]').should('have.focus');

    cy.get('[data-cy=editSkillButton_skill2]').click();
    cy.get('[aria-label=Close]').filter('.text-light').click();
    cy.get('[data-cy=editSkillButton_skill2]').should('have.focus');
    cy.contains('Skill 2test 123');

    //skill 1
    cy.get('[data-cy=editSkillButton_skill1]').click();
    cy.get('[data-cy=skillName]').should('be.visible');
    cy.get('[data-cy=skillName]').type('{esc}');
    cy.get('[data-cy=editSkillButton_skill1]').should('have.focus');

    cy.get('[data-cy=editSkillButton_skill1]').click();
    cy.get('[data-cy=closeSkillButton]').click();
    cy.get('[data-cy=editSkillButton_skill1]').should('have.focus');

    cy.get('[data-cy=editSkillButton_skill1]').click();
    cy.get('[data-cy=skillName]').type('test 123');
    cy.get('[data-cy=saveSkillButton]').click();
    cy.wait('@saveSkill');
    cy.wait('@loadSkill');
    cy.get('[data-cy=editSkillButton_skill1]').should('have.focus');

    cy.get('[data-cy=editSkillButton_skill1]').click();
    cy.get('[aria-label=Close]').filter('.text-light').click();
    cy.get('[data-cy=editSkillButton_skill1]').should('have.focus');
  });

  it('skill user details does not break breadcrumb bar', () => {
    cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
      projectId: 'proj1',
      subjectId: "subj1",
      skillId: "skill1",
      name: "Skill 1",
      pointIncrement: '50',
      numPerformToCompletion: '5'
    });

    cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'someuser', timestamp: new Date().getTime()})
    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/');
    cy.clickNav('Users').click();
    cy.get('[data-cy="usersTable"]').contains('someuser').click();
    cy.get('[data-cy=breadcrumb-subj1]').should('be.visible');
    cy.get('[data-cy=breadcrumb-skill1]').should('be.visible');
    cy.get('[data-cy=breadcrumb-Users]').should('be.visible');
  })

    it('description is validated against custom validators', () => {
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1').as('loadSubject');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');
        cy.clickButton('Skill')

        cy.get('[data-cy="skillName"]').type('Great Name');
        cy.get('[data-cy="saveSkillButton"]').should('be.enabled');

        cy.get('[data-cy="skillDescription"]').type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="skillDescriptionError"]').contains('Skill Description - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveSkillButton"]').should('be.disabled');

        cy.get('[data-cy="skillDescription"]').type('{backspace}');
        cy.get('[data-cy="saveSkillButton"]').should('be.enabled');
    });


});
