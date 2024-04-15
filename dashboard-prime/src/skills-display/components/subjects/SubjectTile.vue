<script setup>
import { computed } from 'vue'
import Ribbon from '@/skills-display/components/subjects/Ribbon.vue'
import { useSkillsDisplayPreferencesState } from '@/skills-display/stores/UseSkillsDisplayPreferencesState.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'

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

const preferencesState = useSkillsDisplayPreferencesState()
const numFormat = useNumberFormat()
const ribbonColor = ['#4472ba', '#c74a41', '#44843E', '#BE5A09', '#A15E9A', '#23806A'][props.tileIndex % 6]

const progress = computed(() => {
  let levelBeforeToday = 0
  if (props.subject.levelPoints > props.subject.todaysPoints) {
    levelBeforeToday = ((props.subject.levelPoints - props.subject.todaysPoints) / props.subject.levelTotalPoints) * 100
  } else {
    levelBeforeToday = 0
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
</script>

<template>
  <Card class="h-full text-center" data-cy="subjectTile">
    <template #content>
      <ribbon :color="ribbonColor" class="subject-tile-ribbon">
        {{ subject.subject }}
      </ribbon>

      <i :class="subject.iconClass" class="text-7xl text-400" />
      <div class="text-xl pt-1 font-medium">{{ preferencesState.levelDisplayName }} {{ subject.skillsLevel }}</div>
      <div class="flex justify-content-center mt-2 subject-progress-stars-icons">
        <Rating v-model="subject.skillsLevel" :stars="subject.totalLevels" readonly :cancel="false" />
      </div>

      <div class="flex mt-2">
        <div class="flex-1 text-left">
          <label class="skill-label" style="min-width: 5rem;">Overall</label>
        </div>
        <div class="">
          <label class="skill-label text-right">
            {{ numFormat.pretty(subject.points) }} / {{ numFormat.pretty(subject.totalPoints) }}
          </label>
        </div>
      </div>
      <div>
        <vertical-progress-bar
          :total-progress="progress.total"
          :total-progress-before-today="progress.totalBeforeToday"
        />
      </div>

      <div class=" mt-4">
        <div class="flex">
          <div v-if="!progress.allLevelsComplete" class="flex-1 text-left">
            <label class="skill-label" style="min-width: 10rem;">Next {{ preferencesState.levelDisplayName
              }}</label>
          </div>
          <div v-if="!progress.allLevelsComplete" class="">
            <label class="skill-label">
              {{ subject.levelPoints | number }} / {{ subject.levelTotalPoints | number }}
            </label>
          </div>
        </div>
        <div v-if="progress.allLevelsComplete" class="">
          <label class="skill-label text-center text-uppercase"><i class="fas fa-check text-success" /> All
            {{ preferencesState.levelDisplayName.toLowerCase() }}s complete</label>
        </div>
        <div class="">
          <vertical-progress-bar
            :total-progress="progress.level"
            :total-progress-before-today="progress.levelBeforeToday"
          />
          <!--          <progress-bar-->
          <!--            v-if="progress.allLevelsComplete"-->
          <!--            :val="progress.level"-->
          <!--            :size="18"-->
          <!--            :bar-color="completeColor"-->
          <!--            class="progress-border"/>-->
          <!--          <vertical-progress-bar v-else-->
          <!--                                 :before-today-bar-color="beforeTodayColor"-->
          <!--                                 :total-progress-bar-color="earnedTodayColor"-->
          <!--                                 :total-progress="progress.level"-->
          <!--                                 :total-progress-before-today="progress.levelBeforeToday"/>-->
        </div>
      </div>
    </template>
    <template #footer>
      <router-link
        :to="{ name:'SubjectDetailsPage', params: { subjectId: subject.subjectId } }"
        :aria-label="`Click to navigate to the ${subject.subject} subject page.`"
        data-cy="subjectTileBtn">
        <Button
          label="View"
          icon="far fa-eye"
          outlined class="w-full" size="small" />
      </router-link>
    </template>
  </Card>
</template>

<style>
.subject-progress-stars-icons .p-rating-icon {
  width: 1.2rem;
  height: 1.2rem;
}
</style>

<style scoped>

</style>