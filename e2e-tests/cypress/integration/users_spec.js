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

})
