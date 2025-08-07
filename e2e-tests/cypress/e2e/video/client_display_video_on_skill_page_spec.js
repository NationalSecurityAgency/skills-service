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

describe('Display Video on Skill Page Tests', () => {

    const testVideo = '/static/videos/create-quiz.mp4'
    beforeEach(() => {
    });

    it('display video on skill page', () => {
        cy.intercept('GET', '/api/projects/proj1/skills/skill1/videoCaptions')
            .as('getVideoCaptions');
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1, description: 'blah blah'})
        const vidAttr = { videoUrl: testVideo, captions: 'some', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, description: 'blah blah', selfReportingType: 'Video' });
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.wait('@getVideoCaptions').its('response.body').should('include', 'some')

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)
        cy.get('[data-cy="markdownViewer"]').contains('blah blah')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
    });

    it('captions are only loaded if they were configured for that skill', () => {
        cy.intercept('GET', '/api/projects/proj1/skills/skill1/videoCaptions')
            .as('getSkill1VideoCaptions');
        cy.intercept('GET', '/api/projects/proj1/skills/skill1/videoCaptions', cy.spy().as('getSkill1VideoCaptionsWithSpy'))
        cy.intercept('GET', '/api/projects/proj1/skills/skill2/videoCaptions', cy.spy().as('getSkill2VideoCaptionsWithSpy'))
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        cy.createSkill(1, 1, 2, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, captions: 'some', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.saveVideoAttrs(1, 2, {  videoUrl: testVideo })
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.wait('@getSkill1VideoCaptions').its('response.body').should('include', 'some')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')

        cy.get('@getSkill1VideoCaptionsWithSpy').should('have.been.called');
        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"]').click()
        cy.cdClickSkill(1)
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.wait(5000)
        cy.get('@getSkill2VideoCaptionsWithSpy').should('not.have.been.called');
    });

    it('transcript is only loaded if it was configured ', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        cy.createSkill(1, 1, 2, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.saveVideoAttrs(1, 2, {  videoUrl: testVideo })
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"]').click()
        cy.cdClickSkill(1)
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('not.exist')
    });

    it('ability to view transcript', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')

        cy.get('[data-cy="videoTranscript"]').should('not.exist')
        cy.get('[data-cy="viewTranscriptBtn"]').click()
        cy.get('[data-cy="videoTranscript"]').should('have.text', 'another')
        cy.get('[data-cy="certifyTranscriptReadCheckbox"]').should('not.exist')
        cy.get('[data-cy="claimPtsByReadingTranscriptBtn"]').should('not.exist')
    });

    it('ability to view transcript on an achieved skill', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser') })

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')

        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')

        cy.get('[data-cy="videoTranscript"]').should('not.exist')
        cy.get('[data-cy="viewTranscriptBtn"]').click()
        cy.get('[data-cy="videoTranscript"]').should('have.text', 'another')
        cy.get('[data-cy="certifyTranscriptReadCheckbox"]').should('not.exist')
        cy.get('[data-cy="claimPtsByReadingTranscriptBtn"]').should('not.exist')
    });

    it('ability to achieve skill by reading the transcript', () => {
        cy.intercept('POST', '/api/projects/proj1/skills/skill1').as('reportSkill1')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'read me' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1, selfReportingType: 'Video'})
        // cy.doReportSkill({ project: 1, skill: 1, subjNum: 1, userId: Cypress.env('proxyUser') })

        cy.cdVisit('/subjects/subj1/skills/skill1');
        // cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')

        cy.get('[data-cy="videoTranscript"]').should('not.exist')
        cy.get('[data-cy="viewTranscriptBtn"]').click()
        cy.get('[data-cy="videoTranscript"]').should('have.text', 'read me')
        cy.get('[data-cy="certifyTranscriptReadCheckbox"]').should('not.be.checked')
        cy.get('[data-cy="claimPtsByReadingTranscriptBtn"]').should('be.disabled')
        cy.get('[data-cy="certifyTranscriptReadCheckbox"] input').click({force: true})
        cy.get('[data-cy="claimPtsByReadingTranscriptBtn"]').should('be.enabled')

        cy.get('[data-cy="claimPtsByReadingTranscriptBtn"]').click()
        cy.get('[data-cy="certifyTranscriptReadCheckbox"]').should('not.exist')
        cy.get('[data-cy="claimPtsByReadingTranscriptBtn"]').should('not.exist')
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 100 points')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
        cy.wait('@reportSkill1')

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="viewTranscriptBtn"]').click()
        cy.get('[data-cy="videoTranscript"]').should('have.text', 'read me')
        cy.get('[data-cy="watchVideoAlert"]').should('not.exist')
        cy.get('[data-cy="certifyTranscriptReadCheckbox"]').should('not.exist')
        cy.get('[data-cy="claimPtsByReadingTranscriptBtn"]').should('not.exist')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
    });

    it('points messages and progress is only shown when skill is selfReport=Video', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        cy.createSkill(1, 1, 2, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.saveVideoAttrs(1, 2, {  videoUrl: testVideo })
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)

        cy.get('[data-cy="skillsDisplayBreadcrumbBar"] [data-cy="breadcrumb-subj1"]').click()
        cy.cdClickSkill(1)
        cy.get('[data-cy="skillVideo-skill2"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"]').should('not.exist')
    });

    it('achieve skill by watching external video', () => {
        cy.intercept('POST', '/api/projects/proj1/skills/skill1').as('reportSkill1')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(15000)
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 100 points')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
        cy.wait('@reportSkill1')
    });

    it('achieve skill by watching skilltree hosted video', () => {
        cy.intercept('POST', '/api/projects/proj1/skills/skill1').as('reportSkill1')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        const vidAttr = { file: 'create-subject.webm', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(8000)
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 100 points')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
        cy.wait('@reportSkill1')
    });

    it('skill is only achieved for self-report-type of Video', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"]').should('not.exist')

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(18000) // 15-second video but just to be sure added extra 3 seconds
        cy.get('[data-cy="watchVideoMsg"]').should('not.exist')
        cy.get('[data-cy="overallPointsEarnedCard"] [data-cy="mediaInfoCardTitle"]').should('have.text', '0 Total')
    });

    it('skill with unmet prerequisites will not allow to play the video', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        cy.createSkill(1, 1, 2, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.addLearningPathItem(1, 2, 1)

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="videoIsLockedMsg"]').contains('Complete this skill\'s prerequisites to unlock the video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"]').should('not.exist')
        cy.get('[data-cy="viewTranscriptBtn"]').should('not.exist')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"]').should('not.exist')
    });

    it('skill with met prerequisites will allow to play the video', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        cy.createSkill(1, 1, 2, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, selfReportingType: 'Video' });

        cy.addLearningPathItem(1, 2, 1)
        cy.doReportSkill({ project: 1, skill: 2, subjNum: 1, userId: Cypress.env('proxyUser') })

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)
        cy.get('[data-cy="videoIsLockedMsg"]').should('not.exist')
    });

    it('watch video when subject has insufficient points', () => {
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        const vidAttr = { videoUrl: testVideo, transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, pointIncrement: 33, selfReportingType: 'Video' });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(15000)
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 33 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 100)
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('0 / 33 Points')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 33 points for the skill by watching this Video')
        cy.get('[data-cy="videoError"]').contains('Insufficient project points')
    });

    it('video can require multiple watches to complete', () => {
        cy.intercept('POST', '/api/projects/proj1/skills/skill1').as('reportSkill1')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 4})
        const vidAttr = { file: 'create-subject.webm', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 4, pointIncrement: 50, pointIncrementInterval: 0, selfReportingType: 'Video' });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 50 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(8000)
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 50 points! But you can still earn more points by watching the Video again')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('50 / 200 Points')
        cy.wait('@reportSkill1')

        cy.get('.vjs-play-control').click()
        cy.wait(8000)
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 50 points! But you can still earn more points by watching the Video again')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 200 Points')
        cy.wait('@reportSkill1')

        cy.get('.vjs-play-control').click()
        cy.wait(8000)
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 50 points! But you can still earn more points by watching the Video again')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('150 / 200 Points')
        cy.wait('@reportSkill1')

        cy.get('.vjs-play-control').click()
        cy.wait(8000)
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 50 points and completed the skill!')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('200 / 200 Points')
        cy.wait('@reportSkill1')
    });

    it('notified if attempting to get points outside of time window', () => {
        cy.intercept('POST', '/api/projects/proj1/skills/skill1').as('reportSkill1')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 4})
        const vidAttr = { file: 'create-subject.webm', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 4, pointIncrement: 50, selfReportingType: 'Video' });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 50 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(8000)
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 50 points! But you can still earn more points by watching the Video again')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('50 / 200 Points')
        cy.wait('@reportSkill1')

        cy.get('.vjs-play-control').click()
        cy.wait(8000)
        cy.get('[data-cy="videoError"]').contains('This skill was already performed within the configured time period (within the last 8 hours)')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('50 / 200 Points')

    });

    it('notified if video has reached max points', () => {
        cy.intercept('POST', '/api/projects/proj1/skills/skill1').as('reportSkill1')
        cy.createProject(1)
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, {numPerformToCompletion : 1})
        const vidAttr = { file: 'create-subject.webm', transcript: 'another' }
        cy.saveVideoAttrs(1, 1, vidAttr)
        cy.createSkill(1, 1, 1, { numPerformToCompletion : 1, pointIncrement: 100, selfReportingType: 'Video' });

        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]')
        cy.get('[data-cy="viewTranscriptBtn"]').should('be.enabled')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('Earn 100 points for the skill by watching this Video')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', 0)

        cy.get('[data-cy="skillVideo-skill1"] [data-cy="videoPlayer"] [title="Play Video"]').click()
        cy.wait(8000)
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').contains('You just earned 100 points and completed the skill!')
        cy.get('[data-cy="viewTranscriptBtn"]')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')
        cy.wait('@reportSkill1')

        cy.get('.vjs-play-control').click()
        cy.wait(8000)
        cy.get('[data-cy="videoError"]').contains('This skill reached its maximum points')
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').contains('100 / 100 Points')

    });
});
