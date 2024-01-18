<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();
const props = defineProps(['navItems']);
</script>

<template>
  <div class="mt-3" data-cy="nav">
    <div class="flex">
      <div class="flex-none" data-cy="nav-col">
        <Menu :model="navItems">
          <template #start>
            <div class="px-3 h6 mt-2">
              Navigate
            </div>
          </template>
          <template #item="{ item, props }">
            <router-link v-if="item.route" v-slot="{ href, navigate }" :to="item.route" custom>
              <a v-ripple :href="href" v-bind="props.action" @click="navigate">
                <span :class="item.icon" />
                <span class="ms-2">{{ item.label }}</span>
              </a>
            </router-link>
            <a v-else v-ripple :href="item.url" :target="item.target" v-bind="props.action">
              <span :class="item.icon" />
              <span class="ms-2">{{ item.label }}</span>
            </a>
          </template>
        </Menu>
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
.select-cursor {
  cursor: pointer;
}

@media (min-width: 768px) {
  .skills-nav {
    min-height: calc(100vh - 10rem);
  }
}

.nav-title {
  color: #3f5971;
}

.skills-menu-content {
  /* this little hack is required to prevent apexcharts from wrapping onto a new line;
  the gist is that they calculate width dynamically and do not work properly with the width of 0*/
  min-width: 1rem;
}

.skills-menu-content:focus {
  outline: none;
}
</style>
