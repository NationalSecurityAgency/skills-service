/*
 * Copyright 2025 SkillTree
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
import './community-commands'

describe('Community and Desc Prefix Project Tests', () => {

    const allDragonsUser = 'allDragons@email.org'
    beforeEach( () => {
        const descMsg = 'Friendly Reminder: Only safe descriptions for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.addPrefixToInvalidParagraphsOptions = 'All Dragons:(A) ,(B) |Divine Dragon:(A) ,(B) ,(C) ,(D) ';
                conf.descriptionWarningMessage = descMsg;
                res.send(conf);
            });
        }).as('getConfig');

        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, "password");
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);


        });

        cy.viewport(1400, 1400)
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);
        cy.createSkillsGroup(1, 1, 2)
        cy.createBadge(1, 1, {description: null})

        cy.createProject(2, {enableProtectedUserCommunity: true});
        cy.createSubject(2, 1);
        cy.createSkill(2, 1, 1);
        cy.createSkillsGroup(2, 1, 2)
        cy.createBadge(2, 1, {description: null})
    });

    it('skill justification', () => {
        cy.createSkill(1, 1, 3, {selfReportingType: 'Approval'});
        cy.createSkill(2, 1, 3, {selfReportingType: 'Approval'});
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill3')
        cy.wait('@getConfig')
        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.validateAllDragonOptions(null)

        cy.visit('/progress-and-rankings/projects/proj2/subjects/subj1/skills/skill3')
        cy.wait('@getConfig')
        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.validateDivineDragonOptions(null)
    })

    it('skill justification as all dragons user', () => {
        cy.createSkill(1, 1, 3, {selfReportingType: 'Approval'});
        cy.createSkill(2, 1, 3, {selfReportingType: 'Approval'});

        cy.logout()
        cy.login(allDragonsUser)

        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1/skills/skill3')
        cy.wait('@getConfig')
        cy.get('[data-cy="requestApprovalBtn"]').click()
        cy.validateAllDragonOptions(null)
    })

    it('skill justification from subject page', () => {
        cy.createSkill(1, 1, 3, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(1, 1, 5, { selfReportingType: 'Approval' });

        cy.createSkill(2, 1, 3, { selfReportingType: 'Approval' });
        cy.createSkill(2, 1, 4, { selfReportingType: 'Approval' });
        cy.createSkill(2, 1, 5, { selfReportingType: 'Approval' });
        cy.visit('/progress-and-rankings/projects/proj1/subjects/subj1')
        cy.wait('@getConfig')
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="skillProgressTitle"]').contains('Great Skill 5')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="skillProgress_index-1"]')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requestApprovalBtn"]').click()
        cy.validateAllDragonOptions(null, '[data-cy="skillProgress_index-2"]')


        cy.visit('/progress-and-rankings/projects/proj2/subjects/subj1')
        cy.wait('@getConfig')
        cy.get('[data-cy="toggleSkillDetails"]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgressTitle"]').contains('Great Skill 1')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgressTitle"]').contains('Great Skill 3')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgressTitle"]').contains('Great Skill 4')
        cy.get('[data-cy="skillProgress_index-3"] [data-cy="skillProgressTitle"]').contains('Great Skill 5')

        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requestApprovalBtn"]').click()
        cy.validateDivineDragonOptions(null, '[data-cy="skillProgress_index-1"]')

        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requestApprovalBtn"]').click()
        cy.validateDivineDragonOptions(null, '[data-cy="skillProgress_index-2"]')
    });

});
