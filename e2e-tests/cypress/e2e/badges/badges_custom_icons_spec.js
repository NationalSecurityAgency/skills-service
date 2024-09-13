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

    it('custom badge icons are loaded for multiple projects', function () {
        cy.intercept(' /api/projects/proj1/customIconCss').as('proj1CustomIcons')
        cy.intercept(' /api/projects/proj2/customIconCss').as('proj2CustomIcons')
        cy.uploadCustomIcon('valid_icon.png', '/admin/projects/proj1/icons/upload')

        cy.enableBadge(1, 1, { iconClass: 'proj1-validiconpng' })

        cy.createProject(2)

        cy.createBadge(2, 1)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 1)
        cy.assignSkillToBadge(2, 1, 1)
        cy.uploadCustomIcon('anothervalid_icon.png', '/admin/projects/proj2/icons/upload')
        cy.enableBadge(2, 1, { iconClass: 'proj2-anothervalidiconpng' })

        cy.visit('/administrator/');
        cy.get('[data-cy="projCard_proj1_manageBtn"]').click()
        cy.wait('@proj1CustomIcons')
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="nav-Badges"]').click()
        cy.wait(1000)
        cy.get('[data-cy="badgeCard-badge1"] .proj1-validiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })

        cy.get('[data-cy="breadcrumb-Projects"]').click()
        cy.get('[data-cy="projCard_proj2_manageBtn"]').click()
        cy.wait('@proj2CustomIcons')
        cy.get('[data-cy="subjectCard-subj1"]')
        cy.get('[data-cy="nav-Badges"]').click()
        cy.wait(1000)
        cy.get('[data-cy="badgeCard-badge1"] .proj2-anothervalidiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })
    });

    it('custom badge icons are loaded when navigating to a project directly', function () {
        cy.intercept(' /api/projects/proj1/customIconCss').as('proj1CustomIcons')
        cy.intercept(' /api/projects/proj2/customIconCss').as('proj2CustomIcons')
        cy.uploadCustomIcon('valid_icon.png', '/admin/projects/proj1/icons/upload')

        cy.enableBadge(1, 1, { iconClass: 'proj1-validiconpng' })

        cy.createProject(2)

        cy.createBadge(2, 1)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 1)
        cy.assignSkillToBadge(2, 1, 1)
        cy.uploadCustomIcon('anothervalid_icon.png', '/admin/projects/proj2/icons/upload')
        cy.enableBadge(2, 1, { iconClass: 'proj2-anothervalidiconpng' })

        cy.visit('/administrator/projects/proj1');
        cy.wait('@proj1CustomIcons')
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="projectInsufficientPoints"]')
        cy.get('[data-cy="nav-Badges"]').click()
        cy.wait(1000)
        cy.get('[data-cy="badgeCard-badge1"] .proj1-validiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })

        cy.visit('/administrator/projects/proj1/badges');
        cy.wait('@proj1CustomIcons')
        cy.wait(1000)
        cy.get('[data-cy="badgeCard-badge1"] .proj1-validiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })

        cy.visit('/administrator/projects/proj1/badges/badge1');
        cy.wait('@proj1CustomIcons')
        cy.get('[data-cy="btn_edit-badge"]').click()
        cy.wait(1000)
        cy.get('[data-cy="iconPicker"] .proj1-validiconpng')
          .invoke('css', 'background-image')
          .then((bgImage) => {
              expect(bgImage).to.contain('data:image/png;base64')
          })


    });


});
