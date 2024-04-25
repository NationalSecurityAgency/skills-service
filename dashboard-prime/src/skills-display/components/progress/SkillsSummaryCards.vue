<script setup>
import { computed } from 'vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useLanguagePluralSupport } from '@/components/utils/misc/UseLanguagePluralSupport.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'

const props = defineProps({
  skill: Object,
  shortSubTitles: {
    type: Boolean,
    default: false,
  },
})
const numFormat = useNumberFormat()
const pluralSupport = useLanguagePluralSupport()
const themeState = useSkillsDisplayThemeState()

const timeWindowTitle= computed(() => props.skill.pointIncrement * props.skill.maxOccurrencesWithinIncrementInterval)
const isTimeWindowDisabled = computed(() => props.skill.pointIncrementInterval <= 0 || props.skill.pointIncrement === props.skill.totalPoints)
const timeWindowLabel= computed(() => {
const hours = props.skill.pointIncrementInterval > 59 ? Math.floor(props.skill.pointIncrementInterval / 60) : 0;
  const minutes = props.skill.pointIncrementInterval > 60 ? props.skill.pointIncrementInterval % 60 : props.skill.pointIncrementInterval;
  const occur = props.skill.maxOccurrencesWithinIncrementInterval;
  const points = occur * props.skill.pointIncrement;
  let res = `Up-to ${numFormat.pretty(points)} points within `;
  if (hours) {
    res = `${res} ${hours} hr${pluralSupport.sOrNone(hours)}`;
  }
  if (minutes) {
    if (hours) {
      res = ` ${res} and`;
    }
    res = `${res} ${minutes} min${pluralSupport.sOrNone(minutes)}`;
  }
  return res;
})


</script>

<template>
  <div class="flex flex-wrap gap-3" data-cy="skillsSummaryCards">
    <div class="flex-1">
      <media-info-card
        :title="`${numFormat.pretty(skill.points)} Total`"
        class="h-full w-min-13rem"
        icon-class="fa fa-running"
        :icon-color="themeState.infoCards().iconColors[0]"
        data-cy="overallPointsEarnedCard">
        <Tag>Overall</Tag> Points Earned
      </media-info-card>
    </div>

    <div class="flex-1">
      <media-info-card
        :title="`${numFormat.pretty(skill.todaysPoints)} Today`"
        class="h-full w-min-13rem"
        icon-class="fa fa-clock"
        :icon-color="themeState.infoCards().iconColors[1]"
        data-cy="pointsAchievedTodayCard">
        Points Achieved <Tag>Today</Tag>
      </media-info-card>
    </div>

    <div class="flex-1">
      <media-info-card
        :title="`${numFormat.pretty(skill.pointIncrement)} Increment`"
        class="h-full w-min-13rem"
        icon-class="fas fa-flag-checkered"
        :icon-color="themeState.infoCards().iconColors[2]"
        data-cy="pointsPerOccurrenceCard">
        Points per Occurrence
      </media-info-card>
    </div>

    <div v-if="!isTimeWindowDisabled" class="flex-1">
      <media-info-card
        :title="`${timeWindowTitle} Limit`"
        class="h-full w-min-13rem"
        icon-class="fas fa-hourglass-half"
        :icon-color="themeState.infoCards().iconColors[3]"
        data-cy="timeWindowPts">
        {{ timeWindowLabel }}
      </media-info-card>
    </div>

  </div>
</template>

<style scoped>

</style>