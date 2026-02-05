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

    it('configure grader starting from config page', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
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
                cy.get('[data-cy="answerForGrading"]').type('fancy answer')
                cy.get('[data-cy="minConfidenceLevelInput"]').type('{selectAll}33')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.enabled')
                cy.get('[data-cy="saveGraderSettingsBtn"]').click()
                cy.get('[data-cy="settingsSavedMsg"]')

                cy.visit(`/administrator/quizzes/quiz1/questions/${questions[0].id}/ai-grader`);
                cy.get('[data-cy="aiGraderEnabled"] [data-pc-section="input"]').should('be.checked')
                cy.get('[data-cy="answerForGrading"]').should('have.value', 'fancy answer')
                cy.get('[data-cy="minConfidenceLevelInput"] [data-pc-name="pcinputtext"]').should('have.value', '33')
            })
    });

    it('correct answer validation', () => {
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

    it('Minimum Correct Confidence validation', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
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

                cy.get('[data-cy="minConfidenceLevelInput"]').type('{selectAll}{backspace}')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.disabled')
                cy.get('[data-cy="minimumConfidenceLevelError"]').contains('Minimum Correct Confidence % is a required field')

                cy.get('[data-cy="minConfidenceLevelInput"]').type('1')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.enabled')
                cy.get('[data-cy="minimumConfidenceLevelError"]').should('not.exist')

                cy.get('[data-cy="minConfidenceLevelInput"]').type('{selectAll}0')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.disabled')
                cy.get('[data-cy="minimumConfidenceLevelError"]').contains('Minimum Correct Confidence % must be greater than or equal to 1')

                cy.get('[data-cy="minConfidenceLevelInput"]').type('100')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.enabled')
                cy.get('[data-cy="minimumConfidenceLevelError"]').should('not.exist')

                cy.get('[data-cy="minConfidenceLevelInput"]').type('{selectAll}101')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.disabled')
                cy.get('[data-cy="minimumConfidenceLevelError"]').contains('Minimum Correct Confidence % must be less than or equal to 100')

            })
    });

    it('do not show grading config input if ai integration is not enabled', () => {
        cy.createQuizDef(1);
        cy.createTextInputQuestionDef(1, 1)

        cy.request(`/admin/quiz-definitions/quiz1/questions`)
            .then((response) => {
                const questions = response.body.questions
                cy.visit(`/administrator/quizzes/quiz1/questions/${questions[0].id}/ai-grader`);

                cy.get('[data-cy="aiNotEnabled"]')
                cy.get('[data-cy="aiGraderEnabled"]').should('not.exist')
                 cy.get('[data-cy="answerForGrading"]').should('not.exist')
            })
    });

    it('preview ai grading', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
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
                cy.get('[data-cy="answerForGrading"]').type('fancy answer')
                cy.get('[data-cy="saveGraderSettingsBtn"]').should('be.enabled')

                cy.get('[data-cy="testAnswersBtn"]').should('be.disabled')
                cy.get('[data-cy="answerToTestInput"]').type('answer 92')
                cy.get('[data-cy="testAnswersBtn"]').should('be.enabled')

                cy.get('[data-cy="testAnswersBtn"]').click()
                cy.get('[data-cy="gradeResCorrect"]')
                cy.get('[data-cy="gradeResWrong"]').should('not.exist')
                cy.get('[data-cy="gradeResConfidence"]').should('have.text', '92%')
                cy.get('[data-cy="aiConfidenceTag"]').should('have.text', 'Very High')
                cy.get('[data-cy="resJustification"]').should('have.text', 'Your answer has confidence level of 92')

                cy.get('[data-cy="answerToTestInput"]').type('{selectAll}answer 75')
                cy.get('[data-cy="testAnswersBtn"]').click()
                cy.get('[data-cy="gradeResConfidence"]').should('have.text', '75%')
                cy.get('[data-cy="aiConfidenceTag"]').should('have.text', 'High')
                cy.get('[data-cy="resJustification"]').should('have.text', 'Your answer has confidence level of 75')
                cy.get('[data-cy="gradeResCorrect"]')
                cy.get('[data-cy="gradeResWrong"]').should('not.exist')

                cy.get('[data-cy="answerToTestInput"]').type('{selectAll}answer 65')
                cy.get('[data-cy="testAnswersBtn"]').click()
                cy.get('[data-cy="gradeResConfidence"]').should('have.text', '65%')
                cy.get('[data-cy="aiConfidenceTag"]').should('have.text', 'Moderate')
                cy.get('[data-cy="resJustification"]').should('have.text', 'Your answer has confidence level of 65')
                cy.get('[data-cy="gradeResCorrect"]').should('not.exist')
                cy.get('[data-cy="gradeResWrong"]')

                cy.get('[data-cy="answerToTestInput"]').type('{selectAll}answer 51')
                cy.get('[data-cy="testAnswersBtn"]').click()
                cy.get('[data-cy="gradeResConfidence"]').should('have.text', '51%')
                cy.get('[data-cy="aiConfidenceTag"]').should('have.text', 'Low')
                cy.get('[data-cy="resJustification"]').should('have.text', 'Your answer has confidence level of 51')
                cy.get('[data-cy="gradeResCorrect"]').should('not.exist')
                cy.get('[data-cy="gradeResWrong"]')

                cy.get('[data-cy="answerToTestInput"]').type('{selectAll}answer 22')
                cy.get('[data-cy="testAnswersBtn"]').click()
                cy.get('[data-cy="gradeResConfidence"]').should('have.text', '22%')
                cy.get('[data-cy="aiConfidenceTag"]').should('have.text', 'Very Low')
                cy.get('[data-cy="resJustification"]').should('have.text', 'Your answer has confidence level of 22')
                cy.get('[data-cy="gradeResCorrect"]').should('not.exist')
                cy.get('[data-cy="gradeResWrong"]')
            })
    });

    it('preview ai grading validation', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
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

                cy.get('[data-cy="testAnswersBtn"]').should('be.disabled')
                cy.get('[data-cy="answerToTestInput"]').type('a')
                cy.get('[data-cy="testAnswersBtn"]').should('be.enabled')

                cy.get('[data-cy="answerForGrading"]').type('{backspace}')
                cy.get('[data-cy="testAnswersBtn"]').should('be.disabled')
                cy.get('[data-cy="answerUsedForGradingError"]').contains('Answer Used for Grading is a required field')
                cy.get('[data-cy="answerForGrading"]').type('a')
                cy.get('[data-cy="testAnswersBtn"]').should('be.enabled')

                cy.get('[data-cy="answerToTestInput"]').type('{backspace}')
                cy.get('[data-cy="testAnswersBtn"]').should('be.disabled')
            })
    });

    it('preview ai grading failed', () => {
        cy.intercept('GET', '/public/config', (req) => {
            req.reply((res) => {
                const conf = res.body;
                conf.enableOpenAIIntegration = true;
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

                cy.get('[data-cy="testAnswersBtn"]').should('be.disabled')
                cy.get('[data-cy="answerToTestInput"]').type('a')
                cy.get('[data-cy="testAnswersBtn"]').click()

                cy.get('[data-cy="gradingFailedMsg"]')

                cy.get('[data-cy="answerToTestInput"]').type('{selectAll}answer 75')
                cy.get('[data-cy="testAnswersBtn"]').click()
                cy.get('[data-cy="gradeResConfidence"]').should('have.text', '75%')
                cy.get('[data-cy="aiConfidenceTag"]').should('have.text', 'High')

                cy.get('[data-cy="gradingFailedMsg"]').should('not.exist')
            })
    });


});


