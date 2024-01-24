<script setup>
import { ref, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";

const router = useRouter();
const route = useRoute()
const props = defineProps(['navItems']);
const collapsed = ref(false)

function getCollapsedFromLocalStorage() {
  const storageCollapsed = localStorage.skillsNavCollapsed;
  collapsed.value = storageCollapsed === 'true' ? Boolean(storageCollapsed) : false;
}
function flipCollapsed() {
  collapsed.value = !collapsed.value;
  localStorage.skillsNavCollapsed = collapsed.value;
}
onMounted(() => {
  getCollapsedFromLocalStorage()
})
</script>

<template>
  <div class="mt-3" data-cy="nav">
    <div class="flex">
      <div class="flex-none" data-cy="nav-col">
        <div class="border-1 border-300 border-round-md surface-border font-medium surface-0" style="min-height: calc(100vh - 20rem); !important">
            <div class="text-900 font-semibold flex">
              <div v-if="!collapsed" class="pt-3 px-3">Navigate</div>
              <div class="flex-1" :class="{ 'text-right': !collapsed, 'text-center': collapsed}">
                <Button size="small" text
                        v-ripple
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
                    <div class="" :class="{'mr-4': !collapsed}">
                      <i :class="navItem.iconClass" class="fas text-base mr-2"
                         aria-hidden="true" /> <span v-if="!collapsed" class="font-medium">{{ navItem.name }}</span>
                      <i v-if="navItem.isDisabled" class="fas fa-exclamation-circle text-red-500 ml-1" />
                    </div>
                  </Button>
                </li>
              </router-link>
            </ul>
        </div>
      </div>

      <div class="flex-1" ref="content">
        <div class="pl-3">
          <router-view id="mainContent2" tabindex="-1" aria-label="Main content area, click tab to navigate"></router-view>
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
