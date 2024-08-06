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
import { ref, nextTick } from 'vue'
import { defineStore } from 'pinia'
import { useSkillsDisplayParentFrameState } from '@/skills-display/stores/UseSkillsDisplayParentFrameState.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js'
import { useLog } from '@/components/utils/misc/useLog.js'

export const useScrollSkillsIntoViewState = defineStore('scrollSkillsIntoViewState', () => {

  const jumpToLastViewed = ref(false)
  const lastViewedSkillId = ref(null)

  const parentFrame = useSkillsDisplayParentFrameState()
  const skillsDisplayInfo = useSkillsDisplayInfo()
  const log = useLog()

  const setLastViewedSkillId = (skillId) => {
    lastViewedSkillId.value = skillId
  }

  const isScrollFeatureSupported = () => {
    if (!skillsDisplayInfo.isSkillsClientPath()) {
      return true
    }
    const opts = parentFrame.options
    return opts && Object.keys(opts).length > 0
  }

  const scrollToLastViewedSkill = (timeout = null) => {
    if (isScrollFeatureSupported()) {
      if (timeout) {
        setTimeout(() => {
          nextTick(() => doScrollToLastViewedSkill())
        }, timeout)
      } else {
        nextTick(() => doScrollToLastViewedSkill())
      }
    }
  }
  const doScrollToLastViewedSkill = () => {
    if (isScrollFeatureSupported() && lastViewedSkillId.value) {
      const elementId = `skillRow-${lastViewedSkillId.value}`
      const element = document.getElementById(elementId)
      if (element) {
        const opts = parentFrame.options
        const shouldUseScrollOffsetStrategy = opts && (opts.autoScrollStrategy === 'top-offset') && opts.scrollTopOffset && opts.scrollTopOffset > 0

        nextTick(() => {
          if (shouldUseScrollOffsetStrategy) {
            const scrollToElement = document.getElementById(elementId)
            const distanceFromTop = scrollToElement.getBoundingClientRect().top
            if (parentFrame.parentFrame) {
              parentFrame.parentFrame.emit('do-scroll', distanceFromTop)
            } else {
              log.warn('WARNING: scrollInfo as provided but the code is not running within iframe')
            }
          } else {
            element.scrollIntoView({ behavior: 'smooth' })
          }
          nextTick(() => {
            const idToFocusOn = `skillProgressTitleLink-${lastViewedSkillId.value}`
            const elementFocusOn = document.getElementById(idToFocusOn)
            elementFocusOn.focus({ preventScroll: true })
            log.trace(`Focused on [${idToFocusOn}]`)
          })
        })
      }
    }
  }

  return {
    setLastViewedSkillId,
    lastViewedSkillId,
    isScrollFeatureSupported,
    scrollToLastViewedSkill
  }
})