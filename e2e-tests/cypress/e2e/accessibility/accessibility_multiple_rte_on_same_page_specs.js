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

        Cypress.Commands.add("getToRte", (tabIndexValues) => {
            cy.visit('/progress-and-rankings/quizzes/quiz1');
            cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
            cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
            cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
            cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

            cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
            cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

            cy.get('[data-cy="startQuizAttempt"]').click()
        })

        Cypress.Commands.add("tabToRte", (tabIndexValues) => {
           return cy.get('[data-cy="question_1"] [data-cy="editorFeaturesUrl"]').tab()
        })

        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)
    });

    const rte1Selector = '[data-cy="question_1"] [data-cy="markdownEditorInput"]'
    const rte2Selector = '[data-cy="question_2"] [data-cy="markdownEditorInput"]'
    const rte3Selector = '[data-cy="question_3"] [data-cy="markdownEditorInput"]'
    const rte2ContentsSelector = `${rte2Selector} .toastui-editor-contents`

    it('header selector - use keyboard to select', () => {
        cy.getToRte()
        cy.tabToRte().type('{enter}{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus')
        cy.validateHeaderTabIndex(['0', '-1', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus').type('{enter}My Header')
        cy.get(`${rte2ContentsSelector} h1`).should('have.text', 'My Header');
    });

    it('header selector - use keyboard to select - using keyboard shortcut', () => {
        cy.getToRte()

        cy.get(rte2Selector).type('{ctrl+alt+t}')
        cy.tabToRte().type('{downArrow}')
        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus')
        cy.validateHeaderTabIndex(['0', '-1', '-1', '-1', '-1', '-1', '-1'])

        cy.get('.toastui-editor-popup-body #headerChoicesId [data-level="1"]').should('have.focus').type('{enter}My Header')
        cy.get(`${rte2ContentsSelector} h1`).should('have.text', 'My Header');
    });

    it('header selector - ability to navigate down using keyboard', () => {
        cy.getToRte()

        cy.tabToRte().type('{enter}{downArrow}')
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
        cy.get(`${rte2Selector} .toastui-editor-contents h2`).should('have.text', 'My Header');
    });

    it('header selector - ability to navigate up using keyboard', () => {
        cy.getToRte()

        cy.tabToRte().type('{enter}{downArrow}')
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
        cy.get(`${rte2ContentsSelector} h6`).should('have.text', 'My Header');
    });

    it('toolbar custom aria-labels', () => {
        cy.getToRte()

        cy.get(`${rte1Selector} .toastui-editor-defaultUI-toolbar [aria-label="Font Size"][type="button"]`).should('have.text', 'F')
        cy.get(`${rte1Selector} .toastui-editor-defaultUI-toolbar .more[aria-label="More Toolbar Controls"][type="button"]`)

        cy.get(`${rte2Selector} .toastui-editor-defaultUI-toolbar [aria-label="Font Size"][type="button"]`).should('have.text', 'F')
        cy.get(`${rte2Selector} .toastui-editor-defaultUI-toolbar .more[aria-label="More Toolbar Controls"][type="button"]`)

        cy.get(`${rte3Selector} .toastui-editor-defaultUI-toolbar [aria-label="Font Size"][type="button"]`).should('have.text', 'F')
        cy.get(`${rte3Selector} .toastui-editor-defaultUI-toolbar .more[aria-label="More Toolbar Controls"][type="button"]`)
    });

    it('modify font size via keyboard - use keyboard mapping', () => {
        cy.getToRte()

        cy.typeInMarkdownEditor(rte2Selector, 'value{selectAll}{ctrl+alt+s}')
        cy.get(`.toastui-editor-popup-body .size-input`).should('have.focus').type('22{enter}')
        cy.get(`${rte2Selector} .toastui-editor-contents [style="font-size: 22px;"]`).should('have.text', 'value');
    });

    it('insert link using keyboard - use keyboard mapping', () => {
        cy.getToRte()

        cy.typeInMarkdownEditor(rte2Selector, '{ctrl+alt+r}')
        cy.get(`${rte2Selector} .toastui-editor-popup-body #toastuiLinkUrlInput`).should('have.focus')
        cy.get(`${rte2Selector} .toastui-editor-popup-body #toastuiLinkUrlInput`).type('/static/img/skilltree_logo_v1.png')
        cy.get(`${rte2Selector} .toastui-editor-popup-body #toastuiLinkUrlInput`).tab()
        cy.get(`${rte2Selector} .toastui-editor-popup-body #toastuiLinkTextInput`).should('have.focus')
        cy.get(`${rte2Selector} .toastui-editor-popup-body #toastuiLinkTextInput`).type('Logo')
        cy.get(`${rte2Selector} .toastui-editor-popup-body #toastuiLinkTextInput`).tab()
        cy.get(`${rte2Selector} .toastui-editor-popup-body .toastui-editor-close-button`).should('have.focus')
        cy.get(`${rte2Selector} .toastui-editor-popup-body .toastui-editor-close-button`).tab()
        cy.get(`${rte2Selector} .toastui-editor-popup-body .toastui-editor-ok-button`).should('have.focus')
        cy.get(`${rte2Selector} .toastui-editor-popup-body .toastui-editor-ok-button`).click()
        cy.get(`${rte2Selector} .toastui-editor-contents a[href="/static/img/skilltree_logo_v1.png"]`).should('have.text', 'Logo')
    });

});
