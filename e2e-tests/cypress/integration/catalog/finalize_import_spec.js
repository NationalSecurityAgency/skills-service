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

    it.only('finalize imported skills', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();
        cy.get('[data-cy="doPerformFinalizeButton"]').should('be.enabled');
        cy.get('[data-cy="finalizeCancelButton"]').should('be.enabled');

        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 2 imported skills!')

        // drill down and make sure alert is gone
        cy.get('[data-cy="manageBtn_subj1"]').click();
        cy.contains('Very Great Skill 2');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="disabledBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="disabledBadge-skill1"]').should('not.exist')
    });

    it('finalize imported skills - refresh after initiating finalize', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();
        cy.get('[data-cy="doPerformFinalizeButton"]').should('be.enabled');
        cy.get('[data-cy="finalizeCancelButton"]').should('be.enabled');

        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')

        // refresh
        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 2 imported skills!')

        // drill down and make sure alert is gone
        cy.get('[data-cy="manageBtn_subj1"]').click();
        cy.contains('Very Great Skill 2');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="disabledBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="disabledBadge-skill1"]').should('not.exist')
    });

    it('finalize imported skills - drill down after initiating finalize', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();
        cy.get('[data-cy="doPerformFinalizeButton"]').should('be.enabled');
        cy.get('[data-cy="finalizeCancelButton"]').should('be.enabled');

        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')


        // drill down and make sure alert is gone
        cy.get('[data-cy="manageBtn_subj1"]').click();
        cy.contains('Very Great Skill 2');
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 2 imported skills!')

        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="disabledBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="disabledBadge-skill1"]').should('not.exist')
    });

    it('finalize imported skills on subject page', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();
        cy.get('[data-cy="doPerformFinalizeButton"]').should('be.enabled');
        cy.get('[data-cy="finalizeCancelButton"]').should('be.enabled');

        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 2 imported skills!')

        cy.get('[data-cy="disabledBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="disabledBadge-skill1"]').should('not.exist')
    });

    it('finalize imported skills on subject page - refresh after finalization', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();
        cy.get('[data-cy="doPerformFinalizeButton"]').should('be.enabled');
        cy.get('[data-cy="finalizeCancelButton"]').should('be.enabled');

        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');

        // refresh
        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 2 imported skills!')

        cy.get('[data-cy="disabledBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="disabledBadge-skill1"]').should('not.exist')
    });

    it('finalize imported skills on subject page - navigate to project after finalization', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();
        cy.get('[data-cy="doPerformFinalizeButton"]').should('be.enabled');
        cy.get('[data-cy="finalizeCancelButton"]').should('be.enabled');

        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')

        // user breadcrumb to navigate up to a project
        cy.get('[data-cy="breadcrumb-proj1"]').click();
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 2 imported skills!')
    });

    it('disable count in the finalize warning only counts catalog skills', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.createSkillsGroup(1, 1, 20);
        cy.addSkillToGroup(1, 1, 20, 21, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 20, 22, { pointIncrement: 10, numPerformToCompletion: 5 });

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]').should('have.text', '4')
        cy.get('[data-cy="importFinalizeAlert"]').contains('There are 2 imported skills in this project')

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]').should('have.text', '4')
        cy.get('[data-cy="importFinalizeAlert"]').contains('There are 2 imported skills in this project')
    });


    it('non-imported disabled skills must not enable finalize alert', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createSkillsGroup(1, 1, 20);
        cy.addSkillToGroup(1, 1, 20, 21, { pointIncrement: 10, numPerformToCompletion: 5 });
        cy.addSkillToGroup(1, 1, 20, 22, { pointIncrement: 10, numPerformToCompletion: 5 });

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]').should('have.text', '2')
        cy.get('[data-cy="importFinalizeAlert"]').should('not.exist')

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]').should('have.text', '2')
        cy.get('[data-cy="importFinalizeAlert"]').should('not.exist')
    });

    it('finalize refreshes metric cards on subject page', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '0')
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]').should('have.text', '2')


        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();
        cy.get('[data-cy="doPerformFinalizeButton"]').should('be.enabled');
        cy.get('[data-cy="finalizeCancelButton"]').should('be.enabled');

        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 2 imported skills!')
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2')
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]').should('not.exist')
    });

    it('finalize refreshes metric cards on project page', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.bulkImportSkillFromCatalog(1, 1, [
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1')
        cy.get('[data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]').should('have.text', '0')
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]').should('have.text', '2')

        cy.get('[data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]').should('have.text', '0')
        cy.get('[data-cy="pagePreviewCardStat_# Skills_disabled"]').should('have.text', '2')

        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();
        cy.get('[data-cy="doPerformFinalizeButton"]').should('be.enabled');
        cy.get('[data-cy="finalizeCancelButton"]').should('be.enabled');

        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 2 imported skills!')

        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]').should('have.text', '2')
        cy.get('[data-cy="pageHeaderStat_Skills_disabledCount"]').should('not.exist')

        cy.get('[data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]').should('have.text', '2')
        cy.get('[data-cy="pagePreviewCardStat_# Skills_disabled"]').should('not.exist')
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
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click()
        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')

        cy.get('[data-cy="importFromCatalogBtn"]').click();
        cy.get('[data-cy="catalogSkillImport-finalizationInProcess"]').contains('Finalization in Progress')
        cy.get('[data-cy="importBtn"]').should('be.disabled')
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
            { projNum: 2, skillNum: 1 },
            { projNum: 2, skillNum: 2 },
        ])

        cy.visit('/administrator/projects/proj1/subjects/subj1')
        cy.get('[data-cy="disabledBadge-skill2"]').should('exist')
        cy.get('[data-cy="disabledBadge-skill1"]').should('exist')
        cy.get('[data-cy="importFinalizeAlert"]').contains('There are 2 imported skills in this project that are not yet finalized')


        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();
        cy.get('[data-cy="doPerformFinalizeButton"]').should('be.enabled');
        cy.get('[data-cy="finalizeCancelButton"]').should('be.enabled');

        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills')
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 2 imported skills!')

        cy.get('[data-cy="disabledBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="disabledBadge-skill1"]').should('not.exist')

        cy.get('[data-cy="importFromCatalogBtn"]').click()
        cy.get('[data-cy="skillSelect_proj2-skill3"]').check({force: true})
        cy.get('[data-cy="importBtn"]').should('be.enabled')
        cy.get('[data-cy="importBtn"]').click();

        cy.get('[data-cy="disabledBadge-skill3"]').should('exist')
        cy.get('[data-cy="disabledBadge-skill2"]').should('not.exist')
        cy.get('[data-cy="disabledBadge-skill1"]').should('not.exist')
        cy.get('[data-cy="manageSkillBtn_skill3"]').should('exist')
        cy.get('[data-cy="manageSkillBtn_skill2"]').should('exist')
        cy.get('[data-cy="manageSkillBtn_skill3"]').should('exist')

        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 2 imported skills').should('not.exist')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('be.enabled')

        cy.get('[data-cy="importFinalizeAlert"]').contains('There is 1 imported skill in this project that is not yet finalized')
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').click();

        cy.contains('There is 1 skill to finalize.')
        cy.get('[data-cy="doPerformFinalizeButton"]').click()
        cy.get('[data-cy="importFinalizeAlert"] [data-cy="finalizeBtn"]').should('not.exist');
        cy.get('[data-cy="importFinalizeAlert"]').contains('Finalizing 1 imported skill')

        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="importFinalizeAlert"]').contains('Successfully finalized 1 imported skill!')
    });
});
