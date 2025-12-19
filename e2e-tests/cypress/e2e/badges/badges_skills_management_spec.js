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

    it('can not remove last skill from enabled badge', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]').click();
        cy.contains('New Badge');
        cy.get('[data-cy="name"]')
          .type('Test Badge');
        cy.clickSave();
        cy.wait('@loadBadges');
        cy.get('[data-cy=manageBtn_TestBadgeBadge]')
          .click();
        cy.wait(500);
        cy.wait('@loadSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 1'
            }, {
                colIndex: 1,
                value: 'skill1'
            }],
        ], 5, false, null, false);

        cy.contains('[data-cy="breadcrumbItemValue"]', 'Badges')
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
        cy.wait('@loadBadges');
        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]')
          .contains('Status: Live')
          .should('exist');
        cy.get('[data-cy=manageBtn_TestBadgeBadge]')
          .click();
        cy.wait(500);
        cy.get('[data-cy=deleteSkill_skill1]').should('be.disabled')

    });

    it('remove skill after navigating to the link directly', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        const numSkills = 4;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${i}`,
                name: `Skill ${i}`,
                pointIncrement: '50',
                numPerformToCompletion: '5'
            });

            cy.request('POST', `/admin/projects/proj1/badge/badge1/skills/skill${i}`);
        }

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get(`${tableSelector} th`)
          .contains('Skill ID')
          .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill0'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill3'
            }],
        ], 5, false, null, false);

        cy.get('[data-cy="deleteSkill_skill2"]')
          .click();
        cy.contains('YES, Delete It')
          .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill0'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'skill3'
            }],
        ], 5, false, null, false);
    });

    it('skills table sorting', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        const numSkills = 7;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${i}`,
                name: `Skill ${10 - i}`,
                pointIncrement: '50',
                numPerformToCompletion: (i + 1)
            });

            cy.request('POST', `/admin/projects/proj1/badge/badge1/skills/skill${i}`);
        }

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get(`${tableSelector} th`)
          .contains('Skill ID')
          .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 10'
            }, {
                colIndex: 1,
                value: 'skill0'
            }, {
                colIndex: 4,
                value: '50'
            }],
            [{
                colIndex: 0,
                value: 'Skill 9'
            }, {
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 4,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'Skill 8'
            }, {
                colIndex: 1,
                value: 'skill2'
            }, {
                colIndex: 4,
                value: '150'
            }],
            [{
                colIndex: 0,
                value: 'Skill 7'
            }, {
                colIndex: 1,
                value: 'skill3'
            }, {
                colIndex: 4,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Skill 6'
            }, {
                colIndex: 1,
                value: 'skill4'
            }, {
                colIndex: 4,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 'Skill 5'
            }, {
                colIndex: 1,
                value: 'skill5'
            }, {
                colIndex: 4,
                value: '300'
            }],
            [{
                colIndex: 0,
                value: 'Skill 4'
            }, {
                colIndex: 1,
                value: 'skill6'
            }, {
                colIndex: 4,
                value: '350'
            }],
        ], 10);

        cy.get(`${tableSelector} th`)
          .contains('Skill ID')
          .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 4'
            }, {
                colIndex: 1,
                value: 'skill6'
            }, {
                colIndex: 4,
                value: '350'
            }],
            [{
                colIndex: 0,
                value: 'Skill 5'
            }, {
                colIndex: 1,
                value: 'skill5'
            }, {
                colIndex: 4,
                value: '300'
            }],
            [{
                colIndex: 0,
                value: 'Skill 6'
            }, {
                colIndex: 1,
                value: 'skill4'
            }, {
                colIndex: 4,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 'Skill 7'
            }, {
                colIndex: 1,
                value: 'skill3'
            }, {
                colIndex: 4,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Skill 8'
            }, {
                colIndex: 1,
                value: 'skill2'
            }, {
                colIndex: 4,
                value: '150'
            }],
            [{
                colIndex: 0,
                value: 'Skill 9'
            }, {
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 4,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'Skill 10'
            }, {
                colIndex: 1,
                value: 'skill0'
            }, {
                colIndex: 4,
                value: '50'
            }],
        ], 10);

        cy.get(`${tableSelector} th`)
          .contains('Skill Name')
          .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 4'
            }, {
                colIndex: 1,
                value: 'skill6'
            }, {
                colIndex: 4,
                value: '350'
            }],
            [{
                colIndex: 0,
                value: 'Skill 5'
            }, {
                colIndex: 1,
                value: 'skill5'
            }, {
                colIndex: 4,
                value: '300'
            }],
            [{
                colIndex: 0,
                value: 'Skill 6'
            }, {
                colIndex: 1,
                value: 'skill4'
            }, {
                colIndex: 4,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 'Skill 7'
            }, {
                colIndex: 1,
                value: 'skill3'
            }, {
                colIndex: 4,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Skill 8'
            }, {
                colIndex: 1,
                value: 'skill2'
            }, {
                colIndex: 4,
                value: '150'
            }],
            [{
                colIndex: 0,
                value: 'Skill 9'
            }, {
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 4,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'Skill 10'
            }, {
                colIndex: 1,
                value: 'skill0'
            }, {
                colIndex: 4,
                value: '50'
            }],
        ], 10);

        cy.get(`${tableSelector} th`)
          .contains('Skill Name')
          .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 10'
            }, {
                colIndex: 1,
                value: 'skill0'
            }, {
                colIndex: 4,
                value: '50'
            }],
            [{
                colIndex: 0,
                value: 'Skill 9'
            }, {
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 4,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'Skill 8'
            }, {
                colIndex: 1,
                value: 'skill2'
            }, {
                colIndex: 4,
                value: '150'
            }],
            [{
                colIndex: 0,
                value: 'Skill 7'
            }, {
                colIndex: 1,
                value: 'skill3'
            }, {
                colIndex: 4,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Skill 6'
            }, {
                colIndex: 1,
                value: 'skill4'
            }, {
                colIndex: 4,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 'Skill 5'
            }, {
                colIndex: 1,
                value: 'skill5'
            }, {
                colIndex: 4,
                value: '300'
            }],
            [{
                colIndex: 0,
                value: 'Skill 4'
            }, {
                colIndex: 1,
                value: 'skill6'
            }, {
                colIndex: 4,
                value: '350'
            }],
        ], 10);

        cy.get(`${tableSelector} th`)
          .contains('Total Points')
          .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 10'
            }, {
                colIndex: 1,
                value: 'skill0'
            }, {
                colIndex: 4,
                value: '50'
            }],
            [{
                colIndex: 0,
                value: 'Skill 9'
            }, {
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 4,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'Skill 8'
            }, {
                colIndex: 1,
                value: 'skill2'
            }, {
                colIndex: 4,
                value: '150'
            }],
            [{
                colIndex: 0,
                value: 'Skill 7'
            }, {
                colIndex: 1,
                value: 'skill3'
            }, {
                colIndex: 4,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Skill 6'
            }, {
                colIndex: 1,
                value: 'skill4'
            }, {
                colIndex: 4,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 'Skill 5'
            }, {
                colIndex: 1,
                value: 'skill5'
            }, {
                colIndex: 4,
                value: '300'
            }],
            [{
                colIndex: 0,
                value: 'Skill 4'
            }, {
                colIndex: 1,
                value: 'skill6'
            }, {
                colIndex: 4,
                value: '350'
            }],
        ], 10);

        cy.get(`${tableSelector} th`)
          .contains('Total Points')
          .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Skill 4'
            }, {
                colIndex: 1,
                value: 'skill6'
            }, {
                colIndex: 4,
                value: '350'
            }],
            [{
                colIndex: 0,
                value: 'Skill 5'
            }, {
                colIndex: 1,
                value: 'skill5'
            }, {
                colIndex: 4,
                value: '300'
            }],
            [{
                colIndex: 0,
                value: 'Skill 6'
            }, {
                colIndex: 1,
                value: 'skill4'
            }, {
                colIndex: 4,
                value: '250'
            }],
            [{
                colIndex: 0,
                value: 'Skill 7'
            }, {
                colIndex: 1,
                value: 'skill3'
            }, {
                colIndex: 4,
                value: '200'
            }],
            [{
                colIndex: 0,
                value: 'Skill 8'
            }, {
                colIndex: 1,
                value: 'skill2'
            }, {
                colIndex: 4,
                value: '150'
            }],
            [{
                colIndex: 0,
                value: 'Skill 9'
            }, {
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 4,
                value: '100'
            }],
            [{
                colIndex: 0,
                value: 'Skill 10'
            }, {
                colIndex: 1,
                value: 'skill0'
            }, {
                colIndex: 4,
                value: '50'
            }],
        ], 10);
    });

    it('rows per page control is enabled once # of skills is greater than page size', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        const numSkills = 6;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${i}`,
                name: `Skill ${i}`,
                pointIncrement: '50',
                numPerformToCompletion: '5'
            });

            if (i < numSkills-1) {
                cy.request('POST', `/admin/projects/proj1/badge/badge1/skills/skill${i}`);
            }
        }

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get(`${tableSelector} th`)
          .contains('Skill ID')
          .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill0'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill4'
            }],
        ], 10, false, null, false);
        cy.get('[data-pc-name="pcrowperpagedropdown"]').should('not.exist');

        // add one more skill to the badge to make 6 skills total
        // cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill5');
        cy.get('[data-cy="skillsSelector"]')
          .click();
        cy.get('[data-pc-section="option"]').first().click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill0'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill4'
            }],
            [{
                colIndex: 1,
                value: 'skill5'
            }],
        ], 10, true, null, false);
        cy.get('[data-pc-name="pcrowperpagedropdown"]').should('exist');

        // now delete a skill to go back to 5 skills total
        cy.get('[data-cy="deleteSkill_skill2"]')
          .click();
        cy.contains('YES, Delete It')
          .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill0'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill4'
            }],
            [{
                colIndex: 1,
                value: 'skill5'
            }],
        ], 10, false, null, false);
        cy.get('[data-pc-name="pcrowperpagedropdown"]').should('not.exist');
    });

    it('Can add Skill requirements to disabled badge', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });

        cy.visit('/administrator/projects/proj1/badges');
        // // // cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="btn_Badges"]').click();
        cy.contains('New Badge');
        cy.get('[data-cy="name"]')
          .type('Test Badge');
        cy.clickSave();
        cy.wait('@loadBadges');
        cy.get('[data-cy=manageBtn_TestBadgeBadge]')
          .click();
        cy.wait(500);
        cy.wait('@loadSkills');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.contains('[data-cy="breadcrumbItemValue"]', 'Badges')
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
        cy.wait('@loadBadges');
        cy.contains('Test Badge');
        cy.get('[data-cy=badgeStatus]')
          .contains('Status: Live')
          .should('exist');
    });

    it('skills table has manage button', () => {
        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1'
        });

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        const numSkills = 7;
        for (let i = 0; i < numSkills; i += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${i}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${i}`,
                name: `Skill ${10 - i}`,
                pointIncrement: '50',
                numPerformToCompletion: (i + 1)
            });

            cy.request('POST', `/admin/projects/proj1/badge/badge1/skills/skill${i}`);
        }

        cy.visit('/administrator/projects/proj1/badges/badge1');
        // // cy.get('[data-cy="inception-button"]').contains('Level');
        for (let i = 0; i < 5; i +=1) {
            cy.get(`[data-cy="manage_skill${i}"]`).should('exist')
            cy.get(`[data-cy="deleteSkill_skill${i}"]`).should('exist')
        }

    });

    it('change sort order using keyboard', () => {
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);
        cy.createBadge(1, 3);

        cy.visit('/administrator/projects/proj1/badges');
        cy.validateElementsOrder('[data-cy="badgeCard"] [data-cy="titleLink"]', ['Badge 1', 'Badge 2', 'Badge 3']);

        // move down
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="titleLink"]')
          .tab()
          .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="badgeCard"] [data-cy="titleLink"]', ['Badge 2', 'Badge 1', 'Badge 3']);
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="sortControlHandle"]')
          .should('have.focus');

        // move down
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="titleLink"]')
          .tab()
          .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="badgeCard"] [data-cy="titleLink"]', ['Badge 2', 'Badge 3', 'Badge 1']);
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="sortControlHandle"]')
          .should('have.focus');

        // move down - already the last item
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="titleLink"]')
          .tab()
          .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="badgeCard"] [data-cy="titleLink"]', ['Badge 2', 'Badge 3', 'Badge 1']);
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="sortControlHandle"]')
          .should('have.focus');

        // refresh and validate
        cy.visit('/administrator/projects/proj1/badges');
        cy.validateElementsOrder('[data-cy="badgeCard"] [data-cy="titleLink"]', ['Badge 2', 'Badge 3', 'Badge 1']);
        cy.get('[data-cy="badgeCard-badge1"] [data-cy="sortControlHandle"]')
          .should('not.have.focus');

        // move up
        cy.get('[data-cy="badgeCard-badge3"] [data-cy="titleLink"]')
          .tab()
          .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="badgeCard"] [data-cy="titleLink"]', ['Badge 3', 'Badge 2', 'Badge 1']);
        cy.get('[data-cy="badgeCard-badge3"] [data-cy="sortControlHandle"]')
          .should('have.focus');

        // move up - already first
        cy.get('[data-cy="badgeCard-badge3"] [data-cy="titleLink"]')
          .tab()
          .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="badgeCard"] [data-cy="titleLink"]', ['Badge 3', 'Badge 2', 'Badge 1']);
        cy.get('[data-cy="badgeCard-badge3"] [data-cy="sortControlHandle"]')
          .should('have.focus');
    });



    it('typing into skill search input and then erasing the text should not cause an error', () => {
        cy.visit('/administrator/projects/proj1/badges');
        cy.get('[data-cy="btn_Badges"]').click();
        cy.contains('New Badge');
        cy.get('[data-cy="name"]')
          .type('Test Badge');
        cy.clickSave();
        cy.wait('@loadBadges');
        cy.get('[data-cy=manageBtn_TestBadgeBadge]')
          .click();
        cy.wait(500);
        cy.wait('@loadSkills');
        cy.get('[data-cy="skillsSelector"]').type('a{backspace}');
    })

    it('adding a skill returns focus to the select input', () => {
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)
        cy.createSkill(1, 1, 4)
        cy.createBadge(1, 1)
        cy.visit('/administrator/projects/proj1/badges/badge1');
        cy.get('[data-cy="noContent"]').contains('No Skills Selected Yet');
        cy.selectSkill('[data-cy="skillsSelector"]', 'skill1');
        cy.get('[data-cy="skillsSelector"] button').should('have.focus');
    })

    it('removing a skill returns focus to the select input', () => {
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.visit('/administrator/projects/proj1/badges/badge1');
        cy.get('[data-cy="deleteSkill_skill1"]').click();
        cy.contains('Remove Required Skill');
        cy.get('[data-pc-name="pcacceptbutton"]').click();
        cy.get('[data-cy="skillsSelector"] button').should('have.focus');
    })

    it('cancel delete dialog should return focus to delete button', () => {
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createBadge(1, 1)
        cy.assignSkillToBadge(1, 1, 1)
        cy.visit('/administrator/projects/proj1/badges/badge1');
        cy.get('[data-cy="deleteSkill_skill1"]').click();
        cy.contains('Remove Required Skill');
        cy.get('[data-pc-name="pcrejectbutton"]').click();
        cy.get('[data-cy="deleteSkill_skill1"]').should('have.focus');
    })
});
