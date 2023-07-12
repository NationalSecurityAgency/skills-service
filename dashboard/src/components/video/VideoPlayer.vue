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
<div>
  <video ref="videoPlayer" class="video-js vjs-fluid"></video>
</div>
</template>

<script>
  import videojs from 'video.js';

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
          currentStop: null,
          totalWatchTime: 0,
          videoDuration: 0,
          percentWatched: 0,
        },
      };
    },
    mounted() {
      this.player = videojs(this.$refs.videoPlayer, this.videoOptions, () => {
        this.player.log('onPlayerReady', this);

        const thePlayer = this.player;
        this.player.ready(() => {
          // thePlayer.currentTime(10);

          // get the current time, should be 120 seconds
          // eslint-disable-next-line no-console
          console.log(thePlayer.currentTime());
          // eslint-disable-next-line no-console
          console.log(`duration: ${thePlayer.duration()}`);
          // eslint-disable-next-line no-console
          console.log(thePlayer.remainingTime());
        });
        thePlayer.on('loadedmetadata', () => {
          this.watchProgress.videoDuration = thePlayer.duration().toFixed(2);
          this.$emit('watched-progress', this.watchProgress);
        });
        thePlayer.on('play', () => {
          // eslint-disable-next-line no-console
          console.log(`Video playback started: ${thePlayer.currentTime()}`);
          this.watchProgress.currentStart = thePlayer.currentTime();
        });
        thePlayer.on('pause', () => {
          // eslint-disable-next-line no-console
          console.log(`Video playback paused: ${thePlayer.currentTime()}`);
          this.watchProgress.currentStop = thePlayer.currentTime();
          this.updateProgress();
        });
        thePlayer.on('seeking', () => {
          // eslint-disable-next-line no-console
          console.log(`Video seeking: ${thePlayer.currentTime()}`);
        });
        thePlayer.on('seeked', () => {
          // eslint-disable-next-line no-console
          console.log(`Video seek ended: ${thePlayer.currentTime()}`);
        });
        thePlayer.on('ended', () => {
          // eslint-disable-next-line no-console
          console.log('Video playback ended.');
        });
        thePlayer.on('timeupdate', () => {
          // eslint-disable-next-line no-console
          console.log(`Current position: ${thePlayer.currentTime()}`);
        });

        console.log(`captions url: ${this.options.captionsUrl}`);
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
        // eslint-disable-next-line no-console
        console.log(`Destroying, current time is: ${this.player.currentTime()}`);
        this.player.dispose();
      }
    },
    destroyed() {
      this.$emit('player-destroyed', true);
    },
    methods: {
      updateProgress() {
        const newSegment = { start: this.watchProgress.currentStart.toFixed(2), stop: this.watchProgress.currentStop.toFixed(2) };
        this.watchProgress.watchSegments.push(newSegment);
        this.watchProgress.currentStart = null;
        this.watchProgress.currentStop = null;
        const sum = this.watchProgress.watchSegments.map((segment) => segment.stop - segment.start).reduce((acc, cur) => acc + cur, 0);
        this.watchProgress.totalWatchTime = sum.toFixed(2);
        this.watchProgress.percentWatched = Math.trunc((this.watchProgress.totalWatchTime / this.watchProgress.videoDuration) * 100);
        this.$emit('watched-progress', this.watchProgress);
      },
    },
  };
</script>

<style scoped>

</style>
