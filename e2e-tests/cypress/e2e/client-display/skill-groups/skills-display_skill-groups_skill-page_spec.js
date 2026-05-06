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

describe('Client Display Skills Groups Tests', () => {

    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);

        Cypress.Commands.add('reportHonorSkill', (skillNum) => {
            cy.get(`[data-cy="group-group1_skillProgress-skill${skillNum}"] [data-cy="claimPointsBtn"]`)
                .click();
            cy.get(`[data-cy="group-group1_skillProgress-skill${skillNum}"] [data-cy="selfReportAlert"]`)
                .contains('Congrats! You just earned 100 points');
        });
    });

    it('group info is shown on skill page if not toggled off', () => {
        let groupDescription = "This is where cool description"
        cy.createSkillsGroup(1, 1, 1, {
            numSkillsRequired: 1,
            description: groupDescription
        });
        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 150,
            numPerformToCompletion: 2
        });

        cy.cdVisit('/subjects/subj1/skills/skill1', true);

        cy.get('[data-cy="groupInformationSection"]').should('exist');
        cy.get('[data-cy="toggleGroupDescription"]').click();
        cy.get('[data-cy="groupDescriptionSection"]').should('exist');
        cy.get('[data-cy="groupDescriptionSection"]').contains(groupDescription);
    });

    it('group info is not shown on skill page if setting is toggled', () => {
        let groupDescription = "This is where cool description"
        cy.createSkillsGroup(1, 1, 1, {
            numSkillsRequired: 1,
            description: groupDescription
        });

        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 150,
            numPerformToCompletion: 2
        });

        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'false',
                setting: 'group-info-on-skill-page',
                projectId: 'proj1',
            },
        ]);

        cy.cdVisit('/subjects/subj1/skills/skill1', true);

        cy.get('[data-cy="groupInformationSection"]').should('exist');
        cy.get('[data-cy="groupDescriptionSection"]').should('not.exist');

        cy.get('[data-cy="toggleGroupDescription"]').click();
        cy.get('[data-cy="groupDescriptionSection"]').should('exist');
        cy.get('[data-cy="groupDescriptionSection"]').contains(groupDescription);

        cy.get('[data-cy="toggleGroupDescription"]').click();
        cy.get('[data-cy="groupDescriptionSection"]').should('not.exist');
    });

    it('group info is shown on skill page if always show descriptions is toggled', () => {
        let groupDescription = "This is where cool description"
        cy.createSkillsGroup(1, 1, 1, {
            numSkillsRequired: 1,
            description: groupDescription
        });

        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 150,
            numPerformToCompletion: 2
        });

        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'false',
                setting: 'group-info-on-skill-page',
                projectId: 'proj1',
            },
            {
                value: 'true',
                setting: 'group-descriptions',
                projectId: 'proj1',
            },
        ]);

        cy.cdVisit('/subjects/subj1/skills/skill1', true);

        cy.get('[data-cy="groupInformationSection"]').should('exist');

        cy.get('[data-cy="toggleGroupDescription"]').should('not.exist');
        cy.get('[data-cy="groupDescriptionSection"]').should('exist');
        cy.get('[data-cy="groupDescriptionSection"]').contains(groupDescription);

    });

    it('group info is not shown on skill page if always show descriptions is toggled but group info on skill page is not', () => {
        let groupDescription = "This is where cool description"
        cy.createSkillsGroup(1, 1, 1, {
            numSkillsRequired: 1,
            description: groupDescription
        });

        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 150,
            numPerformToCompletion: 2
        });

        cy.request('POST', '/admin/projects/proj1/settings', [
            {
                value: 'true',
                setting: 'group-info-on-skill-page',
                projectId: 'proj1',
            },
            {
                value: 'true',
                setting: 'group-descriptions',
                projectId: 'proj1',
            },
        ]);

        cy.cdVisit('/subjects/subj1/skills/skill1', true);

        cy.get('[data-cy="groupInformationSection"]').should('not.exist');
        cy.get('[data-cy="toggleGroupDescription"]').should('not.exist');
        cy.get('[data-cy="groupDescriptionSection"]').should('not.exist');

    });

    it('test full setting process', () => {
        let groupDescription = "This is where cool description"
        cy.createSkillsGroup(1, 1, 1, {
            numSkillsRequired: 1,
            description: groupDescription
        });

        cy.addSkillToGroup(1, 1, 1, 1, {
            pointIncrement: 100,
            numPerformToCompletion: 2
        });
        cy.addSkillToGroup(1, 1, 1, 2, {
            pointIncrement: 150,
            numPerformToCompletion: 2
        });

        cy.cdVisit('/subjects/subj1/skills/skill1', true);

        cy.get('[data-cy="groupInformationSection"]').should('exist');
        cy.get('[data-cy="toggleGroupDescription"]').should('exist');
        cy.get('[data-cy="toggleGroupDescription"]').click();
        cy.get('[data-cy="groupDescriptionSection"]').should('exist');
        cy.get('[data-cy="groupDescriptionSection"]').contains(groupDescription);

        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="groupInfoOnSkillPageSwitch"]').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="saveSettingsBtn"]').click();

        cy.cdVisit('/subjects/subj1/skills/skill1', true);

        cy.get('[data-cy="groupInformationSection"]').should('not.exist');
        cy.get('[data-cy="toggleGroupDescription"]').should('not.exist');

        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="groupDescriptionsSwitch"]').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="saveSettingsBtn"]').click();

        cy.cdVisit('/subjects/subj1/skills/skill1', true);
        cy.get('[data-cy="groupInformationSection"]').should('not.exist');

        cy.visit('/administrator/projects/proj1/settings');
        cy.get('[data-cy="groupInfoOnSkillPageSwitch"]').click();
        cy.get('[data-cy="saveSettingsBtn"]').should('be.enabled');
        cy.get('[data-cy="saveSettingsBtn"]').click();

        cy.cdVisit('/subjects/subj1/skills/skill1', true);
        cy.get('[data-cy="groupInformationSection"]').should('exist');
        cy.get('[data-cy="groupDescriptionSection"]').should('exist');
        cy.get('[data-cy="groupDescriptionSection"]').contains(groupDescription);

    });
})