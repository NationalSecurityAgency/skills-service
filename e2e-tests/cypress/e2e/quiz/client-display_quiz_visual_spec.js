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

dayjs.extend(relativeTimePlugin);
dayjs.extend(advancedFormatPlugin);

describe('Client Display Quiz Visual Tests', () => {

    const snapConfig = { errorThreshold: 0.05, blackout: '[data-cy="skillTreePoweredBy"]' }

    it('quiz splash screen', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);

        cy.setQuizMaxNumAttempts(1, 3)
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points for Very Great Skill 1 skill by passing this quiz')

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', snapConfig);
    });

    it('quiz with questions screen', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizMultipleChoiceQuestionDef(1, 4);
        cy.createQuizMultipleChoiceQuestionDef(1, 5);

        cy.setQuizMaxNumAttempts(1, 3)
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_3"]').click()
        cy.get('[data-cy="question_5"] [data-cy="answer_4"]').click().blur()

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', snapConfig);
    });

    it('quiz passed screen', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizMultipleChoiceQuestionDef(1, 4);

        cy.setQuizMaxNumAttempts(1, 3)
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_4"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_4"] [data-cy="answer_2"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizCompletion"]').contains('Congrats!! You just earned 150 points for Very Great Skill 1 skill by passing the quiz.')
        cy.get('[data-cy="numAttemptsInfoCard"]').should('not.exist')

        const snapshotOptions = {
            blackout: '[data-cy="quizRuntime"], [data-cy="skillTreePoweredBy"]',
            errorThreshold: 0.05
        };
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', snapshotOptions);
    });

    it('quiz failed screen', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1);
        cy.createQuizMultipleChoiceQuestionDef(1, 2);
        cy.createQuizQuestionDef(1, 3);
        cy.createQuizMultipleChoiceQuestionDef(1, 4);

        cy.setQuizMaxNumAttempts(1, 1)
        cy.setMinNumQuestionsToPass(1, 2)

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_3"]').click()

        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()
        cy.get('[data-cy="question_4"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_4"] [data-cy="answer_2"]').click()

        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="quizFailed"]')

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', snapConfig);
    });

    it('survey splash screen', () => {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Survey')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points')

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', snapConfig);
    });

    it('survey questions screen', () => {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Survey')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click().blur()

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', snapConfig);
    });

    it('survey completed screen', () => {
        cy.createSurveyDef(1);
        cy.createTextInputQuestionDef(1, 1);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2);
        cy.createSurveyMultipleChoiceQuestionDef(1, 2, { questionType: 'SingleChoice' });

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Survey')
        cy.get('[data-cy="quizSplashScreen"]').contains('You will earn 150 points')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="textInputAnswer"]').type('a')
        cy.get('[data-cy="question_2"] [data-cy="answer_1"]').click()
        cy.get('[data-cy="question_3"] [data-cy="answer_2"]').click()

        // must wait so "Completed In: <N> SECONDS" card renders consistently, otherwise
        // it will sometimes execute in 1 second and the singular form will fail the comparison
        cy.wait(2000)
        cy.get('[data-cy="completeQuizBtn"]').click()
        cy.get('[data-cy="surveyCompletion"]').contains('Congrats!! You just earned 150 points')

        cy.wait(500)
        const snapshotOptions = {
            blackout: '[data-cy="surveyRuntimeCard"], [data-cy="skillTreePoweredBy"]',
            errorThreshold: 0.05
        };
        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', snapshotOptions);
    });

    it('questions support markdown', () => {
        const markdown = '# Title1\n## Title2\n### Title 3\n#### Title 4\n##### Title 5\nTitle 6\n\n' +
            '---\n' +
            '# Emphasis\n' +
            'italics: *italicized* or _italicized_\n\n' +
            'bold: **bolded** or __bolded__\n\n' +
            'combination **_bolded & italicized_**\n\n' +
            'strikethrough: ~~struck~~\n\n' +
            '---\n' +
            '# Inline\n' +
            'Inline `code` has `back-ticks around` it\n\n' +
            '---\n' +
            '# Multiline\n' +
            '\n' +
            '\n' +
            '```\n' +
            'import { SkillsDirective } from \'@skilltree/skills-client-vue\';\n' +
            'Vue.use(SkillsDirective);\n' +
            '```\n' +
            '# Lists\n' +
            'Ordered Lists:\n' +
            '1. Item one\n' +
            '1. Item two\n' +
            '1. Item three (actual number does not matter)\n\n' +
            'If List item has multiple lines of text, subsequent lines must be idented four spaces, otherwise list item numbers will reset, e.g.,\n' +
            '1. item one\n' +
            '    paragrah one\n' +
            '1. item two\n' +
            '1. item three\n' +
            '\n' +
            'Unordered Lists\n' +
            '* Item\n' +
            '* Item\n' +
            '* Item\n' +
            '___\n' +
            '# Links\n' +
            '[in line link](https://www.somewebsite.com)\n' +
            '___\n' +
            '# Blockquotes\n' +
            '> Blockquotes are very handy to emulate reply text.\n' +
            '> This line is part of the same quote.\n\n' +
            '# Horizontal rule\n' +
            'Use three or more dashes, asterisks, or underscores to generate a horizontal rule line\n' +
            '\n' +
            'Separate me\n\n' +
            '___\n\n' +
            'Separate me\n\n' +
            '---\n\n' +
            'Separate me\n\n' +
            '***\n\n' +
            '';

        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, { question: markdown});

        cy.createProject(1)
        cy.createSubject(1,1)
        cy.createSkill(1, 1, 1, { selfReportingType: 'Quiz', quizId: 'quiz1',  pointIncrement: '150', numPerformToCompletion: 1 });

        cy.cdVisit('/subjects/subj1/skills/skill1/quizzes/quiz1');
        cy.get('[data-cy="title"]').contains('Quiz')

        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.get('[data-cy="question_1"] [data-cy="answer_1"]')

        cy.matchSnapshotImageForElement('[data-cy="skillsDisplayHome"]', snapConfig);
    });

});



