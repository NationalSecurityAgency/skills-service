<script setup>

import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'

const timeUtils = useTimeUtils();

const props = defineProps({
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
  <Card :pt="{ body: { class: 'p-2' }, content: { class: 'p-2' } }">
    <template #content>
      <div class="grid w-full">
        <div class="col">
          <div class="uppercase font-light card-title text-sm mb-1">{{ title }}</div>
          <slot name="card-value">
            <span class="text-2xl font-bold mb-0" data-cy="statCardValue">
              <span v-if="calculateTimeFromNow"><span v-if="statNum">{{ timeUtils.timeFromNow(statNum) }}</span><span v-else>Never</span></span>
              <span v-else>{{ statNum }}</span>
            </span>
          </slot>
        </div>
        <div class="col-auto">
          <i :class="icon" style="font-size: 2.2rem;"/>
        </div>
      </div>
      <p class="font-light text-sm mt-3 mb-0" data-cy="statCardDescription">
        <slot />
      </p>
    </template>
  </Card>
</template>

<style scoped>

</style>