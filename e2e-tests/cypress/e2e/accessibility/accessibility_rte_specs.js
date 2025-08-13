/*
 * Copyright 2024 SkillTree
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

describe('Accessibility Rich Text Editor Tests', () => {

    beforeEach(() => {
        Cypress.Commands.add("validateHeaderTabIndex", (tabIndexValues) => {
            cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.attr', 'tabindex', tabIndexValues[0])
            cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="2"]').should('have.attr', 'tabindex', tabIndexValues[1])
            cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="3"]').should('have.attr', 'tabindex', tabIndexValues[2])
            cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="4"]').should('have.attr', 'tabindex', tabIndexValues[3])
            cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="5"]').should('have.attr', 'tabindex', tabIndexValues[4])
            cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="6"]').should('have.attr', 'tabindex', tabIndexValues[5])
            cy.get('.toastui-editor-popup-body #headerChoicesId [data-type="Paragraph"]').should('have.attr', 'tabindex', tabIndexValues[6])
        })

    });

    it('header selector - use keyboard to select', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=newProjectButton]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')

        cy.get('[data-cy="projectName"]').tab().tab().type('{enter}{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus')
        cy.validateHeaderTabIndex(['0', '-1', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus').type('{enter}My Header')
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents h1').should('have.text', 'My Header');
    });

    it('header selector - use keyboard to select - using keyboard shortcut', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=newProjectButton]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')

        cy.get('[data-cy="markdownEditorInput"]').type('{ctrl+alt+t}')
        cy.get('#headerButtonId').should('have.focus').type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus')
        cy.validateHeaderTabIndex(['0', '-1', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus').type('{enter}My Header')
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents h1').should('have.text', 'My Header');
    });

    it('header selector - ability to navigate down using keyboard', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=newProjectButton]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')

        cy.get('[data-cy="projectName"]').tab().tab().type('{enter}{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus')
        cy.validateHeaderTabIndex(['0', '-1', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus').type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="2"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '0', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="2"]').should('have.focus').type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="3"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '0', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="3"]').should('have.focus').type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="4"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '0', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="4"]').should('have.focus').type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="5"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '-1', '0', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="5"]').should('have.focus').type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="6"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '-1', '-1', '0', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="6"]').should('have.focus').type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-type="Paragraph"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '-1', '-1', '-1', '0'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-type="Paragraph"]').should('have.focus').type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus')
        cy.validateHeaderTabIndex(['0', '-1', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus').type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="2"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '0', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="2"]').should('have.focus').type('{enter}My Header')
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents h2').should('have.text', 'My Header');
    });

    it('header selector - ability to navigate up using keyboard', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=newProjectButton]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')

        cy.get('[data-cy="projectName"]').tab().tab().type('{enter}{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus')
        cy.validateHeaderTabIndex(['0', '-1', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus').type('{upArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-type="Paragraph"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '-1', '-1', '-1', '0'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-type="Paragraph"][data-type="Paragraph"]').should('have.focus').type('{upArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="6"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '-1', '-1', '0', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="6"]').should('have.focus').type('{upArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="5"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '-1', '0', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="5"]').should('have.focus').type('{upArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="4"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '0', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="4"]').should('have.focus').type('{upArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="3"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '0', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="3"]').should('have.focus').type('{upArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="2"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '0', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="2"]').should('have.focus').type('{upArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus')
        cy.validateHeaderTabIndex(['0', '-1', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus').type('{upArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-type="Paragraph"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '-1', '-1', '-1', '0'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-type="Paragraph"][data-type="Paragraph"]').should('have.focus').type('{upArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="6"]').should('have.focus')
        cy.validateHeaderTabIndex(['-1', '-1', '-1', '-1', '-1', '0', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="6"]').should('have.focus').type('{enter}My Header')
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents h6').should('have.text', 'My Header');
    });

    it('toolbar custom aria-labels', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=newProjectButton]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')

        cy.get('.toastui-editor-defaultUI-toolbar [aria-label="Font Size"][type="button"]').should('have.text', 'F')
        cy.get('.toastui-editor-defaultUI-toolbar .more[aria-label="More Toolbar Controls"][type="button"]')
    });

    it('modify font size via keyboard - use keyboard mapping', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=newProjectButton]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', 'value{selectAll}{ctrl+alt+s}')
        cy.get('.toastui-editor-popup-body .size-input').should('have.focus').type('22{enter}')
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents [style="font-size: 22px;"]').should('have.text', 'value');
    });

    it('ability to insert image using keyboard - use keyboard mapping', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=newProjectButton]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{ctrl+alt+i}')
        cy.get('.toastui-editor-popup-body .toastui-editor-tabs .tab-item.active').should('have.focus');
        cy.get('.toastui-editor-popup-body .toastui-editor-tabs .tab-item.active').tab()
        cy.get('.toastui-editor-popup-body .toastui-editor-file-select-button').should('have.focus')
        cy.get('.toastui-editor-popup-body .toastui-editor-file-select-button').tab()
        cy.get('.toastui-editor-popup-body #toastuiAltTextInput').should('have.focus')
        cy.get('.toastui-editor-popup-body #toastuiAltTextInput').tab()
        cy.get('.toastui-editor-popup-body .toastui-editor-close-button').should('have.focus')
        cy.get('.toastui-editor-popup-body .toastui-editor-close-button').tab()
        cy.get('.toastui-editor-popup-body .toastui-editor-ok-button').should('have.focus')
    });

    it('insert image via URL using keyboard - use keyboard mapping', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=newProjectButton]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{ctrl+alt+i}')
        cy.get('.toastui-editor-popup-body .toastui-editor-tabs .tab-item.active').should('have.focus').should('have.text', 'File')
        cy.get('.toastui-editor-popup-body .toastui-editor-tabs .tab-item.active').tab()
        cy.get('.toastui-editor-popup-body .toastui-editor-tabs .tab-item.active').type('{rightArrow}')
        cy.get('.toastui-editor-popup-body .toastui-editor-tabs .tab-item.active').should('have.focus').should('have.text', 'URL')
        cy.get('.toastui-editor-popup-body .toastui-editor-tabs .tab-item.active').tab()
        cy.get('.toastui-editor-popup-body #toastuiImageUrlInput').should('have.focus')
        cy.get('.toastui-editor-popup-body #toastuiImageUrlInput').type('/static/img/skilltree_logo_v1.png')
        cy.get('.toastui-editor-popup-body #toastuiImageUrlInput').tab()
        cy.get('.toastui-editor-popup-body #toastuiAltTextInput').should('have.focus')
        cy.get('.toastui-editor-popup-body #toastuiAltTextInput').type('logo')
        cy.get('.toastui-editor-popup-body #toastuiAltTextInput').tab()
        cy.get('.toastui-editor-popup-body .toastui-editor-close-button').should('have.focus')
        cy.get('.toastui-editor-popup-body .toastui-editor-close-button').tab()
        cy.get('.toastui-editor-popup-body .toastui-editor-ok-button').should('have.focus')
        cy.get('.toastui-editor-popup-body .toastui-editor-ok-button').click()
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents img[alt="logo"]')
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents img[src="/static/img/skilltree_logo_v1.png"]')
    });

    it('insert link using keyboard - use keyboard mapping', () => {
        cy.visit('/administrator/');
        cy.get('[data-cy=newProjectButton]').click()
        cy.get('[data-cy="projectName"]').should('have.focus')

        cy.typeInMarkdownEditor('[data-cy="markdownEditorInput"]', '{ctrl+alt+r}')
        cy.get('.toastui-editor-popup-body #toastuiLinkUrlInput').should('have.focus')
        cy.get('.toastui-editor-popup-body #toastuiLinkUrlInput').type('/static/img/skilltree_logo_v1.png')
        cy.get('.toastui-editor-popup-body #toastuiLinkUrlInput').tab()
        cy.get('.toastui-editor-popup-body #toastuiLinkTextInput').should('have.focus')
        cy.get('.toastui-editor-popup-body #toastuiLinkTextInput').type('Logo')
        cy.get('.toastui-editor-popup-body #toastuiLinkTextInput').tab()
        cy.get('.toastui-editor-popup-body .toastui-editor-close-button').should('have.focus')
        cy.get('.toastui-editor-popup-body .toastui-editor-close-button').tab()
        cy.get('.toastui-editor-popup-body .toastui-editor-ok-button').should('have.focus')
        cy.get('.toastui-editor-popup-body .toastui-editor-ok-button').click()
        cy.get('[data-cy="markdownEditorInput"] .toastui-editor-contents a[href="/static/img/skilltree_logo_v1.png"]').should('have.text', 'Logo')
    });

});
