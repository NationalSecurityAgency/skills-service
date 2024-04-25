<script setup>

import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'

defineProps(['date'])
const timeUtils = useTimeUtils()
</script>

<template>
  <div class="" data-cy="achievementDate">
    <div class="">
      <div class="flex">
        <Avatar icon="fa fa-medal" class="mr-2" size="large" />
        <div class="">
          <div>Achieved on <Tag severity="secondary" data-cy="achievementOn">{{ timeUtils.formatDate(date, 'MMMM Do YYYY') }}</Tag></div>
          <div class="text-color-secondary text-sm skills-theme-secondary-color" data-cy="timePassed">
            {{ timeUtils.relativeTime(date) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>