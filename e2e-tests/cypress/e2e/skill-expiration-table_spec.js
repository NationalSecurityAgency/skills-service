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

    Cypress.Commands.add('configureExpiration', (skillNum = '1', numDays = 30, every=1, expirationType='YEARLY') => {
        const isRecurring = expirationType === 'YEARLY' || expirationType === 'MONTHLY'
        const m = isRecurring ? moment.utc().add(numDays, 'day') : null;
        cy.request('POST', `/admin/projects/proj1/skills/skill${skillNum}/expiration`, {
            expirationType: expirationType,
            every: every,
            monthlyDay: m ? m.date() : null,
            nextExpirationDate: m ? m.format('x') : null
        });
    });

    Cypress.Commands.add('expireSkills', () => {
        cy.logout();
        cy.resetEmail();

        cy.fixture('vars.json')
            .then((vars) => {
                cy.register(vars.rootUser, vars.defaultPass, true);
            });

        cy.login('root@skills.org', 'password');

        cy.request('POST', `/root/runSkillExpiration`);

        cy.logout();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.login(vars.rootUser, vars.defaultPass);
            });
    });

    it('Expired skills show up in table', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)

        cy.configureExpiration(1, 0, 1, 'DAILY');
        const yesterday = moment.utc().subtract(1, 'day')
        const twoDaysAgo = moment.utc().subtract(2, 'day')
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: yesterday.format('YYYY-MM-DD HH:mm') })
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser'), date: twoDaysAgo.format('YYYY-MM-DD HH:mm') })
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
                value: 'user0'
            }],
        ], 3);
    });

});