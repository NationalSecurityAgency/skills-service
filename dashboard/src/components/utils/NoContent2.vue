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
  <div class="text-color-secondary text-center" data-cy="noContent" role="alert" aria-live="assertive"
       aria-atomic="true">
    <div class="flex justify-content-center text-center">
      <div class="border-circle w-8rem h-8rem m-2 surface-500 font-bold flex align-items-center justify-content-center">
        <i class="text-0 text-7xl" :class="icon"></i>
      </div>
    </div>
    <div v-if="title" class="mt-1">
      <div class="text-3xl no-content text-center">{{ title }}</div>
    </div>
    <div class="grid justify-content-center mt-1">
      <div class="col md:col-8 lg:col-7 xl:col-5 no-content sd-theme-primary-color">
        <slot>
          {{ message }}
        </slot>
      </div>
    </div>
    <div v-if="showRefreshAction" class="grid justify-content-center mt-3">
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
