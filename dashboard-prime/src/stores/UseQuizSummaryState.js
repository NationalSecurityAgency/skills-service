import { ref } from 'vue'
import { defineStore } from 'pinia'
import QuizService from '@/components/quiz/QuizService.js';

export const useQuizSummaryState = defineStore('quizSummaryState', () => {
  const quizSummary = ref(null);
  const loadingQuizSummary = ref(false)

  function loadQuizSummary(quizId) {
    return new Promise((resolve, reject) => {
      loadingQuizSummary.value = true
      QuizService.getQuizDefSummary(quizId)
        .then((response) => {
          quizSummary.value = response;
          resolve(response);
        })
        .catch((error) => reject(error))
        .finally(() => {
          loadingQuizSummary.value = false
        });
    });
  }

  return {
    quizSummary,
    loadQuizSummary,
    loadingQuizSummary
  }
});