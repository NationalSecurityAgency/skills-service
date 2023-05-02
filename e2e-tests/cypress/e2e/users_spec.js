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
    });

    it('sort and page', () => {
        for (let i = 0; i < 6; i += 1) {
            for (let j = 0; j <= i+2; j += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill1`, {userId: `user${i}@skills.org`, timestamp: m.clone().add(j, 'day').format('x')})
            }
        }

        cy.visit('/administrator/projects/proj1/');
        cy.clickNav('Users');

        // default sort order is 'Points Last Earned' desc
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(2, 'day')) }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Points Last Earned').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(2, 'day')) }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(7, 'day')) }],
        ], 5);

        cy.get(`${tableSelector}`).contains('User Id').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 2,  value: '4,500' }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 2,  value: '6,000' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 2,  value: '7,500' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 2,  value: '9,000' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 2,  value: '10,500' }],
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 2,  value: '12,000' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('User Id').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 2,  value: '12,000' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 2,  value: '10,500' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 2,  value: '9,000' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 2,  value: '7,500' }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 2,  value: '6,000' }],
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 2,  value: '4,500' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Progress').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 2,  value: '4,500' }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 2,  value: '6,000' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 2,  value: '7,500' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 2,  value: '9,000' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 2,  value: '10,500' }],
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 2,  value: '12,000' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Progress').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 2,  value: '12,000' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 2,  value: '10,500' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 2,  value: '9,000' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 2,  value: '7,500' }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 2,  value: '6,000' }],
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 2,  value: '4,500' }],
        ], 5);
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

        cy.get(`${tableSelector}`).contains('User Id').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
            [{ colIndex: 0,  value: 'userc@skills.org' }],
            [{ colIndex: 0,  value: 'userd@skills.org' }],
            [{ colIndex: 0,  value: 'usere@skills.org' }],
        ], 5, true, 12);

        cy.get('[data-cy="skillsBTablePageSize"]').select('10');
        cy.validateTable(tableSelector, [
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

        cy.get('[data-cy="skillsBTablePageSize"]').select('15');
        cy.validateTable(tableSelector, [
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
        ], 15, true, 12);
    });

    it('filter by user id', () => {
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

        cy.get(`${tableSelector}`).contains('User Id').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera0@skills.org' }],
            [{ colIndex: 0,  value: 'usera1@skills.org' }],
            [{ colIndex: 0,  value: 'usera2@skills.org' }],
            [{ colIndex: 0,  value: 'userb3@skills.org' }],
            [{ colIndex: 0,  value: 'userb4@skills.org' }],
            [{ colIndex: 0,  value: 'userb5@skills.org' }],
            [{ colIndex: 0,  value: 'userb6@skills.org' }],
        ], 5);

        cy.get('[data-cy="users-skillIdFilter"]').type('usera');
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera0@skills.org' }],
            [{ colIndex: 0,  value: 'usera1@skills.org' }],
            [{ colIndex: 0,  value: 'usera2@skills.org' }],
        ], 5);

        cy.get('[data-cy="users-skillIdFilter"]').clear().type('4');
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'userb4@skills.org' }],
        ], 5);

        cy.get('[data-cy="users-resetBtn"]').click();
        cy.wait('@getUsers')

        // filter should clear paging params and display the first page
        cy.get('[data-cy="skillsBTablePaging"]').contains('2').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'userb5@skills.org' }],
            [{ colIndex: 0,  value: 'userb6@skills.org' }],
        ], 2, true, 7);

        cy.get('[data-cy="users-skillIdFilter"]').clear().type('userb');
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'userb3@skills.org' }],
            [{ colIndex: 0,  value: 'userb4@skills.org' }],
            [{ colIndex: 0,  value: 'userb5@skills.org' }],
            [{ colIndex: 0,  value: 'userb6@skills.org' }],
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

        cy.get(`${tableSelector}`).contains('User Id').click();
        const rowSelector = `${tableSelector} tbody tr`
        cy.get(rowSelector).should('have.length', 2).as('cyRows');

        cy.get('@cyRows').eq(0).find('td').as('row1');
        cy.get('@row1').eq(3).should('not.contain', 'Today');

        cy.get('@cyRows').eq(1).find('td').as('row2');
        cy.get('@row2').eq(3).should('contain', 'Today');
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
                [{colIndex: 0, value: 'skills@evoforge.org'}],
                [{colIndex: 0, value: 'skills@evo-forge.org'}],
                [{colIndex: 0, value: 'foo-hydra'}]
            ], 5);
        } else {
            cy.validateTable(tableSelector, [
                [{colIndex: 0, value: 'skills@evoforge.org'}],
                [{colIndex: 0, value: 'skills@evo-forge.org'}],
                [{colIndex: 0, value: 'foo'}]
            ], 5);
        }
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

        cy.get(`${tableSelector}`).contains('User Id').click();
        cy.get('[data-cy="skillsBTablePaging"]').contains('2').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'userf@skills.org' }],
        ], 1, true, 6);

        cy.get('[data-cy="users-resetBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
            [{ colIndex: 0,  value: 'userc@skills.org' }],
            [{ colIndex: 0,  value: 'userd@skills.org' }],
            [{ colIndex: 0,  value: 'usere@skills.org' }],
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

        cy.get(`${tableSelector}`).contains('User Id').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5, true);

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsBtn"]`).first().click();
        cy.contains("Client Display");
        cy.contains("ID: usera@skills.org");

        // validate from subject
        cy.intercept('/admin/projects/proj1/subjects/subj1/users?query=*')
          .as('getSubjectUsers');
        cy.visit('/administrator/projects/proj1/subjects/subj1/users');
        cy.wait('@getSubjectUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5, true);

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsBtn"]`).first().click();
        cy.contains("Client Display");
        cy.contains("ID: usera@skills.org");

        // validate from skill
        cy.intercept('/admin/projects/proj1/skills/skill1/users?query=*')
          .as('getSkillsUsers');
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/users');
        cy.wait('@getSkillsUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5, true);

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsBtn"]`).first().click();
        cy.contains("Client Display");
        cy.contains("ID: usera@skills.org");

        // validate from badge
        cy.intercept('/admin/projects/proj1/badges/badge1/users?query=*')
          .as('getBadgeUsers');
        cy.visit('/administrator/projects/proj1/badges/badge1/users');
        cy.wait('@getBadgeUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5, true);

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsBtn"]`).first().click();
        cy.contains("Client Display");
        cy.contains("ID: usera@skills.org");
    });

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
        cy.get('[data-cy="skillsBTablePageSize"]').select('10');
        cy.wait('@getUsers')
        cy.get(`${tableSelector}`).contains('User Id').click();
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
        cy.get('[data-cy="skillsBTablePageSize"]').select('20');
        cy.wait('@getSubjUsers');
        cy.get(`${tableSelector}`).contains('User Id').click();
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
        cy.get('[data-cy="manageSkillBtn_skill1Subj2"]').click();
        cy.clickNav('Users');
        cy.wait('@getSkill1Users')
        cy.get('[data-cy="skillsBTablePageSize"]').select('10');
        cy.wait('@getSkill1Users')
        cy.get(`${tableSelector}`).contains('User Id').click();
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
        cy.get('[data-cy="skillsBTablePageSize"]').select('20');
        cy.wait('@getBadgeUsers');
        cy.get(`${tableSelector}`).contains('User Id').click();
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

        cy.get(`${tableSelector} th`).should('have.length', 4)
        cy.get(`${tableSelector}`).should('contain', 'Org');
        cy.validateTable(tableSelector, [
            [{colIndex: 1, value: 'tagA'}],
            [{colIndex: 1, value: 'tagB'}],
            [{colIndex: 1, value: 'tagC'}]
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
        cy.get(`${tableSelector} th`).should('have.length', 3)
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

        cy.get(`${tableSelector} th`).should('have.length', 4)

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsBtn"]`).first().click();
        cy.wait('@getUserTags')
        cy.contains("Client Display");
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

        cy.get(`${tableSelector} th`).should('have.length', 4)

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsBtn"]`).first().click();
        cy.contains("Client Display");
        cy.get('.h5').should('not.have.text', "Org: tagA");
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
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(2, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}50')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(4, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}60')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(5, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}70')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(6, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}80')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 3,  value: dateFormatter(m.clone().add(7, 'day')) }],
        ], 5);

        cy.get('[data-cy=users-progress-input]').type('{selectall}90')
        cy.get('[data-cy="users-filterBtn"]').click();
        // users-progress-input

        cy.get(tableSelector).contains('There are no records to show');
    });
})


