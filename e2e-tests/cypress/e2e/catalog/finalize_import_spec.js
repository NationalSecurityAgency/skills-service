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

describe('Finalize Imported Skills Tests', () => {

    beforeEach(() => {
        cy.waitForBackendAsyncTasksToComplete();

        cy.createProject(1);
        cy.createSubject(1, 1);
    });

    it('finalize imported skills', () => {
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

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 2 imported skills!');

        // drill down and make sure alert is gone
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.contains('Very Great Skill 2');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill1"]')
            .should('not.exist');
    });

    it('finalize imported skills - refresh after initiating finalize', () => {
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

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');

        // refresh
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 2 imported skills!');

        // drill down and make sure alert is gone
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.contains('Very Great Skill 2');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill1"]')
            .should('not.exist');
    });

    it('finalize imported skills - drill down after initiating finalize', () => {
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

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');

        // drill down and make sure alert is gone
        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.contains('Very Great Skill 2');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 2 imported skills!');

        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill1"]')
            .should('not.exist');
    });

    it('finalize imported skills on subject page', () => {
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

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 2 imported skills!');

        cy.get('[data-cy="disabledBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill1"]')
            .should('not.exist');
    });

    it('finalize imported skills on subject page - refresh after finalization', () => {
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

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');

        // refresh
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 2 imported skills!');

        cy.get('[data-cy="disabledBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill1"]')
            .should('not.exist');
    });

    it('finalize imported skills on subject page - navigate to project after finalization', () => {
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

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');

        // user breadcrumb to navigate up to a project
        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 2 imported skills!');
    });

    it('disable count in the finalize warning only counts catalog skills', () => {
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

        cy.createSkillsGroup(1, 1, 20);
        cy.addSkillToGroup(1, 1, 20, 21, {
            pointIncrement: 10,
            numPerformToCompletion: 5
        });
        cy.addSkillToGroup(1, 1, 20, 22, {
            pointIncrement: 10,
            numPerformToCompletion: 5
        });

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('have.text', '2');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project');

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('have.text', '2');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project');
    });

    it('finalize refreshes metric cards on subject page', () => {
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

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('have.text', '2');

        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 2 imported skills!');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('not.exist');
    });

    it('finalize refreshes metric cards on project page', () => {
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
        cy.get('[data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('have.text', '2');

        cy.get('[data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]')
            .should('have.text', '0');
        cy.get('[data-cy="pagePreviewCardStat_# Skills_disabled"]')
            .should('have.text', '2');

        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 2 imported skills!');

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('not.exist');

        cy.get('[data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]')
            .should('have.text', '2');
        cy.get('[data-cy="pagePreviewCardStat_# Skills_disabled"]')
            .should('not.exist');
    });

    it('cannot import while finalizing', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);
        cy.exportSkillToCatalog(2, 1, 3);

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

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-cy="catalogSkillImport-finalizationInProcess"]')
            .contains('Finalization in Progress');
        cy.get('[data-cy="importBtn"]')
            .should('be.disabled');
        cy.waitForBackendAsyncTasksToComplete();
    });

    it('finalize -> import again -> finalize again', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);
        cy.createSkill(2, 1, 3);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);
        cy.exportSkillToCatalog(2, 1, 3);

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

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="disabledBadge-skill2"]')
            .should('exist');
        cy.get('[data-cy="disabledBadge-skill1"]')
            .should('exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There are 2 imported skills in this project that are not yet finalized');

        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills');
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 2 imported skills!');

        cy.get('[data-cy="disabledBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill1"]')
            .should('not.exist');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
                cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="importBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="importBtn"]')
            .click();

        cy.get('[data-cy="disabledBadge-skill3"]')
            .should('exist');
        cy.get('[data-cy="disabledBadge-skill2"]')
            .should('not.exist');
        cy.get('[data-cy="disabledBadge-skill1"]')
            .should('not.exist');
        cy.get('[data-cy="manageSkillLink_skill3"]')
            .should('exist');
        cy.get('[data-cy="manageSkillLink_skill2"]')
            .should('exist');
        cy.get('[data-cy="manageSkillLink_skill3"]')
            .should('exist');

        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 2 imported skills')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('There is 1 imported skill in this project that is not yet finalized');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();

        cy.contains('There is 1 skill to finalize.');
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Finalizing 1 imported skill');

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]')
            .contains('Successfully finalized 1 imported skill!');
    });

    it('cannot finalize imported skills if point thresholds not met', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { pointIncrement: 100 });
        cy.createSkill(2, 1, 2, { pointIncrement: 100 });

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createSkill(2, 1, 1, { pointIncrement: 10 });
        cy.createSkill(2, 1, 2, { pointIncrement: 10 });

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

        cy.intercept('/admin/projects/proj1/pendingFinalization/pointTotals')
            .as('loadPendingPoints');
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.wait('@loadPendingPoints');
        //intercept and wait on count loading
        cy.get('[data-cy="no-finalize"]')
            .should('exist')
            .contains('Finalization cannot be performed until This is project 1 has at least 100 points. Finalizing currently imported Skills would only bring This is project 1 to 40 points.');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .click();

        cy.createSubject(1, 2);
        cy.createSkill(1, 2, 1, { pointIncrement: 25 });
        cy.createSkill(1, 2, 2, { pointIncrement: 25 });

        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.wait('@loadPendingPoints');
        cy.get('[data-cy="no-finalize"]')
            .should('exist')
            .contains('Finalization cannot be performed until Subject 1 has at least 100 points. Finalizing the currently imported skills would only result in Subject 1: 40 points.');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="closeDialogBtn"]')
            .click();

        cy.createSkill(1, 1, 3, { pointIncrement: 15 });
        cy.createSkill(1, 1, 4, { pointIncrement: 15 });
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]')
            .click();
        cy.wait('@loadPendingPoints');
        cy.get('[data-cy="no-finalize"]')
            .should('not.exist');
        cy.get('[data-cy="saveDialogBtn"]')
            .should('be.enabled');
    });

    it('Check the point system and warn users when finalizing skills catalog if imported points are outside of the exiting point scheme', () => {
        cy.createSkill(1, 1, 1, { pointIncrement: 52 });
        cy.createSkill(1, 1, 2, { pointIncrement: 673 });

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 6, { pointIncrement: 10 });
        cy.createSkill(2, 1, 7, { pointIncrement: 1000 });
        cy.createSkill(2, 1, 8, { pointIncrement: 1000 });
        cy.createSkill(2, 1, 9, { pointIncrement: 1000 });

        cy.exportSkillToCatalog(2, 1, 6);
        cy.exportSkillToCatalog(2, 1, 7);
        cy.exportSkillToCatalog(2, 1, 8);
        cy.exportSkillToCatalog(2, 1, 9);

        cy.bulkImportSkillFromCatalog(1, 1, [
            {
                projNum: 2,
                skillNum: 6
            },
            {
                projNum: 2,
                skillNum: 7
            },
            {
                projNum: 2,
                skillNum: 8
            },
            {
                projNum: 2,
                skillNum: 9
            },
        ]);

        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="outOfRangeWarning"]')
            .contains('Your Project skills point values range from [104] to [1,346]. 4 skills you are importing fall outside of that point value.');
        cy.get('[data-cy="viewSkillsWithPtsOutOfRange"]')
            .click();

        const tableSelector = '[data-cy="skillsWithOutOfBoundsPoints"]';
        cy.get(`${tableSelector} [data-p-index="0"]`).contains('Skill 6')
        cy.get(`${tableSelector} [data-p-index="0"]`).contains('20 ( less than 104 )')

        cy.get(`${tableSelector} [data-p-index="1"]`).contains('Skill 7')
        cy.get(`${tableSelector} [data-p-index="1"]`).contains('2,000 ( more than 1,346 )')

        cy.get(`${tableSelector} [data-p-index="2"]`).contains('Skill 8')
        cy.get(`${tableSelector} [data-p-index="2"]`).contains('2,000 ( more than 1,346 )')

        cy.get(`${tableSelector} [data-p-index="3"]`).contains('Skill 9')
        cy.get(`${tableSelector} [data-p-index="3"]`).contains('2,000 ( more than 1,346 )')

        cy.get(`${tableSelector} [data-p-index="4"]`).should('not.exist')

    });

    it('Check the point system and warn users when finalizing skills catalog if imported points are outside of the exiting point scheme - no warning', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 6, { pointIncrement: 60 });
        cy.createSkill(2, 1, 7, { pointIncrement: 52 });
        cy.createSkill(2, 1, 8, { pointIncrement: 672 });

        cy.exportSkillToCatalog(2, 1, 6);
        cy.exportSkillToCatalog(2, 1, 7);
        cy.exportSkillToCatalog(2, 1, 8);

        cy.bulkImportSkillFromCatalog(1, 1, [
            {
                projNum: 2,
                skillNum: 6
            },
            {
                projNum: 2,
                skillNum: 7
            },
            {
                projNum: 2,
                skillNum: 8
            },
        ]);

        // first check when the importing project has 0 native skills
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="outOfRangeWarning"]')
            .should('not.exist');

        // then with native skills
        cy.createSkill(1, 1, 1, { pointIncrement: 52 });
        cy.createSkill(1, 1, 2, { pointIncrement: 673 });
        cy.visit('/administrator/projects/proj1');
        cy.get('[data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="outOfRangeWarning"]')
            .should('not.exist');

    });
});


