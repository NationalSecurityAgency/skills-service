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
    <b-overlay :show="loading.video || loadingSkill">
    <b-card>
      <div v-if="isReadOnly" class="alert alert-info" data-cy="readOnlyAlert">
        <i class="fas fa-exclamation-triangle" aria-hidden="true"/> Video attributes of <span
        v-if="isImported"><b-badge variant="success"><i class="fas fa-book" aria-hidden="true"/> Imported</b-badge></span><span v-if="isReused"><b-badge variant="success"><i class="fas fa-recycle" aria-hidden="true"/> Reused</b-badge></span>
        skills are read-only.
      </div>
      <div v-if="!isReadOnly && savedAtLeastOnce && skill && hasVideoUrl" data-cy="videoSelfReportAlert">
        <div v-if="skill.selfReportingType === 'Video'" class="alert alert-success">
          <i class="fas fa-file-video" style="font-size: 1.4rem;" aria-hidden="true"/>
          Users are required to watch this video in order to earn the skill and its points.
        </div>
        <div v-else class="alert alert-info">
          <i class="fas fa-exclamation-triangle" aria-hidden="true"/>
          Optionally set <i>Self Reporting</i> type to <b-badge>Video</b-badge> in order to award the skill for watching this video. Click the <i>Edit</i> button above to update the <i>Self Reporting</i> type.
        </div>
      </div>
      <b-alert>hi</b-alert>
      <ValidationObserver ref="observer" v-slot="{invalid, handleSubmit}" slim>
      <b-form-group label-for="videoUrlInput">
        <div slot="label">
          <div class="row">
            <div class="col my-auto">* Video:</div>
            <div class="col-auto">
              <b-button v-if="!showFileUpload" variant="outline-info" size="sm"
                        aria-label="Switch to Video Upload input option"
                        @click="switchToFileUploadOption" data-cy="showFileUploadBtn"><i class="fas fa-arrow-circle-up" aria-hidden="true" /> Switch to Upload</b-button>
              <b-button v-if="showFileUpload" variant="outline-info" size="sm"
                        aria-label="Clear uploaded video"
                        @click="switchToExternalUrlOption" data-cy="showExternalUrlBtn"><i class="fas fa-globe"></i> Switch to External Link</b-button>
            </div>
          </div>
        </div>

        <b-form-file id="videoUrlInput"
                     v-if="showFileUpload && !videoConf.isInternallyHosted"
                     v-model="videoConf.file"
                     @input="onFileUploadInput"
                     data-cy="videoFileUpload"
                     placeholder="Upload file from my computer by clicking Browse or drag-n-dropping it here..."
                     drop-placeholder="Drop file here..." />

        <ValidationProvider v-if="videoConf.isInternallyHosted" rules="videoMimeTypesValidation|videoMaxSizeValidation" v-slot="{ errors }" name="Video File" :immediate="true">
        <b-input-group>
          <template #prepend>
            <b-input-group-text><i class="fas fa-server mr-1"></i>
              SkillTree Hosted
            </b-input-group-text>
          </template>
          <b-form-input id="videoUrlInput"
                        v-model="videoConf.hostedFileName"
                        data-cy="videoUrl"
                        :disabled="isReadOnly" />
          <b-input-group-append>
            <b-button @click="switchToFileUploadOption"><i class="fa fa-broom" aria-hidden="true"/> Reset</b-button>
          </b-input-group-append>
        </b-input-group>
        <small role="alert" class="form-text text-danger" id="videoFileError" data-cy="videoFileError">{{errors[0]}}</small>
        </ValidationProvider>

        <ValidationProvider v-if="!showFileUpload && !videoConf.isInternallyHosted" rules="customUrlValidator" :debounce="250" v-slot="{ errors }" name="Video URL">
            <b-form-input id="videoUrlInput"
                          v-model="videoConf.url"
                          data-cy="videoUrl"
                          @input="validate"
                          placeholder="Please enter external URL"
                          :disabled="isReadOnly" />
          <small role="alert" class="form-text text-danger" id="videoUrlError" data-cy="videoUrlErr">{{errors[0]}}</small>
        </ValidationProvider>
      </b-form-group>

      <b-form-group label-for="videoCaptionsInput">
        <div slot="label">
          <div class="row">
            <div class="col my-auto">Captions:</div>
            <div v-if="!videoConf.captions && !isReadOnly" class="col-auto">
              <b-button variant="outline-info" size="sm"
                        aria-label="Click to fill in sample captions using The Web Video Text Tracks (WEBVTT) format"
                        @click="fillInCaptionsExample" data-cy="fillCaptionsExamples"><i class="fas fa-plus"></i> Add Example</b-button>
            </div>
          </div>
        </div>
        <ValidationProvider rules="maxVideoCaptionsLength|videoUrlMustBePresent" :debounce="250" v-slot="{ errors }" name="Captions">
          <b-form-textarea
            id="videoCaptionsInput"
            v-model="videoConf.captions"
            placeholder="Enter captions using The Web Video Text Tracks (WebVTT) format (optional)"
            rows="3"
            max-rows="6"
            data-cy="videoCaptions"
            :disabled="isReadOnly"
          ></b-form-textarea>
          <small role="alert" class="form-text text-danger" id="videoCaptionsError" data-cy="videoCaptionsError">{{errors[0]}}</small>
        </ValidationProvider>
      </b-form-group>

      <b-form-group label="Transcript:" label-for="videoTranscriptInput">
        <ValidationProvider rules="maxVideoTranscriptLength|customDescriptionValidator|videoUrlMustBePresent" :debounce="250" v-slot="{ errors }" name="Video Transcript">
        <b-form-textarea
          id="videoTranscriptInput"
          v-model="videoConf.transcript"
          placeholder="Please enter video's transcript here. Video transcript will be available for download (optional)"
          rows="3"
          max-rows="6"
          data-cy="videoTranscript"
          :disabled="isReadOnly"
        ></b-form-textarea>
          <small role="alert" id="videoTranscriptError" class="form-text text-danger" data-cy="videoTranscriptError">{{ errors[0] }}</small>
        </ValidationProvider>
      </b-form-group>

      <div v-if="overallErrMsg" class="alert alert-danger">
        {{ overallErrMsg }}
      </div>

        <div class="row" v-if="!isReadOnly">
          <div class="col-sm mt-2">
            <b-button variant="outline-success"
                      class="ml-2"
                      :disabled="!hasVideoUrl || invalid"
                      data-cy="saveVideoSettingsBtn"
                      aria-label="Save video settings"
                      @click="handleSubmit(submitSaveSettingsForm)">Save and Preview <i class="fas fa-save" aria-hidden="true"/></b-button>
            <span v-if="showSavedMsg" aria-hidden="true" class="ml-2 text-success" data-cy="savedMsg"><i class="fas fa-check" /> Saved</span>
          </div>
          <div class="col-auto mt-2">
            <b-button variant="outline-secondary"
                      class="mr-2"
                      data-cy="discardChangesBtn"
                      aria-label="Discard Unsaved"
                      @click="discardChanges">Discard Changes <i class="fas fa-sync" aria-hidden="true"/></b-button>
            <b-button variant="outline-danger"
                      :disabled="!formHasAnyData"
                      data-cy="clearVideoSettingsBtn"
                      aria-label="Clear video settings"
                      @click="confirmClearSettings">Clear <i class="fas fa-trash-alt" aria-hidden="true"/></b-button>
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
          <div v-if="!isDurationAvailable" class="alert alert-danger" data-cy="noDurationWarning">
            <i class="fas fa-exclamation-triangle" aria-hidden="true"/> Browser cannot derive the duration of this video. Percentage will only be updated after the video is fully watched.
          </div>
          <div class="row">
            <div class="col-6 col-lg-3 col-xl-2">Total Duration:</div>
            <div class="col">
              <span v-if="watchedProgress.videoDuration === Infinity" class="text-danger" data-cy="videoTotalDuration">N/A</span>
              <span v-else class="text-primary" data-cy="videoTotalDuration">{{ Math.trunc(watchedProgress.videoDuration * 1000) | formatDuration(true) }}</span>
            </div>
          </div>
          <div class="row">
            <div class="col-6 col-lg-3 col-xl-2">Time Watched:</div>
            <div class="col"><span class="text-primary" data-cy="videoTimeWatched">{{ Math.trunc(watchedProgress.totalWatchTime * 1000) | formatDuration(true) }}</span></div>
          </div>
          <div class="row">
            <div class="col-6 col-lg-3 col-xl-2">% Watched:</div>
            <div class="col">
              <span v-if="watchedProgress.videoDuration === Infinity" class="text-danger" data-cy="percentWatched">N/A</span>
              <span v-else class="text-primary" data-cy="percentWatched">{{ watchedProgress.percentWatched }}%</span>
            </div>
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
  import { createNamespacedHelpers } from 'vuex';
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import VideoService from '@/components/video/VideoService';
  import VideoPlayer from '@/common-components/video/VideoPlayer';
  import MsgBoxMixin from '@/components/utils/modal/MsgBoxMixin';
  import FileUploadService from '@/common-components/utilities/FileUploadService';

  const skills = createNamespacedHelpers('skills');

  export default {
    name: 'VideoConfigPage',
    components: { VideoPlayer, SubPageHeader },
    mixins: [MsgBoxMixin],
    data() {
      return {
        videoConf: {
          file: null,
          url: '',
          captions: '',
          transcript: '',
          isInternallyHosted: false,
          hostedFileName: '',
        },
        watchedProgress: null,
        isDurationAvailable: true,
        preview: false,
        refreshingPreview: false,
        loading: {
          video: true,
          skill: true,
        },
        showSavedMsg: false,
        overallErrMsg: null,
        showFileUpload: true,
        savedAtLeastOnce: false,
      };
    },
    created() {
      this.assignCustomValidation();
    },
    mounted() {
      this.loadSettings();
      this.loadSkillInfo();
    },
    computed: {
      ...skills.mapGetters([
        'skill',
      ]),
      ...skills.mapGetters([
        'loadingSkill',
      ]),
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
        return this.videoConf.url || this.videoConf.captions || this.videoConf.transcript;
      },
      isImported() {
        return this.skill && this.skill.copiedFromProjectId && this.skill.copiedFromProjectId.length > 0 && !this.skill.reusedSkill;
      },
      isReused() {
        return this.skill && this.skill.reusedSkill;
      },
      isReadOnly() {
        return this.isReused || this.isImported;
      },
    },
    methods: {
      ...skills.mapActions([
        'loadSkill',
      ]),
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
      onFileUploadInput(newFile) {
        this.showFileUpload = false;
        this.videoConf.isInternallyHosted = true;
        this.videoConf.hostedFileName = newFile.name;
        // basically a placeholder
        this.videoConf.url = `/${newFile.name}`;
        this.validate();
      },
      switchToFileUploadOption() {
        this.showFileUpload = true;
        this.clearVideoOptions();
      },
      switchToExternalUrlOption() {
        this.showFileUpload = false;
        this.clearVideoOptions();
      },
      clearVideoOptions() {
        this.videoConf.isInternallyHosted = false;
        this.videoConf.hostedFileName = '';
        this.videoConf.url = '';
        this.videoConf.file = null;
        this.validate();
      },
      saveSettings() {
        this.isDurationAvailable = true;
        this.preview = false;
        this.loading.video = true;
        const data = new FormData();
        if (this.videoConf.file) {
          data.append('file', this.videoConf.file);
        } else if (this.videoConf.url) {
          if (this.videoConf.isInternallyHosted) {
            data.append('isAlreadyHosted', this.videoConf.isInternallyHosted);
          } else {
            data.append('videoUrl', this.videoConf.url);
          }
        }
        if (this.videoConf.captions) {
          data.append('captions', this.videoConf.captions);
        }
        if (this.videoConf.transcript) {
          data.append('transcript', this.videoConf.transcript);
        }

        const endpoint = `/admin/projects/${this.$route.params.projectId}/skills/${this.$route.params.skillId}/video`;
        FileUploadService.upload(endpoint, data, (response) => {
          this.savedAtLeastOnce = true;
          this.updateVideoSettings(response.data);
          this.showSavedMsg = true;
          this.loading.video = false;
          setTimeout(() => {
            this.showSavedMsg = false;
          }, 3500);
          this.$nextTick(() => this.$announcer.polite('Video settings were saved'));
          this.setupPreview();
        }, () => {
          // console.log(err);
          this.loading.video = false;
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
        this.loading.video = true;
        this.videoConf.url = '';
        this.videoConf.videoType = '';
        this.videoConf.captions = '';
        this.videoConf.transcript = '';
        this.preview = false;
        this.isDurationAvailable = true;
        this.switchToFileUploadOption();
        VideoService.deleteVideoSettings(this.$route.params.projectId, this.$route.params.skillId)
          .finally(() => {
            this.loading.video = false;
            this.validate();
            this.$nextTick(() => this.$announcer.polite('Video settings were cleared'));
          });
      },
      discardChanges() {
        this.videoConf.file = null;
        this.loadSettings()
          .then(() => {
            this.validate();
          });
      },
      loadSettings() {
        this.loading.video = true;
        return VideoService.getVideoSettings(this.$route.params.projectId, this.$route.params.skillId)
          .then((settingRes) => {
            this.updateVideoSettings(settingRes);
          }).finally(() => {
            this.loading.video = false;
          });
      },
      updateVideoSettings(settingRes) {
        this.videoConf.url = settingRes.videoUrl;
        this.videoConf.videoType = settingRes.videoType;
        this.videoConf.captions = settingRes.captions;
        this.videoConf.transcript = settingRes.transcript;
        this.videoConf.isInternallyHosted = settingRes.isInternallyHosted;
        this.videoConf.hostedFileName = settingRes.internallyHostedFileName;
        if (this.videoConf.url) {
          this.showFileUpload = this.videoConf.isInternallyHosted;
          this.savedAtLeastOnce = true;
        } else {
          this.showFileUpload = true;
        }
      },
      loadSkillInfo() {
        this.loadSkill({
          projectId: this.$route.params.projectId,
          subjectId: this.$route.params.subjectId,
          skillId: this.$route.params.skillId,
        });
      },
      updatedWatchProgress(progress) {
        this.watchedProgress = progress;
        if (this.watchedProgress.videoDuration === Infinity) {
          this.isDurationAvailable = false;
        }
      },
      assignCustomValidation() {
        const self = this;
        extend('videoUrlMustBePresent', {
          message: (field) => `${field} is not valid without Video field`,
          validate() {
            const toValidate = self.videoConf.url ? self.videoConf.url.trim() : null;
            const hasUrl = toValidate !== null && toValidate.length > 0;
            const hasFile = self.videoConf.file;
            return hasUrl || hasFile;
          },
        });
        extend('videoMimeTypesValidation', {
          validate() {
            const supportedFileTypes = self.$store.getters.config.allowedVideoUploadMimeTypes;
            const { file } = self.videoConf;
            if (!file) {
              return true;
            }
            const res = supportedFileTypes.includes(file.type);
            if (res) {
              return true;
            }

            return `Unsupported [${file.type}] file type, supported types: [${supportedFileTypes}]`;
          },
        });
        extend('videoMaxSizeValidation', {
          validate() {
            const maxSize = self.$store.getters.config.maxAttachmentSize ? Number(self.$store.getters.config.maxAttachmentSize) : 0;
            const { file } = self.videoConf;
            if (!file) {
              return true;
            }
            const res = maxSize > file.size;
            if (res) {
              return true;
            }

            return `File exceeds maximum size of ${self.$options.filters.prettyBytes(maxSize)}`;
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
