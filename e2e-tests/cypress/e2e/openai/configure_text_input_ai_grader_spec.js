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

import {
    newDescWelcomeMsg,
    errMsg,
    stopMsg,
    gotStartedMsg
}
    from './openai_helper_commands'

describe('Configure Text Input AI Grader Tests', () => {

    it('quiz page only shows AI Grader Links if enableOpenAIIntegration property is configured', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.visit('/administrator/quizzes/quiz1');
        cy.get('[data-cy="add-video-question-1"]')
        cy.get('[data-cy="add-video-question-2"]')

        cy.get('[data-cy="ai-grader-question-2"]').should('not.exist')
        cy.get('[data-cy="ai-grader-question-1"]').should('not.exist')
    });

    it('quiz page show AI Grader Links', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)
        cy.createTextInputQuestionDef(1, 4)

        const testVideo = '/static/videos/create-quiz.mp4'
        const vidAttr = { videoUrl: testVideo, captions: 'some', transcript: 'another' }
        cy.request(`/admin/quiz-definitions/quiz1/questions`)
            .then((response) => {
                const questions = response.body.questions
                cy.saveVideoAttrs(1, questions[2].id, vidAttr, true)
                cy.saveQuizTextInputAiGraderConfigs(1, questions[2].id, "correct answer", 66)
                cy.saveQuizTextInputAiGraderConfigs(1, questions[3].id, "correct answer", 66)
            })

        cy.visit('/administrator/quizzes/quiz1');
        cy.wait('@getConfig')
        cy.get('[data-cy="add-video-question-1"]')
        cy.get('[data-cy="add-video-question-2"]')
        cy.get('[data-cy="add-video-question-3"]')
        cy.get('[data-cy="add-video-question-4"]')

        cy.get('[data-cy="ai-grader-question-1"]').should('not.exist') // not text input question
        cy.get('[data-cy="ai-grader-question-2"]')
        cy.get('[data-cy="ai-grader-question-3"]')
        cy.get('[data-cy="ai-grader-question-4"]')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionAiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionAiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionAiGraded"]')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionAiGraded"]')

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionHasVideoOrAudio"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionHasVideoOrAudio"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionHasVideoOrAudio"]')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionHasVideoOrAudio"]').should('not.exist')
    });

    it('configure grader for a question starting from quiz page', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1)
        cy.createTextInputQuestionDef(1, 2)
        cy.createTextInputQuestionDef(1, 3)
        cy.createTextInputQuestionDef(1, 4)

        cy.visit('/administrator/quizzes/quiz1');
        cy.wait('@getConfig')

        cy.get('[data-cy="ai-grader-question-1"]').should('not.exist') // not text input question
        cy.get('[data-cy="ai-grader-question-2"]')
        cy.get('[data-cy="ai-grader-question-3"]')
        cy.get('[data-cy="ai-grader-question-4"]')

        cy.get('[data-cy="ai-grader-question-3"]').click()
        cy.get('[data-cy="aiGraderEnabled"]').click()
        cy.get('[data-cy="answerForGrading"]').type('The answer')
        cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.enabled')
        cy.get('[data-cy="saveGraderSettingsBtn"]').click()
        cy.get('[data-cy="settingsSavedMsg"]')

        cy.get('[data-cy="backToQuestionsPage"]').click()

        cy.get('[data-cy="questionDisplayCard-1"] [data-cy="questionAiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-2"] [data-cy="questionAiGraded"]').should('not.exist')
        cy.get('[data-cy="questionDisplayCard-3"] [data-cy="questionAiGraded"]')
        cy.get('[data-cy="questionDisplayCard-4"] [data-cy="questionAiGraded"]').should('not.exist')
    });

    it('correct answer must be provided validation', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
                conf.maxTextInputAiGradingCorrectAnswerLength = 5;
                res.send(conf);
            });
        }).as('getConfig');
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.request(`/admin/quiz-definitions/quiz1/questions`)
            .then((response) => {
                const questions = response.body.questions
                cy.visit(`/administrator/quizzes/quiz1/questions/${questions[0].id}/ai-grader`);
                cy.wait('@getConfig')

                cy.get('[data-cy="aiGraderEnabled"]').click()
                cy.get('[data-cy="answerForGrading"]').type('a')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.enabled')
                cy.get('[data-cy="answerForGrading"]').type('{backspace}')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.disabled')
                cy.get('[data-cy="answerUsedForGradingError"]').contains('Answer Used for Grading is a required field')

                cy.get('[data-cy="answerForGrading"]').type('12345')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.enabled')
                cy.get('[data-cy="answerForGrading"]').type('6')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.disabled')
                cy.get('[data-cy="answerUsedForGradingError"]').contains('Answer Used for Grading must be at most 5 characters')
            })
    });

});


