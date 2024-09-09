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
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Project Settings Tests', () => {
    beforeEach(() => {
        cy.intercept('GET', '/app/projects')
            .as('getProjects');
        cy.intercept('GET', '/api/icons/customIconCss')
            .as('getProjectsCustomIcons');
        cy.intercept('GET', '/app/userInfo')
            .as('getUserInfo');
        cy.intercept('/admin/projects/proj1/users/root@skills.org/roles*')
            .as('getRolesForRoot');
    });

    it('project-level settings: rank opt-out for all admins', () => {
        let leaderboardSwitch = '[data-cy="rankAndLeaderboardOptOutSwitch"] [data-pc-section="input"]'
        cy.createProject(1);
        cy.visit('/administrator/projects/proj1/settings');

        cy.get(leaderboardSwitch)
            .should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get(leaderboardSwitch)
            .click();

        cy.get(leaderboardSwitch)
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get(leaderboardSwitch)
            .click();

        cy.get(leaderboardSwitch)
            .should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get(leaderboardSwitch)
            .click();

        cy.get(leaderboardSwitch)
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // refresh
        cy.visit('/administrator/projects/proj1/settings');
        cy.get(leaderboardSwitch)
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get(leaderboardSwitch)
            .click();

        cy.get(leaderboardSwitch)
            .should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get(leaderboardSwitch)
            .click();
        cy.get(leaderboardSwitch)
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
    });

    it('project-level settings: set custom labels', () => {
        let labelsSwitch = '[data-cy="customLabelsSwitch"] [data-pc-section="input"]';
        cy.createProject(1);
        cy.visit('/administrator/projects/proj1/settings');

        cy.get(labelsSwitch)
            .should('not.be.checked');
        cy.get(labelsSwitch)
            .click({ force: true });
        cy.get(labelsSwitch)
            .should('be.checked');
        cy.get('[data-cy="projectDisplayNameTextInput"]')
            .should('have.value', 'Project');
        cy.get('[data-cy="subjectDisplayNameTextInput"]')
            .should('have.value', 'Subject');
        cy.get('[data-cy="groupDisplayNameTextInput"]')
            .should('have.value', 'Group');
        cy.get('[data-cy="skillDisplayNameTextInput"]')
            .should('have.value', 'Skill');
        cy.get('[data-cy="levelDisplayNameTextInput"]')
            .should('have.value', 'Level');
        cy.get('[data-cy="pointDisplayNameTextInput"]')
            .should('have.value', 'Point');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy=projectDisplayNameTextInput]')
            .clear()
            .type('Work Role');
        cy.get('[data-cy=subjectDisplayNameTextInput]')
            .clear()
            .type('Competency');
        cy.get('[data-cy=groupDisplayNameTextInput]')
            .clear()
            .type('KSA');
        cy.get('[data-cy=skillDisplayNameTextInput]')
            .clear()
            .type('Course');
        cy.get('[data-cy=levelDisplayNameTextInput]')
            .clear()
            .type('Stage');
        cy.get('[data-cy=pointDisplayNameTextInput]')
            .clear()
            .type('Hour');

        cy.get('[data-cy="projectDisplayNameTextInput"]')
            .should('have.value', 'Work Role');
        cy.get('[data-cy="subjectDisplayNameTextInput"]')
            .should('have.value', 'Competency');
        cy.get('[data-cy="groupDisplayNameTextInput"]')
            .should('have.value', 'KSA');
        cy.get('[data-cy="skillDisplayNameTextInput"]')
            .should('have.value', 'Course');
        cy.get('[data-cy="levelDisplayNameTextInput"]')
            .should('have.value', 'Stage');
        cy.get('[data-cy="pointDisplayNameTextInput"]')
            .should('have.value', 'Hour');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // refresh
        cy.visit('/administrator/projects/proj1/settings');

        cy.get(labelsSwitch)
            .should('be.checked');
        cy.get('[data-cy="projectDisplayNameTextInput"]')
            .should('have.value', 'Work Role');
        cy.get('[data-cy="subjectDisplayNameTextInput"]')
            .should('have.value', 'Competency');
        cy.get('[data-cy="groupDisplayNameTextInput"]')
            .should('have.value', 'KSA');
        cy.get('[data-cy="skillDisplayNameTextInput"]')
            .should('have.value', 'Course');
        cy.get('[data-cy="levelDisplayNameTextInput"]')
            .should('have.value', 'Stage');
        cy.get('[data-cy="pointDisplayNameTextInput"]')
            .should('have.value', 'Hour');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // set back to default
        cy.get('[data-cy=projectDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=subjectDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=groupDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=skillDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=levelDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy="pointDisplayNameTextInput"]')
            .clear();

        cy.get('[data-cy="projectDisplayNameTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="subjectDisplayNameTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="groupDisplayNameTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="skillDisplayNameTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="levelDisplayNameTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="pointDisplayNameTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="saveSettingsBtn"]')
            .click();
        cy.get('[data-cy="settingsSavedAlert"]')
            .contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // refresh, validate default is back
        cy.visit('/administrator/projects/proj1/settings');

        cy.get(labelsSwitch)
            .should('not.be.checked');
        cy.get(labelsSwitch)
            .click({ force: true });
        cy.get(labelsSwitch)
            .should('be.checked');
        cy.get('[data-cy="projectDisplayNameTextInput"]')
            .should('have.value', 'Project');
        cy.get('[data-cy="subjectDisplayNameTextInput"]')
            .should('have.value', 'Subject');
        cy.get('[data-cy="groupDisplayNameTextInput"]')
            .should('have.value', 'Group');
        cy.get('[data-cy="skillDisplayNameTextInput"]')
            .should('have.value', 'Skill');
        cy.get('[data-cy="levelDisplayNameTextInput"]')
            .should('have.value', 'Level');
        cy.get('[data-cy="pointDisplayNameTextInput"]')
            .should('have.value', 'Point');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy=projectDisplayNameTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=projectDisplayNameError]')
            .contains('Project Display Text must be at most 20 characters')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=projectDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=projectDisplayNameError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=subjectDisplayNameTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=subjectDisplayNameError]')
            .contains('Subject Display Text must be at most 20 characters')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=subjectDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=subjectDisplayNameError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=groupDisplayNameTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=groupDisplayNameError]')
            .contains('Group Display Text must be at most 20 characters')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=groupDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=groupDisplayNameError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=skillDisplayNameTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=skillDisplayNameError]')
            .contains('Skill Display Text must be at most 20 characters')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=skillDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=skillDisplayNameError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=levelDisplayNameTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=levelDisplayNameError]')
            .contains('Level Display Text must be at most 20 characters')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=levelDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=levelDisplayNameError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=pointDisplayNameTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=pointDisplayNameError]')
            .contains('Point Display Text must be at most 20 characters')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=pointDisplayNameTextInput]')
            .clear();
        cy.get('[data-cy=pointDisplayNameError]')
            .should('not.exist');
    });

    it('project-level settings: project visibility', () => {

        cy.createProject(1);
        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('getSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('saveSettings');
        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@getSettings');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Project Catalog');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Hidden');

        cy.selectItem('[data-cy="projectVisibilitySelector"]', 'Add to the Project Catalog');
        // cy.get('[data-cy="projectVisibilitySelector"]')
        //     .select('dpr');
        cy.get('[data-cy="saveSettingsBtn"')
            .click();
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Project Catalog');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Discoverable');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('not.include.text', 'Hidden');

        cy.selectItem('[data-cy="projectVisibilitySelector"]', 'Private Invite Only');
        // cy.get('[data-cy="projectVisibilitySelector"]')
        //     .select('pio');
        cy.get('.p-confirm-dialog')
            .should('be.visible')
            .should('include.text', 'Changing to Invite Only')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users.');
        cy.clickButton('Ok');
        cy.get('[data-cy="saveSettingsBtn"')
            .click({ force: true });
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="pageHeaderStat_Protection"]')
            .contains('PRIVATE');
        cy.get('[data-cy="pageHeaderStatSecondaryLabel_Protection"]')
            .contains('Invite Only');
    });

    it('project-level settings: project description', () => {
        cy.createProject(1, { description: 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque vitae tellus.'});

        cy.createProject(2, { description: '' });

        cy.createProject(3, { description: 'Lorem ipsum dolor sit amet' });

        cy.request('POST', '/admin/projects/proj2/settings', [{
            setting: 'production.mode.enabled',
            value: 'true',
            projectId: 'proj2'
        }]);
        cy.request('POST', '/admin/projects/proj2/settings', [{
            setting: 'show_project_description_everywhere',
            value: 'true',
            projectId: 'proj2'
        }]);

        cy.request('POST', '/admin/projects/proj3/settings', [{
            setting: 'production.mode.enabled',
            value: 'true',
            projectId: 'proj3'
        }]);

        cy.intercept('GET', '/admin/projects/proj1/settings')
            .as('p1GetSettings');
        cy.intercept('POST', '/admin/projects/proj1/settings')
            .as('p1SaveSettings');
        cy.intercept('GET', '/admin/projects/proj2/settings')
            .as('p2GetSettings');
        cy.intercept('POST', '/admin/projects/proj2/settings')
            .as('p2SaveSettings');

        cy.intercept('GET', '/api/availableForMyProjects').as('loadMyProjects');
        cy.intercept('GET', '/app/projects/proj1/description').as('loadProj1Description');

        cy.visit('/administrator/projects/proj1/settings');
        cy.wait('@p1GetSettings');
        cy.selectItem('[data-cy="projectVisibilitySelector"]', 'Add to the Project Catalog');

        cy.get('[data-cy="showProjectDescriptionSelector"]').should('have.text', 'Only show Description in Project Catalog');
        cy.selectItem('[data-cy="showProjectDescriptionSelector"]', 'Show Project Description everywhere');
        cy.get('[data-cy="saveSettingsBtn"')
            .click();
        cy.wait('@p1SaveSettings');

        //make sure that the setting persisted
        cy.get('[data-cy="nav-Access"]').click();
        cy.contains('Access Management').should('be.visible');
        cy.get('[data-cy="nav-Settings"]').click();
        cy.wait('@p1GetSettings');
        cy.get('[data-cy="showProjectDescriptionSelector"]').should('have.text', 'Show Project Description everywhere');
        //validate that changed value for description display persisted


        //validate that the project description expander is only present for proj1 as proj2 has no description defined, proj3 should not have an expander as while it does
        cy.fixture('vars.json')
          .then((vars) => {
              cy.request('POST', '/logout');
              cy.register(Cypress.env('proxyUser'), vars.defaultPass, false);
              cy.loginAsProxyUser();
          });
        cy.loginAsProxyUser();
        cy.visit('/progress-and-rankings/manage-my-projects');
        cy.wait('@loadMyProjects');
        cy.get('[data-pc-section="rowtoggler"]').should('have.length', 3);
        cy.get('[data-pc-section="rowtoggler"]').first().click();
        cy.wait('@loadProj1Description');
        cy.get('[data-cy="projectDescriptionRow_proj1"]').should('exist');
        cy.get('[data-cy="projectDescriptionRow_proj1"]').should('contain.text', 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque vitae tellus.');


        cy.intercept('/progress-and-rankings/projects/proj1** ')
            .as('loadP1Cd');
        cy.visit('/progress-and-rankings/projects/proj1');
        cy.wait('@loadP1Cd');
        cy.get('[data-cy="projectDescription"]').should('be.visible');
        cy.get('[data-cy="projectDescription"]').should('contain.text', 'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Quisque vitae tellus.')

        // proj2 has no description defined so it shouldn't be displayed
        cy.intercept('/progress-and-rankings/projects/proj2** ')
            .as('loadP2Cd');
        cy.visit('/progress-and-rankings/projects/proj2');
        cy.wait('@loadP2Cd');
        cy.get('[data-cy="projectDescription"]').should('not.exist');

        // proj3 has a description but is using the default configuration which hides it from display in the client-display
        cy.intercept('/progress-and-rankings/projects/proj3** ')
            .as('loadP3Cd');
        cy.visit('/progress-and-rankings/projects/proj3');
        cy.wait('@loadP3Cd');
        cy.get('[data-cy="projectDescription"]').should('not.exist');
    });
});