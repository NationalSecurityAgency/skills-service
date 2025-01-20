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
import { useProjectInfo } from '@/common-components/stores/UseCurrentProjectInfo.js'
import { useQuizInfo } from '@/common-components/stores/UseCurrentQuizInfo.js';

export const useDescriptionValidatorService = () => {

  const projectInfo = useProjectInfo()
  const quizInfo = useQuizInfo()

  const validateDescription = (description, enableProjectIdParam = true, useProtectedCommunityValidator = null, enableQuizIdParam = true) => {
    const body = {
      value: description,
      projectId: enableProjectIdParam ? projectInfo.currentProjectId : null,
      useProtectedCommunityValidator,
      quizId: enableQuizIdParam ? quizInfo.currentQuizId : null
    };
    return axios.post('/api/validation/description', body).then((result) => result.data);
  }

  return {
    validateDescription
  }
};
