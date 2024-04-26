<script setup>
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { useStorage } from '@vueuse/core'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'

const router = useRouter();
defineProps(['navItems']);
const collapsed = useStorage('navigationCollapsed', false)
const responsive = useResponsiveBreakpoints()
const showCollapsed = computed(() => collapsed.value || responsive.lg.value)

function flipCollapsed() {
  collapsed.value = !collapsed.value;
}

const navOnSmallScreen = (changeEvent) => {
  router.push({ name: changeEvent.value.page })
}
</script>

<template>
  <div class="mt-3" data-cy="nav">
    <div class="sticky top-0 z-5 mb-3 md:hidden">
      <Dropdown
        :options="navItems"
        @change="navOnSmallScreen"
        optionLabel="name"
        placeholder="Navigation"
        class="w-full md:w-14rem">
        <template #option="slotProps">
          <div class="flex align-items-center">
            <i :class="slotProps.option.iconClass" class="fas text-base mr-2 w-2rem" aria-hidden="true" />
            <div>{{ slotProps.option.name }}</div>
          </div>
        </template>
        <template #value>
          <i class="fas fa-bars mr-2" aria-hidden="true"></i>
          <span class="uppercase">Navigation</span>
        </template>
      </Dropdown>
    </div>

    <div class="flex">
      <div class="flex-none hidden md:flex" data-cy="nav-col">
        <div class="border-1 border-300 border-round-md surface-border font-medium surface-0" style="min-height: calc(100vh - 20rem); !important">
            <div class="text-900 font-semibold flex">
              <div v-if="!showCollapsed" class="pt-3 px-3">Navigate</div>
              <div class="flex-1 hidden lg:block" :class="{ 'text-right': !showCollapsed, 'text-center': showCollapsed}">
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
              <router-link v-for="(navItem) of navItems"
                           :key="navItem.name"
                           :to="{ name: navItem.page }"
                           v-slot="{ href, navigate, isActive, isExactActive }"
                           custom>
<!--                v-b-tooltip="{ title: navItem.msg ? navItem.msg : navItem.name, placement: 'right', variant: 'primary', disabled: !collapsed && !navItem.isDisabled }"-->
                <li>
                  <Button link
                          :disabled="navItem.isDisabled"
                          :class="{ 'bg-primary': isExactActive }"
                          class="no-underline w-full"
                          @click="(e) => { navigate(e); }"
                          :aria-label="`Navigate to ${navItem.name} page`"
                          :aria-current="isExactActive ? 'page' : false"
                          :data-cy="`nav-${navItem.name}`">
                    <div class="" :class="{'mr-4': !showCollapsed}">
                      <i :class="navItem.iconClass"
                         v-tooltip="{ value: navItem.name, autoHide: false, disabled: !responsive.lg.value }"
                         class="fas text-base mr-2 w-2rem"
                         aria-hidden="true" /> <span v-if="!showCollapsed" class="font-medium">{{ navItem.name }}</span>
                      <i v-if="navItem.isDisabled" class="fas fa-exclamation-circle text-red-500 ml-1" />
                    </div>
                  </Button>
                </li>
              </router-link>
            </ul>
        </div>
      </div>

      <div class="flex-1" ref="content">
        <div class="md:pl-3" id="mainContent2" aria-label="Main content area, click tab to navigate">
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
