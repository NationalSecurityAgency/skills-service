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
describe('Global Badges Tests', () => {

    const tableSelector = '[data-cy="badgeSkillsTable"]';
    const levelsTableSelector = '[data-cy="simpleLevelsTable"]';

    beforeEach(() => {
        cy.logout();
        const supervisorUser = 'supervisor@skills.org';
        cy.register(supervisorUser, 'password');
        cy.login('root@skills.org', 'password');
        cy.request('PUT', `/root/users/${supervisorUser}/roles/ROLE_SUPERVISOR`);
        cy.logout();
        cy.login(supervisorUser, 'password');
        cy.log('completed supervisor user login');

        // Cypress.Commands.add('selectSkill', (skillsSelector='[data-cy="skillsSelectionItem-proj1-skill1"]', retry=true) => {
        //     cy.get('[data-cy="skillsSelector"] [data-pc-section="dropdownicon"]').as('getOptions')
        //         .click();
        //     cy.wait(500);
        //     cy.get(skillsSelector).click();
        // });

        cy.intercept('GET', '/supervisor/badges/*/skills/available?*')
          .as('loadAvailableSkills');
    });

    it('Create badge with special chars', () => {

        const expectedId = 'LotsofspecialPcharsBadge';
        const providedName = '!L@o#t$s of %s^p&e*c/?#(i)a_l++_|}{P c\'ha\'rs';

        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');
        cy.intercept('PUT', `/supervisor/badges/${expectedId}`)
            .as('postGlobalBadge');
        cy.intercept('GET', `/supervisor/badges/id/${expectedId}/exists`)
            .as('idExists');
        cy.intercept('POST', '/supervisor/badges/name/exists')
            .as('nameExists');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR')
            .as('checkSupervisorRole');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');
        cy.wait('@checkSupervisorRole');

        cy.get('[data-cy="btn_Global Badges"]').click();

        cy.get('[data-cy="name"]')
            .type(providedName);
        cy.wait('@nameExists');
        cy.clickSave();
        cy.wait('@idExists');
        cy.wait('@postGlobalBadge');

        cy.contains(`ID: ${expectedId}`);
    });

    it('name causes id to fail validation', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxIdLength = 50;
                res.send(conf);
            });
        })
            .as('loadConfig');
        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');
        cy.intercept('POST', '/supervisor/badges/name/exists')
            .as('nameExists');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR')
            .as('checkSupervisorRole');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');
        cy.wait('@checkSupervisorRole');

        cy.get('[data-cy="btn_Global Badges"]').click();

        // name causes id to be too long
        const msg = 'Badge ID must be at most 50 characters';
        const validNameButInvalidId = Array(47)
            .fill('a')
            .join('');
        cy.get('[data-cy="name"]')
            .click();
        cy.get('[data-cy="name"]')
            .invoke('val', validNameButInvalidId)
            .trigger('input');
        cy.get('[data-cy=idError]')
            .contains(msg)
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.get('[data-cy="name"]')
            .type('{backspace}{backspace}');
        cy.get('[data-cy=idError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');
    });

    it('help url validation', () => {
        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');
        cy.intercept('POST', '/supervisor/badges/name/exists')
            .as('nameExists');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR')
            .as('checkSupervisorRole');
        cy.intercept('POST', '/api/validation/url')
            .as('customUrlValidation');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');
        cy.wait('@checkSupervisorRole');

        cy.get('[data-cy="btn_Global Badges"]').click();

        // name causes id to be too long
        cy.get('[data-cy="name"]')
            .click();
        cy.get('[data-cy="name"]')
            .type('Global Badge');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');

        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('javascript:alert("uh oh");');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('be.visible');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('have.text', 'Help URL/Path must start with "/" or "http(s)"');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('/foo?p1=v1&p2=v2');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('have.text', 'Help URL/Path must start with "/" or "http(s)"');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('http://foo.bar?p1=v1&p2=v2');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');
        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('https://foo.bar?p1=v1&p2=v2');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');

        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('https://');
        cy.wait('@customUrlValidation');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');

        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('https://---??..??##');
        cy.wait('@customUrlValidation');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.disabled');
        // trailing space should work now
        cy.get('[data-cy=skillHelpUrl]')
            .clear()
            .type('https://foo.bar?p1=v1&p2=v2 ');
        cy.wait('@customUrlValidation');
        cy.get('[data-cy=skillHelpUrlError]')
            .should('not.be.visible');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');
    });

    it('Delete badge', () => {
        const expectedId = 'JustABadgeBadge';
        const providedName = 'JustABadge';

        cy.request('PUT', `/supervisor/badges/${expectedId}`, {
            badgeId: expectedId,
            description: '',
            iconClass: 'fas fa-award',
            isEdit: false,
            name: providedName,
            originalBadgeId: ''
        });

        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR')
            .as('checkSupervisorRole');
        cy.intercept('DELETE', `/supervisor/badges/${expectedId}`)
            .as('deleteGlobalBadge');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');
        cy.wait('@checkSupervisorRole');

        cy.get('[data-cy=badgeCard-JustABadgeBadge] [data-cy="deleteBtn"]')
            .click();
        cy.contains('Removal Safety Check');
        cy.get('[data-cy=currentValidationText]')
            .type('Delete Me');
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled')
            .click();
        cy.wait('@deleteGlobalBadge');
        cy.contains('No Badges Yet')
            .should('be.visible');
    });

    it('Add skill dependencies to badge places focus on the select button', () => {
        cy.createGlobalBadge(1)
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.get('[data-cy="noContent"]').contains('No Skills Added Yet');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.get('[data-cy="skillsSelector"] button').should('have.focus');
    })

    it('Remove skill dependency from a badge places focus on the select button', () => {
        cy.createGlobalBadge(1)
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.get('[data-cy="badgeSkillsTable"] tr').contains('proj1')
        cy.get('[data-cy="deleteSkill_skill1"]').click()
        cy.contains('Remove Required Skill');
        cy.get('[data-pc-name="pcacceptbutton"]').click();
        cy.get('[data-cy="skillsSelector"] button').should('have.focus');
    })

    it('Cancel Delete dialog of a skill returns focus to the delete button', () => {
        cy.createGlobalBadge(1)
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.get('[data-cy="badgeSkillsTable"] tr').contains('proj1')
        cy.get('[data-cy="deleteSkill_skill1"]').click()
        cy.contains('Remove Required Skill');
        cy.get('[data-pc-name="pcrejectbutton"]').click();
        cy.get('[data-cy="deleteSkill_skill1"]').should('have.focus');
    })

    it('Add dependencies to badge', () => {
        //proj/subj/skill1
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        //proj/subj/skill2
        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'proj2'
        });
        cy.request('POST', '/admin/projects/proj2/subjects/subj1', {
            projectId: 'proj2',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj2/subjects/subj1/skills/skill1`, {
            projectId: 'proj2',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        const badgeId = 'a_badge';
        cy.request('PUT', `/supervisor/badges/${badgeId}`, {
            badgeId: badgeId,
            description: '',
            iconClass: 'fas fa-award',
            isEdit: false,
            name: 'A Badge',
            originalBadgeId: ''
        });

        cy.intercept('GET', '/supervisor/badges')
            .as('getBadges');
        cy.intercept('GET', '/supervisor/projects/proj2/levels')
            .as('getLevels');
        cy.intercept('GET', '/supervisor/badges/a_badge/projects/available*')
            .as('getAvailableLevels');

        cy.visit('/administrator/');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.clickNav('Global Badges');
        cy.wait('@getBadges');
        cy.get('[data-cy="manageBtn_a_badge"]')
            .click();
        cy.wait('@loadAvailableSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1', '', 'proj2')
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'proj2'
            }, {
                colIndex: 1,
                value: 'This is 1'
            }, {
                colIndex: 2,
                value: 'skill1'
            }],
        ], 5);

        cy.clickNav('Levels');
        cy.wait('@getAvailableLevels');

        cy.get('#project-selector')
            .click()
        cy.get('[data-pc-name="pcfilter"]').click().type('proj2');
        cy.wait('@getAvailableLevels');
        cy.get('[data-cy="proj2_option"]').click();

        cy.wait('@getLevels');
        cy.get('#level-selector')
            .click()
        cy.get('[data-pc-name="pcfilter"]').click().type('5');
        cy.get('[data-pc-section="option"]').contains(5).click();

        cy.contains('Add')
            .click();
        cy.validateTable(levelsTableSelector, [
            [{
                colIndex: 0,
                value: 'proj2'
            }, {
                colIndex: 1,
                value: '5'
            }],
        ], 5);

        cy.get('[data-cy=editProjectLevelButton_proj2]')
            .should('be.visible')
            .click();
        // cy.contains('Change Required Level for proj2')
        //     .should('be.visible');
        cy.get('#existingLevel > .p-inputtext')
            .should('have.value', 5);

        cy.get('.p-dialog-content #level-selector')
            .click()
        cy.get('[data-pc-section="option"]').contains(5).get('[data-p-disabled="true"]'); //should('be.disabled');

        cy.get('[data-pc-section="option"]').contains(1).click();
        cy.get('[data-cy=saveDialogBtn]')
            .should('be.enabled');
        cy.get('[data-cy=saveDialogBtn]')
            .click();
        cy.validateTable(levelsTableSelector, [
            [{
                colIndex: 0,
                value: 'proj2'
            }, {
                colIndex: 1,
                value: '1'
            }],
        ], 5);
        cy.get('[data-cy=editProjectLevelButton_proj2]')
            .should('have.focus');
        cy.get('[data-cy=editProjectLevelButton_proj2]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy=editProjectLevelButton_proj2]')
            .should('have.focus');
        cy.get('[data-cy=editProjectLevelButton_proj2]')
            .click();
        cy.get('[data-pc-name="pcclosebutton"]')
            .click();
        cy.get('[data-cy=editProjectLevelButton_proj2]')
            .should('have.focus');
    });

    it('Navigate to global badges menu entry', () => {

        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');

        cy.visit('/administrator/');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.clickNav('Global Badges');
        cy.wait('@getGlobalBadges');
        cy.contains('No Badges Yet');
    });

    it('Cannot publish Global Badge with no Skills and no Levels', () => {
        const expectedId = 'TestBadgeBadge';
        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');
        cy.intercept('PUT', `/supervisor/badges/${expectedId}`)
            .as('postGlobalBadge');
        cy.intercept('GET', `/supervisor/badges/id/${expectedId}/exists`)
            .as('idExists');
        cy.intercept('POST', '/supervisor/badges/name/exists')
            .as('nameExists');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR')
            .as('checkSupervisorRole');
        cy.intercept('GET', `/supervisor/badges/${expectedId}`)
            .as('getExpectedBadge');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');
        cy.wait('@checkSupervisorRole');

        cy.get('[data-cy="btn_Global Badges"]').click();

        cy.get('[data-cy="name"]')
            .type('Test Badge');
        cy.wait('@nameExists');
        cy.clickSave();
        cy.wait('@postGlobalBadge');

        cy.clickNav('Global Badges');

        cy.contains('Test Badge')
            .should('exist');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('This Global Badge has no assigned Skills or Project Levels. A Global Badge cannot be published without at least one Skill or Project Level.')
            .should('exist');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
    });

    it('Global Badge is disabled when created, can only be enabled once', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        const expectedId = 'TestBadgeBadge';
        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');
        cy.intercept('PUT', `/supervisor/badges/${expectedId}`)
            .as('postGlobalBadge');
        cy.intercept('GET', `/supervisor/badges/id/${expectedId}/exists`)
            .as('idExists');
        cy.intercept('POST', '/supervisor/badges/name/exists')
            .as('nameExists');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR')
            .as('checkSupervisorRole');
        cy.intercept('GET', `/supervisor/badges/${expectedId}`)
            .as('getExpectedBadge');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');
        cy.wait('@checkSupervisorRole');

        cy.get('[data-cy="btn_Global Badges"]').click();

        cy.get('[data-cy="name"]')
            .type('Test Badge');
        cy.wait('@nameExists');
        cy.clickSave();
        cy.wait('@postGlobalBadge');

        cy.clickNav('Global Badges');
        cy.contains('Manage')
            .click();
        cy.wait('@loadAvailableSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1')
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'proj1'
            }, {
                colIndex: 1,
                value: 'This is 1'
            }],
        ], 5);
        cy.contains('GlobalBadges')
            .click();

        cy.contains('Test Badge')
            .should('exist');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('Please Confirm!')
            .should('exist');
        cy.contains('Yes, Go Live!')
            .click();
        cy.wait('@postGlobalBadge');
        cy.wait('@getExpectedBadge');
        cy.wait('@getGlobalBadges');
        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Live')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .should('not.exist');
    });

    it('Canceling go live dialog should leave global badge disabled', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        const expectedId = 'TestBadgeBadge';
        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');
        cy.intercept('PUT', `/supervisor/badges/${expectedId}`)
            .as('postGlobalBadge');
        cy.intercept('GET', `/supervisor/badges/id/${expectedId}/exists`)
            .as('idExists');
        cy.intercept('POST', '/supervisor/badges/name/exists')
            .as('nameExists');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR')
            .as('checkSupervisorRole');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');
        cy.wait('@checkSupervisorRole');

        cy.get('[data-cy="btn_Global Badges"]').click();

        cy.get('[data-cy="name"]')
            .type('Test Badge');
        cy.wait('@nameExists');
        cy.clickSave();
        cy.wait('@postGlobalBadge');

        cy.contains('Test Badge')
            .should('exist');
        cy.contains('Manage')
            .click();
        cy.wait('@loadAvailableSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1')
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'proj1'
            }, {
                colIndex: 1,
                value: 'This is 1'
            }],
        ], 5);

        cy.contains('GlobalBadges')
            .click();
        cy.wait('@getGlobalBadges');

        cy.contains('Test Badge')
            .should('exist');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('Please Confirm!')
            .should('exist');
        cy.contains('Cancel')
            .click();
        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Live')
            .should('not.exist');
        cy.get('[data-cy=goLive]')
            .should('exist');
    });

    it('Can add Skill and Level requirements to disabled Global Badge', () => {
        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');
        cy.intercept('PUT', `/supervisor/badges/ABadgeBadge`)
            .as('postGlobalBadge');
        cy.intercept('GET', `/supervisor/badges/id/ABadgeBadge/exists`)
            .as('idExists');
        cy.intercept('POST', '/supervisor/badges/name/exists')
            .as('nameExists');
        cy.intercept('GET', '/supervisor/badges/ABadgeBadge/projects/available*')
            .as('availableProjects');
        cy.intercept('GET', '/supervisor/badges/ABadgeBadge/skills/available?query=')
            .as('availableSkills');
        cy.intercept('GET', '/supervisor/badges/ABadgeBadge')
            .as('badgeInfo');
        cy.intercept('GET', '/supervisor/projects/proj2/levels')
            .as('proj2Levels');
        //proj/subj/skill1
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        //proj/subj/skill2
        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'proj2'
        });
        cy.request('POST', '/admin/projects/proj2/subjects/subj1', {
            projectId: 'proj2',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj2/subjects/subj1/skills/skill1`, {
            projectId: 'proj2',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.visit('/administrator/globalBadges/');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');

        cy.get('[data-cy="btn_Global Badges"]').click();

        cy.wait(1000)
        cy.get('[data-cy="name"]')
            .type('A Badge', { delay: 100 })
        cy.wait('@nameExists');
        cy.clickSave();
        cy.wait('@idExists');
        cy.wait('@postGlobalBadge');

        cy.contains('A Badge')
            .should('exist');
        cy.contains('Manage')
            .click();
        cy.wait('@loadAvailableSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1')
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'proj1'
            }, {
                colIndex: 1,
                value: 'This is 1'
            }, {
                colIndex: 2,
                value: 'skill1'
            }],
        ], 5);

        cy.clickNav('Levels');

        cy.wait('@availableProjects');
        cy.wait('@badgeInfo');

        cy.get('#project-selector')
            .click()
        cy.get('[data-pc-name="pcfilter"]').click().type('proj2');
        cy.wait('@availableProjects');
        cy.get('[data-cy="proj2_option"]').click();

        cy.wait('@proj2Levels');
        cy.get('#level-selector')
            .click()
        cy.get('[data-pc-name="pcfilter"]').click().type('5');
        cy.get('[data-pc-section="option"]').contains(5).click();

        cy.contains('Add')
            .click();
        cy.get('#simple-levels-table')
            .should('be.visible');

        cy.get('[data-cy="breadcrumb-GlobalBadges"]')
            .click();
        cy.wait('@getGlobalBadges');

        cy.contains('A Badge')
            .should('exist');

        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('Please Confirm!')
            .should('exist');
        cy.contains('Yes, Go Live!')
            .click();
        cy.wait('@getGlobalBadges');
        cy.contains('A Badge')
            .should('exist');
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Live')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .should('not.exist');
    });

    it('Removing all skills should not cause published Global Badge to become disabled', () => {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 1)

        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.assignProjectToGlobalBadge(1, 2, 5);
        cy.enableGlobalBadge();

        cy.visit('/administrator/globalBadges/globalBadge1');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'skill1'
            }],
        ], 5);

        cy.get('[data-cy=deleteSkill_skill1]')
             .click();
        cy.contains('YES, Delete It!')
            .click();
        cy.contains('No Skills Added Yet');
        cy.get('[data-cy="breadcrumb-GlobalBadges"]')
            .click();
        cy.get('[data-cy=badgeStatus]')
            .contains('Status: Live')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .should('not.exist');
    });

    it('new badge button should retain focus after dialog is closed', () => {
        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');

        cy.visit('/administrator/');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.clickNav('Global Badges');
        cy.wait('@getGlobalBadges');

        cy.get('[aria-label="new global badge"]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[aria-label="new global badge"]')
            .should('have.focus');

        cy.get('[aria-label="new global badge"]')
            .click();
        cy.get('[data-cy="name"]')
            .type('{esc}');
        cy.get('[aria-label="new global badge"]')
            .should('have.focus');

        cy.get('[aria-label="new global badge"]')
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[aria-label="new global badge"]')
            .should('have.focus');

        cy.get('[aria-label="new global badge"]')
            .click();
        cy.get('[data-cy="name"]')
            .type('test 123');
        cy.get('[data-cy=saveDialogBtn]')
            .click();
        cy.get('[aria-label="new global badge"]')
            .should('have.focus');
    });

    it('global badge skills table does not have manage button', () => {
        cy.request('POST', '/app/projects/proj1', {
             projectId: 'proj1',
             name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        const numSkills = 8;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${i}`,
                name: `This is ${100 - i}`,
                type: 'Skill',
                pointIncrement: 100,
                numPerformToCompletion: 5,
                pointIncrementInterval: 0,
                numMaxOccurrencesIncrementInterval: -1,
                version: 0,
            });
        }

        cy.request('POST', '/supervisor/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/supervisor/badges/badge1/projects/proj1/skills/skill${i}`);
        }

        cy.visit('/administrator/globalBadges/badge1');
        cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get(`${tableSelector} th`)
            .contains('Skill ID')
            .click();

        for (let i = 0; i < 5; i +=1) {
            cy.get(`[data-cy="manage_skill${i}"]`).should('not.exist')
            cy.get(`[data-cy="deleteSkill_skill${i}"]`).should('exist')
        }

    });

    it('sort skills table', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        const numSkills = 8;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${i}`,
                name: `This is ${100 - i}`,
                type: 'Skill',
                pointIncrement: 100,
                numPerformToCompletion: 5,
                pointIncrementInterval: 0,
                numMaxOccurrencesIncrementInterval: -1,
                version: 0,
            });
        }

        cy.request('POST', '/supervisor/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/supervisor/badges/badge1/projects/proj1/skills/skill${i}`);
        }

        cy.visit('/administrator/globalBadges/badge1');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get(`${tableSelector} th`)
            .contains('Skill Name')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'This is 93'
            }, {
                colIndex: 2,
                value: 'skill7'
            }],
            [{
                colIndex: 1,
                value: 'This is 94'
            }, {
                colIndex: 2,
                value: 'skill6'
            }],
            [{
                colIndex: 1,
                value: 'This is 95'
            }, {
                colIndex: 2,
                value: 'skill5'
            }],
            [{
                colIndex: 1,
                value: 'This is 96'
            }, {
                colIndex: 2,
                value: 'skill4'
            }],
            [{
                colIndex: 1,
                value: 'This is 97'
            }, {
                colIndex: 2,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'This is 98'
            }, {
                colIndex: 2,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'This is 99'
            }, {
                colIndex: 2,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'This is 100'
            }, {
                colIndex: 2,
                value: 'skill0'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Skill Name')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'This is 100'
            }, {
                colIndex: 2,
                value: 'skill0'
            }],
            [{
                colIndex: 1,
                value: 'This is 99'
            }, {
                colIndex: 2,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'This is 98'
            }, {
                colIndex: 2,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'This is 97'
            }, {
                colIndex: 2,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'This is 96'
            }, {
                colIndex: 2,
                value: 'skill4'
            }],
            [{
                colIndex: 1,
                value: 'This is 95'
            }, {
                colIndex: 2,
                value: 'skill5'
            }],
            [{
                colIndex: 1,
                value: 'This is 94'
            }, {
                colIndex: 2,
                value: 'skill6'
            }],
            [{
                colIndex: 1,
                value: 'This is 93'
            }, {
                colIndex: 2,
                value: 'skill7'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Skill ID')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'This is 100'
            }, {
                colIndex: 2,
                value: 'skill0'
            }],
            [{
                colIndex: 1,
                value: 'This is 99'
            }, {
                colIndex: 2,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'This is 98'
            }, {
                colIndex: 2,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'This is 97'
            }, {
                colIndex: 2,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'This is 96'
            }, {
                colIndex: 2,
                value: 'skill4'
            }],
            [{
                colIndex: 1,
                value: 'This is 95'
            }, {
                colIndex: 2,
                value: 'skill5'
            }],
            [{
                colIndex: 1,
                value: 'This is 94'
            }, {
                colIndex: 2,
                value: 'skill6'
            }],
            [{
                colIndex: 1,
                value: 'This is 93'
            }, {
                colIndex: 2,
                value: 'skill7'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Skill ID')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'This is 93'
            }, {
                colIndex: 2,
                value: 'skill7'
            }],
            [{
                colIndex: 1,
                value: 'This is 94'
            }, {
                colIndex: 2,
                value: 'skill6'
            }],
            [{
                colIndex: 1,
                value: 'This is 95'
            }, {
                colIndex: 2,
                value: 'skill5'
            }],
            [{
                colIndex: 1,
                value: 'This is 96'
            }, {
                colIndex: 2,
                value: 'skill4'
            }],
            [{
                colIndex: 1,
                value: 'This is 97'
            }, {
                colIndex: 2,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'This is 98'
            }, {
                colIndex: 2,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'This is 99'
            }, {
                colIndex: 2,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'This is 100'
            }, {
                colIndex: 2,
                value: 'skill0'
            }],
        ], 5);
    });

    it('sort levels table', () => {
        const numProjects = 8;

        for (let i = 0; i < numProjects; i += 1) {
            cy.request('POST', `/app/projects/proj${i}`, {
                projectId: `proj${i}`,
                name: `proj${10 - i}`,
            });
        }

        cy.request('POST', '/supervisor/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        for (let i = 0; i < numProjects; i += 1) {
            cy.request('POST', `/supervisor/badges/badge1/projects/proj${i}/level/${i < 5 ? i + 1 : '1'}`);
        }

        cy.visit('/administrator/globalBadges/badge1/levels');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get(`${levelsTableSelector} th`)
            .contains('Level')
            .click();
        cy.validateTable(levelsTableSelector, [
            [{
                colIndex: 1,
                value: '1'
            }],
            [{
                colIndex: 1,
                value: '1'
            }],
            [{
                colIndex: 1,
                value: '1'
            }],
            [{
                colIndex: 1,
                value: '1'
            }],
            [{
                colIndex: 1,
                value: '2'
            }],
            [{
                colIndex: 1,
                value: '3'
            }],
            [{
                colIndex: 1,
                value: '4'
            }],
            [{
                colIndex: 1,
                value: '5'
            }],
        ], 5);

        cy.get(`${levelsTableSelector} th`)
            .contains('Level')
            .click();
        cy.validateTable(levelsTableSelector, [
            [{
                colIndex: 1,
                value: '5'
            }],
            [{
                colIndex: 1,
                value: '4'
            }],
            [{
                colIndex: 1,
                value: '3'
            }],
            [{
                colIndex: 1,
                value: '2'
            }],
            [{
                colIndex: 1,
                value: '1'
            }],
            [{
                colIndex: 1,
                value: '1'
            }],
            [{
                colIndex: 1,
                value: '1'
            }],
            [{
                colIndex: 1,
                value: '1'
            }],
        ], 5);

        cy.get(`${levelsTableSelector} th`)
            .contains('Project Name')
            .click();
        cy.validateTable(levelsTableSelector, [
            [{
                colIndex: 0,
                value: 'proj3'
            }],
            [{
                colIndex: 0,
                value: 'proj4'
            }],
            [{
                colIndex: 0,
                value: 'proj5'
            }],
            [{
                colIndex: 0,
                value: 'proj6'
            }],
            [{
                colIndex: 0,
                value: 'proj7'
            }],
            [{
                colIndex: 0,
                value: 'proj8'
            }],
            [{
                colIndex: 0,
                value: 'proj9'
            }],
            [{
                colIndex: 0,
                value: 'proj10'
            }],
        ], 5);

        cy.get(`${levelsTableSelector} th`)
            .contains('Project Name')
            .click();
        cy.validateTable(levelsTableSelector, [
            [{
                colIndex: 0,
                value: 'proj10'
            }],
            [{
                colIndex: 0,
                value: 'proj9'
            }],
            [{
                colIndex: 0,
                value: 'proj8'
            }],
            [{
                colIndex: 0,
                value: 'proj7'
            }],
            [{
                colIndex: 0,
                value: 'proj6'
            }],
            [{
                colIndex: 0,
                value: 'proj5'
            }],
            [{
                colIndex: 0,
                value: 'proj4'
            }],
            [{
                colIndex: 0,
                value: 'proj3'
            }],
        ], 5);
    });

    it('remove skill after navigating to the link directly', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        const numSkills = 2;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${i}`,
                name: `This is ${100 - i}`,
                type: 'Skill',
                pointIncrement: 100,
                numPerformToCompletion: 5,
                pointIncrementInterval: 0,
                numMaxOccurrencesIncrementInterval: -1,
                version: 0,
            });
        }

        cy.request('POST', '/supervisor/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/supervisor/badges/badge1/projects/proj1/skills/skill${i}`);
        }

        cy.visit('/administrator/globalBadges/badge1');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get(`${tableSelector} th`)
            .contains('Skill ID')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'This is 100'
            }, {
                colIndex: 2,
                value: 'skill0'
            }],
            [{
                colIndex: 1,
                value: 'This is 99'
            }, {
                colIndex: 2,
                value: 'skill1'
            }],
        ], 5);

        cy.get('[data-cy="deleteSkill_skill1"]')
            .click();
        cy.contains('YES, Delete It!')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'This is 100'
            }, {
                colIndex: 2,
                value: 'skill0'
            }],
        ], 5);
    });

    it('remove level after navigating to the page directly', () => {
        const numProjects = 2;

        for (let i = 0; i < numProjects; i += 1) {
            cy.request('POST', `/app/projects/proj${i}`, {
                projectId: `proj${i}`,
                name: `proj${10 - i}`,
            });
        }

        cy.request('POST', '/supervisor/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        for (let i = 0; i < numProjects; i += 1) {
            cy.request('POST', `/supervisor/badges/badge1/projects/proj${i}/level/${i < 5 ? i + 1 : '1'}`);
        }

        cy.visit('/administrator/globalBadges/badge1/levels');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get(`${levelsTableSelector} th`)
            .contains('Level')
            .click();
        cy.validateTable(levelsTableSelector, [
            [{
                colIndex: 0,
                value: 'proj10'
            }, {
                colIndex: 1,
                value: '1'
            }],
            [{
                colIndex: 0,
                value: 'proj9'
            }, {
                colIndex: 1,
                value: '2'
            }],
        ], 5);

        cy.get('[data-cy="deleteLevelBtn_proj1-2"]')
            .click();
        cy.contains('YES, Delete It!')
            .click();
        cy.validateTable(levelsTableSelector, [
            [{
                colIndex: 0,
                value: 'proj10'
            }, {
                colIndex: 1,
                value: '1'
            }],
        ], 5);
    });

    it('edit badge button should retain focus after dialog is closed', () => {
        cy.request('POST', '/supervisor/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.request('POST', '/supervisor/badges/badge2', {
            projectId: 'proj1',
            badgeId: 'badge2',
            name: 'Badge 2'
        });

        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');

        cy.visit('/administrator/');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.clickNav('Global Badges');
        cy.wait('@getGlobalBadges');

        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .click();
        cy.get('[data-cy="name"]')
            .type('{esc}');
        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .click();
        cy.get('[data-cy=closeDialogBtn]')
            .click();
        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .click();
        cy.get('[aria-label=Close]')
            .click();
        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .click();
        cy.get('[data-cy="name"]')
            .type('{esc}');
        cy.get('[data-cy="badgeCard-badge2"] [data-cy=editBtn]')
            .should('have.focus');

        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .click();
        cy.contains('Badge 1');
        cy.get('[data-cy="name"]')
            .type('42');
        cy.get('[data-cy=saveDialogBtn]')
            .click();
        cy.wait('@getGlobalBadges');
        cy.get('[data-cy="badgeCard-badge1"] [data-cy=editBtn]')
            .should('have.focus');
    });

    it('description is validated against custom validators', () => {
        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');

        cy.get('[data-cy="btn_Global Badges"]')
            .click();

        cy.get('[data-cy="name"]')
            .type('Great Name');

        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="markdownEditorInput"]')
            .type('ldkj aljdl aj\n\njabberwocky');
        cy.get('[data-cy="descriptionError"]')
            .contains('Badge Description - paragraphs may not contain jabberwocky');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="markdownEditorInput"]')
            .type('{backspace}');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="descriptionError"]')
            .contains('Subject Description - paragraphs may not contain jabberwocky')
            .should('not.exist');
    });

    it('description is validated against custom validators', () => {
        cy.intercept('GET', `/supervisor/badges`)
          .as('getGlobalBadges');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');

        cy.get('[data-cy="btn_Global Badges"]')
          .click();

        cy.get('[data-cy="name"]')
          .type('Great Name');

        cy.get('[data-cy="nameError"]')
          .should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.enabled');

        cy.get('input[data-cy="name"]')
          .type('{selectall}(A) Updated Badge Name');
        cy.get('[data-cy="nameError"]')
          .contains('Badge Name - names may not contain (A)');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.disabled');

        cy.get('input[data-cy="name"]')
          .type('{selectall}(B) A Updated Badge Name');
        cy.get('[data-cy="nameError"]')
          .should('not.be.visible');
        cy.get('[data-cy="saveDialogBtn"]')
          .should('be.enabled');
    });

    it('edit in place', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'Proj 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        //proj/subj/skill2
        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'proj2'
        });
        cy.request('POST', '/admin/projects/proj2/subjects/subj1', {
            projectId: 'proj2',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj2/subjects/subj1/skills/skill1`, {
            projectId: 'proj2',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        const badgeId = 'a_badge';
        cy.request('PUT', `/supervisor/badges/${badgeId}`, {
            badgeId: badgeId,
            description: '',
            iconClass: 'fas fa-award',
            isEdit: false,
            name: 'A Badge',
            originalBadgeId: ''
        });

        cy.intercept('GET', '/supervisor/badges')
            .as('getBadges');
        cy.intercept('GET', '/supervisor/projects/proj2/levels')
            .as('getLevels');
        cy.intercept('GET', '/supervisor/badges/a_new_id/projects/available*')
            .as('getAvailableLevels');
        cy.intercept('DELETE', '/supervisor/badges/a_new_id/projects/proj2/level/5')
            .as('removeLevel');

        cy.visit('/administrator/globalBadges/a_badge/');
        cy.get('[data-cy="inception-button"]').contains('Level');

        cy.contains('BADGE: A Badge')
            .should('be.visible');
        cy.contains('ID: a_badge')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-a_badge]')
            .should('be.visible');

        cy.get('button[data-cy=btn_edit-badge]')
            .click();
        cy.get('input[data-cy="name"]')
            .type('{selectall}New Global Badge Name');
        cy.get('button[data-cy=saveDialogBtn]')
            .click();
        cy.contains('BADGE: A Badge')
            .should('not.exist');
        cy.contains('BADGE: New Global Badge Name')
            .should('be.visible');
        cy.contains('ID: a_badge')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-a_badge]')
            .should('be.visible');
        cy.get('button[data-cy=btn_edit-badge]')
            .should('have.focus');

        cy.get('button[data-cy=btn_edit-badge]')
            .click();
        cy.get('[data-cy=enableIdInput]').click();
        cy.get('input[data-cy=idInputValue]')
            .type('{selectall}a_new_id');
        cy.get('button[data-cy=saveDialogBtn]')
            .click();
        cy.contains('ID: a_badge')
            .should('not.exist');
        cy.contains('ID: a_new_id')
            .should('be.visible');
        cy.get('[data-cy=breadcrumb-a_badge]')
            .should('not.exist');
        cy.get('[data-cy=breadcrumb-a_new_id]')
            .should('be.visible');
        cy.contains('BADGE: New Global Badge Name')
            .should('be.visible');
        cy.location()
            .should((loc) => {
                expect(loc.pathname)
                    .to
                    .eq('/administrator/globalBadges/a_new_id');
            });
        cy.wait('@loadAvailableSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1', '', 'proj2')
        cy.get('button[data-cy=deleteSkill_skill1]')
            .click();
        cy.contains('YES, Delete It!')
            .click();

        cy.clickNav('Levels');
        cy.wait('@getAvailableLevels');
        cy.get('#project-selector')
            .click()
        cy.get('[data-pc-name="pcfilter"]').click().type('proj2');
        cy.wait('@getAvailableLevels');
        cy.get('[data-cy="proj2_option"]').click();

        cy.wait('@getLevels');
        cy.get('#level-selector')
            .click()
        cy.get('[data-pc-name="pcfilter"]').click().type('5');
        cy.get('[data-pc-section="option"]').contains(5).click();

        cy.contains('Add')
            .click();
        cy.get('[data-cy=deleteLevelBtn_proj2-5]')
            .should('exist');
        cy.get('[data-cy=deleteLevelBtn_proj2-5]')
            .click();
        cy.contains('YES, Delete It!')
            .click();
        cy.wait('@removeLevel');
        cy.get('[data-cy=deleteLevelBtn_proj2-5]')
            .should('not.exist');
    });

    it('project can not be deleted when it belongs to a global badge', () => {
        cy.createProject(1);
        cy.createProject(2);
        cy.createGlobalBadge(1);
        cy.assignProjectToGlobalBadge(1, 1, 2);

        cy.visit('/administrator/');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="deleteProjBtn"]')
            .click();
        cy.contains('Removal Safety Check');
        cy.get('#stepOneCheck').check();
        cy.get('[data-cy="firstNextButton"]').click();

        cy.get('#stepTwoCheck').check();
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy=currentValidationText]')
            .type('Delete This Project');
        cy.get('[data-cy=deleteProjectButton]')
            .should('be.enabled')
            .click();
        cy.contains('Cannot delete this project as it belongs to one or more global badges');
        cy.contains('Ok')
            .click();

        cy.get('[data-cy="projectCard_proj2"] [data-cy="deleteProjBtn"]')
            .click();
        cy.contains('Removal Safety Check');
        cy.get('#stepOneCheck').check();
        cy.get('[data-cy="firstNextButton"]').click();

        cy.get('#stepTwoCheck').check();
        cy.get('[data-cy="secondNextButton"]').click();

        cy.get('[data-cy=currentValidationText]')
            .type('Delete This Project');
        cy.contains('Cannot delete this project as it belongs to one or more global badges')
            .should('not.exist');
    });

    it('edit from global badges page', () => {
        cy.createGlobalBadge(1);
        cy.createGlobalBadge(2);

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy="badgeCard-globalBadge1"]')
            .contains('Global Badge 1');
        cy.get('[data-cy="badgeCard-globalBadge2"]')
            .contains('Global Badge 2');

        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="editBtn"]')
            .click();
        cy.get('[data-cy="name"]')
            .clear()
            .type('Other Name');
        cy.clickSave();

        cy.get('[data-cy="badgeCard-globalBadge1"]')
            .contains('Global Badge 1')
            .should('not.exist');
        cy.get('[data-cy="badgeCard-globalBadge1"]')
            .contains('Other Name');
        cy.get('[data-cy="badgeCard-globalBadge2"]')
            .contains('Global Badge 2');
    });

    it('navigate to badge by clicking on name and icon', () => {
        cy.createGlobalBadge(1);
        cy.createGlobalBadge(2);

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');

        // using title link
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="titleLink"]')
            .click();
        cy.contains('ID: globalBadge1');
        cy.contains('No Skills Added Yet');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');

        // using icon link
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="iconLink"]')
            .click();
        cy.contains('ID: globalBadge1');
        cy.contains('No Skills Added Yet');

    });

    it('drag-and-drop sort management', () => {
        cy.createGlobalBadge(1);
        cy.createGlobalBadge(2);
        cy.createGlobalBadge(3);
        cy.createGlobalBadge(4);
        cy.createGlobalBadge(5);

        const badge1Card = '[data-cy="badgeCard-globalBadge1"] [data-cy="sortControlHandle"]';
        const badge2Card = '[data-cy="badgeCard-globalBadge2"] [data-cy="sortControlHandle"]';
        const badge4Card = '[data-cy="badgeCard-globalBadge4"] [data-cy="sortControlHandle"]';
        const badge5Card = '[data-cy="badgeCard-globalBadge5"] [data-cy="sortControlHandle"]';

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 1', 'Badge 2', 'Badge 3', 'Badge 4', 'Badge 5']);
        cy.get(badge1Card)
            .dragAndDrop(badge4Card);
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 2', 'Badge 3', 'Badge 4', 'Badge 1', 'Badge 5']);

        // refresh to make sure it was saved
        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 2', 'Badge 3', 'Badge 4', 'Badge 1', 'Badge 5']);

        cy.get(badge5Card)
            .dragAndDrop(badge2Card);
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 5', 'Badge 2', 'Badge 3', 'Badge 4', 'Badge 1']);

        cy.get(badge2Card)
            .dragAndDrop(badge1Card);
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 5', 'Badge 3', 'Badge 4', 'Badge 1', 'Badge 2']);

        // refresh to make sure it was saved
        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 5', 'Badge 3', 'Badge 4', 'Badge 1', 'Badge 2']);
    });

    it('no drag-and-drag sort controls when there is only 1 badge', () => {
        cy.createGlobalBadge(1);
        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="badgeCard-globalBadge1"]');
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="sortControlHandle"]')
            .should('not.exist');

        cy.createGlobalBadge(2);
        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="badgeCard-globalBadge1"]');
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="sortControlHandle"]');
    });

    it('drag-and-drag sort should spinner while backend operation is happening', () => {
        cy.intercept('/supervisor/badges/globalBadge1', (req) => {
            req.reply((res) => {
                res.send({ delay: 6000 });
            });
        })
            .as('badge1Async');

        cy.createGlobalBadge(1);
        cy.createGlobalBadge(2);

        const badge1Card = '[data-cy="badgeCard-globalBadge1"] [data-cy="sortControlHandle"]';
        const badge2Card = '[data-cy="badgeCard-globalBadge2"] [data-cy="sortControlHandle"]';

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 1', 'Badge 2']);
        cy.get(badge1Card)
            .dragAndDrop(badge2Card);

        // overlay over both cards but loading message only on badge 1
        cy.get('[data-cy="globalBadge1_overlayShown"] [data-cy="updatingSortMsg"]')
            .contains('Updating sort order');
        cy.get('[data-cy="globalBadge2_overlayShown"]');
        cy.get('[data-cy="globalBadge2_overlayShown"] [data-cy="updatingSortMsg"]')
            .should('not.exist');
        cy.wait('@badge1Async');
        cy.get('[data-cy="globalBadge1_overlayShown"]')
            .should('not.exist');
        cy.get('[data-cy="globalBadge2_overlayShown"]')
            .should('not.exist');
    });

    it('badge card stats', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);

        cy.createProject(2);
        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.assignSkillToGlobalBadge(1, 2, 1);
        cy.assignProjectToGlobalBadge(1, 1, 2);

        cy.createGlobalBadge(2);

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]')
            .contains(2);
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="pagePreviewCardStat_Projects"] [data-cy="statNum"]')
            .contains(1);

        cy.get('[data-cy="badgeCard-globalBadge2"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]')
            .contains(0);
        cy.get('[data-cy="badgeCard-globalBadge2"] [data-cy="pagePreviewCardStat_Projects"] [data-cy="statNum"]')
            .contains(0);
    });

    it('skill filter is fully cleared after skill is selected', () => {
        cy.intercept('GET', '/supervisor/badges/badge1/skills/available**').as('loadBadgeSkills');

        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });


        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skillOne`,
            name: `This is One`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill11`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skillOneOne`,
            name: `This is One One`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skillTwo`,
            name: `This is Two`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill3`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skillThree`,
            name: `This is Three`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        cy.request('POST', '/supervisor/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.visit('/administrator/globalBadges/badge1');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@loadBadgeSkills');

        cy.get('[data-cy="skillsSelector"]').click();
        cy.get('[role="listbox"]').children().should('have.length', 4);
        cy.get('[data-cy="skillsSelector"]').click().type('one');
        cy.get('[role="listbox"]').children().eq(0).click();
        cy.get('[data-cy="badgeSkillsTable"').children().eq(0).children().find('td').eq(0).should('contain.text', 'proj1');
        cy.get('[data-cy="skillsSelector"]').should('not.contain.text', 'one');
        cy.get('[data-cy="skillsSelector"]').click();
        cy.get('[role="listbox"]').children().should('have.length', 3);
    });

    it('Project selector displays message if project count exceeds displayable max', () => {
        //proj/subj/skill1
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });
        //proj/subj/skill2
        cy.request('POST', '/app/projects/proj2', {
            projectId: 'proj2',
            name: 'proj2'
        });
        cy.request('POST', '/admin/projects/proj2/subjects/subj1', {
            projectId: 'proj2',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj2/subjects/subj1/skills/skill1`, {
            projectId: 'proj2',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        [3,4,5,6,7,8,9,10,11,12,13,14].forEach((num) => {
            cy.request('POST', `/app/projects/proj${num}`, {
                projectId: `proj${num}`,
                name: `proj${num}`
            });
        });

        const badgeId = 'a_badge';
        cy.request('PUT', `/supervisor/badges/${badgeId}`, {
            badgeId: badgeId,
            description: '',
            iconClass: 'fas fa-award',
            isEdit: false,
            name: 'A Badge',
            originalBadgeId: ''
        });

        cy.intercept('GET', '/supervisor/badges')
            .as('getBadges');
        cy.intercept('GET', '/supervisor/projects/proj2/levels')
            .as('getLevels');
        cy.intercept('GET', '/supervisor/badges/a_badge/projects/available*')
            .as('getAvailableLevels');

        cy.visit('/administrator/');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.clickNav('Global Badges');
        cy.wait('@getBadges');
        cy.get('[data-cy="manageBtn_a_badge"]')
            .click();

        cy.clickNav('Levels');
        cy.wait('@getAvailableLevels');

        cy.get('#project-selector')
            .click()
        cy.get('[data-cy="projectSelectorCountMsg"]').should('exist').should('contain.text', 'Showing 10 of 14 results.  Use search to narrow results.');

        cy.get('[data-pc-name="pcfilter"]').click().type('1');
        cy.wait('@getAvailableLevels');
        //not displayed if results less then availableCount
        cy.get('[data-cy="projectSelectorCountMsg"]').should('not.exist');
    });

    it('global badge details has go live button and can go live', () => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 100,
            numPerformToCompletion: 5,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            version: 0,
        });

        const expectedId = 'TestBadgeBadge';
        cy.intercept('GET', `/supervisor/badges`)
            .as('getGlobalBadges');
        cy.intercept('PUT', `/supervisor/badges/${expectedId}`)
            .as('postGlobalBadge');
        cy.intercept('GET', `/supervisor/badges/id/${expectedId}/exists`)
            .as('idExists');
        cy.intercept('POST', '/supervisor/badges/name/exists')
            .as('nameExists');
        cy.intercept('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR')
            .as('checkSupervisorRole');
        cy.intercept('GET', `/supervisor/badges/${expectedId}`)
            .as('getExpectedBadge');

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');
        cy.wait('@checkSupervisorRole');

        cy.get('[data-cy="btn_Global Badges"]').click();

        cy.get('[data-cy="name"]')
            .type('Test Badge');
        cy.wait('@nameExists');
        cy.clickSave();
        cy.wait('@postGlobalBadge');

        cy.clickNav('Global Badges');
        cy.contains('Manage')
            .click();
        cy.wait('@loadAvailableSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1')
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'proj1'
            }, {
                colIndex: 1,
                value: 'This is 1'
            }],
        ], 5);

        cy.visit('/administrator/globalBadges/TestBadgeBadge');
        cy.get('[data-cy="inception-button"]').contains('Level');

        cy.contains('Test Badge')
            .should('exist');
        cy.get('[data-cy=statPreformatted]')
            .contains('Disabled')
            .should('exist');
        cy.get('[data-cy=goLive]')
            .click();
        cy.contains('Please Confirm!')
            .should('exist');
        cy.contains('Yes, Go Live!')
            .click();

        cy.get('[data-cy=statPreformatted]')
            .contains('Live')
            .should('exist');
    });

    it('global badges do not have awards', () => {
        cy.createGlobalBadge(1);

        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');

        cy.get('[data-cy="badgeCard-globalBadge1"]')
            .contains('Global Badge 1');

        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="editBtn"]')
            .click();
        cy.get('[data-cy="awardName"]').should('not.exist');
    });

    it('adding a new level disables the selector', () => {
        const numProjects = 4;

        cy.intercept('GET', '/supervisor/badges/badge1/projects/available*')
            .as('getAvailableLevels');

        for (let i = 0; i < numProjects; i += 1) {
            cy.request('POST', `/app/projects/proj${i}`, {
                projectId: `proj${i}`,
                name: `proj${i}`,
            });
        }

        cy.request('POST', '/supervisor/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.visit('/administrator/globalBadges/badge1/');
        cy.clickNav('Levels');

        cy.wait('@getAvailableLevels');

        cy.get('#project-selector').click()
        cy.get('[data-cy="proj0_option"]').click();

        cy.get('#level-selector').click()
        cy.get('[data-pc-section="option"]').contains(5).click();

        cy.get('#project-selector').contains('proj0');
        cy.contains('Add').click();
        cy.get('#level-selector > [data-pc-name="select"]').should('have.class', 'p-disabled');

    });

    it('custom icons are loaded', function () {
        cy.logout()
        cy.loginAsRootUser();

        cy.uploadCustomIcon('valid_icon.png', '/supervisor/icons/upload')
        cy.createGlobalBadge(1);
        cy.createProject(1)
        cy.assignProjectToGlobalBadge(1, 1);
        cy.enableGlobalBadge(1, { iconClass: 'GLOBAL-validiconpng' });

        cy.intercept('/api/icons/customIconCss').as('customIcons')
        cy.visit('/administrator');
        cy.get('[data-cy="nav-Global Badges"]').click()
        cy.wait('@customIcons')
        cy.wait(1000)
        cy.get('[data-cy="badgeCard-globalBadge1"] .GLOBAL-validiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })

        cy.visit('/administrator/globalBadges/');
        cy.wait('@customIcons')
        cy.wait(1000)
        cy.get('[data-cy="badgeCard-globalBadge1"] .GLOBAL-validiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })

        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.wait('@customIcons')
        cy.get('[data-cy="btn_edit-badge"]').click()
        cy.wait(1000)
        cy.get('[data-cy="iconPicker"] .GLOBAL-validiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })
    });

    it('cannot delete a skill assigned to a global badge', function () {
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createGlobalBadge(1);
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.enableGlobalBadge(1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy=deleteSkillButton_skill1]').click()
        cy.get('[data-cy="skillBelongsToGlobalBadgeWarning"]').should('be.visible')
        cy.get('[data-cy="deleteSkillWarning"]').should('not.exist')
        cy.get('[data-cy="closeDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')
    })
});
