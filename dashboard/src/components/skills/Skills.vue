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
import { computed, ref, onMounted, provide } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import { useRoute } from 'vue-router'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue'
import SkillsTable from '@/components/skills/SkillsTable.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'
import EditSkill from '@/components/skills/EditSkill.vue'
import { SkillsReporter } from '@skilltree/skills-client-js'
import EditSkillGroup from '@/components/skills/skillsGroup/EditSkillGroup.vue'
import ImportFromCatalogDialog from '@/components/skills/catalog/ImportFromCatalogDialog.vue'
import { useFinalizeInfoState } from '@/stores/UseFinalizeInfoState.js'

const appConfig = useAppConfig()
const projConfig = useProjConfig()
const route = useRoute()
const skillsState = useSubjectSkillsState()
const subjectState = useSubjectsState()
const finalizeInfoState = useFinalizeInfoState()
const announcer = useSkillsAnnouncer()
const isLoading = computed(() => {
  // return this.loadingSubjectSkills || this.isLoadingProjConfig;
  return false
})

const addSkillDisabled = computed(() => {
  return subjectState.subject.numSkills >= appConfig.maxSkillsPerSubject;
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
  isEdit: false,
})
const createOrUpdateGroup = (skill = {}, isEdit = false) => {
  editGroup.value = {
    skill,
    isEdit,
    show: true,
  }
}
provide('createOrUpdateGroup', createOrUpdateGroup)

onMounted(() => {
  skillsState.loadSubjectSkills(route.params.projectId, route.params.subjectId)
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
const createOrUpdateSkill = (skill = {}, isEdit = false, isCopy = false, groupId = null, isGroupEnabled = true) => {
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

const reportSkills = (createdSkill) => {
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
}

const skillCreatedOrUpdated = (skill) => {
  const skills = skill.groupId ? skillsState.getGroupSkills(skill.groupId) : skillsState.subjectSkills
  const existingIndex = skills.findIndex((item) => item.skillId === skill.originalSkillId)
  const createdSkill = ({
    ...skill,
    subjectId: route.params.subjectId
  })
  if (existingIndex >= 0) {
    const existingSkill = skills[existingIndex]
    if (skill.isGroupType && skill.enabled !== existingSkill.enabled) {
      skillsState.loadGroupSkills(skill.projectId, skill.skillId)
      finalizeInfoState.loadInfo()
    }
    skills.splice(existingIndex, 1, createdSkill)
  } else {
    skills.push(createdSkill)
    SkillsReporter.reportSkill('CreateSkill')
  }
  if (skill.groupId) {
    skillsState.setGroupSkills(skill.groupId, skills)
    const parentGroup = skillsState.subjectSkills.find((item) => item.skillId === skill.groupId)
    const groupSkills = skillsState.getGroupSkills(skill.groupId)
    parentGroup.totalPoints = groupSkills
      .filter((item) => item.enabled === true)
      .map((item) => item.totalPoints)
      .reduce((accumulator, currentValue) => {
        return accumulator + currentValue
      }, 0)

    parentGroup.numSkillsInGroup = groupSkills.length
  }
  // attribute based skills should report on new or update operation
  reportSkills(createdSkill)

  subjectState.loadSubjectDetailsState()

  const msg = skill.type === 'SkillGroup' ? `Group ${skill.name} has been saved` : `Skill ${skill.name} has been saved`
  announcer.polite(msg)
  return createdSkill
}

</script>

<template>
  <div>
    <!--    :disabled="addSkillDisabled"-->
    <!--    :disabled-msg="addSkillsDisabledMsg"-->
    <sub-page-header
      ref="subPageHeader"
      title="Skills"
      :is-loading="isLoading"
      aria-label="new skill">
      <div v-if="!projConfig.isReadOnlyProj">

        <SkillsButton
          id="importFromCatalogBtn"
          ref="importFromCatalogBtn"
          label="Import"
          outlined
          class="text-primary bg-primary-contrast"
          icon="fas fa-book"
          @click="initiateImport()"
          size="small"
          :track-for-focus="true"
          aria-label="import from catalog"
          data-cy="importFromCatalogBtn" />
        <SkillsButton
          id="newGroupBtn"
          ref="newGroupButton"
          label="Group"
          icon="fas fa-plus-circle"
          @click="createOrUpdateGroup"
          size="small"
          outlined
          class="text-primary bg-primary-contrast ml-1"
          aria-label="new skills group"
          data-cy="newGroupButton"
          :track-for-focus="true"
          :aria-disabled="addSkillDisabled"
          :disabled="addSkillDisabled" />
        <SkillsButton
          id="newSkillBtn"
          ref="newSkillButton"
          label="Skill"
          icon="fas fa-plus-circle"
          @click="createOrUpdateSkill"
          variant="outline-primary"
          size="small"
          aria-label="new skill"
          data-cy="newSkillButton"
          outlined
          class="text-primary bg-primary-contrast ml-1"
          :track-for-focus="true"
          :aria-disabled="addSkillDisabled"
          :disabled="addSkillDisabled" />
        <div v-if="addSkillDisabled">
          <InlineMessage severity="warn"
                         class="mx-2"
                         :aria-label="addSkillsDisabledMsg"
                         data-cy="addSkillDisabledWarning"  :closable="false">
            {{ addSkillsDisabledMsg }}
          </InlineMessage>
        </div>
      </div>
    </sub-page-header>


    <Card :pt="{ body: { class: 'p-0!' } }">
      <template #content>
        <skills-spinner
          v-if="skillsState.loadingSubjectSkills && !skillsState.hasSkills "
          :is-loading="skillsState.loadingSubjectSkills"
          extraClass="py-20 my-0 h-[23rem]" />
        <skills-table v-if="skillsState.hasSkills" />
        <no-content2
          v-if="!skillsState.loadingSubjectSkills && !skillsState.hasSkills"
          title="No Skills Yet"
          class="py-20"
          message="Projects are composed of Subjects which are made of Skills and a single skill defines a training unit within the gamification framework." />
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
      @skill-saved="skillCreatedOrUpdated"
    />

    <edit-skill-group
      v-if="editGroup.show"
      v-model="editGroup.show"
      :skill="editGroup.skill"
      :is-subject-enabled="subjectState.subject.enabled"
      :is-edit="editGroup.isEdit"
      @skill-saved="skillCreatedOrUpdated"
      />
    <import-from-catalog-dialog
      v-if="importDialog.show"
      v-model="importDialog.show"
      :group-id="importDialog.groupId"/>
  </div>
</template>

<style scoped></style>
