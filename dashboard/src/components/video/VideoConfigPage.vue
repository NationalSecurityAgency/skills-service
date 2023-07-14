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
    <sub-page-header title="Configure Video"/>
    <b-overlay :show="loading">
    <b-card>
      <div class="row">
        <div class="col">
          <b-form-group label="Video URL:" label-for="videoUrlInput">
            <b-form-input id="videoUrlInput" v-model="videoConf.url"/>
          </b-form-group>
        </div>
        <div class="col-3">
          <b-form-group label="Video Type:" label-for="videoTypeInput">
            <b-form-input id="videoTypeInput" v-model="videoConf.videoType"/>
          </b-form-group>
        </div>
      </div>

      <b-form-group label="Player captions:" label-for="videoCaptionsInput">
        <b-form-textarea
          id="videoCaptionsInput"
          v-model="videoConf.captions"
          placeholder="Enter captions using The Web Video Text Tracks (WebVTT) format"
          rows="3"
          max-rows="6"
        ></b-form-textarea>
      </b-form-group>

      <b-form-group label="Transcript:" label-for="videoTranscriptInput">
        <b-form-textarea
          id="videoTranscriptInput"
          v-model="videoConf.transcript"
          placeholder="Please enter video's transcript here. Video transcript will be available for download."
          rows="3"
          max-rows="6"
        ></b-form-textarea>
      </b-form-group>

      <b-button variant="outline-info" @click="setupPreview">Preview <i class="fas fa-eye" aria-hidden="true"/></b-button>
      <b-button variant="outline-success" class="ml-2" @click="saveSettings">Save <i class="fas fa-save" aria-hidden="true"/></b-button>
      <span v-if="showSavedMsg" aria-hidden="true" class="ml-2 text-success"><i class="fas fa-check" /> Saved</span>
      <b-card v-if="preview" class="mt-3" header="Video Preview" body-class="p-0">
        <video-player v-if="!refreshingPreview"
                      :options="computedVideoConf"
                      @player-destroyed="turnOffRefresh"
                      @watched-progress="updatedWatchProgress"
        />

        <div v-if="watchedProgress" class="p-3 pt-4">
          <div class="row">
            <div class="col-6 col-lg-3 col-xl-2">Total Duration:</div>
            <div class="col"><span class="text-primary">{{ watchedProgress.videoDuration.toFixed(2) }}</span> <span class="font-italic">Seconds</span></div>
          </div>
          <div class="row">
            <div class="col-6 col-lg-3 col-xl-2">Time Watched:</div>
            <div class="col"><span class="text-primary">{{ watchedProgress.totalWatchTime.toFixed(2) }}</span> <span class="font-italic">Seconds</span></div>
          </div>
          <div class="row">
            <div class="col-6 col-lg-3 col-xl-2">% Watched:</div>
            <div class="col"><span class="text-primary">{{ watchedProgress.percentWatched }}%</span></div>
          </div>
          <div class="row">
            <div class="col-6 col-lg-3 col-xl-2">Current Position:</div>
            <div class="col"><span class="text-primary">{{ watchedProgress.currentPosition.toFixed(2) }}</span> <span class="font-italic">Seconds</span></div>
          </div>
          <div class="row">
            <div class="col-6 col-lg-3 col-xl-2">Watched Segments:</div>
            <div class="col">
              <div v-if="watchedProgress.currentStart !== null && watchedProgress.lastKnownStopPosition"> <span class="text-primary">{{ watchedProgress.currentStart.toFixed(2) }}</span>
                <i class="fas fa-arrow-circle-right text-secondary mx-2" :aria-hidden="true"/>
                <span class="text-primary">{{ watchedProgress.lastKnownStopPosition.toFixed(2) }}</span> <span class="font-italic">Seconds</span>
              </div>
              <div v-for="segment in watchedProgress.watchSegments" :key="segment.start"><span class="text-primary">{{ segment.start.toFixed(2) }}</span>
                <i class="fas fa-arrow-circle-right text-secondary mx-2" :aria-hidden="true"/><span class="text-primary">{{ segment.stop.toFixed(2) }}</span> <span class="font-italic">Seconds</span>
              </div>
            </div>
          </div>
        </div>
      </b-card>
    </b-card>
    </b-overlay>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import VideoService from '@/components/video/VideoService';
  import VideoPlayer from '@/components/video/VideoPlayer';

  export default {
    name: 'VideoConfigPage',
    components: { VideoPlayer, SubPageHeader },
    data() {
      return {
        videoConf: {
          url: '',
          videoType: '',
          captions: '',
          transcript: '',
        },
        watchedProgress: null,
        preview: false,
        refreshingPreview: false,
        loading: true,
        showSavedMsg: false,
      };
    },
    mounted() {
      this.loadSettings();
    },
    computed: {
      computedVideoConf() {
        const captionsUrl = this.videoConf.captions && this.videoConf.captions.trim().length > 0
          ? `/api/projects/${this.$route.params.projectId}/skills/${this.$route.params.skillId}/videoCaptions`
          : null;
        return {
          url: this.videoConf.url,
          videoType: this.videoConf.videoType,
          captionsUrl,
        };
      },
    },
    methods: {
      setupPreview() {
        if (this.preview) {
          this.refreshingPreview = true;
        } else {
          this.preview = true;
        }
      },
      turnOffRefresh() {
        this.refreshingPreview = false;
      },
      saveSettings() {
        this.preview = false;
        this.loading = true;
        const settings = {
          videoUrl: this.videoConf.url,
          videoType: this.videoConf.videoType,
          captions: this.videoConf.captions,
          transcript: this.videoConf.transcript,
        };
        VideoService.saveVideoSettings(this.$route.params.projectId, this.$route.params.skillId, settings)
          .then(() => {
            this.showSavedMsg = true;
            setTimeout(() => {
              this.showSavedMsg = false;
            }, 3500);
          })
          .finally(() => {
            this.loading = false;
          });
      },
      loadSettings() {
        this.loading = true;
        VideoService.getVideoSettings(this.$route.params.projectId, this.$route.params.skillId)
          .then((videoSettings) => {
            this.videoConf.url = videoSettings.videoUrl;
            this.videoConf.videoType = videoSettings.videoType;
            this.videoConf.captions = videoSettings.captions;
            this.videoConf.transcript = videoSettings.transcript;
          }).finally(() => {
            this.loading = false;
          });
      },
      updatedWatchProgress(progress) {
        this.watchedProgress = progress;
      },
    },
  };
</script>

<style scoped>

</style>
