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

    const markdownInput = '[data-cy=markdownEditorInput] div.toastui-editor-contents[contenteditable="true"]';

    Cypress.Commands.add('clickToolbarButton', (buttonName) => {
        cy.get(`button.${buttonName}`).click({force: true})
    });
    Cypress.Commands.add('addHeading', (headingLevel, headingText) => {
        cy.clickToolbarButton('heading')
        cy.get(`ul > li[data-level=${headingLevel}]`).click({force: true})
        cy.focused().type(headingText);
    });
    Cypress.Commands.add('selectParagraphText', () => {
        cy.clickToolbarButton('heading')
        cy.get('ul > li[data-type=Paragraph]').click({force: true})
    });
    Cypress.Commands.add('addBold', (text) => {
        cy.clickToolbarButton('bold')
        cy.focused().type(text);
    });

    beforeEach(() => {
        cy.request('POST', '/app/projects/proj1', {
            projectId: 'proj1',
            name: 'proj1'
        });
        cy.request('POST', '/admin/projects/proj1/subjects/subj1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            name: 'Subject 1',
        });
    });

    it('URL in markdown must open in a new tab', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy=newSkillButton]')
            .click();
        cy.get('[data-cy=skillName]')
            .type('skill1');
        cy.clickToolbarButton('link')
        cy.get('#toastuiLinkUrlInput').type('https://google.com')
        cy.get('#toastuiLinkTextInput').type('Google Home Page')
        cy.get('.toastui-editor-ok-button').click()
        cy.focused().type('\n\n')
        cy.get('a[href="https://google.com"]')
            .should('have.attr', 'target', '_blank');
        cy.clickSave();
        cy.get('[data-cy="manageSkillBtn_skill1Skill"]')
            .click();
        cy.get('a[href="https://google.com"]')
            .should('have.attr', 'target', '_blank');
    });

    it('upload an attachment', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy=newSkillButton]')
          .click();
        cy.get('[data-cy=skillName]')
          .type('skill1');

        cy.clickToolbarButton('attachment-button')
        cy.get('input[type=file]').selectFile('cypress/attachments/test-pdf.pdf', { force: true })

        cy.get(markdownInput).get('a[href$="test-pdf.pdf"]')
          .should('have.attr', 'target', '_blank');
        cy.get('[data-cy="attachmentWarningMessage"]').should('have.text', 'Only upload attachments that are safe!')
        cy.clickSave();
        cy.get('[data-cy="manageSkillBtn_skill1Skill"]')
          .click();
        cy.get('a[href$="test-pdf.pdf"]')
          .should('have.attr', 'target', '_blank');
    });

    it('drag-drop upload an attachment', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy=newSkillButton]')
          .click();
        cy.get('[data-cy=skillName]')
          .type('skill1');

        cy.get(markdownInput).focus().selectFile('cypress/attachments/test-pdf.pdf', { action: 'drag-drop' })

        cy.get('a[href$="test-pdf.pdf"]')
          .should('have.attr', 'target', '_blank');
        cy.get('[data-cy="attachmentWarningMessage"]').should('have.text', 'Only upload attachments that are safe!')
        cy.clickSave();
        cy.get('[data-cy="manageSkillBtn_skill1Skill"]')
          .click();
        cy.get('a[href$="test-pdf.pdf"]')
          .should('have.attr', 'target', '_blank');
    });

    it('attempt to upload an attachment that is too large', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.maxAttachmentSize = 5;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy=newSkillButton]')
          .click();
        cy.get('[data-cy=skillName]')
          .type('skill1');

        cy.clickToolbarButton('attachment-button')
        cy.get('input[type=file]').selectFile('cypress/attachments/test-pdf.pdf', { force: true })

        cy.get(markdownInput).get('a[href$="test-pdf.pdf"]')
          .should('not.exist');
        cy.get('[data-cy=saveSkillButton]').should('be.disabled');
        cy.get('[data-cy=attachmentError]').contains('Unable to upload attachment - File size [7.25 KB] exceeds maximum file size [5 B]');
    });

    it('do not display upload warning when not configured', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.attachmentWarningMessage = null;
                res.send(conf);
            });
        }).as('loadConfig');
        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy=newSkillButton]')
          .click();
        cy.get('[data-cy=skillName]')
          .type('skill1');

        cy.get('[data-cy="attachmentWarningMessage"]').should('not.exist')
        cy.clickToolbarButton('attachment-button')
        cy.get('input[type=file]').selectFile('cypress/attachments/test-pdf.pdf', { force: true })

        cy.get(markdownInput).get('a[href$="test-pdf.pdf"]')
          .should('have.attr', 'target', '_blank');
        cy.get('[data-cy="attachmentWarningMessage"]').should('not.exist')
    });

    it('attempt to upload an attachment that is not an accepted mime-type', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy=newSkillButton]')
          .click();
        cy.get('[data-cy=skillName]')
          .type('skill1');

        cy.clickToolbarButton('attachment-button')
        cy.get('input[type=file]').selectFile([{
            contents: 'cypress/attachments/test-pdf.pdf',
            mimeType: 'invalid/type'  // assign invalid mime-type
        }], { force: true })

        cy.get(markdownInput).get('a[href$="test-pdf.pdf"]')
          .should('not.exist');
        cy.get('[data-cy=saveSkillButton]').should('be.disabled');
        cy.get('[data-cy=attachmentError]').contains('Unable to upload attachment - File type is not supported. Supported file types are [.xlsx,.docx,.pptx,.doc,.odp,.ods,.odt,.pdf,.ppt,.xls]');
    });

    it('upload valid mime-types', () => {
        cy.visit('/administrator/projects/proj1/subjects/subj1/');
        cy.get('[data-cy=newSkillButton]')
          .click();
        cy.get('[data-cy=skillName]')
          .type('skill1');

        const attachmentFiles = cy.task('getFilesFromDir', { directory: 'cypress/attachments/', ignore: 'invalid' });
        attachmentFiles.each((file) => {
            cy.get('input[type=file]').selectFile(`cypress/attachments/${file}`, {force: true})
            cy.get(markdownInput).get(`a[href$="/${file}"]`).should('exist');
            cy.get(markdownInput).type('\n\n')
        });
        cy.get('[data-cy=saveSkillButton]').should('be.enabled');
        cy.get('[data-cy=attachmentError]').scrollIntoView().should('not.be.visible');
        cy.clickSave();
        cy.get('[data-cy="manageSkillBtn_skill1Skill"]').click();

        attachmentFiles.each((file) => {
            cy.get('[data-cy="skillOverviewDescription"]').get(`a[href$="/${file}"]`).should('exist');
        });
    });

    it('keyboard navigation', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5',
        });
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');

        cy.get('[data-cy="editSkillButton_skill1"]').click();

        cy.get(markdownInput).click()
        cy.get('div.ProseMirror.toastui-editor-contents').should('have.focus')

        cy.realPress(['Shift', 'Tab'])
        cy.get('button.more').should('have.focus')

        cy.realPress('Tab')
        cy.get('div.ProseMirror.toastui-editor-contents').should('have.focus')

        cy.realPress('Tab')
        cy.get('[data-cy="editorFeaturesUrl"]').should('have.focus')

        cy.realPress(['Shift', 'Tab'])
        cy.get('div.ProseMirror.toastui-editor-contents').should('have.focus')
    });

    it('wysiwyg features', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5',
        });
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');

        cy.get('[data-cy="editSkillButton_skill1"]').click();
        cy.get(markdownInput).clear()
        cy.addHeading(1, 'Title1\n')
        cy.addHeading(2, 'Title2\n')
        cy.addHeading(3, 'Title3\n')
        cy.addHeading(4, 'Title4\n')
        cy.addHeading(5, 'Title5\n')
        cy.addHeading(6, 'Title6\n\n')

        cy.selectParagraphText()
        cy.focused().type('regular paragraph text\n\n');

        cy.focused().type('bold: ');
        cy.clickToolbarButton('bold')
        cy.focused().type('bolded\n\n');

        cy.focused().type('strikethrough: ');
        cy.clickToolbarButton('strike')
        cy.focused().type('struck\n\n');

        cy.clickToolbarButton('hrline')
        cy.focused().type('{downArrow}Separate me\n');
        cy.clickToolbarButton('hrline')
        cy.focused().type('{downArrow}Separate me\n');
        cy.clickToolbarButton('hrline')
        cy.focused().type('{downArrow}Separate me\n\n');

        cy.focused().type('list: \n');
        cy.clickToolbarButton('bullet-list')
        cy.focused().type('Item 1\nItem 1-A')
        cy.clickToolbarButton('indent')
        cy.focused().type('\nItem 1-B\nItem 1-C\nItem 2');
        cy.clickToolbarButton('outdent')
        cy.focused().type('\nItem 3\nItem 4\n\n')

        cy.focused().type('ordered list: \n');
        cy.clickToolbarButton('ordered-list')
        cy.focused().type('Item 1\nItem 1-A')
        cy.clickToolbarButton('indent')
        cy.focused().type('\nItem 1-B\nItem 1-C\nItem 2');
        cy.clickToolbarButton('outdent')
        cy.focused().type('\nItem 3\nItem 4\n\n')

        cy.clickToolbarButton('image')
        cy.get('div.toastui-editor-popup.toastui-editor-popup-add-image').contains('URL').click()
        cy.get('#toastuiImageUrlInput').type('https://github.com/NationalSecurityAgency/skills-service/raw/master/skilltree_logo.png')
        cy.get('.toastui-editor-ok-button').click()
        cy.focused().type('\n\n')

        cy.clickToolbarButton('link')
        cy.get('#toastuiLinkUrlInput').type('https://skilltreeplatform.dev/')
        cy.get('#toastuiLinkTextInput').type('SkillTree Docs')
        cy.get('.toastui-editor-ok-button').click()
        cy.focused().type('\n\n')

        cy.focused().type('This is some ');
        cy.clickToolbarButton('code')
        cy.focused().type('inline code');
        cy.clickToolbarButton('code')
        cy.focused().type('surrounded by normal text\n\n');

        cy.focused().type('\n{upArrow}Some text followed by a code block\n');
        cy.clickToolbarButton('codeblock')
        cy.focused().type('\n' +
          'const validateMarkdown = (markdown, snapshotName) => {\n' +
          '}\n');
        cy.clickToolbarButton('codeblock')

        cy.clickSave();
        cy.matchSnapshotImageForElement('[data-cy="childRowDisplay_skill1"]', 'WYSIWYG-Features', snapshotOptions);
    });

    it('on skills pages', () => {

        const markdown = '# Title1\n## Title2\n### Title 3\n#### Title 4\n##### Title 5\nTitle 6\n\n' +
            '---\n' +
            '# Emphasis\n' +
            'italics: *italicized* or _italicized_\n\n' +
            'bold: **bolded** or __bolded__\n\n' +
            'combination **_bolded & italicized_**\n\n' +
            'strikethrough: ~~struck~~\n\n' +
            '---\n' +
            '# Inline\n' +
            'Inline `code` has `back-ticks around` it\n\n' +
            '---\n' +
            '# Multiline\n' +
            '\n' +
            '\n' +
            '```\n' +
            'import { SkillsDirective } from \'@skilltree/skills-client-vue\';\n' +
            'Vue.use(SkillsDirective);\n' +
            '```\n' +
            '# Lists\n' +
            'Ordered Lists:\n' +
            '1. Item one\n' +
            '1. Item two\n' +
            '1. Item three (actual number does not matter)\n\n' +
            'If List item has multiple lines of text, subsequent lines must be idented four spaces, otherwise list item numbers will reset, e.g.,\n' +
            '1. item one\n' +
            '    paragrah one\n' +
            '1. item two\n' +
            '1. item three\n' +
            '\n' +
            'Unordered Lists\n' +
            '* Item\n' +
            '* Item\n' +
            '* Item\n' +
            '___\n' +
            '# Links\n' +
            '[in line link](https://www.somewebsite.com)\n' +
            '___\n' +
            '# Blockquotes\n' +
            '> Blockquotes are very handy to emulate reply text.\n' +
            '> This line is part of the same quote.\n\n' +
            '# Horizontal rule\n' +
            'Use three or more dashes, asterisks, or underscores to generate a horizontal rule line\n' +
            '\n' +
            'Separate me\n\n' +
            '___\n\n' +
            'Separate me\n\n' +
            '---\n\n' +
            'Separate me\n\n' +
            '***\n\n' +
            '# Emojis\n' +
            ':+1: :+1: :+1: :+1:\n' +
            '';
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5',
            description: markdown
        });
        cy.intercept('GET', '/api/projects/Inception/level')
            .as('inceptionLevel');
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');

        cy.contains('Description');
        cy.wait('@inceptionLevel');
        cy.contains('Level');
        cy.contains('Emojis');
        cy.contains('👍 👍 👍 👍');
        cy.matchSnapshotImageForElement('[data-cy="childRowDisplay_skill1"]', 'Markdown-SkillsPage-Overview', snapshotOptions);

        cy.visit('/administrator/projects/proj1/subjects/subj1');
        cy.wait('@inceptionLevel');
        cy.contains('Level');
        const selectorSkillsRowToggle = '[data-cy="expandDetailsBtn_skill1"]';
        cy.get(selectorSkillsRowToggle)
            .click();
        cy.contains('Description');
        cy.contains('Emojis');
        cy.contains('👍 👍 👍 👍');
        cy.matchSnapshotImageForElement('[data-cy="childRowDisplay_skill1"]', 'Markdown-SubjectPage-SkillPreview', snapshotOptions);
    });

    it('enter a block quote', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5',
        });
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');

        cy.get('[data-cy="editSkillButton_skill1"]').click();
        cy.get(markdownInput).clear()
        cy.clickToolbarButton('quote')
        cy.focused().type('this is a quote');
        cy.clickSave();
    });

    it('data-type selector does not exist on code blocks', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5',
        });
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');

        cy.get('[data-cy="editSkillButton_skill1"]').click();
        cy.get(markdownInput).clear()
        cy.clickToolbarButton('codeblock')
        cy.focused().type('this is some fancy code');
        cy.get('div.toastui-editor-ww-code-block').then($els => {
            // get Window reference from element
            const win = $els[0].ownerDocument.defaultView
            // use getComputedStyle to read the pseudo selector
            const after = win.getComputedStyle($els[0], 'after')
            // read the value of the `content` CSS property
            const contentValue = after.getPropertyValue('content')
            expect(contentValue).to.eq('none')
        })
        cy.clickSave();
    });

    it('enter a block quote', () => {
        cy.request('POST', '/admin/projects/proj1/subjects/subj1/skills/skill1', {
            projectId: 'proj1',
            subjectId: 'subj1',
            skillId: 'skill1',
            name: 'Skill 1',
            pointIncrement: '50',
            numPerformToCompletion: '5',
        });
        cy.visit('/administrator/projects/proj1/subjects/subj1/skills/skill1');

        cy.get('[data-cy="editSkillButton_skill1"]').click();
        cy.get(markdownInput).clear()
        cy.focused().type('this is some text');
        cy.focused().type('{selectAll}');
        cy.get('button').contains('F').click({force: true})
        cy.get('div.drop-down-item').contains('24px').click({force: true})
        cy.get('span').contains('this is some text')
          .should('have.attr', 'style', 'font-size: 24px;');
        cy.clickSave();
    });

});
