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

describe('Survey Text Responses Tests', () => {

    const textInput = (aIndex) => `[data-cy="question_1"] [data-cy="customAnswerText"][id="questions[0].quizAnswers[${aIndex}].answerText"]`
    const textError = (aIndex) => `[data-cy="questions[0].quizAnswers[${aIndex}].answerTextError"]`
    const answerRow = (aNum) => `[data-cy="question_1"] [data-cy="answer_${aNum}"]`
    const recoveryMessage = 'We couldn\'t save your response. Copy your text responses before refreshing the page. After refreshing, re-enter any missing text before completing the survey.'

    const createTextResponseSurvey = (questionType) => {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1, {
            questionType,
            answers: [
                { answer: 'First', isCorrect: false },
                { answer: 'Second', isCorrect: false },
                { answer: 'Other reason', isCorrect: false, allowCustomText: true },
                { answer: 'Another reason', isCorrect: false, allowCustomText: true },
            ],
        });
    }

    const startSurvey = () => {
        cy.visit('/progress-and-rankings/quizzes/quiz1')
        cy.get('[data-cy="startQuizAttempt"]').click()
    }

    // Names each answer save so tests can wait for a specific one: selection changes are selectSave or
    // deselectSave, text saves use the alias given for their text, or textSave.
    // Returns the list of text responses sent, to check that a text was never sent.
    const interceptSaves = (textAliases = {}, handlers = {}) => {
        const sentTexts = []
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*', (req) => {
            const { isSelected, answerText } = req.body
            if (typeof answerText === 'string') {
                sentTexts.push(answerText)
                req.alias = textAliases[answerText] || 'textSave'
            } else {
                req.alias = isSelected ? 'selectSave' : 'deselectSave'
            }
            handlers[answerText]?.(req)
        })
        return sentTexts
    }
    const checkNeverSent = (sentTexts, text) => {
        cy.wrap(sentTexts).should('not.include', text)
    }

    // the learner's own attempt history and the admin's view of the same attempt
    const checkHistory = (expectedAnswers) => {
        const checkAnswers = () => {
            Object.entries(expectedAnswers).forEach(([aIndex, text]) => {
                cy.get(`[data-cy="questionDisplayCard-1"] [data-cy="answer-${aIndex}_displayText"]`).should('have.text', text)
            })
        }
        cy.visit('/progress-and-rankings/my-quiz-attempts');
        cy.get('[data-cy="viewQuizAttempt"]').first().click()
        checkAnswers()
        cy.visit('/administrator/quizzes/quiz1/runs');
        cy.get('[data-cy="row0-viewRun"]').click()
        checkAnswers()
    }

    const questionTypes = ['SingleChoice', 'MultipleChoice']
    questionTypes.forEach((questionType) => {
        const isSingle = questionType === 'SingleChoice'

        it(`configure, edit and show text responses on the question card [${questionType}]`, () => {
            cy.createSurveyDef(1);
            cy.visit('/administrator/quizzes/quiz1');
            cy.get('[data-cy="btn_Questions"]').click()
            cy.typeQuestion('Why did you pick this course?')
            cy.get('[data-cy="answerTypeSelector"]').click()
            cy.get(`[data-cy="selectionItem_${questionType}"]`).last().click()
            cy.get('[data-cy="textResponseColumnHeader"]').contains('Text Response')
            cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('Required')
            cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('Interest')
            cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()

            // an answer that allows a text response needs a label
            cy.get('[data-cy="checkbox_answers[2].allowCustomText"]').click()
            cy.get('[data-cy="answersError"]').contains('Answers that allow a text response must have text')
            cy.get('[data-cy="saveDialogBtn"]').should('be.disabled')
            cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('Other reason')
            cy.get('[data-cy="answersError"]').should('not.be.visible')
            cy.clickSaveDialogBtn()

            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').should('have.text', 'Other reason')
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_allowsTextResponse"]').contains('Text response')
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_allowsTextResponse"]').should('not.exist')
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_allowsTextResponse"]').should('not.exist')

            // enable on another answer and disable on the first one
            cy.get('[data-cy="editQuestionButton_1"]').click()
            cy.get('[data-cy="checkbox_answers[2].allowCustomText"] input').should('be.checked')
            cy.get('[data-cy="checkbox_answers[0].allowCustomText"] input').should('not.be.checked')
            cy.get('[data-cy="checkbox_answers[1].allowCustomText"]').click()
            cy.get('[data-cy="checkbox_answers[2].allowCustomText"]').click()
            cy.clickSaveDialogBtn()
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_allowsTextResponse"]').contains('Text response')
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_allowsTextResponse"]').should('not.exist')

            cy.visit('/administrator/quizzes/quiz1');
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_allowsTextResponse"]').contains('Text response')
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_allowsTextResponse"]').should('not.exist')
        });

        it(`text input focus, required and length validation [${questionType}]`, () => {
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(textInput(2)).should('not.exist')
            cy.get(answerRow(3)).click()
            cy.get(textInput(2)).should('have.focus')
            cy.get(textInput(2)).type('two words here')
            cy.get(textInput(2)).should('have.value', 'two words here')
            cy.get(`${answerRow(3)} [data-cy="selected_true"]`)

            cy.get(textInput(2)).clear()
            cy.get(textError(2)).contains('Text response is required')
            cy.get(textInput(2)).type('x'.repeat(1001), { delay: 0 })
            cy.get(textError(2)).contains('Text response must not exceed 1,000 characters')
            cy.get(textInput(2)).type('{backspace}')
            cy.get(textError(2)).should('not.contain', 'must not exceed')

            // keyboard selection also focuses the input
            cy.get(answerRow(4)).focus().type(' ')
            cy.get(textInput(3)).should('have.focus')
        });

        it(`spaces at either end count toward the length limit [${questionType}]`, () => {
            interceptSaves({ [` ${'x'.repeat(999)}`]: 'atLimitSave' })
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(answerRow(3)).click()
            cy.wait('@selectSave')
            // exactly at the limit w/ a leading space: accepted by the browser and the server
            cy.get(textInput(2)).type(` ${'x'.repeat(999)}`, { delay: 0 })
            cy.wait('@atLimitSave').its('response.statusCode').should('eq', 200)
            cy.get(textError(2)).should('not.contain', 'must not exceed')

            // one character over because of a trailing space: rejected in the browser, as on the server
            cy.get(textInput(2)).type(' ')
            cy.get(textError(2)).contains('Text response must not exceed 1,000 characters')
            cy.get(textInput(2)).type('{backspace}')
            cy.get(textError(2)).should('not.contain', 'must not exceed')

            // one character over because of a leading space
            cy.get(textInput(2)).clear()
            cy.get(textInput(2)).type(` ${'x'.repeat(1000)}`, { delay: 0 })
            cy.get(textError(2)).contains('Text response must not exceed 1,000 characters')
        });

        it(`server validation errors are shown on the text input [${questionType}]`, () => {
            interceptSaves()
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(answerRow(3)).click()
            cy.wait('@selectSave')
            cy.get(textInput(2)).type('jabberwocky')
            cy.wait('@textSave').its('response.statusCode').should('eq', 400)
            cy.get(textError(2)).contains('answerText is invalid: paragraphs may not contain jabberwocky')
        });

        it(`switch options, clear, resume and complete [${questionType}]`, () => {
            interceptSaves({ '': 'clearedSave' })
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(answerRow(1)).click()
            cy.get(answerRow(3)).click()
            cy.get(textInput(2)).type('First text')
            cy.get(answerRow(4)).click()
            cy.get(textInput(3)).type('Second text')
            if (isSingle) {
                cy.get(textInput(2)).should('not.exist')
                cy.get(`${answerRow(1)} [data-cy="selected_false"]`)
            } else {
                cy.get(textInput(2)).should('have.value', 'First text')
                cy.get(`${answerRow(1)} [data-cy="selected_true"]`)
            }

            // a cleared response stays selected and blocks completion
            cy.get(textInput(3)).clear()
            cy.wait('@clearedSave')
            cy.reload()
            cy.get(`${answerRow(4)} [data-cy="selected_true"]`)
            cy.get(textInput(3)).should('have.value', '')
            if (isSingle) {
                cy.get(`${answerRow(3)} [data-cy="selected_false"]`)
            } else {
                cy.get(textInput(2)).should('have.value', 'First text')
            }
            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get(textError(3)).contains('Text response is required')
            cy.get('[data-cy="surveyCompletion"]').should('not.exist')

            // typing right before completion is saved before the attempt completes
            cy.get(textInput(3)).type('Final text')
            cy.clickCompleteQuizBtn()
            cy.get('[data-cy="surveyCompletion"]').contains('Thank you for taking the time to complete the survey')

            checkHistory(isSingle ?
                { 0: 'First', 1: 'Second', 2: 'Other reason', 3: 'Another reason: Final text' } :
                { 0: 'First', 1: 'Second', 2: 'Other reason: First text', 3: 'Another reason: Final text' })
        });

        it(`deselecting right after typing does not restore the option [${questionType}]`, () => {
            const sentTexts = interceptSaves()
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(answerRow(3)).click()
            cy.wait('@selectSave')
            cy.get(textInput(2)).type('typed then removed')
            cy.get(answerRow(3)).click()
            cy.get(textInput(2)).should('not.exist')
            cy.wait('@deselectSave')
            cy.reload()
            cy.get(`${answerRow(3)} [data-cy="selected_false"]`)
            cy.get(textInput(2)).should('not.exist')
            if (isSingle) {
                // a newer single choice selection is not overridden by a pending text save
                cy.get(answerRow(3)).click()
                cy.wait('@selectSave')
                cy.get(textInput(2)).type('pending text')
                cy.get(answerRow(2)).click()
                cy.wait('@selectSave')
                cy.reload()
                cy.get(`${answerRow(2)} [data-cy="selected_true"]`)
                cy.get(`${answerRow(3)} [data-cy="selected_false"]`)
                checkNeverSent(sentTexts, 'pending text')
            }
            checkNeverSent(sentTexts, 'typed then removed')
        });

        it(`an older text save does not restore a cleared response [${questionType}]`, () => {
            // the first text save is slow; a second edit finishes its typing delay while it is still in flight
            let slowSaveSent = false
            const sentTexts = interceptSaves({ 'slow text': 'slowSave' }, {
                'slow text': (req) => {
                    slowSaveSent = true
                    req.continue((res) => {
                        res.setDelay(2000)
                    })
                },
            })
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(answerRow(3)).click()
            cy.wait('@selectSave')
            cy.clock(Date.now(), ['setTimeout', 'clearTimeout'])
            cy.get(textInput(2)).type('slow text')
            cy.tick(1000)
            cy.wrap(null).should(() => expect(slowSaveSent).to.equal(true))
            cy.get(textInput(2)).type(' and more')
            cy.tick(1000)
            // while both edits wait, the respondent clears the text by switching away and back
            if (isSingle) {
                cy.get(answerRow(1)).click()
            } else {
                cy.get(answerRow(3)).click()
            }
            cy.get(answerRow(3)).click()
            cy.get(textInput(2)).should('have.value', '')
            cy.clock().then((clock) => clock.restore())
            cy.wait('@slowSave')
            if (isSingle) {
                cy.wait(['@selectSave', '@selectSave'])
            } else {
                cy.wait(['@deselectSave', '@selectSave'])
            }

            // the text stays cleared after resuming, and a blank response blocks completion
            cy.reload()
            cy.get(`${answerRow(3)} [data-cy="selected_true"]`)
            cy.get(textInput(2)).should('have.value', '')
            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get(textError(2)).contains('Text response is required')
            cy.get('[data-cy="surveyCompletion"]').should('not.exist')
            checkNeverSent(sentTexts, 'slow text and more')

            // fresh text entered right before completion is the one saved
            cy.get(textInput(2)).type('fresh text')
            cy.clickCompleteQuizBtn()
            cy.get('[data-cy="surveyCompletion"]')
            checkNeverSent(sentTexts, 'slow text and more')
            checkHistory({ 0: 'First', 2: 'Other reason: fresh text' })
        });

        it(`a failed save explains how to recover and keeps the text [${questionType}]`, () => {
            interceptSaves({ 'unsaved text': 'failedSave' }, {
                'unsaved text': (req) => req.reply({ forceNetworkError: true }),
            })
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(answerRow(3)).click()
            cy.wait('@selectSave')
            cy.get(textInput(2)).type('unsaved text')
            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get('[data-cy="quizSaveError"]').should('have.text', recoveryMessage)
            cy.get(textInput(2)).should('have.value', 'unsaved text')
            cy.get('[data-cy="surveyCompletion"]').should('not.exist')

            // invalid text keeps its specific error
            cy.get(textInput(2)).clear()
            cy.get(textInput(2)).type('jabberwocky text')
            cy.get('[data-cy="completeQuizBtn"]').click()
            cy.get('[data-cy="quizSaveError"]').contains('answerText is invalid: paragraphs may not contain jabberwocky')
            cy.get('[data-cy="quizSaveError"]').should('not.contain', 'Copy your text responses')
            cy.get('[data-cy="surveyCompletion"]').should('not.exist')
        });

        it(`a saved response is kept when the setting is turned off mid-attempt [${questionType}]`, () => {
            interceptSaves({ 'kept text': 'keptSave' })
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(answerRow(3)).click()
            cy.get(textInput(2)).type('kept text')
            cy.wait('@keptSave').its('response.statusCode').should('eq', 200)

            // the admin turns off the text response on that answer
            cy.request('/admin/quiz-definitions/quiz1/questions').then((res) => {
                const question = res.body.questions[0]
                question.answers[2].allowCustomText = false
                question.quizId = 'quiz1'
                cy.request('POST', `/admin/quiz-definitions/quiz1/questions/${question.id}`, question)
            })

            cy.reload()
            cy.get(`${answerRow(3)} [data-cy="selected_true"]`)
            cy.get(textInput(2)).should('not.exist')
            cy.clickCompleteQuizBtn()
            cy.get('[data-cy="surveyCompletion"]')
            checkHistory({ 2: 'Other reason: kept text' })
        });

        it(`saved responses show in attempt history and the results table [${questionType}]`, () => {
            createTextResponseSurvey(questionType)
            startSurvey()
            cy.get(answerRow(4)).click()
            cy.get(textInput(3)).type('A response with <b>markup</b> & more')
            if (!isSingle) {
                cy.get(answerRow(3)).click()
                cy.get(textInput(2)).type('Other response')
            }
            cy.clickCompleteQuizBtn()
            cy.get('[data-cy="surveyCompletion"]')

            checkHistory(isSingle ?
                { 3: 'Another reason: A response with markup & more' } :
                { 2: 'Other reason: Other response', 3: 'Another reason: A response with markup & more' })

            cy.visit('/administrator/quizzes/quiz1/results');
            cy.get('[data-cy="metrics-q1"] [data-p-index="3"] [data-pc-section="rowtogglebutton"]').click()
            const historyTable = '[data-cy="metrics-q1"] [data-cy="row3-answerHistory"]'
            cy.get(`${historyTable} [data-cy="expandAll"]`).click()
            cy.get(`${historyTable} [data-cy="row0-colAnswerTxt"]`).should('have.text', 'A response with markup & more')
        });
    });
});
