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

describe('Community and Desc Prefix Quiz Tests', () => {

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

            cy.loginAsAdminUser()
        });

        cy.viewport(1400, 1000)
        cy.createQuizDef(1)
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.runQuizForUser(1, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.runQuizForUser(1, 2, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')

        cy.createQuizDef(2, {enableProtectedUserCommunity: true});
        cy.createQuizQuestionDef(2, 1)
        cy.createTextInputQuestionDef(2, 1)
        cy.createTextInputQuestionDef(2, 2)
        cy.runQuizForUser(2, 1, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
        cy.runQuizForUser(2, 2, [{selectedIndex: [0]}, {selectedIndex: [0]}], true, 'My Answer')
    });

    it('new quiz', () => {
        cy.visit('/administrator/quizzes')
        cy.wait('@getConfig')
        cy.get('[data-cy="btn_Quizzes And Surveys"]').click()
        cy.get('[data-p="modal"] [data-pc-section="title"]').contains('New Quiz')

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

    it('edit a quiz - all dragons', () => {
        cy.visit('/administrator/quizzes')
        cy.get('@getConfig')
        cy.get('[data-cy="editQuizButton_quiz1"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validateAllDragonPrefixOps(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'jabberwocky', ['A', 'B'], ['C', 'D'], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
    })

    it('edit a quiz - all dragons - quiz page', () => {
        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')
        cy.get('[data-cy="editQuizButton"]').click()
        cy.get(`[data-pc-name="dialog"] [data-cy="markdownEditorInput"]`).should('be.visible')

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validateAllDragonPrefixOps(null)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'divinedragon', ['A', 'B', 'C', 'D'], [], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
        cy.validatePrefixOps(null, 'jabberwocky', ['A', 'B'], ['C', 'D'], '', false)

        cy.get('[data-cy="restrictCommunity"]').click()
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{selectall}{backspace}');
    })

    it('edit a project - divine dragons', () => {
        cy.visit('/administrator/quizzes')
        cy.get('@getConfig')
        cy.validateDivineDragonPrefixOps('[data-cy="editQuizButton_quiz2"]', '', true)
    })

    it('edit a quiz - divine dragons - quiz page', () => {
        cy.visit('/administrator/quizzes/quiz2')
        cy.get('@getConfig')
        cy.validateDivineDragonPrefixOps('[data-cy="editQuizButton"]', '', true)
    })
});

