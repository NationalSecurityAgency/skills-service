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
<template>
  <div data-cy="videoPlayer">
    <video ref="videoPlayer" class="video-js vjs-fluid"></video>
  </div>
</template>

<script>
  import videojs from 'video.js';
  import WatchedSegmentsUtil from '@/common-components/video/WatchedSegmentsUtil';

  export default {
    name: 'VideoPlayer',
    props: {
      options: Object,
    },
    data() {
      return {
        player: null,
        videoOptions: {
          autoplay: false,
          controls: true,
          responsive: true,
          playbackRates: [0.5, 1, 1.5, 2],
          sources: [
            {
              src: this.options.url,
              type: this.options.videoType,
            },
          ],
          captionsUrl: this.options.captionsUrl,
        },
        watchProgress: {
          watchSegments: [],
          currentStart: null,
          lastKnownStopPosition: null,
          totalWatchTime: 0,
          videoDuration: 0,
          percentWatched: 0,
          currentPosition: 0,
        },
      };
    },
    mounted() {
      this.player = videojs(this.$refs.videoPlayer, this.videoOptions, () => {
        const thePlayer = this.player;
        thePlayer.on('durationchange', () => {
          this.watchProgress.videoDuration = thePlayer.duration();
          this.updateProgress(thePlayer.currentTime());
        });
        thePlayer.on('loadedmetadata', () => {
          this.watchProgress.videoDuration = thePlayer.duration();
          this.$emit('watched-progress', this.watchProgress);
        });
        thePlayer.on('timeupdate', () => {
          this.updateProgress(thePlayer.currentTime());
        });
        if (this.options.captionsUrl) {
          thePlayer.addRemoteTextTrack({
            src: this.options.captionsUrl,
            kind: 'subtitles',
            srclang: 'en',
            label: 'English',
          });
        }
      });
    },
    beforeDestroy() {
      if (this.player) {
        this.player.dispose();
      }
    },
    destroyed() {
      this.$emit('player-destroyed', true);
    },
    methods: {
      updateProgress(currentTime) {
        WatchedSegmentsUtil.updateProgress(this.watchProgress, currentTime);
        this.$emit('watched-progress', this.watchProgress);
      },
    },
  };
</script>

<style scoped>

</style>
