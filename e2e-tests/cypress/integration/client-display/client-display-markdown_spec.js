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
import moment from 'moment';
const dateFormatter = value => moment.utc(value).format('YYYY-MM-DD[T]HH:mm:ss[Z]');

describe('Client Display Markdown Tests', () => {
    const snapshotOptions = {
        blackout: ['[data-cy=pointHistoryChart]'],
        failureThreshold: 0.03, // threshold for entire image
        failureThresholdType: 'percent', // percent of image or number of pixels
        customDiffConfig: { threshold: 0.01 }, // threshold for each pixel
        capture: 'fullPage', // When fullPage, the application under test is captured in its entirety from top to bottom.
    };

    beforeEach(() => {
        Cypress.env('disabledUILoginProp', true);
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
    })

    it('subject\'s markdown', () => {
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

        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1',
            helpUrl: 'http://doHelpOnThisSubject.com',
            description: markdown
        });

        cy.request('POST', `/admin/projects/proj1/subjects/subj1/skills/skill1`, {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: `skill1`,
            name: `This is 1`,
            type: 'Skill',
            pointIncrement: 50,
            numPerformToCompletion: 2,
            pointIncrementInterval: 0,
            numMaxOccurrencesIncrementInterval: -1,
            description: markdown,
            version: 0,
            helpUrl: 'http://doHelpOnThisSkill.com'
        });

        cy.request('POST', '/admin/projects/proj1/badges/badge1', {
            projectId: 'proj1',
            badgeId: 'badge1',
            name: 'Badge 1',
            "iconClass":"fas fa-ghost",
            description: markdown,
        });

        cy.cdVisit('/');
        cy.contains('Overall Points');

        // check subject
        cy.cdClickSubj(0, 'Subject 1');
        cy.contains('Emphasis');
        cy.matchSnapshotImage(`Markdown-subject`, snapshotOptions);

        // check skill
        cy.cdClickSkill(0);
        cy.contains('This is 1');
        cy.contains('Emphasis');
        cy.matchSnapshotImage(`Markdown-skill`, snapshotOptions);

        // check expanded skill
        cy.cdBack('Subject 1');
        cy.get('[data-cy=toggleSkillDetails]').click()
        cy.contains('Overall Points Earned');
        cy.matchSnapshotImage(`Markdown-Skill-Preview`, snapshotOptions);

        cy.cdVisit('/');
        cy.contains('Overall Points');

        // check badge
        cy.cdClickBadges();
        cy.contains('Badges');
        cy.contains('Emphasis');
        cy.matchSnapshotImage(`Markdown-Badge`, snapshotOptions);
    });
});
