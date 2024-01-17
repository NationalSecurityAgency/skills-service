<script setup>
import { ref } from "vue";
import { useRouter } from "vue-router";

const router = useRouter();

const items = ref([
  {
    label: 'Projects',
    icon: 'pi pi-list',
    route: '/unstyled'
  },
  {
    label: 'Quizzes & Surveys',
    icon: 'pi pi-check-square',
    command: () => {
      this.$router.push('/introduction');
    }
  }
]);
</script>

<template>
  <div class="mt-3" data-cy="nav">
    <div class="row">
      <div class="col-md-auto" data-cy="nav-col">
        <Menu :model="items">
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

      <div class="col-md skills-menu-content" ref="content">
        <div class="pb-4">
          <router-view id="mainContent2" tabindex="-1" aria-label="Main content area, click tab to navigate"></router-view>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped></style>
