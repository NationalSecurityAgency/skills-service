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
import {computed, onMounted, ref} from "vue";
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
import SlideDeck from "@/components/slides/SlideDeck.vue";
import {useRoute, useRouter} from "vue-router";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import SkillsTextInput from "@/components/utils/inputForm/SkillsTextInput.vue";
import {useQuizConfig} from "@/stores/UseQuizConfig.js";
import {useProjConfig} from "@/stores/UseProjConfig.js";
import {useProjectCommunityReplacement} from "@/components/customization/UseProjectCommunityReplacement.js";
import SlidesService from "@/components/slides/SlidesService.js";
import {useLayoutSizesState} from "@/stores/UseLayoutSizesState.js";

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

const container = route.params.projectId ? route.params.projectId : route.params.quizId;
const item = route.params.skillId ? route.params.skillId : route.params.questionId;
const isSkill = !!route.params.skillId;

const slidesConf = ref({
  file: null,
  url: '',
  transcript: '',
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
      data.append('slidesUrl', slidesConf.value.url);
    }
  }
  const endpoint = `/admin/${requestEndpoint.value}/slides`;
  FileUploadService.upload(endpoint, data, (response) => {
    updateSlidesSettings(response.data);
    showSavedMsg.value = true;
    loading.value.video = false;
    setTimeout(() => {
      showSavedMsg.value = false;
    }, 3500);
    announcer.polite('Slides settings were saved');
    setupPreview();
  }, (error) => {
    isSaving.value = false;
    upgradeInProgressErrorChecker.checkError(error)
  });
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

const videoUploadWarningMessage = computed(() => {
  const warningMessageValue = appConfig?.videoUploadWarningMessage;
  try {

    let communityValue = null
    if (route.params.projectId) {
      communityValue = projConfig.getProjectCommunityValue();
    } else if (route.params.quizId) {
      communityValue = quizConfig.quizCommunityValue;
    }
    return projectCommunityReplacement.populateProjectCommunity(warningMessageValue, communityValue, `projId=[${container}], skillId=[${item}] config.videoUploadWarningMessage `);
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
</script>

<template>
  <div>
    <SubPageHeader title="Configure Slides"/>
    <Card>
      <template #content>
        <skills-spinner v-if="loading" :is-loading="loading" />
        <div v-else>
          <div class="flex flex-col gap-2">
            <label>* Slides:</label>
            <video-file-input
                @file-selected="onFileSelectedEvent"
                :show-file-upload="true"
                :hosted-file-name="slidesConf.hostedFileName"
                :isInternallyHosted="slidesConf.isInternallyHosted"
                name="slidesFile"
                data-cy="slidesFileInput"/>

            <Message
                v-if="errors && errors['slidesFile']"
                severity="error"
                variant="simple"
                size="small"
                :closable="false"
                :data-cy="`${name}Error`"
                :id="`${name}Error`">
              {{ errors['slidesFile'] }}
            </Message>

            <Message v-if="slidesConf.file && videoUploadWarningMessage"
                     data-cy="slidesUploadWarningMessage"
                     severity="error"
                     icon="fas fa-exclamation-circle" :closable="false">
              {{ videoUploadWarningMessage }}
            </Message>

            <div v-if="!showFileUpload && !skillsConf.isInternallyHosted">
              <SkillsTextInput id="videoUrlInput"
                               v-model="skillsConf.url"
                               name="videoUrl"
                               data-cy="videoUrl"
                               @input="validate"
                               placeholder="Please enter audio/video external URL"
                               :disabled="isReadOnly"
              />
            </div>
          </div>

          <Message severity="error" v-if="overallErrMsg">
            {{ overallErrMsg }}
          </Message>

          <div class="flex-1 mt-3">
            <SkillsButton
                severity="success"
                data-cy="saveSlidesSettingsBtn"
                aria-label="Save slides settings"
                @click="submitSaveSettingsForm"
                icon="fas fa-save"
                label="Save and Preview"/>
            <span v-if="showSavedMsg" aria-hidden="true" class="ml-2 text-success" data-cy="savedMsg"><i
                class="fas fa-check"/> Saved</span>
          </div>


          <Card v-if="preview" class="mt-4" data-cy="slidesPreviewCard" :pt="{ body: { class: 'p-0!' } }">
            <template #header>
              <div class="border border-surface rounded-t bg-surface-100 dark:bg-surface-700 p-4">Slides Preview</div>
            </template>
            <template #content>
              <slide-deck
                  class="my-5"
                  :pdf-url="slidesConf.url"
                  :max-width="layoutSize.tableMaxWidth-30"
              />
            </template>
          </Card>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>