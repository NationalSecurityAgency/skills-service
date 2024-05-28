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
const renderDivWhereBackButtonResides = computed(() => (showBackButton.value || !themeState.theme.disableSkillTreeBrand) && !themeState.theme?.breadcrumb?.align)
const renderDivWhereBrandResides = computed(() => showBackButton.value || !themeState.theme.disableSkillTreeBrand)
const isThemeAligned = computed(() => themeState.theme?.pageTitle?.textAlign)
</script>

<template>
  <Card class="skills-theme-page-title" data-cy="skillsTitle"
        role="heading" aria-level="1"
        :pt="{ body: { class: 'p-0' }, content: { class: 'px-2 pt-2 pb-3' } }">
    <template #content>
      <div class="flex flex-wrap flex-column md:flex-row align-content-center gap-2" :class="{'px-2': !renderDivWhereBackButtonResides}">
        <div v-if="renderDivWhereBackButtonResides"
             :class="{'text-center md:text-left md:w-8rem': !isThemeAligned}">
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

        <div v-if="renderDivWhereBrandResides" class="md:w-8rem">
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