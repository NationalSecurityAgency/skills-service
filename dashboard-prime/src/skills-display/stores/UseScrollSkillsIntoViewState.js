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