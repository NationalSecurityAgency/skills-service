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
import {computed, onMounted, ref} from 'vue'
import SkillsDisplayBreadcrumb from '@/skills-display/components/header/SkillsDisplayBreadcrumb.vue'
import PoweredBySkilltree from '@/skills-display/components/header/PoweredBySkilltree.vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useSkillsDisplayBreadcrumbState } from '@/skills-display/stores/UseSkillsDisplayBreadcrumbState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import SkillsDisplaySearch from '@/skills-display/components/SkillsDisplaySearch.vue'
import ProjectService from "@/components/projects/ProjectService.js";
import {useRoute} from "vue-router";

const route = useRoute()

const attributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const breadcrumb = useSkillsDisplayBreadcrumbState()
const skillsDisplayInfo = useSkillsDisplayInfo()

const projectId = attributes.projectId
const showSkillsDisplaySearchDialog = ref(false)

const props = defineProps({
  backButton: { type: Boolean, default: true },
  animatePowerByLabel: { type: Boolean, default: true },
})

const showBackButton = computed(() => {
  return props.backButton && attributes.internalBackButton
})
const navigateBack = () => {
  breadcrumb.navUpBreadcrumb()
}
const isTrueCaseInsensitive = (value) => {
  return value === true || String(value).toLowerCase() === 'true';
}

const disableSearchButton = computed(() => isTrueCaseInsensitive(themeState.theme.disableSearchButton))
const disableBreadcrumb = computed(() => isTrueCaseInsensitive(themeState.theme.disableBreadcrumb))
const disableSkillTreeBrand = computed(() => isTrueCaseInsensitive(themeState.theme.disableSkillTreeBrand))
const renderDivWhereBackButtonResides = computed(() => (showBackButton.value || !disableSearchButton.value || !disableSkillTreeBrand.value))
const renderDivWhereBrandResides = computed(() => showBackButton.value || !disableSkillTreeBrand.value)
const isThemeAligned = computed(() => themeState.theme?.pageTitle?.textAlign)

const isProjectLevel = computed(() => {
  return !(route.params.skillId || route.params.badgeId || route.params.subjectId || (route.params.tagKey && route.params.tagFilter))
})

const isMyProject = ref(false);
const showAddedMsg = ref(false);

onMounted(() => {
  loadProjectSavedStatus()
})

const loadProjectSavedStatus = () => {
  ProjectService.isMyProject(attributes.projectId).then((res) => {
    isMyProject.value = res;
  });
}

const addToMyProjects = () => {
  ProjectService.addToMyProjects(attributes.projectId).then(() => {
        loadProjectSavedStatus();
        showAddedMsg.value = true;
        setTimeout(() => {
          showAddedMsg.value = false;
        }, 4000);
      })
}
</script>

<template>
  <Card class="skills-theme-page-title" data-cy="skillsTitle"
        :pt="{ body: { class: 'p-0!' }, content: { class: 'px-2! pt-2! pb-3!' } }">
    <template #content>
      <div class="flex flex-wrap flex-col md:flex-row content-center gap-2" :class="{'px-2': !renderDivWhereBackButtonResides}">
        <div v-if="renderDivWhereBackButtonResides"
             :class="{'text-center md:text-left md:w-32': !isThemeAligned}">
          <SkillsButton
            v-if="showBackButton"
            @click="navigateBack"
            outlined
            icon="fas fa-arrow-left"
            class="skills-theme-btn"
            data-cy="back"
            aria-label="navigate back" />

          <SkillsButton
              v-if="!disableSearchButton"
              id="skillsDisplaySearchBtn"
              :track-for-focus="true"
              class="skills-search-btn"
              :class="{'ml-2': showBackButton}"
              @click="showSkillsDisplaySearchDialog = true"
              data-cy="skillsDisplaySearchBtn"
              title="Search Project"
              icon="fa-solid fa-magnifying-glass" />
        </div>

        <div :class="{'mx-5': showBackButton}" class="text-center flex flex-col md:flex-row items-center">
          <SkillsDisplayBreadcrumb v-if="!disableBreadcrumb"></SkillsDisplayBreadcrumb>
          <h1 data-cy="title"
               :class="{ 'mt-2': disableBreadcrumb}"
               class="skills-title uppercase text-2xl font-normal m-0">
            <slot />
          </h1>
          <SkillsButton
              v-if="!isMyProject && !showAddedMsg && isProjectLevel"
              label="Add To My Projects"
              icon="fa-solid fa-heart-circle-plus"
              @click="addToMyProjects()"
              outlined
              class="animate-fadein animate-duration-300 mt-2 ml-4"
              size="small"
              :data-cy="`addButton-${attributes.projectId}`"
              :aria-label="`add project ${attributes.projectId} to my projects`"/>
          <InlineMessage v-if="showAddedMsg" class="ml-4 mt-2" severity="success">
            Project added!
          </InlineMessage>
        </div>

        <div v-if="renderDivWhereBrandResides" class="md:w-32">
          <div v-if="!disableSkillTreeBrand"
               class="flex items-center justify-center" >
            <powered-by-skilltree :animate-power-by-label="animatePowerByLabel && skillsDisplayInfo.isHomePage.value" />
          </div>
        </div>

        <skills-display-search v-if="showSkillsDisplaySearchDialog && !disableSearchButton"
                               ref="skillsDisplaySearch"
                               v-model="showSkillsDisplaySearchDialog"
                               :project-id="projectId" />
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>