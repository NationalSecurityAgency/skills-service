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
import dayjs from 'dayjs';

const moment = require('moment-timezone');

describe('Skill Reuse and Dashboard Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSubject(1, 2);
    });

    it('Search and Navigate directly to a skill properly labels reused skills', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.createSkillsGroup(1, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 1, 1, 12);

        cy.visit('/administrator/projects/proj1/');

        cy.get('[data-cy="skillsSelector"] input')
          .invoke('attr', 'placeholder')
          .should('contain', 'Search and Navigate directly to a skill');
        cy.get('[data-cy="skillsSelector"]')
            .click();
        cy.get('li.p-autocomplete-empty-message')
            .contains('Type to search for skills')
            .should('be.visible');
        cy.get('[data-cy="skillsSelector"]')
            .type('s');

        cy.get('[data-cy="skillsSelector-skillId"]')
            .should('have.length', 3)
            .as('skillIds');
        cy.get('@skillIds')
            .eq(0)
            .contains('skill1');
        cy.get('@skillIds')
            .eq(1)
            .contains('skill1');
        cy.get('@skillIds')
            .eq(2)
            .contains('skill1');

        cy.get('[data-cy="skillsSelector-skillName"]')
            .should('have.length', 3)
            .as('skillIds');
        cy.get('@skillIds')
            .eq(0)
            .find('[data-cy="reusedBadge"]')
            .should('not.exist');
        cy.get('@skillIds')
            .eq(1)
            .find('[data-cy="reusedBadge"]');
        cy.get('@skillIds')
            .eq(2)
            .find('[data-cy="reusedBadge"]');

        cy.get('[data-cy="skillsSelector-groupName"]')
            .should('have.length', 1)
            .as('skillIds');
        cy.get('@skillIds')
            .eq(0)
            .contains('Awesome Group 12 Subj1');
    });

    it('reused skills must NOT be available for badges', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);
        cy.createSkillsGroup(1, 1, 12);
        cy.reuseSkillIntoAnotherGroup(1, 1, 1, 12);
        cy.createBadge(1, 1);

        cy.visit('/administrator/projects/proj1/badges/badge1');
        cy.get('[data-cy="skillsSelector"]')
            .click();
        cy.get('[data-cy="skillsSelector-skillId"]')
            .should('have.length', 1)
            .as('skillIds');
        cy.get('@skillIds')
            .eq(0)
            .contains('skill1');
    });

    it('cannot initiate reuse when finalization is pending', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSubject(2, 2);
        cy.createSkill(2, 1, 11);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1/');
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[data-cy="reuseModalContent"]')
            .contains('Cannot initiate skill reuse while skill finalization is pending');
    });

    it('cannot initiate reuse when finalization is running', () => {
        cy.createSkill(1, 1, 1);
        cy.createSkill(1, 1, 2);
        cy.exportSkillToCatalog(1, 1, 1);
        cy.exportSkillToCatalog(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSubject(2, 2);
        cy.createSkill(2, 1, 11);
        cy.createSkill(2, 2, 22);

        cy.importSkillFromCatalog(2, 1, 1, 1);
        cy.importSkillFromCatalog(2, 1, 1, 2);

        cy.visit('/administrator/projects/proj2/subjects/subj1/');
        cy.get('[data-cy="skillsTable"] [data-p-index="2"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="finalizeBtn"]')
            .click();
        cy.get('[data-cy="saveDialogBtn"]')
            .click();
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()
        cy.get('[data-cy="reuseModalContent"]')
            .contains('Cannot initiate skill reuse while skill finalization is pending');
        cy.waitForBackendAsyncTasksToComplete();
    });

    it('display disabled and reused counts on a subject page stats card', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 10);
        cy.createSkill(2, 1, 11);
        cy.exportSkillToCatalog(2, 1, 10);
        cy.exportSkillToCatalog(2, 1, 11);

        cy.importSkillFromCatalog(1, 1, 2, 10);
        cy.importSkillFromCatalog(1, 1, 2, 11);

        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('have.text', '200');

        cy.get('[data-cy="subj1_card"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]')
            .should('have.text', 1);
        cy.get('[data-cy="subj1_card"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .should('have.text', 200);
        cy.get('[data-cy="subj1_card"] [data-cy="pagePreviewCardStat_# Skills_reused"]')
            .should('not.exist');
        cy.get('[data-cy="subj1_card"] [data-cy="pagePreviewCardStat_Points_reused"]')
            .should('not.exist');
        cy.get('[data-cy="subj1_card"] [data-cy="pagePreviewCardStat_# Skills_disabled"]')
            .should('have.text', '2');

        cy.get('[data-cy="subj2_card"] [data-cy="pagePreviewCardStat_# Skills"] [data-cy="statNum"]')
            .should('have.text', 0);
        cy.get('[data-cy="subj2_card"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .should('have.text', 0);
        cy.get('[data-cy="subj2_card"] [data-cy="pagePreviewCardStat_# Skills_reused"]')
            .should('have.text', '1');
        cy.get('[data-cy="subj2_card"] [data-cy="pagePreviewCardStat_Points_reused"]')
            .should('have.text', '200');

        cy.get('[data-cy="manageBtn_subj1"]')
            .click();
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('not.exist');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('not.exist');

        cy.get('[data-cy="breadcrumb-proj1"]')
            .click();
        cy.get('[data-cy="manageBtn_subj2"]')
            .click();
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '0');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('not.exist');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('have.text', '200');
    });

    it('display disabled and reused counts on a project(s) page stats card', () => {
        cy.reuseSkillIntoAnotherSubject(1, 1, 2);

        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 10);
        cy.createSkill(2, 1, 11);
        cy.exportSkillToCatalog(2, 1, 10);
        cy.exportSkillToCatalog(2, 1, 11);

        cy.importSkillFromCatalog(1, 1, 2, 10);
        cy.importSkillFromCatalog(1, 1, 2, 11);

        cy.visit('/administrator/');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Skills"] [data-cy="statNum"]')
            .should('have.text', '1');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Skills_disabled"]')
            .should('have.text', '2');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Skills_reused"]')
            .should('have.text', '1');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .should('have.text', '200');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Points_disabled"]')
            .should('not.exist');
        cy.get('[data-cy="projectCard_proj1"] [data-cy="pagePreviewCardStat_Points_reused"]')
            .should('have.text', '200');

        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Skills"] [data-cy="statNum"]')
            .should('have.text', '2');
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Skills_disabled"]')
            .should('not.exist');
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Skills_reused"]')
            .should('not.exist');

        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Points"] [data-cy="statNum"]')
            .should('have.text', '400');
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Points_disabled"]')
            .should('not.exist');
        cy.get('[data-cy="projectCard_proj2"] [data-cy="pagePreviewCardStat_Points_reused"]')
            .should('not.exist');

        cy.get('[data-cy="projCard_proj1_manageLink"]')
            .click();
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '200');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('have.text', '1');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('have.text', '200');

        cy.get('[data-cy="breadcrumb-Projects"]')
            .click();
        cy.get('[data-cy="projCard_proj2_manageLink"]')
            .click();
        cy.get('[data-cy="pageHeaderStat_Skills"] [data-cy="statValue"]')
            .should('have.text', '2');
        cy.get('[data-cy="pageHeaderStat_Points"] [data-cy="statValue"]')
            .should('have.text', '400');
        cy.get('[data-cy="pageHeaderStats_Skills_reused"]')
            .should('not.exist');
        cy.get('[data-cy="pageHeaderStats_Skills_disabled"]')
            .should('not.exist');
        cy.get('[data-cy="pageHeaderStats_Points_reused"]')
            .should('not.exist');

    });

});
