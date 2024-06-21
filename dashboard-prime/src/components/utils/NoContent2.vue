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

const props = defineProps({
  title: String,
  message: {
    type: String,
    default: '',
  },
  icon: {
    type: String,
    default: 'fas fa-dragon',
  },
  iconSize: {
    type: String,
    default: 'fa-3x',
  },
  iconColor: {
    type: String,
    default: 'text-color-secondary',
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
  <div class="text-color-secondary text-center" data-cy="noContent" role="alert" aria-live="assertive" aria-atomic="true">
    <div class="grid justify-content-center text-center">
      <div class="col">
        <span class="fa-stack text-center" :class="iconSize" style="vertical-align: top;" aria-hidden="true">
          <i class="fas fa-circle fa-stack-2x" :class="iconColor"></i>
          <i class="fa-stack-1x fa-inverse" :class="icon"></i>
        </span>
      </div>
      <div class="w-full"></div>
      <div class="col pt-2">
        <div class="text-3xl no-content text-center">{{ title }}</div>
      </div>
    </div>
    <div class="grid justify-content-center">
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

</style>
