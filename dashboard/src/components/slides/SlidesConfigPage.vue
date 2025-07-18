<script setup>
import {computed, ref} from "vue";
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

const byteFormat = useByteFormat()
const announcer = useSkillsAnnouncer()
const upgradeInProgressErrorChecker = useUpgradeInProgressErrorChecker()

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
const isSaving = ref(false);
const showSavedMsg = ref(false);
const showFileUpload = ref(true);
const preview = ref(false);
const overallErrMsg = ref(null);

const slidesMimeTypesValidation = (value, context) => {
  const supportedFileTypes = appConfig.allowedVideoUploadMimeTypes;
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
  'slidesFileInput': yup.object()
      .nullable()
      // .required()
      .test('videoMimeTypesValidation', (value, context) => slidesMimeTypesValidation(value, context))
      .test('videoMaxSizeValidation', (value, context) => slidesMaxSizeValidation(value, context))
      .label('File'),
})

const {values, meta, handleSubmit, resetForm, validate, errors} = useForm({validationSchema: schema,})


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
  slidesConf.value.url = settingRes.slidesUrl;
  slidesConf.value.slidesType = settingRes.slidesType;
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

</script>

<template>
  <div>
    <SubPageHeader title="Configure Slides"/>
    <Card>
      <template #content>
        <div class="flex flex-col gap-2">
          <label>* Slides:</label>
          <video-file-input
              @file-selected="onFileSelectedEvent"
              :show-file-upload="true"
              :hosted-file-name="slidesConf.hostedFileName"
              :isInternallyHosted="slidesConf.isInternallyHosted"
              name="selectSlidesFile"
              data-cy="slidesFileInput"/>
        </div>

        <Message severity="error" v-if="overallErrMsg">
          {{ overallErrMsg }}
        </Message>

        <div class="flex-1">
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
                class="mb-5"
                :pdf-url="slidesConf.url"
            />
          </template>
        </Card>

      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>