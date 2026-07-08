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
import InputSanitizer from '@/components/utils/InputSanitizer.js'

const model = defineModel()
const emit = defineEmits(['added-tag'])
const appConfig = useAppConfig()
const route = useRoute()

const loadingExistingTags = ref(true)
const existingTags = ref([])
const validateAgainstCurrentTags = (value) => {
  if (!value) {
    return true
  }
  const tagInfo = constructTagInfo(value)
  const tagId = tagInfo.tagId?.toString()?.toLowerCase()
  const tagValue = tagInfo.tagValue?.toString()?.toLowerCase()
  const existingTag = existingTags.value.find((item) => item.tagId?.toString()?.toLowerCase() === tagId)
  const existingValue = existingTags.value.find((item) => item.tagValue?.toString()?.toLowerCase() === tagValue)
  return existingTag === undefined && existingValue === undefined
}
const schema = object({
  'newTag': string()
      .trim()
      .required()
      .max(appConfig.maxSkillTagLength)
      .test(
          'isNotAnExistingTag',
          ({label}) => `${label} already exist`,
          async (value, testContext) => validateAgainstCurrentTags(value)
      )
      .label('Tag Name'),
})

const loadExistingTags = () => {
  loadingExistingTags.value = true
  return SkillsService.getTagsForProject(route.params.projectId)
    .then((res) => {
      existingTags.value = res;
      return res
    }).finally(() => {
        loadingExistingTags.value = false
      })
}
const initialData = {
  'newTag': '',
}
const constructTagInfo = (tagToSave) => {
  const valToSave = tagToSave.trim()
  const tagValue = valToSave;
  const tagId = InputSanitizer.removeSpecialChars(valToSave)?.toLowerCase();
  const skillIds = []
  const taggedInfo = { tagId: tagId, tagValue: tagValue, skillIds };
  return taggedInfo
}
const saveTag = (tagToSave) => {
  const taggedInfo = constructTagInfo(tagToSave.newTag)
  return SkillsService.addTagToSkills(route.params.projectId, taggedInfo.skillIds, taggedInfo.tagId, taggedInfo.tagValue)
    .then(() => {
      return taggedInfo
    });
}
const afterSave = (taggedInfo) => {
  emit('added-tag', taggedInfo)
}

</script>

<template>
  <SkillsInputFormDialog
    id="createTagDialog"
    header="Create New Skill Tag"
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
  >
    <SkillsTextInput
      label="New Tag"
      name="newTag" />
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>