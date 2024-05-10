<script setup>
import { computed, ref } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import SkillTreeSvgIcon from '@/skills-display/components/header/SkillTreeSvgIcon.vue'

defineProps({
  animatePowerByLabel: {
    type: Boolean,
    default: true,
  }
})
const appConfig = useAppConfig()
const themeState = useSkillsDisplayThemeState()

const logoFillThemeColor = computed(() => {
  //  1. if skillTreeBrandColor is provided
  //  2. pageTitle.textColor
  //  3. pageTitleTextColor (backward compat)

  if (themeState.theme.skillTreeBrandColor) {
    return themeState.theme.skillTreeBrandColor
  }
  const color = themeState.theme.pageTitle && themeState.theme.pageTitle.textColor
    ? themeState.theme.pageTitle.textColor : null
  if (color) {
    return color
  }
  return themeState.theme.pageTitleTextColor
})

const isHovering = ref(false)
</script>

<template>
  <div class="fadein animation-duration-500 w-8rem text-left" data-cy="skillTreePoweredBy">
    <a :href="appConfig.docsHost" target="_blank" class=""
       aria-label="Powered by SkillTree">
      <div
        class="poweredByContainer flex border-1 border-round surface-border py-1 px-2"
        @mouseover="isHovering = true"
        @mouseout="isHovering = false">

        <div class="poweredByLabel pb-2 skills-theme-brand"
             :class="{'flipleft animation-duration-2000':animatePowerByLabel}">powered by
        </div>
        <skill-tree-svg-icon
          class="float-right skill-tree-svg-icon" :is-hovering="isHovering" :logo-fill="logoFillThemeColor" />
      </div>
    </a>
  </div>

</template>

<style scoped>
.poweredByContainer .poweredByLabel {
  font-size: 0.8rem !important;
  margin-right: -20px !important;
  margin-bottom: -10px !important;
  //position: absolute !important;
  //bottom: 0.8rem;
}

.poweredByContainer .skill-tree-svg-icon {
  //width: 4.5rem;
}
</style>