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
      <div class="row">
        <div class="col">
          <h5 class="text-uppercase text-muted card-title small mb-1">{{ title }}</h5>
          <span class="h4 font-weight-bold mb-0" data-cy="statCardValue">
            <span v-if="!calculateTimeFromNow">{{ statNum | number }}</span>
            <span v-else>{{ statNum | timeFromNow }}</span>
          </span>
        </div>
        <div class="col-auto">
          <i :class="icon" style="font-size: 2.2rem;"/>
        </div>
      </div>
      <p class="text-muted small mt-3 mb-0" data-cy="statCardDescription"><slot /></p>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'StatsCard',
    props: {
      title: {
        type: String,
        required: true,
      },
      statNum: {
        type: Number,
        required: true,
      },
      icon: {
        type: String,
        required: true,
      },
      calculateTimeFromNow: {
        type: Boolean,
        required: false,
        default: false,
      },
    },
  };
</script>

<style scoped>

</style>
