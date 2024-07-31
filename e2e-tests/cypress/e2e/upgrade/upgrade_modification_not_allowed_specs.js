/*
Copyright 2024 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
describe('Modifications not permitted when upgrade in progress is configured', () => {

  beforeEach(() => {
    cy.createProject(1)
    cy.createSubject(1, 1)
    cy.createSkill(1, 1, 1)
    cy.createSkill(1, 1, 2)
    cy.createProject(2)
    cy.createSubject(2, 1)

    cy.intercept('GET', '/public/config', (req) => {
      req.reply((res) => {
        const conf = res.body;
        conf.dbUpgradeInProgress = true;
        res.send(conf);
      });
    }).as('loadConfigWithDbInProgressUpgrade')

    Cypress.Commands.add("ignoreUpgradeError", () => {
      cy.on('uncaught:exception', (err, runnable) => {
        if (err.message.includes('Request failed with status code 503')) {
          return false
        }
        return true
      })
    });

  })

  const upgradeInProgressResponse = {
    statusCode: 503,
    body: {
      errorCode: 'DbUpgradeInProgress',
    },
  }

  it('redirect to upgrade in progress page - create project', () => {
    cy.ignoreUpgradeError()
    cy.intercept('POST', '/app/projects/**', upgradeInProgressResponse).as('saveProject')

    cy.visit('/administrator/')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="newProjectButton"]').click()
    cy.get('[data-cy="projectName"]')
      .type('My New test Project');
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
    cy.wait('@saveProject')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - create subject', () => {
    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/subjects/newSubject', upgradeInProgressResponse).as('saveSubject')

    cy.visit('/administrator/projects/proj1')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="btn_Subjects"]').click();
    cy.get('[data-cy="subjectName"]').type('new');
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
    cy.wait('@saveSubject')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - create skill', () => {
    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/newSkill', upgradeInProgressResponse).as('saveSkill')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="newSkillButton"]').click();
    cy.get('[data-cy="skillName"]').type('new');
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
    cy.wait('@saveSkill')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - edit skill', () => {
    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/skill2', upgradeInProgressResponse).as('saveSkill')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="editSkillButton_skill2"]').click();
    cy.get('[data-cy="skillName"]').type('1');
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
    cy.wait('@saveSkill')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - create skill group', () => {
    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/newGroup', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="newGroupButton"]').click();
    cy.get('[data-cy="name"]').type('new');
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - edit skill group', () => {
    cy.ignoreUpgradeError()
    cy.createSkillsGroup(1, 1, 5);
    cy.intercept('POST', '/admin/projects/proj1/subjects/subj1/skills/group5', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/subjects/subj1')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="editSkillButton_group5"]').click();
    cy.get('[data-cy="name"]').type('new');
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - import skill', () => {
    cy.exportSkillToCatalog(1, 1, 1);

    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj2/subjects/subj1/import', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj2/subjects/subj1')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="importFromCatalogBtn"]').click();
    cy.get('[data-cy="importSkillsFromCatalogTable"] [data-p-index="0"] [data-pc-name="rowcheckbox"]').click()
    cy.get('[data-cy="numSelectedSkills"]').should('have.text', '1');
    cy.get('[data-cy="importBtn"]').should('be.enabled').click();
    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - configure video', () => {
    cy.exportSkillToCatalog(1, 1, 1);

    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/skills/skill1/video', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-video')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="showExternalUrlBtn"]').click()
    cy.get('[data-cy="showFileUploadBtn"]').should('be.visible')
    cy.get('[data-cy="showExternalUrlBtn"]').should('not.exist')
    cy.get('[data-cy="videoUrl"]').type('http://some.vid')
    cy.get('[data-cy="saveVideoSettingsBtn"]').click()
    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - configure skill expiration', () => {
    cy.exportSkillToCatalog(1, 1, 1);

    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/skills/skill1/expiration', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1/config-expiration')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="yearlyRadio"]').click()
    cy.get('[data-cy="saveSettingsBtn"]').click()
    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - create badge', () => {
    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/badges/newBadge', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/badges')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="btn_Badges"]').click();
    cy.get('[data-cy="name"]').type('new');
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - edit badge', () => {
    cy.ignoreUpgradeError()
    cy.createBadge(1, 1)
    cy.intercept('POST', '/admin/projects/proj1/badges/badge1', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/badges')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="badgeCard-badge1"] [data-cy="editBtn"]').click();
    cy.get('[data-cy="name"]').type('1');
    cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - add skill to badge', () => {
    cy.ignoreUpgradeError()
    cy.createBadge(1, 1)
    cy.intercept('POST', '/admin/projects/proj1/badge/badge1/skills/skill*', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/badges/badge1')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="skillsSelector"] [data-pc-section="trigger"]').click();
    cy.get('[data-pc-section="item"]').first().click();

    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - go live badge', () => {
    cy.ignoreUpgradeError()
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 1);
    cy.intercept('POST', '/admin/projects/proj1/badges/badge1', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/badges/badge1')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="goLive"]').click();
    cy.get('[data-pc-name="acceptbutton"]').click();

    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - save project settings', () => {
    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/settings', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/settings')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="groupDescriptionsSwitch"]').click();
    cy.get('[data-cy="saveSettingsBtn"]').click();

    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - self report approve', () => {
    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/approvals/approve', upgradeInProgressResponse).as('saveEndpoint')

    cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
    cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
    cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
    cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

    cy.visit('/administrator/projects/proj1/self-report');
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-pc-name="rowcheckbox"]').click()
    cy.get('[data-cy="approveBtn"]').click();

    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - self report reject', () => {
    cy.ignoreUpgradeError()
    cy.intercept('POST', '/admin/projects/proj1/approvals/reject', upgradeInProgressResponse).as('saveEndpoint')

    cy.createSkill(1, 1, 1, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 2, { selfReportingType: 'Approval' });
    cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
    cy.reportSkill(1, 2, 'user2', '2020-09-16 11:00');
    cy.reportSkill(1, 3, 'user1', '2020-09-17 11:00');
    cy.reportSkill(1, 1, 'user0', '2020-09-18 11:00');

    cy.visit('/administrator/projects/proj1/self-report');
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="skillsReportApprovalTable"] [data-p-index="1"] [data-pc-name="rowcheckbox"]').click()
    cy.get('[data-cy="rejectBtn"]').click();
    cy.get('[data-cy="saveDialogBtn"]').click();

    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - configure approver workload - single skill', () => {
    cy.ignoreUpgradeError()

    cy.createSkill(1, 1, 6, { selfReportingType: 'Approval' })
    const pass = 'password';
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

    cy.intercept('POST', '/admin/projects/proj1/approverConf/user1', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/administrator/projects/proj1/self-report/configure');
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    const user1 = 'user1'
    cy.get(`[data-cy="workloadCell_${user1}"] [data-cy="editApprovalBtn"]`).click()
    cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="skillsSelector"]`).click();
    cy.get(`[data-cy="skillsSelectionItem-proj1-skill1"]`).click()
    cy.get(`[data-cy="expandedChild_${user1}"] [data-cy="addSkillConfBtn"]`).click()

    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  it('redirect to upgrade in progress page - user preference', () => {
    cy.ignoreUpgradeError()
    cy.createBadge(1, 1)
    cy.assignSkillToBadge(1, 1, 1);
    cy.intercept('POST', '/app/userInfo/settings', upgradeInProgressResponse).as('saveEndpoint')

    cy.visit('/settings/preferences')
    cy.wait('@loadConfigWithDbInProgressUpgrade')

    cy.get('[data-cy="enableDarkModeSwitch"]').click();
    cy.get('[data-cy="userPrefsSettingsSave"]').click();

    cy.wait('@saveEndpoint')

    cy.get('[data-cy="upgradeInProgressError"]')
    cy.url().should('include', '/upgrade-in-progress')
  })

  if (!Cypress.env('oauthMode')) {
    it('redirect to upgrade in progress page - add admin', () => {
      cy.ignoreUpgradeError()
      cy.createBadge(1, 1)
      cy.assignSkillToBadge(1, 1, 1);
      cy.intercept('PUT', '/admin/projects/proj1/users/root@skills.org/roles/ROLE_PROJECT_ADMIN', upgradeInProgressResponse).as('saveEndpoint')

      cy.visit('/administrator/projects/proj1/access')
      cy.wait('@loadConfigWithDbInProgressUpgrade')

      cy.get('[data-cy="existingUserInput"]').type('root');
      cy.wait(500);
      cy.get('#existingUserInput_0').click();
      cy.get('[data-cy="userRoleSelector"]').click()
      cy.get('[data-pc-section="panel"] [aria-label="Administrator"]').click();
      cy.get('[data-cy="addUserBtn"]').click();

      cy.wait('@saveEndpoint')

      cy.get('[data-cy="upgradeInProgressError"]')
      cy.url().should('include', '/upgrade-in-progress')
    })
  }
})
