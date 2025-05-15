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

describe('Display Video on Subject Page Tests', () => {

    const testVideo = '/static/videos/create-quiz.mp4'
    beforeEach(() => {
    });

    it('display video on subject page', () => {
        const extraCommonAttr = {numPerformToCompletion : 1, description: 'blah blah'}
        cy.intercept('GET', '/api/projects/proj1/skills/skill1/videoCaptions')
            .as('getSkill1VideoCaptions');
        cy.intercept('GET', '/api/projects/proj1/skills/skill2/videoCaptions')
            .as('getSkill2VideoCaptions');
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, extraCommonAttr)
        cy.createSkill(1, 1, 2, extraCommonAttr)
        cy.createSkill(1, 1, 3, extraCommonAttr)
        cy.saveVideoAttrs(1, 1, { videoUrl: testVideo, captions: 'some1', transcript: 'blah blah' })
        cy.saveVideoAttrs(1, 2, { videoUrl: testVideo, captions: 'some2', transcript: 'blah blah' })
        cy.saveVideoAttrs(1, 3, { videoUrl: testVideo })
        cy.createSkill(1, 1, 1, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.createSkill(1, 1, 3, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.cdVisit('/subjects/subj1/');
        cy.get('[data-cy=toggleSkillDetails]').click();
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="selfReportApprovalTag"]').contains('Video')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="selfReportApprovalTag"]').should('not.exist')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="selfReportApprovalTag"]').contains('Video')

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"]').should('not.exist')

        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="watchVideoAlert"]').should('not.exist')

        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"]').should('not.exist')

        // expand first skill
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').click()
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="viewTranscriptBtn"]').should('be.enabled')

        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="watchVideoAlert"]').should('not.exist')

        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"]').should('not.exist')
        cy.wait('@getSkill1VideoCaptions').its('response.body').should('include', 'some1')

        // expand second skill
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').click()
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="watchVideoAlert"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.wait('@getSkill2VideoCaptions').its('response.body').should('include', 'some2')

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')

        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"]').should('not.exist')

        // expand third skill
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').click()
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="percentWatched"]').should('have.text', 0)
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="viewTranscriptBtn"]').should('not.exist')

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"] [title="Play Video"]')
    });

    it('play external video on subject page', () => {
        cy.intercept('POST', '/api/projects/proj1/skills/skill3').as('reportSkill3')
        const extraCommonAttr = {numPerformToCompletion : 1, description: 'blah blah', pointIncrement: 33}
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, extraCommonAttr)
        cy.createSkill(1, 1, 2, extraCommonAttr)
        cy.createSkill(1, 1, 3, extraCommonAttr)
        cy.createSkill(1, 1, 4, extraCommonAttr)
        cy.saveVideoAttrs(1, 1, { videoUrl: testVideo, captions: 'some1', transcript: 'blah blah' })
        cy.saveVideoAttrs(1, 2, { videoUrl: testVideo, captions: 'some2', transcript: 'blah blah' })
        cy.saveVideoAttrs(1, 3, { videoUrl: testVideo })
        cy.createSkill(1, 1, 1, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.createSkill(1, 1, 2, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.createSkill(1, 1, 3, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.cdVisit('/subjects/subj1/');
        cy.get('[data-cy=toggleSkillDetails]').click();

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"]').should('not.exist')

        // expand 2nd video
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').click()
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 33 points for the skill by watching this Video')

        // expand 3rd skill
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').click()
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 33 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="percentWatched"]').should('have.text', 0)
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="viewTranscriptBtn"]').should('not.exist')

        // play 3rd video
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(15000)
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"]').contains('You just earned 33 points')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 33 Points')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 33 Points')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('33 / 33 Points')
        cy.get('[data-cy="overallPointsEarnedToday"]').contains('33')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 33 points for the skill by watching this Video')
        cy.wait('@reportSkill3')

        // 1st is still collapsed
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"]').should('not.exist')
    });

    it('play skilltree hosted video on subject page', () => {
        cy.intercept('POST', '/api/projects/proj1/skills/skill3').as('reportSkill3')
        const extraCommonAttr = {numPerformToCompletion : 1, description: 'blah blah', pointIncrement: 33}
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, extraCommonAttr)
        cy.createSkill(1, 1, 2, extraCommonAttr)
        cy.createSkill(1, 1, 3, extraCommonAttr)
        cy.createSkill(1, 1, 4, extraCommonAttr)
        cy.saveVideoAttrs(1, 1, { file: 'create-project.webm', captions: 'some1', transcript: 'blah blah' })
        cy.saveVideoAttrs(1, 2, { file: 'create-quiz.mp4', captions: 'some2', transcript: 'blah blah' })
        cy.saveVideoAttrs(1, 3, { file: 'create-subject.webm' })
        cy.createSkill(1, 1, 1, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.createSkill(1, 1, 2, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.createSkill(1, 1, 3, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.cdVisit('/subjects/subj1/');
        cy.get('[data-cy=toggleSkillDetails]').click();

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"]').should('not.exist')

        // expand 2nd video
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').click()
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 33 points for the skill by watching this Video')

        // expand 3rd skill
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').click()
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 33 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="percentWatched"]').should('have.text', 0)
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="viewTranscriptBtn"]').should('not.exist')

        // play 3rd video
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(8000)
        cy.get('[data-cy="skillVideo-skill3"] [data-cy="watchVideoAlert"]').contains('You just earned 33 points')
        cy.get('[data-cy="skillProgress_index-0"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 33 Points')
        cy.get('[data-cy="skillProgress_index-1"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 33 Points')
        cy.get('[data-cy="skillProgress_index-2"] [data-cy="skillProgress-ptsOverProgressBard"]').contains('33 / 33 Points')
        cy.get('[data-cy="overallPointsEarnedToday"]').contains('33')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 33 points for the skill by watching this Video')
        cy.wait('@reportSkill3')

        // 1st is still collapsed
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"]').should('not.exist')
    });

    it('skill with unmet prerequisites will not allow to play the video', () => {
        const extraCommonAttr = {numPerformToCompletion : 1, description: 'blah blah', pointIncrement: 33}
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, extraCommonAttr)
        cy.createSkill(1, 1, 2, extraCommonAttr)
        cy.createSkill(1, 1, 3, extraCommonAttr)
        cy.createSkill(1, 1, 4, extraCommonAttr)
        cy.saveVideoAttrs(1, 1, { videoUrl: testVideo, captions: 'some1', transcript: 'blah blah' })
        cy.saveVideoAttrs(1, 2, { videoUrl: testVideo, captions: 'some2', transcript: 'blah blah' })
        cy.saveVideoAttrs(1, 3, { videoUrl: testVideo })
        cy.createSkill(1, 1, 1, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.createSkill(1, 1, 2, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.createSkill(1, 1, 3, { ...extraCommonAttr, selfReportingType: 'Video' });
        cy.addLearningPathItem(1, 2, 1)

        cy.cdVisit('/subjects/subj1/');
        cy.get('[data-cy=toggleSkillDetails]').click();
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').click()
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoIsLockedMsg"]').contains('Complete this skill\'s prerequisites to unlock the video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"]').should('not.exist')

        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').click()
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoCollapsed"] [data-cy="expandVideoBtn"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"] [title="Play Video"]')
    });
});
