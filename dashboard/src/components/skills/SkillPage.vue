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
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useSubjectsState } from '@/stores/UseSubjectsState.js'
import Badge from 'primevue/badge'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import PageHeader from '@/components/utils/pages/PageHeader.vue'
import Navigation from '@/components/utils/Navigation.vue'
import SkillReuseIdUtil from '@/components/utils/SkillReuseIdUtil'
import { useSkillsState } from '@/stores/UseSkillsState.js'
import { useProjConfig } from '@/stores/UseProjConfig.js'
import ShowMore from '@/components/skills/selfReport/ShowMore.vue'
import EditSkill from '@/components/skills/EditSkill.vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import SkillNavigation from "@/skills-display/components/utilities/SkillNavigation.vue";

const route = useRoute()
const router = useRouter()
const announcer = useSkillsAnnouncer()
const subjectState = useSubjectsState()
const projConfig = useProjConfig()
const appConfig = useAppConfig()

const headerOptions = ref({})
const showEdit = ref(false)

// let skill = ref(store.getters["skills/skill"]);

const skillsState = useSkillsState()

const isReadOnlyProj = computed(() => projConfig.isReadOnlyProj);

onMounted(() => {
  if (!projConfig.loadingProjConfig) {
    loadData();
  }
})

// Vue caches components and when re-directed to the same component the path will be pushed
// to the url but the component will NOT be re-mounted therefore we must listen for events and re-load
// the data; alternatively could update
//    <router-view :key="$route.fullPath"/>
// but components will never get cached - caching maybe important for components that want to update
// the url so the state can be re-build later (example include browsing a map or dependency graph in our case)
watch(
  () => route.params.skillId,
  () => {
    if (!route.query.preventReload) {
      loadData()
    }
  }
)

watch(
    () => projConfig.loadingProjConfig,
    () => {
      if (!projConfig.loadingProjConfig) {
        loadData();
      }
    }
)

const isLoading = computed(() => {
  return subjectState.isLoadingSubject || skillsState.loadingSkill || projConfig.loadingProjConfig
})

const navItems = ref([])
const buildNavItems = () => {
  const items = []
  items.push({ name: 'Overview', iconClass: 'fa-info-circle skills-color-overview', page: 'SkillOverview' })
  items.push({ name: 'Slides', iconClass: 'fa-solid fa-file-pdf', page: 'ConfigureSlides' })
  items.push({ name: 'Audio/Video', iconClass: 'fa-play-circle skills-color-video', page: 'ConfigureVideo' })
  items.push({
    name: 'Expiration',
    iconClass: 'fa-hourglass-end skills-color-expiration',
    page: 'ConfigureExpiration'
  })
  items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'SkillUsers' })
  if (!isImported?.value && !isReadOnlyProj.value && !isDisabled.value) {
    items.push({
      name: 'Add Event',
      iconClass: 'fa-user-plus skills-color-events',
      page: 'AddSkillEvent',
    })
  }
  items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'SkillMetrics' })
  return items
}

const isImported = computed(() => {
  return skillsState.skill && skillsState.skill.copiedFromProjectId && skillsState.skill.copiedFromProjectId.length > 0
})
const isDisabled = computed(() => {
  return skillsState.skill && !skillsState.skill.enabled
})

// Methods
const displayEdit = () => {
  // should only enable edit button if dirty, isn't currently
  showEdit.value = true
}

const loadData = () => {
  skillsState.loadSkill(route.params.projectId, route.params.subjectId, route.params.skillId)
    .then(() => {
      headerOptions.value = buildHeaderOptions()
      if (subjectState.subject && subjectState.subject.subjectId === route.params.subjectId) {
        navItems.value = buildNavItems()
      } else {
        subjectState.loadSubjectDetailsState().then(() => {
          navItems.value = buildNavItems()
        })
      }
    })
}

const skillEdited = (editedSkill) => {
  const origId = skillsState.skill.skillId
  skillsState.setSkill({ ...editedSkill, subjectId: route.params.subjectId })

  if (origId !== skillsState.skill.skillId) {
    router.replace({ name: route.name, params: { ...route.params, skillId: skillsState.skill.skillId }, query: { preventReload: true} })
  }
  headerOptions.value = buildHeaderOptions()
  announcer.polite(`Skill ${editedSkill.name} has been edited`)
}

const buildHeaderOptions = () => {
  const skillId = skillsState.skill?.skillId ? SkillReuseIdUtil.removeTag(skillsState.skill.skillId) : ''
  const iconClass = skillsState.skill?.iconClass ? skillsState.skill.iconClass : 'fas fa-graduation-cap'

  return {
    icon: `${iconClass} skills-color-skills`,
    title: `SKILL: ${skillsState.skill?.name}`,
    subTitle: `ID: ${skillId} | GROUP ID: ${skillsState.skill?.groupId}`,
    stats: [{
      label: 'Points',
      count: skillsState.skill?.totalPoints,
      icon: 'far fa-arrow-alt-circle-up skills-color-points'
    }]
  }
}

const skillId = computed(() => {
  return skillsState.skill ? `ID: ${SkillReuseIdUtil.removeTag(skillsState.skill.skillId)}` : 'Loading...'
})

const prevButtonClicked = () => {
  const params = { skillId: skillsState.skill.prevSkillId, projectId: route.params.projectId }
  router.push({ name: route.name, params: params })
}

const nextButtonClicked = () => {
  const params = { skillId: skillsState.skill.nextSkillId, projectId: route.params.projectId }
  router.push({ name: route.name, params: params })
}

</script>

<template>
  <div class="mt-2">
    <Card class="p-2" :pt="{ body: { class: 'p-0!' } }" v-if="skillsState.skill && (skillsState.skill.prevSkillId || skillsState.skill.nextSkillId)" >
      <template #content>
        <skill-navigation @prevButtonClicked="prevButtonClicked" @nextButtonClicked="nextButtonClicked" :skill="skillsState.skill" buttonSeverity="info" />
      </template>
    </Card>
    <page-header :loading="isLoading" :options="headerOptions">
      <template #subTitle v-if="skillsState.skill">
        <div v-for="(tag) in skillsState.skill.tags" :key="tag.tagId" class="h6 mr-2 d-inline-block"
             :data-cy="`skillTag-${skillsState.skill.skillId}-${tag.tagId}`">
          <Badge variant="info">
            <span><i class="fas fa-tag"></i> {{ tag.tagValue }}</span>
          </Badge>
        </div>
        <div class="h5 text-muted" data-cy="skillId">
          <show-more :limit="54" :text="skillId"></show-more>
        </div>
        <div class="h5 text-muted" v-if="skillsState.skill && skillsState.skill.groupId">
          <span style="font-size: 1rem">Group:</span> <span>{{ skillsState.skill.groupName }}</span>
        </div>
      </template>
      <template #subSubTitle v-if="!isImported">
        <SkillsButton
          id="edidSkillBtn"
          v-if="skillsState.skill && !isReadOnlyProj"
          @click="displayEdit"
          size="small"
          outlined
          severity="info"
          label="Edit"
          icon="fas fa-edit"
          :track-for-focus="true"
          :data-cy="`editSkillButton_${route.params.skillId}`"
          :aria-label="'edit Skill '+skillsState.skill.name"
          ref="editSkillInPlaceBtn" />
      </template>
      <template #right-of-header
                v-if="!isLoading && (skillsState.skill.sharedToCatalog || isImported || !skillsState.skill.enabled)">
        <Tag v-if="skillsState.skill.sharedToCatalog" class="ml-2" data-cy="exportedBadge"><i
          class="fas fa-book" aria-hidden="true"></i> EXPORTED
        </Tag>
        <Tag v-if="isImported" class="ml-2" severity="success" data-cy="importedBadge" aria-label="Reused">
          <span v-if="skillsState.skill.reusedSkill"><i class="fas fa-recycle"  aria-hidden="true"></i> Reused</span>
          <span v-else><i class="fas fa-book" aria-hidden="true"></i> IMPORTED</span>
        </Tag>
        <Tag v-if="!skillsState.skill.enabled"
             severity="secondary"
             class="ml-2" data-cy="disabledSkillBadge"><i
          class="fas fa-eye-slash mr-1" aria-hidden="true"></i> DISABLED</Tag>
      </template>
    </page-header>

    <navigation :nav-items="navItems">
    </navigation>

    <edit-skill
      v-if="showEdit"
      v-model="showEdit"
      :skill="skillsState.skill"
      :is-subject-enabled="subjectState.subject.enabled"
      :is-edit="true"
      :project-user-community="projConfig.getProjectCommunityValue()"
      @skill-saved="skillEdited" />
  </div>
</template>

<style scoped></style>
