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

    const tableSelector = '[data-cy="badgeSkillsTable"]';
    const makdownDivSelector = '#markdown-editor div.toastui-editor-main.toastui-editor-ww-mode > div > div.toastui-editor-ww-container > div > div'
    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        })
            .as('createProject');

        Cypress.Commands.add('gemNextMonth', () => {
            cy.get('[data-cy="gemDates"] [data-pc-section="nextbutton"]')
                .first()
                .click();
            cy.wait(150);
        });
        Cypress.Commands.add('gemPrevMonth', () => {
            cy.get('[data-cy="gemDates"] [data-pc-section="previousbutton"]')
                .first()
                .click();
            cy.wait(150);
        });
        Cypress.Commands.add('gemSetDay', (dayNum) => {
            cy.get(`[data-cy="gemDates"] [data-pc-section="table"] [aria-label="${dayNum}"]`)
              .not('[data-p-other-month="true"]')
                .click();
        });

        cy.intercept('POST', '/admin/projects/proj1/badgeNameExists')
            .as('nameExistsCheck');
        cy.intercept('GET', '/admin/projects/proj1/badges')
            .as('loadBadges');
        cy.intercept('GET', '/admin/projects/proj1/skills?*')
          .as('loadSkills');
    });

    it('drag-and-drop sort management', () => {
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);
        cy.createBadge(1, 3);
        cy.createBadge(1, 4);
        cy.createBadge(1, 5);

        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');

        const badge1Card = '[data-cy="badgeCard-badge1"] [data-cy="sortControlHandle"]';
        const badge2Card = '[data-cy="badgeCard-badge2"] [data-cy="sortControlHandle"]';
        const badge4Card = '[data-cy="badgeCard-badge4"] [data-cy="sortControlHandle"]';
        const badge5Card = '[data-cy="badgeCard-badge5"] [data-cy="sortControlHandle"]';

        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 1', 'Badge 2', 'Badge 3', 'Badge 4', 'Badge 5']);
        cy.get(badge1Card)
            .dragAndDrop(badge4Card);
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 2', 'Badge 3', 'Badge 4', 'Badge 1', 'Badge 5']);

        // refresh to make sure it was saved
        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 2', 'Badge 3', 'Badge 4', 'Badge 1', 'Badge 5']);

        cy.get(badge5Card)
            .dragAndDrop(badge2Card);
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 5', 'Badge 2', 'Badge 3', 'Badge 4', 'Badge 1']);

        cy.get(badge2Card)
            .dragAndDrop(badge1Card);
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 5', 'Badge 3', 'Badge 4', 'Badge 1', 'Badge 2']);

        // refresh to make sure it was saved
        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 5', 'Badge 3', 'Badge 4', 'Badge 1', 'Badge 2']);
    });

    it('no drag-and-drag sort controls when there is only 1 badge', () => {
        cy.createBadge(1, 1);

        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="badgeCard-badge1"]');
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="sortControlHandle"]')
            .should('not.exist');

        cy.createBadge(1, 2);
        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="badgeCard-badge1"]');
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="sortControlHandle"]');
    });

    it('drag-and-drag sort should spinner while backend operation is happening', () => {
        cy.intercept('/admin/projects/proj1/badges/badge1', (req) => {
            req.reply((res) => {
                res.send({ delay: 6000 });
            });
        })
            .as('badge1Async');

        cy.createBadge(1, 1);
        cy.createBadge(1, 2);

        const badge1Card = '[data-cy="badgeCard-badge1"] [data-cy="sortControlHandle"]';
        const badge2Card = '[data-cy="badgeCard-badge2"] [data-cy="sortControlHandle"]';

        cy.visit('/administrator/projects/proj1/badges');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 1', 'Badge 2']);
        cy.get(badge1Card)
            .dragAndDrop(badge2Card);

        // overlay over both cards but loading message only on badge 1
        cy.get('[data-cy="badge1_overlayShown"] [data-cy="updatingSortMsg"]')
            .contains('Updating sort order');
        cy.get('[data-cy="badge2_overlayShown"]');
        cy.get('[data-cy="badge2_overlayShown"] [data-cy="updatingSortMsg"]')
            .should('not.exist');
        cy.wait('@badge1Async');
        cy.get('[data-cy="badge1_overlayShown"]')
            .should('not.exist');
        cy.get('[data-cy="badge2_overlayShown"]')
            .should('not.exist');
    });

});
