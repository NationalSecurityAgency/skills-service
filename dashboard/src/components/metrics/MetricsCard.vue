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

      <div class="media">
        <div class="d-inline-block mt-1 mr-3">
          <i class="fa-3x" :class="icon"/>
        </div>
        <div class="media-body">
          <h4 class="mb-2 text-uppercase">{{ title }}</h4>
          <div style="font-size: 0.95rem;" class="text-secondary">
            <slot>
              {{ subtitle }}
            </slot>
          </div>
        </div>
      </div>
      <p class="mt-2">
        <small>{{ description }}</small>
      </p>
    </div>

    <div class="card-footer text-center">
      <b-button @click="loadChart" variant="outline-hc">Add <i class="fas fa-plus-circle"/></b-button>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'StatCard',
    props: ['title', 'subtitle', 'description', 'icon', 'chartBuilderId'],
    methods: {
      loadChart() {
        this.$emit('load-chart', this.chartBuilderId);
      },
    },
  };
</script>

<style scoped>
  .card {
    min-width: 15rem;
  }
</style>
