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

const logoFillThemeColor = computed(() => themeState.theme.skillTreeBrandColor)
const isHovering = ref(false)
</script>

<template>
  <div class="animate-fadein animate-duration-500 w-32 text-left" data-cy="skillTreePoweredBy">
    <a :href="appConfig.docsHost" target="_blank" class=""
       aria-label="Powered by SkillTree">
      <div
        class="poweredByContainer flex border rounded-border border-surface py-1 px-2"
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
}

</style>