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

describe('Run Quizzes With Text Input Questions', () => {

    beforeEach(() => {
    });

    it('run quiz with 1 text input', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer # 1')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
    });

    it('run quiz with multiple questions where 1 is text input', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer # 1')
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
    });

    it('run quiz with multiple questions and multiple Text Input questions', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)
        cy.createTextInputQuestionDef(1, 4)
        cy.createTextInputQuestionDef(1, 5)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '5')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer # 1')
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="question_4"] [data-cy="markdownEditorInput"]').type('Answer # 2')
        cy.get('[data-cy="question_5"] [data-cy="markdownEditorInput"]').type('Answer # 3')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

    });

    it('show input text needs grading when quizAlwaysShowCorrectAnswers property is enabled', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)
        cy.setQuizShowCorrectAnswers(1, true);

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '3')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer # 1')
        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="question_1"] [data-cy="questionAnsweredCorrectly"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_1"] [data-cy="selected_true"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_2"] [data-cy="selected_false"]')
        cy.get('[data-cy="question_1"] [data-cy="answer_3"] [data-cy="selected_false"]')

        cy.get('[data-cy="question_1"] [data-cy="needsGradingTag"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="needsGradingTag"]')
        cy.get('[data-cy="question_2"] [data-cy="questionAnsweredCorrectly"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="questionAnsweredWrong"]').should('not.exist')
        cy.get('[data-cy="question_2"] [data-cy="questionsText"]').contains('This is a question # 2')
        cy.get('[data-cy="question_2"] [data-cy="textInputAnswer"]').contains('Answer # 1')
        cy.get('[data-cy="question_3"] [data-cy="needsGradingTag"]').should('not.exist')


        cy.get('[data-cy="question_3"] [data-cy="questionAnsweredWrong"]')
        cy.get('[data-cy="question_3"] [data-cy="answer_1"] [data-cy="selected_false"]')
        cy.get('[data-cy="question_3"] [data-cy="answer_2"] [data-cy="selected_true"]')
        cy.get('[data-cy="question_3"] [data-cy="answer_3"] [data-cy="selected_false"]')
        // errors are rendered async
        cy.wait(2000)
        cy.get('[data-cy="questionErrors"]').should('not.exist')
    });

    it('cannot start previously taken quiz that needs grading', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}], 'My Answer')

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '1 / Unlimited')

        cy.get('[data-cy="quizRequiresGradingMsg"]')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="closeQuizAttempt"]').should('be.enabled')
    });

    it('cannot start previously taken quiz that needs grading even if quizMultipleTakes=true', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.setQuizMultipleTakes(1, true);

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}], 'My Answer')

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '1 / Unlimited')

        cy.get('[data-cy="quizRequiresGradingMsg"]')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="startQuizAttempt"]').should('not.exist')
        cy.get('[data-cy="closeQuizAttempt"]').should('be.enabled')
    });

    it('can retake a graded and passed quiz when quizMultipleTakes=true', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.setQuizMultipleTakes(1, true);

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}], 'My Answer')
        cy.gradeQuizAttempt(1, true)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').should( 'not.contain', 'You will earn')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '1 / Unlimited')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer # 1')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
    });

    it('Input Text validation: max num chars', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        const str = 'a'.repeat(2000)
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"] .toastui-editor-contents').invoke('text', str)
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('b')
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 must not exceed 2,000 characters')
        cy.clickCompleteQuizBtn()
        cy.wait(1000)
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 must not exceed 2,000 characters')

        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('{backspace}')
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
    });

    it('Input Text validation: custom description validation', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('some jabberwocky')
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky')

        cy.clickCompleteQuizBtn()
        cy.wait(1000)
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky')

        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('{backspace}')
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')
        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')
    });

    it('text value is only saved if validation passes', () => {
        cy.intercept('POST', '/api/quizzes/quiz1/attempt/*/answers/*').as('reportAnswer')
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('jabberwoc')
        cy.wait(2000)
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('k')
        cy.wait('@reportAnswer')
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('y')
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky')

        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('{backspace}')
        cy.wait('@reportAnswer')
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').should('not.be.visible')

        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('y')
        cy.get('[data-cy="question_1"] [data-cy="descriptionError"]').contains('Answer to question #1 - paragraphs may not contain jabberwocky')

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"] .toastui-editor-main-container').contains('jabberwock')
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"] .toastui-editor-main-container').contains('jabberwocky').should('not.exist')
    });

    it('Input Text validation: validation endpoint only called for question being updated', () => {
        cy.intercept({ method: 'POST', url:'/api/validation/description*'}, (req) => {
            if (req.body.value.includes('Answer to question #1')) {
                req.alias = 'validateDescriptionAnswer1'
            }
            if (req.body.value.includes('Answer to question #2')) {
                req.alias = 'validateDescriptionAnswer2'
            }
        });

        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer to question #1')
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer to question #2')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('X')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Y')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Z')
        cy.wait(1000)
        cy.get('@validateDescriptionAnswer1.all').should('have.length', 1)
        cy.get('@validateDescriptionAnswer2.all').should('have.length', 4)
    });

    it('Input Text validation: validation endpoint is always called for invalid answers', () => {
        cy.intercept({ method: 'POST', url:'/api/validation/description*'}, (req) => {
            if (req.body.value.includes('Answer to question #1')) {
                req.alias = 'validateDescriptionAnswer1'
            }
            if (req.body.value.includes('Answer to question #2 jabberwock')) {
                req.alias = 'validateDescriptionAnswer2'
            }
        });

        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer to question #1')
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer to question #2 jabberwoc')
        cy.wait(3000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('ky')
        cy.get('[data-cy="question_2"] [data-cy="descriptionError"]').contains('Answer to question #2 - paragraphs may not contain jabberwocky')

        cy.wait('@validateDescriptionAnswer1')
        cy.wait('@validateDescriptionAnswer2')
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 2)
        // can be 1 or two depending on order of execution
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lt', 3)

        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('X')
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 2)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 2)
    });

    it('Input Text validation: validation endpoint is always called on submit', () => {
        cy.intercept({ method: 'POST', url:'/api/validation/description*'}, (req) => {
            if (req.body.value.includes('Answer to question #1')) {
                req.alias = 'validateDescriptionAnswer1'
            }
            if (req.body.value.includes('Answer to question #2')) {
                req.alias = 'validateDescriptionAnswer2'
            }
        });

        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer to question #1')
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer to question #2')

        cy.clickCompleteQuizBtn()

        // validation called once when user typed answer, and again on submit
        cy.get('@validateDescriptionAnswer1.all').should('have.length', 2)
        cy.get('@validateDescriptionAnswer2.all').should('have.length', 1)
    });

    it('Input Text validation: answer cache is reset when starting a quiz (after refresh), and validation endpoint called for all questions', () => {
        cy.intercept({ method: 'POST', url:'/api/validation/description*'}, (req) => {
            if (req.body.value.includes('Answer to question #1')) {
                req.alias = 'validateDescriptionAnswer1'
            }
            if (req.body.value.includes('Answer to question #2')) {
                req.alias = 'validateDescriptionAnswer2'
            }
        });

        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer to question #1')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer to question #2')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('X')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Y')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Z')
        cy.wait(1000)
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 2)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 5)

        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Z')
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 2)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 6)

        // reload the page, all answers are revalidated on load (or visit?)
        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]')
        cy.wait('@validateDescriptionAnswer1')
        cy.wait('@validateDescriptionAnswer2')
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 3)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 7)

        // update answer 2 and only answer 2 gets revalidated
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Z')
        cy.wait('@validateDescriptionAnswer2')
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 3)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 8)
    });

    it('Input Text validation: answer cache is reset when starting a quiz (after navigating away), and validation endpoint called for all questions', () => {
        cy.intercept({ method: 'POST', url:'/api/validation/description*'}, (req) => {
            if (req.body.value.includes('Answer to question #1')) {
                req.alias = 'validateDescriptionAnswer1'
            }
            if (req.body.value.includes('Answer to question #2')) {
                req.alias = 'validateDescriptionAnswer2'
            }
        });

        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)

        cy.createQuizDef(2);
        cy.createTextInputQuestionDef(2, 1)
        cy.createTextInputQuestionDef(2, 2)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #1! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer to question #1')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer to question #2')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('X')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Y')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Z')
        cy.wait(1000)
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 2)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 5)

        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Z')
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 2)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 6)

        cy.visit('/progress-and-rankings/quizzes/quiz2');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '2')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '0 / Unlimited')

        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizDescription"]').contains('What a cool quiz #2! Thank you for taking it!')

        cy.get('[data-cy="cancelQuizAttempt"]').should('be.enabled')
        cy.get('[data-cy="startQuizAttempt"]').should('be.enabled')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer to question #1')
        cy.wait(1000)
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer to question #2')

        // one more for each question
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 3)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 7)

        // update answer 2 and only answer 2 gets revalidated
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Z')
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 3)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 8)

        // navigate back to quiz 1, all answers are revalidated on load
        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="myProgressTitle"]').contains('Quiz')
        cy.wait('@validateDescriptionAnswer1')
        cy.wait('@validateDescriptionAnswer2')
        cy.wait(1000)
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 4)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 8)

        // update answer 2 and only answer 2 gets revalidated
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Z')
        cy.wait('@validateDescriptionAnswer2')
        cy.get('@validateDescriptionAnswer1.all').should('have.length.lte', 4)
        cy.get('@validateDescriptionAnswer2.all').should('have.length.lte', 9)
    });

});


