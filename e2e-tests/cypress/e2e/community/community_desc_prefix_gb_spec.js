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

describe('Community and Desc Prefix Global Badge Tests', () => {

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

            cy.register(allDragonsUser, vars.defaultPass);
            cy.logout();

            cy.login(vars.defaultUser, vars.defaultPass);
        });

        cy.viewport(1400, 1000)
        cy.createGlobalBadge(1)

        cy.createGlobalBadge(2, {enableProtectedUserCommunity: true});
    });

    it('new global badge', () => {
        cy.visit('/administrator/globalBadges')
        cy.wait('@getConfig')
        cy.get('[data-cy="btn_Global Badges"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Badge')

        cy.validateAllDragonPrefixOps(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'jabberwocky', ['A', 'B'], ['C', 'D'], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)
    })

    it('edit a global badge - all dragons', () => {
        cy.visit('/administrator/globalBadges')
        cy.get('@getConfig')
        cy.get('[data-cy="badgeCard-globalBadge1"] [data-cy="editBtn"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.validateAllDragonPrefixOps(null, '', true)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'jabberwocky', ['A', 'B'], ['C', 'D'], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)
    })

    it('edit a global badge - all dragons - gb page', () => {
        cy.visit('/administrator/globalBadges/globalBadge1')
        cy.get('@getConfig')
        cy.get('[data-cy="btn_edit-badge"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.validateAllDragonPrefixOps(null, '', true)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'jabberwocky', ['A', 'B'], ['C', 'D'], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)
    })

    it('edit a global badge - divine dragons', () => {
        cy.visit('/administrator/globalBadges')
        cy.get('@getConfig')
        cy.validateDivineDragonPrefixOps('[data-cy="badgeCard-globalBadge2"] [data-cy="editBtn"]', '', true)
    })

    it('edit a global badge - divine dragons - gb page', () => {
        cy.visit('/administrator/globalBadges/globalBadge2')
        cy.get('@getConfig')
        cy.validateDivineDragonPrefixOps('[data-cy="btn_edit-badge"]', '', true)
    })
});

