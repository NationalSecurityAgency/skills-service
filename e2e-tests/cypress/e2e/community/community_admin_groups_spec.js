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

describe('Community Admin Group Tests', () => {

    const allDragonsUser = 'allDragons@email.org'
    const adminGroupTableSelector = '[data-cy="adminGroupDefinitionsTable"]';
    beforeEach( () => {

        cy.intercept('GET', '/admin/admin-group-definitions/adminGroup1/userRoles**')
            .as('loadUserRoles');
        cy.intercept('POST', '*suggestDashboardUsers*').as('suggest');

        cy.fixture('vars.json').then((vars) => {
            cy.request('POST', '/logout');
            cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request({
                method: 'POST',
                url: '/root/saveSystemSettings',
                body: {
                    customHeader: '<div class="bg-success text-center text-white py-1 community-desc-header">{{ community.descriptor}}</div>',
                    customFooter: '<div class="bg-success text-center text-white py-1 community-desc-footer">{{ community.descriptor}}</div>',
                }
            });
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${Cypress.env('proxyUser')}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.loginAsAdminUser();
            cy.createProject(1, {enableProtectedUserCommunity: true})
            cy.createProject(2)

            cy.createQuizDef(1, {enableProtectedUserCommunity: true})
            cy.createQuizDef(2);


            cy.request('POST', `/admin/projects/proj2/users/${allDragonsUser}/roles/ROLE_PROJECT_ADMIN`);

            cy.createAdminGroupDef(1, { name: 'UC Protected Admin Group', enableProtectedUserCommunity: true });
            cy.createAdminGroupDef(2, { name: 'Non-UC Protected Admin Group', enableProtectedUserCommunity: false });
        });
    });

    it('create restricted community admin group', function () {
        cy.visit('/administrator/adminGroups/')

        cy.get('[data-cy="btn_Admin Groups"]').click()
        cy.get('.p-dialog-header').contains('New Admin Group')

        cy.get('[data-cy="adminGroupName"]').type('My First Admin Group')
        cy.get('[data-cy="idInputValue"]').should('have.value', 'MyFirstAdminGroup')

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"]').should('not.exist')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="adminGroupName"]').should('not.exist')
        cy.get('[data-cy="btn_Admin Groups"]').should('have.focus')
        cy.validateTable(adminGroupTableSelector, [
            [{
                colIndex: 0,
                value: 'UC Protected Admin Group'
            }],

            [{
                colIndex: 0,
                value: 'Non-UC Protected Admin Group'
            }],
            [{
                colIndex: 0,
                value: 'My First Admin GroupFor Divine Dragon NationManage'
            }],
        ], 5);

        cy.get('[data-cy="managesAdminGroupBtn_MyFirstAdminGroup"]').click()
        cy.get('[data-cy="subTitle"]').contains('For Divine Dragon Nation')
    });

    it('can assign UC user as group owner to UC protected admin group', function () {
        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Group Owner"]').click();
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click()
        cy.get('[data-cy="userCell_root@skills.org"]')
        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '2');

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.contains('No results found')
    })

    it('can assign UC user as group member to UC protected admin group', function () {
        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains('root@skills.org').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Group Owner"]').click();
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click()
        cy.get('[data-cy="userCell_root@skills.org"]')
        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '2');

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.wait('@suggest');
        cy.wait(500);
        cy.contains('No results found')
    })

    it('cannot assign non-UC user as group owner to UC protected admin group', function () {
        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="existingUserInput"]').type(allDragonsUser);
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains(allDragonsUser).click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Group Owner"]').click();
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click()
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');

        cy.get('[data-cy="error-msg"]').contains('Error! Request could not be completed! User [allDragons@email.org] is not allowed to be assigned [Admin Group Owner] user role for admin group [adminGroup1]');

        cy.get('[data-cy="existingUserInput"]').type(allDragonsUser);
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains(allDragonsUser)
    })

    it('cannot assign non-UC user as group member to UC protected admin group', function () {
        cy.visit('/administrator/adminGroups/adminGroup1');
        cy.wait('@loadUserRoles');

        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');
        cy.get('[data-cy="existingUserInput"]').type(allDragonsUser);
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains(allDragonsUser).click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Group Member"]').click();
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="addUserBtn"]').click()
        cy.get('[data-cy="userCell_root@skills.org"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Members"] [data-cy="statValue"]').should('have.text', '1');

        cy.get('[data-cy="error-msg"]').contains('Error! Request could not be completed! User [allDragons@email.org] is not allowed to be assigned [Admin Group Member] user role for admin group [adminGroup1]');

        cy.get('[data-cy="existingUserInput"]').type(allDragonsUser);
        cy.wait('@suggest');
        cy.wait(500);
        cy.get('[data-pc-section="option"]').contains(allDragonsUser)
    })

    it('cannot add UC protected project to a non-UC admin group', function () {
        cy.intercept('GET', '/admin/admin-group-definitions/adminGroup2/projects')
            .as('loadGroupProjects');
        cy.visit('/administrator/adminGroups/adminGroup2/group-projects');
        cy.wait('@loadGroupProjects');
        cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="projectSelector"] [data-pc-section="label"]').contains('Search available projects...').should('be.visible')
        cy.get('[data-cy="projectSelector"]').click()
        cy.get('[data-cy="availableProjectSelection-proj1"]').click()

        cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="error-msg"]').contains('Error! Request could not be completed! Project [This is project 1] is not allowed to be assigned [Non-UC Protected Admin Group] Admin Group')
    })

    it('cannot add UC protected quiz to a non-UC admin group', function () {
        cy.intercept('GET', '/admin/admin-group-definitions/adminGroup2/quizzes').as('loadGroupQuizzes');
        cy.visit('/administrator/adminGroups/adminGroup2/group-quizzes');
        cy.wait('@loadGroupQuizzes');
        cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="quizSelector"] [data-pc-section="label"]').contains('Search available quizzes and surveys...').should('be.visible')
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('[data-cy="availableQuizSelection-quiz1"]').click()

        cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="error-msg"]').contains('Error! Request could not be completed! Admin Group [Non-UC Protected Admin Group] is not allowed to be assigned to [This is quiz 1] Quiz as the group does not have Divine Dragon permission')
    })

    it('when adding quiz to an admin group and an error occurs redirect to the error page', () => {
        cy.intercept('POST', '/admin/admin-group-definitions/adminGroup1/quizzes/quiz1', { statusCode: 500 }).as('saveEndpoint')
        cy.intercept('GET', '/admin/admin-group-definitions/adminGroup1/quizzes').as('loadGroupQuizzes');
        cy.visit('/administrator/adminGroups/adminGroup1/group-quizzes');
        cy.wait('@loadGroupQuizzes');
        cy.get('[data-cy="pageHeaderStat_Quizzes and Surveys"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="quizSelector"] [data-pc-section="label"]').contains('Search available quizzes and surveys...').should('be.visible')
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('[data-cy="availableQuizSelection-quiz1"]').click()

        cy.wait('@saveEndpoint')
        cy.get('[data-cy="errorPage"]').contains("Failed to add Quiz to Admin Group")
        cy.url().should('include', '/error')
    })

    it('when adding project to an admin group and an error occurs redirect to the error page', () => {
        cy.intercept('POST', '/admin/admin-group-definitions/adminGroup1/projects/proj1', { statusCode: 500 }).as('saveEndpoint')
        cy.intercept('GET', '/admin/admin-group-definitions/adminGroup1/projects').as('loadGroupProjects');
        cy.visit('/administrator/adminGroups/adminGroup1/group-projects');
        cy.wait('@loadGroupProjects');
        cy.get('[data-cy="pageHeaderStat_Projects"] [data-cy="statValue"]').should('have.text', '0');
        cy.get('[data-cy="noContent"]')
        cy.get('[data-cy="projectSelector"] [data-pc-section="label"]').contains('Search available projects...').should('be.visible')
        cy.get('[data-cy="projectSelector"]').click()
        cy.get('[data-cy="availableProjectSelection-proj1"]').click()

        cy.wait('@saveEndpoint')
        cy.get('[data-cy="errorPage"]').contains("Failed to add Project to Admin Group")
        cy.url().should('include', '/error')
    })

    it('cannot enable UC protection for admin group if it contains a non UC member', function () {
        cy.request('PUT', `/admin/admin-group-definitions/adminGroup2/users/${allDragonsUser}/roles/ROLE_ADMIN_GROUP_OWNER`);

        cy.visit('/administrator/adminGroups/')
        cy.get('[data-cy="editAdminGroupButton_adminGroup2"]').click()

        cy.get('[data-cy="restrictCommunityControls"]').contains('Access to Divine Dragon users only')
        cy.get('[data-cy="userCommunityDocsLink"]').should('not.exist')
        const warningMsg = 'Please note that once the restriction is enabled it cannot be lifted/disabled';
        cy.get('[data-cy="restrictCommunityControls"]').contains(warningMsg).should('not.exist')
        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="communityProtectionErrors"]').contains('Unable to restrict access to Divine Dragon users only')
        cy.get('[data-cy="communityProtectionErrors"]').contains(`Has existing ${allDragonsUser} user that is not authorized`)
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="communityRestrictionWarning"]').should('not.exist')
    })

    it('cannot disable UC protection for admin group after it has already been enabled', function () {
        cy.visit('/administrator/adminGroups/')
        cy.get('[data-cy="editAdminGroupButton_adminGroup1"]').click()
        cy.get('[data-cy="restrictCommunityControls"]').contains('Access is restricted to Divine Dragon users only and cannot be lifted/disabled');
        cy.get('[data-cy="restrictCommunity"]').should('not.exist')
    })

    it('cannot enable UC protection for project that belongs to a non-UC admin group', function () {
        cy.request('PUT', `/admin/admin-group-definitions/adminGroup2/users/${allDragonsUser}/roles/ROLE_ADMIN_GROUP_OWNER`);
        cy.addProjectToAdminGroupDef(2, 2)

        cy.visit('/administrator/')
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.get('[data-cy="projectCard_proj2"] [data-cy="editProjBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="communityProtectionErrors"]').should('not.exist')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="communityProtectionErrors"]').contains('Unable to restrict access to Divine Dragon users only')
        cy.get('[data-cy="communityProtectionErrors"]').contains('Has existing allDragons@email.org user that is not authorized')
        cy.get('[data-cy="communityProtectionErrors"]').contains('This project is part of one or more Admin Groups that has not enabled user community protection')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="communityRestrictionWarning"]').should('not.exist')
    })

    it('cannot enable UC protection for quiz that belongs to a non-UC admin group', function () {
        cy.request('PUT', `/admin/admin-group-definitions/adminGroup2/users/${allDragonsUser}/roles/ROLE_ADMIN_GROUP_OWNER`);
        cy.addQuizToAdminGroupDef(2, 2)

        cy.visit('/administrator/quizzes/')
        // cy.get('[data-cy="noQuizzesYet"]')
        cy.get('[data-cy="editQuizButton_quiz2"]').click()
        cy.get('[data-cy="markdownEditorInput"]')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="communityProtectionErrors"]').should('not.exist')

        cy.get('[data-cy="restrictCommunity"] [data-pc-section="input"]').click()
        cy.get('[data-cy="communityProtectionErrors"]').contains('Unable to restrict access to Divine Dragon users only')
        cy.get('[data-cy="communityProtectionErrors"]').contains('Has existing allDragons@email.org user that is not authorized')
        cy.get('[data-cy="communityProtectionErrors"]').contains('This quiz is part of one or more Admin Groups that do no have Divine Dragon permission')
        cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
        cy.get('[data-cy="communityRestrictionWarning"]').should('not.exist')
    })


});
