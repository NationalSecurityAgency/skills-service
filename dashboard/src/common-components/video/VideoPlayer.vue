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
import {computed, nextTick, onBeforeUnmount, onMounted, onUnmounted, ref} from 'vue'
import videojs from 'video.js';
import WatchedSegmentsUtil from '@/common-components/video/WatchedSegmentsUtil';
import {useStorage} from "@vueuse/core";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

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
  }
})
const announcer = useSkillsAnnouncer()
const vidPlayerId = props.videoPlayerId
const emit = defineEmits(['player-destroyed', 'watched-progress', 'on-resize'])
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

const isPlaying = ref(false)
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
  const player = videojs(vidPlayerId, {
    playbackRates: [0.5, 1, 1.5, 2],
    enableSmoothSeeking: true,
  }, () => {
    player.on('durationchange', () => {
      watchProgress.value.videoDuration = player.duration();
      updateProgress(player.currentTime());
    });
    player.on('loadedmetadata', () => {
      watchProgress.value.videoDuration = player.duration();
      emit('watched-progress', watchProgress.value);
    });
    player.on('timeupdate', () => {
      updateProgress(player.currentTime());
    });
    player.on('play', () => {
      isPlaying.value = true
    });
    player.on('pause', () => {
      isPlaying.value = false
      nextTick(() => {
        createResizeSupport()
      })
    });
    playerContainer.player = player
  });

  createResizeSupport()
})
onBeforeUnmount(() => {
  if (playerContainer.player) {
    playerContainer.player.dispose()
  }
})
onUnmounted(() => {
  emit('player-destroyed', true)
})
const updateProgress = (currentTime) => {
  WatchedSegmentsUtil.updateProgress(watchProgress.value, currentTime)
  emit('watched-progress', watchProgress.value)
}

const getResizableElement = () => {
  const resizableDiv = `#${vidPlayerId}Container`
  return document.querySelector(resizableDiv)
}

const getResizableElementRect = () => {
  const element = getResizableElement();
  return element.getBoundingClientRect()
}

const resizePlayerSmaller = () => resizePlayer(-50)
const resizePlayerBigger = () => resizePlayer(50)

const resizePlayer = (resizeWidth) => {
  const element = getResizableElement();
  const clientRect = element.getBoundingClientRect()
  element.style.width = (clientRect.width + resizeWidth) + 'px'
  updateResizableInfo()
  announcer.polite(`Resized the video player by ${resizeWidth} pixels`)
}

const updateResizableInfo = () => {
  const clientRect = getResizableElementRect()
  const width = Math.trunc(clientRect.width)
  const height = Math.trunc(clientRect.height)
  playerWidth.value = width;
  playerHeight.value = height;
  if (props.storeAndRecoverSizeFromStorage) {
    videoPlayerSizeInStorage.value = { width, height }
  }
  emit('on-resize', width, height);
}
const createResizeSupport = () => {
  function makeResizableDiv() {
    const handle = document.querySelectorAll( `#${vidPlayerId}ResizeHandle`)[0]

    handle.addEventListener('mousedown', function (e) {
      e.preventDefault()
      window.addEventListener('mousemove', resize)
      window.addEventListener('mouseup', stopResize)
    })

    function resize(e) {
      isResizing.value = true
      const element = getResizableElement();
      const clientRect = element.getBoundingClientRect()
      element.style.width = e.pageX - clientRect.left + 'px'
      updateResizableInfo()
    }

    function stopResize() {
      window.removeEventListener('mousemove', resize)
      isResizing.value = false
      if (playerWidth.value && playerHeight.value) {
        announcer.polite(`Resized the video player to ${playerWidth.value} x ${playerHeight.value}`)
      }
    }
  }

  makeResizableDiv()
}


</script>

<template>
  <div class="flex justify-content-center mt-2">
    <div :class="{ 'flex-1' : !isConfiguredVideoSize }">
  <div :id="`${vidPlayerId}Container`" data-cy="videoPlayer"  :style="playerWidth ? `width: ${playerWidth}px;` : ''"
       class="videoPlayerContainer p-0 border-1 border-round-sm border-200">
      <i v-if="!isPlaying"
         class="fas fa-expand-alt fa-rotate-90 handle border-1 border-500 p-1 bg-primary-reverse border-round"
         :id="`${vidPlayerId}ResizeHandle`"
         data-cy="videoResizeHandle"
         role="button"
         aria-label="Resize video dimensions control. Press right or left to resize the video player."
         @keyup.right="resizePlayerBigger"
         @keyup.left="resizePlayerSmaller"
         tabindex="0"></i>
    <div v-if="isResizing" class="text-center flex align-items-center justify-content-center ">
      <div class="absolute z-4 top-0 left-0 right-0 bottom-0 bg-gray-600 opacity-50 text-center flex align-items-center justify-content-center " >
      </div>
      <div class="absolute top-0 z-5 text-center bg-primary-reverse mt-5 border-1 border-round" style="width: 100px;">
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
  //resize: horizontal;
  overflow: hidden;
  max-width: 100%;
  min-width: 222px;
  position: relative;
}

.handle{
  font-size: 1.1rem;
  right: 3px;
  bottom: 0px;
  position: absolute;
  z-index: 500;
}

.handle:hover{
  cursor: ew-resize;
}

.handle:active{
  cursor: ew-resize;
}

.handle:focus{
  cursor: ew-resize;
}

.handle:current{
  cursor: ew-resize;
}


</style>