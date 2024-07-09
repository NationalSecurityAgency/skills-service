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

describe('Client Display Skills Imported from Catalog Tests', () => {

    beforeEach(() => {

    });

    it('display imported skill along other skills', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(2, 1, 2, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(2, 1, 3, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);
        cy.exportSkillToCatalog(2, 1, 3);

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.importSkillFromCatalog(1, 1, 2, 1);
        cy.createSkill(1, 1, 4, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.bulkImportSkillFromCatalogAndFinalize(1, 1, [
            {
                projNum: 2,
                skillNum: 2
            },
            {
                projNum: 2,
                skillNum: 3
            },
        ]);

        cy.createSkillsGroup(1, 1, 5);
        cy.addSkillToGroup(1, 1, 5, 6);
        cy.addSkillToGroup(1, 1, 5, 7);
        cy.createSkillsGroup(1, 1, 5, {
            enabled: true,
            numSkillsRequired: 1
        });

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1 in This is project 2');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 4');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('Very Great Skill 2 in This is project 2');
        cy.get('[data-cy="skillProgress_index-3"]')
            .contains('Very Great Skill 3 in This is project 2');
        cy.get('[data-cy="skillProgress_index-4"]')
            .contains('Awesome Group 5 Subj1');
    });

    it('skills details', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.' });
        cy.createSkill(2, 1, 2, {
            selfReportingType: 'HonorSystem',
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        });
        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.bulkImportSkillFromCatalogAndFinalize(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
            {
                projNum: 2,
                skillNum: 2
            },
        ]);
        cy.createSkill(1, 1, 3);

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1 in This is project 2');
        cy.get('[data-cy="skillProgress_index-1"]')
            .contains('Very Great Skill 2 in This is project 2');
        cy.get('[data-cy="skillProgress_index-2"]')
            .contains('Very Great Skill 3');

        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="catalogImportStatus"]')
            .contains('This skill is originally defined in This is project 2 and re-used in this project! Navigate to This is project 2 project to perform Very Great Skill 1 skill.');
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="catalogImportStatus"]')
            .contains('This skill is originally defined in This is project 2 and re-used in this project! This skill can be self-reported via the button below.');
        cy.get('[data-cy="skillDescription-skill3"] [data-cy="catalogImportStatus"]')
            .should('not.exist');

        cy.get('[data-cy="skillDescription-skill1"] [data-cy="claimPointsBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="claimPointsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy="skillDescription-skill3"] [data-cy="claimPointsBtn"]')
            .should('not.exist');

        // check single pages
        cy.cdClickSkill(0);
        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 1');
        cy.get('[data-cy="importedFromProj"]').should('have.text', 'This is project 2');
        cy.get('[data-cy="catalogImportStatus"]')
            .contains('This skill is originally defined in This is project 2 and re-used in this project! Navigate to This is project 2 project to perform Very Great Skill 1 skill.');
        cy.get('[data-cy="claimPointsBtn"]')
            .should('not.exist');

        cy.cdBack('Subject 1');
        cy.cdClickSkill(1);
        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 2');
        cy.get('[data-cy="importedFromProj"]').should('have.text', 'This is project 2');
        cy.get('[data-cy="catalogImportStatus"]')
            .contains('This skill is originally defined in This is project 2 and re-used in this project! This skill can be self-reported via the button below.');
        cy.get('[data-cy="claimPointsBtn"]')
            .should('be.enabled');

        cy.cdBack('Subject 1');
        cy.cdClickSkill(2);
        cy.get('[data-cy="skillProgressTitle"]')
            .contains('Very Great Skill 3');
        cy.get('[data-cy="importedFromProj"]').should('not.exist');
        cy.get('[data-cy="catalogImportStatus"]')
            .should('not.exist');
        cy.get('[data-cy="claimPointsBtn"]')
            .should('not.exist');
    });

    it('self report imported skill', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, {
            selfReportingType: 'HonorSystem',
            pointIncrementInterval: 0,
            description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.'
        });
        cy.exportSkillToCatalog(2, 1, 1);

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.bulkImportSkillFromCatalogAndFinalize(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
        ]);

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy="skillProgress_index-0"]')
            .contains('Very Great Skill 1 in This is project 2');

        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="claimPointsBtn"]')
            .click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="selfReportAlert"]')
            .contains('Congrats! You just earned 100 points!');
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('100 / 200 Points');

        cy.waitForBackendAsyncTasksToComplete();

        cy.cdClickSkill(0);
        cy.get('[data-cy="claimPointsBtn"]')
            .click();
        cy.get('[data-cy="selfReportAlert"]')
            .contains('Congrats! You just earned 100 points and completed the skill!');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]')
            .contains('200 / 200 Points');
    });

    it('catalog imported skill visual regression', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 1);

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.bulkImportSkillFromCatalogAndFinalize(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
        ]);

        cy.cdVisit('/');
        cy.cdClickSubj(0);

        cy.matchSnapshotImageForElement('[data-cy="skillProgress_index-0"]', {
            name: 'catalog imported skill visual regression - skill preview',
            blackout: '[data-cy="skillProgressTitle"]',
            errorThreshold: 0.05
        });

        cy.cdClickSkill(0);
        cy.matchSnapshotImageForElement('[data-cy="skillProgress"]', {
            name: 'catalog imported skill visual regression - skill details',
            errorThreshold: 0.05
        });

        cy.cdVisit('/?enableTheme=true');
        cy.cdClickSubj(0);

        cy.matchSnapshotImageForElement('[data-cy="skillProgress_index-0"]', {
            name: 'catalog imported skill visual regression - skill preview themed',
            errorThreshold: 0.05
        });

        cy.cdClickSkill(0);
        cy.matchSnapshotImageForElement('[data-cy="skillProgress"]', {
            name: 'catalog imported skill visual regression - skill details themed',
            errorThreshold: 0.05
        });
    });

    it('catalog imported skill has self-report approval request', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { selfReportingType: 'Approval', });
        cy.createSkill(2, 1, 2, { selfReportingType: 'Approval', });
        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.bulkImportSkillFromCatalogAndFinalize(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
            {
                projNum: 2,
                skillNum: 2
            },
        ]);

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.cdClickSkill(0);
        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSubmitBtn"]')
            .click();
        cy.get('[data-cy="selfReportAlert"]')
            .contains('Submitted successfully');
        cy.get('[data-cy="selfReportAlert"]')
            .contains('requires approval');

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy="toggleSkillDetails"]')
            .click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="pendingApprovalStatus"]')
            .contains('pending approval');
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="requestApprovalBtn"]')
            .should('not.exist');
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="pendingApprovalStatus"]')
            .should('not.exist');
        cy.get('[data-cy="skillDescription-skill2"] [data-cy="requestApprovalBtn"]')
            .should('be.enabled');

        cy.cdClickSkill(0);
        cy.get('[data-cy="pendingApprovalStatus"]')
            .contains('pending approval');
        cy.get('[data-cy="requestApprovalBtn"]')
            .should('not.exist');
    });

    it('gracefully handle self-reporting for the imported skill which already has pending approval reported another way after the page was loaded', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, { selfReportingType: 'Approval', });
        cy.createSkill(2, 1, 2, { selfReportingType: 'Approval', });
        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.bulkImportSkillFromCatalogAndFinalize(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
            {
                projNum: 2,
                skillNum: 2
            },
        ]);

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="requestApprovalBtn"]');

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.waitForBackendAsyncTasksToComplete();

        cy.get('[data-cy="skillDescription-skill1"] [data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSubmitBtn"] ')
            .click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="selfReportError"]')
            .contains('This skill was already submitted for approval and is still pending approval');

        cy.cdClickSkill(1);
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSubmitBtn"] ')
            .click();
        cy.get('[data-cy="selfReportError"]')
            .contains('This skill was already submitted for approval and is still pending approval');
    });

    it('gracefully handle self-reporting for the imported skill that was achieved another way after the page was loaded', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1, {
            selfReportingType: 'Approval',
            numPerformToCompletion: 1
        });
        cy.createSkill(2, 1, 2, {
            selfReportingType: 'Approval',
            numPerformToCompletion: 1
        });
        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.bulkImportSkillFromCatalogAndFinalize(1, 1, [
            {
                projNum: 2,
                skillNum: 1
            },
            {
                projNum: 2,
                skillNum: 2
            },
        ]);

        cy.cdVisit('/');
        cy.cdClickSubj(0);
        cy.get('[data-cy=toggleSkillDetails]')
            .click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="requestApprovalBtn"]');

        cy.reportSkill(1, 1, Cypress.env('proxyUser'), 'now');
        cy.waitForBackendAsyncTasksToComplete();
        cy.approveRequest(2);
        cy.waitForBackendAsyncTasksToComplete();

        cy.get('[data-cy="skillDescription-skill1"] [data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSubmitBtn"] ')
            .click();
        cy.get('[data-cy="skillDescription-skill1"] [data-cy="selfReportError"]')
            .contains('This skill reached its maximum points');

        cy.cdClickSkill(1);
        cy.reportSkill(1, 2, Cypress.env('proxyUser'), 'now');
        cy.waitForBackendAsyncTasksToComplete();
        cy.approveRequest(2);
        cy.waitForBackendAsyncTasksToComplete();
        cy.get('[data-cy="requestApprovalBtn"]')
            .click();
        cy.get('[data-cy="selfReportSubmitBtn"] ')
            .click();
        cy.get('[data-cy="selfReportError"]')
            .contains('This skill reached its maximum points');
    });

});


