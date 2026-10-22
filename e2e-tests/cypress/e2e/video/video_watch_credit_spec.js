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

describe('Video Watch Credit Regression Tests', () => {
    beforeEach(() => {
        cy.createProject(1);
        cy.createSubject(1, 1);
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1 });
        cy.saveVideoAttrs(1, 1, { videoUrl: '/static/videos/create-quiz.mp4' });
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 1, selfReportingType: 'Video' });
        cy.cdVisit('/subjects/subj1/skills/skill1');
    });

    [
        { name: 'award credit after watching the entire video with regular progress events', delayProgressEvents: false },
        { name: 'award credit after watching the entire video with delayed progress events', delayProgressEvents: true },
        { name: 'award credit when the first progress events are delayed', delayProgressEvents: true, delayFrom: 0, delayTo: 3 },
    ].forEach(({ name, delayProgressEvents, delayFrom = 2, delayTo = 5 }) => {
        it(name, () => {
            const progressDelivery = { suppressed: 0, beforeGap: null, afterGap: null };
            cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', '0');
            cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '0 / 100 Points');

            cy.get('[data-cy="videoPlayer"] video').then(($video) => {
                const video = $video[0];
                video.addEventListener('seeking', cy.spy().as('videoSeeking'));

                if (delayProgressEvents) {
                    // Model delayed progress delivery while the real media keeps playing.
                    // Suppress events in the configured interval before Video.js receives them;
                    // the next event exposes a playback-position gap without a seek.
                    const delayProgress = (event) => {
                        if (video.currentTime < delayFrom) {
                            progressDelivery.beforeGap = video.currentTime;
                        } else if (video.currentTime < delayTo) {
                            progressDelivery.suppressed += 1;
                            event.stopImmediatePropagation();
                        } else {
                            progressDelivery.afterGap = video.currentTime;
                            video.removeEventListener('timeupdate', delayProgress, true);
                        }
                    };
                    video.addEventListener('timeupdate', delayProgress, true);
                }
            });

            cy.get('[data-cy="videoPlayer"] [title="Play Video"]').click();
            cy.get('[data-cy="videoPlayer"] video', { timeout: 30000 })
                .should('have.prop', 'ended', true)
                .then(($video) => {
                    const video = $video[0];
                    // Verify actual playback covered the whole clip, including the
                    // interval where progress events were withheld from the app.
                    expect(video.played.length, 'one continuous played range').to.equal(1);
                    expect(video.played.start(0), 'playback started at the beginning').to.be.closeTo(0, 0.25);
                    expect(video.played.end(0), 'playback reached the end').to.be.closeTo(video.duration, 0.25);

                    if (delayProgressEvents) {
                        expect(progressDelivery.suppressed, 'progress events were suppressed').to.be.greaterThan(0);
                        expect(progressDelivery.afterGap, 'progress resumed after the gap').to.be.a('number');
                        if (delayFrom > 0) {
                            expect(progressDelivery.beforeGap, 'progress was delivered before the gap').to.be.a('number');
                            expect(progressDelivery.afterGap - progressDelivery.beforeGap, 'delayed playback-position update')
                                .to.be.greaterThan(delayTo - delayFrom);
                        } else {
                            expect(progressDelivery.afterGap, 'first delivered playback position').to.be.at.least(delayTo);
                        }
                    }
                });
            cy.get('@videoSeeking').should('not.have.been.called');
            cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '100 / 100 Points');
            cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').should('contain.text', 'You just earned 100 points');
        });
    });

    const skipToEnd = () => {
        cy.get('[data-cy="videoPlayer"] video').should(($video) => {
            const video = $video[0];
            expect(video.paused, 'video is playing').to.equal(false);
            expect(video.currentTime, 'playback started').to.be.greaterThan(1);
            expect(video.currentTime, 'there is unwatched content to skip').to.be.lessThan(video.duration - 3);
        }).then(($video) => {
            const video = $video[0];
            video.currentTime = video.duration - 0.5;
        });
        cy.get('[data-cy="videoPlayer"] video').should('have.prop', 'ended', true);
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should(($percent) => {
            expect(Number($percent.text()), 'most of the video was skipped').to.be.lessThan(50);
        });
    };

    it('do not award credit for seeking past unwatched content', () => {
        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').click();
        skipToEnd();
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '0 / 100 Points');
        cy.get('[data-cy="watchVideoAlert"] [data-cy="watchVideoMsg"]').should('contain.text', 'Earn 100 points');
    });

    it('award credit after pausing and rewatching an already played section', () => {
        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').click();
        cy.get('[data-cy="videoPlayer"] video').should(($video) => {
            expect($video[0].currentTime, 'initial section was watched').to.be.greaterThan(3);
        }).then(($video) => {
            $video[0].pause();
        });
        cy.get('[data-cy="videoPlayer"] video').should('have.prop', 'paused', true).then(($video) => {
            $video[0].currentTime = 1;
        });
        cy.get('[data-cy="videoPlayer"] video').should(($video) => {
            expect($video[0].seeking, 'rewind finished').to.equal(false);
            expect($video[0].currentTime, 'rewound to an already watched section').to.be.closeTo(1, 0.1);
        });
        cy.get('[data-cy="videoPlayer"] .vjs-play-control').click();
        cy.get('[data-cy="videoPlayer"] video', { timeout: 30000 }).should('have.prop', 'ended', true);
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '100 / 100 Points');
        cy.get('[data-cy="skillVideo-skill1"] [data-cy="percentWatched"]').should('have.text', '100');
    });

    it('require a new complete watch for replay credit', () => {
        cy.createSkill(1, 1, 1, { numPerformToCompletion: 2, pointIncrementInterval: 0, selfReportingType: 'Video' });
        cy.cdVisit('/subjects/subj1/skills/skill1');
        cy.get('[data-cy="videoPlayer"] [title="Play Video"]').click();
        cy.get('[data-cy="videoPlayer"] video', { timeout: 30000 }).should('have.prop', 'ended', true);
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '100 / 200 Points');

        cy.get('[data-cy="videoPlayer"] .vjs-play-control').click();
        skipToEnd();
        cy.get('[data-cy="skillProgress-ptsOverProgressBard"]').should('contain.text', '100 / 200 Points');
    });
});
