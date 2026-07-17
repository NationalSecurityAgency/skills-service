/*
 * Copyright 2026 SkillTree
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
describe('Users Page for a Single Skill Tag Tests', () => {

    const tableSelector = '[data-cy="usersTableMetric"]'

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 5);
        cy.createSkill(1, 2, 6);
        cy.createSkill(1, 2, 7);

        cy.addTagToSkills(1, ['skill1', 'skill2', 'skill3', 'skill5Subj2', 'skill6Subj2'], 1)
    });

    it('no users', () => {
        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.get(`${tableSelector} [data-pc-section="emptymessage"]`).contains('There are no records to show')
    })

    it('users with progress', () => {
        // user 1
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user1', date: '2026-07-16 14:12' })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: 'user1', date: '2026-07-14 10:11' })
        cy.doReportSkill({ project: 1, skill: 4, subjNum: 1, userId: 'user1', date: '2026-07-16 10:11' })

        // user 2
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user2', date: '2026-07-01 08:09' })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user2', date: '2026-07-02 08:09' })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: 'user2', date: '2026-07-03 08:09' })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: 'user2', date: '2026-07-04 08:09' })
        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: 'user2', date: '2026-07-05 08:09' })
        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: 'user2', date: '2026-07-06 08:09' })
        cy.doReportSkill({ project: 1, skill: 4, subjNum: 1, userId: 'user2', date: '2026-07-07 08:09' })
        cy.doReportSkill({ project: 1, skill: 4, subjNum: 1, userId: 'user2', date: '2026-07-08 08:09' })

        cy.doReportSkill({ project: 1, skill: 5, subjNum: 2, userId: 'user2', date: '2026-07-09 08:09' })
        cy.doReportSkill({ project: 1, skill: 5, subjNum: 2, userId: 'user2', date: '2026-07-10 08:09' })
        cy.doReportSkill({ project: 1, skill: 6, subjNum: 2, userId: 'user2', date: '2026-07-11 08:09' })
        cy.doReportSkill({ project: 1, skill: 6, subjNum: 2, userId: 'user2', date: '2026-07-12 14:10' })
        cy.doReportSkill({ project: 1, skill: 7, subjNum: 2, userId: 'user2', date: '2026-07-13 08:09' })
        cy.doReportSkill({ project: 1, skill: 7, subjNum: 2, userId: 'user2', date: '2026-07-14 08:09' })

        // user 3
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user3', date: '2026-07-01 08:09' })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user3', date: '2026-07-02 08:09' })
        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: 'user3', date: '2026-07-05 08:09' })
        cy.doReportSkill({ project: 1, skill: 4, subjNum: 1, userId: 'user3', date: '2026-07-07 08:09' })
        cy.doReportSkill({ project: 1, skill: 4, subjNum: 1, userId: 'user3', date: '2026-07-08 08:09' })

        cy.doReportSkill({ project: 1, skill: 5, subjNum: 2, userId: 'user3', date: '2026-07-09 08:09' })
        cy.doReportSkill({ project: 1, skill: 5, subjNum: 2, userId: 'user3', date: '2026-07-10 08:09' })
        cy.doReportSkill({ project: 1, skill: 6, subjNum: 2, userId: 'user3', date: '2026-07-11 14:10' })
        cy.doReportSkill({ project: 1, skill: 7, subjNum: 2, userId: 'user3', date: '2026-07-14 08:09' })
        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: 'user1'}, {colIndex: 2, value: '200 / 1,000'}, {colIndex: 3, value: '2026-07-14 10:11'}, {colIndex: 4, value: '2026-07-16 14:12'}],
            [{colIndex: 0, value: 'user2'}, {colIndex: 2, value: '1,000 / 1,000'}, {colIndex: 3, value: '2026-07-01 08:09'}, {colIndex: 4, value: '2026-07-12 14:10'}],
            [{colIndex: 0, value: 'user3'}, {colIndex: 2, value: '600 / 1,000'}, {colIndex: 3, value: '2026-07-01 08:09'}, {colIndex: 4, value: '2026-07-11 14:10'}],
        ], 10);
    })

    it('filter by user id', () => {
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user1', date: '2026-07-16 14:12' })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user2', date: '2026-07-01 08:09' })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user3', date: '2026-07-01 08:09' })
        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: 'user1'}],
            [{colIndex: 0, value: 'user2'}],
            [{colIndex: 0, value: 'user3'}],
        ], 10);

        cy.get('[data-cy="users-skillIdFilter"]').type('sEr2{enter}')
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: 'user2'}],
        ], 10);
    })

    it('filter by progress', () => {
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user1', date: '2026-07-16 14:12' })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user2', date: '2026-07-01 08:09' })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: 'user2', date: '2026-07-01 08:09' })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user3', date: '2026-07-01 08:09' })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: 'user3', date: '2026-07-01 08:09' })
        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: 'user3', date: '2026-07-01 08:09' })

        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: 'user1'}],
            [{colIndex: 0, value: 'user2'}],
            [{colIndex: 0, value: 'user3'}],
        ], 10);

        cy.get('[data-cy="users-progress-input"]').type('{selectAll}19{enter}')
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: 'user2'}],
            [{colIndex: 0, value: 'user3'}],
        ], 10);
        cy.get('[data-cy="users-max-progress-input"]').type('{selectAll}21{enter}')
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: 'user2'}],
        ], 10);
    })

    it('filter by org', () => {
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user1', date: '2026-07-16 14:12' })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user2', date: '2026-07-01 08:09' })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user3', date: '2026-07-01 08:09' })

        const tagKey = 'dutyOrganization'
        cy.addUserTag([{
            tagKey,
            tags: ['AA1']
        }, {
            tagKey,
            tags: ['AB1']
        }, {
            tagKey,
            tags: ['AC1']
        }]);

        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: 'user1'}],
            [{colIndex: 0, value: 'user2'}],
            [{colIndex: 0, value: 'user3'}],
        ], 10);

        cy.get('[data-cy="users-userTagFilter"]').type('ab{enter}')
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: 'user2'}],
        ], 10);
    })

    it('sort and page - user id', () => {
        
        const expected = []
        for (let i = 0; i < 12; i++) {
            const userId = `user${i.toString().padStart(2, '0')}`
            cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: userId, date: `2026-06-${10+i} 14:12` })
            expected.push([{colIndex: 0, value: userId}])
        }
        const reversedExpected = [...expected].reverse()
        
        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.get(`${tableSelector} [data-pc-section="columntitle"]`).contains('User').click();
        cy.validateTable(tableSelector, expected, 10);

        // saved into local storage
        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.validateTable(tableSelector, expected, 10);

        cy.get(`${tableSelector} [data-pc-section="columntitle"]`).contains('User').click();
        cy.validateTable(tableSelector, reversedExpected, 10);
    })

    it('sort by org', () => {
        const tagKey = 'dutyOrganization'
        const tagsToAdd = []
        const expected = []
        for (let i = 0; i < 12; i++) {
            const paddedNum = i.toString().padStart(2, '0')
            const userId = `user${paddedNum}`

            cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: userId, date: `2026-06-${10+i} 14:12` })
            expected.push([{colIndex: 0, value: userId}])
            tagsToAdd.push({ userId, tagKey, tags: [`AA${paddedNum}`] })
        }
        const reversedExpected = [...expected].reverse()
        cy.addUserTag(tagsToAdd)

        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.get(`${tableSelector} [data-pc-section="columntitle"]`).contains('Org').click();
        cy.validateTable(tableSelector, expected, 10);

        cy.get(`${tableSelector} [data-pc-section="columntitle"]`).contains('Org').click();
        cy.validateTable(tableSelector, reversedExpected, 10);
    })

    it('sort by progress', () => {
        cy.viewport(1200, 1800)
        const expected = []

        cy.createSkill(1, 1, 10, { points: 10, numPerformToCompletion: 20, pointIncrementInterval: 0})
        cy.addTagToSkills(1, ['skill10'], 1)
        for (let i = 0; i < 12; i++) {
            const paddedNum = i.toString().padStart(2, '0')
            const userId = `user${paddedNum}`

            for (let j = 0; j < i+1; j++) {
                cy.doReportSkill({ project: 1, skill: 10, subjNum: 1, userId: userId, date: `2026-06-${10+i} 14:12` })
            }
            expected.push([{colIndex: 0, value: userId}])
        }
        const reversedExpected = [...expected].reverse()

        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.get(`${tableSelector} [data-pc-section="columntitle"]`).contains('Progress').click();
        cy.validateTable(tableSelector, expected, 10);

        cy.get(`${tableSelector} [data-pc-section="columntitle"]`).contains('Progress').click();
        cy.validateTable(tableSelector, reversedExpected, 10);
    })

    it('do not show excludeImported switch', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 10)
        cy.exportSkillToCatalog(2, 1, 10);
        cy.importSkillFromCatalog(1, 1, 2, 10);
        cy.finalizeCatalogImport(1);

        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: 'user1', date: '2026-07-16 14:12' })

        cy.visit('/administrator/projects/proj1/skills-tags/tag1/users');
        cy.validateTable(tableSelector, [
            [{colIndex: 0, value: 'user1'}],
        ], 10);
        cy.get('[data-cy="excludeImportedToggle"]').should('not.exist')
    })

})
