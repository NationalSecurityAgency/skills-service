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
import relativeTimePlugin from 'dayjs/plugin/relativeTime';
import advancedFormatPlugin from 'dayjs/plugin/advancedFormat';
import moment from "moment-timezone";

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Skills Display Run Quizzes With Fill In the Blank Questions', () => {

    const quizPath = '/subjects/subj1/skills/skill1/quizzes/quiz1';
    const firstBlank = '[data-cy="question_1"] [data-cy="questions[0].answerTextArray[0]"]';
    const secondBlank = '[data-cy="question_1"] [data-cy="questions[0].answerTextArray[1]"]';
    const thirdBlank = '[data-cy="question_1"] [data-cy="questions[0].answerTextArray[2]"]';

    const createQuizSkill = () => {
        cy.createQuizDef(1);
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1', pointIncrement: '150', numPerformToCompletion: 1 });
    };

    it('run quiz with 1 fill in the blank', () => {
        cy.createQuizDef(1);
        cy.createFillInTheBlankQuestionDef(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizMsg"]')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[0]"]').type('Question 1 - First Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[1]"]').type('Question 1 - Second Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[2]"]').type('Question 1 - Third Answer')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

    });

    it('run quiz with multiple questions where 1 is fill in the blank', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createFillInTheBlankQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()

        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[0]"]').type('Question 2 - First Answer')
        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[1]"]').type('Question 2 - Second Answer')
        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[2]"]').type('Question 2 - Third Answer')

        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
    });

    it('run quiz with multiple questions and multiple fill in the blank questions', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createFillInTheBlankQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)
        cy.createFillInTheBlankQuestionDef(1, 4)
        cy.createFillInTheBlankQuestionDef(1, 5)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '5')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()

        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[0]"]').type('Question 2 - First Answer')
        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[1]"]').type('Question 2 - Second Answer')
        cy.get('[data-cy="question_2"] [data-cy="questions[1].answerTextArray[2]"]').type('Question 2 - Third Answer')

        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="question_4"] [data-cy="questions[3].answerTextArray[0]"]').type('Question 4 - First Answer')
        cy.get('[data-cy="question_4"] [data-cy="questions[3].answerTextArray[1]"]').type('Question 4 - Second Answer')
        cy.get('[data-cy="question_4"] [data-cy="questions[3].answerTextArray[2]"]').type('Question 4 - Third Answer')

        cy.get('[data-cy="question_5"] [data-cy="questions[4].answerTextArray[0]"]').type('Question 5 - First Answer')
        cy.get('[data-cy="question_5"] [data-cy="questions[4].answerTextArray[1]"]').type('Question 5 - Second Answer')
        cy.get('[data-cy="question_5"] [data-cy="questions[4].answerTextArray[2]"]').type('Question 5 - Third Answer')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
    });

    it('fill in the blank trims leading and trailing spaces when grading', () => {
        cy.createQuizDef(1);
        cy.createFillInTheBlankQuestionDef(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizMsg"]')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[0]"]').type('       Question 1 - First Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[1]"]').type('Question 1 - Second Answer       ')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[2]"]').type('Question 1 - Third Answer')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

    });

    it('fill in the blank is not case sensitive', () => {
        cy.createQuizDef(1);
        cy.createFillInTheBlankQuestionDef(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizMsg"]')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[0]"]').type('QUESTION 1 - First Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[1]"]').type('Question 1 - SECOND Answer')
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[2]"]').type('QUESTION 1 - Third ANSWER')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

    });

    it('answers are not reported after quiz is completed', () => {
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*', cy.spy().as('reportAnswer'))
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/complete', cy.spy().as('completeQuiz'))
        cy.createQuizDef(1);
        cy.createFillInTheBlankQuestionDef(1, 1, {
            answers: [{
                answer: `Only Answer`,
                isCorrect: false,
            }]
        })

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizMsg"]')
        cy.get('[data-cy="takeQuizBtn"]').click()
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="questions[0].answerTextArray[0]"]').type('Only Answer')

        cy.clickCompleteQuizBtn()
        cy.get('@completeQuiz').should('have.been.called');
        cy.get('@reportAnswer').should('have.been.calledBefore', '@completeQuiz');

        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')

    });
    it('waits for every blank to save before completing the quiz', () => {
        cy.createQuizDef(1);
        cy.createFillInTheBlankQuestionDef(1, 1);
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1', pointIncrement: '150', numPerformToCompletion: 1 });
        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').click();

        let releaseFirstAnswer;
        const firstAnswerReady = new Cypress.Promise((resolve) => { releaseFirstAnswer = resolve; });
        let completionRequested = false;
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*', (req) => {
            if (req.body.answerText === 'Question 1 - First Answer') {
                return firstAnswerReady.then(() => req.continue());
            }
            if (req.body.answerText === 'Question 1 - Third Answer') {
                req.alias = 'lastBlankSaved';
            }
        });
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/complete', () => {
            completionRequested = true;
        });
        cy.get('[data-cy="questions[0].answerTextArray[0]"]').type('Question 1 - First Answer');
        cy.get('[data-cy="questions[0].answerTextArray[1]"]').type('Question 1 - Second Answer');
        cy.get('[data-cy="questions[0].answerTextArray[2]"]').type('Question 1 - Third Answer');
        cy.wait('@lastBlankSaved');
        cy.get('[data-cy="completeQuizBtn"]').click();
        cy.get('[data-cy="completeQuizBtn"]').should('be.disabled').then(() => {
            expect(completionRequested).to.equal(false);
            releaseFirstAnswer();
        });
        cy.get('[data-cy="quizCompletion"]').should('contain.text', 'Congrats!!');
    });

    it('creates and edits fill in the blank questions using the question form', () => {
        cy.createQuizDef(1);
        cy.visit('/administrator/quizzes/quiz1');
        cy.openDialog('[data-cy="btn_Questions"]', true);

        cy.get('[data-cy="answerTypeSelector"]').click();
        cy.get('[data-cy="selectionItem_FillInTheBlank"]').click();
        cy.get('[data-cy="answer-0"]').should('not.exist');
        cy.typeInMarkdownEditor('[data-cy="questionText"]', 'First ___');
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').type('alpha', { force: true });
        cy.get('[data-cy="answer-1"]').should('not.exist');

        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"] .toastui-editor-ww-container .toastui-editor-contents').type(' and second ___', { force: true });
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').type('beta', { force: true });
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"] .toastui-editor-ww-container .toastui-editor-contents').type(' and third ___', { force: true });
        cy.get('[data-cy="answer-2"] [data-cy="answerText"]').type('gamma', { force: true });
        cy.get('[data-cy="questionText"] [data-cy="markdownEditorInput"] .toastui-editor-ww-container .toastui-editor-contents').type('{backspace}{backspace}{backspace}');
        cy.get('[data-cy="answer-2"]').should('not.exist');
        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'alpha');
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'beta');
        cy.clickSaveDialogBtn();

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').should('contain.text', 'First ___ and second ___ and third');
        cy.get('[data-cy="questionDisplayCard-1"]').should('contain.text', 'alpha').and('contain.text', 'beta');
        cy.get('[data-cy="questionDisplayCard-1"]').should('not.contain.text', 'gamma');

        cy.get('[data-cy="editQuestionButton_1"]').click();
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'alpha');
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'beta');
        cy.get('[data-cy="editQuestionModal"] [data-cy="questionText"] [data-cy="markdownEditorInput"] .toastui-editor-ww-container .toastui-editor-contents').type(' ___', { force: true });
        cy.get('[data-cy="editQuestionModal"] [data-cy="answer-2"] [data-cy="answerText"]').type('gamma', { force: true });

        cy.clickSaveDialogBtn();
        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionDisplayText"]').should('contain.text', 'First ___ and second ___ and third ___');
        cy.get('[data-cy="questionDisplayCard-1"]').should('contain.text', 'gamma');
    });

    it('retains correct answers when switching an existing question to fill in the blank', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, {
            question: 'First ___ and second ___',
            answers: [
                { answer: 'alpha', isCorrect: true },
                { answer: 'beta', isCorrect: false },
            ],
        });
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="editQuestionButton_1"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click();
        cy.get('[data-cy="selectionItem_FillInTheBlank"]').click();

        cy.get('[data-cy="answer-0"] [data-cy="answerText"]').should('have.value', 'alpha');
        cy.get('[data-cy="answer-1"] [data-cy="answerText"]').should('have.value', 'beta');
        cy.clickSaveDialogBtn();

        cy.get('[data-cy="editQuestionButton_1"]').click();
        cy.get('[data-cy="answerTypeSelector"]').click();
        cy.get('[data-cy="selectionItem_SingleChoice"]').click();
        cy.get('[data-cy="answer-0"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]').should('be.visible');
        cy.get('[data-cy="answer-1"] [data-cy="selectCorrectAnswer"] [data-cy="selected"]').should('be.visible');
    });

    it('shows missing blanks and allows completing after they are filled', () => {
        createQuizSkill();
        cy.createFillInTheBlankQuestionDef(1, 1);
        cy.cdVisit(quizPath);
        cy.get('[data-cy="startQuizAttempt"]').click();

        cy.get(firstBlank).type('Question 1 - First Answer');
        cy.get(secondBlank).should('have.value', '');
        cy.get(thirdBlank).type('Question 1 - Third Answer');
        cy.get('[data-cy="completeQuizBtn"]').click();
        cy.get('[data-cy="questionErrors"]').should('be.visible').and('contain.text', 'All blanks must be filled in');
        cy.get('[data-cy="quizCompletion"]').should('not.exist');

        cy.get(secondBlank).type('Question 1 - Second Answer');
        cy.get('[data-cy="questionErrors"]').should('not.exist');
        cy.clickCompleteQuizBtn();
        cy.get('[data-cy="quizCompletion"]').should('contain.text', 'Congrats!!');
    });

    it('restores saved blanks after leaving and reopening an attempt, then saves edits', () => {
        createQuizSkill();
        cy.createFillInTheBlankQuestionDef(1, 1);
        cy.cdVisit(quizPath);
        cy.get('[data-cy="startQuizAttempt"]').click();

        // Leaving after the save response exercises the debounced reporting path without a race with navigation.
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*').as('saveBlank');
        cy.get(firstBlank).type('Question 1 - First Answer');
        cy.wait('@saveBlank');
        cy.get(thirdBlank).type('Question 1 - Third Answer');
        cy.wait('@saveBlank');
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="takeQuizBtn"]').should('be.visible').click();

        cy.get(firstBlank).should('have.value', 'Question 1 - First Answer');
        cy.get(secondBlank).should('have.value', '');
        cy.get(thirdBlank).should('have.value', 'Question 1 - Third Answer');
        cy.get(firstBlank).clear().type('wrong answer');
        cy.get(secondBlank).type('Question 1 - Second Answer');
        cy.get(firstBlank).clear().type('Question 1 - First Answer');
        cy.clickCompleteQuizBtn();
        cy.get('[data-cy="quizCompletion"]').should('contain.text', 'Congrats!!');
    });

    it('shows a failed answer save and completes only after the edited answer is saved', () => {
        createQuizSkill();
        cy.createFillInTheBlankQuestionDef(1, 1, {
            question: 'Finish ___',
            answers: [{ answer: 'Only Answer', isCorrect: true }],
        });
        cy.cdVisit(quizPath);
        cy.get('[data-cy="startQuizAttempt"]').click();

        let failNextSave = true;
        let completionRequests = 0;
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*', (req) => {
            if (failNextSave) {
                failNextSave = false;
                req.reply({ statusCode: 500, body: { explanation: 'Unable to save answer' } });
            } else {
                req.continue();
            }
        });
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/complete', (req) => {
            completionRequests += 1;
            req.continue();
        });

        cy.get(firstBlank).type('wrong answer');
        cy.get('[data-cy="completeQuizBtn"]').click();
        cy.get('[data-cy="quizSaveError"]').should('be.visible').and('contain.text', 'Unable to save answer');
        cy.get('[data-cy="quizCompletion"]').should('not.exist');
        cy.get('[data-cy="completeQuizBtn"]').should('be.enabled');
        cy.then(() => expect(completionRequests).to.equal(0));

        cy.get(firstBlank).clear().type('Only Answer');
        cy.clickCompleteQuizBtn();
        cy.get('[data-cy="quizSaveError"]').should('not.exist');
        cy.get('[data-cy="quizCompletion"]').should('contain.text', 'Congrats!!');
        cy.then(() => expect(completionRequests).to.equal(1));
    });

    it('retries a failed answer save when completing without editing the blank again', () => {
        createQuizSkill();
        cy.createFillInTheBlankQuestionDef(1, 1, {
            question: 'Finish ___',
            answers: [{ answer: 'Only Answer', isCorrect: true }],
        });
        cy.cdVisit(quizPath);
        cy.get('[data-cy="startQuizAttempt"]').click();

        let answerRequests = 0;
        let completionRequests = 0;
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*', (req) => {
            answerRequests += 1;
            if (answerRequests < 3) {
                req.reply({ statusCode: 500, body: { explanation: 'Unable to save answer' } });
            } else {
                req.continue();
            }
        });
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/complete', (req) => {
            completionRequests += 1;
            req.continue();
        });

        cy.get(firstBlank).type('Only Answer');
        cy.get('[data-cy="completeQuizBtn"]').click();
        cy.get('[data-cy="quizSaveError"]').should('be.visible').and('contain.text', 'Unable to save answer');
        cy.get('[data-cy="quizCompletion"]').should('not.exist');
        cy.then(() => {
            expect(answerRequests).to.equal(1);
            expect(completionRequests).to.equal(0);
        });

        cy.clickCompleteQuizBtn();
        cy.get('[data-cy="quizSaveError"]').should('be.visible').and('contain.text', 'Unable to save answer');
        cy.get('[data-cy="quizCompletion"]').should('not.exist');
        cy.then(() => {
            expect(answerRequests).to.equal(2);
            expect(completionRequests).to.equal(0);
        });

        cy.clickCompleteQuizBtn();
        cy.get('[data-cy="quizSaveError"]').should('not.exist');
        cy.get('[data-cy="quizCompletion"]').should('contain.text', 'Congrats!!');
        cy.then(() => {
            expect(answerRequests).to.equal(3);
            expect(completionRequests).to.equal(1);
        });
    });

});
