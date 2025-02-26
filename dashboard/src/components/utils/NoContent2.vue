/*
Copyright 2020 SkillTree

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

defineProps({
  title: String,
  message: {
    type: String,
    default: '',
  },
  icon: {
    type: String,
    default: 'fas fa-dragon',
  },
  showRefreshAction: {
    type: Boolean,
    default: false,
  }
})

function refresh() {
  window.location.reload();
}
</script>

<template>
  <div class="text-muted-color text-center" data-cy="noContent" role="alert" aria-live="assertive"
       aria-atomic="true">
    <div class="flex justify-center text-center">
      <div class="rounded-full w-32 h-32 m-2 bg-surface-500 dark:bg-surface-300 font-bold flex items-center justify-center">
        <i class="text-surface-0 dark:text-surface-900 text-7xl" :class="icon"></i>
      </div>
    </div>
    <div v-if="title" class="mt-1">
      <div class="text-3xl text-center">{{ title }}</div>
    </div>
    <div class="flex justify-center mt-1 sd-theme-primary-color">
      <div class="max-w-full lg:max-w-2xl">
        <slot>
          {{ message }}
        </slot>
      </div>
    </div>
    <div v-if="showRefreshAction" class="grid grid-cols-12 gap-4 justify-center mt-4">
      <div class="col col-md-8 col-lg-7 col-xl-5 no-content">
        Please
        <Button severity="info" @click="refresh" class="uppercase" size="small"
                  data-cy="refreshBtn"><i class="fas fa-redo-alt"></i> refresh
        </Button>
        the page.
      </div>
    </div>
  </div>
</template>

<style scoped>
.circle-icon {
  font-size: 8rem;
}

.in-circle-icon {
  font-size: 3.5rem;
  position: absolute;
  top: 2.2rem;
  right: 0;
  left: 0;
  margin: auto;
}

</style>
