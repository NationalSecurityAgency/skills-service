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
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useSkillsDisplayThemeState } from '@/skills-display/stores/UseSkillsDisplayThemeState.js'
import MediaInfoCard from '@/components/utils/cards/MediaInfoCard.vue'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js'
import SelfReportType from "@/components/skills/selfReport/SelfReportType.js";
import QuizType from "@/skills-display/components/quiz/QuizType.js";
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";

const props = defineProps({
  skill: Object,
  shortSubTitles: {
    type: Boolean,
    default: false,
  },
})
const numFormat = useNumberFormat()
const pluralize = usePluralize()
const themeState = useSkillsDisplayThemeState()
const attributes = useSkillsDisplayAttributesState()

const timeWindowTitle= computed(() => props.skill.pointIncrement * props.skill.maxOccurrencesWithinIncrementInterval)
const isTimeWindowDisabled = computed(() => props.skill.pointIncrementInterval <= 0 || props.skill.pointIncrement === props.skill.totalPoints)
const timeWindowLabel= computed(() => {
const hours = props.skill.pointIncrementInterval > 59 ? Math.floor(props.skill.pointIncrementInterval / 60) : 0;
  const minutes = props.skill.pointIncrementInterval > 60 ? props.skill.pointIncrementInterval % 60 : props.skill.pointIncrementInterval;
  const occur = props.skill.maxOccurrencesWithinIncrementInterval;
  const points = occur * props.skill.pointIncrement;
  let res = `Up-to ${numFormat.pretty(points)} ${attributes.pointDisplayNamePlural.toLowerCase()} within `;
  if (hours) {
    res = `${res} ${hours} ${pluralize.plural('hr', hours)}`;
  }
  if (minutes) {
    if (hours) {
      res = ` ${res} and`;
    }
    res = `${res} ${minutes} ${pluralize.plural('min', minutes)}`;
  }
  return res;
})


</script>

<template>
  <div class="flex flex-wrap gap-4 sd-theme-summary-cards flex-col md:flex-row" data-cy="skillsSummaryCards">
    <div class="flex-1">
      <media-info-card
        :title="`${numFormat.pretty(skill.points)} Total`"
        class="h-full w-min-13rem"
        icon-class="fa fa-running"
        :icon-color="themeState.infoCards().iconColors[0]"
        :add-border="true"
        data-cy="overallPointsEarnedCard">
        <Tag>Overall</Tag> {{ attributes.pointDisplayNamePlural }} Earned
      </media-info-card>
    </div>

    <div class="flex-1">
      <media-info-card
        :title="`${numFormat.pretty(skill.todaysPoints)} Today`"
        class="h-full sm:w-min-13rem"
        icon-class="far fa-clock"
        :icon-color="themeState.infoCards().iconColors[1]"
        :add-border="true"
        data-cy="pointsAchievedTodayCard">
        {{ attributes.pointDisplayNamePlural }} Achieved <Tag>Today</Tag>
      </media-info-card>
    </div>

    <div v-if="!SelfReportType.isQuizOrSurvey(skill.selfReporting?.type)" class="flex-1">
      <media-info-card
        :title="`${numFormat.pretty(skill.pointIncrement)} Increment`"
        class="h-full sm:w-min-13rem"
        icon-class="fas fa-flag-checkered"
        :icon-color="themeState.infoCards().iconColors[2]"
        :add-border="true"
        data-cy="pointsPerOccurrenceCard">
        {{ attributes.pointDisplayNamePlural }} per Occurrence
      </media-info-card>
    </div>

    <div v-if="SelfReportType.isQuizOrSurvey(skill.selfReporting?.type)" class="flex-1">
      <media-info-card
          title="Quiz Requirement"
          class="h-full sm:w-min-13rem"
          icon-class="fas fa-spell-check"
          :icon-color="themeState.infoCards().iconColors[2]"
          :add-border="true"
          data-cy="quizRequirementCard">
        <div v-if="QuizType.isQuiz(skill.selfReporting.type)">
          <div v-if="skill.selfReporting.quizOrSurveyPassed">
            You passed <b>{{ skill.selfReporting.quizName }}</b> quiz. Well done!
          </div>
          <div v-else>
            Pass <b>{{ skill.selfReporting.quizName }}</b> quiz to earn the {{ attributes.skillDisplayNameLower }}
          </div>
        </div>

        <div v-if="QuizType.isSurvey(skill.selfReporting.type)">
          <div v-if="skill.selfReporting.quizOrSurveyPassed">
            You completed <b>{{ skill.selfReporting.quizName }}</b> survey. Well done!
          </div>
          <div v-else>
            Complete <b>{{ skill.selfReporting.quizName }}</b> survey to earn the {{ attributes.skillDisplayNameLower }}
          </div>
        </div>

      </media-info-card>
    </div>

    <div v-if="!isTimeWindowDisabled" class="flex-1">
      <media-info-card
        :title="`${timeWindowTitle} Limit`"
        class="h-full sm:w-min-13rem"
        icon-class="fas fa-hourglass-half"
        :icon-color="themeState.infoCards().iconColors[3]"
        :add-border="true"
        data-cy="timeWindowPts">
        {{ timeWindowLabel }}
      </media-info-card>
    </div>

  </div>
</template>

<style scoped>

</style>