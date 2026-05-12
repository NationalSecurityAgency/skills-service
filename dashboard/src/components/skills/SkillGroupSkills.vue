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
import { computed, onBeforeMount, provide, ref } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { useRoute } from 'vue-router'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import EditSkill from '@/components/skills/EditSkill.vue'
import { SkillsReporter } from '@skilltree/skills-client-js'
import EditSkillGroup from '@/components/skills/skillsGroup/EditSkillGroup.vue'
import ImportFromCatalogDialog from '@/components/skills/catalog/ImportFromCatalogDialog.vue'
import { useFinalizeInfoState } from '@/stores/UseFinalizeInfoState.js'
import ChildRowSkillGroupDisplay from '@/components/skills/skillsGroup/ChildRowSkillGroupDisplay.vue'
import { useSkillsState } from '@/stores/UseSkillsState.js'


const appConfig = useAppConfig()
const projConfig = useProjConfig()
const route = useRoute()
const skillsState = useSkillsState()
const subjectSkillsState = useSubjectSkillsState()
const subjectState = useSubjectsState()
const finalizeInfoState = useFinalizeInfoState()
const announcer = useSkillsAnnouncer()
const isLoading = computed(() => {
  return skillsState.loadingSkill || subjectSkillsState.getLoadingGroupSkills(skillsState.skill.skillId)
})

const addSkillDisabled = computed(() => {
  return (
    (subjectState.subject?.numSkills || 0) + (subjectState.subject?.numSkillsReused || 0) >=
    appConfig.maxSkillsPerSubject
  )
})

const addSkillsDisabledMsg = computed(() => {
  return `The maximum number of Skills allowed is ${appConfig.maxSkillsPerSubject}.`
})

const importDialog = ref({
  show: false,
  groupId: ''
})
const initiateImport = (groupId = '') => {
  importDialog.value.groupId = groupId
  importDialog.value.show = true
}
provide('initiateImport', initiateImport)
const editGroup = ref({
  skill: {},
  show: false,
  isEdit: false
})
const createOrUpdateGroup = (skill = {}, isEdit = false) => {
  editGroup.value = {
    skill,
    isEdit,
    show: true
  }
}
provide('createOrUpdateGroup', createOrUpdateGroup)

onBeforeMount(() => {
  if (skillsState.skill?.skillId !== route.params.groupId) {
    skillsState.loadSkill(route.params.projectId, route.params.subjectId, route.params.groupId)
  }
  if (subjectState.subject?.subjectId !== route.params.subjectId) {
    subjectState.loadSubjectDetailsState()
  }
})

const newSkillInfo = ref({
  skill: {},
  show: false,
  isEdit: false,
  isCopy: false,
  groupId: null,
  isGroupEnabled: true,
  version: 1
})
const createOrUpdateSkill = (
  skill = {},
  isEdit = false,
  isCopy = false,
  groupId = null,
  isGroupEnabled = true
) => {
  if (skill.isGroupType) {
    createOrUpdateGroup(skill, isEdit)
  } else {
    newSkillInfo.value = {
      skill,
      isEdit,
      show: true,
      isCopy,
      groupId,
      isGroupEnabled
    }
  }
}
provide('createOrUpdateSkill', createOrUpdateSkill)

const reportSkills = (origExistingSkill, createdSkill) => {
  if (createdSkill.pointIncrementInterval <= 0) {
    SkillsReporter.reportSkill('CreateSkillDisabledTimeWindow')
  }
  if (createdSkill.numMaxOccurrencesIncrementInterval > 1) {
    SkillsReporter.reportSkill('CreateSkillMaxOccurrencesWithinTimeWindow')
  }
  if (createdSkill.helpUrl) {
    SkillsReporter.reportSkill('CreateSkillHelpUrl')
  }
  if (createdSkill.groupId) {
    SkillsReporter.reportSkill('CreateSkillGroup')
  }
  if (
    (!origExistingSkill &&
      !createdSkill?.iconClass?.toLowerCase()?.includes('fa-graduation-cap')) ||
    (origExistingSkill &&
      createdSkill?.iconClass?.toLowerCase() !== origExistingSkill?.iconClass?.toLowerCase())
  ) {
    SkillsReporter.reportSkill('ConfigureSkillIcon')
  }
  if (
    createdSkill.quizId &&
    (!origExistingSkill?.quizId || origExistingSkill?.quizId !== createdSkill.quizId)
  ) {
    SkillsReporter.reportSkill('SkillQuizOrSurvey')
  }
}

const skillCreatedOrUpdated = (skill) => {
  let origExistingSkill = null
  const skills = subjectSkillsState.getGroupSkills(skill.groupId)
  const existingIndex = skills.findIndex((item) => item.skillId === skill.originalSkillId)
  const createdSkill = {
    ...skill,
    subjectId: route.params.subjectId
  }
  if (existingIndex >= 0) {
    const existingSkill = skills[existingIndex]
    origExistingSkill = { ...existingSkill }
    if (skill.isGroupType && skill.enabled !== existingSkill.enabled) {
      subjectSkillsState.loadGroupSkills(skill.projectId, skill.skillId)
      finalizeInfoState.loadInfo()
    }
    skills.splice(existingIndex, 1, createdSkill)
  } else {
    skills.push(createdSkill)
    SkillsReporter.reportSkill('CreateSkill')
    if (!skill.enabled) {
      SkillsReporter.reportSkill('CreateSkillInitiallyHidden')
    }
  }
  if (skill.groupId) {
    subjectSkillsState.setGroupSkills(skill.groupId, skills)
    const parentGroup = skillsState.skill
    const groupSkills = subjectSkillsState.getGroupSkills(skill.groupId)
    parentGroup.totalPoints = groupSkills
      .filter((item) => item.enabled === true)
      .map((item) => item.totalPoints)
      .reduce((accumulator, currentValue) => {
        return accumulator + currentValue
      }, 0)

    parentGroup.numSkillsInGroup = groupSkills.length
    skillsState.skill.totalPoints = parentGroup.totalPoints
    skillsState.skill.numSkillsInGroup = parentGroup.numSkillsInGroup
  }
  // attribute based skills should report on new or update operation
  reportSkills(origExistingSkill, createdSkill)

  subjectState.loadSubjectDetailsState()

  const msg =
    skill.type === 'SkillGroup'
      ? `Group ${skill.name} has been saved`
      : `Skill ${skill.name} has been saved`
  announcer.polite(msg)
  return createdSkill
}
</script>

<template>
  <div>
    <sub-page-header
      ref="subPageHeader"
      title="Skills Group"
      :is-loading="isLoading"
      aria-label="new skill">

      <div v-if="addSkillDisabled && !projConfig.isReadOnlyProj">
        <InlineMessage severity="warn"
                       class="mx-2"
                       :aria-label="addSkillsDisabledMsg"
                       data-cy="addSkillDisabledWarning"  :closable="false">
          {{ addSkillsDisabledMsg }}
        </InlineMessage>
      </div>
    </sub-page-header>

    <Card :pt="{ body: { class: 'p-0!' } }">
      <template #content>
        <skills-spinner
          v-if="skillsState.loadingSkill"
          :is-loading="true"
          extraClass="py-20 my-0 h-[23rem]" />
        <child-row-skill-group-display
          v-else
          :show-legend="false"
          :skill="skillsState.skill"
          class="ml-6" />
      </template>
    </Card>

    <edit-skill
      v-if="newSkillInfo.show"
      v-model="newSkillInfo.show"
      :skill="newSkillInfo.skill"
      :is-subject-enabled="subjectState.subject.enabled"
      :is-group-enabled="newSkillInfo.isGroupEnabled"
      :is-edit="newSkillInfo.isEdit"
      :is-copy="newSkillInfo.isCopy"
      :group-id="newSkillInfo.groupId"
      :project-user-community="projConfig.getProjectCommunityValue()"
      @skill-saved="skillCreatedOrUpdated" />

    <edit-skill-group
      v-if="editGroup.show"
      v-model="editGroup.show"
      :skill="editGroup.skill"
      :is-subject-enabled="subjectState.subject.enabled"
      :is-edit="editGroup.isEdit"
      @skill-saved="skillCreatedOrUpdated" />
    <import-from-catalog-dialog
      v-if="importDialog.show"
      v-model="importDialog.show"
      :group-id="importDialog.groupId" />
  </div>
</template>

<style scoped></style>
