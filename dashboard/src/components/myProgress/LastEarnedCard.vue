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
import { computed } from 'vue'
import MyProgressInfoCardUtil from '@/components/myProgress/MyProgressInfoCardUtil.vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import { useTimeUtils } from '@/common-components/utilities/UseTimeUtils.js'
import dayjs from '@/common-components/DayJsCustomizer'

const myProgressState = useMyProgressState()
const myProgress = computed(() => myProgressState.myProgress)
const timeUtils = useTimeUtils()

const isWithinOneWeek = (timestamp) => {
  return dayjs(timestamp).isAfter(dayjs().subtract(7, 'day'))
}

const getFooterText = () => {
  if (myProgress.value.mostRecentAchievedSkill !== null) {
    if (isWithinOneWeek(myProgress.value.mostRecentAchievedSkill)) {
      return 'Keep up the good work!!'
    }
    return 'It\'s been a while, perhaps earn another skill?'
  }
  return 'No skills achieved yet, time to get started!'
}
</script>

<template>
  <my-progress-info-card-util title="Earned">
    <template #left-content>
      <i class="fas fa-calendar-alt mt-1 mb-2 text-bluegray-600" style="font-size: 5rem;" />
    </template>
    <template #right-content>
      <div class="pt-2 pr-3 text-center sm:text-left w-min-15rem">
        <div v-if="myProgress.mostRecentAchievedSkill !== null" data-cy="mostRecentAchievedSkill">
          <span>Last Achieved skill</span>
          <Tag severity="success" class="ml-2">{{ timeUtils.timeFromNow(myProgress.mostRecentAchievedSkill) }}</Tag>
        </div>
        <div class="my-2" data-cy="numAchievedSkillsLastWeek">
          <Tag severity="info" style="font-size: 1rem;">{{ myProgress.numAchievedSkillsLastWeek }}</Tag> skills
          in the last week
        </div>
        <div class="my-2" data-cy="numAchievedSkillsLastMonth">
          <Tag severity="info" style="font-size: 1rem;">{{ myProgress.numAchievedSkillsLastMonth }}</Tag> skills
          in the last month
        </div>
      </div>
    </template>
    <template #footer>
      <span data-cy="last-earned-footer" class="w-min-10rem">{{ getFooterText() }}</span>
    </template>
  </my-progress-info-card-util>
</template>

<style scoped>

</style>