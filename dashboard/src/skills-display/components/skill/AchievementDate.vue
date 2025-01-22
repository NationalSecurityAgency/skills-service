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
          <div class="text-muted-color text-sm skills-theme-secondary-color" data-cy="timePassed">
            {{ timeUtils.relativeTime(date) }}
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>