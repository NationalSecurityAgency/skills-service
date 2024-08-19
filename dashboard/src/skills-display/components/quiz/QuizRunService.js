/*
 * Copyright 2020 SkillTree
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
import axios from 'axios';

export default {

  userId: new URLSearchParams(window.location.search).get('userId'),

  getUserIdParams() {
    if (!this.userId) {
      return {};
    }

    if (typeof this.userId === 'string') {
      return { userId: this.userId };
    }
    return {
      userId: this.userId.id,
      idType: this.userId.idType,
    };
  },

  getQuizInfo(quizId) {
    return axios.get(`/api/quizzes/${quizId}`, { params: this.getUserIdParams() })
      .then((response) => response.data);
  },
  startQuizAttempt(quizId, skillId, projectId) {
    return axios.post(`/api/quizzes/${quizId}/attempt`, { ...this.getUserIdParams(), skillId, projectId })
        .then((response) => response.data);
  },
  reportAnswer(quizId, attemptId, answerId, isSelected, answerText = null) {
    return axios.post(`/api/quizzes/${quizId}/attempt/${attemptId}/answers/${answerId}`, { ...this.getUserIdParams(), isSelected, answerText })
        .then((response) => response.data);
  },
  completeQuizAttempt(quizId, attemptId) {
    return axios.post(`/api/quizzes/${quizId}/attempt/${attemptId}/complete`, { ...this.getUserIdParams() })
        .then((response) => response.data);
  },
  failQuizAttempt(quizId, attemptId) {
    return axios.post(`/api/quizzes/${quizId}/attempt/${attemptId}/fail`, { ...this.getUserIdParams() })
        .then((response) => response.data);
  },
  validateDescription(description) {
    return axios.post('/api/validation/description', { value: description })
        .then((response) => response.data);
  },

  setUserId(userId) {
    this.userId = userId;
  },
};
