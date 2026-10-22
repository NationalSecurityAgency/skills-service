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
const mergeSegments = (segments) => {
  const sorted = segments.map((segment) => ({ ...segment })).sort((a, b) => a.start - b.start);
  const merged = [];
  sorted.forEach((segment) => {
    const last = merged[merged.length - 1];
    if (last && segment.start <= last.stop) {
      last.stop = Math.max(last.stop, segment.stop);
    } else {
      merged.push(segment);
    }
  });
  return merged;
};

/* eslint-disable no-param-reassign */
export default {
  startSegment(watchProgress, currentTime) {
    if (watchProgress.currentStart === null) {
      watchProgress.currentStart = currentTime;
      watchProgress.lastKnownStopPosition = currentTime;
    }
    this.updateProgress(watchProgress, currentTime);
  },
  endSegment(watchProgress) {
    if (watchProgress.currentStart !== null && watchProgress.lastKnownStopPosition !== null) {
      watchProgress.watchSegments = mergeSegments([
        ...watchProgress.watchSegments,
        { start: watchProgress.currentStart, stop: watchProgress.lastKnownStopPosition },
      ]);
    }
    watchProgress.currentStart = null;
    watchProgress.lastKnownStopPosition = null;
    this.calculateProgress(watchProgress);
  },
  updateProgress(watchProgress, currentTime) {
    watchProgress.currentPosition = currentTime;
    // Playback and seeking events define segment boundaries. A delayed
    // timeupdate during continuous playback must not discard watched time.
    if (watchProgress.currentStart !== null && currentTime >= watchProgress.lastKnownStopPosition) {
      watchProgress.lastKnownStopPosition = currentTime;
    }
    this.calculateProgress(watchProgress);
  },
  calculateProgress(watchProgress) {
    const segments = [...watchProgress.watchSegments];
    if (watchProgress.currentStart !== null && watchProgress.lastKnownStopPosition !== null) {
      segments.push({ start: watchProgress.currentStart, stop: watchProgress.lastKnownStopPosition });
    }
    watchProgress.totalWatchTime = mergeSegments(segments)
      .reduce((total, segment) => total + segment.stop - segment.start, 0);

    if (Number.isFinite(watchProgress.videoDuration) && watchProgress.videoDuration > 0) {
      if (Math.abs(watchProgress.videoDuration - watchProgress.totalWatchTime) < 1) {
        watchProgress.totalWatchTime = watchProgress.videoDuration;
      }
      watchProgress.percentWatched = Math.min(100, Math.trunc((watchProgress.totalWatchTime / watchProgress.videoDuration) * 100));
    } else {
      watchProgress.percentWatched = 0;
    }
  },
};
/* eslint-enable no-param-reassign */
