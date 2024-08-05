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
import { computed, ref } from 'vue'
import { defineStore } from 'pinia'
import SkillsService from '@/components/skills/SkillsService'

export const useSkillsState = defineStore('skillsState',  () => {
  const skillState = ref(null);
  const loadingSkill = ref(true);

  function loadSkill(projectId, subjectId, skillId) {
    loadingSkill.value = true;
    return new Promise((resolve, reject) => {
      SkillsService.getSkillDetails(projectId, subjectId, skillId).then((response) => {
        skillState.value = Object.assign(response, { subjectId });
        resolve(response)
      }).catch((error) => reject(error)).finally(() => {
        loadingSkill.value = false;
      })
    })
  }
  const skill = computed(() => skillState.value)
  const setSkill = (skill) => {
    skillState.value = skill
  }

  return { skill, setSkill, loadingSkill, loadSkill };

})