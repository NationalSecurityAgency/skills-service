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
<template>
  <div class="card h-100">
    <div class="card-body">
      <div class="row mb-2">
        <div class="col">
          <div class="media">
            <div v-if="options.icon" class="d-inline-block mr-2 border rounded text-info text-center" style="min-width: 3.2rem;">
              <i :class="[`${options.icon}`]" class="m-1"/>
            </div>
            <div class="media-body" style="min-width: 0px;">
              <div class="text-truncate text-info mb-0 pb-0 preview-card-title">{{ options.title }}
              <i v-if="options.warn" class="fas fa-exclamation-circle text-warning ml-1" style="font-size: 1.5rem;" v-b-tooltip.hover="options.warnMsg"/>
              </div>
              <div class="text-truncate text-secondary preview-card-subTitle">{{ options.subTitle }}</div>
            </div>
            <div class="col-auto text-center text-sm-right p-0">
              <slot name="header-top-right"></slot>
            </div>
          </div>
        </div>
      </div>

      <div class="row text-center justify-content-center">
        <div v-for="(stat) in options.stats" :key="stat.label" class="col my-3" style="min-width: 10rem;">
          <div>
            <p class="text-uppercase text-muted count-label">{{ stat.label }}</p>
            <strong class="h5">{{ stat.count | number }}</strong>
            <i v-if="stat.warn" class="fas fa-exclamation-circle text-warning ml-1" style="font-size: 1.5rem;" v-b-tooltip.hover="stat.warnMsg"/>
          </div>
        </div>
      </div>

      <div class="text-center mt-1">
        <slot name="footer"></slot>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'PagePreviewCard',
    props: {
      options: {
        icon: String,
        title: String,
        warn: Boolean,
        warnMsg: String,
        subTitle: String,
        stats: [],
      },
    },
  };
</script>

<style scoped>
  .preview-card-title {
    font-size: 1.4rem;
    font-weight: bold;
  }

  .preview-card-subTitle {
    max-width: 12rem;
    font-size: 0.8rem;
  }

  .count-label {
    font-size: 0.9rem;
  }

  i {
    font-size: 2.5rem;
    display: inline-block;
  }
</style>
