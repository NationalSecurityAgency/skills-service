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
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStorage } from '@vueuse/core'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useContentMaxWidthState } from '@/stores/UseContentMaxWidthState.js'
import { useLayoutSizesState } from '@/stores/UseLayoutSizesState.js'

const router = useRouter();
defineProps(['navItems']);
const collapsed = useStorage('navigationCollapsed', false)
const mainContentWidth = useContentMaxWidthState()
const colors = useColors()
const layoutSizes = useLayoutSizesState()
const showCollapsed = computed(() => collapsed.value)

function flipCollapsed() {
  collapsed.value = !collapsed.value;
  nextTick(() => {
    mainContentWidth.updateWidth()
    handleResize()
  })
}

const navOnSmallScreen = (changeEvent) => {
  router.push({ name: changeEvent.value.page })
}

const skillsNavigation = ref(null)
const handleResize = () => {
  layoutSizes.updateNavbarWidth(skillsNavigation.value.getBoundingClientRect().width)
}
onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize);
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
})

</script>

<template>
  <div class="mt-4" data-cy="nav" aria-label="Navigation">
    <div class="sticky top-0 mb-4 md:hidden" style="z-index: 101">
      <Select
        :options="navItems"
        @change="navOnSmallScreen"
        optionLabel="name"
        data-cy="navSmallScreenExpandMenu"
        placeholder="Navigation"
        class="w-full md:w-56">
        <template #option="slotProps">
          <div class="flex items-center">
            <i :class="slotProps.option.iconClass" class="fas text-base mr-2 w-8" aria-hidden="true" />
            <div>{{ slotProps.option.name }}</div>
          </div>
        </template>
        <template #value>
          <i class="fas fa-bars mr-2" aria-hidden="true"></i>
          <span class="uppercase">Navigation</span>
        </template>
      </Select>
    </div>

    <div class="flex">
      <div id="skillsNavigation" ref="skillsNavigation" class="flex-none hidden md:flex" data-cy="nav-col">
        <div class="border border-surface-300 dark:border-surface-500 rounded-md border-surface font-medium bg-surface-0 dark:bg-surface-900" style="min-height: calc(100vh - 20rem); !important">
            <div class="text-surface-900 dark:text-surface-0 font-semibold flex">
              <div v-if="!showCollapsed" class="pt-4 px-4">Navigate</div>
              <div class="flex-1" :class="{ 'text-right': !showCollapsed, 'text-center': showCollapsed}">
                <Button size="small" text
                        data-cy="navCollapseOrExpand"
                        @click="flipCollapsed"
                        :aria-label="collapsed ? 'Expand Navigation' : 'Collapse Navigation'"
                        :title="collapsed ? 'Expand Navigation' : 'Collapse Navigation'">
                  <i v-if="!collapsed" class="fas fa-compress-alt"/><i v-else class="fas fa-expand-alt"/>
                </Button>
              </div>
            </div>
            <ul class="list-none p-0 text-color">
              <router-link v-for="(navItem, index) of navItems"
                           :key="navItem.name"
                           :to="{ name: navItem.page }"
                           v-slot="{ navigate, isExactActive }"
                           custom>
                <li :class="{ 'bg-primary ': isExactActive }">
                  <Button link
                          class="no-underline w-full"
                          @click="(e) => { navigate(e); }"
                          :aria-label="`Navigate to ${navItem.name} page`"
                          :aria-current="isExactActive ? 'page' : false"
                          :data-cy="`nav-${navItem.name}`">
                    <div class="flex justify-start w-full" :class="{'mr-4': !showCollapsed, 'text-zinc-100': isExactActive }">
                      <i :class="`${navItem.iconClass} ${colors.getTextClass(index)}${isExactActive ? ' text-primary bg-primary-contrast rounded-border border py-1' : ''}`"
                         class="fas mr-2 w-8"
                         aria-hidden="true" /> <span v-if="!showCollapsed" class="font-medium">{{ navItem.name }}</span>
                    </div>
                  </Button>
                </li>
              </router-link>
            </ul>
        </div>
      </div>

      <div class="flex-1" ref="content">
        <div class="md:pl-4" id="mainContent2"
             tabindex="-1"
             aria-label="Main content area, click tab to navigate">
<!--          <router-view id="mainContent2" -->
<!--                       tabindex="-1" -->
<!--                       aria-label="Main content area, click tab to navigate"></router-view>-->
          <router-view />
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
a:visited {
  color: inherit;
}
</style>
