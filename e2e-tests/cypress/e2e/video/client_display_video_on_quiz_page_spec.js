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

describe('Display Video on Quiz Page Tests', () => {

    const testVideo = '/static/videos/create-quiz.mp4'
    beforeEach(() => {
    });

    it('display video on quiz question card', () => {
        cy.intercept('GET', '/api/quiz-definitions/quiz1/questions/*/videoCaptions').as('getVideoCaptions');
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null, { videoUrl: testVideo, captions: 'some', transcript: 'another' })
        cy.createQuizQuestionDef(1, 2)
        cy.createQuizMultipleChoiceQuestionDef(1, 3)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.wait('@getVideoCaptions').its('response.body').should('include', 'some')

        cy.get('[data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
    });

    it('captions are only loaded if they were configured for that quiz question', () => {let captionCounter = 0;
        cy.intercept('GET', '/api/quiz-definitions/quiz1/questions/*/videoCaptions').as('getVideoCaptions');

        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { videoUrl: testVideo, captions: 'some', transcript: 'another' })
        cy.createQuizQuestionDef(1, 2, null, {  videoUrl: testVideo })
        cy.createQuizMultipleChoiceQuestionDef(1, 3)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').click()
        cy.wait('@getVideoCaptions').its('response.body').should('include', 'some')

    });

    it('transcript is only loaded if it was configured ', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { videoUrl: testVideo, captions: 'some', transcript: 'another' })
        cy.createQuizQuestionDef(1, 2, null, {  videoUrl: testVideo })
        cy.createQuizMultipleChoiceQuestionDef(1, 3)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').eq(0).should('exist')
        cy.get('[data-cy="viewTranscriptBtn"]').eq(0).should('exist');

        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').eq(1).should('exist')
        cy.get('[data-cy="viewTranscriptBtn"]').eq(1).should('not.exist');
    });

    it('ability to view transcript', () => {
        cy.createQuizDef(1);
        cy.createQuizQuestionDef(1, 1, null,  { videoUrl: testVideo, captions: 'some', transcript: 'another' })
        cy.createQuizQuestionDef(1, 2, null, {  videoUrl: testVideo })
        cy.createQuizMultipleChoiceQuestionDef(1, 3)

        cy.visit('/progress-and-rankings/quizzes/quiz1');
        cy.get('[data-cy="startQuizAttempt"]').click()

        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').eq(0).should('exist')
        cy.get('[data-cy="viewTranscriptBtn"]').should('exist');

        cy.get('[data-cy="videoTranscript"]').should('not.exist')
        cy.get('[data-cy="viewTranscriptBtn"]').click()
        cy.get('[data-cy="videoTranscript"]').should('have.text', 'another')
    });

});
