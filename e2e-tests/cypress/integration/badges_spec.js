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
describe('Badges Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        }).as('createProject');

        Cypress.Commands.add("gemStartNextMonth", () => {
            cy.get('[data-cy="startDatePicker"] header .next').first().click()
        });
        Cypress.Commands.add("gemStartPrevMonth", () => {
            cy.get('[data-cy="startDatePicker"] header .prev').first().click()
        });
        Cypress.Commands.add("gemEndNextMonth", () => {
            cy.get('[data-cy="endDatePicker"] header .next').first().click()
        });
        Cypress.Commands.add("gemEndPrevMonth", () => {
            cy.get('[data-cy="endDatePicker"] header .prev').first().click()
        });
        Cypress.Commands.add("gemStartSetDay", (dayNum) => {
            cy.get('[data-cy="startDatePicker"] .day').contains(dayNum).click()
        });
        Cypress.Commands.add("gemEndSetDay", (dayNum) => {
            cy.get('[data-cy="endDatePicker"] .day').contains(dayNum).click()
        });

        cy.server();
        cy.route('POST', '/admin/projects/proj1/badgeNameExists').as('nameExistsCheck');
        cy.route('GET', '/admin/projects/proj1/badges').as('loadBadges');

    });

    it('create badge with special chars', () => {
        const expectedId = 'LotsofspecialPcharsBadge';
        const providedName = "!L@o#t$s of %s^p&e*c(i)a_l++_|}{P/ c'ha'rs";

        cy.route('POST', `/admin/projects/proj1/badges/${expectedId}`).as('postNewBadge');
        cy.route('POST', '/admin/projects/proj1/badgeNameExists').as('nameExistsCheck');
        cy.route('GET', '/admin/projects/proj1/badges').as('loadBadges');

        cy.get('@createProject').should((response) => {
            expect(response.status).to.eql(200)
        });

        cy.visit('/projects/proj1/badges');
        cy.wait('@loadBadges');
        cy.clickButton('Badge');

        cy.get('#badgeName').type(providedName);

        cy.wait('@nameExistsCheck');

        cy.getIdField().should('have.value', expectedId);

        cy.clickSave();
        cy.wait('@postNewBadge');

        cy.contains('ID: Lotsofspecial')
    });

    it('create badge with enter key', () => {
        const expectedId = 'LotsofspecialPcharsBadge';
        const providedName = "!L@o#t$s of %s^p&e*c(i)a_l++_|}{P/ c'ha'rs";

        cy.route('POST', `/admin/projects/proj1/badges/${expectedId}`).as('postNewBadge');
        cy.route('POST', '/admin/projects/proj1/badgeNameExists').as('nameExistsCheck');
        cy.route('GET', '/admin/projects/proj1/badges').as('loadBadges');

        cy.get('@createProject').should((response) => {
            expect(response.status).to.eql(200)
        });

        cy.visit('/projects/proj1/badges');
        cy.wait('@loadBadges');
        cy.clickButton('Badge');

        cy.get('#badgeName').type(providedName);

        cy.wait('@nameExistsCheck');

        cy.getIdField().should('have.value', expectedId);

        cy.get('#badgeName').type('{enter}');
        cy.wait('@postNewBadge');

        cy.contains('ID: Lotsofspecial')
    });

    if('Close badge dialog', () => {
        cy.route('GET', '/admin/projects/proj1/badges').as('loadBadges');

        cy.get('@createProject').should((response) => {
            expect(response.status).to.eql(200)
        });

        cy.visit('/projects/proj1/badges');
        cy.wait('@loadBadges');
        cy.clickButton('Badge');
        cy.get('[data-cy=closeBadgeButton]').click();
        cy.get('[data-cy=closeBadgeButton]').should('not.be.visible');
    });

    it('cannot publish badge with no skills', () => {
        cy.route('POST', `/admin/projects/proj1/badges/anameBadge`).as('postNewBadge');
        cy.route('POST', '/admin/projects/proj1/badgeNameExists').as('nameExistsCheck');
        cy.route('GET', '/admin/projects/proj1/badges').as('loadBadges');

        cy.get('@createProject').should((response) => {
            expect(response.status).to.eql(200)
        });

        cy.visit('/projects/proj1/badges');
        cy.wait('@loadBadges');
        cy.clickButton('Badge');

        cy.get('#badgeName').type('a name');

        cy.wait('@nameExistsCheck');

        cy.getIdField().should('have.value', 'anameBadge');

        cy.clickSave();
        cy.wait('@postNewBadge');
        cy.get('[data-cy=badgeStatus]').contains('Status: Disabled').should('exist');
        cy.get('[data-cy=goLive]').click();
        cy.contains('This Badge has no assigned Skills. A Badge cannot be published without at least one assigned Skill.').should('be.visible');
        cy.get('[data-cy=badgeStatus]').contains('Status: Disabled').should('exist');
    });

    it('inactive badge displays warning', () => {
        const expectedId = 'InactiveBadge';
        const providedName = 'Inactive';
        cy.server();
        cy.route('GET', '/app/userInfo').as('getUserInfo');
        cy.route('GET', '/app/userInfo/hasRole/ROLE_SUPERVISOR').as('hasSupervisor');
        cy.route('POST', `/admin/projects/proj1/badges/${expectedId}`).as('postNewBadge');
        cy.route('POST', '/admin/projects/proj1/badgeNameExists').as('nameExistsCheck');
        cy.route('GET', '/admin/projects/proj1/badges').as('loadBadges');

        cy.get('@createProject').should((response) => {
            expect(response.status).to.eql(200)
        });

        cy.visit('/projects/proj1/badges');

        cy.wait('@loadBadges');
        cy.wait('@getUserInfo');
        cy.wait('@hasSupervisor');
        cy.clickButton('Badge');

        cy.get('#badgeName').type(providedName);

        cy.wait('@nameExistsCheck');

        cy.clickSave();
        cy.wait('@postNewBadge');

        cy.get('div.card-body i.fa-exclamation-circle').should('be.visible');
    });

    it('name causes id to fail validation', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badgeExist', {
            projectId: 'proj1',
            name: "Badge Exist",
            badgeId: 'badgeExist'
        })

        cy.visit('/projects/proj1/badges');
        cy.clickButton('Badge');
        cy.contains('New Badge');

        // name causes id to be too long
        const msg = 'Badge ID cannot exceed 50 characters';
        const validNameButInvalidId = Array(46).fill('a').join('');
        cy.get('[data-cy=badgeName]').click();
        cy.get('[data-cy=badgeName]').fill( validNameButInvalidId);
        cy.get('[data-cy=idError]').should('be.visible');
        cy.get('[data-cy=saveBadgeButton]').should('be.disabled');
        cy.get('[data-cy=badgeName]').type('{backspace}');
        cy.get('[data-cy=idError]').should('not.be.visible');
        cy.get('[data-cy=saveBadgeButton]').should('be.enabled');
    });

    it('badge validation', () => {
        // create existing badge
        cy.request('POST', '/admin/projects/proj1/badges/badgeExist', {
            projectId: 'proj1',
            name: "Badge Exist",
            badgeId: 'badgeExist'
        })

        cy.visit('/projects/proj1/badges');
        cy.clickButton('Badge');
        cy.contains('New Badge');

        const overallFormValidationMsg = 'Form did NOT pass validation, please fix and try to Save again';

        // name is too short
        let msg = 'Badge Name cannot be less than 3 characters';
        cy.get('#badgeName').type('Te');
        cy.get('[data-cy=badgeNameError]').contains(msg).should('be.visible');
        cy.get('[data-cy=saveBadgeButton]').should('be.disabled');
        cy.get('#badgeName').type('Tes');
        cy.get('[data-cy=badgeNameError]').should('not.be.visible');

        // name too long
        msg = 'Badge Name cannot exceed 50 characters';
        cy.contains('Enable').click();
        cy.getIdField().clear().type("badgeId");
        const invalidName = Array(50).fill('a').join('');
        cy.get('#badgeName').clear();
        cy.get('#badgeName').type(invalidName);
        cy.get('#badgeName').type('b');
        cy.get('[data-cy=badgeNameError]').contains(msg).should('be.visible');
        cy.get('[data-cy=saveBadgeButton]').should('be.disabled');
        cy.get('#badgeName').type('{backspace}');
        cy.get('[data-cy=badgeNameError]').should('not.be.visible');

        // id too short
        msg = 'Badge ID cannot be less than 3 characters';
        cy.getIdField().clear().type("aa");
        cy.get('[data-cy=idError]').contains(msg).should('be.visible');
        cy.get('[data-cy=saveBadgeButton]').should('be.disabled');
        cy.getIdField().type("a");
        cy.get('[data-cy=idError]').should('not.be.visible');

        // id too long
        msg = 'Badge ID cannot exceed 50 characters';
        const invalidId = Array(51).fill('a').join('');
        cy.getIdField().clear()
        cy.getIdField().click().type(invalidId);
        cy.get('[data-cy=idError]').contains(msg).should('be.visible');
        cy.getIdField().type('{backspace}');
        cy.get('[data-cy=idError]').should('not.be.visible');

        // id must not have special chars
        msg = 'Badge ID may only contain alpha-numeric characters';
        cy.getIdField().clear().type('With$Special');
        cy.get('[data-cy=idError]').contains(msg).should('be.visible');
        cy.getIdField().clear().type('GoodToGo');
        cy.get('[data-cy=idError]').should('not.be.visible');

        cy.getIdField().clear().type('SomeId');
        // !L@o#t$s of %s^p&e*c(i)a_l++_|}{P/ c'ha'rs
        let specialChars = [' ', '_', '!', '@', '#', '%', '^', '&', '*', '(', ')', '-', '+', '='];
        specialChars.forEach((element) => {
            cy.getIdField().type(element);
            cy.get('[data-cy=idError]').contains(msg).should('be.visible');
            cy.getIdField().type('{backspace}');
            cy.get('[data-cy=idError]').should('not.be.visible');
        })

        // badge name must not be already taken
        msg = 'The value for Badge Name is already taken';
        cy.get('#badgeName').clear().type('Badge Exist');
        cy.get('[data-cy=badgeNameError]').contains(msg).should('be.visible');
        cy.get('#badgeName').type('1');
        cy.get('[data-cy=badgeNameError]').should('not.be.visible');

        // badge id must not already exist
        msg = 'The value for Badge ID is already taken';
        cy.getIdField().clear().type('badgeExist');
        cy.get('[data-cy=idError]').contains(msg).should('be.visible');
        cy.getIdField().type('1');
        cy.get('[data-cy=idError]').should('not.be.visible');

        // max description
        msg='Badge Description cannot exceed 2000 characters';
        const invalidDescription = Array(2000).fill('a').join('');
        // it takes way too long using .type method
        cy.get('#markdown-editor textarea').fill(invalidDescription);
        cy.get('#markdown-editor').type('a');
        cy.get('[data-cy=badgeDescriptionError]').contains(msg).should('be.visible');
        cy.get('#markdown-editor').type('{backspace}');
        cy.get('[data-cy=badgeDescriptionError]').should('not.be.visible')

        // finally let's save
        cy.clickSave();
        cy.wait('@loadBadges');
        cy.contains('Badge Exist1');
    });

    it('gem start and end time validation', () => {
        cy.visit('/projects/proj1/badges');
        cy.clickButton('Badge');
        cy.contains('New Badge');
        cy.get('[data-cy="gemEditContainer"]').click()
        cy.contains('Enable Gem Feature').click();
        cy.contains('Start Date');

        cy.get('#badgeName').type('Test Badge');

        // dates should not overlap
        let msg = 'Start Date must come before End Date';
        cy.gemStartNextMonth();
        cy.gemStartSetDay(1);
        cy.gemEndNextMonth();
        cy.gemEndSetDay(1);
        cy.get('[data-cy=endDateError]').contains(msg).should('be.visible');
        cy.gemEndSetDay(2);
        cy.get('[data-cy=endDateError]').should('not.be.visible');

        // start date should be before end date
        msg = 'Start Date must come before End Date';
        cy.gemStartSetDay(3);
        cy.get('[data-cy=startDateError]').contains(msg).should('be.visible');
        cy.gemEndSetDay(4);
        cy.get('[data-cy=startDateError]').should('not.be.visible');

        // dates should not be in the past
        msg = 'End Date cannot be in the past';
        cy.gemStartPrevMonth();
        cy.gemStartPrevMonth();
        cy.gemStartSetDay(1);
        cy.gemEndPrevMonth();
        cy.gemEndPrevMonth();
        cy.gemEndSetDay(2);
        cy.get('[data-cy=endDateError]').contains(msg).should('be.visible');

        // should not save if there are validation errors
        cy.get('[data-cy=saveBadgeButton]').should('be.disabled');

        // fix the errors and save
        cy.gemStartNextMonth();
        cy.gemStartNextMonth();
        cy.gemEndNextMonth();
        cy.gemEndNextMonth();
        cy.gemStartSetDay(1);
        cy.gemEndSetDay(2);
        cy.get('[data-cy=endDateError]').should('not.be.visible');
        cy.get('[data-cy=saveBadgeButton]').should('be.enabled');

        cy.clickSave();
        cy.wait('@loadBadges');
        cy.contains('Test Badge');
    });

    it('Badge is disabled when created, can only be enabled once', () => {

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.visit('/projects/proj1/badges');
        cy.clickButton('Badge');
        cy.contains('New Badge');
        cy.get('#badgeName').type('Test Badge');
        cy.clickSave();
        cy.wait('@loadBadges');

        cy.get('[data-cy=manageBadge]').click();
        cy.get('#skills-selector').click();
        cy.get('#skills-selector input[type=text]').type('{enter}');
        cy.contains('.router-link-active', 'Badges').click();

        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]').contains('Status: Disabled').should('exist');
        cy.get('[data-cy=goLive]').click();
        cy.contains('Please Confirm!').should('exist');
        cy.contains('Yes, Go Live!').click();

        cy.wait('@loadBadges');
        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]').contains('Status: Live').should('exist');
        cy.get('[data-cy=goLive]').should('not.exist');
    });

    it('Badge is disabled when created, canceling confirm dialog leaves badge disabled', () => {

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.visit('/projects/proj1/badges');
        cy.clickButton('Badge');
        cy.contains('New Badge');
        cy.get('#badgeName').type('Test Badge');
        cy.clickSave();
        cy.wait('@loadBadges');

        cy.get('[data-cy=manageBadge]').click();
        cy.get('#skills-selector').click();
        cy.get('#skills-selector input[type=text]').type('{enter}');
        cy.contains('.router-link-active', 'Badges').click();

        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]').contains('Status: Disabled').should('exist');
        cy.get('[data-cy=goLive]').click();
        cy.contains('Please Confirm!').should('exist');
        cy.contains('Cancel').click();

        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]').contains('Status: Disabled').should('exist');
        cy.get('[data-cy=badgeStatus]').contains('Status: Live').should('not.exist');
        cy.get('[data-cy=goLive]').should('exist');
    });

    it('Can add Skill requirements to disabled badge', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.visit('/projects/proj1/badges');
        cy.clickButton('Badge');
        cy.contains('New Badge');
        cy.get('#badgeName').type('Test Badge');
        cy.clickSave();
        cy.wait('@loadBadges');
        cy.get('[data-cy=manageBadge]').click();
        cy.get('#skills-selector').click();
        cy.get('#skills-selector input[type=text]').type('{enter}');
        cy.contains('.router-link-active', 'Badges').click();
        cy.contains('Test Badge').should('exist');
        cy.get('[data-cy=badgeStatus]').contains('Status: Disabled').should('exist');
        cy.get('[data-cy=goLive]').click();
        cy.contains('Please Confirm!').should('exist');
        cy.contains('Yes, Go Live!').click();
        cy.wait('@loadBadges');
        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]').contains('Status: Live').should('exist');
    });

    it('removing last skill from enabled badge does not disable badge', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.visit('/projects/proj1/badges');
        cy.clickButton('Badge');
        cy.contains('New Badge');
        cy.get('#badgeName').type('Test Badge');
        cy.clickSave();
        cy.wait('@loadBadges');
        cy.get('[data-cy=manageBadge]').click();
        cy.get('#skills-selector').click();
        cy.get('#skills-selector input[type=text]').type('{enter}');
        cy.contains('.router-link-active', 'Badges').click();
        cy.contains('Test Badge').should('exist');
        cy.get('[data-cy=badgeStatus]').contains('Status: Disabled').should('exist');
        cy.get('[data-cy=goLive]').click();
        cy.contains('Please Confirm!').should('exist');
        cy.contains('Yes, Go Live!').click();
        cy.wait('@loadBadges');
        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]').contains('Status: Live').should('exist');
        cy.get('[data-cy=manageBadge]').click();
        cy.get('[data-cy=deleteSkill]').click();
        cy.contains('YES, Delete It!').click();
        cy.contains('.router-link-active', 'Badges').click();
        cy.contains('Test Badge').should('exist');
        cy.get('[data-cy=badgeStatus]').contains('Status: Live').should('exist');
    });

    it('badge user details does not break breadcrumb bar', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1',
            "iconClass":"fas fa-ghost",
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        });

        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1')
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: 'someuser', timestamp: new Date().getTime()})


        cy.visit('/projects/proj1/badges/badge1');
        cy.get('[data-cy=nav-Users]').click();
        cy.contains('Details').click();
        cy.get('[data-cy=breadcrumb-badge1]').should('be.visible');
        cy.get('[data-cy=breadcrumb-Users]').should('be.visible');
    })

});
