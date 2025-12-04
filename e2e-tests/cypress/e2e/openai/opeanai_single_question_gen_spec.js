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

import {
    newSingleQuestionWelcomeMsg,
    gotStartedMsg,
    completedMsg,
    selectRockBandsQuestion,
    selectSingleRockBandQuestion,
    textInputQuestion, errMsg
}
    from './openai_helper_commands'

describe('Generate Single Question Tests', () => {

    const validateAnswers = (expected) => {
        expected.forEach((answer, index) => {
            cy.get(`[data-cy="answer-${index}"] [data-pc-name="inputtext"]`).should('have.value', answer.text)
            answer.isCorrect ? cy.get(`[data-cy="answer-${index}"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]`) : cy.get(`[data-cy="answer-${index}"] [data-cy="selectCorrectAnswer"] [data-cy="notSelected"]`)
        })
    }
    const validateSavedAnswers = (expected) => {
        expected.forEach((answer, index) => {
            cy.get(`[data-cy="answer-${index}_displayText"]`).should('have.text', answer.text)
            answer.isCorrect ? cy.get(`[data-cy="answerDisplay-${index}"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]`) : cy.get(`[data-cy="answerDisplay-${index}"] [data-cy="selectCorrectAnswer"] [data-cy="notSelected"]`)
        })
    }

    it('generate a new Multiple Answers question', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newSingleQuestionWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').type('Great rock bands{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Great rock bands')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(selectRockBandsQuestion)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Led Zeppelin`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Norah Jones`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')

        cy.get('[data-cy="instructionsInput"]').should('have.focus')
        cy.get('[data-cy="useGenValueBtn-2"]').click()
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')

        cy.get('[data-cy="markdownEditorInput"]').contains(selectRockBandsQuestion)

        const expectedAnswers = [
            { text: 'Led Zeppelin', isCorrect: true },
            { text: 'Daft Punk', isCorrect: false },
            { text: 'Pink Floyd', isCorrect: true },
            { text: 'The Beatles', isCorrect: true },
            { text: 'Norah Jones', isCorrect: false },
        ]

        validateAnswers(expectedAnswers)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"]').contains(selectRockBandsQuestion)
        validateSavedAnswers(expectedAnswers)
    });

    it('generate a new Multiple Choice question', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newSingleQuestionWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="genQuestionTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="instructionsInput"]').type('Great rock bands{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Great rock bands')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(selectSingleRockBandQuestion)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Led Zeppelin`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Bach`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')

        cy.get('[data-cy="instructionsInput"]').should('have.focus')
        cy.get('[data-cy="useGenValueBtn-2"]').click()
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')

        cy.get('[data-cy="markdownEditorInput"]').contains(selectSingleRockBandQuestion)

        cy.get('[data-cy="answerTypeSelector"] [data-cy="selectionItem_SingleChoice"]')
        const expectedAnswers = [
            { text: 'Led Zeppelin', isCorrect: true },
            { text: 'Mozart', isCorrect: false },
            { text: 'Eminem', isCorrect: false },
            { text: 'Bach', isCorrect: false },
        ]

        validateAnswers(expectedAnswers)

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"]').contains(selectSingleRockBandQuestion)
        validateSavedAnswers(expectedAnswers)
    })

    it('generate a new Text Input question', () => {
        cy.viewport(1280, 1400)
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newSingleQuestionWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="genQuestionTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_TextInput"]').click()
        cy.get('[data-cy="instructionsInput"]').type('Great rock bands{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Great rock bands')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(textInputQuestion)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"] [data-cy="textAreaPlaceHolder"]')
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')

        cy.get('[data-cy="instructionsInput"]').should('have.focus')
        cy.get('[data-cy="useGenValueBtn-2"]').click()
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')

        cy.get('[data-cy="markdownEditorInput"]').contains(textInputQuestion)
        cy.get('[data-cy="answerTypeSelector"] [data-cy="selectionItem_TextInput"]')

        cy.wait(1000)
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"]').contains(textInputQuestion)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="textAreaPlaceHolder"]')
    })

    it('gracefully handle the error when answers are sent with bad json', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newSingleQuestionWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')
        cy.get('[data-cy="instructionsInput"]').type('Great rock bands - Bad JSON{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Great rock bands')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(selectRockBandsQuestion)
        cy.get('[data-cy="finalSegment"]').contains(errMsg)
    });

    it('generate a new Multiple Choice question with a prefix', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.addPrefixToInvalidParagraphsOptions = '(A) ,(B) ,(C) ,(D) ';
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')

        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(newSingleQuestionWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="instructionsInput"]').type('Great jabberwocky rock bands{enter}')
        cy.get('[data-cy="userMsg-1"]').contains('Great jabberwocky rock bands')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains('Select jabberwocky rock bands?')
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Led Zeppelin`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Norah Jones`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')

        cy.get('[data-cy="instructionsInput"]').should('have.focus')

        cy.get('[data-cy="prefixSelect"]').click()
        const letters = ['A', 'B', 'C', 'D'];
        for (const letter of letters) {
            cy.get(`[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(${letter}) "]`);
        }

        cy.get('[data-pc-section="overlay"] [data-pc-section="list"] [aria-label="(B) "]').click()

        cy.get('[data-cy="addPrefixBtn"]').click()

        cy.get('[data-cy="markdownEditorInput"]').contains('(B) Select jabberwocky rock bands?')

        const expectedAnswers = [
            { text: 'Led Zeppelin', isCorrect: true },
            { text: 'Daft Punk', isCorrect: false },
            { text: 'Pink Floyd', isCorrect: true },
            { text: 'The Beatles', isCorrect: true },
            { text: 'Norah Jones', isCorrect: false },
        ]

        validateAnswers(expectedAnswers)
    })

});


