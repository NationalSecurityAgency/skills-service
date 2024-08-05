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
import { computed, ref } from 'vue'
import { object } from 'yup'
import { useRoute } from 'vue-router'
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useFocusState } from '@/stores/UseFocusState.js'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsService from '@/components/skills/SkillsService.js'

const model = defineModel()
const emit = defineEmits(['removed-tag'])
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
const route = useRoute()
const skillsState = useSubjectSkillsState()
const focusState = useFocusState()
const schema = object({
  'existingTag': object().required()
})
const existingTags = ref([])
const loadExistingTags = () => {
  const skillIds = props.skills.map((skill) => skill.skillId);
  return SkillsService.getTagsForSkills(route.params.projectId, skillIds)
    .then((res) => {
      existingTags.value = res;
      return {}
    })
}
const hasExistingTags = computed(() => existingTags.value && existingTags.value.length > 0)
const initialData = {}
const deleteTags = (values) => {
  const tagId = values.existingTag.tagId
  const skillIds = props.skills.map((skill) => skill.skillId);
  return SkillsService.deleteTagForSkills(route.params.projectId, skillIds, tagId)
    .then(() => {
     return { tagId, skillIds }
    });
}
const afterDelete = (taggedInfo) => {
  const skills = props.groupId ? skillsState.getGroupSkills(props.groupId) : skillsState.subjectSkills
  const toUpdate = skills.filter(sk => taggedInfo.skillIds.includes(sk.skillId))
  toUpdate.forEach((sk) => {
    sk.tags = sk.tags.filter((tag) => tag.tagId !== taggedInfo.tagId)
  })
  SkillsReporter.reportSkill('AddOrModifyTags')
  emit('removed-tag', taggedInfo)
  const focusOn = props.groupId ? `group-${props.groupId}_newSkillBtn` : 'newSkillBtn'
  focusState.setElementId(focusOn)
  focusState.focusOnLastElement()
}
</script>

<template>
  <SkillsInputFormDialog
    id="contactProjectAdmins"
    header="Remove Tag From Selected Skills"
    save-button-label="Remove"
    save-button-icon="fas fa-trash"
    v-model="model"
    :save-data-function="deleteTags"
    @saved="afterDelete"
    :async-load-data-function="loadExistingTags"
    :validation-schema="schema"
    :initial-values="initialData"
    :enable-return-focus="true"
    data-cy="addSkillTagDialog"
  >
    <SkillsDropDown
      v-if="hasExistingTags"
      label="Select Tag to Remove"
      :options="existingTags"
      optionLabel="tagValue"
      name="existingTag" />
    <Message v-else :closable="false" severity="warn">
      The selected skills do not have any tags.
    </Message>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>