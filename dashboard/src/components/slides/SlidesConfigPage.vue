/*
Copyright 2025 SkillTree

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
import {computed, defineAsyncComponent, onMounted, ref} from "vue";
import { SkillsReporter } from '@skilltree/skills-client-js'
import SubPageHeader from "@/components/utils/pages/SubPageHeader.vue";
import VideoFileInput from "@/components/video/VideoFileInput.vue";
import SkillsButton from "@/components/utils/inputForm/SkillsButton.vue";
import FileUploadService from "@/common-components/utilities/FileUploadService.js";
import * as yup from "yup";
import {string} from "yup";
import {useForm} from "vee-validate";
import {useByteFormat} from "@/common-components/filter/UseByteFormat.js";
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";
import {useUpgradeInProgressErrorChecker} from "@/components/utils/errors/UseUpgradeInProgressErrorChecker.js";
import {useRoute, useRouter} from "vue-router";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import SkillsTextInput from "@/components/utils/inputForm/SkillsTextInput.vue";
import {useQuizConfig} from "@/stores/UseQuizConfig.js";
import {useProjConfig} from "@/stores/UseProjConfig.js";
import {useProjectCommunityReplacement} from "@/components/customization/UseProjectCommunityReplacement.js";
import SlidesService from "@/components/slides/SlidesService.js";
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";
import {useSkillsState} from "@/stores/UseSkillsState.js";
import {useResponsiveBreakpoints} from "@/components/utils/misc/UseResponsiveBreakpoints.js";
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";
import ReminderMessage from "@/components/utils/misc/ReminderMessage.vue";

const SlideDeck = defineAsyncComponent(() =>
    import('@/components/slides/SlideDeck.vue')
);

const byteFormat = useByteFormat()
const announcer = useSkillsAnnouncer()
const upgradeInProgressErrorChecker = useUpgradeInProgressErrorChecker()
const route = useRoute()
const router = useRouter()
const appConfig = useAppConfig()
const quizConfig = useQuizConfig()
const projConfig = useProjConfig()
const projectCommunityReplacement = useProjectCommunityReplacement()
const layoutSize = useLayoutSizesState()
const skillsState = useSkillsState();
const responsive = useResponsiveBreakpoints()
const dialogMessages = useDialogMessages()

const container = route.params.projectId ? route.params.projectId : route.params.quizId;
const item = route.params.skillId ? route.params.skillId : route.params.questionId;
const isSkill = !!route.params.skillId;

const slidesConf = ref({
  file: null,
  url: '',
  isInternallyHosted: false,
  hostedFileName: '',
})
const loading = ref(true)
const isSaving = ref(false);
const showSavedMsg = ref(false);
const showFileUpload = ref(true);
const preview = ref(false);
const overallErrMsg = ref(null);


const slidesMimeTypesValidation = (value, context) => {
  const supportedFileTypes = appConfig.allowedSlidesUploadMimeTypes;
  const {file} = slidesConf.value;
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
const slidesMaxSizeValidation = (value, context) => {
  const {file} = slidesConf.value;
  const maxSize = appConfig.maxAttachmentSize ? Number(appConfig.maxAttachmentSize) : 0;
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

const schema = yup.object().shape({
  'slidesUrl': string()
      .nullable()
      .urlValidator()
      .label('Video URL'),
  'slidesFile': yup.object()
      .nullable()
      // .required()
      .test('videoMimeTypesValidation', (value, context) => slidesMimeTypesValidation(value, context))
      .test('videoMaxSizeValidation', (value, context) => slidesMaxSizeValidation(value, context))
      .label('File'),
})

const {values, meta, handleSubmit, resetForm, validate, errors} = useForm({validationSchema: schema,})

onMounted(() => {
  loadSettings();
});


const loadSettings = () => {
  loading.value = true;
  return SlidesService.getSettings(container, item, isSkill)
      .then((settingRes) => {
        updateSlidesSettings(settingRes);
      }).finally(() => {
        loading.value = false;
      });
}

const onFileSelectedEvent = (selectFileEvent) => {
  const newFile = selectFileEvent.file
  slidesConf.value.file = newFile;
  slidesConf.value.isInternallyHosted = true;
  slidesConf.value.hostedFileName = newFile.name;
  // basically a placeholder
  slidesConf.value.url = `/${newFile.name}`;
  slidesConf.value.videoType = newFile.type
  validate();
}

const submitSaveSettingsForm = () => {
  validate().then(({valid}) => {
    if (!valid) {
      overallErrMsg.value = 'Form did NOT pass validation, please fix and try to Save again';
    } else {
      saveSettings();
    }
  })
}

const requestEndpoint = computed(() => {
  return isSkill ? `projects/${container}/skills/${item}` : `quiz-definitions/${container}/questions/${item}`;
})
const saveSettings = () => {
  isSaving.value = true;

  const data = new FormData();
  if (slidesConf.value.file) {
    data.append('file', slidesConf.value.file);
  } else if (slidesConf.value.url) {
    if (slidesConf.value.isInternallyHosted) {
      data.append('isAlreadyHosted', slidesConf.value.isInternallyHosted);
    } else {
      data.append('url', slidesConf.value.url);
    }
  }
  if (configuredSlidesWidthValue.value) {
    data.append('width', configuredSlidesWidthValue.value);
  }

  const endpoint = `/admin/${requestEndpoint.value}/slides`;
  return FileUploadService.upload(endpoint, data, (response) => {
    updateSlidesSettings(response.data);
    showSavedMsg.value = true;
    loading.value = false;
    setTimeout(() => {
      showSavedMsg.value = false;
    }, 3500);
    setupPreview();
    isSaving.value = false;
    unsavedConfigChanges.value = false;
    announcer.polite('Slides settings were saved');
    SkillsReporter.reportSkill('AddSkillSlides')
  }, (error) => {
    upgradeInProgressErrorChecker.checkError(error)
  })
}

const updateSlidesSettings = (settingRes) => {
  slidesConf.value.url = settingRes.url;
  slidesConf.value.slidesType = settingRes.type;
  slidesConf.value.isInternallyHosted = settingRes.isInternallyHosted;
  slidesConf.value.hostedFileName = settingRes.internallyHostedFileName;
  if (slidesConf.value.url) {
    showFileUpload.value = slidesConf.value.isInternallyHosted;
  } else {
    showFileUpload.value = true;
  }
  if (settingRes.width) {
    configuredSlidesWidthValue.value = settingRes.width;
  }
  setFieldValues();
  if (slidesConf.value.url) {
    setupPreview();
  }
}
const setFieldValues = () => {
  resetForm({
    values: {
      videoUrl: slidesConf.value.url,
    }
  });
}

const setupPreview = () => {
  if (!preview.value) {
    preview.value = true;
    announcer.polite('Opened slides preview card below. Navigate down to it.');
  }
}

const slidesUploadWarningMessage = computed(() => {
  const warningMessageValue = appConfig?.slidesUploadWarningMessage;
  try {

    let communityValue = null
    if (route.params.projectId) {
      communityValue = projConfig.getProjectCommunityValue();
    } else if (route.params.quizId) {
      communityValue = quizConfig.quizCommunityValue;
    }
    return projectCommunityReplacement.populateProjectCommunity(warningMessageValue, communityValue, `projId=[${container}], skillId=[${item}] config.slidesUploadWarningMessage `);
  } catch (err) {
    // eslint-disable-next-line vue/no-side-effects-in-computed-properties
    console.error(err)
    router.push({name: 'ErrorPage', query: {err}});
  }
  return warningMessageValue
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

const unsavedConfigChanges = ref(false)
const configuredSlidesWidthValue = ref(null)
const hasBeenResized = ref(false)
const configuredSlidesWidth = computed(() => configuredSlidesWidthValue.value || 'No width configured yet')
const onSlidesResize = (newWidth) => {
  configuredSlidesWidthValue.value = Math.trunc(newWidth)
  unsavedConfigChanges.value = true
  hasBeenResized.value = true
}

const switchToFileUploadOption = () => {
  showFileUpload.value = true;
  clearSlidesOptions();
}
const switchToExternalUrlOption = () => {
  showFileUpload.value = false;
  clearSlidesOptions();
}
const clearSlidesOptions = () => {
  slidesConf.value.isInternallyHosted = false;
  slidesConf.value.hostedFileName = '';
  slidesConf.value.url = '';
  slidesConf.value.file = null;
  preview.value = false;
  validate();
}
const hasSlidesUrl = computed(() => {
  return slidesConf.value.url && slidesConf.value.url.trim().length > 0;
});
const confirmClearSettings = () => {
  dialogMessages.msgConfirm({
    message: 'Slide settings will be permanently cleared. Are you sure you want to proceed?',
    header: 'Please Confirm!',
    acceptLabel: 'Yes, Do clear',
    rejectLabel: 'Cancel',
    accept: () => {
      clearSettings();
    }
  });
}
const clearSettings = () => {
  loading.value = true;
  slidesConf.value.url = '';
  slidesConf.value.type = '';
  slidesConf.value.file = null;
  preview.value = false;
  switchToFileUploadOption();
  SlidesService.deleteSettings(container, item, isSkill)
      .finally(() => {
        loading.value = false;
        validate();
        announcer.polite('Slides settings were cleared');
      });
}
const formHasAnyData = computed(() => {
  return slidesConf.value.url;
});
</script>

<template>
  <div>
    <SubPageHeader title="Configure Slides"/>
    <Card>
      <template #content>
        <skills-spinner v-if="loading" :is-loading="loading" />
        <div v-else>
          <Message v-if="isReadOnly" severity="info" icon="fas fa-exclamation-triangle" data-cy="readOnlyAlert" :closable="false">
            Slide attributes of
            <span v-if="isImported"><Tag severity="success"><i class="fas fa-book mr-1" aria-hidden="true"/> Imported</Tag></span>
            <span v-if="isReused"><Tag severity="success"><i class="fas fa-recycle mr-1" aria-hidden="true"/> Reused</Tag></span>
            skills are read-only.
          </Message>
          <reminder-message v-if="!isReadOnly"
              icon="fas fa-lightbulb"
              id="slideDeckCreationReminderMsg"
              class="mb-5"
              :expireAfterMins="20160">
            PRO TIP: Build your slide deck in your preferred presentation tool (ex. PowerPoint), then export your finished deck as a PDF and upload it here.
          </reminder-message>

          <div class="flex flex-col gap-2">
            <div class="flex flex-col md:flex-row gap-2 md:mb-2">
              <div class="flex-1 content-end">
                <label>* PDF Slides:</label>
              </div>
              <div class="flex" v-if="!isReadOnly">
                <SkillsButton
                    v-if="!showFileUpload"
                    data-cy="showFileUploadBtn"
                    :disabled="isReadOnly"
                    size="small"
                    severity="info"
                    outlined
                    :class="{'w-full': responsive.md.value }"
                    aria-label="Switch to Slides Upload input option"
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
            <video-file-input
                v-if="showFileUpload"
                @file-selected="onFileSelectedEvent"
                @reset="switchToFileUploadOption"
                :hosted-file-name="slidesConf.hostedFileName"
                :show-file-upload="showFileUpload"
                :isInternallyHosted="slidesConf.isInternallyHosted"
                :disabled="isReadOnly"
                name="slidesFile"
                data-cy="slidesFileInput"/>

            <Message
                v-if="errors && errors['slidesFile']"
                severity="error"
                variant="simple"
                size="small"
                :closable="false"
                data-cy="slidesFileError"
                id="slidesFileError">
              {{ errors['slidesFile'] }}
            </Message>

            <Message v-if="slidesConf.file && slidesUploadWarningMessage"
                     data-cy="slidesUploadWarningMessage"
                     severity="error" :closable="false">
              {{ slidesUploadWarningMessage }}
            </Message>

            <div v-if="!showFileUpload && !slidesConf.isInternallyHosted">
              <SkillsTextInput id="pdfUrlInput"
                               v-model="slidesConf.url"
                               name="pdfUrl"
                               data-cy="pdfUrl"
                               @input="validate"
                               placeholder="Please enter pdf external URL"
                               aria-label="PDF external URL"
                               :disabled="isReadOnly"
              />
            </div>
          </div>

          <Message severity="error" v-if="overallErrMsg">
            {{ overallErrMsg }}
          </Message>

          <div v-if="!isReadOnly" class="flex-1 mt-4 flex flex-col md:flex-row gap-2">
            <div class="flex-1 flex gap-2">
              <SkillsButton
                  severity="success"
                  data-cy="saveSlidesSettingsBtn"
                  aria-label="Save slides settings"
                  @click="submitSaveSettingsForm"
                  :disabled="!hasSlidesUrl || !meta.valid"
                  icon="fas fa-save"
                  label="Save and Preview"/>
              <InlineMessage v-if="showSavedMsg" aria-hidden="true" data-cy="savedMsg" severity="success"
                             size="small" icon="fas fa-check">Saved
              </InlineMessage>
            </div>
            <div>
              <SkillsButton
                  severity="danger"
                  outlined
                  :disabled="!formHasAnyData"
                  data-cy="clearSlidesSettingsBtn"
                  id="clearSlidesSettingsBtn"
                  :track-for-focus="true"
                  aria-label="Clear slides settings"
                  @click="confirmClearSettings"
                  icon="fas fa-trash-alt"
                  label="Clear"/>
            </div>

          </div>

          <Card v-if="preview" class="mt-7" data-cy="slidesPreviewCard" :pt="{ body: { class: 'p-0!' } }">
            <template #header>
              <div class="border border-surface rounded-t bg-surface-100 dark:bg-surface-700 p-4 uppercase">Preview</div>
            </template>
            <template #content>
              <slide-deck
                  class="my-5"
                  :slides-id="`${route.params.projectId}-${route.params.skillId}`"
                  :pdf-url="slidesConf.url"
                  :default-width="configuredSlidesWidthValue"
                  :max-width="layoutSize.tableMaxWidth-50"
                  :able-to-resize="!isReadOnly"
                  @on-resize="onSlidesResize"
              />

              <hr />
              <div class="px-5 py-5">
                <div class="grid md:grid-cols-[10rem_1fr] md:gap-4">
                  <div>Default Slides Width:</div>
                  <div>
                    <span class="text-primary" data-cy="defaultVideoSize">{{ configuredSlidesWidth }}</span> <Tag v-if="unsavedConfigChanges" severity="warn" data-cy="unsavedVideoSizeChanges"><i class="fas fa-exclamation-circle mr-1" aria-hidden="true"></i>Unsaved Changes</Tag>
                    <div class="text-sm italic">** Change the size by dragging the handle at the bottom right of the slides and click Save Changes button.</div>
                  </div>
                </div>

                <div v-if="!isReadOnly && hasBeenResized" class="flex items-center">
                  <SkillsButton
                      severity="success"
                      class="mt-2"
                      outlined
                      :disabled="!meta.valid || !unsavedConfigChanges"
                      data-cy="updateSlidesSettingsBtn"
                      aria-label="Save video settings"
                      @click="submitSaveSettingsForm"
                      :loading="isSaving"
                      icon="fas fa-save"
                      label="Save Changes" />
                  <InlineMessage v-if="showSavedMsg" aria-hidden="true" class="ml-4" data-cy="savedMsgSecondBtn" severity="success" size="small" icon="fas fa-check">Saved</InlineMessage>
                </div>

              </div>

            </template>
          </Card>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>