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
import SkillsService from "@/components/skills/SkillsService.js";
import {useFocusState} from "@/stores/UseFocusState.js";

const model = defineModel()
const route = useRoute()
const focusState = useFocusState()

const close = () => {
  model.value = false
  focusState.focusOnLastElement()
}

const canCopy = computed(() => selectedProject.value != null && !loadingOtherProjects.value && !validatingOtherProj.value && validationErrors.value.length === 0)
const copying = ref(false)
const copied = ref(false)
const doCopy = () => {
  copying.value = true
  return SubjectsService.copySubjectToAnotherProject(route.params.projectId, route.params.subjectId, selectedProject.value.projectId)
      .then(() => {
        copying.value = false
        copied.value = true
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
const validatingOtherProj = ref(false)
const validationErrors = ref([])
const hasValidationErrors = computed(() => validationErrors.value.length > 0)
const onProjectChanged = (changedProj) => {
  validationErrors.value = []
  if (changedProj != null) {
    validatingOtherProj.value = true

    return SubjectsService.validateCopySubjectToAnotherProject(route.params.projectId, route.params.subjectId, selectedProject.value.projectId)
        .then((res) => {
          if (!res.isAllowed) {
            validationErrors.value.push(...res.validationErrors)
          }
        }).finally(() => {
          validatingOtherProj.value = false
        })
  }
}
const hasProjects = computed(() => otherProjects.value?.length > 0)
const showOkButton = computed(() => !copied.value && (hasProjects.value || loadingOtherProjects.value))
</script>

<template>
  <SkillsDialog
      :maximizable="false"
      v-model="model"
      header="Copy Subject To Another Project"
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
    <BlockUI v-if="hasProjects && !copied" :blocked="copying" class="py-4">
      <div>
        <FloatLabel>
          <Dropdown id="selectAProjectDropdown"
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
          </Dropdown>
          <label for="selectAProjectDropdown">Project To Copy Subject To</label>
        </FloatLabel>
      </div>
      <div v-if="validatingOtherProj">
        <skills-spinner  :is-loading="validatingOtherProj" class="mt-4"/>
        <div class="text-center text-secondary" role="alert">Validating if copy is possible...</div>
      </div>
      <Message v-if="hasValidationErrors" :closable="false" severity="error" data-cy="validationFailedMsg">
        <div>Subject cannot be copied:</div>
        <ul>
          <li v-for="error in validationErrors" :key="error"><span v-html="error"></span></li>
        </ul>

      </Message>
      <Message v-if="canCopy" :closable="false" severity="success" data-cy="validationPassedMsg">Validation Passed! This subject is eligible to be copied to <b>{{ selectedProject.name }}</b> project</Message>
    </BlockUI>
    <Message v-if="copied" severity="success" :closable="false">Subject copied to <b>{{ selectedProject.name }}</b></Message>
  </SkillsDialog>
</template>

<style scoped>

</style>