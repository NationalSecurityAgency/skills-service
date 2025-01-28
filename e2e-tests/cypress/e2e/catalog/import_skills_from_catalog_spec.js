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

describe('Import skills from Catalog Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    const tableSelector = '[data-cy="importSkillsFromCatalogTable"]';

    it('there are no skills to import - empty catalog', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]')
            .contains('Nothing Available for Import');
        cy.get('[data-cy="importBtn"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');

        cy.get('[data-pc-section="closebutton"]')
            .click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]')
            .should('not.exist');
    });

    it('there are no skills to import - all skills were imported', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get('[data-cy="catalogSkillImportModal-NoData"]')
            .contains('Nothing Available for Import');
        cy.get('[data-cy="importBtn"]')
            .should('not.exist');
        cy.get('[data-cy="closeButton"]')
            .should('not.exist');
        cy.get('[data-pc-section="closebutton"]')
          .click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]')
            .should('not.exist');
    });

    it('import 1 skill', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 3,
                value: 'This is project 2'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: 'This is project 2'
            }],
        ], 5, false, null, false);


        cy.get('[data-cy="importBtn"]').should('be.disabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="numSelectedSkills"]').should('have.text', '1');
        cy.get('[data-cy="importBtn"]').should('be.enabled').click();
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Catalog"]').click()
        cy.get('[data-pc-section="closebutton"]').click()
        cy.validateTable('[data-cy="skillsTable"]', [
            [{
                colIndex: 5,
                value: 'Imported from This is project 2'
            }],
        ], 5, false, null, false);

        // refresh and re-validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.validateTable('[data-cy="skillsTable"]', [
            [{
                colIndex: 5,
                value: 'Imported from This is project 2'
            }],
        ], 5, false, null, false);
    });

    it('import last available skill', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '1');
        cy.get('[data-cy="importBtn"]')
            .click();

        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '2');
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="importedBadge-skill2"]');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]')
            .contains('Nothing Available for Import');
    });

    it('cancel import', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.importSkillFromCatalog(2, 1, 1, 1);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get('[data-cy="closeButton"]')
            .click();
        cy.get('[data-cy="importSkillsFromCatalogTable"]')
            .should('not.exist');

        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '1');
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="importedBadge-skill2"]')
            .should('not.exist');
    });

    it('show skill details', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 4);
        cy.exportSkillToCatalog(2, 1, 3);
        cy.exportSkillToCatalog(2, 1, 4);

        cy.createProject(3);
        cy.createSubject(3, 1);

        cy.importSkillFromCatalog(3, 1, 1, 1);
        cy.importSkillFromCatalog(3, 1, 1, 2);

        cy.importSkillFromCatalog(3, 1, 2, 3);
        cy.importSkillFromCatalog(3, 1, 2, 4);

        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '4');

        cy.get('[data-p-index="1"] [data-pc-section="rowtogglebutton"]').click()
        cy.get('[data-cy="childRowDisplay_skill3"]')
            .contains('This skill was imported');
        cy.get('[data-cy="childRowDisplay_skill3"]')
            .contains('initially defined in the This is project 2 project');

        cy.get('[data-p-index="2"] [data-pc-section="rowtogglebutton"]').click()
        cy.get('[data-cy="childRowDisplay_skill2"]')
            .contains('This skill was imported');
        cy.get('[data-cy="childRowDisplay_skill2"]')
            .contains('initially defined in the This is project 1 project');
    });

    it('import 1 page of skills', () => {
        // mix skill names since it's sorted by skillId - this will force different projects in the first page
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 8);

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 6);
        cy.exportSkillToCatalog(1, 1, 7);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.exportSkillToCatalog(2, 1, 2); // proj 2
        cy.exportSkillToCatalog(2, 1, 3); // proj 2
        cy.exportSkillToCatalog(2, 1, 8); // proj 2

        cy.createProject(3);
        cy.createSubject(3, 1);

        cy.visit('/administrator/projects/proj3/subjects/subj1');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');

        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="importSkillsFromCatalogTable"]  [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click();

        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="importSkillsFromCatalogTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }

        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '5');

        cy.get('[data-cy="importBtn"]')
            .click();

        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '5');
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="importedBadge-skill2"]');
        cy.get('[data-cy="importedBadge-skill3"]');
        cy.get('[data-cy="importedBadge-skill4"]');
        cy.get('[data-cy="importedBadge-skill5"]');

        // only 3 left after import
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get(`${tableSelector} [data-cy="skillsBTableTotalRows"]`)
            .should('have.text', '3');
    });

    it('filter then import', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2, { name: 'Find this 1' });
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4, { name: 'Find this 2' });
        cy.createSkill(1, 1, 5, { name: 'Find this 3' });
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get('[data-cy="skillNameFilter"]')
            .type('find{enter}');
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', '3');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()

        cy.get('[data-cy="importBtn"]')
            .click();

        cy.get(`[data-cy="skillsTable"] tbody tr`)
            .should('have.length', '1');
        cy.get('[data-cy="importedBadge-skill4"]');
    });

    it('remove imported skill should re-appear in the import table', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);

        cy.createProject(2);
        cy.createSubject(2, 1);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);
        cy.importSkillFromCatalog(2, 1, 1, 3);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] tbody tr`)
            .should('have.length', '3');
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="importedBadge-skill2"]');
        cy.get('[data-cy="importedBadge-skill3"]');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]')
            .contains('Nothing Available for Import');
        cy.get('[data-pc-section="closebutton"]')
            .click();
        cy.get('[data-cy="catalogSkillImportModal-NoData"]')
            .should('not.exist');

        cy.get('[data-cy="deleteSkillButton_skill2"]')
            .click();
        cy.acceptRemovalSafetyCheck();
        cy.get(`[data-cy="skillsTable"] tbody tr`)
            .should('have.length', '2');
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="importedBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="importedBadge-skill3"]');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: 'This is project 1'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 1);
        cy.get('[data-cy="alreadyExistWarning_proj1-skill1"]')
            .should('not.exist');

        cy.get('[data-cy="closeButton"]')
            .click();

        cy.get('[data-cy="deleteSkillButton_skill3"]')
            .click();
        cy.acceptRemovalSafetyCheck();
        cy.get(`[data-cy="skillsTable"] tbody tr`)
            .should('have.length', '1');
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="importedBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="importedBadge-skill3"]')
            .should('not.exist');

        cy.get('[data-cy="deleteSkillButton_skill1"]')
            .click();
        cy.acceptRemovalSafetyCheck();
        cy.contains('No Skills Yet');
        cy.get('[data-cy="importedBadge-skill1"]')
            .should('not.exist');
        cy.get('[data-cy="importedBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="importedBadge-skill3"]')
            .should('not.exist');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 3,
                value: 'This is project 1'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: 'This is project 1'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 3,
                value: 'This is project 1'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 3);
        cy.get('[data-cy="alreadyExistWarning_proj1-skill1"]')
            .should('not.exist');
        cy.get('[data-cy="alreadyExistWarning_proj1-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="alreadyExistWarning_proj1-skill3"]')
            .should('not.exist');

        // refresh and re-validate
        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 3,
                value: 'This is project 1'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: 'This is project 1'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 3'
            }, {
                colIndex: 3,
                value: 'This is project 1'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 3);
        cy.get('[data-cy="alreadyExistWarning_proj1-skill1"]')
            .should('not.exist');
        cy.get('[data-cy="alreadyExistWarning_proj1-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="alreadyExistWarning_proj1-skill3"]')
            .should('not.exist');
    });

    it('do not allow import if skill id or name already exist', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.createSkill(1, 1, 3);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 3);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 5);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 4, { name: 'Some Other' });
        cy.createSkill(2, 1, 5, { skillId: 'someOther' });
        cy.importSkillFromCatalog(2, 1, 1, 1);

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();

        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '4');

        cy.get('[data-cy="alreadyExistWarning_proj1-skill2"]')
            .contains('Cannot import!Skill ID and name already exist in this project!');
        cy.get('[data-cy="alreadyExistWarning_proj1-skill3"]')
            .should('not.exist');
        cy.get('[data-cy="alreadyExistWarning_proj1-skill4"]')
            .contains('Cannot import!Skill ID already exists in this project!');
        cy.get('[data-cy="alreadyExistWarning_proj1-skill5"]')
            .contains('Cannot import!Skill name already exists in this project!');

        // anything that cannot be imported must not have a selection
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"] [data-pc-section="input"]').should('not.be.visible')
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').should('be.visible')
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"] [data-pc-section="input"]').should('not.be.visible')
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="3"] [data-pc-name="pcrowcheckbox"] [data-pc-section="input"]').should('not.be.visible')

        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');
    });

    it('do not allow to cross-project deps for the catalog imported skills', () => {
        cy.createSkill(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.importSkillFromCatalog(2, 1, 1, 1);

        cy.visit('/administrator/projects/proj2/learning-path');
        cy.get('[data-cy="projectLastReportedSkillValue"]').contains('Never')
        cy.get('[data-cy="noContent"]').contains('Here you can create and manage the project\'s Learning Path')
        cy.get('[data-cy="noContent"]').contains('To make your project\'s skills eligible please select a skill and')
        cy.get('[data-cy="noContent"]').contains('Coordinate with other projects to share skills with this project.')
        cy.get('[data-cy="skillSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj2-skill2"]');
        cy.get('[data-cy="skillsSelectionItem-proj2-skill1"]')
            .should('not.exist'); // imported skill
    });

    it('do not allow to use imported skills in a Global Badge', () => {
        cy.createSkill(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.importSkillFromCatalog(2, 1, 1, 1);

        cy.logout();
        const supervisorUser = 'supervisor@skills.org';
        cy.register(supervisorUser, 'password');
        cy.login('root@skills.org', 'password');
        cy.request('PUT', `/root/users/${supervisorUser}/roles/ROLE_SUPERVISOR`);
        cy.logout();
        cy.login(supervisorUser, 'password');
        cy.log('completed supervisor user login');

        cy.createGlobalBadge(1);

        cy.visit('/administrator/globalBadges/globalBadge1');

        cy.get('[data-cy="skillsSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj1-skill1"]');
        cy.get('[data-cy="skillsSelectionItem-proj2-skill2"]');
        cy.get('[data-cy="skillsSelectionItem-proj2-skill1"]')
            .should('not.exist'); // imported skill
    });

    it('allow to add imported skills to a project badge', () => {
        cy.createSkill(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 1);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.importSkillFromCatalog(2, 1, 1, 1);

        cy.createBadge(2, 1);

        cy.visit('/administrator/projects/proj2/badges/badge1');
        cy.get('[data-cy="skillsSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj2-skill2"]');
        cy.get('[data-cy="skillsSelectionItem-proj2-skill1"]')
            .should('not.exist'); // imported non-finalized skill cannot be added to the badge

        cy.finalizeCatalogImport(2);

        cy.visit('/administrator/projects/proj2/badges/badge1');
        cy.get('[data-cy="skillsSelector"]')
            .click();
        cy.get('[data-cy="skillsSelectionItem-proj2-skill2"]');
        cy.get('[data-cy="skillsSelectionItem-proj2-skill1"]')
            .click(); // imported and finalized skill
        cy.get('[data-cy="badgeSkillsTable"]')
            .contains('Very Great Skill 1'); // imported skill added to the badge

    });

    it('imported skills are disabled and finalization alert is displayed', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.validateTable(tableSelector, [
            [{
                colIndex: 2,
                value: 'Very Great Skill 1'
            }, {
                colIndex: 3,
                value: 'This is project 2'
            }],
            [{
                colIndex: 2,
                value: 'Very Great Skill 2'
            }, {
                colIndex: 3,
                value: 'This is project 2'
            }],
        ], 5, false, null, false);
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', 2);

        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="importBtn"]')
            .click();
        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '2');
        cy.get('[data-cy="disabledBadge-skill2"]');
        cy.get('[data-cy="disabledBadge-skill1"]');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project that are not yet finalized.');

        // refresh at subject level and validate
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '2');
        cy.get('[data-cy="disabledBadge-skill2"]');
        cy.get('[data-cy="disabledBadge-skill1"]');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project that are not yet finalized.');

        // navigate app to a project and validate
        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project that are not yet finalized.');

        // refresh at project level and validate
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project that are not yet finalized.');

        // navigate down from the project and validate
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '2');
        cy.get('[data-cy="disabledBadge-skill2"]');
        cy.get('[data-cy="disabledBadge-skill1"]');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project that are not yet finalized.');

        // navigate down from projects and validate
        cy.visit('/administrator');
        cy.get('[data-cy="projCard_proj1_manageLink"]')
            .click();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project that are not yet finalized.');
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '2');
        cy.get('[data-cy="disabledBadge-skill2"]');
        cy.get('[data-cy="disabledBadge-skill1"]');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project that are not yet finalized.');
    });

    it('imported skill has disabled badge on the skill page', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
            {
                projNum: 2,
                skillNum: 2
            },
        ]);

        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="pageHeader"] [data-cy="disabledSkillBadge"]');
        cy.get('[data-cy="childRowDisplay_skill1"]')
            .contains('This skill is disabled');

        cy.finalizeCatalogImport(1);
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');
        cy.get('[data-cy="pageHeader"] [data-cy="disabledSkillBadge"]')
            .should('not.exist');
    });

    it('cancel finalize modal', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
            {
                projNum: 2,
                skillNum: 2
            },
        ]);

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="closeDialogBtn"]')
            .click();
        cy.get('[data-cy="closeDialogBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('exist');

        // now close via the X on top right
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.get('[aria-label="Close"]')
            .click();
        cy.get('[data-cy="closeDialogBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('exist');
    });

    it('must not be able to import while finalizing', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
        ]);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="saveDialogBtn"]')
            .click();
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 1 imported skill');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="catalogSkillImport-finalizationInProcess"]')
            .contains('Finalization in Progress');
    });

    it('must not be able to import while finalizing - state session has catalog already loaded', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
        ]);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('be.enabled');
        // cy.get('[data-cy="saveDialogBtn"]').should('be.enabled');
        // cy.get('[data-cy="closeDialogBtn"]').should('be.enabled');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '1');

        cy.finalizeCatalogImportWithoutWaiting(1);
        cy.get('[data-cy="importBtn"]')
            .click();
        cy.get('[data-cy="catalogSkillImport-finalizationInProcess"]')
            .contains('Finalization in Progress');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
    });

    it('import more than 1 page of skills', () => {
        // mix skill names since it's sorted by skillId - this will force different projects in the first page
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.createSkill(1, 1, 9);
        cy.createSkill(1, 1, 66);
        cy.createSkill(1, 1, 67);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 8);

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 6);
        cy.exportSkillToCatalog(1, 1, 7);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 5);
        cy.exportSkillToCatalog(1, 1, 9);
        cy.exportSkillToCatalog(1, 1, 66);
        cy.exportSkillToCatalog(1, 1, 67);

        cy.exportSkillToCatalog(2, 1, 2); // proj 2
        cy.exportSkillToCatalog(2, 1, 3); // proj 2
        cy.exportSkillToCatalog(2, 1, 8); // proj 2

        cy.createProject(3);
        cy.createSubject(3, 1);

        cy.intercept('/admin/projects/proj3/skills/catalog**')
            .as('getCatalogSkills');

        cy.visit('/administrator/projects/proj3/subjects/subj1');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.wait('@getCatalogSkills');

        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');

        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="importSkillsFromCatalogTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-pc-name="headercheckbox"] [data-pc-section="input"]').click();

        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="importSkillsFromCatalogTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }

        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '5');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 2"]')
            .click();
        cy.wait('@getCatalogSkills');

        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="importSkillsFromCatalogTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('not.be.checked')
        }

        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="importSkillsFromCatalogTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"]`).click()
        }
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '10');
        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="importSkillsFromCatalogTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }
        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 1"]')
          .click();
        cy.wait('@getCatalogSkills');
        for (let i= 0; i < 5 ; i++) {
            cy.get(`[data-cy="importSkillsFromCatalogTable"] [data-p-index="${i}"] [data-pc-name="pcrowcheckbox"] input`).should('be.checked')
        }
        cy.get('[data-cy="importBtn"]')
            .click();

        cy.get('[data-cy="skillsTable"] tbody tr')
            .should('have.length', '10');
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.get('[data-cy="importedBadge-skill2"]');
        cy.get('[data-cy="importedBadge-skill3"]');
        cy.get('[data-cy="importedBadge-skill4"]');
        cy.get('[data-cy="importedBadge-skill5"]');
        cy.get('[data-cy="importedBadge-skill6"]');
        cy.get('[data-cy="importedBadge-skill7"]');
        cy.get('[data-cy="importedBadge-skill8"]');
        cy.get('[data-cy="importedBadge-skill66"]');
        cy.get('[data-cy="importedBadge-skill67"]');

        // only 3 left after import
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get(`${tableSelector} tbody tr`)
            .should('have.length', '1');
    });

    it('respect maxSkillsInBulkImport configuration', () => {
        // mix skill names since it's sorted by skillId - this will force different projects in the first page
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxSkillsInBulkImport = 3;
                res.send(conf);
            });
        })
            .as('loadConfig');

        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.createSkill(1, 1, 9);
        cy.createSkill(1, 1, 66);
        cy.createSkill(1, 1, 67);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 8);

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 6);
        cy.exportSkillToCatalog(1, 1, 7);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 5);
        cy.exportSkillToCatalog(1, 1, 9);
        cy.exportSkillToCatalog(1, 1, 66);
        cy.exportSkillToCatalog(1, 1, 67);

        cy.exportSkillToCatalog(2, 1, 2); // proj 2
        cy.exportSkillToCatalog(2, 1, 3); // proj 2
        cy.exportSkillToCatalog(2, 1, 8); // proj 2

        cy.createProject(3);
        cy.createSubject(3, 1);

        cy.intercept('/admin/projects/proj3/skills/catalog**')
            .as('getCatalogSkills');

        cy.visit('/administrator/projects/proj3/subjects/subj1');
        cy.wait('@loadConfig');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.wait('@getCatalogSkills');

        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('Cannot import more than 3 Skills at once');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 2"]')
          .click();
        cy.wait('@getCatalogSkills');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('Cannot import more than 3 Skills at once');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 1"]').click();
        cy.wait('@getCatalogSkills');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="4"] [data-pc-name="pcrowcheckbox"] input').should('not.be.checked')
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('Cannot import more than 3 Skills at once');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=maximum-selected]')
            .should('not.exist');
    });

    it('respect maxSkillsPerSubject configuration', () => {
        // mix skill names since it's sorted by skillId - this will force different projects in the first page
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxSkillsInBulkImport = 5;
                conf.maxSkillsPerSubject = 10;
                res.send(conf);
            });
        })
            .as('loadConfig');

        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 4);
        cy.createSkill(1, 1, 5);
        cy.createSkill(1, 1, 6);
        cy.createSkill(1, 1, 7);
        cy.createSkill(1, 1, 9);
        cy.createSkill(1, 1, 66);
        cy.createSkill(1, 1, 67);
        cy.createSkill(1, 1, 68);
        cy.createSkill(1, 1, 69);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);
        cy.createSkill(2, 1, 8);
        cy.createSkill(2, 1, 10);
        cy.createSkill(2, 1, 11);
        cy.createSkill(2, 1, 12);

        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 6);
        cy.exportSkillToCatalog(1, 1, 7);
        cy.exportSkillToCatalog(1, 1, 4);
        cy.exportSkillToCatalog(1, 1, 5);
        cy.exportSkillToCatalog(1, 1, 9);
        cy.exportSkillToCatalog(1, 1, 66);
        cy.exportSkillToCatalog(1, 1, 67);
        cy.exportSkillToCatalog(1, 1, 68);
        cy.exportSkillToCatalog(1, 1, 69);

        cy.intercept('/admin/projects/proj2/skills/catalog**')
            .as('getCatalogSkills');

        cy.visit('/administrator/projects/proj2/subjects/subj1');
        cy.wait('@loadConfig');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.wait('@getCatalogSkills');

        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="3"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="4"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('No more than 10 Skills per Subject are allowed, this project already has 6');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 2"]')
          .click();
        cy.wait('@getCatalogSkills');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('No more than 10 Skills per Subject are allowed, this project already has 6');
        // for some reason two elements with the same aria-label are created in this test, we have to get the 2nd element
        // or the click event doesn't do anything
        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 1"]').click();
        cy.wait('@getCatalogSkills');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="4"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=maximum-selected]')
            .should('not.exist');
        cy.get('[data-cy=closeButton]')
            .click();

        cy.importSkillFromCatalog(2, 1, 1, 68);
        cy.importSkillFromCatalog(2, 1, 1, 69);
        // force skills to be repopulated (which would happen if imports were done via ui)
        cy.get('[data-cy="nav-Levels"]')
            .click();
        cy.get('[data-cy="nav-Skills"]')
            .click();

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.wait('@getCatalogSkills');

        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="2"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('No more than 10 Skills per Subject are allowed, this project already has 8');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 2"]').click();
        cy.wait('@getCatalogSkills');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('No more than 10 Skills per Subject are allowed, this project already has 8');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 1"]').click();
        cy.wait('@getCatalogSkills');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('No more than 10 Skills per Subject are allowed, this project already has 8');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=maximum-selected]')
            .should('not.exist');

        cy.get('[data-cy=closeButton]')
            .click();
        cy.createSkillsGroup(2, 1, 1);
        cy.addSkillToGroup(2, 1, 1, 909);
        //need to get skills in Skills component to update
        cy.get('[data-cy=nav-Levels]')
            .click();
        cy.get('[data-cy=nav-Skills]')
            .click();
        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.wait('@getCatalogSkills');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="numSelectedSkills"]')
            .should('have.text', '0');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="1"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('No more than 10 Skills per Subject are allowed, this project already has 9');

        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 2"]').click();
        cy.wait('@getCatalogSkills');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('No more than 10 Skills per Subject are allowed, this project already has 9');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [aria-label="Page 1"]').click();
        cy.wait('@getCatalogSkills');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=maximum-selected]')
            .should('exist')
            .contains('No more than 10 Skills per Subject are allowed, this project already has 9');
        cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=maximum-selected]')
            .should('not.exist');
    });
});



