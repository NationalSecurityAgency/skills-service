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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import PageHeader from '@/components/utils/pages/PageHeader.vue'
import Navigation from '@/components/utils/Navigation.vue'
import ImportFinalizeAlert from '@/components/skills/catalog/ImportFinalizeAlert.vue'
import { useSubjectSkillsState } from '@/stores/UseSubjectSkillsState.js'
import { useFinalizeInfoState } from '@/stores/UseFinalizeInfoState.js'
import EditSkillGroup from '@/components/skills/skillsGroup/EditSkillGroup.vue'
import { useSkillsState } from '@/stores/UseSkillsState.js'

const route = useRoute()
const router = useRouter()
const announcer = useSkillsAnnouncer()
const projConfig = useProjConfig()
const subjectState = useSubjectsState()
const subjectSkillsState = useSubjectSkillsState()
const skillsState = useSkillsState()
const finalizeInfoState = useFinalizeInfoState()

const showEditGroup = ref(false)

const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj)

onMounted(() => {
  loadData()
})

const loadData = () => {
  if (skillsState.skill?.skillId !== route.params.groupId) {
    skillsState.loadSkill(route.params.projectId, route.params.subjectId, route.params.groupId)
  }
  if (subjectState.subject?.subjectId !== route.params.subjectId) {
    subjectState.loadSubjectDetailsState()
  }
}

const isLoadingData = computed(() => {
  return skillsState.loadingSkill
})

const navItems = computed(() => {
  const items = [
    {
      name: 'Skills Group',
      iconClass: 'fa-graduation-cap skills-color-skills',
      page: 'GroupSkills'
    }
  ]
  items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'GroupUsers' })
  items.push({
    name: 'Metrics',
    iconClass: 'fa-chart-bar skills-color-metrics',
    page: 'GroupMetrics'
  })
  return items
})

const headerOptions = computed(() => {
  return {
    icon: 'fas fa-layer-group skills-color-groups',
    title: `GROUP: ${skillsState.skill?.name}`,
    subTitle: `ID: ${skillsState.skill?.skillId || ''}`,
    stats: [
      {
        label: 'Skills',
        count: skillsState.skill?.numSkillsInGroup,
        icon: 'fas fa-graduation-cap skills-color-skills'
      },
      {
        label: 'Points',
        count: skillsState.skill?.totalPoints,
        icon: 'far fa-arrow-alt-circle-up skills-color-points',
        secondaryStats: [
          {
            label: 'reused',
            count: skillsState.skill?.totalPointsReused,
            badgeVariant: 'info'
          }
        ]
      }
    ]
  }
})

const displayEditGroup = () => {
  showEditGroup.value = true
}

const groupEdited = (updatedGroup) => {
  const origId = skillsState.skill.skillId
  const enabledStateChanged = updatedGroup.enabled !== skillsState.skill.enabled
  skillsState.setSkill({ ...updatedGroup, subjectId: route.params.subjectId })

  if (origId !== skillsState.skill.skillId) {
    router.replace({
      name: route.name,
      params: { ...route.params, skillId: skillsState.skill.skillId },
      query: { preventReload: true }
    })
  }
  if (enabledStateChanged) {
    subjectSkillsState.loadGroupSkills(updatedGroup.projectId, updatedGroup.skillId)
    finalizeInfoState.loadInfo()
  }
  announcer.polite(`Group ${updatedGroup.name} has been edited`)
}
</script>

<template>
  <div>
    <page-header :loading="isLoadingData" :options="headerOptions">
      <template #subSubTitle v-if="!isLoadingData && !isReadOnlyProj">
        <SkillsButton
          id="editGroupBtn"
          v-if="!isReadOnlyProj"
          @click="displayEditGroup"
          ref="editGroupButton"
          label="Edit"
          icon="fas fa-edit"
          outlined
          class="btn btn-outline-primary mr-1"
          size="small"
          :track-for-focus="true"
          severity="info"
          data-cy="btn_edit-group"
          :aria-label="`edit Group ${skillsState.skill.name}`" />
      </template>
      <template #right-of-header v-if="!isLoadingData && !skillsState.skill.enabled">
        <Tag
          v-if="!skillsState.skill.enabled"
          severity="secondary"
          class="ml-2"
          data-cy="disabledGroupBadge"
          ><i class="fas fa-eye-slash mr-1" aria-hidden="true"></i> DISABLED</Tag
        >
      </template>
      <template #footer> </template>
    </page-header>

    <import-finalize-alert />

    <navigation v-if="!isLoadingData" :nav-items="navItems"> </navigation>

    <edit-skill-group
      v-if="showEditGroup"
      v-model="showEditGroup"
      :skill="skillsState.skill"
      :is-subject-enabled="subjectState.subject.enabled"
      :is-edit="true"
      @skill-saved="groupEdited" />
  </div>
</template>

<style scoped></style>
