/*
Copyright 2024 SkillTree

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
  title: {
    type: String,
    required: true,
  },
  componentName: {
    type: String,
    required: true,
  },
  userAchieved: {
    required: true,
  },
  totalAvailable: {
    required: true,
  },
  unit: {
    type: String,
    required: true,
  },
  icon: {
    type: String,
    required: true
  },
  route: {
    type: Object,
    required: false,
  },
  isSummaryOnly: {
    type: Boolean,
    required: false,
  },
  optedOut: {
    type: Boolean,
    required: false,
  },
  loading: {
    type: Boolean,
    required: false,
    default: false,
  }
})
</script>

<template>
  <Card class="skills-progress-card w-min-15rem h-full" :data-cy="componentName" :pt="{ content: { class: 'py-0' } }">
    <template #subtitle>
      <div class="text-center text-xl font-medium" :data-cy="`${componentName}Title`">
        {{ title }}
      </div>
    </template>
    <template #content>
      <div class="fa-stack skills-icon user-rank-stack text-blue-300 flex flex-wrap align-items-center" v-if="!loading">
        <i :class="`${icon} fa-stack-2x watermark-icon`" />

        <div v-if="optedOut" class="pt-2 text-danger fa-stack-1x user-rank-text sd-theme-primary-color font-bold text-blue-700 text-lg" data-cy="optedOutMessage">
          <div>Opted-Out</div>
          <div style="font-size: 0.8rem; line-height: 1rem;" class="mb-2">
            Your position would be {{ userAchieved }} if you opt-in!
          </div>
        </div>
        <div class="fa-stack-1x user-rank-text sd-theme-primary-color font-bold text-blue-700 text-lg p-1" v-else>
          <div class="text-3xl" style="line-height: 1.2em" :data-cy="`${componentName}Position`">{{ userAchieved }}</div>
          <div class="mt-1">out of</div>
          <div>{{ totalAvailable }} {{ unit }}</div>
        </div>
      </div>
      <skills-spinner :is-loading="loading"/>
    </template>
    <template #footer v-if="!isSummaryOnly">
      <router-link
          :to="route"
          :aria-label="`Click to navigate to ${title} page`"
          :data-cy="`${componentName}Btn`" tabindex="-1">
        <Button
            label="View"
            icon="far fa-eye"
            outlined class="w-full" size="small" />
      </router-link>
    </template>
  </Card>
</template>

<style scoped>
@media only screen and (min-width: 1200px) {
  .skills-progress-card {
    min-width: 18rem !important;
  }
}

.skills-progress-card .skills-icon {
  display: inline-block;
  color: #b1b1b1;
  margin: 5px 0;
}

.skills-progress-card .skills-icon.user-rank-stack {
  margin: 14px 0;
  font-size: 4.1rem;
  width: 100%;
  //color: #0fcc15d1;
}

.skills-progress-card .skills-icon.user-rank-stack i {
  opacity: 0.38;
}

.skills-progress-card .user-rank-text {
  font-size: 0.4em;
  line-height: 0.9em;
  background: rgba(255, 255, 255, 0.6);
}
</style>