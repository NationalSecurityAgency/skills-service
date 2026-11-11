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

        it(`text input focus, spaces, required and length validation [${questionType}]`, () => {
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

        it(`server validation errors are shown on the text input [${questionType}]`, () => {
            cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*').as('reportAnswer')
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(answerRow(3)).click()
            cy.wait('@reportAnswer')
            cy.get(textInput(2)).type('jabberwocky')
            cy.wait('@reportAnswer').its('response.statusCode').should('eq', 400)
            cy.get(textError(2)).contains('answerText is invalid: paragraphs may not contain jabberwocky')
        });

        it(`switch options, clear, resume and complete [${questionType}]`, () => {
            cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*').as('reportAnswer')
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
            cy.wait(1500)
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

            cy.request('/admin/quiz-definitions/quiz1/runs?query=&limit=10&ascending=false&page=1&orderBy=started').then((res) => {
                cy.request(`/admin/quiz-definitions/quiz1/runs/${res.body.data[0].attemptId}`).then((run) => {
                    const answers = run.body.questions[0].answers.map((a) => a.answer)
                    expect(answers).to.deep.equal(isSingle ?
                        ['First', 'Second', 'Other reason', 'Another reason: Final text'] :
                        ['First', 'Second', 'Other reason: First text', 'Another reason: Final text'])
                })
            })
        });

        it(`deselecting right after typing does not restore the option [${questionType}]`, () => {
            cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*').as('reportAnswer')
            createTextResponseSurvey(questionType)
            startSurvey()

            cy.get(answerRow(3)).click()
            cy.get(textInput(2)).type('typed then removed')
            cy.get(answerRow(3)).click()
            cy.get(textInput(2)).should('not.exist')
            cy.wait(2000)
            cy.reload()
            cy.get(`${answerRow(3)} [data-cy="selected_false"]`)
            cy.get(textInput(2)).should('not.exist')
            if (isSingle) {
                // a newer single choice selection is not overridden by a pending text save
                cy.get(answerRow(3)).click()
                cy.get(textInput(2)).type('pending text')
                cy.get(answerRow(2)).click()
                cy.wait(2000)
                cy.reload()
                cy.get(`${answerRow(2)} [data-cy="selected_true"]`)
                cy.get(`${answerRow(3)} [data-cy="selected_false"]`)
            }
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

            cy.visit('/progress-and-rankings/my-quiz-attempts');
            cy.get('[data-cy="viewQuizAttempt"]').first().click()
            cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-3_displayText"]').should('have.text', 'Another reason: A response with markup & more')
            if (!isSingle) {
                cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').should('have.text', 'Other reason: Other response')
            }

            cy.visit('/administrator/quizzes/quiz1/results');
            cy.get('[data-cy="metrics-q1"] [data-p-index="3"] [data-pc-section="rowtogglebutton"]').click()
            const historyTable = '[data-cy="metrics-q1"] [data-cy="row3-answerHistory"]'
            cy.get(`${historyTable} [data-cy="expandAll"]`).click()
            cy.get(`${historyTable} [data-cy="row0-colAnswerTxt"]`).should('have.text', 'A response with markup & more')
        });
    });
});
