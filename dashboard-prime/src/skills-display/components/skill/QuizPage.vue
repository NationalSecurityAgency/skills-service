<script setup>

import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js';
import { useSkillsDisplaySubjectState } from '@/skills-display/stores/UseSkillsDisplaySubjectState.js';
import QuizRunService from '@/common-components/quiz/QuizRunService.js';
import QuizRun from '@/components/quiz/runs/QuizRun.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import SkillsTitle from '@/skills-display/components/utilities/SkillsTitle.vue';
import AnimatedNumber from '@/skills-display/components/utilities/AnimatedNumber.vue';

const props = defineProps({
  skill: {
    type: Object,
    default: null,
  },
})

const route = useRoute()
const skillsDisplayInfo = useSkillsDisplayInfo()
const skillState = useSkillsDisplaySubjectState()

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
const isSurveySkill = computed(() => {
  return skillInternal.value.selfReporting.type === 'Survey';
})
const isLoading = computed(() => {
  return loadingQuizInfo.value || loadingSkillInfo.value;
})

onMounted(() => {
  if (route.params.skill) {
    skillInternal.value = ({ ...route.params.skill });
    loadingSkillInfo.value = false;
    loadQuizInfo();
  } else {
    loadSkillInfo().then(() => {
      loadQuizInfo();
    });
  }
})

const done = () => {
  skillsDisplayInfo.routerPush('skillDetails',
      {
        subjectId: subjectId.value,
        skillId: skillId.value,
      }
  )
}
const loadSkillInfo = () => {
  loadingSkillInfo.value = true;
  return skillState.loadSkillSummary(skillId.value, null, null)
      .then((res) => {
        skillInternal.value = res;
      }).finally(() => {
        loadingSkillInfo.value = false;
      });
}
const loadQuizInfo = () => {
  loadingQuizInfo.value = true;
  QuizRunService.getQuizInfo(quizId.value)
      .then((res) => {
        quizInfo.value = res;
      })
      .finally(() => {
        loadingQuizInfo.value = false;
      });
}
</script>

<template>
  <div>
    <SkillsSpinner :is-loading="isLoading"/>
    <div v-if="!isLoading">
      <SkillsTitle>{{ quizInfo.quizType }}</SkillsTitle>
      <div class="text-left mt-3">
        <QuizRun :quiz-id="quizId"
                 :quiz="quizInfo"
                 @testWasTaken="done"
                 @cancelled="done">
          <template #splashPageTitle>
            <div class="mb-4">
              <i class="fas fa-glass-cheers text-info skills-theme-quiz-correct-answer" style="font-size: 1.5rem;"></i> You will earn <Tag severity="success">
              <AnimatedNumber :num="skillInternal.pointIncrement"></AnimatedNumber></Tag> points for
              <span class="font-bold text-primary" style="font-size: 1.2rem">{{ skillInternal.skill }}</span>
              skill by <span v-if="isSurveySkill">completing this survey</span><span v-else>passing this quiz</span>.
            </div>
          </template>
          <template #completeAboveTitle>
            <div class="mb-4">
              <i class="fas fa-glass-cheers text-info skills-theme-quiz-correct-answer" style="font-size: 1.5rem;"></i> Congrats!! You just earned <Tag severity="success">
              <AnimatedNumber :num="skillInternal.pointIncrement"></AnimatedNumber></Tag> points for
              <span class="font-bold text-primary" style="font-size: 1.2rem">{{ skillInternal.skill }}</span>
              skill by <span v-if="isSurveySkill">completing the survey</span><span v-else>passing the quiz</span>.
            </div>
          </template>
        </QuizRun>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>