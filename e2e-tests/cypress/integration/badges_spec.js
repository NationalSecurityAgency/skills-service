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
        })

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
        cy.contains(msg);
        cy.clickSave();
        cy.contains(overallFormValidationMsg);
        cy.get('#badgeName').type('Tes');
        cy.contains(msg).should('not.exist');
        cy.contains(overallFormValidationMsg).should('not.exist');

        // name too long
        msg = 'Badge Name cannot exceed 50 characters';
        cy.contains('Enable').click();
        cy.getIdField().clear().type("badgeId");
        const invalidName = Array(51).fill('a').join('');
        cy.get('#badgeName').clear().type(invalidName);
        cy.contains(msg);
        cy.clickSave();
        cy.contains(overallFormValidationMsg);
        cy.get('#badgeName').type('{backspace}');
        cy.contains(msg).should('not.exist');
        cy.contains(overallFormValidationMsg).should('not.exist');

        // id too short
        msg = 'Badge ID cannot be less than 3 characters';
        cy.getIdField().clear().type("aa");
        cy.contains(msg);
        cy.clickSave();
        cy.contains(overallFormValidationMsg);
        cy.getIdField().type("a");
        cy.contains(msg).should('not.exist');
        cy.contains(overallFormValidationMsg).should('not.exist');

        // id too long
        msg = 'Badge ID cannot exceed 50 characters';
        const invalidId = Array(51).fill('a').join('');
        cy.getIdField().clear().type(invalidId);
        cy.contains(msg);
        cy.getIdField().type('{backspace}');
        cy.contains(msg).should('not.exist');

        // id must not have special chars
        msg = 'The Badge ID field may only contain alpha-numeric characters';
        cy.getIdField().clear().type('With$Special');
        cy.contains(msg);
        cy.getIdField().clear().type('GoodToGo');
        cy.contains(msg).should('not.exist');

        cy.getIdField().clear().type('SomeId');
        // !L@o#t$s of %s^p&e*c(i)a_l++_|}{P/ c'ha'rs
        let specialChars = [' ', '_', '!', '@', '#', '%', '^', '&', '*', '(', ')', '-', '+', '='];
        specialChars.forEach((element) => {
            cy.getIdField().type(element);
            cy.contains(msg);
            cy.getIdField().type('{backspace}');
            cy.contains(msg).should('not.exist');
        })

        // badge name must not be already taken
        msg = 'The value for Badge Name is already taken';
        cy.get('#badgeName').clear().type('Badge Exist');
        cy.contains(msg);
        cy.get('#badgeName').type('1');
        cy.contains(msg).should('not.exist');

        // badge id must not already exist
        msg = 'The value for Badge ID is already taken';
        cy.getIdField().clear().type('badgeExist');
        cy.contains(msg);
        cy.getIdField().type('1');
        cy.contains(msg).should('not.exist');

        // max description
        msg='Badge Description cannot exceed 2000 characters';
        const invalidDescription = Array(2000).fill('a').join('');
        // it takes way too long using .type method
        cy.get('#markdown-editor textarea').invoke('val', invalidDescription).trigger('change');
        cy.get('#markdown-editor').type('a');
        cy.contains(msg);
        cy.get('#markdown-editor').type('{backspace}');
        cy.contains(msg).should('not.exist')

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
        cy.contains(msg);
        cy.gemEndSetDay(2);
        cy.contains(msg).should('not.exist');

        // start date should be before end date
        msg = 'Start Date must come before End Date';
        cy.gemStartSetDay(3);
        cy.contains(msg)
        cy.gemEndSetDay(4);
        cy.contains(msg).should('not.exist');

        // dates should not be in the past
        const overallFormValidationMsg = 'Form did NOT pass validation, please fix and try to Save again';
        msg = 'End Date cannot be in the past';
        cy.gemStartPrevMonth();
        cy.gemStartPrevMonth();
        cy.gemStartSetDay(1);
        cy.gemEndPrevMonth();
        cy.gemEndPrevMonth();
        cy.gemEndSetDay(2);
        cy.contains(msg);

        // should not save if there are validation errors
        cy.clickSave();
        cy.contains(overallFormValidationMsg);

        // fix the errors and save
        cy.gemStartNextMonth();
        cy.gemStartNextMonth();
        cy.gemEndNextMonth();
        cy.gemEndNextMonth();
        cy.gemStartSetDay(1);
        cy.gemEndSetDay(2);
        cy.contains(msg).should('not.exist');
        cy.contains(overallFormValidationMsg).should('not.exist');

        cy.clickSave();
        cy.wait('@loadBadges');
        cy.contains('Test Badge');
    }) ;


})
