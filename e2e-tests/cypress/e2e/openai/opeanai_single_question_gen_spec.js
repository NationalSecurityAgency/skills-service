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
    textInputQuestion, errMsg, existingQuestionWelcomeMsg
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

    it('generate a new Multiple Choice question and change answers before saving', () => {
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

        validateAnswers([
            { text: 'Led Zeppelin', isCorrect: true },
            { text: 'Mozart', isCorrect: false },
            { text: 'Eminem', isCorrect: false },
            { text: 'Bach', isCorrect: false },
        ])

        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').click()
        validateAnswers([
            { text: 'Led Zeppelin', isCorrect: false },
            { text: 'Mozart', isCorrect: true },
            { text: 'Eminem', isCorrect: false },
            { text: 'Bach', isCorrect: false },
        ])

        cy.get('[data-cy="answer-3"] [data-cy="selectCorrectAnswer"]').click()
        validateAnswers([
            { text: 'Led Zeppelin', isCorrect: false },
            { text: 'Mozart', isCorrect: false },
            { text: 'Eminem', isCorrect: false },
            { text: 'Bach', isCorrect: true },
        ])

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"]').contains(selectSingleRockBandQuestion)
        validateSavedAnswers([
            { text: 'Led Zeppelin', isCorrect: false },
            { text: 'Mozart', isCorrect: false },
            { text: 'Eminem', isCorrect: false },
            { text: 'Bach', isCorrect: true },
        ])
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

    it('edit existing Multiple Choice question', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')

        cy.get('[data-cy="editQuestionButton_1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(existingQuestionWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="genQuestionTypeSelector"] [data-cy="selectionItem_SingleChoice"]')

        cy.intercept('POST', '/openai/stream/description', (req) => {
            const requestBody = req.body;
            req.reply();

            req.on('response', (res) => {
                expect(requestBody).to.have.property('instructions');
                const instructions = requestBody.instructions.replace(/\s+/g, ' ').trim();

                // Validate question
                const expectedQuestion = '## Existing Question ### Question: This is a question # 1';
                expect(instructions).to.include(expectedQuestion.replace(/\s+/g, ' ').trim());

                const expectedAnswers = [
                    { answer: "Question 1 - First Answer", isCorrect: true },
                    { answer: "Question 1 - Second Answer", isCorrect: false },
                    { answer: "Question 1 - Third Answer", isCorrect: false }
                ];

                // Parse the instructions to get the answers section
                const answersMatch = instructions.match(/### Answers:\s*(\[.*?\])/s);
                expect(answersMatch).to.exist;

                const actualAnswers = JSON.parse(answersMatch[1]);
                expect(actualAnswers).to.have.length(expectedAnswers.length);

                expectedAnswers.forEach((expected, index) => {
                    const actual = actualAnswers[index];
                    expect(actual).to.include({
                        answer: expected.answer,
                        isCorrect: expected.isCorrect,
                    });
                    // validate that displayOrder and multiPartAnswer properties do not exist
                    expect(actual).to.not.have.property('displayOrder');
                    expect(actual).to.not.have.property('multiPartAnswer');
                });
            });
        }).as('genDescription');
        cy.get('[data-cy="instructionsInput"]').type('Great rock bands{enter}')
        cy.wait('@genDescription')
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

        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').should('be.visible')
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').contains(selectSingleRockBandQuestion)

        cy.get('[data-cy="answerTypeSelector"] [data-cy="selectionItem_SingleChoice"]')
        const expectedAnswers = [
            { text: 'Led Zeppelin', isCorrect: true },
            { text: 'Mozart', isCorrect: false },
            { text: 'Eminem', isCorrect: false },
            { text: 'Bach', isCorrect: false },
        ]

        validateAnswers(expectedAnswers)

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.wait(1000)
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"]').contains(selectSingleRockBandQuestion)
        validateSavedAnswers(expectedAnswers)
    })

    it('edit existing Multiple Answers question', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);
        cy.createQuizMultipleChoiceQuestionDef(1, 1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')

        cy.get('[data-cy="editQuestionButton_1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(existingQuestionWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="genQuestionTypeSelector"] [data-cy="selectionItem_MultipleChoice"]')

        cy.intercept('POST', '/openai/stream/description', (req) => {
            const requestBody = req.body;
            req.reply();

            req.on('response', (res) => {
                expect(requestBody).to.have.property('instructions');
                const instructions = requestBody.instructions.replace(/\s+/g, ' ').trim();

                // Validate question
                const expectedQuestion = '## Existing Question ### Question: This is a question # 1';
                expect(instructions).to.include(expectedQuestion.replace(/\s+/g, ' ').trim());

                const expectedAnswers = [
                    { answer: "First Answer", isCorrect: true },
                    { answer: "Second Answer", isCorrect: false },
                    { answer: "Third Answer", isCorrect: true },
                    { answer: "Fourth Answer", isCorrect: false },
                ];

                // Parse the instructions to get the answers section
                const answersMatch = instructions.match(/### Answers:\s*(\[.*?\])/s);
                expect(answersMatch).to.exist;

                const actualAnswers = JSON.parse(answersMatch[1]);
                expect(actualAnswers).to.have.length(expectedAnswers.length);

                expectedAnswers.forEach((expected, index) => {
                    const actual = actualAnswers[index];
                    expect(actual).to.include({
                        answer: expected.answer,
                        isCorrect: expected.isCorrect,
                    });
                    // validate that displayOrder and multiPartAnswer properties do not exist
                    expect(actual).to.not.have.property('displayOrder');
                    expect(actual).to.not.have.property('multiPartAnswer');
                });
            });
        }).as('genDescription');
        cy.get('[data-cy="instructionsInput"]').type('Great rock bands{enter}')
        cy.wait('@genDescription')
        cy.get('[data-cy="userMsg-1"]').contains('Great rock bands')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(selectRockBandsQuestion)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Led Zeppelin`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Norah Jones`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="generatedSegmentNotes"]').contains("Here is what was changed")
        cy.get('[data-cy="generatedSegmentNotes"]').contains("Change 1")
        cy.get('[data-cy="generatedSegmentNotes"]').contains("Change 2")
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')

        cy.get('[data-cy="instructionsInput"]').should('have.focus')
        cy.get('[data-cy="useGenValueBtn-2"]').click()
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')

        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').should('be.visible')
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').contains(selectRockBandsQuestion)

        cy.get('[data-cy="answerTypeSelector"] [data-cy="selectionItem_MultipleChoice"]')
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
    })

    it('edit existing Text Input question', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1);

        cy.visit('/administrator/quizzes/quiz1')
        cy.get('@getConfig')

        cy.get('[data-cy="editQuestionButton_1"]').click()
        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(existingQuestionWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="genQuestionTypeSelector"] [data-cy="selectionItem_TextInput"]')

        cy.intercept('POST', '/openai/stream/description', (req) => {
            const requestBody = req.body;
            req.reply();

            req.on('response', (res) => {
                expect(requestBody).to.have.property('instructions');
                const instructions = requestBody.instructions.replace(/\s+/g, ' ').trim();

                // Validate question
                const expectedQuestion = '## Existing Question ### Question: This is a question # 1';
                expect(instructions).to.include(expectedQuestion.replace(/\s+/g, ' ').trim());

                const expectedAnswers = [];

                // Parse the instructions to get the answers section
                const answersMatch = instructions.match(/### Answers:\s*(\[.*?\])/s);
                expect(answersMatch).to.exist;

                const actualAnswers = JSON.parse(answersMatch[1]);
                expect(actualAnswers).to.have.length(expectedAnswers.length);

                expectedAnswers.forEach((expected, index) => {
                    const actual = actualAnswers[index];
                    expect(actual).to.include({
                        answer: expected.answer,
                        isCorrect: expected.isCorrect,
                    });
                    // validate that displayOrder and multiPartAnswer properties do not exist
                    expect(actual).to.not.have.property('displayOrder');
                    expect(actual).to.not.have.property('multiPartAnswer');
                });
            });
        }).as('genDescription');
        cy.get('[data-cy="instructionsInput"]').type('Great rock bands{enter}')
        cy.wait('@genDescription')
        cy.get('[data-cy="userMsg-1"]').contains('Great rock bands')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(textInputQuestion)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"] [data-cy="textAreaPlaceHolder"]')
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')

        cy.get('[data-cy="instructionsInput"]').should('have.focus')
        cy.get('[data-cy="useGenValueBtn-2"]').click()
        cy.get('[data-cy="useGenValueBtn-2"]').should('not.exist')

        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').should('be.visible')
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"]').contains(textInputQuestion)

        cy.get('[data-cy="answerTypeSelector"] [data-cy="selectionItem_TextInput"]')
        cy.wait(1000)
        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="markdownEditorInput"]').should('not.exist')

        cy.get('[data-cy="questionDisplayCard-1"]').contains(textInputQuestion)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="textAreaPlaceHolder"]')
    })

    it('use any information that was already entered', () => {
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
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="markdownEditorInput"]').type('What is the capital of France?')

        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()

        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('Berlin')
        cy.get('[data-cy="answer-3"] [data-cy="answerText"]').type('Paris')

        cy.get('[data-cy="answer-3"] [data-cy="selectCorrectAnswer"]').click()

        cy.get('[data-cy="aiButton"]').click()
        cy.get('[data-cy="aiMsg-0"]').contains(existingQuestionWelcomeMsg)
        cy.get('[data-cy="userMsg-1"]').should('not.exist')

        cy.get('[data-cy="genQuestionTypeSelector"] [data-cy="selectionItem_SingleChoice"]')

        cy.intercept('POST', '/openai/stream/description', (req) => {
            const requestBody = req.body;
            req.reply();

            req.on('response', (res) => {
                expect(requestBody).to.have.property('instructions');
                const instructions = requestBody.instructions.replace(/\s+/g, ' ').trim();

                const expectedTask = '# Task: Update an existing SingleChoice question'
                expect(instructions).to.include(expectedTask.replace(/\s+/g, ' ').trim());

                const expectedQuestion = '## Existing Question ### Question: What is the capital of France?';
                expect(instructions).to.include(expectedQuestion.replace(/\s+/g, ' ').trim());

                const expectedAnswers = [
                    { answer: "Berlin", isCorrect: false },
                    { answer: "Paris", isCorrect: true },
                ];

                // Parse the instructions to get the answers section
                const answersMatch = instructions.match(/### Answers:\s*(\[.*?\])/s);
                expect(answersMatch).to.exist;

                const actualAnswers = JSON.parse(answersMatch[1]);
                expect(actualAnswers).to.have.length(expectedAnswers.length);

                expectedAnswers.forEach((expected, index) => {
                    const actual = actualAnswers[index];
                    expect(actual).to.include({
                        answer: expected.answer,
                        isCorrect: expected.isCorrect,
                    });
                    // validate that displayOrder and multiPartAnswer properties do not exist
                    expect(actual).to.not.have.property('displayOrder');
                    expect(actual).to.not.have.property('multiPartAnswer');
                });
            });
        }).as('genDescription');
        cy.get('[data-cy="instructionsInput"]').type('Great rock bands{enter}')
        cy.wait('@genDescription')
        cy.get('[data-cy="userMsg-1"]').contains('Great rock bands')
        cy.get('[data-cy="aiMsg-2"] [data-cy="origSegment"]').contains(gotStartedMsg)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedSegment"]').contains(selectSingleRockBandQuestion)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Led Zeppelin`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="generatedAnswers"]').contains(`Bach`)
        cy.get('[data-cy="aiMsg-2"] [data-cy="finalSegment"]').contains(completedMsg)
        cy.get('[data-cy="useGenValueBtn-2"]').should('be.enabled')

        cy.get('[data-cy="instructionsInput"]').should('have.focus')
    })
});


