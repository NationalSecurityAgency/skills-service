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
import CardWithVericalSections from "@/components/utils/cards/CardWithVericalSections.vue";

const props = defineProps({
  title: {
    type: String,
    required: true,
  },
  componentName: {
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
  loading: {
    type: Boolean,
    required: false,
    default: false,
  }
})
</script>

<template>
  <CardWithVericalSections class="skills-progress-card w-min-15rem h-full" :data-cy="componentName">
    <template #header>
      <h2 class="text-center text-xl font-medium pt-4 px-4 pb-5" :data-cy="`${componentName}Title`">
        {{ title }}
      </h2>
    </template>
    <template #content>
      <div class="px-4">
        <div class="fa-stack skills-icon user-rank-stack flex flex-wrap align-items-center" v-if="!loading">
          <i :class="`${icon} fa-stack-2x watermark-icon`" />

          <div class="text-blue-600 dark:text-blue-800 fa-stack-1x">
            <slot name="userRanking" />
          </div>
        </div>
        <skills-spinner :is-loading="loading"/>
      </div>
    </template>
    <template #footer v-if="!isSummaryOnly">
      <div class="p-4">
        <router-link
            :to="route"
            :aria-label="`Click to navigate to ${title} page`"
            :data-cy="`${componentName}Btn`" tabindex="-1">
          <Button
              label="View"
              icon="far fa-eye"
              outlined class="w-full" size="small" />
        </router-link>
      </div>
    </template>
  </CardWithVericalSections>
</template>

<style>
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
}

.skills-progress-card .skills-icon.user-rank-stack i {
  opacity: 0.38;
}

.skills-progress-card .user-rank-text {
  font-size: 0.3em;
  line-height: 0.9em;
  background: rgba(255, 255, 255, 0.6);
  border-radius: 5px;
}
</style>