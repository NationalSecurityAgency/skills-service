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
describe('Inception Skills Tests', () => {

    let defaultUser
    beforeEach(() => {
        cy.fixture('vars.json')
            .then((vars) => {
                defaultUser = Cypress.env('oauthMode') ? 'foo-hydra' : vars.defaultUser
            })


        Cypress.Commands.add("assertInceptionPoints", (subjectId, skillId, points, waitOnReport = true) => {
            cy.intercept('POST', `/api/projects/Inception/skills/${skillId}`)
                .as('reportSkill');

            if (waitOnReport) {
                cy.wait('@reportSkill')
            }
            const url = `/api/projects/Inception/subjects/${subjectId}/skills/${skillId}/summary`;
            cy.request(url)
                .then((response) => {
                    expect(response.body).to.have.property('points', points)
                })


        })

    });

    it('copy project via card', () => {
        cy.createProject(1);
        cy.assertInceptionPoints('Projects', 'CopyProject', 0, false)
        cy.visit('/administrator/');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.get('[data-cy="saveDialogBtn"]')
            .click();
        cy.get('[data-cy="lengthyOpModal"] [data-cy="successMessage"]')
            .contains('Project\'s training profile was successfully copied');
        cy.get('[data-cy="allDoneBtn"]')
            .click();
        cy.get('[data-cy="projCard_NewProject_manageBtn"]');

        cy.assertInceptionPoints('Projects', 'CopyProject', 50)
    });

    it.skip('share project', () => {
        // this is needed to grant headless chrome permissions to copy-and-paste
        cy.wrap(Cypress.automation('remote:debugger:protocol', {
            command: 'Browser.grantPermissions',
            params: {
                permissions: ['clipboardReadWrite', 'clipboardSanitizedWrite'],
                origin: window.location.origin,
            },
        }))

        cy.assertInceptionPoints('Projects', 'ShareProject', 0, false)

        cy.createProject(1);
        cy.enableProdMode(1);
        cy.visit('/administrator/projects/proj1')
        cy.contains('No Subjects Yet')
        cy.get('[data-cy="shareProjBtn"]')
            .realClick()
        cy.get('[data-cy="shareProjOkBtn"]')

        cy.assertInceptionPoints('Projects', 'ShareProject', 50)
    });

    it('change subject display order', () => {
        cy.intercept('/admin/projects/proj1/subjects/subj1').as('subj1Async')
        cy.assertInceptionPoints('Projects', 'ChangeSubjectDisplayOrder', 0, false)

        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)

        const subj1Card = '[data-cy="subjectCard-subj1"] [data-cy="sortControlHandle"]';
        const subj2Card = '[data-cy="subjectCard-subj2"] [data-cy="sortControlHandle"]';

        cy.visit('/administrator/projects/proj1')
        cy.validateElementsOrder('[data-cy="subjectCard"]', ['Subject 1', 'Subject 2']);
        cy.get(subj1Card).dragAndDrop(subj2Card)
        cy.wait('@subj1Async')
        cy.validateElementsOrder('[data-cy="subjectCard"]', ['Subject 2', 'Subject 1']);

        cy.assertInceptionPoints('Projects', 'ChangeSubjectDisplayOrder', 25)
    });

    it('change badge display order', () => {
        cy.assertInceptionPoints('Projects', 'ChangeBadgeDisplayOrder', 0, false)

        cy.createProject(1);
        cy.createBadge(1, 1);
        cy.createBadge(1, 2);

        cy.visit('/administrator/projects/proj1/badges');

        const badge1Card = '[data-cy="badgeCard-badge1"] [data-cy="sortControlHandle"]';
        const badge2Card = '[data-cy="badgeCard-badge2"] [data-cy="sortControlHandle"]';

        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 1', 'Badge 2']);
        cy.get(badge1Card)
            .dragAndDrop(badge2Card);
        cy.validateElementsOrder('[data-cy="badgeCard"]', ['Badge 2', 'Badge 1']);

        cy.assertInceptionPoints('Projects', 'ChangeBadgeDisplayOrder', 25)
    });

    it('Search and Navigate directly to a skill', () => {
        cy.assertInceptionPoints('Skills', 'SearchandNavigatedirectlytoaskill', 0, false)

        cy.createProject(1);
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1);
        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="skillsSelector"]').click();
        cy.get('li.p-dropdown-empty-message').contains('Type to search for skills').should('be.visible')
        cy.get(`[data-pc-section="filterinput"]`).type('s')

        cy.get('[data-cy="skillsSelectionItem-skillId"]').should('have.length', 1).as('skillIds');
        cy.get('@skillIds').eq(0).click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill1')

        cy.assertInceptionPoints('Skills', 'SearchandNavigatedirectlytoaskill', 5)
    });

    it('copy skill', () => {
        cy.createProject(1);
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1)

        cy.assertInceptionPoints('Skills', 'CopySkill', 0, false)

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="copySkillButton_skill1"]').click()
        cy.get('[data-cy="skillName"]').should('have.value', "Copy of Very Great Skill 1")

        cy.assertInceptionPoints('Skills', 'CopySkill', 10)
    });

    it('create skill group', () => {
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newGroupButton"]')
            .click();

        cy.get('[data-cy="name"]')
            .type('Group');

        cy.get('[data-cy="saveDialogBtn"]')
            .click();
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtoggler"]`).click()
        cy.get(`[data-cy="addSkillToGroupBtn-GroupGroup"]`).click();
        cy.get('[data-cy="skillName"]').type('Skill');

        cy.assertInceptionPoints('Skills', 'CreateSkillGroup', 0, false)

        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist');

        cy.assertInceptionPoints('Skills', 'CreateSkillGroup', 25)
    });

    it('create skill', () => {
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newSkillButton"]')
            .click();

        cy.get('[data-cy="skillName"]')
            .type('Skill');

        cy.assertInceptionPoints('Skills', 'CreateSkill', 0, false)

        cy.get('[data-cy="saveDialogBtn"]').click();
        cy.get('[data-cy="manageSkillLink_SkillSkill"]')

        cy.assertInceptionPoints('Skills', 'CreateSkill', 10)
        // because crate group credit is given when group's skill is saved
        // it is wise to make sure that the credit is not given mistakenly
        // cy.assertInceptionPoints('Skills', 'CreateSkillGroup', 0, false)
    });

    it('change skill display order - move up', () => {
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        const tableSelector = '[data-cy="skillsTable"]'
        cy.get(`${tableSelector} th`).contains('Display').click();

        // enable reorder should add buttons and sort by display order
        cy.get('[data-cy="enableDisplayOrderSort"]').click()

        cy.assertInceptionPoints('Skills', 'ChangeSkillDisplayOrder', 0, false)

        cy.get('[data-cy="orderMoveUp_skill2"]').click();

        cy.get('[data-cy="orderMoveUp_skill1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill1"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_skill2"]').should('be.disabled');
        cy.get('[data-cy="orderMoveDown_skill2"]').should('be.enabled');

        cy.assertInceptionPoints('Skills', 'ChangeSkillDisplayOrder', 5)
    })

    it('change skill display order - move down', () => {
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        const tableSelector = '[data-cy="skillsTable"]'
        cy.get(`${tableSelector} th`).contains('Display').click();


        // enable reorder should add buttons and sort by display order
        cy.get('[data-cy="enableDisplayOrderSort"]').click()

        cy.assertInceptionPoints('Skills', 'ChangeSkillDisplayOrder', 0, false)

        cy.get('[data-cy="orderMoveDown_skill1"]').click();

        cy.get('[data-cy="orderMoveUp_skill1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill1"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_skill2"]').should('be.disabled');
        cy.get('[data-cy="orderMoveDown_skill2"]').should('be.enabled');

        cy.assertInceptionPoints('Skills', 'ChangeSkillDisplayOrder', 5)
    })

    it('use skill table additional columns', () => {
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.assertInceptionPoints('Skills', 'SkillsTableAdditionalColumns', 0, false)
        // cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Time Window').click();
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="trigger"]').click()
        cy.get('[data-pc-section="panel"] [aria-label="Time Window"]').click()
        cy.assertInceptionPoints('Skills', 'SkillsTableAdditionalColumns', 5)
    })


    it('reuse skill', () => {
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Reuse in this Project"]').click()

        // step 1
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be reused in the [Subject 2] subject.');
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
          .click();

        // step 3
        cy.assertInceptionPoints('Skills', 'ReuseSkill', 0, false)
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 1 skill');
        cy.assertInceptionPoints('Skills', 'ReuseSkill', 25)
    })

    it('move skill', () => {
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Move Skills"]').click()

        // step 1
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be moved to the [Subject 2] subject.');

        cy.assertInceptionPoints('Skills', 'MoveSkill', 0, false)
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
          .click();

        // step 3
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 1 skill');
        cy.assertInceptionPoints('Skills', 'MoveSkill', 10)
    })

    it('import skill', () => {
        cy.createProject(2);
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkill(2, 1, 2);

        cy.exportSkillToCatalog(2, 1, 1);
        cy.exportSkillToCatalog(2, 1, 2);

        cy.createProject(1);
        cy.createSubject(1, 1)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="importFromCatalogBtn"]')
            .click();
        cy.get('[data-p-index="0"] [data-pc-name="rowcheckbox"] input').click()
        cy.assertInceptionPoints('Skills', 'ImportSkillfromCatalog', 0, false)
        cy.get('[data-cy="importBtn"]')
            .click();
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.assertInceptionPoints('Skills', 'ImportSkillfromCatalog', 25)
    });

    it('export to catalog', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"]  [data-pc-name="headercheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.assertInceptionPoints('Dashboard', 'ExporttoCatalog', 0, false)
        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.contains('Skill [Very Great Skill 1] was successfully exported to the catalog!');
        cy.assertInceptionPoints('Dashboard', 'ExporttoCatalog', 50)
    });

    it('configure self report skill - fallback user', () => {
        const pass = 'password';
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.register('user1', pass);
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);

        cy.visit('/administrator/projects/proj1/self-report/configure');

        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 0, false)
        cy.get('[data-cy="workloadCell_user1"] [data-cy="fallbackSwitch"] input').click({force: true})
        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 25)
    });

    it('configure self report skill - add skill', () => {
        const pass = 'password';
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.register('user1', pass);
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);

        cy.visit('/administrator/projects/proj1/self-report/configure');

        const user1 = 'user1'
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noSkillConf"]`).should('exist')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).type('skill 1');
        cy.get(`[data-cy="skillsSelectionItem-proj1-skill1"]`).click()

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).should('be.enabled')

        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 0, false)
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()
        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 25)
    });

    it('configure self report skill - add user', () => {
        const pass = 'password';
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.reportSkill(1, 1, 'userA', 'now');

        cy.register('user1', pass);
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);

        cy.visit('/administrator/projects/proj1/self-report/configure');

        const user1 = 'user1'
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noUserConf"]`).should('exist')

        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"]`).click();
        cy.selectItem(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] #existingUserInput`, 'userA', true, true);
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).should('be.enabled')

        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 0, false)
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).click()
        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 25)
    });

    it('configure self report skill - add user tag conf', () => {
        const pass = 'password';
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.reportSkill(1, 1, 'userA', 'now');

        cy.register('user1', pass);
        cy.fixture('vars.json')
            .then((vars) => {
                if (!Cypress.env('oauthMode')) {
                    cy.log('NOT in oauthMode, using form login');
                    cy.login(vars.defaultUser, vars.defaultPass);
                } else {
                    cy.log('oauthMode, using loginBySingleSignOn');
                    cy.loginBySingleSignOn();
                }
            });
        cy.request('POST', `/admin/projects/proj1/users/user1/roles/ROLE_PROJECT_APPROVER`);

        cy.visit('/administrator/projects/proj1/self-report/configure');

        const user1 = 'user1'
        cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="noTagKeyConf"]`).should('exist')
        cy.get(`[data-cy="workloadCell_${user1}"]`).contains('Default Fallback - All Unmatched Requests')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).should('be.disabled')
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="userTagValueInput"]`).type('First');

        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 0, false)
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addTagKeyConfBtn"]`).click()
        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 25)
    });

    it('tag skills', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]').click()

        cy.get('[data-cy="newTag"]').type('New Tag 1')

        cy.assertInceptionPoints('Skills', 'AddOrModifyTags', 0, false)
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.assertInceptionPoints('Skills', 'AddOrModifyTags', 10)
    });

    it('untag skills', () => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.addTagToSkills();

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('TAG 1').click()
        cy.assertInceptionPoints('Skills', 'AddOrModifyTags', 0, false)
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.assertInceptionPoints('Skills', 'AddOrModifyTags', 10)
        cy.get('[data-cy="skillTag-skill1-tag1"]').should('not.exist')
    });
});
