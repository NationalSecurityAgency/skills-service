<script setup>
import { ref } from 'vue'
import { object, string } from 'yup'
import { useRoute } from 'vue-router'
import { SkillsReporter } from '@skilltree/skills-client-js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useFocusState } from '@/stores/UseFocusState.js'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsService from '@/components/skills/SkillsService.js'
import InputSanitizer from '@/components/utils/InputSanitizer.js'

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
const schema = object({
  'newTag': string()
    .trim()
    .max(appConfig.maxSkillTagLength)
    .label('Tag Name'),
  'existingTag': object()
    .test(
    'existingOrNewTagMustBePresent',
    () => 'New or existing tag must be supplied',
    async (value, testContext) => {
      return value || testContext.parent.newTag
    }
  )
})
const existingTags = ref([])
const loadExistingTags = () => {
  return SkillsService.getTagsForProject(route.params.projectId)
    .then((res) => {
      existingTags.value = res;
      return {}
    })
}
const initialData = {
  'newTag': '',
}
const saveTags = (values) => {
  const valToSave = values.newTag?.trim() || values.existingTag.tagValue.trim()
  const tagValue = valToSave;
  const tagId = InputSanitizer.removeSpecialChars(valToSave)?.toLowerCase();

  const skillIds = props.skills.map((skill) => skill.skillId);
  const taggedInfo = { tagId: tagId, tagValue: tagValue, skillIds };
  return SkillsService.addTagToSkills(route.params.projectId, skillIds, tagId, tagValue)
    .then(() => {
      return taggedInfo
    });
}
const afterSave = (taggedInfo) => {
  const skills = props.groupId ? skillsState.getGroupSkills(props.groupId) : skillsState.subjectSkills
  const toUpdate = skills.filter(sk => taggedInfo.skillIds.includes(sk.skillId))
  toUpdate.forEach((sk) => {
    sk.tags.push({ tagId: taggedInfo.tagId, tagValue: taggedInfo.tagValue })
  })
  SkillsReporter.reportSkill('AddOrModifyTags')
  emit('added-tag', taggedInfo)
  const focusOn = props.groupId ? `group-${props.groupId}_newSkillBtn` : 'newSkillBtn'
  focusState.setElementId(focusOn)
  focusState.focusOnLastElement()
}
const disableExistingTagsSelector = ref(false)

const onNewTagChange = (newValue) => {
 disableExistingTagsSelector.value = (newValue && newValue.trim().length > 0) || false
}
</script>

<template>
  <SkillsInputFormDialog
    id="contactProjectAdmins"
    header="Tag Selected Skills"
    v-model="model"
    :save-data-function="saveTags"
    @saved="afterSave"
    :async-load-data-function="loadExistingTags"
    :validation-schema="schema"
    :initial-values="initialData"
    :enable-return-focus="true"
    data-cy="addSkillTagDialog"
  >
    <SkillsDropDown
      label="Select Existing Tag"
      :options="existingTags"
      optionLabel="tagValue"
      name="existingTag"
      :disabled="disableExistingTagsSelector"/>
    <Divider align="left" type="dotted">
      <b>OR</b>
    </Divider>
    <SkillsTextInput
      label="Create New Tag"
      name="newTag"
      @input="onNewTagChange"/>
  </SkillsInputFormDialog>
</template>

<style scoped>

</style>