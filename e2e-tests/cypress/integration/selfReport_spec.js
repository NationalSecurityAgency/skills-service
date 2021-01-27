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
describe('Self Report Skills Management Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1"
        })
    });

    it('manage self reporting settings at project level', () => {
        cy.visit('/projects/proj1/settings');

        cy.get('[data-cy="selfReportSwitch"]').should('not.be.checked');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled');
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="selfReportSwitch"]').should('be.checked');

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes');

        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').click({force:true})
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');

        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes');

        cy.get('[data-cy="saveSettingsBtn"]').click();
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated');
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled');

        // refresh and check that the values persisted
        cy.visit('/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="settingsSavedAlert"]').should('not.exist')

        // disable skill, refresh and validate
        cy.get('[data-cy="selfReportSwitch"]').uncheck({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes')
        cy.get('[data-cy="settingsSavedAlert"]').should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="saveSettingsBtn"]').click();

        cy.visit('/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');
        cy.get('[data-cy="settingsSavedAlert"]').should('not.exist');
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled');

        // enable then disable should disable save button
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="selfReportSwitch"]').uncheck({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist')
        cy.get('[data-cy="saveSettingsBtn"]').should('be.disabled');

        // enabled and save
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="saveSettingsBtn"]').click()
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated');

        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').click({force:true});
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').click({force:true});
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');

        cy.get('[data-cy="selfReportSwitch"]').uncheck({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').contains('Unsaved Changes');
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="unsavedChangesAlert"]').should('not.exist');
    });

    it('create skills - self reporting disabled - no project level default', () => {
        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy=skillName]').type('skill1');
        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1Skill"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');
    });

    it('create skills - self reporting with approval - no project level default', () => {
        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy="selfReportEnableCheckbox"]').check({force: true})
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy=skillName]').type('skill1');
        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1Skill"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');
    });

    it('create skills - self reporting with Honor System - no project level default', () => {
        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.disabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy="selfReportEnableCheckbox"]').check({force: true})
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');

        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').click({force: true});
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');

        cy.get('[data-cy=skillName]').type('skill1');
        cy.clickSave();
        cy.get('[data-cy="editSkillButton_skill1Skill"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');
    });

    it('create skill - project level default of Honor System', () => {
        cy.visit('/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').click({force:true})
        cy.get('[data-cy="saveSettingsBtn"]').click();
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated');

        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('not.be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.checked');
    });

    it('create skill - project level default of Approval', () => {
        cy.visit('/projects/proj1/settings');
        cy.get('[data-cy="selfReportSwitch"]').check({force: true});
        cy.get('[data-cy="saveSettingsBtn"]').click();
        cy.get('[data-cy="settingsSavedAlert"]').contains('Settings Updated');

        cy.visit('/projects/proj1/subjects/subj1');
        cy.get('[data-cy="btn_Skills"]').click();
        cy.get('[data-cy="selfReportEnableCheckbox"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('be.enabled');
        cy.get('[data-cy="selfReportTypeSelector"] [value="Approval"]').should('be.checked');
        cy.get('[data-cy="selfReportTypeSelector"] [value="HonorSystem"]').should('not.be.checked');
    });



});

