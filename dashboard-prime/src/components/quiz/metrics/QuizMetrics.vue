<script setup>

import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router'
import QuizService from '@/components/quiz/QuizService.js';
import NoContent2 from "@/components/utils/NoContent2.vue";

const route = useRoute();
const isLoading = ref(true);
const quizId = ref(route.params.quizId);
const metrics = ref(null);

const isSurvey = computed(() => metrics.value && metrics.value.quizType === 'Survey');
const hasMetrics = computed(() => metrics.value && metrics.value.numTaken > 0);

onMounted(() => {
  isLoading.value = true;
  QuizService.getQuizMetrics(quizId.value)
      .then((res) => {
        metrics.value = res;
      })
      .finally(() => {
        isLoading.value = false;
      });

})
</script>

<template>
  <div>
    <SubPageHeader title="Results"
                   aria-label="results">
    </SubPageHeader>

    <SkillsSpinner :is-loading="isLoading"/>

    <Card :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
      <template #content>
        <NoContent2 v-if="!hasMetrics && !isLoading"
                    title="No Results Yet..."
                    class="my-5 py-5"
                    :message="`Results will be available once at least 1 ${metrics.quizType} is completed`"
                    data-cy="noMetricsYet"/>
      </template>
    </Card>
    <div v-if="hasMetrics">
      Has Metrics!
    </div>
  </div>
</template>

<style scoped>

</style>