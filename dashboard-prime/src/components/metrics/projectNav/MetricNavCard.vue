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
import CardWithVericalSections from '@/components/utils/cards/CardWithVericalSections.vue'

defineProps(['title', 'subtitle', 'description', 'icon', 'pathName']);
</script>

<template>
  <CardWithVericalSections>
    <template #content>
      <div class="p-5">
        <div class="flex">
          <div class="flex d-inline-block mt-1 mr-3 border rounded p-2">
            <i class="fa-3x" :class="icon"/>
          </div>
          <div class="flex flex-1 flex-column">
            <div class="h4 mb-2 font-semibold">{{ title }}</div>
            <div class="font-light text-sm">
              <slot name="subtitle">
                {{ subtitle }}
              </slot>
            </div>
          </div>
        </div>
        <p class="mt-2">
          <small>{{ description }}</small>
        </p>
      </div>

    </template>
    <template #footer>
      <div class="text-center pb-3 px-5">
        <router-link :to="{ name: pathName }" :data-cy="`metricsNav-${title}`" tabindex="-1">
          <SkillsButton size="small" :label="`${title}`" icon="fas fa-arrow-right" />
        </router-link>
      </div>
    </template>
  </CardWithVericalSections>
</template>

<style scoped>

</style>