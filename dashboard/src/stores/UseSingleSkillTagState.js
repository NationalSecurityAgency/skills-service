/*
 * Copyright 2026 SkillTree
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
import {computed, ref} from 'vue'
import {defineStore} from 'pinia'
import SkillsService from '@/components/skills/SkillsService'

export const useSingleSkillTagState = defineStore('singleTagState', () => {
    const skillTagState = ref(null);
    const loadingSkillTag = ref(true);

    function loadSkillTagInfo(projectId, tagId) {
        loadingSkillTag.value = true;
        return new Promise((resolve, reject) => {
            SkillsService.getTagInfo(projectId, tagId).then((response) => {
                skillTagState.value = response
                resolve(response)
            }).catch((error) => reject(error))
                .finally(() => {
                    loadingSkillTag.value = false;
                })
        })
    }

    const skillTag = computed(() => skillTagState.value)
    const setSkillTag = (skillTag) => {
        skillTagState.value = skillTag
    }

    const skills = computed(() => skillTag.value?.skills)
    const hasSkills = computed(() => skills.value?.length > 0)
    const numSkills = computed(() => skills.value?.length || 0)

    return {
        skillTag,
        setSkillTag,
        loadingSkillTag,
        loadSkillTagInfo,
        hasSkills,
        skills,
        numSkills,
    };

})