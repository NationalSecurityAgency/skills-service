/*
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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