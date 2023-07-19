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
      <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit}" slim>
      <div class="row">
        <div class="col-md">
          <b-form-group label="* Video URL:" label-for="videoUrlInput">
            <ValidationProvider rules="customUrlValidator" :debounce="250" v-slot="{ errors }" name="Video URL">
              <b-form-input id="videoUrlInput" v-model="videoConf.url" data-cy="videoUrl" @input="validate"/>
              <small role="alert" class="form-text text-danger" id="videoUrlError" data-cy="videoUrlErr">{{errors[0]}}</small>
            </ValidationProvider>
          </b-form-group>
        </div>
        <div class="col-md-3">
          <b-form-group label="Video Type:" label-for="videoTypeInput">
            <ValidationProvider rules="videoUrlMustBePresent" :debounce="250" v-slot="{ errors }" name="Video Type">
               <b-form-input id="videoTypeInput" v-model="videoConf.videoType" data-cy="videoType"/>
               <small role="alert" class="form-text text-danger" id="videoTypeError" data-cy="videoTypeErr">{{errors[0]}}</small>
            </ValidationProvider>
          </b-form-group>
        </div>
      </div>

      <b-form-group label="Captions:" label-for="videoCaptionsInput">
        <div slot="label">
          <div class="row">
            <div class="col my-auto">Captions:</div>
            <div v-if="!videoConf.captions" class="col-auto">
              <b-button variant="outline-info" size="sm"
                        aria-label="Click to fill in sample captions using The Web Video Text Tracks (WEBVTT) format"
                        @click="fillInCaptionsExample" data-cy="fillCaptionsExamples"><i class="fas fa-plus"></i> Add Example</b-button>
            </div>
          </div>
        </div>
        <ValidationProvider rules="videoUrlMustBePresent" :debounce="250" v-slot="{ errors }" name="Captions">
          <b-form-textarea
            id="videoCaptionsInput"
            v-model="videoConf.captions"
            placeholder="Enter captions using The Web Video Text Tracks (WebVTT) format (optional)"
            rows="3"
            max-rows="6"
            data-cy="videoCaptions"
          ></b-form-textarea>
          <small role="alert" class="form-text text-danger" id="videoCaptionsError" data-cy="videoCaptionsError">{{errors[0]}}</small>
        </ValidationProvider>
      </b-form-group>

      <b-form-group label="Transcript:" label-for="videoTranscriptInput">
        <ValidationProvider rules="maxDescriptionLength|customDescriptionValidator|videoUrlMustBePresent" :debounce="250" v-slot="{ errors }" name="Video Transcript">
        <b-form-textarea
          id="videoTranscriptInput"
          v-model="videoConf.transcript"
          placeholder="Please enter video's transcript here. Video transcript will be available for download (optional)"
          rows="3"
          max-rows="6"
          data-cy="videoTranscript"
        ></b-form-textarea>
          <small role="alert" id="videoTranscriptError" class="form-text text-danger" data-cy="videoTranscriptError">{{ errors[0] }}</small>
        </ValidationProvider>
      </b-form-group>

      <div v-if="overallErrMsg" class="alert alert-danger">
        {{ overallErrMsg }}
      </div>

        <div class="row">
          <div class="col-sm mt-2">
            <b-button variant="outline-info"
                      :disabled="!hasVideoUrl"
                      data-cy="previewVideoSettingsBtn"
                      aria-label="Preview video"
                      @click="setupPreview">Preview <i class="fas fa-eye" aria-hidden="true"/></b-button>
            <b-button variant="outline-success"
                      class="ml-2"
                      :disabled="!hasVideoUrl || invalid"
                      data-cy="saveVideoSettingsBtn"
                      aria-label="Save video settings"
                      @click="handleSubmit(submitSaveSettingsForm)">Save <i class="fas fa-save" aria-hidden="true"/></b-button>
            <span v-if="showSavedMsg" aria-hidden="true" class="ml-2 text-success" data-cy="savedMsg"><i class="fas fa-check" /> Saved</span>
          </div>
          <div class="col-auto mt-2">
            <b-button variant="outline-danger"
                      :disabled="!formHasAnyData"
                      data-cy="clearVideoSettingsBtn"
                      aria-label="Clear video settings"
                      @click="confirmClearSettings">Clear <i class="fas fa-ban" aria-hidden="true"/></b-button>
          </div>
        </div>
      </ValidationObserver>
      <b-card v-if="preview" class="mt-3" header="Video Preview" body-class="p-0" data-cy="videoPreviewCard">
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
            <div class="col"><span class="text-primary" data-cy="percentWatched">{{ watchedProgress.percentWatched }}%</span></div>
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
  import { extend } from 'vee-validate';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import VideoService from '@/components/video/VideoService';
  import VideoPlayer from '@/common-components/video/VideoPlayer';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';

  export default {
    name: 'VideoConfigPage',
    components: { VideoPlayer, SubPageHeader },
    mixins: [MsgBoxMixin],
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
        overallErrMsg: null,
      };
    },
    created() {
      this.assignCustomValidation();
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
      hasVideoUrl() {
        return this.videoConf.url && this.videoConf.url.trim().length > 0;
      },
      formHasAnyData() {
        return this.videoConf.url || this.videoConf.videoType || this.videoConf.captions || this.videoConf.transcript;
      },
    },
    methods: {
      setupPreview() {
        if (this.preview) {
          this.refreshingPreview = true;
        } else {
          this.preview = true;
          this.$nextTick(() => this.$announcer.polite('Opened video preview card below. Navigate down to it.'));
        }
      },
      turnOffRefresh() {
        this.refreshingPreview = false;
      },
      submitSaveSettingsForm() {
        this.$refs.observer.validate()
          .then((res) => {
            if (!res) {
              this.overallErrMsg = 'Form did NOT pass validation, please fix and try to Save again';
            } else {
              this.saveSettings();
            }
          });
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
            this.$nextTick(() => this.$announcer.polite('Video settings were saved'));
          })
          .finally(() => {
            this.loading = false;
          });
      },
      confirmClearSettings() {
        this.msgConfirm('Video settings will be permanently cleared. Are you sure you want to proceed?', 'Please Confirm!', 'Yes, Do clear')
          .then((res) => {
            if (res) {
              this.clearSettings();
            }
          });
      },
      clearSettings() {
        this.loading = true;
        this.videoConf.url = '';
        this.videoConf.videoType = '';
        this.videoConf.captions = '';
        this.videoConf.transcript = '';
        this.preview = false;
        // this.$refs.observer.reset();
        VideoService.deleteVideoSettings(this.$route.params.projectId, this.$route.params.skillId)
          .finally(() => {
            this.loading = false;
            this.validate();
            this.$nextTick(() => this.$announcer.polite('Video settings were cleared'));
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
      assignCustomValidation() {
        const self = this;
        extend('videoUrlMustBePresent', {
          message: (field) => `${field} is not valid without Video URL field`,
          validate() {
            const toValidate = self.videoConf.url ? self.videoConf.url.trim() : null;
            const res = toValidate !== null && toValidate.length > 0;
            return res;
          },
        });
      },
      fillInCaptionsExample() {
        if (!this.videoConf.captions) {
          this.videoConf.captions = 'WEBVTT\n'
            + '\n'
            + '1\n'
            + '00:00:00.500 --> 00:00:04.000\n'
            + 'This is the very first caption!\n'
            + '\n'
            + '2\n'
            + '00:00:04.100 --> 00:00:08.000\n'
            + 'Enjoying this captions example?\n'
            + '\n'
            + '3\n'
            + '00:00:08.100 --> 00:00:12.500\n'
            + 'Last caption';
          this.$nextTick(() => this.$announcer.polite('Example captions were added'));
          this.$nextTick(() => this.validate());
        }
      },
      validate() {
        this.$refs.observer.validate();
      },
    },
  };
</script>

<style scoped>
.underline {
  text-decoration: underline;
}
</style>
