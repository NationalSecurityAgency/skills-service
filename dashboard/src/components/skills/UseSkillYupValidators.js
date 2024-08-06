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
import { useDebounceFn } from '@vueuse/core'
import SkillsService from '@/components/skills/SkillsService.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useRoute } from 'vue-router'

export const useSkillYupValidators = () => {

  const appConfig = useAppConfig()
  const route = useRoute()

  const checkSkillNameUnique = useDebounceFn((value, origValue, isEdit) => {
    if (!value || value.length === 0) {
      return true
    }
    if (isEdit && (origValue === value || origValue.localeCompare(value, 'en', { sensitivity: 'base' }) === 0)) {
      return true
    }
    return SkillsService.skillWithNameExists(route.params.projectId, value).then((remoteRes) => remoteRes)
  }, appConfig.formFieldDebounceInMs)

  const checkSkillIdUnique = useDebounceFn((value, origValue, isEdit) => {
    if (!value || value.length === 0 || (isEdit && origValue === value)) {
      return true
    }
    return SkillsService.skillWithIdExists(route.params.projectId, value)
      .then((remoteRes) => remoteRes)

  }, appConfig.formFieldDebounceInMs)

  return {
    checkSkillNameUnique,
    checkSkillIdUnique
  }
}