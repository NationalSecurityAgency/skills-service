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
const isLessOrEqual = (a, b, equalityThreshold = 0.2) => a < b || Math.abs(a - b) < equalityThreshold;
const isStartWithin = (existingSegment, newSegment) => isLessOrEqual(existingSegment.start, newSegment.start) && isLessOrEqual(newSegment.start, existingSegment.stop);
const isEndWithin = (existingSegment, newSegment) => isLessOrEqual(existingSegment.start, newSegment.stop) && isLessOrEqual(newSegment.stop, existingSegment.stop);
const doesOverlap = (existingSegment, newSegment) => isStartWithin(existingSegment, newSegment) || isEndWithin(existingSegment, newSegment);
const doesCover = (existingSegment, newSegment) => newSegment.start < existingSegment.start && newSegment.stop > existingSegment.stop;
const isWithin = (existingSegment, newSegment) => isStartWithin(existingSegment, newSegment) && isEndWithin(existingSegment, newSegment);
export default {
  updateProgress(watchProgress, currentTime) {
    /* eslint-disable no-param-reassign */
    watchProgress.currentPosition = currentTime;
    if (this.isTimeInSegments(watchProgress.watchSegments, currentTime)) {
      // if there is an existing segment then need to close it and join it with the next segment
      if (watchProgress.currentStart) {
        const newSegment = {
          start: watchProgress.currentStart < 1 ? 0 : watchProgress.currentStart,
          stop: watchProgress.lastKnownPosition,
        };
        watchProgress.watchSegments = this.addToSegments(watchProgress.watchSegments, newSegment);
        watchProgress.currentStart = null;
        watchProgress.lastKnownPosition = null;
      }
    } else {
      // check to see if there is an existing segment to join
      const existingSegmentIndex = watchProgress.watchSegments.findIndex((s) => Math.abs(currentTime - s.stop) < 2);
      if (existingSegmentIndex > -1) {
        const existingSegment = watchProgress.watchSegments[existingSegmentIndex];
        const newSegment = (watchProgress.currentStart && watchProgress.lastKnownPosition) ? {
          start: watchProgress.currentStart < 1 ? 0 : watchProgress.currentStart,
          stop: watchProgress.lastKnownPosition,
        } : null;
        watchProgress.currentStart = existingSegment.start;
        watchProgress.lastKnownPosition = currentTime;

        watchProgress.watchSegments.splice(existingSegmentIndex, 1);
        if (newSegment) {
          watchProgress.watchSegments = this.addToSegments(watchProgress.watchSegments, newSegment);
        }
      } else {
        if (!watchProgress.currentStart) {
          watchProgress.currentStart = currentTime;
        }
        if (watchProgress.lastKnownPosition && Math.abs(watchProgress.lastKnownPosition - currentTime) > 1.2) {
          const newSegment = {
            start: watchProgress.currentStart < 1 ? 0 : watchProgress.currentStart,
            stop: watchProgress.lastKnownPosition,
          };
          watchProgress.watchSegments = this.addToSegments(watchProgress.watchSegments, newSegment);
          watchProgress.currentStart = currentTime;
        }
        watchProgress.lastKnownPosition = currentTime;
      }
      const sum = watchProgress.watchSegments.map((segment) => segment.stop - segment.start)
        .reduce((acc, cur) => acc + cur, 0);
      watchProgress.totalWatchTime = sum + (watchProgress.lastKnownPosition - watchProgress.currentStart);
      watchProgress.percentWatched = Math.trunc((watchProgress.totalWatchTime / watchProgress.videoDuration) * 100);
    }
    /* eslint-enable no-param-reassign */
  },
  isTimeInSegments(existingSegments, time) {
    if (!existingSegments) {
      return false;
    }
    const foundIndex = existingSegments.findIndex((item) => isLessOrEqual(item.start, time) && isLessOrEqual(time, item.stop));
    return foundIndex >= 0;
  },
  addToSegments(existingSegments, newSegement) {
    // console.log(`adding new segment ${JSON.stringify(newSegement)} to ${JSON.stringify(existingSegments)} types[${typeof newSegement.start}]`);
    const copy = existingSegments.map((item) => ({ ...item }));
    const existingSegment = copy.find((item) => doesOverlap(item, newSegement) || doesCover(item, newSegement));
    if (existingSegment) {
      if (!isWithin(existingSegment, newSegement)) {
        if (newSegement.start < existingSegment.start) {
          existingSegment.start = newSegement.start;
        }
        if (newSegement.stop > existingSegment.stop) {
          existingSegment.stop = newSegement.stop;
        }
      }
    } else {
      // console.log('pushing new setment');
      copy.push(newSegement);
    }

    const res = [];
    copy.sort((a, b) => a.start - b.start);
    let lastItem = null;
    copy.forEach((item) => {
      if (lastItem && isLessOrEqual(item.start, lastItem.stop, 2)) {
        lastItem.stop = item.stop;
      } else {
        res.push(item);
        lastItem = item;
      }
    });

    // console.log(`result: ${JSON.stringify(res)}`);
    return res;
  },
};
