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
import { onBeforeUnmount, onMounted, onUnmounted, ref } from 'vue'
import videojs from 'video.js';
import WatchedSegmentsUtil from '@/common-components/video/WatchedSegmentsUtil';

const props = defineProps({
  options: Object,
})
const emit = defineEmits(['player-destroyed', 'watched-progress'])
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
onMounted(() => {
  const player = videojs('vidPlayer1', {
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
  <div data-cy="videoPlayer">
    <video id="vidPlayer1"
           ref="videoPlayer"
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

</style>