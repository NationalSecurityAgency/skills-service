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


        Cypress.Commands.add("assertInceptionPoints", (subjectId, skillId, points) => {
            const url = `/api/projects/Inception/subjects/${subjectId}/skills/${skillId}/summary`;
            cy.request(url)
                .then((response) => {
                    expect(response.body).to.have.property('points', points)
                })


        })

    });

    it('copy project via card', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/CopyProject').as('reportSkill');
        cy.createProject(1);
        cy.assertInceptionPoints('Projects', 'CopyProject', 0)
        cy.visit('/administrator/');

        cy.get('[data-cy="projectCard_proj1"] [data-cy="copyProjBtn"]')
            .click();
        cy.get('[data-cy="projectName"]')
            .type('New Project');
        cy.clickSaveDialogBtn()
        cy.get('[data-cy="lengthyOpModal"] [data-cy="successMessage"]')
            .contains('Project\'s training profile was successfully copied');
        cy.get('[data-cy="allDoneBtn"]')
            .click();
        cy.get('[data-cy="projCard_NewProject_manageBtn"]');

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Projects', 'CopyProject', 50)
    });

    it('share project', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ShareProject').as('reportSkill');
        // this is needed to grant headless chrome permissions to copy-and-paste
        cy.wrap(Cypress.automation('remote:debugger:protocol', {
            command: 'Browser.grantPermissions',
            params: {
                permissions: ['clipboardReadWrite', 'clipboardSanitizedWrite'],
                origin: window.location.origin,
            },
        }))

        cy.assertInceptionPoints('Projects', 'ShareProject', 0)

        cy.createProject(1);
        cy.enableProdMode(1);
        cy.visit('/administrator/projects/proj1')
        cy.contains('No Subjects Yet')
        cy.get('[data-cy="shareProjBtn"]')
            .realClick()
        cy.get('[data-cy="closeDialogBtn"]')

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Projects', 'ShareProject', 50)
    });

    it('change subject display order', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ChangeSubjectDisplayOrder').as('reportSkill');
        cy.intercept('/admin/projects/proj1/subjects/subj1').as('subj1Async')
        cy.assertInceptionPoints('Projects', 'ChangeSubjectDisplayOrder', 0)

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

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Projects', 'ChangeSubjectDisplayOrder', 25)
    });

    it('change badge display order', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ChangeBadgeDisplayOrder').as('reportSkill');
        cy.assertInceptionPoints('Projects', 'ChangeBadgeDisplayOrder', 0)

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

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Projects', 'ChangeBadgeDisplayOrder', 25)
    });

    it('Search and Navigate directly to a skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/SearchandNavigatedirectlytoaskill').as('reportSkill');
        cy.assertInceptionPoints('Skills', 'SearchandNavigatedirectlytoaskill', 0)

        cy.createProject(1);
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1);
        cy.visit('/administrator/projects/proj1/');
        cy.get('[data-cy="skillsSelector"]').click();
        cy.get('li.p-autocomplete-empty-message').contains('Type to search for skills').should('be.visible')
        cy.get(`[data-cy="skillsSelector"]`).type('s')

        cy.get('[data-cy="skillsSelectionItem-skillId"]').should('have.length', 1).as('skillIds');
        cy.get('@skillIds').eq(0).click();
        cy.get('[data-cy="pageHeader"]').contains('ID: skill1')

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'SearchandNavigatedirectlytoaskill', 5)
    });

    it('copy skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/CopySkill').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1)

        cy.assertInceptionPoints('Skills', 'CopySkill', 0, false)

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="copySkillButton_skill1"]').click()
        cy.get('[data-cy="skillName"]').should('have.value', "Copy of Very Great Skill 1")

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'CopySkill', 10)
    });

    it('create skill group', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/CreateSkillGroup').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="newGroupButton"]')
            .click();

        cy.get('[data-cy="name"]')
            .type('Group');

        cy.clickSaveDialogBtn()
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.get(`[data-cy="addSkillToGroupBtn-GroupGroup"]`).click();
        cy.get('[data-cy="skillName"]').type('Skill');

        cy.assertInceptionPoints('Skills', 'CreateSkillGroup', 0)

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="saveDialogBtn"]').should('not.exist');

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'CreateSkillGroup', 25)
    });

    it('create skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/CreateSkill').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.openNewSkillDialog()

        cy.get('[data-cy="skillName"]')
            .type('Skill');

        cy.assertInceptionPoints('Skills', 'CreateSkill', 0)

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="manageSkillLink_SkillSkill"]')

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'CreateSkill', 10)
    });

    it('change skill display order - move up', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ChangeSkillDisplayOrder').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        const tableSelector = '[data-cy="skillsTable"]'
        cy.get(`${tableSelector} th`).contains('Display').click();

        // enable reorder should add buttons and sort by display order
        cy.get('[data-cy="enableDisplayOrderSort"]').click()

        cy.assertInceptionPoints('Skills', 'ChangeSkillDisplayOrder', 0)

        cy.get('[data-cy="orderMoveUp_skill2"]').click();

        cy.get('[data-cy="orderMoveUp_skill1"]').should('be.enabled');
        cy.get('[data-cy="orderMoveDown_skill1"]').should('be.disabled');
        cy.get('[data-cy="orderMoveUp_skill2"]').should('be.disabled');
        cy.get('[data-cy="orderMoveDown_skill2"]').should('be.enabled');

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'ChangeSkillDisplayOrder', 5)
    })

    it('change skill display order - move down', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ChangeSkillDisplayOrder').as('reportSkill');
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

        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'ChangeSkillDisplayOrder', 5)
    })

    it('use skill table additional columns', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/SkillsTableAdditionalColumns').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.assertInceptionPoints('Skills', 'SkillsTableAdditionalColumns', 0, false)
        // cy.get('[data-cy="skillsTable-additionalColumns"]').contains('Time Window').click();
        cy.get('[data-cy="skillsTable-additionalColumns"] [data-pc-section="dropdownicon"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Time Window"]').click()
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'SkillsTableAdditionalColumns', 5)
    })


    it('reuse skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ReuseSkill').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
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
        cy.assertInceptionPoints('Skills', 'ReuseSkill', 0)
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully reused 1 skill');
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'ReuseSkill', 25)
    })

    it('move skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/MoveSkill').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.createSubject(1, 2)
        cy.createSkill(1, 1, 1)
        cy.createSkill(1, 1, 2)

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Move Skills"]').click()

        // step 1
        cy.get('[ data-cy="reuseSkillsModalStep1"] [data-cy="selectDest_subjsubj2"]')
            .click();

        // step 2
        cy.get('[ data-cy="reuseSkillsModalStep2"]')
            .contains('1 skill will be moved to the [Subject 2] subject.');

        cy.assertInceptionPoints('Skills', 'MoveSkill', 0)
        cy.get('[data-cy="reuseSkillsModalStep2"] [data-cy="reuseButton"]')
          .click();

        // step 3
        cy.get('[data-cy="reuseSkillsModalStep3"]')
            .contains('Successfully moved 1 skill');
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'MoveSkill', 10)
    })

    it('import skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ImportSkillfromCatalog').as('reportSkill');
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
        cy.get('[data-p-index="0"] [data-pc-name="pcrowcheckbox"] input').click()
        cy.assertInceptionPoints('Skills', 'ImportSkillfromCatalog', 0)
        cy.get('[data-cy="importBtn"]')
            .click();
        cy.get('[data-cy="importedBadge-skill1"]');
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'ImportSkillfromCatalog', 25)
    });

    it('export to catalog', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ExporttoCatalog').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"]  [data-pc-name="pcheadercheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Export To Catalog"]').click()
        cy.assertInceptionPoints('Dashboard', 'ExporttoCatalog', 0)
        cy.get('[data-cy="exportToCatalogButton"]')
            .click();
        cy.wait('@reportSkill')
        cy.contains('Skill [Very Great Skill 1] was successfully exported to the catalog!');
        cy.assertInceptionPoints('Dashboard', 'ExporttoCatalog', 50)
    });

    it('configure self report skill - fallback user', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ConfigureSelfApprovalWorkload').as('reportSkill');
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

        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 0)
        cy.get('[data-cy="workloadCell_user1"] [data-cy="fallbackSwitch"] input').click({force: true})
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 25)
    });

    it('configure self report skill - add skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ConfigureSelfApprovalWorkload').as('reportSkill');
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

        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 0)
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 25)
    });

    it('configure self report skill - add user', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ConfigureSelfApprovalWorkload').as('reportSkill');
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
        // cy.selectItem(`[data-cy="expandedChild_${user1}"] [data-cy="userIdInput"] #existingUserInput`, 'usera', true, true);
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="existingUserInputDropdown"] [data-pc-section="dropdown"]`).click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('usera').click();
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).should('be.enabled')

        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 0)
        cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addUserConfBtn"]`).click()
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 25)
    });

    it('configure self report skill - add user tag conf', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ConfigureSelfApprovalWorkload').as('reportSkill');
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
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'ConfigureSelfApprovalWorkload', 25)
    });

    it('tag skills', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/AddOrModifyTags').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]')
            .click();
        cy.openDialog('[data-cy="skillsActionsMenu"] [aria-label="Add Tag"]', true)

        cy.get('[data-cy="newTag"]').type('New Tag 1')

        cy.assertInceptionPoints('Skills', 'AddOrModifyTags', 0)
        cy.clickSaveDialogBtn()
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'AddOrModifyTags', 10)
    });

    it('untag skills', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/AddOrModifyTags').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.addTagToSkills();

        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.get('[data-cy="skillsTable"] [data-p-index="0"] [data-pc-name="pcrowcheckbox"]').click()
        cy.get('[data-cy="skillActionsBtn"]').click();
        cy.get('[data-cy="skillsActionsMenu"] [aria-label="Remove Tag"]').click()
        cy.get('[data-cy="existingTag"]').click();
        cy.get('[data-pc-section="list"]').contains('TAG 1').click()
        cy.assertInceptionPoints('Skills', 'AddOrModifyTags', 0)
        cy.clickSaveDialogBtn()
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'AddOrModifyTags', 10)
        cy.get('[data-cy="skillTag-skill1-tag1"]').should('not.exist')
    });

    it('add project administrator', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/AddAdmin').as('reportSkill');
        cy.createProject(1);
        cy.visit('/administrator/projects/proj1/access')

        cy.get('[data-cy="existingUserInput"]').type('root');
        cy.get('#existingUserInput_0').contains('root').click();
        cy.get('[data-cy="userRoleSelector"]').click()
        cy.get('[data-pc-section="overlay"] [aria-label="Administrator"]').click();
        cy.get('[data-cy="addUserBtn"]').click();

        const tableSelector = '[data-cy=roleManagerTable]';
        cy.get(`${tableSelector} [data-cy="userCell_root@skills.org"]`);
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Projects', 'AddAdmin', 50)
    });

    it('Expand Skills Details on Skills Page', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ExpandSkillDetailsSkillsPage').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get(`[data-cy="skillsTable"] [data-p-index="0"] [data-pc-section="rowtogglebutton"]`).click()
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'ExpandSkillDetailsSkillsPage', 5)
    })

    it('create global badge', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/CreateGlobalBadge').as('reportSkill');
        const expectedId = 'TestGlobalBadgeBadge';
        const providedName = 'Test Global Badge';
        cy.intercept('GET', `/app/badges`).as('getGlobalBadges');
        cy.intercept('POST', '/app/badges/name/exists').as('nameExists');
        cy.intercept(`/app/badges/${expectedId}`) .as('postGlobalBadge');
        cy.intercept('GET', `/app/badges/id/${expectedId}/exists`) .as('idExists');
        cy.visit('/administrator/globalBadges');
        cy.get('[data-cy="inception-button"]').contains('Level');
        cy.wait('@getGlobalBadges');

        cy.get('[data-cy="btn_Global Badges"]').click();
        cy.get('[data-cy="name"]') .type(providedName);
        cy.wait('@nameExists');

        cy.assertInceptionPoints('Dashboard', 'CreateGlobalBadge', 0, false)

        cy.clickSave();
        cy.wait('@idExists');
        cy.wait('@postGlobalBadge');
        cy.wait('@reportSkill');
        cy.assertInceptionPoints('Dashboard', 'CreateGlobalBadge', 20)
    });

    it('create initially hidden skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/CreateSkillInitiallyHidden').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1');

        cy.openNewSkillDialog()

        cy.get('[data-cy="skillName"]')
          .type('Skill');
        cy.get('[data-cy="visibilitySwitch"]').click()

        cy.assertInceptionPoints('Skills', 'CreateSkillInitiallyHidden', 0)

        cy.clickSaveDialogBtn()
        cy.get('[data-cy="manageSkillLink_SkillSkill"]')
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Skills', 'CreateSkillInitiallyHidden', 5)
    });

    it('create initially hidden subject', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/CreateSubjectInitiallyHidden').as('reportSkill');
        const expectedId = 'InitiallyDisabledSkillSubject';
        const providedName = 'Initially Disabled Skill';
        cy.intercept('POST', `/admin/projects/proj1/subjects/${expectedId}`).as('postNewSubject');
        cy.intercept('GET', '/admin/projects/proj1/subjects').as('loadSubjects');
        cy.intercept('POST', '/admin/projects/proj1/subjectNameExists').as('nameExists');
        cy.createProject(1);

        cy.visit('/administrator/projects/proj1');
        cy.wait('@loadSubjects');
        cy.get('[data-cy="btn_Subjects"]').click();

        cy.get('[data-cy="subjectName"]').type(providedName);
        cy.wait('@nameExists');
        cy.getIdField().should('have.value', expectedId);

        cy.get('[data-cy="visibilitySwitch"]').click()
        cy.assertInceptionPoints('Projects', 'CreateSubjectInitiallyHidden', 0)

        cy.clickSaveDialogBtn()
        cy.wait('@postNewSubject');
        cy.wait('@reportSkill')
        cy.assertInceptionPoints('Projects', 'CreateSubjectInitiallyHidden', 50)
    });

    it('configure skill icon - edit skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ConfigureSkillIcon').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.get('[data-cy="editSkillButton_skill1"]').click();
        cy.get('[data-cy="iconPicker"]').click();
        // ensure font awesome is selected by default
        cy.get('[data-pc-section="tablist"] [data-p-active="true"] [data-pc-section="itemlink"]').contains('Font Awesome Free')
        cy.wait(1500);
        cy.get('[data-cy=virtualIconList]').scrollTo(0,540);
        cy.get('.icon-item>button:visible', {timeout:10000}).should('be.visible').last().then(($el)=> {
            const clazz = $el.attr('data-cy');
            cy.get(`[data-cy="${clazz}"]`).should('have.length', '1').click({force:true});
            cy.get('[data-cy=saveDialogBtn]').scrollIntoView().should('be.visible').click();
            cy.get('.p-datatable-table-container').contains('Skill 1').should('be.visible');
            const classes = clazz.split(' ');
            let iconClass = classes[classes.length-1];
            iconClass = iconClass.replace(/-link$/, '')
            cy.get(`i.${iconClass}`).should('be.visible');
            cy.wait('@reportSkill')
            cy.assertInceptionPoints('Skills', 'ConfigureSkillIcon', 5, false)
        })
    });


    it('configure skill icon - new skill', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/ConfigureSkillIcon').as('reportSkill');
        cy.createProject(1);
        cy.createSubject(1, 1);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.openNewSkillDialog()
        cy.get('[data-cy="skillName"]').type("test")
        cy.get('[data-cy="iconPicker"]').click();
        // ensure font awesome is selected by default
        cy.get('[data-pc-section="tablist"] [data-p-active="true"] [data-pc-section="itemlink"]').contains('Font Awesome Free')
        cy.wait(1500);
        cy.get('[data-cy=virtualIconList]').scrollTo(0,540);
        cy.get('.icon-item>button:visible', {timeout:10000}).should('be.visible').last().then(($el)=> {
            const clazz = $el.attr('data-cy');
            cy.get(`[data-cy="${clazz}"]`).should('have.length', '1').click({force:true});
            cy.get('[data-cy=saveDialogBtn]').scrollIntoView().should('be.visible').click();
            cy.get('[data-cy="manageSkillLink_testSkill"]').should('be.visible');
            const classes = clazz.split(' ');
            let iconClass = classes[classes.length-1];
            iconClass = iconClass.replace(/-link$/, '')
            cy.get(`i.${iconClass}`).should('be.visible');
            cy.wait('@reportSkill')
            cy.assertInceptionPoints('Skills', 'ConfigureSkillIcon', 5)
        })
    });

    it('configure video', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/SkillAudioVideo').as('reportSkill');
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video');
        cy.get('[data-cy="showExternalUrlBtn"]').click()
        cy.get('[data-cy="showFileUploadBtn"]').should('be.visible')
        cy.get('[data-cy="showExternalUrlBtn"]').should('not.exist')
        cy.get('[data-cy="videoUrl"]').type('http://some.vid')

        cy.assertInceptionPoints('Skills', 'SkillAudioVideo', 0)

        cy.get('[data-cy="saveVideoSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.wait('@reportSkill')

        cy.assertInceptionPoints('Skills', 'SkillAudioVideo', 25)

    })

    it('assign quiz to skill', function () {
        cy.intercept('POST', '/api/projects/Inception/skills/SkillQuizOrSurvey').as('reportSkill');
        cy.createProject(1)
        cy.createSubject(1,1)

        cy.createQuizDef(1, {name: 'Test Your Trivia Knowledge'});
        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.openNewSkillDialog()
        cy.get('[data-cy="skillName"]').type('abc')
        cy.get('[data-cy="selfReportEnableCheckbox"]').click()
        cy.get('[data-cy="selfReportTypeSelector"] [value="Quiz"]').click({ force: true })
        cy.get('[data-cy="quizSelector"]').click()
        cy.get('[data-cy="availableQuizSelection-quiz1"]').click()
        cy.get('[data-cy="quizSelected-quiz1"]')

        cy.assertInceptionPoints('Skills', 'SkillQuizOrSurvey', 0)

        cy.clickSaveDialogBtn()
        cy.wait('@reportSkill')

        cy.assertInceptionPoints('Skills', 'SkillQuizOrSurvey', 25)
    });

    it('configure slides external url', () => {
        cy.intercept('POST', '/api/projects/Inception/skills/AddSkillSlides').as('reportSkill');
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1)
        cy.visit(`/administrator/projects/proj1/subjects/subj1/skills/skill1/config-slides`);
        cy.get('[data-cy="showFileUploadBtn"]').should('not.exist')
        cy.get('[data-cy="saveSlidesSettingsBtn"]').should('be.disabled')
        cy.get('#pdfCanvasId').should('not.exist')
        cy.get('[data-cy="videoFileUpload"] input[type=file]').selectFile(`cypress/fixtures/test-slides-1.pdf`,  { force: true })

        cy.assertInceptionPoints('Skills', 'AddSkillSlides', 0)

        cy.get('[data-cy="saveSlidesSettingsBtn"]').click()
        cy.get('[data-cy="savedMsg"]')
        cy.wait('@reportSkill')

        cy.assertInceptionPoints('Skills', 'AddSkillSlides', 25)
    });
});
