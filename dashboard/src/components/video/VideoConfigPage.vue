/*
Copyright 2024 SkillTree

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
import { computed, nextTick, onMounted, ref, defineAsyncComponent } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import * as yup from 'yup';
import { string } from 'yup';
import { useForm } from 'vee-validate';
import { useByteFormat } from '@/common-components/filter/UseByteFormat.js';
import { useProjConfig } from '@/stores/UseProjConfig.js';
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import { useSkillsState } from '@/stores/UseSkillsState.js';
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js';
import FileUploadService from '@/common-components/utilities/FileUploadService.js';
import VideoService from '@/components/video/VideoService.js';
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue';
import LengthyOperationProgressBar from '@/components/utils/LengthyOperationProgressBar.vue';
import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue';
import SkillsTextInput from '@/components/utils/inputForm/SkillsTextInput.vue';
import VideoFileInput from '@/components/video/VideoFileInput.vue';
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js';
import { useUpgradeInProgressErrorChecker } from '@/components/utils/errors/UseUpgradeInProgressErrorChecker.js'
import { useProjectCommunityReplacement } from '@/components/customization/UseProjectCommunityReplacement.js'
import { WebVTTParser } from 'webvtt-parser';

const parser = new WebVTTParser();
const dialogMessages = useDialogMessages()
const VideoPlayer = defineAsyncComponent(() =>
  import('@/common-components/video/VideoPlayer.vue')
)

const skillsState = useSkillsState();
const route = useRoute();
const router = useRouter()
const upgradeInProgressErrorChecker = useUpgradeInProgressErrorChecker()
const appConfig = useAppConfig()
const projConfig = useProjConfig()
const projectCommunityReplacement = useProjectCommunityReplacement()
const timeUtils = useTimeUtils()
const announcer = useSkillsAnnouncer()
const byteFormat = useByteFormat()
const responsive = useResponsiveBreakpoints()
const container = route.params.projectId ? route.params.projectId : route.params.quizId;
const item = route.params.skillId ? route.params.skillId : route.params.questionId;
const isSkill = !!route.params.skillId;

const videoConf = ref({
  file: null,
  url: '',
  captions: '',
  transcript: '',
  isInternallyHosted: false,
  hostedFileName: '',
})

const watchedProgress = ref(null);
const isDurationAvailable = ref(true);
const preview = ref(false);
const refreshingPreview = ref(false);
const loading = ref({
  video: true,
  skill: isSkill,
});

const showSavedMsg = ref(false);
const overallErrMsg = ref(null);
const showFileUpload = ref(true);
const savedAtLeastOnce = ref(false);
const configuredWidth = ref(null);
const isConfiguredVideoSize = computed(() => configuredWidth.value && configuredHeight.value)
const configuredResolution = computed(() => isConfiguredVideoSize.value ? configuredWidth.value + " x " + configuredHeight.value : 'Not Configured')
const configuredHeight = ref(null);
const requestEndpoint = computed(() => {
  return isSkill ? `projects/${container}/skills/${item}` : `quiz-definitions/${container}/questions/${item}`;
})
const captionsEndpoint = computed(() => {
  return`/api/${requestEndpoint.value}/videoCaptions`
})
const computedVideoConf = computed(() => {
  const captionsUrl = videoConf.value.captions && videoConf.value.captions.trim().length > 0
      ? captionsEndpoint.value
      : null;
  return {
    url: videoConf.value.url,
    videoType: videoConf.value.videoType,
    isAudio: videoConf.value.videoType ? videoConf.value.videoType.includes('audio/') : false,
    captionsUrl,
    width: configuredWidth.value,
    height: configuredHeight.value,
  };
});
const lengthyOperationLoadingBarTimeout = computed(() => {
  const { file } = videoConf.value;
  if (!file || !file.size) {
    return 500;
  }
  const timeoutRatio = appConfig?.videoUploadLoadingBarLengthyCalculationTimeoutRatio || 75001;
  let timeout = Math.trunc(file.size / timeoutRatio);
  timeout = Math.min(1000, timeout);
  timeout = Math.max(100, timeout);
  return timeout;
});
const hasVideoUrl = computed(() => {
  return videoConf.value.url && videoConf.value.url.trim().length > 0;
});
const formHasAnyData = computed(() => {
  return videoConf.value.url || videoConf.value.captions || videoConf.value.transcript;
});
const isImported = computed(() => {
  return skillsState.skill && skillsState.skill.copiedFromProjectId && skillsState.skill.copiedFromProjectId.length > 0 && !skillsState.skill.reusedSkill;
});
const isReused = computed(() => {
  return skillsState.skill && skillsState.skill.reusedSkill;
});
const isReadOnly = computed(() => {
  return isReused.value || isImported.value;
});
const videoUploadWarningMessage = computed(() => {
  const warningMessageValue = appConfig?.videoUploadWarningMessage;
  try {
    return projectCommunityReplacement.populateProjectCommunity(warningMessageValue, projConfig.getProjectCommunityValue(), `projId=[${container}], skillId=[${item}] config.videoUploadWarningMessage `);
  } catch(err) {
    // eslint-disable-next-line vue/no-side-effects-in-computed-properties
    router.push({ name: 'ErrorPage', query: { err } });
  }
  return warningMessageValue
});

onMounted(() => {
  loadSettings();
});

const setupPreview = () => {
  if (preview.value) {
    // refreshingPreview.value = true;
  } else {
    preview.value = true;
    announcer.polite('Opened video preview card below. Navigate down to it.');
  }
}
const turnOffRefresh = () => {
  refreshingPreview.value = false;
}
const submitSaveSettingsForm = () => {
  // this.$refs.observer.validate()
  validate().then(({valid}) => {
    if (!valid) {
      overallErrMsg.value = 'Form did NOT pass validation, please fix and try to Save again';
    } else {
      saveSettings();
    }
  })
}
const onFileSelectedEvent = (selectFileEvent) => {
  const newFile = selectFileEvent.file
  videoConf.value.file = newFile;
  videoConf.value.isInternallyHosted = true;
  videoConf.value.hostedFileName = newFile.name;
  // basically a placeholder
  videoConf.value.url = `/${newFile.name}`;
  videoConf.value.videoType = newFile.type
  validate();
}
const switchToFileUploadOption = () => {
  showFileUpload.value = true;
  clearVideoOptions();
}
const switchToExternalUrlOption = () => {
  showFileUpload.value = false;
  clearVideoOptions();
}
const clearVideoOptions = () => {
  videoConf.value.isInternallyHosted = false;
  videoConf.value.hostedFileName = '';
  videoConf.value.url = '';
  videoConf.value.file = null;
  delete videoConf.value.videoType;
  preview.value = false;
  validate();
}
const saveSettings = () => {
  isDurationAvailable.value = true;
  preview.value = false;
  loading.value.video = true;

  const data = new FormData();
  if (videoConf.value.file) {
    data.append('file', videoConf.value.file);
  } else if (videoConf.value.url) {
    if (videoConf.value.isInternallyHosted) {
      data.append('isAlreadyHosted', videoConf.value.isInternallyHosted);
    } else {
      data.append('videoUrl', videoConf.value.url);
    }
  }
  if (videoConf.value.captions && !computedVideoConf.value.isAudio) {
    data.append('captions', videoConf.value.captions);
  }
  if (videoConf.value.transcript) {
    data.append('transcript', videoConf.value.transcript);
  }
  if (configuredWidth.value && configuredHeight.value) {
    data.append('width', configuredWidth.value)
    data.append('height', configuredHeight.value)
  }

  const endpoint = `/admin/${requestEndpoint.value}/video`;
  FileUploadService.upload(endpoint, data, (response) => {
    savedAtLeastOnce.value = true;
    updateVideoSettings(response.data);
    showSavedMsg.value = true;
    loading.value.video = false;
    unsavedVideoSizeChanges.value = false;
    setTimeout(() => {
      showSavedMsg.value = false;
    }, 3500);
    announcer.polite('Video settings were saved');
    setupPreview();
  }, (error) => {
    loading.value.video = false;
    upgradeInProgressErrorChecker.checkError(error)
  });
}
const confirmClearSettings = () => {
  dialogMessages.msgConfirm({
    message: 'Video settings will be permanently cleared. Are you sure you want to proceed?',
    header: 'Please Confirm!',
    acceptLabel: 'Yes, Do clear',
    rejectLabel: 'Cancel',
    accept: () => {
      clearSettings();
    }
  });
}
const clearSettings = () => {
  loading.value.video = true;
  videoConf.value.url = '';
  videoConf.value.videoType = '';
  videoConf.value.captions = '';
  videoConf.value.transcript = '';
  preview.value = false;
  isDurationAvailable.value = true;
  switchToFileUploadOption();
  VideoService.deleteVideoSettings(container, item, isSkill)
      .finally(() => {
        loading.value.video = false;
        validate();
        announcer.polite('Video settings were cleared');
      });
}
const discardChanges = () => {
  videoConf.value.file = null;
  loadSettings()
      .then(() => {
        validate();
      });
}
const loadSettings = () => {
  loading.value.video = true;
  return VideoService.getVideoSettings(container, item, isSkill)
      .then((settingRes) => {
        updateVideoSettings(settingRes);
      }).finally(() => {
        loading.value.video = false;
      });
}
const updateVideoSettings = (settingRes) => {
  videoConf.value.url = settingRes.videoUrl;
  videoConf.value.videoType = settingRes.videoType;
  videoConf.value.captions = settingRes.captions ? settingRes.captions.replaceAll('&gt;', '>') : '';
  videoConf.value.transcript = settingRes.transcript;
  videoConf.value.isInternallyHosted = settingRes.isInternallyHosted;
  videoConf.value.hostedFileName = settingRes.internallyHostedFileName;
  configuredWidth.value = settingRes.width
  configuredHeight.value = settingRes.height
  if (videoConf.value.url) {
    showFileUpload.value = videoConf.value.isInternallyHosted;
    savedAtLeastOnce.value = true;
  } else {
    showFileUpload.value = true;
  }
  setFieldValues();
  if (videoConf.value.url && savedAtLeastOnce.value) {
    setupPreview();
  }
}
const setFieldValues = () => {
  resetForm({
    values: {
      // videoFileInput: videoConf.value.hostedFileName,
      videoUrl: videoConf.value.url,
      videoCaptions: videoConf.value.captions,
      videoTranscript: videoConf.value.transcript,
    }
  });
}
const updatedWatchProgress = (progress) => {
  watchedProgress.value = progress;
  if (watchedProgress.value.videoDuration === Infinity) {
    isDurationAvailable.value = false;
  }
}
const fillInCaptionsExample = () => {
  if (!videoConf.value.captions) {
    videoConf.value.captions = 'WEBVTT\n'
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
    announcer.polite('Example captions were added');
    nextTick(() => validate());
  }
}

const videoUrlMustBePresent = (value) => {
  if (!value) {
    return true;
  }
  const toValidate = videoConf.value.url ? videoConf.value.url.trim() : null;
  const hasUrl = toValidate !== null && toValidate.length > 0;
  const hasFile = videoConf.value.file;
  return Boolean(hasUrl || hasFile);
}

const videoMimeTypesValidation = (value, context) => {
  const supportedFileTypes = appConfig.allowedVideoUploadMimeTypes;
  const { file } = videoConf.value;
  if (!file) {
    return true;
  }
  const res = supportedFileTypes.includes(file.type);
  if (res) {
    return true;
  }
  return context.createError({
    message: `Unsupported [${file.type}] file type, supported types: [${supportedFileTypes}]`,
  });
}
const videoMaxSizeValidation = (value, context) => {
  const { file } = videoConf.value;
  const maxSize = appConfig.maxAttachmentSize ? Number(appConfig.maxAttachmentSize) : 0;
  // const { file } = self.videoConf;
  if (!file) {
    return true;
  }
  const res = maxSize > file.size;
  if (res) {
    return true;
  }
  return context.createError({
    message: `File exceeds maximum size of ${byteFormat.prettyBytes(maxSize)}`,
  });
}
const webvttValidation = (value, context) => {
  if(!value || value?.length === 0) {
    return true;
  }

  const tree = parser.parse(value, 'metadata');

  if(tree.errors.length === 0) {
    return true;
  }

  return context.createError({
    message: `${tree.errors[0].message} (Line ${tree.errors[0].line})`
  })
}
const schema = yup.object().shape({
  'videoUrl': string()
      .nullable()
      .urlValidator()
      .label('Video URL'),
  'videoFileInput': yup.object()
      .nullable()
      // .required()
      .test('videoMimeTypesValidation', (value, context) => videoMimeTypesValidation(value, context))
      .test('videoMaxSizeValidation', (value, context) => videoMaxSizeValidation(value, context))
      .label('File'),
  'videoCaptions': yup.string()
      .nullable()
      .max(appConfig.maxVideoCaptionsLength)
      .test('videoUrlMustBePresent', 'Captions are not valid without a Video',(value) => videoUrlMustBePresent(value))
      .test('videoCaptionValidation', (value, context) => webvttValidation(value, context))
      .label('Captions'),
  'videoTranscript': string()
      .nullable()
      .max(appConfig.maxVideoTranscriptLength)
      .customDescriptionValidator('Video Transcript')
      .test('videoUrlMustBePresent', 'Transcript is not valid without a Video',(value) => videoUrlMustBePresent(value))
      .label('Video Transcript'),
})

const { values, meta, handleSubmit, resetForm, validate, errors } = useForm({ validationSchema: schema, })
const hasBeenResized = ref(false);
const unsavedVideoSizeChanges = ref(false)

const videoResized = (width, height) => {
  if(width !== configuredWidth.value && height !== configuredHeight.value) {
    hasBeenResized.value = true;
    unsavedVideoSizeChanges.value = true;
  }
  configuredWidth.value = width;
  configuredHeight.value = height;
}

const videoSettingGridCss = computed(() => 'grid sm:grid-cols-[10rem_1fr] sm:gap-4')

</script>

<template>
  <div>
    <SubPageHeader title="Configure Audio/Video">
      <router-link :to="{ name: 'Questions' }" v-if="route.params.quizId" tabindex="-1">
        <SkillsButton size="small" icon="fas fa-arrow-alt-circle-left" label="Back" />
      </router-link>
    </SubPageHeader>
    <SkillsOverlay :show="loading.video || (isSkill ? skillsState.loadingSkill : false) || appConfig.isLoadingConfig">
      <template v-if="videoConf.file" #overlay>
        <div class="text-center text-success pt-8">
          <div class="text-2xl mb-4"><i class="fas fa-video" aria-hidden="true"/> Uploading Video</div>
          <div class="w-9/12 mx-auto">
            <lengthy-operation-progress-bar :timeout="lengthyOperationLoadingBarTimeout" />
          </div>
        </div>
      </template>
      <Card>
        <template #content>
          <Message v-if="isReadOnly" severity="info" icon="fas fa-exclamation-triangle" data-cy="readOnlyAlert" :closable="false">
            Video attributes of
            <span v-if="isImported"><Tag severity="success"><i class="fas fa-book mr-1" aria-hidden="true"/> Imported</Tag></span>
            <span v-if="isReused"><Tag severity="success"><i class="fas fa-recycle mr-1" aria-hidden="true"/> Reused</Tag></span>
            skills are read-only.
          </Message>
          <div v-if="!isReadOnly && savedAtLeastOnce && isSkill && skillsState.skill && hasVideoUrl" data-cy="videoSelfReportAlert">
            <Message v-if="skillsState.skill.selfReportingType === 'Video'" severity="success" icon="fas fa-play-circle" class="alert alert-success" :closable="false">
              Users are required to {{ computedVideoConf.isAudio ? 'listen to this audio' : 'watch this video'}} in order to earn the skill and its points.
            </Message>
            <Message v-else severity="info" icon="fas fa-exclamation-triangle" :closable="false">
              Optionally set <i>Self Reporting</i> type to <Tag>Audio / Video</Tag> in order to award the skill for {{ computedVideoConf.isAudio ? 'listening to this audio' : 'watching this video'}}. Click the <i>Edit</i> button above to update the <i>Self Reporting</i> type.
            </Message>
          </div>

          <BlockUI :blocked="isReadOnly">
            <div data-cy="videoInputFields" class="mb-6" :class="{'flex flex-col gap-2': responsive.md.value }">
              <div class="flex flex-col md:flex-row gap-2 md:mb-2">
                <div class="flex-1 content-end">
                  <label>* Audio/Video:</label>
                </div>
                <div class="flex" >
                  <SkillsButton
                      v-if="!showFileUpload"
                      data-cy="showFileUploadBtn"
                      :disabled="isReadOnly"
                      size="small"
                      severity="info"
                      outlined
                      :class="{'w-full': responsive.md.value }"
                      aria-label="Switch to Video Upload input option"
                      @click="switchToFileUploadOption"
                      icon="fas fa-arrow-circle-up"
                      label="Switch to Upload">
                  </SkillsButton>
                  <SkillsButton
                      v-if="showFileUpload"
                      data-cy="showExternalUrlBtn"
                      :disabled="isReadOnly"
                      size="small"
                      severity="info"
                      outlined
                      :class="{'w-full': responsive.md.value }"
                      aria-label="Switch to External Link input option"
                      @click="switchToExternalUrlOption"
                      icon="fas fa-globe"
                      label="Switch to External Link">
                  </SkillsButton>
                </div>
              </div>

              <!-- upload file input component -->
              <VideoFileInput @file-selected="onFileSelectedEvent"
                              @reset="switchToFileUploadOption"
                              :disabled="isReadOnly"
                              name="selectedFile"
                              :showFileUpload="showFileUpload"
                              :hostedFileName="videoConf.hostedFileName"
                              :isInternallyHosted="videoConf.isInternallyHosted"
                              data-cy="videoFileInput"/>

              <Message v-if="videoConf.file && videoUploadWarningMessage" data-cy="videoUploadWarningMessage" severity="error" icon="fas fa-exclamation-circle" :closable="false">
                {{ videoUploadWarningMessage }}
              </Message>

              <!-- external URL input component-->
              <div v-if="!showFileUpload && !videoConf.isInternallyHosted">
                <SkillsTextInput id="videoUrlInput"
                                 v-model="videoConf.url"
                                 name="videoUrl"
                                 data-cy="videoUrl"
                                 @input="validate"
                                 placeholder="Please enter audio/video external URL"
                                 :disabled="isReadOnly"
                />
              </div>
            </div>

            <div data-cy="videoCaptionsInputFields" :class="{'flex flex-col gap-2': responsive.md.value }" v-if="!computedVideoConf.isAudio">
              <div class="flex flex-col md:flex-row gap-2 md:mb-2">
                <div class="flex-1 content-end">
                  <label for="videoCaptions">Captions:</label>
                </div>
                <div v-if="!videoConf.captions && !isReadOnly" class="flex">
                  <SkillsButton
                      data-cy="fillCaptionsExamples"
                      size="small"
                      severity="info"
                      outlined
                      :class="{'w-full': responsive.md.value }"
                      aria-label="Click to fill in sample captions using The Web Video Text Tracks (WEBVTT) format"
                      @click="fillInCaptionsExample"
                      icon="fas fa-plus"
                      label="Add Example">
                  </SkillsButton>
                </div>
              </div>
              <SkillsTextarea
                  id="videoCaptionsInput"
                  v-model="videoConf.captions"
                  placeholder="Enter captions using The Web Video Text Tracks (WebVTT) format (optional)"
                  aria-label="Enter captions using The Web Video Text Tracks (WebVTT) format (optional)"
                  rows="6"
                  max-rows="6"
                  name="videoCaptions"
                  data-cy="videoCaptions"
                  :disabled="isReadOnly"
              />
            </div>

            <div data-cy="videoTranscriptInput" class="mt-4">
              <div class="flex mb-2">
                <div class="flex-1 content-end">
                  <label for="videoTranscript">Transcript:</label>
                </div>
              </div>
              <SkillsTextarea
                  id="videoTranscriptInput"
                  v-model="videoConf.transcript"
                  placeholder="Please enter the transcript here. The transcript will be available for download (optional)"
                  aria-label="Please enter the transcript here. The transcript will be available for download (optional)"
                  rows="6"
                  max-rows="6"
                  name="videoTranscript"
                  data-cy="videoTranscript"
                  :disabled="isReadOnly"
              />
            </div>

            <Message severity="error" v-if="overallErrMsg">
              {{ overallErrMsg }}
            </Message>

            <div v-if="!isReadOnly" data-cy="updateButtons" class="my-4 flex flex-col md:flex-row gap-2">
              <div class="flex-1">
                <SkillsButton
                    severity="success"
                    :class="{'w-full': responsive.md.value }"
                    outlined
                    :disabled="!hasVideoUrl || !meta.valid"
                    data-cy="saveVideoSettingsBtn"
                    aria-label="Save video settings"
                    @click="submitSaveSettingsForm"
                    icon="fas fa-save"
                    label="Save and Preview" />
                <span v-if="showSavedMsg" aria-hidden="true" class="ml-2 text-success" data-cy="savedMsg"><i class="fas fa-check" /> Saved</span>
              </div>
              <div class="flex flex-col md:flex-row gap-2">
                <SkillsButton
                    severity="secondary"
                    class="md:mr-2"
                    :class="{'w-full': responsive.md.value }"
                    outlined
                    data-cy="discardChangesBtn"
                    aria-label="Discard Unsaved"
                    @click="discardChanges"
                    icon="fas fa-sync"
                    label="Discard Changes" />
                <SkillsButton
                    severity="danger"
                    outlined
                    :disabled="!formHasAnyData"
                    data-cy="clearVideoSettingsBtn"
                    id="clearVideoSettingsBtn"
                    :track-for-focus="true"
                    aria-label="Clear video settings"
                    @click="confirmClearSettings"
                    icon="fas fa-trash-alt"
                    label="Clear" />
              </div>
            </div>

          <!-- Video Preview -->
          <Card v-if="preview" class="mt-4" data-cy="videoPreviewCard" :pt="{ body: { class: '!p-0' } }">
            <template #header>
              <div class="border border-surface rounded-t bg-surface-100 dark:bg-surface-700 p-4">{{computedVideoConf.isAudio ? 'Audio' : 'Video'}} Preview</div>
            </template>
            <template #content>
              <VideoPlayer
                  v-if="!refreshingPreview"
                  :video-player-id="`videoConfigFor-${container}-${item}`"
                  :options="computedVideoConf"
                  @player-destroyed="turnOffRefresh"
                  @watched-progress="updatedWatchProgress"
                  @on-resize="videoResized"
                  :loadFromServer="true"
              />
              <div v-if="watchedProgress" class="p-4 pt-6 flex flex-col gap-2">
                <Message v-if="!isDurationAvailable" severity="warn" icon="fas fa-exclamation-triangle" :closable="false" data-cy="noDurationWarning">
                  Browser cannot derive the duration of this media. Percentage will only be updated after the media is fully viewed.
                </Message>
                <div class="grid md:grid-cols-[10rem_1fr] md:gap-4">
                  <div>Total Duration:</div>
                  <div>
                    <span v-if="watchedProgress.videoDuration === Infinity" class="text-danger" data-cy="videoTotalDuration">N/A</span>
                    <span v-else class="text-primary" data-cy="videoTotalDuration">{{ timeUtils.formatDuration(Math.trunc(watchedProgress.videoDuration * 1000), true) }}</span>
                  </div>
                </div>
                <div class="grid md:grid-cols-[10rem_1fr] md:gap-4">
                  <div>Time Played:</div>
                  <div><span class="text-primary" data-cy="videoTimeWatched">{{ timeUtils.formatDuration(Math.trunc(watchedProgress.totalWatchTime * 1000), true) }}</span></div>
                </div>
                <div class="grid md:grid-cols-[10rem_1fr] md:gap-4">
                  <div>% Played:</div>
                  <div>
                    <span v-if="watchedProgress.videoDuration === Infinity" class="text-danger" data-cy="percentWatched">N/A</span>
                    <span v-else class="text-primary" data-cy="percentWatched">{{ watchedProgress.percentWatched }}%</span>
                  </div>
                </div>
                <div class="grid grid-cols-[10rem_1fr] md:gap-4">
                  <div>Current Position:</div>
                  <div><span class="text-primary">{{ watchedProgress.currentPosition.toFixed(2) }}</span> <span class="italic">Seconds</span></div>
                </div>
                <div class="grid md:grid-cols-[10rem_1fr] md:gap-4" v-if="!computedVideoConf.isAudio">
                  <div>Default Video Size:</div>
                  <div>
                    <span class="text-primary" data-cy="defaultVideoSize">{{ configuredResolution }}</span> <Tag v-if="unsavedVideoSizeChanges" severity="warn" data-cy="unsavedVideoSizeChanges"><i class="fas fa-exclamation-circle mr-1" aria-hidden="true"></i>Unsaved Changes</Tag>
                    <div class="text-sm italic">** Change the size by dragging the handle at the bottom right of the video and click Save Changes button.</div>
                  </div>
                </div>
                <div class="grid md:grid-cols-[10rem_1fr] md:gap-4">
                  <div>Played Segments:</div>
                  <div>
                    <div v-if="watchedProgress.currentStart !== null && watchedProgress.lastKnownStopPosition"> <span class="text-primary">{{ watchedProgress.currentStart.toFixed(2) }}</span>
                      <i class="fas fa-arrow-circle-right text-secondary mx-2" :aria-hidden="true"/>
                      <span class="text-primary">{{ watchedProgress.lastKnownStopPosition.toFixed(2) }}</span> <span class="italic">Seconds</span>
                    </div>
                    <div v-for="segment in watchedProgress.watchSegments" :key="segment.start"><span class="text-primary">{{ segment.start.toFixed(2) }}</span>
                      <i class="fas fa-arrow-circle-right text-secondary mx-2" :aria-hidden="true"/><span class="text-primary">{{ segment.stop.toFixed(2) }}</span> <span class="italic">Seconds</span>
                    </div>
                  </div>
                </div>

                  <div v-if="!isReadOnly && hasBeenResized" class="flex items-center">
                    <SkillsButton
                        severity="success"
                        class="mt-2"
                        :class="{'w-full': responsive.md.value }"
                        outlined
                        :disabled="!hasVideoUrl || !meta.valid"
                        data-cy="updateVideoSettings"
                        aria-label="Save video settings"
                        @click="submitSaveSettingsForm"
                        icon="fas fa-save"
                        label="Save Changes" />
                    <InlineMessage v-if="showSavedMsg" aria-hidden="true" class="ml-4" data-cy="savedMsg" severity="success" size="small" icon="fas fa-check">Saved</InlineMessage>
                  </div>
                </div>

              </template>
            </Card>
          </BlockUI>

        </template>

      </Card>
    </SkillsOverlay>
  </div>
</template>

<style scoped></style>
