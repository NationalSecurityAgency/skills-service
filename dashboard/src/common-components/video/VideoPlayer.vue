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
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";

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
const resolution = ref(null);
const isResizing = ref(false);
const resizeTimer = ref(null);

const resizeObserver = new ResizeObserver((mutations) => {
  if(resizeTimer.value) {
    clearTimeout(resizeTimer.value);
    resizeTimer.value = null;
  }
  isResizing.value = true;
  resizeTimer.value = setTimeout(() => {
    isResizing.value = false;
  }, 250);
  const currentWidth = mutations[0].contentBoxSize[0].inlineSize;
  const currentHeight = mutations[0].contentBoxSize[0].blockSize;
  resolution.value = `${mutations[0].devicePixelContentBoxSize[0].inlineSize} x ${mutations[0].devicePixelContentBoxSize[0].blockSize}`;
  if(!props.loadFromServer) {
    localStorage.setItem('playerSize', JSON.stringify({width: currentWidth, height: currentHeight}));
  }
  emit('on-resize', resolution.value, currentWidth, currentHeight);
})

onMounted(() => {
  if(props.options.width && props.options.height) {
    playerWidth.value = props.options.width + 22;
    playerHeight.value = props.options.height + 22;
  }
  const existingSize = localStorage.getItem('playerSize');
  if (existingSize && !props.loadFromServer) {
    const sizeObject = JSON.parse(existingSize);
    playerWidth.value = sizeObject.width;
    playerHeight.value = sizeObject.height;
  }
  resizeObserver.observe(document.getElementById('videoPlayerContainer'));
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

</script>

<template>
    <div data-cy="videoPlayer" id="videoPlayerContainer" :style="playerWidth ? `width: ${playerWidth}px;` : ''">
        <div class="absolute z-5 top-0 left-0 right-0 bottom-0 bg-gray-500 opacity-50 text-center flex align-items-center justify-content-center" v-if="isResizing">
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
#videoPlayerContainer {
  resize: horizontal;
  overflow: auto;
  padding: 10px;
  border: 1px solid gray;
  max-width: 100%;
  min-width: 222px;
  position: relative;
}
</style>
