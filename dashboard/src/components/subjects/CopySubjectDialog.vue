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

const model = defineModel()
const route = useRoute()

const close = () => {
  model.value = false
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
const loadOtherProjects = () => {
  ProjectService.getProjects().then((projRes) => {
    otherProjects.value = projRes.filter((p) => p.projectId?.toLowerCase() !== thisProjectId?.toLowerCase() && UserRolesUtil.isProjectAdminRole(p.userRole))
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
    SubjectsService.getSubjectDetails(route.params.projectId, route.params.subjectId).then((currentSubject) => {
      SubjectsService.getSubjects(changedProj.projectId).then((subjRes) => {
        const hasSameSubjectId = subjRes.find((subj) => subj.subjectId?.toLowerCase() === currentSubject.subjectId?.toLowerCase())
        if (hasSameSubjectId) {
          validationErrors.value.push('Subject ID already exists in the selected project')
        }
        const hasSameName = subjRes.find((subj) => subj.name?.toLowerCase() === currentSubject.name?.toLowerCase())
        if (hasSameName) {
          validationErrors.value.push('Subject Name already exists in the selected project')
        }

        if (!hasSameSubjectId && !hasSameName) {
          SkillsService.getProjectSkills(route.params.projectId).then((thisProjectSkillIds) => {
            const thisProjSkillIds = thisProjectSkillIds.map((skill) => skill.skillId)
            const thisProjSkillNames = thisProjectSkillIds.map((skill) => skill.name)
            SkillsService.getProjectSkills(changedProj.projectId).then((otherProjSkills) => {
              const otherProjSkillIds = otherProjSkills.map((skill) => skill.skillId)
              const alreadyExistSkillIds = thisProjSkillIds.filter((skillId) => otherProjSkillIds.find((otherSkillId) => otherSkillId?.toLowerCase() === skillId?.toLowerCase()))
              if (alreadyExistSkillIds.length > 0) {
                validationErrors.value.push('Skill(s) already exist in the selected project with the same skill ID(s).')
              }

              const otherProjSkillNames = otherProjSkills.map((skill) => skill.name)
              const alreadyExistSkillNames = thisProjSkillNames.filter((skillName) => otherProjSkillNames.find((otherSkillName) => otherSkillName?.toLowerCase() === skillName?.toLowerCase()))
              if (alreadyExistSkillNames.length > 0) {
                validationErrors.value.push('Skill(s) already exist in the selected project with the same skill name(s).')
              }

            }).finally(() => {
              validatingOtherProj.value = false
            })
          })

        } else {
          validatingOtherProj.value = false
        }
      })
    })

  }
}
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
      :show-ok-button="!copied"
      :cancel-button-label="copied ? 'Close' : 'Cancel'"
      :cancel-button-severity="copied ? 'success' : 'secondary'"
      :cancel-button-icon="copied ? 'fas fa-check' : 'far fa-times-circle'"
      @on-ok="doCopy"
      @on-cancel="close"
      :loading="loadingOtherProjects"
      :submitting="copying"
      :enable-return-focus="true">
    <BlockUI v-if="!copied" :blocked="copying" class="py-4">
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
      <Message v-if="hasValidationErrors" :closable="false" severity="error">
        <div v-for="error in validationErrors" :key="error"><span v-html="error"></span></div>

      </Message>
      <Message v-if="canCopy" :closable="false" severity="success">Validation Passed! This subject is eligible to be copied to <b>{{ selectedProject.name }}</b> project</Message>
    </BlockUI>
    <Message v-if="copied" severity="success" :closable="false">Subject copied to <b>{{ selectedProject.name }}</b></Message>
  </SkillsDialog>
</template>

<style scoped>

</style>