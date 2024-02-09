import { ref, computed } from 'vue'
import { defineStore } from 'pinia'
import QuizService from '@/components/quiz/QuizService.js';
import UserRolesUtil from '@/components/utils/UserRolesUtil.js'

export const useQuizConfig = defineStore('quizConfig', () => {
  const quizConfig = ref(null)
  const loadingQuizConfig = ref(false)

  function loadQuizConfigState(params) {
    const { quizId } = params
    const { updateLoadingVar = true } = params
    return new Promise((resolve, reject) => {
      if (updateLoadingVar) {
        loadingQuizConfig.value = true
      }
      QuizService.getQuizSettings(quizId)
        .then((response) => {
          if (response && typeof response.reduce === 'function') {
            const newQuizConfig = response.reduce((map, obj) => {
              // eslint-disable-next-line no-param-reassign
              map[obj.setting] = obj.value
              return map
            }, {})
            quizConfig.value = newQuizConfig
          }
          resolve(response)
        })
        .finally(() => {
          if (updateLoadingVar) {
            loadingQuizConfig.value = false
          }
        })
        .catch((error) => reject(error))
    })
  }

  function afterQuizConfigStateLoaded({ state }) {
    return new Promise((resolve) => {
      (function waitForQuizConfig() {
        if (!state.loadingQuizConfig) return resolve(state.quizConfig)
        setTimeout(waitForQuizConfig, 100)
        return state.quizConfig
      }())
    })
  }

  const isReadOnlyQuiz = computed(() => {
    return quizConfig.value && UserRolesUtil.isQuizReadOnlyRole(quizConfig.value.quizUserRole);
  });

  const userQuizRole = computed(() => {
    return quizConfig.value && quizConfig.value.quizUserRole;
  });

  return {
    quizConfig,
    loadingQuizConfig,
    loadQuizConfigState,
    afterQuizConfigStateLoaded,
    isReadOnlyQuiz,
    userQuizRole
  }
})