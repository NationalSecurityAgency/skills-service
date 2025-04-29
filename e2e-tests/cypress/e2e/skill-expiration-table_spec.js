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

const moment = require("moment-timezone");
describe('Expired Skill Table Tests', () => {
    let proxyUser, userToValidate;
    before(() => {
        proxyUser = Cypress.env('proxyUser');
        userToValidate = Cypress.env('oauthMode') ? 'foo' : proxyUser;
    })
    
    it('Expired skills show up in table', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.configureExpiration(1, 0, 1, 'DAILY');
        const yesterday = moment.utc().subtract(1, 'day')
        const twoDaysAgo = moment.utc().subtract(2, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: proxyUser, date: yesterday.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: proxyUser, date: twoDaysAgo.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: "user1", date: yesterday.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: "user1", date: twoDaysAgo.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: "user2", date: yesterday.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: "user2", date: twoDaysAgo.format('YYYY-MM-DD HH:mm') })

        cy.expireSkills();

        cy.visit('/administrator/projects/proj1/expirationHistory');

        const tableSelector = '[data-cy=expirationHistoryTable]';
        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'user2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: 'user1'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 1,
                value: userToValidate
            }],
        ], 3);
    });

    it('Sort and page table', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.configureExpiration(1, 0, 1, 'DAILY');
        let yesterday = moment.utc().subtract(1, 'day')
        let twoDaysAgo = moment.utc().subtract(2, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: proxyUser, date: yesterday.format('YYYY-MM-DD HH:mm') });
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: proxyUser, date: twoDaysAgo.format('YYYY-MM-DD HH:mm') });

        for(let x = 1; x < 20; x++) {
            const user = "user" + x;
            yesterday = moment.utc().subtract(1, 'day')
            twoDaysAgo = moment.utc().subtract(2, 'day')
            cy.doReportSkill({
                project: 1,
                skill: 1,
                subjNum: 1,
                userId: user,
                date: yesterday.format('YYYY-MM-DD HH:mm')
            })
            cy.doReportSkill({
                project: 1,
                skill: 1,
                subjNum: 1,
                userId: user,
                date: twoDaysAgo.format('YYYY-MM-DD HH:mm')
            })
        };

        cy.expireSkills();

        cy.visit('/administrator/projects/proj1/expirationHistory');

        const tableSelector = '[data-cy=expirationHistoryTable]';
        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user9'
            }],
            [{
                colIndex: 1,
                value: 'user8'
            }],
            [{
                colIndex: 1,
                value: 'user7'
            }],
            [{
                colIndex: 1,
                value: 'user6'
            }],
            [{
                colIndex: 1,
                value: 'user5'
            }],
            [{
                colIndex: 1,
                value: 'user4'
            }],
            [{
                colIndex: 1,
                value: 'user3'
            }],
            [{
                colIndex: 1,
                value: 'user2'
            }],
            [{
                colIndex: 1,
                value: 'user19'
            }],
            [{
                colIndex: 1,
                value: 'user18'
            }],
        ], 10, true, 20);

        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector).contains('User').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: userToValidate
            }],
            [{
                colIndex: 1,
                value: 'user1'
            }],
            [{
                colIndex: 1,
                value: 'user10'
            }],
            [{
                colIndex: 1,
                value: 'user11'
            }],
            [{
                colIndex: 1,
                value: 'user12'
            }],
            [{
                colIndex: 1,
                value: 'user13'
            }],
            [{
                colIndex: 1,
                value: 'user14'
            }],
            [{
                colIndex: 1,
                value: 'user15'
            }],
            [{
                colIndex: 1,
                value: 'user16'
            }],
            [{
                colIndex: 1,
                value: 'user17'
            }],
        ], 10, true, 20);

        cy.get(headerSelector).contains('User').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user9'
            }],
            [{
                colIndex: 1,
                value: 'user8'
            }],
            [{
                colIndex: 1,
                value: 'user7'
            }],
            [{
                colIndex: 1,
                value: 'user6'
            }],
            [{
                colIndex: 1,
                value: 'user5'
            }],
            [{
                colIndex: 1,
                value: 'user4'
            }],
            [{
                colIndex: 1,
                value: 'user3'
            }],
            [{
                colIndex: 1,
                value: 'user2'
            }],
            [{
                colIndex: 1,
                value: 'user19'
            }],
            [{
                colIndex: 1,
                value: 'user18'
            }],
        ], 10, true, 20);

        cy.get('[data-pc-section="page"]').contains('2').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user17'
            }],
            [{
                colIndex: 1,
                value: 'user16'
            }],
            [{
                colIndex: 1,
                value: 'user15'
            }],
            [{
                colIndex: 1,
                value: 'user14'
            }],
            [{
                colIndex: 1,
                value: 'user13'
            }],
            [{
                colIndex: 1,
                value: 'user12'
            }],
            [{
                colIndex: 1,
                value: 'user11'
            }],
            [{
                colIndex: 1,
                value: 'user10'
            }],
            [{
                colIndex: 1,
                value: 'user1'
            }],
            [{
                colIndex: 1,
                value: userToValidate
            }],
        ], 10, true, 20);
    });

    it('Filter table', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.configureExpiration(1, 0, 1, 'DAILY');
        let yesterday = moment.utc().subtract(1, 'day')
        let twoDaysAgo = moment.utc().subtract(2, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: proxyUser, date: yesterday.format('YYYY-MM-DD HH:mm') });
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: proxyUser, date: twoDaysAgo.format('YYYY-MM-DD HH:mm') });

        for(let x = 1; x < 5; x++) {
            const user = "user" + x;
            yesterday = moment.utc().subtract(1, 'day')
            twoDaysAgo = moment.utc().subtract(2, 'day')
            cy.doReportSkill({
                project: 1,
                skill: 1,
                subjNum: 1,
                userId: user,
                date: yesterday.format('YYYY-MM-DD HH:mm')
            })
            cy.doReportSkill({
                project: 1,
                skill: 1,
                subjNum: 1,
                userId: user,
                date: twoDaysAgo.format('YYYY-MM-DD HH:mm')
            })
        };

        cy.expireSkills();

        cy.visit('/administrator/projects/proj1/expirationHistory');

        const tableSelector = '[data-cy=expirationHistoryTable]';
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector).contains('User').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: userToValidate
            }],
            [{
                colIndex: 1,
                value: 'user1'
            }],
            [{
                colIndex: 1,
                value: 'user2'
            }],
            [{
                colIndex: 1,
                value: 'user3'
            }],
            [{
                colIndex: 1,
                value: 'user4'
            }],
        ], 5);

        cy.get('[data-cy=userIdFilter]').type('user4')

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: 'user4'
            }],
        ], 1);

        cy.get('[data-cy=userIdFilter]').clear()

        cy.validateTable(tableSelector, [
            [{
                colIndex: 1,
                value: userToValidate
            }],
            [{
                colIndex: 1,
                value: 'user1'
            }],
            [{
                colIndex: 1,
                value: 'user2'
            }],
            [{
                colIndex: 1,
                value: 'user3'
            }],
            [{
                colIndex: 1,
                value: 'user4'
            }],
        ], 5);
    });

    it('Sort by skill name', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)
        cy.createSkill(1, 1, 3)

        cy.configureExpiration(1, 0, 1, 'DAILY');
        cy.configureExpiration(2, 0, 1, 'DAILY');
        cy.configureExpiration(3, 0, 1, 'DAILY');
        let yesterday = moment.utc().subtract(1, 'day')
        let twoDaysAgo = moment.utc().subtract(2, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: proxyUser, date: yesterday.format('YYYY-MM-DD HH:mm') });
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: proxyUser, date: twoDaysAgo.format('YYYY-MM-DD HH:mm') });
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: proxyUser, date: yesterday.format('YYYY-MM-DD HH:mm') });
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: proxyUser, date: twoDaysAgo.format('YYYY-MM-DD HH:mm') });
        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: proxyUser, date: yesterday.format('YYYY-MM-DD HH:mm') });
        cy.doReportSkill({ project: 1, skill: 3, subjNum: 1, userId: proxyUser, date: twoDaysAgo.format('YYYY-MM-DD HH:mm') });

        cy.expireSkills();

        cy.visit('/administrator/projects/proj1/expirationHistory');

        const tableSelector = '[data-cy=expirationHistoryTable]';
        const headerSelector = `${tableSelector} thead tr th`;
        cy.get(headerSelector).contains('Skill Name').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 3'
            }],
        ], 3);

        cy.get(headerSelector).contains('Skill Name').click();

        cy.validateTable(tableSelector, [
            [{
                colIndex: 0,
                value: 'Very Great Skill 3'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 0,
                value: 'Very Great Skill 1'
            }],
        ], 3);
    });
});