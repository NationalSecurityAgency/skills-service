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
import { onBeforeUnmount, onMounted, onUnmounted, ref, computed } from 'vue'
import videojs from 'video.js';
import WatchedSegmentsUtil from '@/common-components/video/WatchedSegmentsUtil';
import {useStorage} from "@vueuse/core";

const props = defineProps({
  options: Object,
  loadFromServer: {
    type: Boolean,
    default:  false,
  }
})
const vidPlayerId = props.options.videoId ? `vidPlayer${props.options.videoId}` : 'vidPlayer1'
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
const playerContainer = { player: null }
const playerWidth = ref(null);
const playerHeight = ref(null);
const resolution = computed(() => {
  if (!videoPlayerSizeInStorage.value) {
    return ''
  }
  return `${videoPlayerSizeInStorage.value.width} x ${videoPlayerSizeInStorage.value.height}`
})
const isResizing = ref(false);
const resizeTimer = ref(null);
const isFirstLoad = ref(true);

const videoPlayerSizeInStorage = useStorage(`${vidPlayerId}-playerSize`, {})


onMounted(() => {
  if(props.options.width && props.options.height) {
    playerWidth.value = props.options.width + 22;
    playerHeight.value = props.options.height + 22;
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

const createResizeSupport = () => {
  function makeResizableDiv() {
    const resizableDiv = `#${vidPlayerId}Container`
    const element = document.querySelector(resizableDiv);
    const handle = document.querySelectorAll( `#${vidPlayerId}ResizeHandle`)[0]

    handle.addEventListener('mousedown', function (e) {
      e.preventDefault()
      window.addEventListener('mousemove', resize)
      window.addEventListener('mouseup', stopResize)
    })

    function resize(e) {
      const clientRect = element.getBoundingClientRect()
      const width = Math.trunc(clientRect.width)
      const height = Math.trunc(clientRect.height)
      videoPlayerSizeInStorage.value = { width, height }
      element.style.width = e.pageX - clientRect.left + 'px'
      emit('on-resize', width, height);
    }

    function stopResize() {
      window.removeEventListener('mousemove', resize)
    }
  }

  makeResizableDiv()
}


</script>

<template>
  <div :id="`${vidPlayerId}Container`" data-cy="videoPlayer"  :style="playerWidth ? `width: ${playerWidth}px;` : ''" class="videoPlayerContainer">
    <div class="bg-white block" style="width: 3rem; height: 3rem">
      <i class="fas fa-expand-alt fa-rotate-90 handle bg-white border-top-1"
         :id="`${vidPlayerId}ResizeHandle`"
         data-cy="videoResizeHandle"
         aria-label="Resize video dimensions control"
         tabindex="0"></i>
    </div>
    <div class="absolute z-5 top-0 left-0 right-0 bottom-0 bg-gray-500 opacity-50 text-center flex align-items-center justify-content-center " v-if="isResizing && !isFirstLoad">
      <div class="absolute z-6 top-0 text-center bg-white mt-4" style="width: 100px;">
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
</template>

<style scoped>
.videoPlayerContainer {
  //resize: horizontal;
  overflow: hidden;
  border: 1px solid gray;
  max-width: 100%;
  min-width: 222px;
  position: relative;
}

.handle{
  font-size: 1.1rem;
  right: 0px;
  bottom: 0px;
  position: absolute;
  z-index: 500;
}



</style>