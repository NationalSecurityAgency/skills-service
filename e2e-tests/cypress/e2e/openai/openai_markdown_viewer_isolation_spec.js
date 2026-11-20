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
import { selectSingleRockBandQuestion } from './openai_helper_commands';

describe('AI Markdown Viewer Isolation', () => {
    it('keeps saved generated question text separate from AI prompts with matching viewer IDs', () => {
        const prompt = 'Great rock bands';
        const savedQuestion = '[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]';
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.enableOpenAIIntegration = true;
                res.send(res.body);
            });
        });

        // The shared reset removes questions but does not reset their identity sequence.
        // Force question ID 1 to collide with the first user message's viewer ID.
        cy.exec('npm run backend:execSql -- "SELECT setval(pg_get_serial_sequence(\'quiz_question_definition\', \'id\'), 1, false)"');
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');

        const generateQuestion = () => {
            cy.get('[data-cy="btn_Questions"]').click();
            cy.get('[data-cy="aiButton"]').click();
            cy.get('[data-cy="genQuestionTypeSelector"]').click();
            cy.get('[data-cy="selectionItem_SingleChoice"]').click();
            cy.get('[data-cy="instructionsInput"]').type(`${prompt}{enter}`);
            cy.get('[data-cy="userMsg-1"]').should('contain.text', prompt);
            cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]')
                .should('contain.text', selectSingleRockBandQuestion);
            cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled');
        };
        const saveGeneratedQuestion = () => {
            cy.get('[data-cy="useGenValueBtn-2"]').click();
            cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]')
                .should('contain.text', selectSingleRockBandQuestion);
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
            cy.get('[data-cy="editQuestionModal"]').should('not.exist');
        };
        const assertSavedQuestion = () => {
            cy.get(savedQuestion).should('be.visible')
                .and('contain.text', selectSingleRockBandQuestion)
                .and('not.contain.text', prompt);
        };

        generateQuestion();
        saveGeneratedQuestion();
        assertSavedQuestion();

        // Mount the chat prompt while the saved question's viewer is on the page.
        generateQuestion();
        cy.get(savedQuestion).should('contain.text', selectSingleRockBandQuestion)
            .and('not.contain.text', prompt);
        saveGeneratedQuestion();
        assertSavedQuestion();
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]')
            .should('be.visible').and('contain.text', selectSingleRockBandQuestion)
            .and('not.contain.text', prompt);

        cy.reload();
        assertSavedQuestion();
    });
});
