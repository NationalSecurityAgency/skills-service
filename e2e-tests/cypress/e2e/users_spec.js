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
import moment from 'moment';
const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD HH:mm');
describe('Users Tests', () => {

    const tableSelector = '[data-cy=usersTable]'
    const m = moment.utc('2020-09-12 11', 'YYYY-MM-DD HH');

    const exportedFileName = `cypress/downloads/proj1-users-${moment.utc().format('YYYY-MM-DD')}.xlsx`;

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Interesting Subject 1",
        })

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill1`,
            name: `Very Great Skill # 1`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.cleanupDownloadsDir()
    });

    it('sort and page', () => {
        for (let i = 0; i < 6; i += 1) {
            for (let j = 0; j <= i+2; j += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: `user${i}@skills.org`, timestamp: m.clone().add(j, 'day').format('x')})
            }
        }

        cy.visit('/administrator/projects/proj1/users');

        // default sort order is 'Points Last Earned' desc
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(2, 'day')) }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Points Last Earned').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(2, 'day')) }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
        ], 5);

        cy.get(`${tableSelector}`).contains('User').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 3,  value: '4,500' }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 3,  value: '6,000' }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 3,  value: '7,500' }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 3,  value: '9,000' }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 3,  value: '10,500' }],
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 3,  value: '12,000' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('User').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 3,  value: '12,000' }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 3,  value: '10,500' }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 3,  value: '9,000' }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 3,  value: '7,500' }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 3,  value: '6,000' }],
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 3,  value: '4,500' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Progress').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 3,  value: '4,500' }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 3,  value: '6,000' }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 3,  value: '7,500' }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 3,  value: '9,000' }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 3,  value: '10,500' }],
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 3,  value: '12,000' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Progress').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 3,  value: '12,000' }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 3,  value: '10,500' }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 3,  value: '9,000' }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 3,  value: '7,500' }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 3,  value: '6,000' }],
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 3,  value: '4,500' }],
        ], 5);

        // export users and verify that the file exists
        cy.readFile(exportedFileName).should('not.exist');
        cy.get('[data-cy="exportUsersTableBtn"]').click();
        cy.readFile(exportedFileName).should('exist');
    });

    it('archive and restore users', () => {

        cy.intercept('/admin/projects/proj1/users?query=*').as('getUsers');
        cy.intercept('GET', '/admin/projects/proj1/users/archive?*').as('getArchivedUsers');
        cy.intercept('POST', '/admin/projects/proj1/users/archive').as('archivedUsers');
        cy.intercept('POST', '/admin/projects/proj1/users/*/restore').as('restoreUser');

        for (let i = 0; i < 6; i += 1) {
            for (let j = 0; j <= i+2; j += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: `user${i}@skills.org`, timestamp: m.clone().add(j, 'day').format('x')})
            }
        }

        cy.visit('/administrator/projects/proj1/users');
        cy.wait('@getUsers')

        // default sort order is 'Points Last Earned' desc
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(2, 'day')) }],
        ], 5);

        cy.get('[data-pc-extend="paginator"] [aria-label="Page 1"]').click()

        cy.get('[data-cy="archiveUsersTableBtn"]').should('be.disabled');

        cy.get('[data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get('[data-cy="archiveUsersTableBtn"]').should('be.enabled');
        cy.get('[data-cy="archiveUsersTableBtn"]').click()

        cy.wait('@archivedUsers');
        cy.get('[data-cy="archiveUsersTableBtn"]').should('be.disabled');

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(2, 'day')) }],
        ], 5);

        // navigate to archived users
        cy.get('[data-cy="userArchiveBtn"]').should('be.enabled');
        cy.get('[data-cy="userArchiveBtn"]').click()
        cy.wait('@getArchivedUsers')

        const archivedUsersTableSelector = '[data-cy="userArchiveTable"]';
        cy.validateTable(archivedUsersTableSelector, [
            [{ colIndex: 0,  value: 'user4@skills.org' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }],
        ], 5);

        cy.get('[data-cy="restoreUser-user4@skills.org"]').should('be.enabled');
        cy.get('[data-cy="restoreUser-user4@skills.org"]').click()
        cy.wait('@restoreUser')

        cy.validateTable(archivedUsersTableSelector, [
            [{ colIndex: 0,  value: 'user2@skills.org' }],
        ], 5);


        cy.get('[data-cy="restoreUser-user2@skills.org"]').should('be.enabled');
        cy.get('[data-cy="restoreUser-user2@skills.org"]').click()
        cy.wait('@restoreUser')

        cy.get('[data-cy="noContent"]').contains('No Archived Users')

        // navigate back to project users
        cy.get('[data-cy="backToProjectUsersBtn"]').should('be.enabled');
        cy.get('[data-cy="backToProjectUsersBtn"]').click()

        cy.wait('@getUsers')
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(2, 'day')) }],
        ], 5);

        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get('[data-cy="archiveUsersTableBtn"]').should('be.enabled');
        cy.get('[data-cy="archiveUsersTableBtn"]').click()

        cy.wait('@archivedUsers');
        cy.get('[data-cy="archiveUsersTableBtn"]').should('be.disabled');

        // navigate to back archived users
        cy.get('[data-cy="userArchiveBtn"]').should('be.enabled');
        cy.get('[data-cy="userArchiveBtn"]').click()
        cy.wait('@getArchivedUsers')

        cy.validateTable(archivedUsersTableSelector, [
            [{ colIndex: 0,  value: 'user0@skills.org' }],
        ], 5);


        cy.get(`${archivedUsersTableSelector} [data-cy="usersTable_viewDetailsLink"]`).first().click();
        cy.get('[data-cy="subPageHeader"]').contains("User's Display");
        cy.contains("ID: user0@skills.org");
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTileBtn"]')
        cy.get('[data-cy="archivedUserTag"]').should('be.visible');
    });


    it('different page sizes archive users table', () => {

        cy.intercept('/admin/projects/proj1/users?query=*').as('getUsers');
        cy.intercept('GET', '/admin/projects/proj1/users/archive?*').as('getArchivedUsers');
        cy.intercept('POST', '/admin/projects/proj1/users/archive').as('archivedUsers');

        for (let i = 0; i < 12; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().add(i, 'day').format('x')
            });
        }

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Users');
        cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '12')
        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('15').click();

        cy.get(`${tableSelector} [data-pc-name="pcheadercheckbox"] [data-pc-section="input"]`).click();
        cy.get('[data-cy="archiveUsersTableBtn"]').should('be.enabled');
        cy.get('[data-cy="archiveUsersTableBtn"]').click()

        cy.wait('@archivedUsers');
        cy.get('[data-cy="archiveUsersTableBtn"]').should('be.disabled');

        // navigate to back archived users
        cy.get('[data-cy="userArchiveBtn"]').should('be.enabled');
        cy.get('[data-cy="userArchiveBtn"]').click()
        cy.wait('@getArchivedUsers')

        const archivedUsersTableSelector = '[data-cy="userArchiveTable"]';

        cy.get(`${archivedUsersTableSelector}`).contains('User').click();
        cy.validateTable(archivedUsersTableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
            [{ colIndex: 0,  value: 'userc@skills.org' }],
            [{ colIndex: 0,  value: 'userd@skills.org' }],
            [{ colIndex: 0,  value: 'usere@skills.org' }],
        ], 5, true, 12);

        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('10').click();
        cy.validateTable(archivedUsersTableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
            [{ colIndex: 0,  value: 'userc@skills.org' }],
            [{ colIndex: 0,  value: 'userd@skills.org' }],
            [{ colIndex: 0,  value: 'usere@skills.org' }],
            [{ colIndex: 0,  value: 'userf@skills.org' }],
            [{ colIndex: 0,  value: 'userg@skills.org' }],
            [{ colIndex: 0,  value: 'userh@skills.org' }],
            [{ colIndex: 0,  value: 'useri@skills.org' }],
            [{ colIndex: 0,  value: 'userj@skills.org' }],
        ], 10, true, 12);

        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('20').click();
        cy.validateTable(archivedUsersTableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
            [{ colIndex: 0,  value: 'userc@skills.org' }],
            [{ colIndex: 0,  value: 'userd@skills.org' }],
            [{ colIndex: 0,  value: 'usere@skills.org' }],
            [{ colIndex: 0,  value: 'userf@skills.org' }],
            [{ colIndex: 0,  value: 'userg@skills.org' }],
            [{ colIndex: 0,  value: 'userh@skills.org' }],
            [{ colIndex: 0,  value: 'useri@skills.org' }],
            [{ colIndex: 0,  value: 'userj@skills.org' }],
            [{ colIndex: 0,  value: 'userk@skills.org' }],
            [{ colIndex: 0,  value: 'userl@skills.org' }],
        ], 20, true, 12);
    });


    it('different page sizes', () => {
        for (let i = 0; i < 12; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().add(i, 'day').format('x')
            });
        }

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Users');
        cy.get('[data-cy="skillsBTableTotalRows"]').should('have.text', '12')

        cy.get(`${tableSelector}`).contains('User').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'usera@skills.org' }],
            [{ colIndex: 1,  value: 'userb@skills.org' }],
            [{ colIndex: 1,  value: 'userc@skills.org' }],
            [{ colIndex: 1,  value: 'userd@skills.org' }],
            [{ colIndex: 1,  value: 'usere@skills.org' }],
        ], 5, true, 12);

        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('10').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'usera@skills.org' }],
            [{ colIndex: 1,  value: 'userb@skills.org' }],
            [{ colIndex: 1,  value: 'userc@skills.org' }],
            [{ colIndex: 1,  value: 'userd@skills.org' }],
            [{ colIndex: 1,  value: 'usere@skills.org' }],
            [{ colIndex: 1,  value: 'userf@skills.org' }],
            [{ colIndex: 1,  value: 'userg@skills.org' }],
            [{ colIndex: 1,  value: 'userh@skills.org' }],
            [{ colIndex: 1,  value: 'useri@skills.org' }],
            [{ colIndex: 1,  value: 'userj@skills.org' }],
        ], 10, true, 12);

        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('15').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'usera@skills.org' }],
            [{ colIndex: 1,  value: 'userb@skills.org' }],
            [{ colIndex: 1,  value: 'userc@skills.org' }],
            [{ colIndex: 1,  value: 'userd@skills.org' }],
            [{ colIndex: 1,  value: 'usere@skills.org' }],
            [{ colIndex: 1,  value: 'userf@skills.org' }],
            [{ colIndex: 1,  value: 'userg@skills.org' }],
            [{ colIndex: 1,  value: 'userh@skills.org' }],
            [{ colIndex: 1,  value: 'useri@skills.org' }],
            [{ colIndex: 1,  value: 'userj@skills.org' }],
            [{ colIndex: 1,  value: 'userk@skills.org' }],
            [{ colIndex: 1,  value: 'userl@skills.org' }],
        ], 15, true, 12);
    });

    it('filter by user information', () => {
        cy.intercept('/admin/projects/proj1/users?query=*').as('getUsers');

        for (let i = 0; i < 7; i += 1) {
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${(i < 3) ? 'a' : 'b'}${i}@skills.org`,
                timestamp: m.clone().add(i, 'day')
                    .format('x')
            });
        }

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Users');
        cy.wait('@getUsers')

        cy.get(`${tableSelector}`).contains('User').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'usera0@skills.org' }],
            [{ colIndex: 1,  value: 'usera1@skills.org' }],
            [{ colIndex: 1,  value: 'usera2@skills.org' }],
            [{ colIndex: 1,  value: 'userb3@skills.org' }],
            [{ colIndex: 1,  value: 'userb4@skills.org' }],
            [{ colIndex: 1,  value: 'userb5@skills.org' }],
            [{ colIndex: 1,  value: 'userb6@skills.org' }],
        ], 5);

        cy.get('[data-cy="users-skillIdFilter"]').type('usera');
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'usera0@skills.org' }],
            [{ colIndex: 1,  value: 'usera1@skills.org' }],
            [{ colIndex: 1,  value: 'usera2@skills.org' }],
        ], 5);

        cy.get('[data-cy="users-skillIdFilter"]').clear().type('4');
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'userb4@skills.org' }],
        ], 5);

        cy.get('[data-cy="users-resetBtn"]').click();
        cy.wait('@getUsers')

        // filter should clear paging params and display the first page
        cy.get('[data-pc-section="pagebutton"]').contains('2').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'userb5@skills.org' }],
            [{ colIndex: 1,  value: 'userb6@skills.org' }],
        ], 2, true, 7);

        cy.get('[data-cy="users-skillIdFilter"]').clear().type('userb');
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'userb3@skills.org' }],
            [{ colIndex: 1,  value: 'userb4@skills.org' }],
            [{ colIndex: 1,  value: 'userb5@skills.org' }],
            [{ colIndex: 1,  value: 'userb6@skills.org' }],
        ], 5);
    });

    it('apply today tag', () => {
        cy.intercept('/admin/projects/proj1/users?query=*')
            .as('getUsers');

        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: `usera@skills.org`,
            timestamp: m.clone()
                .format('x')
        });
        cy.request('POST', `/api/projects/proj1/skills/skill1`, {
            userId: `userb@skills.org`,
            timestamp: moment.utc()
                .format('x')
        });

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Users');
        cy.wait('@getUsers')

        cy.get(`${tableSelector}`).contains('User').click();
        const rowSelector = `${tableSelector} tbody tr`
        cy.get(rowSelector).should('have.length', 2).as('cyRows');

        cy.get('@cyRows').eq(0).find('td').as('row1');
        cy.get('@row1').eq(4).should('not.contain', 'Today');

        cy.get('@cyRows').eq(1).find('td').as('row2');
        cy.get('@row2').eq(4).should('contain', 'Today');
    });

    it('strip the oauth provider from the userId if present', () => {
        const res = `
        {"data":
            [
                  {"userIdForDisplay":"skills@evoforge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00"},
                  {"userIdForDisplay":"skills@evo-forge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00"},
                  {"userIdForDisplay":"foo-hydra","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00"}
            ],
        "count":3,"totalCount":3}`;
        cy.intercept('/admin/projects/proj1/users?query=*', {
            statusCode: 200,
            body: res,
        }).as('getUsers');

        cy.request('POST', `/api/projects/proj1/skills/skill1`);

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Users');
        cy.wait('@getUsers')
        if (!Cypress.env('oauthMode')) {
            cy.validateTable(tableSelector, [
                [{colIndex: 1, value: 'skills@evoforge.org'}],
                [{colIndex: 1, value: 'skills@evo-forge.org'}],
                [{colIndex: 1, value: 'foo-hydra'}]
            ], 5);
        } else {
            cy.validateTable(tableSelector, [
                [{colIndex: 1, value: 'skills@evoforge.org'}],
                [{colIndex: 1, value: 'skills@evo-forge.org'}],
                [{colIndex: 1, value: 'foo'}]
            ], 5);
        }
    });

    it('displays user name if available', () => {
        const res = `
        {"data":
            [
                  {"userIdForDisplay":"skills@evoforge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00"},
                  {"userIdForDisplay":"skills@evo-forge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00"},
                  {"userIdForDisplay":"foo-hydra","firstName":"","lastName":"","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00"}
            ],
        "count":3,"totalCount":3}`;
        cy.intercept('/admin/projects/proj1/users?query=*', {
            statusCode: 200,
            body: res,
        }).as('getUsers');

        cy.request('POST', `/api/projects/proj1/skills/skill1`);

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{colIndex: 1, value: 'skills@evoforge.org (Tree, Skill)'}],
            [{colIndex: 1, value: 'skills@evo-forge.org (Tree, Skill)'}],
            [{colIndex: 1, value: 'foo'}]  // OAuth2UserConverterService truncates after the hyphen
        ], 5);
    });

    it('reset should reset paging', () => {
        cy.intercept('/admin/projects/proj1/users?query=*')
            .as('getUsers');

        for (let i = 0; i < 6; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().add(i, 'day').format('x')
            });
        }
        cy.visit('/administrator/projects/proj1/users');
        cy.wait('@getUsers')

        cy.get(`${tableSelector}`).contains('User').click();
        cy.get('[data-pc-section="pagebutton"]').contains('2').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'userf@skills.org' }],
        ], 1, true, 6);

        cy.get('[data-cy="users-resetBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'usera@skills.org' }],
            [{ colIndex: 1,  value: 'userb@skills.org' }],
            [{ colIndex: 1,  value: 'userc@skills.org' }],
            [{ colIndex: 1,  value: 'userd@skills.org' }],
            [{ colIndex: 1,  value: 'usere@skills.org' }],
        ], 5, true, 6);
    });

    it('navigate to user details', () => {
        cy.intercept('/admin/projects/proj1/users?query=*')
            .as('getProjectUsers');

        for (let i = 0; i < 2; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().add(i, 'day').format('x')
            });
        }

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1',
            "iconClass":"fas fa-ghost",
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        });

        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1')

        cy.visit('/administrator/projects/proj1/users');
        cy.wait('@getProjectUsers')

        cy.get(`${tableSelector}`).contains('User').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'usera@skills.org' }],
            [{ colIndex: 1,  value: 'userb@skills.org' }],
        ], 5, true);

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsLink"]`).first().click();
        cy.get('[data-cy="subPageHeader"]').contains("User's Display");
        cy.contains("ID: usera@skills.org");
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTileBtn"]')

        // validate from subject
        cy.intercept('/admin/projects/proj1/subjects/subj1/users?query=*')
          .as('getSubjectUsers');
        cy.visit('/administrator/projects/proj1/subjects/subj1/users');
        cy.wait('@getSubjectUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5, true);

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsLink"]`).first().click();
        cy.get('[data-cy="subPageHeader"]').contains("User's Display");
        cy.contains("ID: usera@skills.org");
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTileBtn"]')


        // validate from skill
        cy.intercept('/admin/projects/proj1/skills/skill1/users?query=*')
          .as('getSkillsUsers');
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/users');
        cy.wait('@getSkillsUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5, true);

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsLink"]`).first().click();
        cy.get('[data-cy="subPageHeader"]').contains("User's Display");
        cy.contains("ID: usera@skills.org");
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTileBtn"]')

        // validate from badge
        cy.intercept('/admin/projects/proj1/badges/badge1/users?query=*')
          .as('getBadgeUsers');
        cy.visit('/administrator/projects/proj1/badges/badge1/users');
        cy.wait('@getBadgeUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5, true);

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsLink"]`).first().click();
        cy.get('[data-cy="subPageHeader"]').contains("User's Display");
        cy.contains("ID: usera@skills.org");
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTileBtn"]')

    });

    it('navigate around user Skills Display', () => {
        for (let i = 0; i < 2; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().add(i, 'day').format('x')
            });
        }

        cy.visit('/administrator/projects/proj1/users');
        cy.get(`${tableSelector}`).contains('User').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'usera@skills.org' }],
            [{ colIndex: 1,  value: 'userb@skills.org' }],
        ], 5, true);

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsLink"]`).first().click();
        const validateProject = ()=> {
            cy.get('[data-cy="subPageHeader"]').contains("User's Display");
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTileBtn"]')
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="earnedPoints"]').should('have.text', '1,500')
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').should('have.length', 4)
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(0).contains('Projects')
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(1).contains('proj1')
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(2).contains('Users')
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(3).contains('usera@skills.org')
        }
        validateProject()

        cy.get('[data-cy="skillsDisplayHome"] [data-cy="subjectTileBtn"]').click()
        const validateSubject = ()=> {
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains("Interesting Subject 1");
            cy.get('[data-cy="pointHistoryChartNoData"]')
            cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillProgressTitle-skill1"]')
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').should('have.length', 5)
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(0).contains('Projects')
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(1).contains('proj1')
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(2).contains('Users')
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(3).contains('usera@skills.org')
            cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(4).contains('subj1')
        }
        validateSubject()

        cy.get('[data-cy="skillsDisplayHome"] [data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains("Skill Overview");
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains("1,500 / 15,000 Points");
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').should('have.length', 6)
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(0).contains('Projects')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(1).contains('proj1')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(2).contains('Users')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(3).contains('usera@skills.org')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(4).contains('subj1')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(5).contains('skill1')

        cy.get('[data-cy="breadcrumb-subj1"]').click()
        validateSubject()

        cy.get('[data-cy="myRankBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains("My Rank");
        cy.get('[data-cy="leaderboard"]').contains('1,500 Points')
        cy.get('[data-cy="levelBreakdownChart-animationEnded"]')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').should('have.length', 6)
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(0).contains('Projects')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(1).contains('proj1')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(2).contains('Users')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(3).contains('usera@skills.org')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(4).contains('subj1')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(5).contains('Rank')

        cy.get('[data-cy="breadcrumb-usera@skills.org"]').click()
        validateProject()

        cy.get('[data-cy="myRankBtn"]').click()
        cy.get('[data-cy="skillsDisplayHome"] [data-cy="title"]').contains("My Rank");
        cy.get('[data-cy="leaderboard"]').contains('1,500 Points')
        cy.get('[data-cy="levelBreakdownChart-animationEnded"]')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').should('have.length', 5)
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(0).contains('Projects')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(1).contains('proj1')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(2).contains('Users')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(3).contains('usera@skills.org')
        cy.get('[data-cy="breadcrumb-bar"] [data-cy=breadcrumbItemValue]').eq(4).contains('Rank')
    })

    it('User Display point history returns data for the selected user', () => {
        cy.createSubject(1, 2)
        cy.createSkill(1, 2, 2,  { pointIncrement: '777', numPerformToCompletion: '2', pointIncrementInterval: 0 })
        const userId = 'usera@skills.org'
        cy.reportSkill(1, 1, userId, 'yesterday')
        cy.reportSkill(1, 1, userId, 'now')
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 2, userId, date: 'now' })

        cy.intercept(`/api/projects/proj1/pointHistory?userId=**`, (req) => {
            req.continue((res) => {
                expect(res.body.achievements).to.have.length(1)
                expect(res.body.achievements[0].points).to.equal(3777)
                expect(res.body.achievements[0].name).to.equal('Level 1')

                expect(res.body.pointsHistory).to.have.length(2)
                expect(res.body.pointsHistory[0].points).to.equal(1500)
                expect(res.body.pointsHistory[1].points).to.equal(3777)
            })
        }).as('pointHistory')

        cy.intercept(`/api/projects/proj1/subjects/subj1/pointHistory?userId=**`, (req) => {
            req.continue((res) => {
                expect(res.body.achievements).to.have.length(1)
                expect(res.body.achievements[0].points).to.equal(1500)
                expect(res.body.achievements[0].name).to.equal('Level 1')

                expect(res.body.pointsHistory).to.have.length(2)
                expect(res.body.pointsHistory[0].points).to.equal(1500)
                expect(res.body.pointsHistory[1].points).to.equal(3000)
            })
        }).as('subj1PointHistory')

        cy.intercept(`/api/projects/proj1/subjects/subj2/pointHistory?userId=**`, (req) => {
            req.continue((res) => {
                expect(res.body.achievements).to.have.length(0)
                expect(res.body.pointsHistory).to.have.length(0)
            })
        }).as('subj2PointHistory')

        cy.visit('/administrator/projects/proj1/users');
        cy.get('[data-p-index="0"] [data-cy="usersTable_viewDetailsLink"]').click()
        cy.wait('@pointHistory')
        cy.get('[data-cy="subjectTile-subj1"] [data-cy="subjectTileBtn"]').click()
        cy.wait('@subj1PointHistory')
        cy.get('[data-cy="skillProgressTitle-skill1"]')

        cy.go('back');  // browser back button
        cy.wait('@pointHistory')
        cy.get('[data-cy="subjectTile-subj2"] [data-cy="subjectTileBtn"]').click()
        cy.wait('@subj2PointHistory')
        cy.get('[data-cy="skillProgressTitle-skill2Subj2"]')
    })

    it('view users from badge and skill', () => {
        cy.intercept('users')
            .as('getUsers');


        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill2`,
            name: `Very Great Skill # 2`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1',
            "iconClass":"fas fa-ghost",
            description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
        });

        cy.request('POST', '/admin/projects/proj1/badge/badge1/skills/skill1')

        for (let i = 0; i < 2; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().add(i, 'day')
                    .format('x')
            });
        }

        // these users don't belong to the badge
        for (let i = 3; i < 5; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill2`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().add(i, 'day')
                    .format('x')
            });
        }

        cy.visit('/administrator/projects/proj1/badges/badge1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'userb@skills.org' }],
            [{ colIndex: 0,  value: 'usera@skills.org' }],
        ], 5);


        // now check both skills
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'userb@skills.org' }],
            [{ colIndex: 0,  value: 'usera@skills.org' }],
        ], 5);

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill2/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usere@skills.org' }],
            [{ colIndex: 0,  value: 'userd@skills.org' }],
        ], 5);
    });


    it('view users from subject', () => {
        cy.intercept('users')
            .as('getUsers');


        cy.request('POST', '/admin/projects/proj1/subjects/subj2', {
            projectId: 'proj1',
            subjectId: 'subj2',
            name: "Interesting Subject 2",
        })

        cy.request('POST', `/admin/projects/proj1/subjects/subj2/skills/skill2`, {
            projectId: 'proj1',
            subjectId: 'subj2',
            skillId: `skill2`,
            name: `Very Great Skill # 2`,
            pointIncrement: '1500',
            numPerformToCompletion: '10',
        });

        for (let i = 0; i < 2; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().add(i, 'day')
                    .format('x')
            });
        }

        // these users don't belong to the badge
        for (let i = 3; i < 5; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill2`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().add(i, 'day')
                    .format('x')
            });
        }

        // now check both subjects
        cy.visit('/administrator/projects/proj1/subjects/subj1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'userb@skills.org' }],
            [{ colIndex: 0,  value: 'usera@skills.org' }],
        ], 5);

        cy.visit('/administrator/projects/proj1/subjects/subj2/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usere@skills.org' }],
            [{ colIndex: 0,  value: 'userd@skills.org' }],
        ], 5);
    });

    it('users with various progress', () => {
        cy.createSkill(1, 1, 3,  { pointIncrement: '1111', numPerformToCompletion: '10', pointIncrementInterval: 0 })
        cy.createSubject(1, 2)
        cy.createSkill(1, 2, 1,  { pointIncrement: '777', numPerformToCompletion: '6', pointIncrementInterval: 0 })
        cy.createSkill(1, 2, 2,  { pointIncrement: '333', numPerformToCompletion: '10', pointIncrementInterval: 0 })
        cy.createSkill(1, 2, 3,  { pointIncrement: '666', numPerformToCompletion: '6', pointIncrementInterval: 0 })
        cy.createBadge(1, 1);
        cy.assignSkillToBadge(1, 1, 3);
        cy.assignSkillToBadge(1, 1, 1, 2);
        cy.assignSkillToBadge(1, 1, 2, 2);
        cy.assignSkillToBadge(1, 1, 3, 2);

        cy.intercept('/admin/projects/proj1/users*').as('getUsers');
        cy.intercept('/admin/projects/proj1/subjects/subj2/users*').as('getSubjUsers');
        cy.intercept('/admin/projects/proj1/skills/skill1Subj2/users*').as('getSkill1Users');
        cy.intercept('/admin/projects/proj1/badges/badge1/users*').as('getBadgeUsers');

        for (let i = 0; i < 6; i += 1) {
            for (let j = 0; j < 6; j += 1) {
                const userId = `user${j}@skills.org`
                cy.log(`Adding events for ${userId}`);
                cy.doReportSkill({ project: 1, skill: 1, subjNum: 2, userId })
                if (j > 1) {
                    cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId })
                }
                if (j > 2) {
                    cy.doReportSkill({ project: 1, skill: 2, subjNum: 2, userId })
                }
            }
        }

        // validate project's users
        cy.visit('/administrator/projects/proj1/users');
        cy.wait('@getUsers')
        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('10').click();
        cy.wait('@getUsers')
        cy.get(`${tableSelector}`).contains('User').click();
        cy.wait('@getUsers')

        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressPercent"]').should('have.text', '12%')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressCurrentPoints"]').should('have.text', '4,662')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressTotalPoints"]').should('have.text', '38,098')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressCurrentLevel"]').should('have.text', '1')

        cy.get('[data-cy="usr_progress-user2@skills.org"] [data-cy="progressPercent"]').should('have.text', '29%')
        cy.get('[data-cy="usr_progress-user2@skills.org"] [data-cy="progressCurrentPoints"]').should('have.text', '11,328')
        cy.get('[data-cy="usr_progress-user2@skills.org"] [data-cy="progressTotalPoints"]').should('have.text', '38,098')
        cy.get('[data-cy="usr_progress-user2@skills.org"] [data-cy="progressCurrentLevel"]').should('have.text', '2')

        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressPercent"]').should('have.text', '34%')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressCurrentPoints"]').should('have.text', '13,326')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressTotalPoints"]').should('have.text', '38,098')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressCurrentLevel"]').should('have.text', '2')

        // validate subject's users
        cy.visit('/administrator/projects/proj1/subjects/subj2/users');
        cy.wait('@getSubjUsers');
        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('20').click();
        cy.wait('@getSubjUsers');
        cy.get(`${tableSelector}`).contains('User').click();
        cy.wait('@getSubjUsers');

        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressPercent"]').should('have.text', '38%')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressCurrentPoints"]').should('have.text', '4,662')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressTotalPoints"]').should('have.text', '11,988')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressCurrentLevel"]').should('have.text', '2')

        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressPercent"]').should('have.text', '55%')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressCurrentPoints"]').should('have.text', '6,660')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressTotalPoints"]').should('have.text', '11,988')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressCurrentLevel"]').should('have.text', '3')

        // validate skill where users have 100%
        cy.clickNav('Skills');
        cy.get('[data-cy="manageSkillLink_skill1Subj2"]').click();
        cy.clickNav('Users');
        cy.wait('@getSkill1Users')
        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('10').click();
        cy.wait('@getSkill1Users')
        cy.get(`${tableSelector}`).contains('User').click();
        cy.wait('@getSkill1Users')

        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressPercent"]').should('have.text', '100%')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressCurrentPoints"]').should('have.text', '4,662')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressTotalPoints"]').should('have.text', '4,662')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressLevels"]').should('not.exist')

        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressPercent"]').should('have.text', '100%')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressCurrentPoints"]').should('have.text', '4,662')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressTotalPoints"]').should('have.text', '4,662')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressLevels"]').should('not.exist')

        // validate badge's users
        cy.visit('/administrator/projects/proj1/badges/badge1/users');
        cy.wait('@getBadgeUsers');
        cy.get('[data-pc-name="pcrowperpagedropdown"]').click().get('[data-pc-section="option"]').contains('20').click();
        cy.wait('@getBadgeUsers');
        cy.get(`${tableSelector}`).contains('User').click();
        cy.wait('@getBadgeUsers');

        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressPercent"]').should('have.text', '20%')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressCurrentPoints"]').should('have.text', '4,662')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressTotalPoints"]').should('have.text', '23,098')
        cy.get('[data-cy="usr_progress-user0@skills.org"] [data-cy="progressLevels"]').should('not.exist')

        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressPercent"]').should('have.text', '57%')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressCurrentPoints"]').should('have.text', '13,326')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressTotalPoints"]').should('have.text', '23,098')
        cy.get('[data-cy="usr_progress-user3@skills.org"] [data-cy="progressLevels"]').should('not.exist')
    });

    it('users with no levels', () => {
        cy.createSkill(1, 1, 3,  { pointIncrement: '1111', numPerformToCompletion: '10', pointIncrementInterval: 0 })

        const userId = 'user0'
        const userId1 = 'user1'
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId })

        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: userId1 })
        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: userId1 })

        cy.visit('/administrator/projects/proj1/users');
        cy.get('[data-cy="usr_progress-user1"] [data-cy="progressCurrentLevel"]').should('have.text', '1')
        cy.get('[data-cy="usr_progress-user0"] [data-cy="progressCurrentLevel"]').should('have.text', 'None')
    });

    it('show user tag in users table', () => {
        const res = `
        {"data":
            [
                  {"userIdForDisplay":"skills@evoforge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagA"},
                  {"userIdForDisplay":"skills@evo-forge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagB"},
                  {"userIdForDisplay":"foo-hydra","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagC"}
            ],
        "count":3,"totalCount":3}`;
        cy.intercept('/admin/projects/proj1/users?query=*', {
            statusCode: 200,
            body: res,
        }).as('getUsers');

        cy.request('POST', `/api/projects/proj1/skills/skill1`);

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Users');
        cy.wait('@getUsers')

        cy.get(`${tableSelector} th`).should('have.length', 6)
        cy.get(`${tableSelector}`).should('contain', 'Org');
        cy.validateTable(tableSelector, [
            [{colIndex: 2, value: 'tagA'}],
            [{colIndex: 2, value: 'tagB'}],
            [{colIndex: 2, value: 'tagC'}]
        ], 5);

        cy.get('[data-cy=usersTable_viewUserTagMetricLink]').eq(0).click()

        cy.contains("Metrics");
        cy.contains("Overall Levels for Org: tagA");
        cy.contains("Users for Org: tagA");
    });

    it('do not show user tag in users table when not configured', () => {
        const res = `
        {"data":
            [
                  {"userIdForDisplay":"skills@evoforge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagA"},
                  {"userIdForDisplay":"skills@evo-forge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagB"},
                  {"userIdForDisplay":"foo-hydra","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagC"}
            ],
        "count":3,"totalCount":3}`;
        cy.intercept('/admin/projects/proj1/users?query=*', {
            statusCode: 200,
            body: res,
        }).as('getUsers');
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                delete conf.usersTableAdditionalUserTagKey;
                delete conf.usersTableAdditionalUserTagLabel;
                res.send(conf);
            });
        }).as('loadConfig');

        cy.request('POST', `/api/projects/proj1/skills/skill1`);

        cy.visit('/administrator/projects/proj1/');
        cy.wait('@loadConfig')
        cy.clickNav('Users');
        cy.wait('@getUsers')
        cy.get(`${tableSelector} th`).should('have.length', 5)
        cy.get(`${tableSelector}`).should('not.contain', 'Org');
    });

    it('show user tag on users page', () => {
        const res = `
        {"data":
            [
                  {"userIdForDisplay":"skills@evoforge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagA"},
                  {"userIdForDisplay":"skills@evo-forge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagB"},
                  {"userIdForDisplay":"foo-hydra","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagC"}
            ],
        "count":3,"totalCount":3}`;
        cy.intercept('/admin/projects/proj1/users?query=*', {
            statusCode: 200,
            body: res,
        }).as('getUsers');

        const tagRes = `
            [
                {"id":1,"userId":"skills@evoforge.org","key":"dutyOrganization","value":"tagA"}
            ]
        `
        cy.intercept('/app/userInfo/userTags/*', {
            statusCode: 200,
            body: tagRes,
        }).as('getUserTags')

        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.userPageTagsToDisplay = 'dutyOrganization/Org'
                res.send(conf);
            });
        }).as('loadConfig');

        cy.request('POST', `/api/projects/proj1/skills/skill1`);

        cy.visit('/administrator/projects/proj1/');
        cy.wait('@loadConfig')
        cy.clickNav('Users');
        cy.wait('@getUsers')

        cy.get(`${tableSelector} th`).should('have.length', 6)

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsLink"]`).first().click();
        cy.wait('@getUserTags')
        cy.contains("Client Display");
        cy.get('[data-cy="userTagHeader"]').should('exist');
        cy.contains("Org: tagA");
    });

    it('do not show user tag on users page if not enabled', () => {
        const res = `
        {"data":
            [
                  {"userIdForDisplay":"skills@evoforge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagA"},
                  {"userIdForDisplay":"skills@evo-forge.org","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagB"},
                  {"userIdForDisplay":"foo-hydra","firstName":"Skill","lastName":"Tree","email":"skills@evoforge.org","dn":null,"userId":"skills@evoforge.org","totalPoints":492,"lastUpdated":"2021-03-04T19:22:44.714+00:00","userTag":"tagC"}
            ],
        "count":3,"totalCount":3}`;
        cy.intercept('/admin/projects/proj1/users?query=*', {
            statusCode: 200,
            body: res,
        }).as('getUsers');

        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                delete conf.userPageTagsToDisplay
                res.send(conf);
            });
        }).as('loadConfig');

        cy.request('POST', `/api/projects/proj1/skills/skill1`);

        cy.visit('/administrator/projects/proj1/');
        cy.wait('@loadConfig')
        cy.clickNav('Users');
        cy.wait('@getUsers')

        cy.get(`${tableSelector} th`).should('have.length', 6)

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsLink"]`).first().click();
        cy.contains("Client Display");
        cy.get('[data-cy="userTagHeader"]').should('not.exist');
    });

    it('filter project users by completion', () => {
        for (let i = 0; i < 6; i += 1) {
            for (let j = 0; j <= i+2; j += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: `user${i}@skills.org`, timestamp: m.clone().add(j, 'day').format('x')})
            }
        }

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Users');

        // default sort order is 'Points Last Earned' desc
        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 1,  value: 'user1@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 1,  value: 'user0@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(2, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}50')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 1,  value: 'user2@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(4, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}60')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 1,  value: 'user3@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(5, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}70')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 1,  value: 'user4@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(6, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}80')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.validateTable(tableSelector, [
            [{ colIndex: 1,  value: 'user5@skills.org' }, { colIndex: 5,  value: dateFormatter(m.clone().add(7, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}90')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.get(tableSelector).contains('There are no records to show');
    });
})


