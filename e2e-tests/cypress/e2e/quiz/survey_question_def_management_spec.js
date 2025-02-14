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
import dayjs from 'dayjs';
import utcPlugin from 'dayjs/plugin/utc';

dayjs.extend(utcPlugin);

describe('Survey Question CRUD Tests', () => {

    beforeEach(() => {
        Cypress.Commands.add('validateChoiceAnswer', (qNum, aNum, val, isSingleChoice) => {
            cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answer-${aNum}_displayText"]`).should('have.text', val)
            if (isSingleChoice) {
                cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answerDisplay-${aNum}"] [data-cy="selectCorrectAnswer"] .fa-circle`)
            } else {
                cy.get(`[data-cy="questionDisplayCard-${qNum}"] [data-cy="answerDisplay-${aNum}"] [data-cy="selectCorrectAnswer"] .fa-square`)
            }
        });
    });

    it('create survey questions', function () {
        cy.createSurveyDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '0')

        // multiple choice question
        cy.get('[data-cy="btn_Questions"]').should('be.enabled')
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="btn_Questions"]').click({force: true})
        cy.typeQuestion('What is 2 + 2?')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('3')

        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').should('not.exist')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').should('not.exist')
        cy.get('[data-cy="answer-2"] [data-cy="selectCorrectAnswer"]').should('not.exist')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('What is 2 + 2?')

        // q1
        cy.validateChoiceAnswer(1, 0, '1', true)
        cy.validateChoiceAnswer(1, 1, '4', true)
        cy.validateChoiceAnswer(1, 2, '3', true)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-3_displayText"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '1')
        cy.get('[data-cy="btn_Questions"]').should('have.focus')

        // single choice question
        cy.openDialog('[data-cy="newQuestionOnBottomBtn"]', true)
        cy.typeQuestion('What is 1 + 2?')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('3')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('three')
        cy.get('[data-cy="answer-3"] [data-cy="answerText"]').type('4')

        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').should('not.exist')
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"]').should('not.exist')
        cy.get('[data-cy="answer-2"] [data-cy="selectCorrectAnswer"]').should('not.exist')
        cy.get('[data-cy="answer-3"] [data-cy="selectCorrectAnswer"]').should('not.exist')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('What is 1 + 2?')
        cy.validateChoiceAnswer(2, 0, '3', false)
        cy.validateChoiceAnswer(2, 1, '1', false)
        cy.validateChoiceAnswer(2, 2, 'three', false)
        cy.validateChoiceAnswer(2, 3, '4', false)
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-4_displayText"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '2')
        cy.get('[data-cy="newQuestionOnBottomBtn"]').should('have.focus')

        // q3 - text input question
        cy.openDialog('[data-cy="newQuestionOnBottomBtn"]', true)
        cy.typeQuestion('Enter Text Here Please')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]')
        cy.get('[data-cy="textAreaPlaceHolder"]').should('not.exist')
        cy.get('[data-cy="selectionItem_TextInput"]').click()
        cy.get('[data-cy="textAreaPlaceHolder"]')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('not.exist')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('not.exist')

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '3')
        cy.get('[data-cy="newQuestionOnBottomBtn"]').should('have.focus')

        // q3
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('Enter Text Here Please')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="textAreaPlaceHolder"]')
        cy.get(`[data-cy="questionDisplayCard-3"] [data-cy="answer-0_displayText"]`).should('not.exist')

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('What is 1 + 2?')
        cy.validateChoiceAnswer(2, 0, '3', false)
        cy.validateChoiceAnswer(2, 1, '1', false)
        cy.validateChoiceAnswer(2, 2, 'three', false)
        cy.validateChoiceAnswer(2, 3, '4', false)
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-4_displayText"]').should('not.exist')

        // q1
        cy.validateChoiceAnswer(1, 0, '1', true)
        cy.validateChoiceAnswer(1, 1, '4', true)
        cy.validateChoiceAnswer(1, 2, '3', true)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-3_displayText"]').should('not.exist')


        // rating choice question
        cy.get('[data-cy="btn_Questions"]').click()
        cy.typeQuestion('How is this quiz?')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_Rating"]').click()

        cy.get('[data-cy="saveDialogBtn"]').should('be.enabled')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('How is this quiz?')

        // q5 doesn't exist
        cy.get('[data-cy="questionDisplayCard-5"]').should('not.exist')

        cy.visit('/administrator/quizzes/quiz1');
        // q3
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('Enter Text Here Please')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="textAreaPlaceHolder"]')
        cy.get(`[data-cy="questionDisplayCard-3"] [data-cy="answer-0_displayText"]`).should('not.exist')

        // q2
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('What is 1 + 2?')
        cy.validateChoiceAnswer(2, 0, '3', false)
        cy.validateChoiceAnswer(2, 1, '1', false)
        cy.validateChoiceAnswer(2, 2, 'three', false)
        cy.validateChoiceAnswer(2, 3, '4', false)
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="answer-4_displayText"]').should('not.exist')

        // q1
        cy.validateChoiceAnswer(1, 0, '1', true)
        cy.validateChoiceAnswer(1, 1, '4', true)
        cy.validateChoiceAnswer(1, 2, '3', true)
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-3_displayText"]').should('not.exist')

        // q3
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('How is this quiz?')

        // q5 doesn't exist
        cy.get('[data-cy="questionDisplayCard-5"]').should('not.exist')
    });

    it('modal validation: at least 2 answers are required', function () {
        cy.createSurveyDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.typeQuestion('a')

        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"]').should('not.exist')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')

        cy.get('[data-cy="answersError"]').should('not.be.visible')
        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="answersError"]').contains('Must have at least 2 answers')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('2')
        cy.get('[data-cy="answersError"]').should('not.be.visible')
    });

    it('empty answers are ignored', function () {
        cy.createSurveyDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.typeQuestion('What is 2 + 2?')

        cy.get(`[data-cy="answer-0"] [data-cy="addNewAnswer"]`).click()
        cy.get(`[data-cy="answer-2"] [data-cy="addNewAnswer"]`).click()
        cy.get(`[data-cy="answer-1"] [data-cy="addNewAnswer"]`).click()

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="answer-4"] [data-cy="answerText"]').type('5')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('What is 2 + 2?')
        cy.validateChoiceAnswer(1, 0, '1', false)
        cy.validateChoiceAnswer(1, 1, '4', false)
        cy.validateChoiceAnswer(1, 2, '5', false)
    });

    it('question and answers are changed in various ways before saved', function () {
        cy.createSurveyDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="btn_Questions"]').click()
        cy.get('[data-cy="descriptionError"]').should('not.be.visible')

        cy.typeQuestion('What is 2 + 2?')

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('2')

        cy.get(`[data-cy="answer-0"] [data-cy="addNewAnswer"]`).click()
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('1a')

        cy.get(`[data-cy="answer-1"] [data-cy="addNewAnswer"]`).click()
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('1b')

        cy.get(`[data-cy="answer-3"] [data-cy="addNewAnswer"]`).click()
        cy.get('[data-cy="answer-4"] [data-cy="answerText"]').type('2a')
        cy.get(`[data-cy="answer-4"] [data-cy="addNewAnswer"]`).click()
        cy.get('[data-cy="answer-5"] [data-cy="answerText"]').type('2b')

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('-1')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('-2')
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('-3')
        cy.get('[data-cy="answer-3"] [data-cy="answerText"]').type('-4')
        cy.get('[data-cy="answer-4"] [data-cy="answerText"]').type('-5')
        cy.get('[data-cy="answer-5"] [data-cy="answerText"]').type('-6')

        cy.get('[data-cy="answer-4"] [data-cy="removeAnswer"]').click()
        cy.get('[data-cy="answer-4"] [data-cy="removeAnswer"]').click()
        cy.get('[data-cy="answer-0"] [data-cy="removeAnswer"]').click()

        cy.get('[data-cy="questionText"] div.toastui-editor-contents[contenteditable="true"]').clear().type('All diff?')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('All diff?')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-0_displayText"]').should('have.text', '1a-2')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-1_displayText"]').should('have.text', '1b-3')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-2_displayText"]').should('have.text', '2-4')
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="answer-3_displayText"]').should('not.exist')
        cy.validateChoiceAnswer(1, 0, '1a-2', false)
        cy.validateChoiceAnswer(1, 1, '1b-3', false)
        cy.validateChoiceAnswer(1, 2, '2-4', false)
    });

    it('edit a multiple choice question', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_2"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 2')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"]  [data-cy="answerText"]').should('have.value', 'Question 2 - First Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"]  [data-cy="answerText"]').should('have.value', 'Question 2 - Second Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"]  [data-cy="answerText"]').should('have.value', 'Question 2 - Third Answer')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="answerText"]').type('-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="answerText"]').clear().type('b')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').clear().type('c')
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').type('-more')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2-more')
        cy.validateChoiceAnswer(2, 0, 'Question 2 - First Answer-more', false)
        cy.validateChoiceAnswer(2, 1, 'b', false)
        cy.validateChoiceAnswer(2, 2, 'c', false)
        cy.get('[data-cy="editQuestionButton_2"]').should('have.focus')

        cy.get('[data-cy="editQuestionButton_2"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 2-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"]  [data-cy="answerText"]').should('have.value', 'Question 2 - First Answer-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"]  [data-cy="answerText"]').should('have.value', 'b')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"]  [data-cy="answerText"]').should('have.value', 'c')
    });

    it('edit a question - change the scale of a rating', function () {
        cy.createSurveyDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '0')

        // multiple choice question
        cy.get('[data-cy="btn_Questions"]').click()

        cy.typeQuestion('How is this quiz?')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_Rating"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('How is this quiz?')
        cy.get('[data-cy="questionDisplayCard-1"] [data-pc-name="rating"] [data-pc-section="option"]').should('have.length', 5)

        cy.get('[data-cy="editQuestionButton_1"]').click();
        cy.typeQuestion(' With more description')
        cy.get('[data-cy="ratingScaleSelect"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('8').click();
        // cy.get('[data-cy="saveDialogBtn"]').click()
        //
        // cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('How is this quiz? With more description')
        // cy.get('[data-cy="questionDisplayCard-1"] [data-pc-name="rating"] [data-pc-section="option"]').should('have.length', 8)
        //
        // cy.visit('/administrator/quizzes/quiz1');
        // cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('How is this quiz? With more description')
        // cy.get('[data-cy="questionDisplayCard-1"] [data-pc-name="rating"] [data-pc-section="option"]').should('have.length', 8)
    });

    it('edit a question - will change question type from MultipleChoice to SingleChoice', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_2"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(2, 0, 'Question 2 - First Answer', true)
        cy.validateChoiceAnswer(2, 1, 'Question 2 - Second Answer', true)
        cy.validateChoiceAnswer(2, 2, 'Question 2 - Third Answer', true)
    });

    it('edit a question - will change question type from SingleChoice to MultipleChoice', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_3"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_MultipleChoice"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(3, 0, 'Question 3 - First Answer', false)
        cy.validateChoiceAnswer(3, 1, 'Question 3 - Second Answer', false)
        cy.validateChoiceAnswer(3, 2, 'Question 3 - Third Answer', false)
    });

    it('edit a question - change question type from MultipleChoice to TextInput', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_2"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_TextInput"]').click()

        cy.get('[data-cy="textAreaPlaceHolder"]')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('not.exist')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('not.exist')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="answerTypeSelector"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '3')

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="textAreaPlaceHolder"]')
        cy.get(`[data-cy="questionDisplayCard-2"] [data-cy="answer-0_displayText"]`).should('not.exist')
    });

    it('edit a question - change question type from SingleChoice to TextInput', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_3"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_TextInput"]').click()

        cy.get('[data-cy="textAreaPlaceHolder"]')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('not.exist')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('not.exist')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="answerTypeSelector"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '3')

        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="textAreaPlaceHolder"]')
        cy.get(`[data-cy="questionDisplayCard-3"] [data-cy="answer-0_displayText"]`).should('not.exist')
    });

    it('edit a question - change question type from TextInput to SingleChoice', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_1"]').click();
        cy.get('[data-cy="answerTypeSelector"]').contains('Input Text')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('a')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('b')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(1, 0, 'a', true)
        cy.validateChoiceAnswer(1, 1, 'b', true)
    });

    it('edit a question - change question type from TextInput to MultipleChoice', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_1"]').click();
        cy.get('[data-cy="answerTypeSelector"]').contains('Input Text')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_MultipleChoice"]').click()

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('a')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('b')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(1, 0, 'a', false)
        cy.validateChoiceAnswer(1, 1, 'b', false)
    });

    it('edit a question - add an answer', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_3"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').type( 'new')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(3, 0, 'Question 3 - First Answer', true)
        cy.validateChoiceAnswer(3, 1, 'Question 3 - Second Answer', true)
        cy.validateChoiceAnswer(3, 2, 'new', true)
        cy.validateChoiceAnswer(3, 3, 'Question 3 - Third Answer', true)
    });

    it('edit a question - remove answer', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="editQuestionButton_3"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="removeAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(3, 0, 'Question 3 - Second Answer', true)
        cy.validateChoiceAnswer(3, 1, 'Question 3 - Third Answer', true)
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="answer-2_displayText"]').should('not.exist')
    });

    it('delete questions', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        const q1Card = '[data-cy="questionDisplayCard-1"] [data-cy="sortControlHandle"]';
        const q2Card = '[data-cy="questionDisplayCard-2"] [data-cy="sortControlHandle"]';
        cy.get(q1Card).should('exist')
        cy.get(q2Card).should('exist')

        cy.openDialog('[data-cy="deleteQuestionButton_2"]')
        cy.get('[data-cy="currentValidationText"]').fill('Delete Me')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get(q1Card).should('not.exist')
        cy.get(q2Card).should('not.exist')

        cy.get('[data-cy="deleteQuestionButton_2"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '1')
        cy.get('[data-cy="btn_Questions"]').should('have.focus')

        cy.get('[data-cy="noQuestionsYet"]').should('not.exist')
        cy.openDialog('[data-cy="deleteQuestionButton_1"]')
        cy.get('[data-cy="currentValidationText"]').fill('Delete Me')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="deleteQuestionButton_2"]').should('not.exist')
        cy.get('[data-cy="deleteQuestionButton_1"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '0')
        cy.get('[data-cy="btn_Questions"]').should('have.focus')
        cy.get('[data-cy="noQuestionsYet"]')
    });

    it('drag and drop to sort questions', function () {
        cy.createSurveyDef(1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 1);
        cy.createTextInputQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        const q1Card = '[data-cy="questionDisplayCard-1"] [data-cy="sortControlHandle"]';
        const q2Card = '[data-cy="questionDisplayCard-2"] [data-cy="sortControlHandle"]';
        const q3Card = '[data-cy="questionDisplayCard-3"] [data-cy="sortControlHandle"]';

        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 1', 'question # 2', 'question # 3']);
        cy.get(q1Card).dragAndDrop(q2Card);
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 1', 'question # 3']);
        cy.get(q3Card).dragAndDrop(q2Card);
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);

        cy.visit('/administrator/quizzes/quiz1');
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);
    });

    it('creating 2nd question enables drag-and-drop', function () {
        cy.createSurveyDef(1);
        cy.visit('/administrator/quizzes/quiz1');

        const q1Card = '[data-cy="questionDisplayCard-1"] [data-cy="sortControlHandle"]';
        const q2Card = '[data-cy="questionDisplayCard-2"] [data-cy="sortControlHandle"]';

        cy.openDialog('[data-cy="newQuestionOnBottomBtn"]', true)
        cy.typeQuestion('question # 1')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('3')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('4')
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get(q1Card).should('not.exist')
        cy.get(q2Card).should('not.exist')

        cy.openDialog('[data-cy="newQuestionOnBottomBtn"]', true)
        cy.get('[data-cy="answerTypeSelector"]').should('exist')
        cy.typeQuestion('question # 2')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_TextInput"]').click()
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get(q1Card).should('exist')
        cy.get(q2Card).should('exist')

        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 1', 'question # 2']);
        cy.get(q1Card).dragAndDrop(q2Card);
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 1']);
    });

    it('user keyboard to sort questions', function () {
        cy.intercept('PATCH', '/admin/quiz-definitions/quiz1/questions/*').as('patchQuestion');
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 1', 'question # 2', 'question # 3']);
        cy.log("Operation: 1")
        cy.wait(1000)
        cy.get('[data-cy="btn_Questions"]')
            .tab()
            .type('{downArrow}');
        cy.wait('@patchQuestion')
        cy.get('.spinner-border').should('not.exist')
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 1', 'question # 3']);
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="sortControlHandle"]').should('have.focus');

        cy.log("Operation: 2")
        cy.wait(1000)
        cy.get('[data-cy="deleteQuestionButton_1"]')
            .tab()
            .type('{downArrow}');
        cy.wait('@patchQuestion')
        cy.get('.spinner-border').should('not.exist')
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 3', 'question # 1']);

        // attempt to move the lowest item - should not change anything
        cy.log("Operation: 3")
        cy.wait(1000)
        cy.get('[data-cy="deleteQuestionButton_2"]')
            .tab()
            .type('{downArrow}');
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 2', 'question # 3', 'question # 1']);

        cy.log("Operation: 4")
        cy.wait(1000)
        cy.get('[data-cy="deleteQuestionButton_1"]')
            .tab()
            .type('{upArrow}');
        cy.wait('@patchQuestion')
        cy.get('.spinner-border').should('not.exist')
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);

        // attempt to move the top item - should not change anything
        cy.log("Operation: 5")
        cy.wait(1000)
        cy.get('[data-cy="btn_Questions"]')
            .tab()
            .type('{upArrow}');
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);

        cy.log("Operation: 6")
        cy.visit('/administrator/quizzes/quiz1');
        cy.validateElementsOrder('[data-cy="questionDisplayCard"]', ['question # 3', 'question # 2', 'question # 1']);
    });

    // copy
    it('copy a multiple choice question', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_2"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 2')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"]  [data-cy="answerText"]').should('have.value', 'Question 2 - First Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"]  [data-cy="answerText"]').should('have.value', 'Question 2 - Second Answer')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"]  [data-cy="answerText"]').should('have.value', 'Question 2 - Third Answer')

        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="answerText"]').type('-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="answerText"]').clear().type('b')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').clear().type('c')
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').type('-more')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 2-more')
        cy.validateChoiceAnswer(4, 0, 'Question 2 - First Answer-more', false)
        cy.validateChoiceAnswer(4, 1, 'b', false)
        cy.validateChoiceAnswer(4, 2, 'c', false)
        cy.get('[data-cy="copyQuestionButton_2"]').should('have.focus')

        cy.get('[data-cy="copyQuestionButton_4"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="markdownEditorInput"]').contains('This is a question # 2-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"]  [data-cy="answerText"]').should('have.value', 'Question 2 - First Answer-more')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"]  [data-cy="answerText"]').should('have.value', 'b')
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"]  [data-cy="answerText"]').should('have.value', 'c')
    });

    it('copy a question - change the scale of a rating', function () {
        cy.createSurveyDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="noQuestionsYet"]')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '0')

        // multiple choice question
        cy.get('[data-cy="btn_Questions"]').click()

        cy.typeQuestion('How is this quiz?')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_Rating"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').contains('How is this quiz?')
        cy.get('[data-cy="questionDisplayCard-1"] [data-pc-name="rating"] [data-pc-section="option"]').should('have.length', 5)

        cy.get('[data-cy="copyQuestionButton_1"]').click();
        cy.get('[data-cy="questionText"] .toastui-editor-ww-container .toastui-editor-contents').type(' With more description', {delay: 0})
        cy.get('[data-cy="ratingScaleSelect"]').click()
        cy.get('[data-pc-section="overlay"] [data-pc-section="option"]').contains('8').click();
        cy.get('[data-cy="saveDialogBtn"]').click()

        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('How is this quiz? With more description')
        cy.get('[data-cy="questionDisplayCard-2"] [data-pc-name="rating"] [data-pc-section="option"]').should('have.length', 8)

        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionDisplayText"]').contains('How is this quiz? With more description')
        cy.get('[data-cy="questionDisplayCard-2"] [data-pc-name="rating"] [data-pc-section="option"]').should('have.length', 8)
    });

    it('copy a question - will change question type from MultipleChoice to SingleChoice', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_2"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(4, 0, 'Question 2 - First Answer', true)
        cy.validateChoiceAnswer(4, 1, 'Question 2 - Second Answer', true)
        cy.validateChoiceAnswer(4, 2, 'Question 2 - Third Answer', true)
    });

    it('copy a question - will change question type from SingleChoice to MultipleChoice', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_3"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_MultipleChoice"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(4, 0, 'Question 3 - First Answer', false)
        cy.validateChoiceAnswer(4, 1, 'Question 3 - Second Answer', false)
        cy.validateChoiceAnswer(4, 2, 'Question 3 - Third Answer', false)
    });

    it('copy a question - change question type from MultipleChoice to TextInput', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_2"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_TextInput"]').click()

        cy.get('[data-cy="textAreaPlaceHolder"]')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('not.exist')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('not.exist')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="answerTypeSelector"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '4')

        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 2')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="textAreaPlaceHolder"]')
        cy.get(`[data-cy="questionDisplayCard-4"] [data-cy="answer-0_displayText"]`).should('not.exist')
    });

    it('copy a question - change question type from SingleChoice to TextInput', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_3"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_TextInput"]').click()

        cy.get('[data-cy="textAreaPlaceHolder"]')
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('not.exist')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('not.exist')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.get('[data-cy="answerTypeSelector"]').should('not.exist')
        cy.get('[data-cy="pageHeaderStat_Questions"] [data-cy="statValue"]').should('have.text', '4')

        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionDisplayText"]').contains('This is a question # 3')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="textAreaPlaceHolder"]')
        cy.get(`[data-cy="questionDisplayCard-4"] [data-cy="answer-0_displayText"]`).should('not.exist')
    });

    it('copy a question - change question type from TextInput to SingleChoice', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_1"]').click();
        cy.get('[data-cy="answerTypeSelector"]').contains('Input Text')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_SingleChoice"]').click()

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('a')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('b')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(4, 0, 'a', true)
        cy.validateChoiceAnswer(4, 1, 'b', true)
    });

    it('copy a question - change question type from TextInput to MultipleChoice', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_1"]').click();
        cy.get('[data-cy="answerTypeSelector"]').contains('Input Text')
        cy.get('[data-cy="answerTypeSelector"]').click()
        cy.get('[data-cy="selectionItem_MultipleChoice"]').click()

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('a')
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('b')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(4, 0, 'a', false)
        cy.validateChoiceAnswer(4, 1, 'b', false)
    });

    it('copy a question - add an answer', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_3"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="addNewAnswer"]').click()
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').type( 'new')

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(4, 0, 'Question 3 - First Answer', true)
        cy.validateChoiceAnswer(4, 1, 'Question 3 - Second Answer', true)
        cy.validateChoiceAnswer(4, 2, 'new', true)
        cy.validateChoiceAnswer(4, 3, 'Question 3 - Third Answer', true)
    });

    it('copy a question - remove answer', function () {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createSurveyMultipleChoiceQuestionDef(1, 2)
        cy.createSurveyMultipleChoiceQuestionDef(1, 3, { questionType: 'SingleChoice' });
        cy.visit('/administrator/quizzes/quiz1');

        cy.get('[data-cy="copyQuestionButton_3"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="removeAnswer"]').click()

        cy.get('[data-cy="saveDialogBtn"]').click()
        cy.validateChoiceAnswer(4, 0, 'Question 3 - Second Answer', true)
        cy.validateChoiceAnswer(4, 1, 'Question 3 - Third Answer', true)
    });
});
