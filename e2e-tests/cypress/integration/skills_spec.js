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

    it('validation', () => {
      cy.server()
      cy.route('POST', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('postNewSkill');
      cy.route('GET', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('getSkill');
      cy.route({
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

      cy.visit('/projects/proj1/subjects/subj1');
      cy.wait('@loadSubject');

      cy.clickButton('Skill');
      cy.get('[data-cy=skillName]').type('Skill123');
      cy.get('[data-cy=skillDescription]').type('loremipsum');
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');
      cy.get('[data-cy=skillName]').type('{selectall}Sk');
      cy.get('[data-cy=skillNameError]').contains('Skill Name cannot be less than 3 characters.').should('be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.disabled');
      const invalidName = Array(101).fill('a').join('');
      cy.get('[data-cy=skillName]').invoke('val', invalidName).trigger('input');
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
      cy.get('[data-cy=skillMinutesError]').should('not.be.visible');
      cy.get('[data-cy=saveSkillButton]').should('be.enabled');
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
        cy.server()
        cy.route('POST', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('postNewSkill');
        cy.route('GET', `/admin/projects/proj1/subjects/subj1/skills/Skill1Skill`).as('getSkill');
        cy.route({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');

        const selectorOccurrencesToCompletion = '[data-cy="numPerformToCompletion"]';
        const selectorSkillsRowToggle = 'table .VueTables__child-row-toggler';
        cy.visit('/projects/proj1/subjects/subj1');

        cy.wait('@loadSubject');

        cy.clickButton('Skill')
        cy.get(selectorOccurrencesToCompletion).should('have.value', '5')
        cy.get('#skillName').type('Skill 1')

        cy.clickSave()
        cy.wait('@postNewSkill');


        cy.get(selectorSkillsRowToggle).click()
        cy.contains('50 Points')

        cy.get('table .control-column .fa-edit').click()
        cy.wait('@getSkill')

        // close toast
        cy.get('.toast-header button').click({ multiple: true })
        cy.get(selectorOccurrencesToCompletion).should('have.value', '5')
        cy.get(selectorOccurrencesToCompletion).type('{backspace}10')
        cy.get(selectorOccurrencesToCompletion).should('have.value', '10')

        cy.clickSave()
        cy.wait('@postNewSkill');

        cy.get(selectorSkillsRowToggle).click()
        cy.contains('100 Points')
    });

    it('create skill with special chars', () => {
        const expectedId = 'LotsofspecialPcharsSkill';
        const providedName = "!L@o#t$s of %s^p&e*c(i)/#?a_l++_|}{P c'ha'rs";
        cy.server();
        cy.route('POST', `/admin/projects/proj1/subjects/subj1/skills/${expectedId}`).as('postNewSkill');
        cy.route('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists');

        cy.route({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1'
        }).as('loadSubject');

        cy.visit('/projects/proj1/subjects/subj1');
        cy.wait('@loadSubject');
        cy.clickButton('Skill');

        cy.get('#skillName').type(providedName);

        cy.getIdField().should('have.value', expectedId);
        cy.wait('@nameExists');

        cy.clickSave();
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

        cy.server();
        cy.route({
            method: 'POST',
            url: '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=TWO'
        }).as('suggestUsers');
        cy.route({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');
        cy.route({
            method: 'POST',
            url: '/api/projects/Inception/skills/ManuallyAddSkillEvent'
        }).as('addSkillEvent');

       cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
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

        cy.server();
        cy.route({
            method: 'POST',
            url: '/app/users/projects/proj1/suggestClientUsers?userSuggestOption=TWO'
        }).as('suggestUsers');
        cy.route({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill');
        cy.contains('Add Event').click();

        cy.contains('ONE').click();
        cy.contains('TWO').click();
        cy.get('.existingUserInput button').contains('TWO');

        cy.contains('Enter user id').type('foo/bar{enter}');
        cy.wait('@suggestUsers');
    });

    it('Add Skill Event User Not Found', () => {
       cy.server();
       cy.route({
           method: 'PUT',
           url: '/api/projects/*/skills/*',
           status: 400,
           response: {errorCode: 'UserNotFound', explanation: 'Some Error Occurred'}
       }).as('addUser');

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.route({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill')

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
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
        cy.server();
        cy.route({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
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

    it('Add Dependency failure', () => {
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

        cy.server();

        cy.route({
            method: 'POST',
            status: 400,
            url: '/admin/projects/proj1/skills/skill1/dependency/*',
            response: {errorCode: 'FailedToAssignDependency', explanation: 'Error Adding Dependency'}
        });

        cy.route({
            method: 'GET',
            url: '/admin/projects/proj1/subjects/subj1/skills/skill1'
        }).as('loadSkill');

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');
        cy.wait('@loadSkill')

        cy.get('div#menu-collapse-control li').contains('Dependencies').click();

        cy.get('.multiselect__tags').click();
        cy.get('.multiselect__tags input').type('{enter}')

        cy.get('div .alert').contains('Error! Request could not be completed! Error Adding Dependency');

    })

    it('create skill and then update skillId', () => {
      const initialId = 'myid1Skill';
      const newId = 'MyId1Skill';
      const providedName = "my id 1";
      cy.server();
      cy.route('POST', `/admin/projects/proj1/subjects/subj1/skills/${initialId}`).as('postNewSkill');
      cy.route('POST', `/admin/projects/proj1/skillNameExists`).as('nameExists');
      cy.route('GET', `/admin/projects/proj1/entityIdExists?id=*`).as('skillIdExists');


      cy.route({
          method: 'GET',
          url: '/admin/projects/proj1/subjects/subj1'
      }).as('loadSubject');

      cy.visit('/projects/proj1/subjects/subj1');
      cy.wait('@loadSubject');
      cy.clickButton('Skill');

      cy.get('#skillName').type(providedName);

      cy.getIdField().should('have.value', initialId);
      cy.wait('@nameExists');
      cy.wait('@skillIdExists');

      cy.clickSave();
      cy.wait('@postNewSkill');

      cy.contains(`ID: ${initialId}`)

      const editButtonSelector = '[data-cy=editSkillButton]';
      cy.get(editButtonSelector).click()

      cy.contains("Enable").click()
      cy.getIdField().clear().type(newId);
      cy.wait('@skillIdExists');

      cy.clickSave();
      cy.wait('@postNewSkill');

      cy.contains(`ID: ${newId}`)
  });

});
