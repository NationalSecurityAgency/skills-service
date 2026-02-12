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

describe('Skills Display Run Quizzes With Text Input Questions', () => {

    beforeEach(() => {
    });

    it('run quiz with 1 text input', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

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
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer # 1')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').should('be.enabled').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should( 'have.text', '0 Total')
        cy.get('[data-cy="quizRequiresGradingMsg"]')
        cy.get('[data-cy="takeQuizMsg"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="approvalHistoryTimeline"]').should('not.exist')
    });

    it('run quiz with multiple questions where 1 is text input', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
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
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer # 1')
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').should('be.enabled').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should( 'have.text', '0 Total')
        cy.get('[data-cy="quizRequiresGradingMsg"]')
        cy.get('[data-cy="takeQuizMsg"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="approvalHistoryTimeline"]').should('not.exist')
    });

    it('run quiz with multiple questions and multiple Text Input questions', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)
        cy.createTextInputQuestionDef(1, 4)
        cy.createTextInputQuestionDef(1, 5)

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
        cy.get('[data-cy="question_2"] [data-cy="markdownEditorInput"]').type('Answer # 1')
        cy.get('[data-cy="question_3"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="question_4"] [data-cy="markdownEditorInput"]').type('Answer # 2')
        cy.get('[data-cy="question_5"] [data-cy="markdownEditorInput"]').type('Answer # 3')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').should('be.enabled').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should( 'have.text', '0 Total')
        cy.get('[data-cy="quizRequiresGradingMsg"]')
        cy.get('[data-cy="takeQuizMsg"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="approvalHistoryTimeline"]').should('not.exist')
    });

    it('show input text needs grading when quizAlwaysShowCorrectAnswers property is enabled', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createQuizQuestionDef(1, 3)
        cy.setQuizShowCorrectAnswers(1, true);

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

        cy.get('[data-cy="quizRunQuestions"] [data-cy="closeQuizBtn"]').should('be.enabled').click()
        cy.get('[data-cy="skillProgressTitle"]').contains('Very Great Skill 1')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should( 'have.text', '0 Total')
        cy.get('[data-cy="quizRequiresGradingMsg"]')
        cy.get('[data-cy="takeQuizMsg"]').should('not.exist')
        cy.get('[data-cy="takeQuizBtn"]').should('not.exist')
        cy.get('[data-cy="approvalHistoryTimeline"]').should('not.exist')
    });

    it('cannot start previously taken quiz that needs grading', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}], 'My Answer')

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
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

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')
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

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}], 'My Answer')
        cy.gradeQuizAttempt(1, true)

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="quizSplashScreen"]').should( 'not.contain', 'You will earn')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numQuestions"]').should('have.text', '1')
        cy.get('[data-cy="quizSplashScreen"] [data-cy="quizInfoCard"] [data-cy="numAttempts"]').should('have.text', '1 / Unlimited')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="markdownEditorInput"]').type('Answer # 1')

        cy.clickCompleteQuizBtn()

        cy.get('[data-cy="quizRequiresGradingMsg"]').should( 'exist' )
        cy.get('[data-cy="quizCompletion"]').should( 'not.contain', 'Congrats!!')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        cy.get('[data-cy="quizCompletion"] [data-cy="closeQuizBtn"]').click()
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(0)
            .should('contain.text', 'Awaiting Grading')
        cy.get('[data-cy="approvalHistoryTimeline"]')
            .children('.p-timeline-event')
            .eq(1)
            .should('contain.text', 'Passed')
    });

    it('Input Text validation: max num chars', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

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

    it('display pending grading status on subject page', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.createQuizDef(2);
        cy.createQuizQuestionDef(2, 1)

        cy.createQuizDef(3);
        cy.createQuizQuestionDef(3, 1)

        cy.createProject(1)
        cy.createSubject(1, 1)
        cy.createSkill(1, 1, 1, {
            selfReportingType: 'Quiz',
            quizId: 'quiz1',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });
        cy.createSkill(1, 1, 2, {
            selfReportingType: 'Quiz',
            quizId: 'quiz2',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });
        cy.createSkill(1, 1, 3, {
            selfReportingType: 'Quiz',
            quizId: 'quiz3',
            pointIncrement: '150',
            numPerformToCompletion: 1
        });

        cy.runQuizForTheCurrentUser(1, [{selectedIndex: [0]}], 'My Answer')
        cy.runQuizForTheCurrentUser(2, [{selectedIndex: [1]}])
        cy.runQuizForTheCurrentUser(3, [{selectedIndex: [0]}])

        cy.cdVisit('/subjects/subj1');
        cy.get('[data-cy="skillProgress_index-0"]')
        cy.get('[data-cy="skillProgress_index-1"]')
        cy.get('[data-cy="skillProgress_index-2"]')

        cy.get('[data-cy="skillProgress_index-0"] [data-cy="requiresGrading"]')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="requiresGrading"]').should('not.exist')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="requiresGrading"]').should('not.exist')
    })

});


