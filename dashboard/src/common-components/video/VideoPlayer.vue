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
const player = ref(null)
const videoPlayer = ref(null)
const videoOptions = ref({
  autoplay: false,
  controls: true,
  responsive: true,
  playbackRates: [0.5, 1, 1.5, 2],
  sources: [
    {
      src: props.options.url,
      type: props.options.videoType,
    },
  ],
  captionsUrl: props.options.captionsUrl,
})
const watchProgress = ref({
  watchSegments: [],
  currentStart: null,
  lastKnownStopPosition: null,
  totalWatchTime: 0,
  videoDuration: 0,
  percentWatched: 0,
  currentPosition: 0,
})

onMounted(() => {
  player.value = videojs(videoPlayer.value, videoOptions.value, () => {
    const thePlayer = player.value;
    thePlayer.on('durationchange', () => {
      watchProgress.value.videoDuration = thePlayer.duration();
      updateProgress(thePlayer.currentTime());
    });
    thePlayer.on('loadedmetadata', () => {
      watchProgress.value.videoDuration = thePlayer.duration();
      emit('watched-progress', watchProgress.value);
    });
    thePlayer.on('timeupdate', () => {
      updateProgress(thePlayer.currentTime());
    });
    if (props.options.captionsUrl) {
      thePlayer.addRemoteTextTrack({
        src: props.options.captionsUrl,
        kind: 'subtitles',
        srclang: 'en',
        label: 'English',
      });
    }
  });
})
onBeforeUnmount(() => {
  if (player.value) {
    player.value.dispose()
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
    <video ref="videoPlayer" class="video-js vjs-fluid"></video>
  </div>
</template>

<style scoped>

</style>