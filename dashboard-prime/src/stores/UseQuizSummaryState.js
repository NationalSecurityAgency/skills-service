import { ref } from 'vue'
import { defineStore } from 'pinia'
import QuizService from '@/components/quiz/QuizService.js';

export const useQuizSummaryState = defineStore('quizSummaryState', () => {
  const quizSummary = ref(null);
  const loadingQuizSummary = ref(true)

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

  const afterQuizSummaryLoaded = () => {
    return new Promise((resolve) => {
      (function waitForQuizSummary() {
        if (!loadingQuizSummary.value) return resolve(quizSummary.value);
        setTimeout(waitForQuizSummary, 100);
        return quizSummary.value;
      }());
    });
  }

  return {
    quizSummary,
    loadQuizSummary,
    loadingQuizSummary,
    afterQuizSummaryLoaded
  }
});