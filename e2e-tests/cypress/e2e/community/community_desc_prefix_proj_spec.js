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

    it('new project', () => {
        cy.visit('/administrator/')
        cy.wait('@getConfig')
        cy.get('[data-cy="newProjectButton"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Project')
        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOptions(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOptions(null, 'jabberwocky', ['A', 'B'], ['C', 'D'], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOptions(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)
    })

    it('edit a project - all dragons', () => {
        cy.visit('/administrator')
        cy.get('@getConfig')
        cy.get('[data-cy="projectCard_proj1"] [data-cy="editProjBtn"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOptions(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOptions(null, 'jabberwocky', ['A', 'B'], ['C', 'D'], '', false)
    })

    it.only('edit a project - all dragons - project page', () => {
        cy.visit('/administrator/projects/proj1')
        cy.get('@getConfig')
        cy.get('[data-cy="btn_edit-project"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.validateAllDragonOptions(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOptions(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOptions(null, 'jabberwocky', ['A', 'B'], ['C', 'D'], '', false)
    })

    it('edit a project - divine dragons', () => {
        cy.visit('/administrator')
        cy.get('@getConfig')
        cy.validateDivineDragonOptions('[data-cy="projectCard_proj2"] [data-cy="editProjBtn"]')
    })

    it('edit a project - divine dragons - project page', () => {
        cy.visit('/administrator/projects/proj2')
        cy.get('@getConfig')
        cy.validateDivineDragonOptions('[data-cy="btn_edit-project"]')
    })

});
