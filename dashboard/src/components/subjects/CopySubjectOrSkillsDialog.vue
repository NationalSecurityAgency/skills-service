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
import {computed, onMounted, ref} from "vue";
import SkillsDialog from "@/components/utils/inputForm/SkillsDialog.vue";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import ProjectService from "@/components/projects/ProjectService.js";
import {useRoute} from "vue-router";
import Dropdown from "primevue/dropdown";
import UserRolesUtil from "@/components/utils/UserRolesUtil.js";
import SubjectsService from "@/components/subjects/SubjectsService.js";
import {useFocusState} from "@/stores/UseFocusState.js";

const props = defineProps({
  copyType: {
    type: String,
    validator: (value) => ['EntireSubject', 'SelectSkills'].includes(value)
  },
  selectedSkills: {
    type: Array,
    default: [],
  },
})
const emits = defineEmits(['after-copied'])
const model = defineModel()
const route = useRoute()
const focusState = useFocusState()

const isSubjectCopy = computed(() => props.copyType === 'EntireSubject')
const isSelectedSkillsCopy = computed(() => props.copyType === 'SelectSkills')
const modalTitle = computed(() => isSubjectCopy.value ? 'Copy Subject To Another Project' : 'Copy Selected Skills To Another Project')

const close = () => {
  model.value = false
  focusState.focusOnLastElement()
}

const areAllValuesSelected = computed(() => isSubjectCopy.value ? selectedProject.value != null : (selectedProject.value && selectedSubjectOrGroup.value))
const canCopy = computed(() => areAllValuesSelected.value && !loadingOtherProjects.value && !validatingOtherProj.value && validationErrors.value.length === 0)
const copying = ref(false)
const copied = ref(false)
const doCopy = () => {
  copying.value = true
  return SubjectsService.copySubjectOrSkillsToAnotherProject(route.params.projectId, selectedProject.value.projectId, endpointsProps.value)
      .then(() => {
        copying.value = false
        copied.value = true
        if (isSelectedSkillsCopy.value) {
          const groupId = props.selectedSkills[0].groupId
          const focusOn = groupId ? `group-${groupId}_newSkillBtn` : 'newSkillBtn'
          focusState.setElementId(focusOn)
        }
        emits('after-copied')
      })
}

const loadingOtherProjects = ref(true)
const thisProjectId = route.params.projectId
const otherProjects = ref([])
const isAllowedRole = (userRole) => UserRolesUtil.isProjectAdminRole(userRole) || UserRolesUtil.isSuperRole(userRole)
const loadOtherProjects = () => {
  ProjectService.getProjects().then((projRes) => {
    otherProjects.value = projRes.filter((p) => p.projectId?.toLowerCase() !== thisProjectId?.toLowerCase() && isAllowedRole(p.userRole))
  }).finally(() => {
    loadingOtherProjects.value = false
  })
}

onMounted(() => {
  loadOtherProjects()
})

const selectedProject = ref(null)
const wasProjectSelected = computed(() => selectedProject.value != null)
const validatingOtherProj = ref(false)
const validationErrors = ref([])
const hasValidationErrors = computed(() => validationErrors.value.length > 0)

const endpointsProps = computed(() => {
  const isDestAGroup = () =>  isSelectedSkillsCopy.value && selectedSubjectOrGroup.value?.type === 'SkillsGroup'
  return {
    copyType: props.copyType,
    fromSubjectId: isSubjectCopy.value ? route.params.subjectId : null,
    skillIds: isSelectedSkillsCopy.value ? props.selectedSkills.map((sk) => sk.skillId) : null,
    toSubjectId: isSelectedSkillsCopy.value && !isDestAGroup() ? selectedSubjectOrGroup.value?.skillId : null,
    toGroupId: isSelectedSkillsCopy.value && isDestAGroup() ? selectedSubjectOrGroup.value?.skillId : null,
  }
})

const onProjectChanged = (changedProj) => {
  if (changedProj != null) {
    validationErrors.value = []
    if (isSubjectCopy.value) {
      validatingOtherProj.value = true
      return SubjectsService.validateCopyItemsToAnotherProject(route.params.projectId, selectedProject.value.projectId, endpointsProps.value)
          .then((res) => {
            if (!res.isAllowed) {
              validationErrors.value.push(...res.validationErrors)
            }
          }).finally(() => {
            validatingOtherProj.value = false
          })
    }

    if (isSelectedSkillsCopy.value) {
      loadingOtherSubjectsAndGroups.value = true
      otherSubjectsAndGroups.value = []
      selectedSubjectOrGroup.value = null
      showSubjectAndGroupSelector.value = false
      return SubjectsService.getSubjectsAndSkillGroups(selectedProject.value.projectId)
          .then((subjectsAndGroups) => {
            otherSubjectsAndGroups.value = subjectsAndGroups;
          }).finally(() => {
            showSubjectAndGroupSelector.value = true
            loadingOtherSubjectsAndGroups.value = false
          })
    }
  }
}

const showSubjectAndGroupSelector = ref(false)
const otherSubjectsAndGroups = ref([])
const loadingOtherSubjectsAndGroups = ref(false)
const selectedSubjectOrGroup = ref(null)
const onSubjectOrGroupChanged = ((changedSubjOrGroup) => {
  if (changedSubjOrGroup) {
    validatingOtherProj.value = true
    validationErrors.value = []
    return SubjectsService.validateCopyItemsToAnotherProject(route.params.projectId, selectedProject.value.projectId, endpointsProps.value)
        .then((res) => {
          if (!res.isAllowed) {
            validationErrors.value.push(...res.validationErrors)
          }
        }).finally(() => {
          validatingOtherProj.value = false
        })
  }
})

const hasProjects = computed(() => otherProjects.value?.length > 0)
const showOkButton = computed(() => !copied.value && (hasProjects.value || loadingOtherProjects.value))
</script>

<template>
  <SkillsDialog
      :maximizable="false"
      v-model="model"
      :header="modalTitle"
      cancel-button-severity="secondary"
      ok-button-severity="danger"
      ok-button-icon="fas fa-copy"
      ok-button-label="Copy"
      :ok-button-disabled="!canCopy"
      :show-ok-button="showOkButton"
      :cancel-button-label="!showOkButton ? 'Close' : 'Cancel'"
      :cancel-button-severity="!showOkButton ? 'success' : 'secondary'"
      :cancel-button-icon="!showOkButton ? 'fas fa-check' : 'far fa-times-circle'"
      @on-ok="doCopy"
      @on-cancel="close"
      :loading="loadingOtherProjects"
      :submitting="copying">
    <Message v-if="!hasProjects" severity="warn" :closable="false" data-cy="noOtherProjectsMsg">You are not currently an administrator on any other projects.</Message>
    <BlockUI v-if="hasProjects && !copied" :blocked="copying" class="py-6">
      <div class="flex flex-col gap-2">
        <label for="selectAProjectDropdown">Destination Project:</label>
          <Select id="selectAProjectDropdown"
                    :options="otherProjects"
                    placeholder="Search for a project..."
                    v-model="selectedProject"
                    @update:model-value="onProjectChanged"
                    :disabled="validatingOtherProj"
                    label="name"
                    class="w-full"
                    data-cy="selectAProjectDropdown"
                    filter
                    :filterFields="['name']">
            <template #value="slotProps" v-if="selectedProject">
              <div>{{slotProps.value?.name}}</div>
            </template>
            <template #option="slotProps">
              <div>
                <div class="h6 project-name" data-cy="projectSelector-projectName">{{ slotProps.option.name }}</div>
                <div class="text-secondary project-id">ID: {{ slotProps.option.projectId }}</div>
              </div>
            </template>
          </Select>

      </div>
      <skills-spinner  :is-loading="loadingOtherSubjectsAndGroups" class="mt-6"/>
      <div v-if="isSelectedSkillsCopy && wasProjectSelected && !loadingOtherSubjectsAndGroups" class="flex flex-col gap-2 mt-8">
        <label for="selectASubjectOrGroupDropdown">Destination Subject or Skills Group:</label>
        <Select id="selectASubjectOrGroupDropdown"
                  :options="otherSubjectsAndGroups"
                  placeholder="Search for a subject or group"
                  v-model="selectedSubjectOrGroup"
                  @update:model-value="onSubjectOrGroupChanged"
                  :disabled="!showSubjectAndGroupSelector || validatingOtherProj"
                  label="name"
                  class="w-full"
                  data-cy="selectASubjectOrGroupDropdown"
                  filter
                  :filterFields="['name']">
          <template #value="slotProps" v-if="selectedSubjectOrGroup">
            <div>{{ slotProps.value?.name }}</div>
          </template>
          <template #option="slotProps">
            <div>
              <div class="h6 project-name" data-cy="subjOrGroupSelector-name">{{ slotProps.option.name }}</div>
              <div class="text-secondary project-id">ID: {{ slotProps.option.skillId }}</div>
            </div>
          </template>
        </Select>
      </div>
      <div v-if="validatingOtherProj">
        <skills-spinner  :is-loading="validatingOtherProj" class="mt-6"/>
        <div class="text-center text-secondary" role="alert">Validating if copy is possible...</div>
      </div>
      <Message v-if="hasValidationErrors" :closable="false" severity="error" data-cy="validationFailedMsg">
        <div v-if="isSubjectCopy">Subject cannot be copied:</div>
        <div v-if="isSelectedSkillsCopy">Skills cannot be copied:</div>
        <ul>
          <li v-for="error in validationErrors" :key="error"><span v-html="error"></span></li>
        </ul>

      </Message>
      <Message v-if="canCopy && isSubjectCopy" :closable="false" severity="success" data-cy="validationPassedMsg">
        Validation Passed! This subject is eligible to be copied to <b>{{ selectedProject.name }}</b> project
      </Message>
      <Message v-if="canCopy && isSelectedSkillsCopy" :closable="false" severity="success" data-cy="validationPassedMsg">
        Validation Passed! <Tag>{{ selectedSkills.length}}</Tag> skill(s) are eligible to be copied to <b>{{ selectedProject.name }}</b> project
      </Message>
    </BlockUI>
    <Message v-if="copied" severity="success" :closable="false" data-cy="copySuccessMsg">
      <div v-if="isSubjectCopy">Subject was copied to <b>{{ selectedProject.name }}</b></div>
      <div v-if="isSelectedSkillsCopy">Selected skill(s) were copied to <b>{{ selectedProject.name }}</b></div>
    </Message>
  </SkillsDialog>
</template>

<style scoped>

</style>