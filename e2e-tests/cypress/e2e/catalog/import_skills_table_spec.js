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
var moment = require('moment-timezone');

describe('Import From Catalog Table Tests', () => {

    const tableSelector = '[data-cy="importSkillsFromCatalogTable"]';

    before(() => {
        Cypress.env('disableResetDb', true);
        cy.resetDb();
        cy.fixture('vars.json')
            .then((vars) => {
                cy.logout();

                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSubject(1, 2);

        // mix skill names since it's sorted by skillId - this will force different projects in the first page
        cy.createSkill(1, 1, 1, {
            pointIncrement: 10,
            numPerformToCompletion: 8,
            description: 'This is the first skill and it is cool'
        });
        cy.createSkill(1, 2, 6, {
            pointIncrement: 15,
            numPerformToCompletion: 7
        });
        cy.createSkill(1, 1, 7, {
            pointIncrement: 20,
            numPerformToCompletion: 6
        });
        cy.createSkill(1, 2, 4, {
            pointIncrement: 25,
            numPerformToCompletion: 5,
            description: '### Title'
        });
        cy.createSkill(1, 1, 5, {
            pointIncrement: 30,
            numPerformToCompletion: 4
        });

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSubject(2, 3);
        cy.createSkill(2, 1, 2, {
            pointIncrement: 35,
            numPerformToCompletion: 3,
            selfReportingType: 'Approval'
        });
        cy.createSkill(2, 3, 3, {
            pointIncrement: 40,
            numPerformToCompletion: 2,
            selfReportingType: 'HonorSystem'
        });
        cy.createSkill(2, 3, 8, {
            pointIncrement: 45,
            numPerformToCompletion: 1
        });

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 2, 6);
        cy.exportSkillToCatalog(1, 1, 7);
        cy.exportSkillToCatalog(1, 2, 4);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.exportSkillToCatalog(2, 1, 2); // proj 2
        cy.exportSkillToCatalog(2, 3, 3); // proj 2
        cy.exportSkillToCatalog(2, 3, 8); // proj 2

        cy.createProject(3);
        cy.createSubject(3, 1);

        Cypress.Commands.add('validateNoSkillsSelected', (numSkills = 5) => {
            for (let i= 0; i <numSkills ; i++) {
                cy.get(`[data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
            }
        });

    });
    after(() => {
        Cypress.env('disableResetDb', false);
    });

    it('select a couple skills', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');

        cy.get('[data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"] input').should('not.be.checked')
        cy.get('[data-p-index="1"] [data-pc-name="pcrowcheckbox"] input').should('be.checked')
        cy.get('[data-p-index="2"] [data-pc-name="pcrowcheckbox"] input').should('not.be.checked')
        cy.get('[data-p-index="3"] [data-pc-name="pcrowcheckbox"] input').should('be.checked')
        cy.get('[data-p-index="4"] [data-pc-name="pcrowcheckbox"] input').should('not.be.checked')

        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '2');
        cy.get('[data-cy="importBtn"]').should('be.enabled');
    });

    it('select pages of skills then clear', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"] input').should('be.checked')
        cy.get('[data-p-index="1"] [data-pc-name="pcrowcheckbox"] input').should('be.checked')
        cy.get('[data-p-index="2"] [data-pc-name="pcrowcheckbox"] input').should('be.checked')
        cy.get('[data-p-index="3"] [data-pc-name="pcrowcheckbox"] input').should('be.checked')
        cy.get('[data-p-index="4"] [data-pc-name="pcrowcheckbox"] input').should('be.checked')

        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '5');
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.validateNoSkillsSelected();
    });

    it('change page size then select the page', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-pc-group-section="pagedropdown"]').click()
        cy.get('[data-pc-section="list"] [aria-label="10"]').click()
        cy.validateNoSkillsSelected(8)
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()

        for (let i= 0; i < 8 ; i++) {
            cy.get(`[data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }

        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '8');
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click()
        cy.validateNoSkillsSelected();
    });

    it('filter by skill name', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');

        cy.get('[data-cy="skillNameFilter"]')
            .type('  sUbJ2  ');
        cy.get('[data-cy="filterBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '2');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 4 Subj2'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 6 Subj2'
            }],
        ], 5);

        cy.get('[data-cy="filterResetBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');

        // now test it via enter
        cy.get('[data-cy="skillNameFilter"]')
            .type('  sKILL 4  {enter}');
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '1');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 4 Subj2'
            }],
        ], 5);
    });

    it('filter by project name', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');

        cy.get('[data-cy="projectNameFilter"]')
            .type('  jEcT 2  ');
        cy.get('[data-cy="filterBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '3');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 3,
                value: 'This is project 2'
            }],
            [{
                colIndex: 3,
                value: 'This is project 2'
            }],
            [{
                colIndex: 3,
                value: 'This is project 2'
            }],
        ], 5);

        cy.get('[data-cy="filterResetBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');

        cy.get('[data-cy="projectNameFilter"]')
            .type('project 1 {enter}');
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '5');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 3,
                value: 'This is project 1'
            }],
            [{
                colIndex: 3,
                value: 'This is project 1'
            }],
            [{
                colIndex: 3,
                value: 'This is project 1'
            }],
            [{
                colIndex: 3,
                value: 'This is project 1'
            }],
            [{
                colIndex: 3,
                value: 'This is project 1'
            }],
        ], 5);
    });

    it('filter by subject name', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');

        cy.get('[data-cy="subjectNameFilter"]')
            .type('  jEcT 2  ');
        cy.get('[data-cy="filterBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '2');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 4,
                value: 'Subject 2'
            }],
            [{
                colIndex: 4,
                value: 'Subject 2'
            }],
        ], 5);

        cy.get('[data-cy="filterResetBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');

        cy.get('[data-cy="subjectNameFilter"]')
            .type('subject 3 {enter}');
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '2');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 4,
                value: 'Subject 3'
            }],
            [{
                colIndex: 4,
                value: 'Subject 3'
            }],
        ], 5);
    });

    it('filter by project name, skill name and subject name', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');

        cy.get('[data-cy="skillNameFilter"]')
            .type('skill 1');
        cy.get('[data-cy="projectNameFilter"]')
            .type('project 1');
        cy.get('[data-cy="subjectNameFilter"]')
            .type('subject 1');
        cy.get('[data-cy="filterBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '1');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Skill 1'
            }, {
                colIndex: 3,
                value: 'project 1'
            }, {
                colIndex: 4,
                value: 'Subject 1'
            }],
        ], 5);
    });

    it('special characters in filters do not cause issues', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');

        cy.get('[data-cy="skillNameFilter"]')
            .type('!@#$%^&*()_+~`-=');
        cy.get('[data-cy="projectNameFilter"]')
            .type('!@#$%^&*()_+~`-=');
        cy.get('[data-cy="subjectNameFilter"]')
            .type('!@#$%^&*()_+~`-=');
        cy.get('[data-cy="filterBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '0');
        cy.get(tableSelector)
            .contains('There are no records to show');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="filterResetBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');
    });

    it('filter input must have max length so it does not exceed max url length', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        const longValue = Array(60)
            .fill('a')
            .join('');
        cy.get('[data-cy="skillNameFilter"]')
            .type(longValue);
        cy.get('[data-cy="projectNameFilter"]')
            .type(longValue);
        cy.get('[data-cy="subjectNameFilter"]')
            .type(longValue);
        cy.get('[data-cy="filterBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '0');

        // max length is hard coded to 50
        const longValue_50char = Array(50)
            .fill('a')
            .join('');
        cy.get('[data-cy="skillNameFilter"]')
            .should('have.value', longValue_50char);
        cy.get('[data-cy="projectNameFilter"]')
            .should('have.value', longValue_50char);
        cy.get('[data-cy="subjectNameFilter"]')
            .should('have.value', longValue_50char);
    });

    it('expand skill details', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '8');
        cy.get('[data-p-index="0"] [data-pc-section="rowtogglebutton"]').click()
        cy.get('[data-cy="skillToImportInfo-proj1_skill1"]')
            .contains('Self Report: N/A');
        cy.get('[data-cy="skillToImportInfo-proj1_skill1"]')
            .contains(`${moment()
                .format('YYYY-MM-DD')}`);
        cy.get('[data-cy="skillToImportInfo-proj1_skill1"] [data-cy="importedSkillInfoDescription"]')
            .contains('This is the first skill and it is cool');
        cy.get('[data-cy="skillToImportInfo-proj1_skill1"] [data-cy="projId"]')
            .should('have.text', 'proj1');
        cy.get('[data-cy="skillToImportInfo-proj1_skill1"] [data-cy="skillId"]')
            .should('have.text', 'skill1');
        cy.get('[data-cy="skillToImportInfo-proj1_skill1"] [data-cy="totalPts"]')
            .should('have.text', '80');

        cy.get('[data-p-index="1"] [data-pc-section="rowtogglebutton"]').click()
        cy.get('[data-cy="skillToImportInfo-proj2_skill2"]')
            .contains('Self Report: Requires Approval');
        cy.get('[data-cy="skillToImportInfo-proj2_skill2"] [data-cy="importedSkillInfoDescription"]')
            .should('not.exist');
        cy.get('[data-cy="skillToImportInfo-proj2_skill2"] [data-cy="projId"]')
            .should('have.text', 'proj2');
        cy.get('[data-cy="skillToImportInfo-proj2_skill2"] [data-cy="skillId"]')
            .should('have.text', 'skill2');
        cy.get('[data-cy="skillToImportInfo-proj2_skill2"] [data-cy="totalPts"]')
            .should('have.text', '105');

        cy.get('[data-p-index="2"] [data-pc-section="rowtogglebutton"]').click()
        cy.get('[data-cy="skillToImportInfo-proj2_skill3Subj3"]')
            .contains('Self Report: Honor System');
        cy.get('[data-cy="skillToImportInfo-proj2_skill3Subj3"] [data-cy="projId"]')
            .should('have.text', 'proj2');
        cy.get('[data-cy="skillToImportInfo-proj2_skill3Subj3"] [data-cy="skillId"]')
            .should('have.text', 'skill3Subj3');
        cy.get('[data-cy="skillToImportInfo-proj2_skill3Subj3"] [data-cy="totalPts"]')
            .should('have.text', '80');

        cy.get('[data-p-index="3"] [data-pc-section="rowtogglebutton"]').click()
        // make sure markdown is not shown
        cy.get('[data-cy="skillToImportInfo-proj1_skill4Subj2"] [data-cy="importedSkillInfoDescription"]')
            .should('have.text', 'Title\n');
    });

    it('skill column sort', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 3 Subj3'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 4 Subj2'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 5'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 6 Subj2'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 7'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 8 Subj3'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Skill')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 8 Subj3'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 7'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 6 Subj2'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 5'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 4 Subj2'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 3 Subj3'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }],
        ], 5);
    });

    it('project column sort', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get(`${tableSelector} th`)
            .contains('Project')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 3,
                value: 'project 2'
            }],
            [{
                colIndex: 3,
                value: 'project 2'
            }],
            [{
                colIndex: 3,
                value: 'project 2'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Project')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 3,
                value: 'project 2'
            }],
            [{
                colIndex: 3,
                value: 'project 2'
            }],
            [{
                colIndex: 3,
                value: 'project 2'
            }],
            [{
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 3,
                value: 'project 1'
            }],
            [{
                colIndex: 3,
                value: 'project 1'
            }],
        ], 5);
    });

    it('subject column sort', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get(`${tableSelector} th`)
            .contains('Subject')
            .click();
        cy.validateTable(tableSelector, [
            [{
               colIndex: 4,
                value: 'Subject 1'
            }],
            [{
               colIndex: 4,
                value: 'Subject 1'
            }],
            [{
               colIndex: 4,
                value: 'Subject 1'
            }],
            [{
               colIndex: 4,
                value: 'Subject 1'
            }],
            [{
               colIndex: 4,
                value: 'Subject 2'
            }],
            [{
               colIndex: 4,
                value: 'Subject 2'
            }],
            [{
               colIndex: 4,
                value: 'Subject 3'
            }],
            [{
               colIndex: 4,
                value: 'Subject 3'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Subject')
            .click();
        cy.validateTable(tableSelector, [
            [{
               colIndex: 4,
                value: 'Subject 3'
            }],
            [{
               colIndex: 4,
                value: 'Subject 3'
            }],
            [{
               colIndex: 4,
                value: 'Subject 2'
            }],
            [{
               colIndex: 4,
                value: 'Subject 2'
            }],
            [{
               colIndex: 4,
                value: 'Subject 1'
            }],
            [{
               colIndex: 4,
                value: 'Subject 1'
            }],
            [{
               colIndex: 4,
                value: 'Subject 1'
            }],
            [{
               colIndex: 4,
                value: 'Subject 1'
            }],
        ], 5);
    });

    it('points column sort', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get(`${tableSelector} th`)
            .contains('Points')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 5,
                value: '45'
            }],
            [{
                colIndex: 5,
                value: '80'
            }],
            [{
                colIndex: 5,
                value: '80'
            }],
            [{
                colIndex: 5,
                value: '105'
            }],
            [{
                colIndex: 5,
                value: '105'
            }],
            [{
                colIndex: 5,
                value: '120'
            }],
            [{
                colIndex: 5,
                value: '120'
            }],
            [{
                colIndex: 5,
                value: '125'
            }],
        ], 5);

        cy.get(`${tableSelector} th`)
            .contains('Points')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 5,
                value: '125'
            }],
            [{
                colIndex: 5,
                value: '120'
            }],
            [{
                colIndex: 5,
                value: '120'
            }],
            [{
                colIndex: 5,
                value: '105'
            }],
            [{
                colIndex: 5,
                value: '105'
            }],
            [{
                colIndex: 5,
                value: '80'
            }],
            [{
                colIndex: 5,
                value: '80'
            }],
            [{
                colIndex: 5,
                value: '45'
            }],
        ], 5);
    });

    it('add project filter', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="importSkillsFromCatalogTable"] tr:nth-child(2) [data-cy="addProjectFilter"]')
            .click();
        cy.get('[data-cy="projectNameFilter"]')
            .should('have.value', 'This is project 2');
        cy.validateTable(tableSelector, [
            [{
                colIndex: 3,
                value: 'This is project 2'
            }],
            [{
                colIndex: 3,
                value: 'This is project 2'
            }],
            [{
                colIndex: 3,
                value: 'This is project 2'
            }],
        ], 5);
    });

    it('add subject filter', () => {
        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="importSkillsFromCatalogTable"] tr:nth-child(3) [data-cy="addSubjectFilter"]')
            .click();
        cy.get('[data-cy="subjectNameFilter"]')
            .should('have.value', 'Subject 3');
        cy.validateTable(tableSelector, [
            [{
               colIndex: 4,
                value: 'Subject 3'
            }],
            [{
               colIndex: 4,
                value: 'Subject 3'
            }],
        ], 5);
    });

    it('sort column and order is saved in local storage', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="importFromCatalogBtn"]').click()
        // initial sort order
        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: 'Skill 2' }, { colIndex: 5, value: '105' }],
            [{ colIndex: 2, value: 'Skill 3' }, { colIndex: 5, value: '80' }],
            [{ colIndex: 2, value: 'Skill 8' }, { colIndex: 5, value: '45' }],
        ], 5);

        cy.get(`${tableSelector} th`)
          .contains('Points')
          .click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: 'Skill 8' }, { colIndex: 5, value: '45' }],
            [{ colIndex: 2, value: 'Skill 3' }, { colIndex: 5, value: '80' }],
            [{ colIndex: 2, value: 'Skill 2' }, { colIndex: 5, value: '105' }],
        ], 5);
        cy.get(`${tableSelector} th`)
          .contains('Points')
          .click();
        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: 'Skill 2' }, { colIndex: 5, value: '105' }],
            [{ colIndex: 2, value: 'Skill 3' }, { colIndex: 5, value: '80' }],
            [{ colIndex: 2, value: 'Skill 8' }, { colIndex: 5, value: '45' }],
        ], 5);

        cy.get('[data-cy="closeButton"]').click()
        cy.get('[data-cy="importFromCatalogBtn"]').click()
        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: 'Skill 2' }, { colIndex: 5, value: '105' }],
            [{ colIndex: 2, value: 'Skill 3' }, { colIndex: 5, value: '80' }],
            [{ colIndex: 2, value: 'Skill 8' }, { colIndex: 5, value: '45' }],
        ], 5);

        // refresh and validate
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="importFromCatalogBtn"]').click()
        cy.validateTable(tableSelector, [
            [{ colIndex: 2, value: 'Skill 2' }, { colIndex: 5, value: '105' }],
            [{ colIndex: 2, value: 'Skill 3' }, { colIndex: 5, value: '80' }],
            [{ colIndex: 2, value: 'Skill 8' }, { colIndex: 5, value: '45' }],
        ], 5);
    })
});
