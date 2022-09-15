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
        cy.createProject(1);
        cy.visit('/administrator/projects/proj1/settings');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .check({ force: true });

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .uncheck({ force: true });

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .check({ force: true });

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
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
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .uncheck({ force: true });

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .contains('Unsaved Changes');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');

        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .check({ force: true });
        cy.get('[data-cy="rankAndLeaderboardOptOutSwitch"]')
            .should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
    });

    it('project-level settings: set custom labels', () => {
        cy.createProject(1);
        cy.visit('/administrator/projects/proj1/settings');

        cy.get('[data-cy="customLabelsSwitch"')
            .should('not.be.checked');
        cy.get('[data-cy="customLabelsSwitch"')
            .click({ force: true });
        cy.get('[data-cy="customLabelsSwitch"')
            .should('be.checked');
        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', 'Project');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', 'Subject');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', 'Group');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', 'Skill');
        cy.get('[data-cy="levelDisplayTextInput"]')
            .should('have.value', 'Level');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy=projectDisplayTextInput]')
            .clear()
            .type('Work Role');
        cy.get('[data-cy=subjectDisplayTextInput]')
            .clear()
            .type('Competency');
        cy.get('[data-cy=groupDisplayTextInput]')
            .clear()
            .type('KSA');
        cy.get('[data-cy=skillDisplayTextInput]')
            .clear()
            .type('Course');
        cy.get('[data-cy=levelDisplayTextInput]')
            .clear()
            .type('Stage');

        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', 'Work Role');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', 'Competency');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', 'KSA');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', 'Course');
        cy.get('[data-cy="levelDisplayTextInput"]')
            .should('have.value', 'Stage');
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

        cy.get('[data-cy="customLabelsSwitch"')
            .should('be.checked');
        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', 'Work Role');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', 'Competency');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', 'KSA');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', 'Course');
        cy.get('[data-cy="levelDisplayTextInput"]')
            .should('have.value', 'Stage');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        // set back to default
        cy.get('[data-cy=projectDisplayTextInput]')
            .clear();
        cy.get('[data-cy=subjectDisplayTextInput]')
            .clear();
        cy.get('[data-cy=groupDisplayTextInput]')
            .clear();
        cy.get('[data-cy=skillDisplayTextInput]')
            .clear();
        cy.get('[data-cy=levelDisplayTextInput]')
            .clear();

        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', '');
        cy.get('[data-cy="levelDisplayTextInput"]')
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

        cy.get('[data-cy="customLabelsSwitch"')
            .should('not.be.checked');
        cy.get('[data-cy="customLabelsSwitch"')
            .click({ force: true });
        cy.get('[data-cy="customLabelsSwitch"')
            .should('be.checked');
        cy.get('[data-cy="projectDisplayTextInput"]')
            .should('have.value', 'Project');
        cy.get('[data-cy="subjectDisplayTextInput"]')
            .should('have.value', 'Subject');
        cy.get('[data-cy="groupDisplayTextInput"]')
            .should('have.value', 'Group');
        cy.get('[data-cy="skillDisplayTextInput"]')
            .should('have.value', 'Skill');
        cy.get('[data-cy="levelDisplayTextInput"]')
            .should('have.value', 'Level');
        cy.get('[data-cy="unsavedChangesAlert"]')
            .should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');

        cy.get('[data-cy=projectDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=projectDisplayTextError]')
            .contains('Project Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=projectDisplayTextInput]')
            .clear();
        cy.get('[data-cy=projectDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=subjectDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=subjectDisplayTextError]')
            .contains('Subject Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=subjectDisplayTextInput]')
            .clear();
        cy.get('[data-cy=subjectDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=groupDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=groupDisplayTextError]')
            .contains('Group Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=groupDisplayTextInput]')
            .clear();
        cy.get('[data-cy=groupDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=skillDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=skillDisplayTextError]')
            .contains('Skill Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=skillDisplayTextInput]')
            .clear();
        cy.get('[data-cy=skillDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
        cy.get('[data-cy=levelDisplayTextInput]')
            .clear()
            .type('123456789012345678901234567890123456789012345678901');
        cy.get('[data-cy=levelDisplayTextError]')
            .contains('Level Display Text cannot exceed 50 characters.')
            .should('be.visible');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.disabled');
        cy.get('[data-cy=levelDisplayTextInput]')
            .clear();
        cy.get('[data-cy=levelDisplayTextError]')
            .should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]')
            .should('be.enabled');
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
            .should('include.text', 'PUBLIC');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Not Discoverable');

        cy.get('[data-cy="projectVisibilitySelector"]')
            .select('dpr');
        cy.get('[data-cy="saveSettingsBtn"')
            .click();
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'PUBLIC');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Discoverable');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('not.include.text', 'Not Discoverable');

        cy.get('[data-cy="projectVisibilitySelector"]')
            .select('pio');
        cy.get('.modal-content')
            .should('be.visible')
            .should('include.text', 'Changing to Invite Only')
            .should('include.text', 'Changing this Project to Invite Only will restrict access to the training profile and skill reporting to only invited users.');
        cy.clickButton('Ok');
        cy.get('[data-cy="saveSettingsBtn"')
            .click({ force: true });
        cy.wait('@saveSettings');
        cy.wait('@getSettings');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'PRIVATE');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Invite Only');

        cy.get('[data-cy="nav-Access"')
            .click();
        cy.wait(200);
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'PRIVATE');
        cy.get('[data-cy="pageHeaderStat"]')
            .eq(0)
            .should('include.text', 'Invite Only');
    });
});