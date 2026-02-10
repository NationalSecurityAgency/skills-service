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
import Ribbon from '@/skills-display/components/subjects/Ribbon.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import CardWithVericalSections from '@/components/utils/cards/CardWithVericalSections.vue'
import LevelsProgress from '@/skills-display/components/utilities/LevelsProgress.vue'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

const props = defineProps({
  subject: {
    type: Object,
    required: true
  },
  tileIndex: {
    type: Number,
    required: true
  }
})

const attributes = useSkillsDisplayAttributesState()
const skillsDisplayInfo = useSkillsDisplayInfo()
const numFormat = useNumberFormat()
const ribbonColor = ['#4472ba', '#c74a41', '#44843E', '#BE5A09', '#A15E9A', '#23806A'][props.tileIndex % 6]
const themeHelper = useThemesHelper()

const progress = computed(() => {
  let levelBeforeToday = 0
  if (props.subject.levelPoints > props.subject.todaysPoints) {
    levelBeforeToday = ((props.subject.levelPoints - props.subject.todaysPoints) / props.subject.levelTotalPoints) * 100
  } else {
    levelBeforeToday = (props.subject.levelPoints / props.subject.levelTotalPoints) * 100
  }


  let level = 0
  if (props.subject.totalPoints > 0) {
    if (props.subject.levelTotalPoints === -1) {
      level = 100
    } else {
      level = (props.subject.levelPoints / props.subject.levelTotalPoints) * 100
    }
  }

  return {
    total: props.subject.totalPoints > 0 ? (props.subject.points / props.subject.totalPoints) * 100 : 0,
    totalBeforeToday: props.subject.totalPoints > 0 ? ((props.subject.points - props.subject.todaysPoints) / props.subject.totalPoints) * 100 : 0,
    level,
    levelBeforeToday,
    allLevelsComplete: props.subject.totalPoints > 0 && props.subject.levelTotalPoints < 0
  }
})

const activePointsColor = computed(() => {
  return themeHelper.isDarkTheme ? 'text-orange-500' : 'text-orange-700'
})
</script>

<template>
  <div data-cy="subjectTile" class="h-full">
  <CardWithVericalSections class="h-full text-center" :data-cy="`subjectTile-${subject.subjectId}`">
    <template #content>
      <div class="px-4 pt-4">
        <ribbon :color="ribbonColor" class="subject-tile-ribbon">
          <div class="flex justify-center">
            <h3 class="overflow-hidden text-ellipsis text-center max-w-[20rem]">{{ subject.subject }}</h3>
          </div>
        </ribbon>
      </div>
    </template>
    <template #footer>
      <div  class="px-4 pb-4 pt-2">
        <i class="text-7xl! text-surface-500 dark:text-surface-300 sd-theme-subject-tile-icon" :class="subject.iconClass" aria-hidden="true"/> 
        <div class="text-xl pt-1 font-medium" data-cy="levelTitle">{{ attributes.levelDisplayName }} {{ subject.skillsLevel }}</div>
        <div class="flex justify-center pt-2 pb-2 subject-progress-stars-icons">
          <LevelsProgress :level="subject.skillsLevel" :totalLevels="subject.totalLevels" data-cy="subjectStars"/>
        </div>

        <div class="flex mt-2">
          <div class="flex-1 text-left">
            <div class="skill-label" style="min-width: 5rem;">Overall</div>
          </div>
          <div class="">
            <div class="skill-label text-right" data-cy="pointsProgress">
              <span :class="activePointsColor" class="font-medium sd-theme-primary-color">{{ numFormat.pretty(subject.points) }}</span> /
              {{ numFormat.pretty(subject.totalPoints) }}
            </div>
          </div>
        </div>
        <div>
          <vertical-progress-bar
            :total-progress="progress.total"
            :aria-label="`Overall progress for ${subject.subject}`"
            :total-progress-before-today="progress.totalBeforeToday"
          />
        </div>

        <div class=" mt-6">
          <div class="flex">
            <div v-if="!progress.allLevelsComplete" class="flex-1 text-left">
              <div class="skill-label" style="min-width: 10rem;">Next {{ attributes.levelDisplayName
                }}</div>
            </div>
            <div v-if="!progress.allLevelsComplete" data-cy="levelProgress">
                <span :class="activePointsColor" class="font-medium sd-theme-primary-color">{{ numFormat.pretty(subject.levelPoints) }}</span> /
                {{ numFormat.pretty(subject.levelTotalPoints) }}
            </div>
          </div>
          <div v-if="progress.allLevelsComplete" data-cy="allLevelsComplete">
            <div class="skill-label text-center uppercase"><i class="fas fa-check text-green-800" /> All
              {{ attributes.levelDisplayName.toLowerCase() }}s complete</div>
          </div>
          <div class="">
            <vertical-progress-bar
              :aria-label="`Level progress for ${subject.subject}`"
              :total-progress="progress.level || 0"
              :total-progress-before-today="progress.levelBeforeToday || 0"
            />
          </div>
        </div>
        <div class="pt-6" >
          <router-link v-if="!attributes.isSummaryOnly"
            :to="{ name: skillsDisplayInfo.getContextSpecificRouteName('SubjectDetailsPage'), params: { subjectId: subject.subjectId } }"
            :aria-label="`Click to navigate to the ${subject.subject} ${attributes.subjectDisplayName} page.`"
            data-cy="subjectTileBtn" tabindex="-1">
            <Button
              label="View"
              icon="far fa-eye"
              outlined class="w-full" size="small" />
          </router-link>
        </div>
      </div>
    </template>
  </CardWithVericalSections>
  </div>
</template>

<style>
.subject-progress-stars-icons .p-rating-icon {
  width: 1.2rem;
  height: 1.2rem;
}
</style>

<style scoped>

</style>