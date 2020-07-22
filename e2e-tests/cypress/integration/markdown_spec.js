/*
 * Copyright 2020 SkillTree
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
describe('Markdown Tests', () => {

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
    });

    it('markdown features', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        })
        cy.visit('/projects/proj1/');

        const markdownInput = '[data-cy=markdownEditorInput]';
        cy.get('[data-cy=cardSettingsButton]').click();
        cy.contains('Edit').click();

        // cy.get(markdownInput).type('# Title1\n## Title2\n### Title 3\n#### Title 4\n##### Title 5\nTitle 6\n\n')
        // cy.contains('Preview').click();
        // cy.matchImageSnapshot('Markdown-Titles');

        const validateMarkdown = (markdown, snapshotName, clickWrite = true) => {
            if (clickWrite) {
                cy.contains('Write').click();
            }
            cy.get(markdownInput).clear().type(markdown);
            cy.contains('Preview').click();
            // move focus away from Preview
            cy.contains('Description').click();
            cy.matchImageSnapshot(snapshotName);
        }
        validateMarkdown('# Title1\n## Title2\n### Title 3\n#### Title 4\n##### Title 5\nTitle 6\n\n', 'Markdown-Titles', false);

        const emphasisMarkdown = "italics: *italicized* or _italicized_\n\n" +
        "bold: **bolded** or __bolded__\n\n" +
        "combination **_bolded & italicized_**\n\n" +
        "strikethrough: ~~struck~~\n\n";
        validateMarkdown(emphasisMarkdown, 'Markdown-Emphasis');

        validateMarkdown("Inline `code` has `back-ticks around` it\n\n", 'Markdown-Inline')

        const multiLineCode = "Some text followed by code\n" +
            "```\n" +
            "const validateMarkdown = (markdown, snapshotName) => {\n" +
            "}\n" +
            "```";
        validateMarkdown(multiLineCode, 'Markdown-MultiLineCode')

        validateMarkdown('Some text:\n1. Item one\n1. Item two\n1. Item three (actual number does not matter)', 'Markdown-NumberedList')

        validateMarkdown('List:\n* Item\n* Item\n* Item\n', 'Markdown-UnorderedList')

        validateMarkdown('[in line link](https://www.somewebsite.com)', 'Markdown-Link')

        const blockQuote = "# Blockquote:\n" +
            "> Blockquotes are very handy to emulate reply text.\n" +
            "> This line is part of the same quote.\n\n";
        validateMarkdown(blockQuote, 'Markdown-blockquote');

        validateMarkdown('Separate me\n\n___\n\nSeparate me\n\n---\n\nSeparate me\n\n***', 'Markdown-Separator')
    });

})
