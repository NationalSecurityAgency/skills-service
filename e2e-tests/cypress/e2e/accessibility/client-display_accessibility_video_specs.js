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

describe('Client Display Accessibility Video Tests', () => {
    const testVideo = '/static/videos/create-quiz.mp4'

    const runWithDarkMode = ['', ' - dark mode']

    runWithDarkMode.forEach((darkMode) => {
        it(`display video on skill page${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.intercept('GET', '/api/projects/proj1/skills/skill1/videoCaptions')
              .as('getVideoCaptions');
            cy.createProject(1)
            cy.createSubject(1, 1);
            cy.createSkill(1, 1, 1, { numPerformToCompletion: 1, description: 'blah blah' })
            const vidAttr = { videoUrl: testVideo, captions: 'some', transcript: 'another' }
            cy.saveVideoAttrs(1, 1, vidAttr)
            cy.createSkill(1, 1, 1, {
                numPerformToCompletion: 1,
                description: 'blah blah',
                selfReportingType: 'Video'
            });
            cy.cdVisit('/subjects/subj1/skills/skill1');
            cy.wait('@getVideoCaptions').its('response.body').should('include', 'some')

            cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
            cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
            cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="percentWatched"]').should('have.text', 0)
            cy.get('[data-cy="markdownViewer"]').contains('blah blah')
            cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });

        it(`skill with unmet prerequisites will not allow to play the video${darkMode}`, () => {
            cy.setDarkModeIfNeeded(darkMode)
            cy.createProject(1)
            cy.createSubject(1, 1);
            cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 })
            cy.createSkill(1, 1, 2, { numPerformToCompletion: 1 })
            const vidAttr = { videoUrl: testVideo, transcript: 'another' }
            cy.saveVideoAttrs(1, 1, vidAttr)
            cy.createSkill(1, 1, 1, { numPerformToCompletion: 1, selfReportingType: 'Video' });

            cy.addLearningPathItem(1, 2, 1)

            cy.cdVisit('/subjects/subj1/skills/skill1');
            cy.get('[data-cy="videoIsLockedMsg"]').contains('Complete this skill\'s prerequisites to unlock the video')
            cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"]').should('not.exist')
            cy.get('[data-cy="viewTranscriptBtn"]').should('not.exist')
            cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"]').should('not.exist')

            cy.get('[data-cy="skillLink-proj1-skill2"]')
            cy.get('#dependent-skills-network canvas')
            cy.get('[data-cy="depsProgress"] [data-cy="numDeps"]').should('have.text', '1')

            // wait for graph to finish loading
            cy.wait(3000)

            cy.customLighthouse();
            cy.injectAxe();
            cy.customA11y();
        });
    })
});
