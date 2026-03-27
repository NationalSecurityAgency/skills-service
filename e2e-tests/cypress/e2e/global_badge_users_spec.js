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
describe('Global Badge Users Tests', () => {

    Cypress.Commands.add('addUserTag', (userId, tagKey, tags) => {
        cy.request('POST', `/root/users/${userId}/tags/${tagKey}`, { tags });
    });

    const tableSelector = '[data-cy=usersTable]'

    beforeEach(() => {
        cy.intercept('GET', '/admin/badges/*/skills/available?*')
            .as('loadAvailableSkills');

        cy.intercept('GET', `/app/badges`)
            .as('getGlobalBadges');
        cy.intercept('POST', '/app/badges/name/exists')
            .as('nameExists');

        cy.intercept('/admin/badges/globalBadge1/users?query=*').as('getUsers');

        cy.createGlobalBadge(1)
        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        cy.createSkill(1, 1, 2, {numPerformToCompletion : 1})
        cy.assignSkillToGlobalBadge(1, 1, 1);
        cy.assignSkillToGlobalBadge(1, 2, 1);

        cy.createProject(2)
        cy.createSubject(2, 1)
        cy.createSkill(2, 1, 1, {numPerformToCompletion : 1})
        cy.createSkill(2, 1, 2, {numPerformToCompletion : 1})
        cy.assignSkillToGlobalBadge(1, 1, 2);
        cy.assignSkillToGlobalBadge(1, 2, 2);

        cy.createProject(3)
        cy.createSubject(3, 1)
        cy.createSkill(3, 1, 1, {numPerformToCompletion : 1})
        cy.createSkill(3, 1, 2, {numPerformToCompletion : 1})

        cy.assignProjectToGlobalBadge(1, 2, 3)
        cy.assignProjectToGlobalBadge(1, 3, 5)

        cy.enableGlobalBadge(1)

    });

    it('Global Badge users updates as user achieves levels and completes skills', () => {
        cy.visit('/administrator/globalBadges/globalBadge1/users');

        cy.get(tableSelector).contains('There are no records to show');

        let userId = 'user1';
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId })

        cy.visit('/administrator/globalBadges/globalBadge1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId }, { colIndex: 2,  value: '16%' }],
        ], 5);

        cy.doReportSkill({ project: 2, skill: 1, subjNum: 1, userId })
        cy.doReportSkill({ project: 2, skill: 2, subjNum: 1, userId })

        cy.visit('/administrator/globalBadges/globalBadge1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId }, { colIndex: 2,  value: '58%' }],
        ], 5);

        cy.doReportSkill({ project: 3, skill: 1, subjNum: 1, userId })

        cy.visit('/administrator/globalBadges/globalBadge1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId }, { colIndex: 2,  value: '83%' }],
        ], 5);

        cy.doReportSkill({ project: 3, skill: 2, subjNum: 1, userId })

        cy.visit('/administrator/globalBadges/globalBadge1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId }, { colIndex: 2,  value: '100%' }],
        ], 5);
    })

    it('Can sort multiple users by total progress', () => {
        cy.visit('/administrator/globalBadges/globalBadge1/users');
        cy.wait('@getUsers')

        cy.get(tableSelector).contains('There are no records to show');

        let userId = 'user1';
        let userId2 = 'user2';
        let userId3 = 'user3';
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: userId })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: userId2 })
        cy.doReportSkill({ project: 2, skill: 1, subjNum: 1, userId: userId2 })
        cy.doReportSkill({ project: 2, skill: 2, subjNum: 1, userId: userId3 })
        cy.doReportSkill({ project: 3, skill: 1, subjNum: 1, userId: userId3 })

        cy.visit('/administrator/globalBadges/globalBadge1/users');
        cy.wait('@getUsers')

        cy.get(`${tableSelector}`).contains('Badge Progress').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId }, { colIndex: 2,  value: '8%' }],
            [{ colIndex: 0,  value: userId2 }, { colIndex: 2,  value: '41%' }],
            [{ colIndex: 0,  value: userId3 }, { colIndex: 2,  value: '58%' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Badge Progress').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId3 }, { colIndex: 2,  value: '58%' }],
            [{ colIndex: 0,  value: userId2 }, { colIndex: 2,  value: '41%' }],
            [{ colIndex: 0,  value: userId }, { colIndex: 2,  value: '8%' }],
        ], 5);
    })

    it('Users that have achieved some level progress but no completions still appear', () => {
        cy.createProject(4)
        cy.createSubject(4, 1)
        cy.createSkill(4, 1, 1, {numPerformToCompletion : 10})
        cy.createSkill(4, 1, 2, {numPerformToCompletion : 10})

        cy.assignProjectToGlobalBadge(1, 4, 3)

        cy.visit('/administrator/globalBadges/globalBadge1/users');
        cy.wait('@getUsers')

        cy.get(tableSelector).contains('There are no records to show');

        let userId = 'user1';
        let userId2 = 'user2';
        let userId3 = 'user3';
        let userId4 = 'user4';
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: userId })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: userId2 })
        cy.doReportSkill({ project: 2, skill: 1, subjNum: 1, userId: userId2 })
        cy.doReportSkill({ project: 3, skill: 1, subjNum: 1, userId: userId3 })
        cy.doReportSkill({ project: 4, skill: 1, subjNum: 1, userId: userId4 })

        cy.visit('/administrator/globalBadges/globalBadge1/users');
        cy.wait('@getUsers')

        cy.get(`${tableSelector}`).contains('Badge Progress').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId4 }, { colIndex: 2,  value: '0%' }],
            [{ colIndex: 0,  value: userId }, { colIndex: 2,  value: '6%' }],
            [{ colIndex: 0,  value: userId3 }, { colIndex: 2,  value: '20%' }],
            [{ colIndex: 0,  value: userId2 }, { colIndex: 2,  value: '33%' }],
        ], 5);
    })

  it('Can filter users by user ID', () => {
      let userId = 'user1';
      let userId2 = 'user2';
      let userId3 = 'user3';
      cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: userId })
      cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: userId2 })
      cy.doReportSkill({ project: 2, skill: 1, subjNum: 1, userId: userId2 })
      cy.doReportSkill({ project: 2, skill: 2, subjNum: 1, userId: userId3 })
      cy.doReportSkill({ project: 3, skill: 1, subjNum: 1, userId: userId3 })

      cy.visit('/administrator/globalBadges/globalBadge1/users');
      cy.wait('@getUsers')

      cy.validateTable(tableSelector, [
          [{ colIndex: 0,  value: userId3 }],
          [{ colIndex: 0,  value: userId2 }],
          [{ colIndex: 0,  value: userId }],
      ], 5);

      cy.get('[data-cy="users-skillIdFilter"]').type('user1');
      cy.get('[data-cy="users-filterBtn"]').click();
      cy.wait('@getUsers')

      cy.validateTable(tableSelector, [
          [{ colIndex: 0,  value: userId }],
      ], 5);

      cy.get('[data-cy="users-resetBtn"]').click();
      cy.wait('@getUsers')

      cy.validateTable(tableSelector, [
          [{ colIndex: 0,  value: userId3 }],
          [{ colIndex: 0,  value: userId2 }],
          [{ colIndex: 0,  value: userId }],
      ], 5);

  })


    it('Can filter users by tag', () => {
        let userId = 'user1';
        let userId2 = 'user2';
        let userId3 = 'user3';

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });

        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: userId })
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: userId2 })
        cy.doReportSkill({ project: 2, skill: 1, subjNum: 1, userId: userId2 })
        cy.doReportSkill({ project: 2, skill: 2, subjNum: 1, userId: userId3 })
        cy.doReportSkill({ project: 3, skill: 1, subjNum: 1, userId: userId3 })

        const tagKey = 'dutyOrganization'
        cy.addUserTag(userId, tagKey, ['tag1'])
        cy.addUserTag(userId2, tagKey, ['tag2'])
        cy.addUserTag(userId3, tagKey, ['tag3'])

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });

        cy.visit('/administrator/globalBadges/globalBadge1/users');
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId3 }],
            [{ colIndex: 0,  value: userId2 }],
            [{ colIndex: 0,  value: userId }],
        ], 5);

        cy.get('[data-cy="users-userTagFilter"]').type('tag1');
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId }, { colIndex: 1,  value: 'tag1' }],
        ], 5);

        cy.get('[data-cy="users-resetBtn"]').click();
        cy.wait('@getUsers')

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: userId3 }],
            [{ colIndex: 0,  value: userId2 }],
            [{ colIndex: 0,  value: userId }],
        ], 5);

    })
})