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

import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import NumberFormatter from "@/components/utils/NumberFormatter.js";
import CardWithVericalSections from '@/components/utils/cards/CardWithVericalSections.vue'

const timeUtils = useTimeUtils();

defineProps({
  title: {
    type: String,
    required: true,
  },
  statNum: {
    type: Number,
    required: false,
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
})

</script>

<template>
  <CardWithVericalSections :pt="{ body: { class: 'p-2' }, content: { class: 'p-2' } }">
    <template #content>
      <div class="grid w-full">
        <div class="col">
          <div class="uppercase font-light card-title text-sm mb-1">{{ title }}</div>
          <slot name="card-value">
            <span class="text-2xl font-bold mb-0" data-cy="statCardValue">
              <span v-if="calculateTimeFromNow"><span v-if="statNum">{{ timeUtils.timeFromNow(statNum) }}</span><span v-else>Never</span></span>
              <span v-else>{{ NumberFormatter.format(statNum) }}</span>
            </span>
          </slot>
        </div>
        <div class="col-auto">
          <i :class="icon" style="font-size: 2.2rem;"/>
        </div>
      </div>

    </template>
    <template #footer>
      <p class="font-light text-sm mt-3 mb-0" data-cy="statCardDescription">
        <slot />
      </p>
    </template>
  </CardWithVericalSections>
</template>

<style scoped>

</style>