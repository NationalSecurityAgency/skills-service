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

    const snapshotOptions = {
        blackout: ['[data-cy=skillTableCellCreatedDate]'],
        failureThreshold: 0.03, // threshold for entire image
        failureThresholdType: 'percent', // percent of image or number of pixels
        customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
        capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
    };

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: "proj1"
        })
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: "Subject 1",
        })
    });

    it('markdown features', () => {
        cy.visit('/projects/proj1/');

        const markdownInput = '[data-cy=markdownEditorInput]';
        cy.get('[data-cy=cardSettingsButton]').click();
        cy.contains('Edit').click();

        const validateMarkdown = (markdown, snapshotName, expectedText = null, clickWrite = true) => {
            if (clickWrite) {
                cy.contains('Write').click();
            }
            cy.get(markdownInput).clear().type(markdown);
            cy.contains('Preview').click();
            // move focus away from Preview
            cy.contains('Description').click();
            if (expectedText) {
                cy.contains(expectedText);
            }
            cy.get('[data-cy="markdownEditor-preview"]').matchImageSnapshot(snapshotName);
        }
        validateMarkdown('# Title1\n## Title2\n### Title 3\n#### Title 4\n##### Title 5\nTitle 6\n\n', 'Markdown-Titles',  null,false);

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

        validateMarkdown(':star: :star: :star: :star:', 'Markdown-emoji', '⭐ ⭐ ⭐ ⭐')
    });

    it('on skills pages', () => {

        const markdown = "# Title1\n## Title2\n### Title 3\n#### Title 4\n##### Title 5\nTitle 6\n\n" +
            "---\n" +
            "# Emphasis\n" +
            "italics: *italicized* or _italicized_\n\n" +
            "bold: **bolded** or __bolded__\n\n" +
            "combination **_bolded & italicized_**\n\n" +
            "strikethrough: ~~struck~~\n\n" +
            "---\n" +
            "# Inline\n" +
            "Inline `code` has `back-ticks around` it\n\n" +
            "---\n" +
            "# Multiline\n" +
            "\n" +
            "\n" +
            "```\n" +
            "import { SkillsDirective } from '@skilltree/skills-client-vue';\n" +
            "Vue.use(SkillsDirective);\n" +
            "```\n" +
            "# Lists\n" +
            "Ordered Lists:\n" +
            "1. Item one\n" +
            "1. Item two\n" +
            "1. Item three (actual number does not matter)\n\n" +
            "If List item has multiple lines of text, subsequent lines must be idented four spaces, otherwise list item numbers will reset, e.g.,\n" +
            "1. item one\n" +
            "    paragrah one\n" +
            "1. item two\n" +
            "1. item three\n" +
            "\n" +
            "Unordered Lists\n" +
            "* Item\n" +
            "* Item\n" +
            "* Item\n" +
            "___\n" +
            "# Links\n" +
            "[in line link](https://www.somewebsite.com)\n" +
            "___\n" +
            "# Blockquotes\n" +
            "> Blockquotes are very handy to emulate reply text.\n" +
            "> This line is part of the same quote.\n\n" +
            "# Horizontal rule\n" +
            "Use three or more dashes, asterisks, or underscores to generate a horizontal rule line\n" +
            "\n" +
            "Separate me\n\n" +
            "___\n\n" +
            "Separate me\n\n" +
            "---\n\n" +
            "Separate me\n\n" +
            "***\n\n" +
            "# Emojis\n" +
            ":star: :star: :star: :star:\n" +
            "";
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: "subj1",
            skillId: "skill1",
            name: "Skill 1",
            pointIncrement: '50',
            numPerformToCompletion: '5',
            description: markdown
        });
        cy.intercept('GET', '/api/projects/Inception/level').as('inceptionLevel');
        cy.visit('/projects/proj1/subjects/subj1/skills/skill1');

        cy.contains('Description');
        cy.wait('@inceptionLevel');
        cy.contains('Level');
        cy.contains('Emojis')
        cy.contains('⭐ ⭐ ⭐ ⭐');
        cy.matchImageSnapshot('Markdown-SkillsPage-Overview', snapshotOptions);

        cy.visit('/projects/proj1/subjects/subj1');
        cy.wait('@inceptionLevel');
        cy.contains('Level');
        const selectorSkillsRowToggle = '[data-cy="expandDetailsBtn_skill1"]';
        cy.get(selectorSkillsRowToggle).click();
        cy.contains('Description');
        cy.contains('Emojis')
        cy.contains('⭐ ⭐ ⭐ ⭐');
        cy.matchImageSnapshot('Markdown-SubjectPage-SkillPreview', snapshotOptions);
    });

})
