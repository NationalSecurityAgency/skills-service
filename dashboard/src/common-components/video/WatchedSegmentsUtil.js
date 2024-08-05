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
    if (watchProgress.start !== null
      && watchProgress.lastKnownStopPosition !== null
      && watchProgress.start > currentTime
      && currentTime < watchProgress.lastKnownStopPosition) {
      // nothing to do
      return;
    }
    if (this.isTimeInSegments(watchProgress.watchSegments, currentTime)) {
      // if there is an existing segment then need to close it and join it with the next segment
      if (watchProgress.currentStart !== null && watchProgress.currentStart >= 0) {
        const newSegment = {
          start: watchProgress.currentStart < 1 ? 0 : watchProgress.currentStart,
          stop: watchProgress.lastKnownStopPosition,
        };
        watchProgress.watchSegments = this.addToSegments(watchProgress.watchSegments, newSegment);
        watchProgress.currentStart = null;
        watchProgress.lastKnownStopPosition = null;
      }
    } else {
      // check to see if there is an existing segment to join
      const existingBeforeSegment = watchProgress.watchSegments.findIndex((s) => {
        const diff = currentTime - s.stop;
        return diff > 0 && diff < 2;
      });
      const existingAfterSegment = watchProgress.watchSegments.findIndex((s) => {
        const diff = s.start - currentTime;
        return diff > 0 && diff < 2;
      });

      if (existingBeforeSegment > -1) {
        const existingSegment = { ...watchProgress.watchSegments[existingBeforeSegment] };
          const newSegment = watchProgress.currentStart && watchProgress.lastKnownStopPosition ? {
            start: watchProgress.currentStart < 1 ? 0 : watchProgress.currentStart,
            stop: watchProgress.lastKnownStopPosition,
          } : null;
          watchProgress.currentStart = existingSegment.start > 1 ? existingSegment.start : 0;
          watchProgress.lastKnownStopPosition = currentTime;
          watchProgress.watchSegments.splice(existingBeforeSegment, 1);
          if (newSegment) {
            watchProgress.watchSegments = this.addToSegments(watchProgress.watchSegments, newSegment);
          }
      } else if (existingAfterSegment > -1) {
        const existingSegment = { ...watchProgress.watchSegments[existingAfterSegment] };
        watchProgress.currentStart = Math.min(existingSegment.start, watchProgress.currentStart);
        watchProgress.lastKnownStopPosition = Math.max(existingSegment.stop, watchProgress.lastKnownStopPosition);
        watchProgress.watchSegments.splice(existingAfterSegment, 1);
      } else {
        if (watchProgress.currentStart === null || watchProgress.currentStart === undefined) {
          watchProgress.currentStart = currentTime > 1 ? currentTime : 0;
        }
        if (watchProgress.lastKnownStopPosition && Math.abs(watchProgress.lastKnownStopPosition - currentTime) > 1.2) {
          const newSegment = {
            start: watchProgress.currentStart < 1 ? 0 : watchProgress.currentStart,
            stop: watchProgress.lastKnownStopPosition,
          };
          watchProgress.watchSegments = this.addToSegments(watchProgress.watchSegments, newSegment);
          watchProgress.currentStart = currentTime;
        }
        watchProgress.lastKnownStopPosition = currentTime;
      }
      const sum = watchProgress.watchSegments.map((segment) => segment.stop - segment.start)
        .reduce((acc, cur) => acc + cur, 0);
      watchProgress.totalWatchTime = sum + (watchProgress.lastKnownStopPosition - watchProgress.currentStart);

      if (watchProgress.videoDuration !== Infinity) {
        if (Math.abs(watchProgress.videoDuration - watchProgress.totalWatchTime) < 1) {
          watchProgress.totalWatchTime = watchProgress.videoDuration;
        }
        watchProgress.percentWatched = Math.trunc((watchProgress.totalWatchTime / watchProgress.videoDuration) * 100);
      }
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

    return res;
  },
};
