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
  <b-overlay :show="doShow" opacity=".5">
    <slot />
    <template v-slot:overlay>
      <div v-if="loading">
        <b-spinner variant="info" label="Spinning"></b-spinner>
      </div>
      <div v-if="!loading && !loadedOnce && !hasData" class="alert alert-info">
        <i :class="chartNotGeneratedIcon"></i> {{ chartNotGeneratedMsg }}
      </div>
      <div v-if="!loading && loadedOnce && !hasData" class="alert alert-info">
        <i :class="noDataIcon" class="mr-1"></i> {{ noDataMsg }}
      </div>
    </template>
  </b-overlay>
</template>

<script>
  export default {
    name: 'MetricsOverlay',
    props: {
      hasData: {
        type: Boolean,
        required: true,
      },
      loading: {
        type: Boolean,
        required: true,
      },
      noDataMsg: {
        type: String,
        default: 'No Data. Yet...',
        required: false,
      },
      noDataIcon: {
        type: String,
        default: 'fas fa-dragon',
        required: false,
      },
      loadedOnce: {
        type: Boolean,
        default: true,
        required: false,
      },
      chartNotGeneratedMsg: {
        type: String,
        default: 'Generate the chart using controls above!',
        required: false,
      },
      chartNotGeneratedIcon: {
        type: String,
        default: 'fas fa-chart-line',
        required: false,
      },
    },
    computed: {
      doShow() {
        return this.loading || !this.hasData;
      },
    },
  };
</script>

<style scoped>

</style>
