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
import './community_warnings_help_commands.js'

describe('Quiz - Community Attachment Warning Tests', () => {

    const allDragonsUser = 'allDragons@email.org'

    beforeEach(() => {
        cy.fixture('vars.json').then((vars) => {
            cy.logout();
            cy.login(vars.rootUser, vars.defaultPass, true);
            cy.request('POST', `/root/users/${vars.rootUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.request('POST', `/root/users/${vars.defaultUser}/tags/dragons`, { tags: ['DivineDragon'] });
            cy.logout();

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });

        const fileMsg = 'Friendly Reminder: Only safe files please for {{community.project.descriptor}}'
        const descMsg = 'Friendly Reminder: Only safe descriptions for {{community.project.descriptor}}'
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.attachmentWarningMessage = fileMsg;
                conf.descriptionWarningMessage = descMsg;
                res.send(conf);
            });
        }).as('loadConfig');

        cy.viewport(1400, 1000)
        cy.createGlobalBadge(1)

        cy.createGlobalBadge(2, {enableProtectedUserCommunity: true});
    });

    it('new global badge', () => {
        cy.visit('/administrator/globalBadges')
        cy.wait('@loadConfig')
        cy.get('[data-cy="btn_Global Badges"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Badge')
        cy.validateAllDragonsWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateAllDragonsWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonWarning(false)
    })

    it('edit global badge', () => {
        cy.visit('/administrator/globalBadges')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="badgeCard-globalBadge1"] [data-cy="editBtn"]', 'Editing Existing Badge')
        cy.validateAllDragonsWarning()

        cy.get('[data-cy="closeDialogBtn"]').click()
        cy.get('[data-p="modal"]').should('not.exist')

        cy.openDescModalAndAttachFile('[data-cy="badgeCard-globalBadge2"] [data-cy="editBtn"]', 'Editing Existing Badge')
        cy.validateDivineDragonWarning()
    })

    it('edit global badge and elevate UC', () => {
        cy.visit('/administrator/globalBadges')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="badgeCard-globalBadge1"] [data-cy="editBtn"]', 'Editing Existing Badge')
        cy.validateAllDragonsWarning()

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateAllDragonsWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonWarning(false)

        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_edit-badge"]', 'Editing Existing Badge')
        cy.validateAllDragonsWarning()

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateAllDragonsWarning(false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.validateDivineDragonWarning(false)
    })

    it('edit global badge - from badge page', () => {
        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.wait('@loadConfig')
        cy.openDescModalAndAttachFile('[data-cy="btn_edit-badge"]', 'Editing Existing Badge')
        cy.validateAllDragonsWarning()

        cy.visit('/administrator/globalBadges/globalBadge2')
        cy.openDescModalAndAttachFile('[data-cy="btn_edit-badge"]', 'Editing Existing Badge')
        cy.validateDivineDragonWarning()
    })

});