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
import { completedMsg } from './openai_helper_commands';

describe('AI Single Question Parsing and Validation', () => {
    beforeEach(() => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                res.body.enableOpenAIIntegration = true;
                res.send(res.body);
            });
        });
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click();
        cy.get('[data-cy="aiButton"]').click();
        cy.get('[data-cy="genQuestionTypeSelector"]').click();
        cy.get('[data-cy="selectionItem_FillInTheBlank"]').click();
    });

    [false, true].forEach((fencedAnswers) => {
        it(`preserves Markdown and code with ${fencedAnswers ? 'fenced' : 'unfenced'} JSON answers`, () => {
            const answers = ['n // 2', '"https://example.org/*docs*/"'];
            cy.get('[data-cy="instructionsInput"]').type(`complex programming question with a code example - ${fencedAnswers ? 'fenced' : 'unfenced'} JSON answers{enter}`);
            cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('contain.text', completedMsg);
            answers.forEach((answer) => {
                cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').should('contain.text', answer);
            });
            cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled').click();
            cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]')
                .should('contain.text', '## Code example')
                .and('contain.text', '# Use integer division and the documentation URL')
                .and('contain.text', 'half = ___')
                .and('contain.text', 'docs = ___')
                .and('contain.text', 'return half, docs')
                .and('contain.text', 'Keep both expressions intact.');
            answers.forEach((answer, index) => {
                cy.get(`[data-cy="answer-${index}"] [data-cy="answerText"]`).should('have.value', answer);
            });
            cy.get('[data-cy="answer-2"]').should('not.exist');
            cy.get('[data-cy="saveDialogBtn"]').should('be.enabled').click();
            cy.get('[data-cy="markdownEditorInput"]').should('not.exist');
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]')
                .should('contain.text', 'Code example')
                .and('contain.text', '# Use integer division and the documentation URL')
                .and('contain.text', 'return half, docs')
                .and('contain.text', 'Keep both expressions intact.');
            answers.forEach((answer) => {
                cy.get('[data-cy="questionDisplayCard-1"]').should('contain.text', answer);
            });
        });
    });

    [
        {
            prompt: 'AI blank validation - mismatched answers',
            error: 'The generated question has 3 blank(s), but 2 answer(s). Please close and reopen the AI assistant to start a new conversation'
        },
        {
            prompt: 'AI blank validation - no blanks',
            error: 'The generated question needs at least one blank (___). Please ask the AI to regenerate it.'
        }
    ].forEach(({ prompt, error }) => {
        it(`rejects generated questions: ${prompt}`, () => {
            cy.get('[data-cy="instructionsInput"]').type(`${prompt}{enter}`);
            cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').should('be.visible').and('contain.text', error);
            cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist');
            cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').should('not.exist');
            cy.get('[data-cy="questionDisplayCard-1"]').should('not.exist');
            cy.get('[data-cy="genQuestionTypeSelector"]').should('not.have.class', 'p-disabled');
            cy.get('[data-cy="instructionsInput"]').should('have.focus');
        });
    });
});
