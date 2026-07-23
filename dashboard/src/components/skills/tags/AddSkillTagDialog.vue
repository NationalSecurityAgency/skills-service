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
import {ref, computed, watch} from 'vue'
import { object, string } from 'yup'
import { useRoute } from 'vue-router'
import Tabs from 'primevue/tabs';
import TabList from 'primevue/tablist';
import Tab from 'primevue/tab';
import TabPanels from 'primevue/tabpanels';
import TabPanel from 'primevue/tabpanel';
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useFocusState } from '@/stores/UseFocusState.js'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import SkillsNameAndIdInput from "@/components/utils/inputForm/SkillsNameAndIdInput.vue";
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";

const model = defineModel()
const emit = defineEmits(['added-tag'])
const props = defineProps({
  skills: {
    type: Array,
    required: true
  },
  groupId: {
    type: String,
    required: false
  },
})
const appConfig = useAppConfig()
const route = useRoute()
const skillsState = useSubjectSkillsState()
const focusState = useFocusState()
const pluralize = usePluralize()
const numberFormat = useNumberFormat()

const schema = object({
  'tagValue': string()
      .trim()
      .noHtml()
      .max(appConfig.maxSkillTagLength)
      .label('Tag'),
  'tagId': string()
      .trim()
      .matches(/^[a-zA-Z0-9]*$/, ({label}) => `${label} may only contain alpha-numeric characters`)
      .max(appConfig.maxSkillTagLength)
      .label('Tag ID'),
  'existingTag': object()
    .test(
    'existingOrNewTagMustBePresent',
    () => 'Existing tag must be supplied',
    async (value, testContext) => {
      return value?.tagId || (testContext.parent.tagValue && testContext.parent.tagId)
    }
  )
})

const addSkillTagDialog = ref(null)
const existingTagTabId = 'existingTagTab'
const newTagTabId = 'newTagTab'
const currentTab = ref(existingTagTabId)
const isExistingTag = computed(() => currentTab.value === existingTagTabId)
watch(() => currentTab.value,
    () => {
      if (isExistingTag.value) {
        addSkillTagDialog.value.setFieldValue('tagId', '')
        addSkillTagDialog.value.setFieldValue('tagValue', '')
      } else {
        addSkillTagDialog.value.setFieldValue('existingTag', {})
      }
    })

const existingTags = ref([])
const loadExistingTags = () => {
  return SkillsService.getTagsForProject(route.params.projectId)
    .then((res) => {
      existingTags.value = res;
      return {}
    })
}
const hasExistingTags = computed(() => existingTags.value.length > 0)
const initialData = {
  'tagValue': '',
  'tagId': '',
}
const saveTags = (values) => {
  const skillIds = props.skills.map((skill) => skill.skillId);
  const tagId = isExistingTag.value ? values.existingTag.tagId?.toString()?.toLowerCase() : values.tagId?.toString()?.toLowerCase()
  const tagValue = isExistingTag.value ? values.existingTag.tagValue : values.tagValue
  return SkillsService.addTagToSkills(route.params.projectId, skillIds, tagId, tagValue)
      .then(() => {
        return {tagId, tagValue, skillIds}
      });
}
const afterSave = (taggedInfo) => {
  const skills = props.groupId ? skillsState.getGroupSkills(props.groupId) : skillsState.subjectSkills
  const toUpdate = skills.filter(sk => taggedInfo.skillIds.includes(sk.skillId))
  toUpdate.forEach((sk) => {
    if (sk.tags.findIndex((item) => item.tagId === taggedInfo.tagId) === -1) {
      sk.tags.push({tagId: taggedInfo.tagId, tagValue: taggedInfo.tagValue})
    }
  })
  SkillsReporter.reportSkill('AddOrModifyTags')
  emit('added-tag', taggedInfo)
  const focusOn = props.groupId ? `group-${props.groupId}_newSkillBtn` : 'newSkillBtn'
  focusState.setElementId(focusOn)
  focusState.focusOnLastElement()
}

</script>

<template>
  <SkillsInputFormDialog
    id="addSkillTagDialog"
    ref="addSkillTagDialog"
    header="Tag Selected Skills"
    v-model="model"
    :save-data-function="saveTags"
    @saved="afterSave"
    :async-load-data-function="loadExistingTags"
    :validation-schema="schema"
    :initial-values="initialData"
    :enable-return-focus="true"
    :enable-input-form-resiliency="false"
    data-cy="addSkillTagDialog"
  >
    <p class="text-xl pb-3" data-cy="instructionsMsg">To tag
      <Tag>{{ numberFormat.pretty(skills.length) }}</Tag>
      selected {{ pluralize.plural('skill', skills.length) }}, please select an existing tag or create a new one:
    </p>
    <Tabs v-model:value="currentTab" class="mx-2">
      <TabList >
        <Tab :value="existingTagTabId"><i class="fa-solid fa-list-check" aria-hidden="true"></i> Select Existing Tag</Tab>
        <Tab :value="newTagTabId"><i class="fa-solid fa-pen-to-square" aria-hidden="true"></i> Create New Tag</Tab>
      </TabList>
      <TabPanels>
        <TabPanel :value="existingTagTabId">
          <div class="p-5">
            <Message
                v-if="!hasExistingTags"
                data-cy="noTagsMessage"
                :closable="false">No tags have been created yet. Please create a new tag using the "Create New Tag" tab.</Message>
            <SkillsDropDown
                v-if="hasExistingTags"
                label="Existing Tag"
                :options="existingTags"
                optionLabel="tagValue"
                name="existingTag"/>
          </div>
        </TabPanel>
        <TabPanel :value="newTagTabId">
          <div class="p-5">
            <SkillsNameAndIdInput
                name-label="Tag"
                name-field-name="tagValue"
                id-label="Tag ID"
                id-field-name="tagId">
            </SkillsNameAndIdInput>
          </div>
        </TabPanel>
      </TabPanels>
    </Tabs>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>