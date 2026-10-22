/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<script setup>
import {computed, onBeforeUnmount, onMounted, onUnmounted, ref} from 'vue'
import videojs from 'video.js';
import WatchedSegmentsUtil from '@/common-components/video/WatchedSegmentsUtil';
import {useStorage} from "@vueuse/core";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";
import registerDownloadPlugin from './registerDownloadPlugin.js';
import registerResizePlugin from './registerResizePlugin.js';

const props = defineProps({
  videoPlayerId: {
    type: String,
    required: true
  },
  options: Object,
  loadFromServer: {
    type: Boolean,
    default:  false,
  },
  storeAndRecoverSizeFromStorage: {
    type: Boolean,
    default: false
  },
  alignCenter: {
    type: Boolean,
    default: true
  }
})
const announcer = useSkillsAnnouncer()
const vidPlayerId = props.videoPlayerId
const emit = defineEmits(['player-destroyed', 'watched-progress', 'on-resize', 'reset-video-progress', 'video-ended'])
const watchProgress = ref({
  watchSegments: [],
  currentStart: null,
  lastKnownStopPosition: null,
  totalWatchTime: 0,
  videoDuration: 0,
  percentWatched: 0,
  currentPosition: 0,
})
const videoPlayerSizeInStorage = props.storeAndRecoverSizeFromStorage ? useStorage(`${vidPlayerId}-playerSize`, {}) : null

const playerContainer = { player: null }
const playerWidth = ref(null);
const playerHeight = ref(null);
const isConfiguredVideoSize = computed(() => playerWidth.value && playerHeight.value)
const resolution = computed(() => {
  if (!playerWidth?.value || !playerHeight?.value) {
    return ''
  }
  return `${playerWidth.value} x ${playerHeight.value}`
})
const isResizing = ref(false);

const hasEnded = ref(false)

const getVideoDuration = (player) => {
  try {
    const duration = player.duration();
    return (duration && !isNaN(duration) && duration > 0) ? duration : -1;
  } catch (e) {
    console.error('Error getting video duration:', e);
    return -1;
  }
}
const getVideoCurrentTime = (player) => {
  try {
    const currentTime = player.currentTime();
    return (Number.isFinite(currentTime) && currentTime >= 0) ? currentTime : -1;
  } catch (e) {
    console.error('Error getting video current time:', e);
    return -1;
  }
}
onMounted(() => {
  if (props.options.width && props.options.height) {
    playerWidth.value = props.options.width;
    playerHeight.value = props.options.height;
  }
  // override the default if configured
  if (props.storeAndRecoverSizeFromStorage && videoPlayerSizeInStorage.value?.width) {
    playerWidth.value = videoPlayerSizeInStorage.value.width;
    playerHeight.value = videoPlayerSizeInStorage.value.height;
  }

  if(!videojs.getPlugin('downloadButton')) {
    registerDownloadPlugin();
  }

  if(!videojs.getPlugin('resizeButton')) {
    registerResizePlugin()
  }

  const playerPlugins = {}

  if(props.options.allowDownloads) {
    playerPlugins.downloadButton = {}
  }
  if(!props.options.isAudio) {
    playerPlugins.resizeButton = {};
  }

  const player = videojs(vidPlayerId, {
    playbackRates: [0.5, 1, 1.5, 2],
    enableSmoothSeeking: true,
    audioOnlyMode: props.options.isAudio,
    plugins: playerPlugins
  }, () => {
    player.on('durationchange', () => {
      updateProgress(player);
    });
    player.on('loadedmetadata', () => {
      updateProgress(player);
    });
    player.on('timeupdate', () => {
      updateProgress(player);
    });
    player.on('play', () => {
      // Reset completed watches on replay, rather than on a pause/resume
      // near the end or only when the rounded coverage is exactly 100%.
      if (hasEnded.value && watchProgress.value.percentWatched > 96) {
        resetProgress()
      }
      hasEnded.value = false;
      startWatching(player);
    });
    player.on('playing', () => {
      startWatching(player);
    });
    player.on('pause', () => {
      updateProgress(player, true);
    });
    player.on('waiting', () => {
      updateProgress(player, true);
    });
    player.on('seeking', () => {
      updateProgress(player, true);
    });
    player.on('seeked', () => {
      if (!player.paused() && !player.ended()) {
        startWatching(player);
      } else {
        updateProgress(player);
      }
    });
    player.on('ended', () => {
      // Reconcile the final position and duration before deciding credit.
      updateProgress(player, true);
      hasEnded.value = true;
      emit('video-ended', watchProgress.value);
    })
    playerContainer.player = player
  });

  player.on('resizeEnd', (e, data) => {
    isResizing.value = false;
    if (playerWidth.value && playerHeight.value) {
      announcer.polite(`Resized the video player to ${playerWidth.value} x ${playerHeight.value}`)
    }
  });

  player.on('resizeDragging', (e, data) => {
    isResizing.value = true

    // Maintain a 16:9 aspect ratio scaling calculation
    const newHeight = Math.round(data.width * (9 / 16));
    const width = data.width;
    const height = newHeight;
    playerWidth.value = width;
    playerHeight.value = height;

    if (props.storeAndRecoverSizeFromStorage) {
      videoPlayerSizeInStorage.value = { width, height }
    }
    emit('on-resize', width, height);

  });
})
onBeforeUnmount(() => {
  if (playerContainer.player) {
    playerContainer.player.dispose()
  }
})
onUnmounted(() => {
  emit('player-destroyed', true)
})
const startWatching = (player) => {
  const currentTime = getVideoCurrentTime(player);
  if (currentTime >= 0 && !player.seeking()) {
    WatchedSegmentsUtil.startSegment(watchProgress.value, currentTime);
  }
  updateProgress(player);
}

const updateProgress = (player, closeSegment = false) => {
  const videoDuration = getVideoDuration(player);
  if (videoDuration > 0) {
    watchProgress.value.videoDuration = videoDuration;
  }
  // During a seek, currentTime is already the destination. Close the
  // watched segment at its last playback position before reading it.
  if (player.seeking()) {
    WatchedSegmentsUtil.endSegment(watchProgress.value);
  }
  const currentTime = getVideoCurrentTime(player);
  if (currentTime >= 0) {
    WatchedSegmentsUtil.updateProgress(watchProgress.value, currentTime);
  }
  if (closeSegment) {
    WatchedSegmentsUtil.endSegment(watchProgress.value);
  }
  emit('watched-progress', watchProgress.value)
}

const resetProgress = () => {
  watchProgress.value.watchSegments = []
  watchProgress.value.currentStart = null
  watchProgress.value.percentWatched = 0
  watchProgress.value.currentPosition = 0
  watchProgress.value.lastKnownStopPosition = null
  watchProgress.value.totalWatchTime = 0

  WatchedSegmentsUtil.updateProgress(watchProgress.value, 0)
  emit('reset-video-progress', watchProgress.value)
}
</script>

<template>
  <div :class="`flex ${ alignCenter ? 'justify-center' : ''} mt-2`">
    <div :class="{ 'flex-1' : !isConfiguredVideoSize }">
      <div :id="`${vidPlayerId}Container`" data-cy="videoPlayer" :style="playerWidth ? `width: ${playerWidth}px;` : ''"
           class="videoPlayerContainer p-0 border rounded-sm border-surface-200 dark:border-surface-600">
        <div v-if="isResizing" class="text-center flex items-center justify-center ">
          <div class="absolute z-40 top-0 left-0 right-0 bottom-0 bg-gray-600 opacity-50 text-center flex items-center justify-center " >
          </div>
          <div class="absolute top-0 z-50 text-center text-primary bg-primary-contrast mt-8 border rounded-border" style="width: 100px;">
            {{ resolution }}
          </div>
        </div>
        <video :id="vidPlayerId"
               class="video-js vjs-fluid"
               data-setup='{}'
               responsive
               controls>
          <source :src="options.url" :type="options.videoType">
          <track v-if="props.options.captionsUrl" :src="props.options.captionsUrl" kind="captions" srclang="en" label="English">
        </video>
      </div>
    </div>
  </div>
</template>

<style scoped>
.videoPlayerContainer {
  overflow: hidden;
  max-width: 100%;
  min-width: 360px;
  position: relative;
}

/* Custom icon styling for the download button using standard CSS */
:deep(.vjs-download-button .vjs-icon-placeholder::before) {
  /* Video.js comes with a built-in download icon class or unicode character */
  content: "\f110";
  font-family: "VideoJS";
  cursor: pointer;
}

:deep(.vjs-resize-button .vjs-icon-placeholder::before) {
  /* Video.js comes with a built-in download icon class or unicode character */
  cursor: pointer;
}

:deep(.video-js:not(.vjs-has-started) .vjs-control-bar) {
  display: flex !important;
  opacity: 1 !important;
  visibility: visible !important;
}

</style>
