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
describe('Skills Table Tests', () => {
    const tableSelector = '[data-cy="skillsTable"]'

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
    });

    it('create first skill then remove it', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.contains('No Skills Yet');

        const skillName = 'This is a Skill'
        cy.get('[data-cy="newSkillButton"').click();
        cy.get('[data-cy="skillName"]').type(skillName)
        cy.clickSave();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: skillName }, { colIndex: 2,  value: 1 }],
        ], 10);

        cy.get('[data-cy="deleteSkillButton_ThisisaSkillSkill"]').click();
        cy.acceptRemovalSafetyCheck();
        cy.contains('No Skills Yet');
    });

    it('copy existing skill', () => {
        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/copy_of_skill2').as('saveSkill');
        cy.intercept('POST', '/api/validation/description').as('validateDescription');

        const numSkills = 3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                description: 'generic description',
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter < 3 ? '1' : '200',
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // force the order
        cy.contains('Display Order').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 1' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: 'Very Great Skill # 2' }, { colIndex: 1,  value: 2 }],
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }, { colIndex: 1,  value: 3 }],
        ], 10);

        cy.get('[data-cy="copySkillButton_skill2"]').click();
        cy.get('#markdown-editor textarea').should('have.value', 'generic description');
        cy.get('[data-cy=skillName]').should('have.value', 'Copy of Very Great Skill # 2');
        cy.get('#idInput').should('have.value', 'copy_of_skill2');
        cy.get('[data-cy=numPerformToCompletion]').should('have.value', '1');
        cy.get('[data-cy=skillPointIncrement]').should('have.value', '150');
        cy.get('#markdown-editor textarea').type('{selectall}copy description edit');
        cy.wait('@validateDescription');
        cy.get('[data-cy=numPerformToCompletion]').type('5');
        cy.clickSave();
        cy.wait('@saveSkill');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 1' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: 'Very Great Skill # 2' }, { colIndex: 1,  value: 2 }],
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }, { colIndex: 1,  value: 3 }],
            [{ colIndex: 0,  value: 'Copy of Very Great Skill # 2' }, { colIndex: 1,  value: 4}],
        ], 10);
    });

    it('edit existing skill', () => {

        cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/skill2').as('saveSkill');

        const numSkills = 3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: `Very Great Skill # ${skillsCounter}`,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter < 3 ? '1' : '200',
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // force the order
        cy.contains('Display Order').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 1' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: 'Very Great Skill # 2' }, { colIndex: 1,  value: 2 }],
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }, { colIndex: 1,  value: 3 }],
        ], 10);

        cy.get('[data-cy="editSkillButton_skill2"]').click();
        const otherSkillName = 'Other Skill';
        cy.get('[data-cy="skillName"]').clear().type(otherSkillName);
        cy.clickSave();
        cy.wait('@saveSkill');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Very Great Skill # 1' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: otherSkillName }, { colIndex: 1,  value: 2 }],
            [{ colIndex: 0,  value: 'Very Great Skill # 3' }, { colIndex: 1,  value: 3 }],
        ], 10);
    });

    it('sort by skill and order', () => {

        const numSkills = 13;
        const expected = [];
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            expected.push([{ colIndex: 0,  value: skillName }, { colIndex: 1,  value: skillsCounter }])
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter < 3 ? '1' : '200',
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // test skill name sorting
        cy.get(`${tableSelector} th`).contains('Skill').click();
        cy.validateTable(tableSelector, expected, 10);

        cy.get(`${tableSelector} th`).contains('Skill').click();
        cy.validateTable(tableSelector, expected.map((item) => item).reverse(), 10);

        cy.get(`${tableSelector} th`).contains('Display Order').click();
        cy.validateTable(tableSelector, expected, 10);


        cy.get(`${tableSelector} th`).contains('Display Order').click();
        cy.validateTable(tableSelector, expected.map((item) => item).reverse(), 10);
    });

    it('sort by created date', () => {

        const numSkills = 3;
        const expected = [];
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            expected.push([{ colIndex: 0,  value: skillName }, { colIndex: 1,  value: skillsCounter }])
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter,
            });
            cy.wait(1001);
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // test created column
        cy.get(`${tableSelector} th`).contains('Created').click();
        cy.validateTable(tableSelector, expected, 10);

        cy.get(`${tableSelector} th`).contains('Created').click();
        cy.validateTable(tableSelector, expected.map((item) => item).reverse(), 10);
    });

    it('sort by additional fields', () => {

        const numSkills = 3;
        const expected = [];
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            expected.push([{ colIndex: 0,  value: skillName }, { colIndex: 1,  value: skillsCounter }])
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter,
                version: skillsCounter,
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // test points column
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Points').click();
        cy.get(`${tableSelector} th`).contains('Points').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 3,  value: 150 }],
            [{ colIndex: 0,  value: 'Skill # 2' }, { colIndex: 3,  value: 300 }],
            [{ colIndex: 0,  value: 'Skill # 3' }, { colIndex: 3,  value: 450 }],
        ], 10);

        // test points column
        cy.get(`${tableSelector} th`).contains('Points').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 3' }, { colIndex: 3,  value: 450 }],
            [{ colIndex: 0,  value: 'Skill # 2' }, { colIndex: 3,  value: 300 }],
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 3,  value: 150 }],
        ], 10);

        // test version column
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Version').click();
        cy.get(`${tableSelector} th`).contains('Version').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 4,  value: 1 }],
            [{ colIndex: 0,  value: 'Skill # 2' }, { colIndex: 4,  value: 2 }],
            [{ colIndex: 0,  value: 'Skill # 3' }, { colIndex: 4,  value: 3 }],
        ], 10);

        cy.get(`${tableSelector} th`).contains('Version').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 3' }, { colIndex: 4,  value: 3 }],
            [{ colIndex: 0,  value: 'Skill # 2' }, { colIndex: 4,  value: 2 }],
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 4,  value: 1 }],
        ], 10);
    });

    it('Time Window field formatting', () => {

        const numSkills = 4;
        const expected = [];
        for (let skillsCounter = 0; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            expected.push([{ colIndex: 0,  value: skillName }, { colIndex: 1,  value: skillsCounter }])
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                pointIncrementInterval: 30 * (skillsCounter),
                numPerformToCompletion: skillsCounter <= 1 ? 1 : skillsCounter,
                version: skillsCounter + 1,
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // test points column
        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Time Window').click();
        cy.get(`${tableSelector} th`).contains('Display Order').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 0' }, { colIndex: 3,  value: 'Time Window Disabled' }],
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 3,  value: 'Time Window N/A' }],
            [{ colIndex: 0,  value: 'Skill # 2' }, { colIndex: 3,  value: '1 Hour' }],
            [{ colIndex: 0,  value: 'Skill # 3' }, { colIndex: 3,  value: '1 Hour 30 Minutes' }],
            [{ colIndex: 0,  value: 'Skill # 4' }, { colIndex: 3,  value: '2 Hours' }],
        ], 10);
      });

    it('Self Reporting Type additional field', () => {
        cy.createSkill(1, 1, 1, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 2, { selfReportingType: 'HonorSystem' });
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Self Report').click();
        cy.get(`${tableSelector} th`).contains('Self Report Type').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 3,  value: 'Approval' }],
            [{ colIndex: 3,  value: 'Approval' }],
            [{ colIndex: 0,  value: 'Very Great Skill 3' }, { colIndex: 3,  value: 'Disabled' }],
            [{ colIndex: 3,  value: 'Honor System' }],
            [{ colIndex: 3,  value: 'Honor System' }],
        ], 10);
    });

    it('display Disabled for self reporting type for a new (non self-reporting) skill', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newSkillButton"]').click();
        cy.get('[data-cy="skillName"]').type('Disabled Test')
        cy.clickSave();

        cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Self Report').click();
        cy.get(`${tableSelector} th`).contains('Self Report Type').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Disabled Test' }, { colIndex: 3,  value: 'Disabled' }],
        ], 10);
    });

    it('change display order', () => {
        const numSkills = 4;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter,
                version: skillsCounter,
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        // sort is enabled when sorted by display order column
        cy.get('[data-cy="orderMoveUp_skill1"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_skill2"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_skill3"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_skill4"]').should('be.disabled');
        cy.get('[data-cy="orderMoveDown_skill1"]').should('be.disabled');
        cy.get('[data-cy="orderMoveDown_skill2"]').should('be.disabled');
        cy.get('[data-cy="orderMoveDown_skill3"]').should('be.disabled');
        cy.get('[data-cy="orderMoveDown_skill4"]').should('be.disabled');

        cy.get(`${tableSelector} th`).contains('Display Order').click();
        cy.get('[data-cy="orderMoveUp_skill1"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_skill2"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_skill3"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_skill4"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill2"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill3"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill4"]').should('be.disabled');

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: 'Skill # 2' }, { colIndex: 1,  value: 2 }],
            [{ colIndex: 0,  value: 'Skill # 3' }, { colIndex: 1,  value: 3 }],
            [{ colIndex: 0,  value: 'Skill # 4' }, { colIndex: 1,  value: 4 }],
        ], 10);

        cy.get('[data-cy="orderMoveUp_skill3"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: 'Skill # 3' }, { colIndex: 1,  value: 2 }],
            [{ colIndex: 0,  value: 'Skill # 2' }, { colIndex: 1,  value: 3 }],
            [{ colIndex: 0,  value: 'Skill # 4' }, { colIndex: 1,  value: 4 }],
        ], 10);

        cy.get('[data-cy="orderMoveDown_skill1"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 3' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 1,  value: 2 }],
            [{ colIndex: 0,  value: 'Skill # 2' }, { colIndex: 1,  value: 3 }],
            [{ colIndex: 0,  value: 'Skill # 4' }, { colIndex: 1,  value: 4 }],
        ], 10);
        cy.get('[data-cy="orderMoveUp_skill1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_skill2"]').should('be.enabled');
        cy.get('[data-cy="orderMoveUp_skill3"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_skill4"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill2"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill3"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill4"]').should('be.disabled');
    })

    it('change display order and validate that manage skill navigation still works', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`${tableSelector} th`).contains('Display Order').click();
        cy.get('[data-cy="orderMoveUp_skill3"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'skill1' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: 'skill3' }, { colIndex: 1,  value: 2 }],
            [{ colIndex: 0,  value: 'skill2' }, { colIndex: 1,  value: 3 }],
        ], 10);

        cy.get('[data-cy="manageSkillBtn_skill3"]').click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill3');
    })

    it('change display order with the last item on the current page', () => {
        const numSkills = 12;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter,
                version: skillsCounter,
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`${tableSelector} th`).contains('Display Order').click();
        cy.get('[data-cy="orderMoveDown_skill10"]').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: 'Skill # 2' }, { colIndex: 1,  value: 2 }],
            [{ colIndex: 0,  value: 'Skill # 3' }, { colIndex: 1,  value: 3 }],
            [{ colIndex: 0,  value: 'Skill # 4' }, { colIndex: 1,  value: 4 }],
            [{ colIndex: 0,  value: 'Skill # 5' }, { colIndex: 1,  value: 5 }],
            [{ colIndex: 0,  value: 'Skill # 6' }, { colIndex: 1,  value: 6 }],
            [{ colIndex: 0,  value: 'Skill # 7' }, { colIndex: 1,  value: 7 }],
            [{ colIndex: 0,  value: 'Skill # 8' }, { colIndex: 1,  value: 8 }],
            [{ colIndex: 0,  value: 'Skill # 9' }, { colIndex: 1,  value: 9 }],
            [{ colIndex: 0,  value: 'Skill # 11' }, { colIndex: 1,  value: 10 }],
            [{ colIndex: 0,  value: 'Skill # 10' }, { colIndex: 1,  value: 11 }],
            [{ colIndex: 0,  value: 'Skill # 12' }, { colIndex: 1,  value: 12 }],
        ], 10);
    })

    it('filter by skill name and skill id', () => {
        const numSkills = 12;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter,
                version: skillsCounter,
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get(`${tableSelector} th`).contains('Display Order').click();

        // look for the name
        cy.get('[data-cy="skillsTable-skillFilter"]').type('# 1')
        cy.get('[data-cy="users-filterBtn"]').click();

        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 1' }, { colIndex: 1,  value: 1 }],
            [{ colIndex: 0,  value: 'Skill # 10' }, { colIndex: 1,  value: 10 }],
            [{ colIndex: 0,  value: 'Skill # 11' }, { colIndex: 1,  value: 11 }],
            [{ colIndex: 0,  value: 'Skill # 12' }, { colIndex: 1,  value: 12 }],
        ], 10);

        cy.get('[data-cy="skillsTable-skillFilter"]').type('2')
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 12' }, { colIndex: 1,  value: 12 }],
        ], 10);

        cy.get('[data-cy="users-resetBtn"]').click();
        cy.get('[data-cy=skillsBTableTotalRows]').contains(12);

        // should be case insensitive
        cy.get('[data-cy="skillsTable-skillFilter"]').type('sKiLl # 5')
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 5' }, { colIndex: 1,  value: 5 }],
        ], 10);

        // filter all records
        cy.get('[data-cy="skillsTable-skillFilter"]').type('a')
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.contains('There are no records to show')

        // reset list by clearing filter
        cy.get('[data-cy="skillsTable-skillFilter"]').clear()
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.get('[data-cy=skillsBTableTotalRows]').contains(12);

        // filter by skill id
        cy.get('[data-cy="skillsTable-skillFilter"]').type('SkIlL6')
        cy.get('[data-cy="users-filterBtn"]').click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 0,  value: 'Skill # 6' }, { colIndex: 1,  value: 6 }],
        ], 10);
    });

    it('expand details', () => {
        const numSkills = 3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter,
                version: skillsCounter,
            });
        };

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="expandDetailsBtn_skill2"]').click();
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('Minimum Time Window between occurrences');
        cy.get('[data-cy="childRowDisplay_skill2"]').contains('300 Points');

        cy.get('[data-cy="expandDetailsBtn_skill1"]').click();
        cy.get('[data-cy="childRowDisplay_skill1"]').contains('150 Points');
        cy.get('[data-cy="childRowDisplay_skill1"]').contains('Time Window N/A');

        cy.get('[data-cy="expandDetailsBtn_skill2"]').click();
        cy.get('[data-cy="childRowDisplay_skill2"]').should('not.exist');

        cy.get('[data-cy="childRowDisplay_skill1"]').contains('150 Points');
        cy.get('[data-cy="expandDetailsBtn_skill1"]').click();
        cy.get('[data-cy="childRowDisplay_skill1"]').should('not.exist');
    });

    it('navigate to skill details page', () => {
        const numSkills = 3;
        for (let skillsCounter = 1; skillsCounter <= numSkills; skillsCounter += 1) {
            const skillName = `Skill # ${skillsCounter}`;
            cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill${skillsCounter}`, {
                projectId: 'proj1',
                subjectId: 'subj1',
                skillId: `skill${skillsCounter}`,
                name: skillName,
                pointIncrement: '150',
                numPerformToCompletion: skillsCounter,
                version: skillsCounter,
            });
        };
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy=manageSkillBtn_skill2]').click();
        cy.contains('ID: skill2');
        cy.contains('Overview');
        cy.contains('300 Points');

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy=manageSkillLink_skill3]').click();
        cy.contains('ID: skill3');
        cy.contains('Overview');
        cy.contains('450 Points');

    });

    it('long skill id row controls wrap to the right', () => {
        cy.log('creating new skill');
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/MaximumWidthPainBreakingControlLayoutForReal1Skill', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "MaximumWidthPainBreakingControlLayoutForReal1Skill",
            name: "MaximumWidthPainBreakingControlLayoutForReal1 1",
            pointIncrement: '50',
            numPerformToCompletion: '5'
        });
        cy.intercept('GET', '/admin/projects/proj1/subjects/subj1/skills').as('loadSkills');

        cy.log('visiting subj1 page');
        cy.viewport(1400, 900);
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@loadSkills');

        //wait for loading
        cy.log('matchSnapshot');
        cy.matchSnapshotImageForElement('.skills-b-table tbody tr:first-of-type td:first-of-type', 'skillsTableFullsize')
        cy.viewport(1200, 900);
        cy.wait(400);
        cy.matchSnapshotImageForElement('.skills-b-table tbody tr:first-of-type td:first-of-type', 'skillsTableSmaller')
    });
});

