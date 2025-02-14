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

describe('Quiz Input Text Question Tests', () => {

    beforeEach(() => {

    })

    it('create input text question', function () {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '0')

        cy.get('[data-cy="questionText"]').type('What is 2 + 2?')

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_TextInput"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('not.exist')
        cy.get('[data-cy="textAreaPlaceHolder"]')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('What is 2 + 2?')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="textAreaPlaceHolder"]')
    })

    it('edit input text question', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)

        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="editQuestionButton_3"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 3')
        cy.get('[data-cy="answerTypeSelector"] [data-cy="selectionItem_TextInput"]')
        cy.get('[data-cy="textAreaPlaceHolder"]')
    })

    it('modify another question type to input text', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)

        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="editQuestionButton_2"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 2')
        cy.get('[data-cy="answerTypeSelector"] [data-cy="selectionItem_SingleChoice"]')

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_TextInput"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('not.exist')
        cy.get('[data-cy="textAreaPlaceHolder"]')

        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('This is a question # 1')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').should('have.text', 'Question 1 - First Answer')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="textAreaPlaceHolder"]')
    })

    it('delete input text question', function () {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createQuizQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)

        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '3')
        cy.get('[data-cy="deleteQuestionButton_1"]').should('exist')
        cy.get('[data-cy="deleteQuestionButton_2"]').should('exist')
        cy.get('[data-cy="deleteQuestionButton_3"]').should('exist')

        cy.openDialog('[data-cy="deleteQuestionButton_3"]')
        cy.get('[data-cy="currentValidationText"]').fill('Delete Me')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="deleteQuestionButton_1"]').should('exist')
        cy.get('[data-cy="deleteQuestionButton_2"]').should('exist')
        cy.get('[data-cy="deleteQuestionButton_3"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '2')
        cy.get('[data-cy="btn_Questions"]').should('have.focus')
    })
})
