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

describe('Performed Skills Table Tests', () => {
    const tableSelector = '[data-cy=performedSkillsTable]'
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
            };
        });

        Cypress.Commands.add('report', (num, sameSkill = true) => {
            for (let i = 0; i < num; i += 1) {
                cy.request('POST', `/api/projects/proj1/skills/skill${sameSkill ? '1' : i + 1}`, {userId: `user1@skills.org`, timestamp: m.clone().add(i, 'day').format('x')})
            }
        });

    });

    it('sort by date', () => {
        cy.createSkills(1);
        cy.report(7);
        cy.visit('/administrator/projects/proj1/subjects/subj1/users/user1@skills.org/skillEvents');
        // default sort by date desc
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-18' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-17' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-16' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-15' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-14' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-13' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-12' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Performed On').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-12' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-13' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-14' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-15' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-16' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-17' }],
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: '2020-09-18' }],
        ], 5);
    });


    it('sort by skill id', () => {
        cy.createSkills(7);
        cy.report(7, false);
        cy.visit('/administrator/projects/proj1/subjects/subj1/users/user1@skills.org/skillEvents');

        cy.get(`${tableSelector}`).contains('Skill Id').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill1' }],
            [{ colIndex: 0,  value: 'skill2' }],
            [{ colIndex: 0,  value: 'skill3' }],
            [{ colIndex: 0,  value: 'skill4' }],
            [{ colIndex: 0,  value: 'skill5' }],
            [{ colIndex: 0,  value: 'skill6' }],
            [{ colIndex: 0,  value: 'skill7' }],
        ], 5);

        cy.get(`${tableSelector}`).contains('Skill Id').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill7' }],
            [{ colIndex: 0,  value: 'skill6' }],
            [{ colIndex: 0,  value: 'skill5' }],
            [{ colIndex: 0,  value: 'skill4' }],
            [{ colIndex: 0,  value: 'skill3' }],
            [{ colIndex: 0,  value: 'skill2' }],
            [{ colIndex: 0,  value: 'skill1' }],
        ], 5);
    });


    it('filter by skill id', () => {
        cy.createSkills(12);
        cy.report(12, false);
        cy.visit('/administrator/projects/proj1/subjects/subj1/users/user1@skills.org/skillEvents');

        cy.get('[data-cy="performedSkills-skillIdFilter"]').type('sKiLl1');
        cy.get('[data-cy="performedSkills-filterBtn"]').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill12' }],
            [{ colIndex: 0,  value: 'skill11' }],
            [{ colIndex: 0,  value: 'skill10' }],
            [{ colIndex: 0,  value: 'skill1' }],
        ], 5);

        cy.get('[data-cy="performedSkills-skillIdFilter"]').type('0');
        cy.get('[data-cy="performedSkills-filterBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill10' }],
        ], 5);

        cy.get('[data-cy="performedSkills-resetBtn"]').click();
        cy.get('[data-cy=skillsBTableTotalRows]').contains(12);

        cy.get('[data-cy="performedSkills-skillIdFilter"]').type('L2');
        cy.get('[data-cy="performedSkills-filterBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill2' }],
        ], 5);
    });


    it('delete skill event', () => {
        cy.createSkills(3);
        cy.report(3, false);
        cy.visit('/administrator/projects/proj1/subjects/subj1/users/user1@skills.org/skillEvents');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill3' }],
            [{ colIndex: 0,  value: 'skill2' }],
            [{ colIndex: 0,  value: 'skill1' }],
        ], 5);

        cy.get('[data-cy="deleteEventBtn"]').should('have.length', 3).as('deleteBtns');
        cy.get('@deleteBtns').eq(1).click();
        cy.contains('Removing skill [skill2]');
        cy.contains('YES, Delete It!').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill3' }],
            [{ colIndex: 0,  value: 'skill1' }],
        ], 5);

    });


});

