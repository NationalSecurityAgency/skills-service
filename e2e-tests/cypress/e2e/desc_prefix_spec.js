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

describe('Desc Prefix Tests', () => {

    beforeEach( () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.addPrefixToInvalidParagraphsOptions = '(A) ,(B) ,(C) ,(D) ';
                res.send(conf);
            });
        }).as('getConfig');


        cy.viewport(1400, 1000)
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1);

    });

    it('apply prefix', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()

        cy.get('[data-cy="prefixSelect"]').should('not.exist')
        cy.get('[data-cy="markdownEditorInput"]').type('jabberwocky');
        cy.get('[data-cy="descriptionError"]').contains('not contain jabberwocky');
        cy.get('[data-cy="prefixSelect"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(A) "]')
        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]')
        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]')
        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(D) "]')

        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]').click()

        cy.get('[data-cy="addPrefixBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]').contains('(B) jabberwocky')
    });


});
