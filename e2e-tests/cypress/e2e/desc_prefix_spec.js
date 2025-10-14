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
    const letters = ['A', 'B', 'C', 'D'];
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
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', 'paragraph 1\n\nparagraph 2 - jabberwocky\n\nparagraph 3\n\nparagraph 4 - jabberwocky\n\nparagraph 5');
        cy.get('[data-cy="descriptionError"]').contains('not contain jabberwocky');
        cy.get('[data-cy="prefixSelect"]').click()
        for (const letter of letters) {
            cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${letter}) "]`);
        }

        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]').click()

        cy.get('[data-cy="addPrefixBtn"]').click()
        cy.validateMarkdownEditorText('[data-cy="markdownEditorInput"]', [
            'paragraph 1',
            '(B) paragraph 2 - jabberwocky',
            'paragraph 3',
            '(B) paragraph 4 - jabberwocky',
            'paragraph 5',
        ])
    });

    it('preview prefix', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1')
        cy.get('@getConfig')

        cy.get('[data-cy="editSkillButton_skill1"]').click()

        cy.get('[data-cy="prefixSelect"]').should('not.exist')
        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', 'paragraph 1\n\nparagraph 2 - jabberwocky\n\nparagraph 3\n\nparagraph 4 - jabberwocky\n\nparagraph 5');
        cy.get('[data-cy="descriptionError"]').contains('not contain jabberwocky');
        cy.get('[data-cy="prefixSelect"]').click()
        for (const letter of letters) {
            cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${letter}) "]`);
        }

        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(C) "]').click()

        cy.get('[data-cy="missingPreviewText"]').should('not.exist')
        cy.get('[data-cy="previewPrefixBtn"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')
        cy.get('[data-cy="previewPrefixBtn"]').should('not.be.visible')
        cy.get('[data-cy="addPrefixBtn"]').should('not.be.visible')
        cy.get('[data-cy="closeMissingPreviewBtn"]').should('be.enabled')
        cy.get('[data-cy="markdownEditorInput"]').should('not.be.visible')
        cy.validateMarkdownViewerText('[data-cy="missingPreviewText"]', [
            'paragraph 1',
            '(C) paragraph 2 - jabberwocky',
            'paragraph 3',
            '(C) paragraph 4 - jabberwocky',
            'paragraph 5',
        ])

        cy.get('[data-cy="closeMissingPreviewBtn"]').click()
        cy.get('[data-cy="descriptionError"]').contains('not contain jabberwocky');
        cy.get('[data-cy="previewPrefixBtn"]').should('be.enabled')
        cy.get('[data-cy="addPrefixBtn"]').should('be.enabled')
        cy.get('[data-cy="closeMissingPreviewBtn"]').should('not.exist')

        cy.validateMarkdownEditorText('[data-cy="markdownEditorInput"]', [
            'paragraph 1',
            'paragraph 2 - jabberwocky',
            'paragraph 3',
            'paragraph 4 - jabberwocky',
            'paragraph 5',
        ])
    });


});
