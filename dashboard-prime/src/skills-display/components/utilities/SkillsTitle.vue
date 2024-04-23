<script setup>
import { computed } from 'vue'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import SkillsDisplayBreadcrumb from '@/skills-display/components/header/SkillsDisplayBreadcrumb.vue'
import PoweredBySkilltree from '@/skills-display/components/header/PoweredBySkilltree.vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useSkillsDisplayBreadcrumbState } from '@/skills-display/stores/UseSkillsDisplayBreadcrumbState.js'

const displayPreferences = useSkillsDisplayPreferencesState()
const themeState = useSkillsDisplayThemeState()
const breadcrumb = useSkillsDisplayBreadcrumbState()

const props = defineProps({
  backButton: { type: Boolean, default: true },
  animatePowerByLabel: { type: Boolean, default: true }
})

const showBackButton = computed(() => {
  return props.backButton && displayPreferences.internalBackButton
})
const navigateBack = () => {
  breadcrumb.navUpBreadcrumb()
}
</script>

<template>
  <Card class=" skills-theme-page-title" data-cy="skillsTitle"
        :pt="{ body: { class: 'p-0' }, content: { class: 'px-0 py-2' } }">
    <template #content>
      <div class="skills-page-title-text-color h-min-" style="position: relative; min-height: 4rem;">
        <div v-if="backButton"
             class="absolute" style="top: 1rem !important; left: 1rem !important;">
          <SkillsButton
            @click="navigateBack"
            outlined
            icon="fas fa-arrow-left"
            class="skills-theme-btn"
            data-cy="back"
            aria-label="navigate back" />
        </div>

        <div :class="{'mx-5': showBackButton}" class="text-center">
          <SkillsDisplayBreadcrumb></SkillsDisplayBreadcrumb>
          <div data-cy="title" class="skills-title uppercase text-3xl">
            <slot />
          </div>
        </div>

        <div v-if="!themeState.theme.disableSkillTreeBrand"
             class="absolute" style="top: 0.25rem !important; right: 1rem !important;">
          <powered-by-skilltree
            :animate-power-by-label="animatePowerByLabel" />
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>