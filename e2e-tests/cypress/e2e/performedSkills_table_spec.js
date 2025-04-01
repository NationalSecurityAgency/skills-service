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
var moment = require('moment-timezone');

describe('Performed Skills Table Tests', () => {
    const tableSelector = '[data-cy=performedSkillsTable]';
    const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');
    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1'
        });

        Cypress.Commands.add('createSkills', (numSkills) => {
            for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
                cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                    projectId: 'proj1',
                    subjectId: 'subj1',
                    skillId: `skill${skillsCounter}`,
                    name: `Very Great Skill # ${skillsCounter}`,
                    pointIncrement: '150',
                    numPerformToCompletion: 20,
                });
            }
            ;
        });

        Cypress.Commands.add('report', (num, sameSkill = true) => {
            for (let i = 0; i < num; i += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill${sameSkill ? '1' : i + 1}`, {
                    userId: `user1@skills.org`,
                    timestamp: m.clone()
                        .add(i, 'day')
                        .format('x')
                });
            }
        });

    });


    it('sort by date', () => {
        cy.createSkills(1);
        cy.report(12);
        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');
        // default sort by date desc
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-23'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-22'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-21'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-20'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-19'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-18'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-17'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-16'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-15'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-14'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-13'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-12'
            }],
        ], 10);

        cy.get(`${tableSelector}`)
            .contains('Performed On')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-12'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-13'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-14'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-15'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-16'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-17'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-18'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-19'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-20'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-21'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-22'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }, {
                colIndex: 2,
                value: '2020-09-23'
            }],
        ], 10);
    });

    it('validate relative time', () => {
        cy.createSkill(1, 1, 1, {
            numPerformToCompletion: 10,
            pointIncrementInterval: 0
        });

        const m = moment.utc();
        let proj = '';
        cy.request({
            method: 'POST',
            url: `/api/projects/proj1/skills/skill1`,
            failOnStatusCode: true,
            body: {
                userId: 'user1@skills.org',
                timestamp: m.clone()
                    .format('x')
            }
        });
        cy.request({
            method: 'POST',
            url: `/api/projects/proj1/skills/skill1`,
            failOnStatusCode: true,
            body: {
                userId: 'user1@skills.org',
                timestamp: m.clone()
                    .subtract(10, 'minutes')
                    .format('x')
            }
        });
        cy.request({
            method: 'POST',
            url: `/api/projects/proj1/skills/skill1`,
            failOnStatusCode: true,
            body: {
                userId: 'user1@skills.org',
                timestamp: m.clone()
                    .subtract(2, 'hours')
                    .format('x')
            }
        });
        cy.request({
            method: 'POST',
            url: `/api/projects/proj1/skills/skill1`,
            failOnStatusCode: true,
            body: {
                userId: 'user1@skills.org',
                timestamp: m.clone()
                    .subtract(3, 'days')
                    .format('x')
            }
        });
        cy.request({
            method: 'POST',
            url: `/api/projects/proj1/skills/skill1`,
            failOnStatusCode: true,
            body: {
                userId: 'user1@skills.org',
                timestamp: m.clone()
                    .subtract(5, 'months')
                    .format('x')
            }
        });

        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'a few seconds ago'
            }],
            [{
                colIndex: 2,
                value: 'minutes ago'
            }],
            [{
                colIndex: 2,
                value: 'hours ago'
            }],
            [{
                colIndex: 2,
                value: 'days ago'
            }],
            [{
                colIndex: 2,
                value: 'months ago'
            }],
        ], 5);
    });

    it('sort by skill id', () => {
        cy.createSkills(12);
        cy.report(12, false);
        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');

        cy.get('[data-cy="skillsBTableTotalRows"]')
            .should('have.text', '12');
        cy.get(`${tableSelector}`)
            .contains('Skill Id')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill1'
            }],
            [{
                colIndex: 1,
                value: 'skill10'
            }],
            [{
                colIndex: 1,
                value: 'skill11'
            }],
            [{
                colIndex: 1,
                value: 'skill12'
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
            [{
                colIndex: 1,
                value: 'skill6'
            }],
            [{
                colIndex: 1,
                value: 'skill7'
            }],
            [{
                colIndex: 1,
                value: 'skill8'
            }],
            [{
                colIndex: 1,
                value: 'skill9'
            }],
        ], 10);

        cy.get(`${tableSelector}`)
            .contains('Skill Id')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill9'
            }],
            [{
                colIndex: 1,
                value: 'skill8'
            }],
            [{
                colIndex: 1,
                value: 'skill7'
            }],
            [{
                colIndex: 1,
                value: 'skill6'
            }],
            [{
                colIndex: 1,
                value: 'skill5'
            }],
            [{
                colIndex: 1,
                value: 'skill4'
            }],
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill12'
            }],
            [{
                colIndex: 1,
                value: 'skill11'
            }],
            [{
                colIndex: 1,
                value: 'skill10'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
        ], 10);
    });

    it('filter by skill id', () => {
        cy.createSkills(12);
        cy.report(12, false);
        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');

        cy.get('[data-cy="skillsBTableTotalRows"]')
            .should('have.text', '12')
        cy.get('[data-cy="performedSkills-skillIdFilter"]')
            .type('sKiLl1');
        cy.get('[data-cy="performedSkills-filterBtn"]')
            .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill12'
            }],
            [{
                colIndex: 1,
                value: 'skill11'
            }],
            [{
                colIndex: 1,
                value: 'skill10'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
        ], 5);

        cy.get('[data-cy="performedSkills-skillIdFilter"]')
            .type('0');
        cy.get('[data-cy="performedSkills-filterBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill10'
            }],
        ], 5);

        cy.get('[data-cy="performedSkills-resetBtn"]')
            .click();
        cy.get('[data-cy=skillsBTableTotalRows]')
            .contains(12);

        cy.get('[data-cy="performedSkills-skillIdFilter"]')
            .type('L2');
        cy.get('[data-cy="performedSkills-filterBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill2'
            }],
        ], 5);
    });

    it('filter by skill name', () => {
        cy.createSkills(12);
        cy.report(12, false);
        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');

        cy.get('[data-cy="skillsBTableTotalRows"]')
            .should('have.text', '12')
        cy.get('[data-cy="performedSkills-skillIdFilter"]')
            .type('# 12');
        cy.get('[data-cy="performedSkills-filterBtn"]')
            .click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill12'
            }],
        ], 5);
    });

    it('filter by using search icon', () => {
        cy.createSkills(12);
        cy.report(12, false);
        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');

        cy.get('[data-cy="skillsBTableTotalRows"]')
            .should('have.text', '12')
        cy.get(`${tableSelector} tr:nth-child(2) [data-cy="addSkillFilter"]`)
            .click();
        cy.get('[data-cy="performedSkills-skillIdFilter"]')
            .should('have.value', 'Very Great Skill # 11');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'Very Great Skill # 11'
            }],
        ], 5);
    });

    it('collapse long skill ids', () => {
        cy.createSkills(12);
        cy.report(12, false);

        cy.intercept('/admin/projects/proj1/performedSkills/user1*',
            {
                statusCode: 200,
                body: {
                    'data': [{
                        'skillName': 'Very Great Skill # 12',
                        'skillId': 'what a crazy long id that comes back it should get truncated sure hope so',
                        'performedOn': '2020-09-23T11:00:00.000+00:00',
                        'importedSkill': false
                    }],
                    'count': 1,
                    'totalCount': 1
                },
            })
            .as('getPerformedSkills');
        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');
        cy.wait('@getPerformedSkills');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'what a crazy long id that comes back it should... >> more'
            }],
        ], 5);

        cy.get(`${tableSelector} tr:nth-child(1)`)
            .contains('more')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'what a crazy long id that comes back it should get truncated sure hope so << less'
            }],
        ], 5);
    });

    it('delete skill event', () => {
        cy.createSkills(3);
        cy.report(3, false);
        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');

        cy.intercept('DELETE', '/admin/projects/proj1/skills/skill2/users/*/events/**')
            .as('delete');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
        ], 5);

        cy.get('[data-cy="deleteEventBtn"]')
            .should('have.length', 3)
            .as('deleteBtns');
        cy.get('@deleteBtns')
            .eq(1)
            .click();
        cy.contains('Removing skill [skill2]');
        cy.contains('YES, Delete It!')
            .click();
        cy.wait('@delete');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
        ], 5);

    });

    it('delete skill event disabled for events on skills imported from the catalog', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(2, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(2, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);
        cy.exportSkillToCatalog(2, 1, 3);

        cy.reportSkill(2, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.importSkillFromCatalog(1, 1, 2, 1);
        cy.finalizeCatalogImport(1);

        cy.visit('/administrator/projects/proj1/users/user6Good@skills.org/skillEvents');
        cy.get('[data-cy="deleteEventBtn"]')
            .should('not.exist');
    });

    it('delete all skill events', () => {
        cy.createSkills(3);
        cy.report(3, false);
        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
        ], 5);

        cy.openDialog('[data-cy="performedSkills-deleteAll"]')
        cy.contains('This will delete all skill events for user1@skills.org.');
        cy.get('[data-cy="currentValidationText"]').type('Delete Me')
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist')

        cy.get(tableSelector).contains('There are no records to show').should('exist')

        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0');
    });

    it('delete batch of skill events', () => {
        cy.createSkills(3);
        cy.report(3, false);
        cy.visit('/administrator/projects/proj1/users/user1@skills.org/skillEvents');

        cy.intercept('DELETE', '/admin/projects/proj1/users/*/events/**')
            .as('delete');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
        ], 5);

        cy.get('[data-cy="performedSkillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="performedSkills-deleteSelected"]').click()
        cy.contains('Removing 1 selected skill(s).');
        cy.contains('YES, Delete Them!').click();

        cy.wait('@delete');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
        ], 5);

        cy.get('[data-cy="performedSkillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="performedSkillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="performedSkills-deleteSelected"]').click()
        cy.contains('Removing 2 selected skill(s)');
        cy.contains('YES, Delete Them!').click();

        cy.wait('@delete');

        cy.get(tableSelector).contains('There are no records to show').should('exist')

    });

    it('can not delete batch of skill events with dependencies', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(2, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(2, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);
        cy.exportSkillToCatalog(2, 1, 3);

        cy.reportSkill(2, 1, 'user6Good@skills.org', '2020-09-12 11:00');
        cy.reportSkill(2, 2, 'user6Good@skills.org', '2020-09-12 11:05');
        cy.reportSkill(2, 3, 'user6Good@skills.org', '2020-09-12 11:10');
        cy.importSkillFromCatalog(1, 1, 2, 1);
        cy.importSkillFromCatalog(1, 1, 2, 2);
        cy.importSkillFromCatalog(1, 1, 2, 3);
        cy.finalizeCatalogImport(1);

        cy.visit('/administrator/projects/proj1/users/user6Good@skills.org/skillEvents');

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
        ], 5);

        cy.get('[data-cy="performedSkillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="performedSkillsTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="performedSkillsTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="performedSkills-deleteSelected"]').click()
        cy.contains('Removing 3 selected skill(s).');
        cy.contains('YES, Delete Them!').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'skill3'
            }],
            [{
                colIndex: 1,
                value: 'skill2'
            }],
            [{
                colIndex: 1,
                value: 'skill1'
            }],
        ], 5);

        cy.contains('Cannot delete skill events for skills imported from the catalog.')
    });
});

