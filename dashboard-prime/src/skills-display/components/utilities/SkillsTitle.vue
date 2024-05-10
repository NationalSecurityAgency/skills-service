<script setup>
import { computed } from 'vue'
import SkillsDisplayBreadcrumb from '@/skills-display/components/header/SkillsDisplayBreadcrumb.vue'
import PoweredBySkilltree from '@/skills-display/components/header/PoweredBySkilltree.vue'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import { useSkillsDisplayBreadcrumbState } from '@/skills-display/stores/UseSkillsDisplayBreadcrumbState.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'

const attributes = useSkillsDisplayAttributesState()
const themeState = useSkillsDisplayThemeState()
const breadcrumb = useSkillsDisplayBreadcrumbState()

const props = defineProps({
  backButton: { type: Boolean, default: true },
  animatePowerByLabel: { type: Boolean, default: true }
})

const showBackButton = computed(() => {
  return props.backButton && attributes.internalBackButton
})
const navigateBack = () => {
  breadcrumb.navUpBreadcrumb()
}

const disableBreadcrumb = computed(() => themeState.theme.disableBreadcrumb)
const backButtonOrBrandPresent = computed(() => showBackButton.value || !themeState.theme.disableSkillTreeBrand)

</script>

<template>
  <Card class="skills-theme-page-title" data-cy="skillsTitle"
        :pt="{ body: { class: 'p-0' }, content: { class: 'px-2 pt-2 pb-3' } }">
    <template #content>
      <div class="flex flex-wrap flex-column md:flex-row align-content-center gap-2">
        <div v-if="backButtonOrBrandPresent"
             class="text-center md:text-left md:w-8rem">
          <SkillsButton
            v-if="showBackButton"
            @click="navigateBack"
            outlined
            icon="fas fa-arrow-left"
            class="skills-theme-btn"
            data-cy="back"
            aria-label="navigate back" />
        </div>

        <div :class="{'mx-5': showBackButton}" class="text-center flex-1">
          <SkillsDisplayBreadcrumb v-if="!disableBreadcrumb"></SkillsDisplayBreadcrumb>
          <div data-cy="title"
               :class="{ 'mt-2': disableBreadcrumb}"
               class="skills-title uppercase text-3xl">
            <slot />
          </div>
        </div>

        <div v-if="backButtonOrBrandPresent" class="md:w-8rem">
          <div v-if="!themeState.theme.disableSkillTreeBrand"
               class="flex align-items-center justify-content-center" >
            <powered-by-skilltree :animate-power-by-label="animatePowerByLabel" />
          </div>
        </div>
      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>