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

import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js';
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js';
import QuizRunService from '@/skills-display/components/quiz/QuizRunService.js';
import QuizRun from '@/skills-display/components/quiz/QuizRun.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue';
import AnimatedNumber from '@/skills-display/components/utilities/AnimatedNumber.vue';
import {useSkillsDisplayAttributesState} from "@/skills-display/stores/UseSkillsDisplayAttributesState.js";
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";

const props = defineProps({
  skill: {
    type: Object,
    default: null,
  },
})

const route = useRoute()
const skillsDisplayInfo = useSkillsDisplayInfo()
const skillState = useSkillsDisplaySubjectState()
const attributes = useSkillsDisplayAttributesState()
const pluralize = usePluralize()

const skillInternal = ref({});
const quizInfo = ref({});
const loadingSkillInfo = ref(true);
const loadingQuizInfo = ref(true);

const quizId = computed(() => {
  return route.params.quizId
})
const skillId = computed(() => {
  return route.params.skillId
})
const subjectId = computed(() => {
  return route.params.subjectId
})
const projectId = computed(() => {
  return route.params.projectId
})
const isSurveySkill = computed(() => {
  return skillInternal.value.selfReporting.type === 'Survey';
})
const isLoading = computed(() => {
  return loadingQuizInfo.value || loadingSkillInfo.value;
})

onMounted(() => {
  loadSkillInfo().then(() => {
    loadQuizInfo();
  });
})

const done = () => {
  if (route.params.crossProjectId) {
    if (route.params.badgeId) {
      // global badge cross-project skill
      skillsDisplayInfo.routerPush('globalBadgeSkillDetailsUnderAnotherProject',
          {
            badgeId: route.params.badgeId,
            crossProjectId: route.params.crossProjectId,
            dependentSkillId: route.params.skillId,
          }
      )
    } else {
      // learning path cross-project skill
      skillsDisplayInfo.routerPush('crossProjectSkillDetails',
          {
            subjectId: subjectId.value,
            skillId: skillId.value,
            crossProjectId: route.params.crossProjectId,
            dependentSkillId: route.params.skillId,
          }
      )
    }
  } else {
    skillsDisplayInfo.routerPush('skillDetails',
        {
          subjectId: subjectId.value,
          skillId: skillId.value,
        }
    )
  }

}
const loadSkillInfo = () => {
  loadingSkillInfo.value = true;
  return skillState.loadSkillSummary(skillId.value, route.params.crossProjectId, null)
      .then((res) => {
        skillInternal.value = res;
      }).finally(() => {
        loadingSkillInfo.value = false;
      });
}
const loadQuizInfo = () => {
  loadingQuizInfo.value = true;
  QuizRunService.getQuizInfo(quizId.value, skillId.value, projectId.value)
      .then((res) => {
        quizInfo.value = res;
      })
      .finally(() => {
        loadingQuizInfo.value = false;
      });
}

const associatedSkillNotCompleted = computed(() => skillInternal.value.points < skillInternal.value.totalPoints)
const pointsLabel = computed(() => pluralize.plural(attributes.pointDisplayName,  skillInternal.value.pointIncrement).toLowerCase())
</script>

<template>
  <div>
    <SkillsSpinner :is-loading="isLoading"/>
    <div v-if="!isLoading">
      <SkillsTitle>{{ quizInfo.quizType }}</SkillsTitle>
      <div class="text-left mt-4">
        <QuizRun :quiz-id="quizId"
                 :quiz="quizInfo"
                 :skillId="skillId"
                 :projectId="projectId"
                 :multipleTakes="quizInfo.multipleTakes || (skillInternal.expirationDate && skillInternal.daysOfInactivityBeforeExp <= 1)"
                 @testWasTaken="done"
                 @cancelled="done">
          <template #splashPageTitle v-if="associatedSkillNotCompleted">
            <div class="mb-6">
              <i class="fas fa-glass-cheers text-info skills-theme-quiz-correct-answer" style="font-size: 1.5rem;"></i> You will earn <Tag severity="success">
              <AnimatedNumber :num="skillInternal.pointIncrement"></AnimatedNumber></Tag> {{ pointsLabel }} for
              <span class="font-bold text-primary" style="font-size: 1.2rem">{{ skillInternal.skill }}</span>
              {{ attributes.skillDisplayNameLower }} by <span v-if="isSurveySkill">completing this survey</span><span v-else>passing this quiz</span>.
            </div>
          </template>
          <template #aboveTitleWhenPassed>
            <Message class="mb-6" severity="success" :closable="false" icon="fas fa-glass-cheers">
              Congrats!! You just earned <Tag severity="success">
              <AnimatedNumber :num="skillInternal.pointIncrement"></AnimatedNumber></Tag> {{ pointsLabel }} for
              <span class="font-bold" style="font-size: 1.2rem">{{ skillInternal.skill }}</span>
              {{ attributes.skillDisplayNameLower }} by <span v-if="isSurveySkill">completing the survey</span><span v-else>passing the quiz</span>.
            </Message>
          </template>
        </QuizRun>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>