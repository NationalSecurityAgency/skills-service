/*
 * Copyright 2026 SkillTree
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

describe('Remove a project user', () => {
    const removeButton = '[data-cy="removeProjectUserBtn"]';
    const confirmRemoval = () => {
        cy.openDialog(removeButton);
        cy.get('[data-cy="currentValidationText"]').type('Delete Me');
        cy.get('[data-cy="saveDialogBtn"]').click();
    };
    const closeSuccessNotification = () => {
        cy.contains('[role="alertdialog"]', 'User Removed').should('be.visible');
        cy.get('[role="alertdialog"]').contains('button', 'Ok').click();
    };
    const createCatalog = () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 3);
        cy.exportSkillToCatalog(2, 1, 3);
        cy.importSkillFromCatalog(1, 1, 2, 3);
        cy.finalizeCatalogImport(1);
        cy.waitForBackendAsyncTasksToComplete();
    };

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
    });

    it('requires confirmation and removes earned progress, while admin preview does not re-register the trainee', () => {
        cy.reportSkill(1, 1, 'user1', 'now');
        cy.reportSkill(1, 1, 'user2', 'now');
        cy.visit('/administrator/projects/proj1/users/user1');
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '100');
        cy.get(removeButton).should('be.enabled');
        cy.openDialog(removeButton);
        cy.get('[data-cy="removalSafetyCheckMsg"]').should('contain.text', 'cannot be undone')
            .and('contain.text', 'This action cannot be undone and permanently deletes');
        cy.get('[data-cy="removeProjectUserQuizWarning"]').should('be.visible')
            .and('contain.text', 'Quizzes:')
            .and('contain.text', 'If this project has skills associated with quizzes')
            .and('contain.text', "this user's passed attempts for those quizzes will also be removed")
            .and('contain.text', 'Their progress in other projects that use the same quizzes may be affected');
        cy.get('[data-cy="removeProjectUserCatalogWarning"]').should('be.visible')
            .and('contain.text', 'Skills Catalog:')
            .and('contain.text', 'If this project exports skills to the Skills Catalog')
            .and('contain.text', "may also remove their points and achievements in other projects that import those skills");
        cy.get('[data-cy="removeProjectUserAccountWarning"]').should('be.visible')
            .and('contain.text', 'Not removed:')
            .and('contain.text', "The user's account, roles, and project access");
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="currentValidationText"]').type('delete me');
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled');
        cy.get('[data-cy="closeDialogBtn"]').click();
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '100');

        confirmRemoval();
        cy.location('pathname').should('eq', '/administrator/projects/proj1/users');
        closeSuccessNotification();
        cy.get('[data-cy="usersTable"]').should('contain.text', 'user2').and('not.contain.text', 'user1');

        cy.visit('/administrator/projects/proj1/users/user1');
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '0');
        cy.visit('/administrator/projects/proj1/users');
        cy.get('[data-cy="usersTable"]').should('contain.text', 'user2').and('not.contain.text', 'user1');
    });

    it('removes a zero-point visit-only participant without removing their admin access, and permits re-entry', () => {
        cy.request('POST', '/api/projects/proj1/progress/initialize', {});
        cy.visit(`/administrator/projects/proj1/users/${Cypress.env('proxyUser')}`);
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '0');
        cy.get(removeButton).should('be.enabled');
        confirmRemoval();
        cy.location('pathname').should('eq', '/administrator/projects/proj1/users');
        closeSuccessNotification();
        cy.get('[data-cy="usersTable"] [data-cy="skillsBTableTotalRows"]').should('have.text', '0');

        cy.visit('/progress-and-rankings/projects/proj1');
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '0');
        cy.visit('/administrator/projects/proj1/users');
        cy.get('[data-cy="usersTable"]').should('contain.text', Cypress.env('proxyUser'));
    });

    ['RUNNING', 'FAILED'].forEach((state) => {
        it(`shows a retry error during ${state} catalog finalization without navigating away or removing progress`, () => {
            createCatalog();
            cy.reportSkill(1, 1, 'user1', 'now');
            cy.request('POST', '/admin/projects/proj1/settings/catalog.finalize.state', {
                projectId: 'proj1', setting: 'catalog.finalize.state', value: state
            });
            cy.visit('/administrator/projects/proj1/users/user1');
            cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '100');
            confirmRemoval();
            cy.get('[data-cy="removeProjectUserError"]').should('be.visible')
                .and('contain.text', state === 'RUNNING' ? 'Please try again after finalization completes.' : 'Please resolve finalization before trying again.');
            cy.location('pathname').should('eq', '/administrator/projects/proj1/users/user1');
            cy.get(removeButton).should('be.enabled');
            cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '100');

            cy.request('POST', '/admin/projects/proj1/settings/catalog.finalize.state', {
                projectId: 'proj1', setting: 'catalog.finalize.state', value: 'COMPLETED'
            });
            confirmRemoval();
            cy.location('pathname').should('eq', '/administrator/projects/proj1/users');
            closeSuccessNotification();
            cy.get('[data-cy="usersTable"]').should('not.contain.text', 'user1');
        });
    });

    it('does not show removal to a project approver', () => {
        const approver = 'removal-approver@skills.org';
        cy.reportSkill(1, 1, 'user1', 'now');
        cy.register(approver, 'password');
        cy.loginAsAdminUser();
        cy.request('POST', `/admin/projects/proj1/users/${approver}/roles/ROLE_PROJECT_APPROVER`);
        cy.logout();
        cy.login(approver, 'password');
        cy.visit('/administrator/projects/proj1/users/user1');
        cy.get('[data-cy="overallPoints"] [data-cy="earnedPoints"]').should('have.text', '100');
        cy.get(removeButton).should('not.exist');
    });
});
