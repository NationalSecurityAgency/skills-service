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

        cy.visit('/projects/proj1/');
        cy.clickNav('Users');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 1,  value: '4,500' }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 1,  value: '6,000' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 1,  value: '7,500' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 1,  value: '9,000' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 1,  value: '10,500' }],
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 1,  value: '12,000' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('User Id').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 1,  value: '12,000' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 1,  value: '10,500' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 1,  value: '9,000' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 1,  value: '7,500' }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 1,  value: '6,000' }],
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 1,  value: '4,500' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Total Points').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 1,  value: '4,500' }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 1,  value: '6,000' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 1,  value: '7,500' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 1,  value: '9,000' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 1,  value: '10,500' }],
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 1,  value: '12,000' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Total Points').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 1,  value: '12,000' }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 1,  value: '10,500' }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 1,  value: '9,000' }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 1,  value: '7,500' }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 1,  value: '6,000' }],
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 1,  value: '4,500' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Last Reported Skill').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(2, 'day')) }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(7, 'day')) }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Last Reported Skill').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'user5@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(7, 'day')) }],
            [{ colIndex: 0,  value: 'user4@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(6, 'day')) }],
            [{ colIndex: 0,  value: 'user3@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(5, 'day')) }],
            [{ colIndex: 0,  value: 'user2@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(4, 'day')) }],
            [{ colIndex: 0,  value: 'user1@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(3, 'day')) }],
            [{ colIndex: 0,  value: 'user0@skills.org' }, { colIndex: 2,  value: dateFormatter(m.clone().add(2, 'day')) }],
        ], 5);
    });

    it('different page sizes', () => {
        for (let i = 0; i < 12; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().format('x')
            });
        }

        cy.visit('/projects/proj1/');
        cy.clickNav('Users');

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
        cy.intercept('users').as('getUsers');

        for (let i = 0; i < 7; i += 1) {
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${(i < 3) ? 'a' : 'b'}${i}@skills.org`,
                timestamp: m.clone()
                    .format('x')
            });
        }

        cy.visit('/projects/proj1/');
        cy.clickNav('Users');
        cy.wait('@getUsers')

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
        cy.intercept('users')
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

        cy.visit('/projects/proj1/');
        cy.clickNav('Users');
        cy.wait('@getUsers')

        const rowSelector = `${tableSelector} tbody tr`
        cy.get(rowSelector).should('have.length', 2).as('cyRows');

        cy.get('@cyRows').eq(0).find('td').as('row1');
        cy.get('@row1').eq(2).should('not.contain', 'Today');

        cy.get('@cyRows').eq(1).find('td').as('row2');
        cy.get('@row2').eq(2).should('contain', 'Today');
    });

    it('use first and last name in the display if available', () => {
        cy.intercept('users')
            .as('getUsers');

        cy.request('POST', `/api/projects/proj1/skills/skill1`);

        cy.visit('/projects/proj1/');
        cy.clickNav('Users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Firstname LastName (skills@skills.org)' }],
        ], 5);

        // make sure filter still works when username is formatted like that
        cy.get('[data-cy="users-skillIdFilter"]').type('last');
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Firstname LastName (skills@skills.org)' }],
        ], 5);
    });


    it('reset should reset paging', () => {
        cy.intercept('users')
            .as('getUsers');

        for (let i = 0; i < 6; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().format('x')
            });
        }
        cy.visit('/projects/proj1/users');
        cy.wait('@getUsers')

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
        cy.intercept('users')
            .as('getUsers');

        for (let i = 0; i < 2; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill1`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone().format('x')
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

        cy.visit('/projects/proj1/users');
        cy.wait('@getUsers')

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsBtn"]`).first().click();
        cy.contains("Client Display");
        cy.contains("ID: usera@skills.org");

        // validate from subject

        cy.visit('/projects/proj1/subjects/subj1/users');
        cy.wait('@getUsers')
        cy.contains('usera@skills.org');

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsBtn"]`).first().click();
        cy.contains("Client Display");
        cy.contains("ID: usera@skills.org");

        // validate from skill

        cy.visit('/projects/proj1/subjects/subj1/skills/skill1/users');
        cy.wait('@getUsers')
        cy.contains('usera@skills.org');

        cy.get(`${tableSelector} [data-cy="usersTable_viewDetailsBtn"]`).first().click();
        cy.contains("Client Display");
        cy.contains("ID: usera@skills.org");

        // validate from badge

        cy.visit('/projects/proj1/badges/badge1/users');
        cy.wait('@getUsers')
        cy.contains('usera@skills.org');

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
                timestamp: m.clone()
                    .format('x')
            });
        }

        // these users don't belong to the badge
        for (let i = 3; i < 5; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill2`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone()
                    .format('x')
            });
        }

        cy.visit('/projects/proj1/badges/badge1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5);


        // now check both skills
        cy.visit('/projects/proj1/subjects/subj1/skills/skill1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5);

        cy.visit('/projects/proj1/subjects/subj1/skills/skill2/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'userd@skills.org' }],
            [{ colIndex: 0,  value: 'usere@skills.org' }],
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
                timestamp: m.clone()
                    .format('x')
            });
        }

        // these users don't belong to the badge
        for (let i = 3; i < 5; i += 1) {
            const charToAdd = String.fromCharCode(97 + i);
            cy.request('POST', `/api/projects/proj1/skills/skill2`, {
                userId: `user${charToAdd}@skills.org`,
                timestamp: m.clone()
                    .format('x')
            });
        }

        // now check both subjects
        cy.visit('/projects/proj1/subjects/subj1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'usera@skills.org' }],
            [{ colIndex: 0,  value: 'userb@skills.org' }],
        ], 5);

        cy.visit('/projects/proj1/subjects/subj2/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'userd@skills.org' }],
            [{ colIndex: 0,  value: 'usere@skills.org' }],
        ], 5);
    });

})
