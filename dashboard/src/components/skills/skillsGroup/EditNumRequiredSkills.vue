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
import { ref, onMounted, toRaw, computed } from 'vue'
import SkillsInputFormDialog from '@/components/utils/inputForm/SkillsInputFormDialog.vue'
import SkillsDropDown from '@/components/utils/inputForm/SkillsDropDown.vue'
import ReminderMessage from '@/components/utils/misc/ReminderMessage.vue'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import SkillsService from '@/components/skills/SkillsService.js'

const show = defineModel()
const props = defineProps({
  group: {
    type: Object,
    required: true
  }
})
const emit = defineEmits(['group-changed'])

const save = (values) => {
  const updatedGroup = {
    ...props.group,
    numSkillsRequired: values.numSkillsRequired.value,
  };
  return SkillsService.saveSkill(updatedGroup)
}
const afterSaved = (updatedGroup) => {
  emit('group-changed', updatedGroup);
}
const initialSkillData = { }

const options = ref([])
const selected = ref({})
const originalSelection = ref({})

const skillsState = useSubjectSkillsState()
const updateNumSkillsRequired = () => {
  const skills = skillsState.getGroupSkills(props.group.skillId)
  const numSkills = skills.length;
  const dropdownOptions = [];
  for (let i = 1; i < numSkills; i += 1) {
    dropdownOptions.push({ value: i, totalSkills: numSkills, outOfSkills: i });
  }
  dropdownOptions.push({ value: -1, isAll: true });
  options.value = dropdownOptions
  selected.value = toRaw(dropdownOptions.find((item) => item.value == props.group.numSkillsRequired))
  originalSelection.value = selected.value
}
onMounted(() => {
  updateNumSkillsRequired()
})



const saveDisabled = computed(() => {
  return selected.value?.value === props.group.numSkillsRequired;
})

</script>

<template>
  <SkillsInputFormDialog
    :id="`edtRequiredNumSkillsModal-${group.skillId}`"
    v-model="show"
    :save-data-function="save"
    header="Required Number of Skills"
    saveButtonLabel="Save"
    :initial-values="initialSkillData"
    :enable-return-focus="true"
    :enable-input-form-resiliency="false"
    :ok-button-disabled="saveDisabled"
    :data-cy="`editRequiredModal-${group.skillId}`"
    dialog-class="w-11/12 sm:w-10/12 lg:w-9/12 xl:w-7/12"
    @saved="afterSaved">

    <reminder-message id="editNumRequiredSkillsMsg">
      <p>
        Groups have an option to only require <Tag>N</Tag> skills out of the total available number of skills added to the group.
      </p>
      <p>
        For example, if a group has <Tag>5</Tag> skills, you can configure it to only require the completion of <Tag>3</Tag> skills.
        In this case, when any <Tag>3</Tag> skills under that group are completed then the group achievement is awarded!
      </p>
    </reminder-message>

    <SkillsDropDown
      label="Number of Skills Required"
      name="numSkillsRequired"
      data-cy="requiredSkillsNumSelect"
      v-model="selected"
      :isRequired="true"
      :options="options">
      <template #option="slotProps">
        <span v-if="slotProps.option.isAll" class="uppercase">All Skills</span>
        <span v-else><Tag>{{slotProps.option.outOfSkills}}</Tag> out of <Tag>{{ slotProps.option.totalSkills}}</Tag></span>
      </template>
      <template #value="slotProps">
        <span v-if="slotProps.value.isAll" class="uppercase">All Skills</span>
        <span v-else><Tag>{{slotProps.value.outOfSkills}}</Tag> out of <Tag>{{ slotProps.value.totalSkills}}</Tag></span>
      </template>
    </SkillsDropDown>


  </SkillsInputFormDialog>
</template>

<style scoped>

</style>