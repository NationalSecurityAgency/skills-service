/*
Copyright 2026 SkillTree

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
import {ref} from 'vue'
import {object, string} from 'yup'
import {useRoute} from 'vue-router'
import {useAppConfig} from '@/common-components/stores/UseAppConfig.js'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import SkillsNameAndIdInput from "@/components/utils/inputForm/SkillsNameAndIdInput.vue";

const model = defineModel()
const emit = defineEmits(['added-tag'])
const props = defineProps({
  tagIdToEdit: {
    type: String,
    required: false
  }
})
const appConfig = useAppConfig()
const route = useRoute()

const header = props.tagIdToEdit ? 'Edit Skill Tag' : 'Create New Skill Tag'
const isEdit = !!props.tagIdToEdit

const loadingExistingTags = ref(true)
const existingTags = ref([])
const origTagValue = ref({tagId: '', tagValue: ''})

const isTagValueAlreadyPresent = (value) => {
  if (!value) {
    return true
  }
  const searchFor = value.toString().trim().toLocaleLowerCase()
  const existingTag = existingTags.value.find((item) => item.tagValue?.toString()?.toLowerCase() === searchFor)
  return existingTag === undefined
}

const isTagIdAlreadyPresent = (value) => {
  if (!value) {
    return true
  }
  const searchFor = value.toString().trim().toLocaleLowerCase()
  const existingTag = existingTags.value.find((item) => item.tagId?.toString()?.toLowerCase() === searchFor)
  return existingTag === undefined
}

const tagValueDifferentDuringEdit = (value, context) => {
  if (!value || !isEdit || !origTagValue.value?.tagValue) {
    return true
  }
  const originalTagValue = origTagValue.value.tagValue?.toString()?.trim()?.toLowerCase()
  const incomingTagValue = value?.toString()?.trim()?.toLowerCase()

  const originalIdValue = origTagValue.value.tagId?.toString()?.trim()?.toLowerCase()
  const incomingIdValue = context.parent?.tagId?.toString()?.trim()?.toLowerCase()

  return originalTagValue !== incomingTagValue || originalIdValue !== incomingIdValue
}

const tagIdDifferentDuringEdit = (value, context) => {
  if (!value || !isEdit || !origTagValue.value?.tagId) {
    return true
  }
  const originalTagValue = origTagValue.value.tagValue?.toString()?.trim()?.toLowerCase()
  const incomingTagValue = context.parent?.tagValue?.toString()?.trim()?.toLowerCase()

  const originalTagId = origTagValue.value.tagId?.toString()?.trim()?.toLowerCase()
  const incomingTagId = value?.toString()?.trim()?.toLowerCase()

  return originalTagId !== incomingTagId || originalTagValue !== incomingTagValue
}

const schema = object({
  'tagValue': string()
      .trim()
      .required()
      .max(appConfig.maxSkillTagLength)
      .test(
          'isNotAnExistingTag',
          ({label}) => `${label} already exists`,
          async (value) => isTagValueAlreadyPresent(value)
      )
      .test(
          'isNotSameTagDuringEdit',
          ({label}) => `${label} needs to be different`,
          async (value, context) => tagValueDifferentDuringEdit(value, context)
      )
      .label('Tag'),
  'tagId': string()
      .trim()
      .required()
      .matches(/^[a-zA-Z0-9]+$/, ({label}) => `${label} may only contain alpha-numeric characters`)
      .max(appConfig.maxSkillTagLength)
      .test(
          'isNotAnExistingIdTag',
          ({label}) => `${label} already exists`,
          async (value) => isTagIdAlreadyPresent(value)
      )
      .test(
          'isNotSameTagIdDuringEdit',
          ({label}) => `${label} needs to be different`,
          async (value, context) => tagIdDifferentDuringEdit(value, context)
      )
      .label('Tag ID'),
})

const loadExistingTags = () => {
  loadingExistingTags.value = true
  return SkillsService.getTagsForProject(route.params.projectId)
    .then((res) => {
      existingTags.value = res;
      if (props.tagIdToEdit) {
        const foundTag = existingTags.value.find((item) => item.tagId?.toString()?.toLowerCase() === props.tagIdToEdit?.toString()?.toLowerCase())
        if (foundTag) {
          origTagValue.value = ({...foundTag})
          initialData.value = ({...foundTag})
          existingTags.value = existingTags.value.filter((item) => item.tagId !== foundTag.tagId)
        }
      }
      return res
    }).finally(() => {
        loadingExistingTags.value = false
      })
}
const initialData = ref({
  'tagValue': '',
  'tagId': '',
})
const saveTag = (tagToSave) => {
  const tagValue = tagToSave.tagValue?.trim()
  const tagId = tagToSave.tagId?.trim();
  const origTagId = props.tagIdToEdit || null
  return SkillsService.addTagToSkills(route.params.projectId, [], tagId, tagValue, origTagId)
    .then(() => {
      return {tagId, tagValue, origTagId}
    });
}
const afterSave = (taggedInfo) => {
  emit('added-tag', {...taggedInfo, operation: props.tagIdToEdit ? 'edit' : 'add' })
}

</script>

<template>
  <SkillsInputFormDialog
    id="createTagDialog"
    :header="header"
    v-model="model"
    :save-data-function="saveTag"
    @saved="afterSave"
    :async-load-data-function="loadExistingTags"
    :loading="loadingExistingTags"
    :validation-schema="schema"
    :initial-values="initialData"
    :enable-return-focus="true"
    :enable-input-form-resiliency="false"
    data-cy="createTagDialog"
    dialog-class="w-11/12 sm:w-10/12 lg:w-9/12 xl:w-7/12"
  >
    <SkillsNameAndIdInput
        name-label="Tag"
        name-field-name="tagValue"
        id-label="Tag ID"
        id-field-name="tagId"
        :name-to-id-sync-enabled="!isEdit">
    </SkillsNameAndIdInput>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>