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
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useLog } from '@/components/utils/misc/useLog.js'

export const useSkillsDisplayBreadcrumbState = defineStore('skillsDisplayBreadcrumbState', () => {

  const router = useRouter()
  const skillsDisplayInfo = useSkillsDisplayInfo()
  const log = useLog()

  const breadcrumbItems = ref([])

  const navUpBreadcrumb = () => {
    const url = breadcrumbItems.value.length > 1 ? breadcrumbItems.value[breadcrumbItems.value.length - 2].contextUrl : skillsDisplayInfo.getRootUrl()
    log.trace(`navUpBreadcrumb: ${url}`)
    router.push({ path: url })
  }
  return {
    breadcrumbItems,
    navUpBreadcrumb
  }
})