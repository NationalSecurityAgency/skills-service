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
// var moment = require('moment-timezone');
import WatchedSegmentsUtil from '@/components/video/WatchedSegmentsUtil.js';

describe('WatchedSegmentsUtil', () => {
  it('add first segment', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0, stop: 3 });
    expect(segments).toEqual([{ start: 0, stop: 3 }]);
  });

  it('add 2 non overlapping segment', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0.00, stop: 2.97 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 19.06, stop: 21.86 });
    expect(segments).toEqual([{ start: 0.00, stop: 2.97 }, { start: 19.06, stop: 21.86 }]);
  });

  it('add 2 segment that was paused then restarted, need to account for slight different in stop/start times', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0.00, stop: 3.151571 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 3.210667, stop: 21.86 });
    expect(segments).toEqual([{ start: 0.00, stop: 21.86 }]);
  });

  it('add 2nd overlapping segment - extends the first segment at the end', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0, stop: 3 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 2.01, stop: 4.23 });
    expect(segments).toEqual([{ start: 0, stop: 4.23 }]);
  });

  it('add 2nd overlapping segment - extends the first segment at the start', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 1.2, stop: 3 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0.5, stop: 2 });
    expect(segments).toEqual([{ start: 0.5, stop: 3 }]);
  });

  it('add 2nd overlapping segment - new segment is within an existing segment', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 1.2, stop: 5.2 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 2.1, stop: 4.7 });
    expect(segments).toEqual([{ start: 1.2, stop: 5.2 }]);
  });

  it('add 2nd overlapping segment - new segment matches an existing segment', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 1.2, stop: 5.2 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 1.2, stop: 5.2 });
    expect(segments).toEqual([{ start: 1.2, stop: 5.2 }]);
  });

  it('add 3rd overlapping segment - extends the first segment at the end', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0, stop: 3 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 7, stop: 8.01 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 2.01, stop: 4.23 });
    expect(segments).toEqual([{ start: 0, stop: 4.23 }, { start: 7, stop: 8.01 }]);
  });

  it('add 3rd overlapping segment - extends the first segment at the start', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 1.4, stop: 3 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 6, stop: 8.01 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0.7, stop: 2.3 });
    expect(segments).toEqual([{ start: 0.7, stop: 3 }, { start: 6, stop: 8.01 }]);
  });

  it('add 3rd overlapping segment - extends the second segment at the end', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0, stop: 3 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 6, stop: 8.01 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 6.01, stop: 9.3 });
    expect(segments).toEqual([{ start: 0, stop: 3 }, { start: 6, stop: 9.3 }]);
  });

  it('add 3rd overlapping segment - extends the second segment at the start', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0, stop: 3 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 6, stop: 8.01 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 5.2, stop: 7.3 });
    expect(segments).toEqual([{ start: 0, stop: 3 }, { start: 5.2, stop: 8.01 }]);
  });

  it('add 3rd overlapping segment - extends the second segment at the start and end', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0, stop: 3 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 6, stop: 8.01 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 5.2, stop: 9.53 });
    expect(segments).toEqual([{ start: 0, stop: 3 }, { start: 5.2, stop: 9.53 }]);
  });

  it('add 3rd overlapping segment - merges the 2 segments', () => {
    let segments = [];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 0, stop: 3 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 6, stop: 8.01 });
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 2.5, stop: 7 });
    expect(segments).toEqual([{ start: 0, stop: 8.01 }]);
  });

  it('add 3rd segment - merges all 3 segments', () => {
    let segments = [{ start: 0.21, stop: 6.15 }, { start: 12.04, stop: 15.44 }];
    segments = WatchedSegmentsUtil.addToSegments(segments, { start: 6.59, stop: 11.64 });
    expect(segments).toEqual([{ start: 0.21, stop: 15.44 }]);
  });

  it('updated progress - falls in between 2 watched segments', () => {
    const watchProgress = {
      watchSegments: [{ start: 0.19, stop: 5.67 }, { start: 12.56, stop: 23.98 }],
        currentStart: null,
        lastKnownPosition: null,
        totalWatchTime: 23,
        videoDuration: 45,
        percentWatched: 12,
        currentPosition: 23.98,
    };
    WatchedSegmentsUtil.updateProgress(watchProgress, 8.92);
    expect(watchProgress.currentStart).toEqual(8.92);
    expect(watchProgress.lastKnownPosition).toEqual(8.92);
    expect(watchProgress.watchSegments).toEqual([{ start: 0.19, stop: 5.67 }, { start: 12.56, stop: 23.98 }]);
  });

  it('updated progress - jump back into previous segment', () => {
    const watchProgress = {
      watchSegments: [{ start: 0, stop: 5 }],
      currentStart: 10,
      lastKnownPosition: 20,
      totalWatchTime: 15,
      videoDuration: 100,
      percentWatched: 15,
      currentPosition: 20,
    };
    WatchedSegmentsUtil.updateProgress(watchProgress, 2);
    expect(watchProgress.currentStart).toBeNull();
    expect(watchProgress.lastKnownPosition).toBeNull();
    expect(watchProgress.watchSegments).toEqual([{ start: 0, stop: 5 }, { start: 10, stop: 20 }]);
    expect(watchProgress.totalWatchTime).toEqual(15);
    expect(watchProgress.percentWatched).toEqual(15);
  });

  it('updated progress - if there is already an existing segment then join it', () => {
    const watchProgress = {
      watchSegments: [{ start: 2, stop: 7 }],
      currentStart: 22,
      lastKnownPosition: 23,
      totalWatchTime: 6,
      videoDuration: 100,
      percentWatched: 6,
      currentPosition: 2,
    };
    WatchedSegmentsUtil.updateProgress(watchProgress, 8.01);
    expect(watchProgress.currentStart).toEqual(2);
    expect(watchProgress.lastKnownPosition).toEqual(8.01);
    expect(watchProgress.watchSegments).toEqual([{ start: 22, stop: 23 }]);
    expect(watchProgress.totalWatchTime).toEqual(7.01);
    expect(watchProgress.percentWatched).toEqual(7);
    expect(watchProgress.currentPosition).toEqual(8.01);
  });

  it('updated progress - if there is already an existing segment then join it - within 2 seconds', () => {
    const watchProgress = {
      watchSegments: [{ start: 2, stop: 7 }],
      currentStart: null,
      lastKnownPosition: null,
      totalWatchTime: 5,
      videoDuration: 100,
      percentWatched: 5,
      currentPosition: 2,
    };
    WatchedSegmentsUtil.updateProgress(watchProgress, 8.99);
    expect(watchProgress.currentStart).toEqual(2);
    expect(watchProgress.lastKnownPosition).toEqual(8.99);
    expect(watchProgress.watchSegments).toEqual([]);
    expect(watchProgress.totalWatchTime).toEqual(6.99);
    expect(watchProgress.percentWatched).toEqual(6);
    expect(watchProgress.currentPosition).toEqual(8.99);
  });

  it('updated progress - start new segment if incoming time is 2 seconds after the last segments stop time', () => {
    const watchProgress = {
      watchSegments: [{ start: 2, stop: 7 }],
      currentStart: null,
      lastKnownPosition: null,
      totalWatchTime: 5,
      videoDuration: 100,
      percentWatched: 5,
      currentPosition: 2,
    };
    WatchedSegmentsUtil.updateProgress(watchProgress, 9.01);
    expect(watchProgress.currentStart).toEqual(9.01);
    expect(watchProgress.lastKnownPosition).toEqual(9.01);
    expect(watchProgress.watchSegments).toEqual([{ start: 2, stop: 7 }]);
    expect(watchProgress.totalWatchTime).toEqual(5);
    expect(watchProgress.percentWatched).toEqual(5);
    expect(watchProgress.currentPosition).toEqual(9.01);
  });

  it('updated progress - if there is already an existing segment then join it - make sure later segments are preserved', () => {
    const watchProgress = {
      watchSegments: [{ start: 2, stop: 7 }, { start: 30, stop: 50 }],
      currentStart: null,
      lastKnownPosition: null,
      totalWatchTime: 25,
      videoDuration: 100,
      percentWatched: 25,
      currentPosition: 2,
    };
    WatchedSegmentsUtil.updateProgress(watchProgress, 8.01);
    expect(watchProgress.currentStart).toEqual(2);
    expect(watchProgress.lastKnownPosition).toEqual(8.01);
    expect(watchProgress.watchSegments).toEqual([{ start: 30, stop: 50 }]);
    expect(Math.round(watchProgress.totalWatchTime * 100) / 100).toEqual(26.01);
    expect(watchProgress.percentWatched).toEqual(26);
    expect(watchProgress.currentPosition).toEqual(8.01);
  });

  it('updated progress - if there is already an existing segment then join it - make sure later segments are preserved - another variant', () => {
    const watchProgress = {
      watchSegments: [{ start: 0, stop: 5.692021 }, { start: 16.716899, stop: 19.852886 }],
      currentStart: null,
      lastKnownPosition: null,
      totalWatchTime: 8.828008,
      videoDuration: 46.61,
      percentWatched: 18,
      currentPosition: 2.447199,
    };
    WatchedSegmentsUtil.updateProgress(watchProgress, 6.16186);
    WatchedSegmentsUtil.updateProgress(watchProgress, 7.16186);
    WatchedSegmentsUtil.updateProgress(watchProgress, 8.16186);
    expect(watchProgress.currentStart).toEqual(0);
    expect(watchProgress.lastKnownPosition).toEqual(8.16186);
    expect(watchProgress.watchSegments).toEqual([{ start: 16.716899, stop: 19.852886 }]);
  });

  it('updated progress - join the segment after me', () => {
    const watchProgress = {
      watchSegments: [{ start: 17.334524, stop: 18.845907 }, { start: 25.97116, stop: 28.562337 }],
      currentStart: 0,
      lastKnownPosition: 16.71553,
      totalWatchTime: 20.81809,
      videoDuration: 46.613333,
      percentWatched: 44,
      currentPosition: 16.71553,
    };
    WatchedSegmentsUtil.updateProgress(watchProgress, 16.983822);
    expect(watchProgress.currentStart).toEqual(0);
    expect(watchProgress.lastKnownPosition).toEqual(18.845907);
    expect(watchProgress.watchSegments).toEqual([{ start: 25.97116, stop: 28.562337 }]);
  });

});

