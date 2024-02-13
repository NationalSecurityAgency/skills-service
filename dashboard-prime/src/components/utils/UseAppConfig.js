import { computed } from 'vue'
import { useStore } from 'vuex'

export const useAppConfig = () => {
  const store = useStore()

  const toNumOr0 = (strNum) => {
    return strNum ? Number(strNum) : 0
  }

  const rankingAndProgressViewsEnabled = computed(() => {
    return (
      store.getters.config.rankingAndProgressViewsEnabled === true ||
      store.getters.config.rankingAndProgressViewsEnabled === 'true'
    )
  })
  const docsHost = computed(() => {
    return store.getters.config.docsHost
  })

  const minNameLength = store.getters.config.minNameLength;
  const maxProjectNameLength = store.getters.config.maxProjectNameLength;
  const maxQuizNameLength = store.getters.config.maxQuizNameLength;
  const nameValidationRegex = store.getters.config.nameValidationRegex;
  const minIdLength = store.getters.config.minIdLength;
  const maxIdLength = store.getters.config.maxIdLength;
  const descriptionMaxLength = store.getters.config.descriptionMaxLength
  const paragraphValidationRegex = store.getters.config.paragraphValidationRegex
  const formFieldDebounceInMs = store.getters.config.formFieldDebounceInMs || 400
  const maxSubjectNameLength = store.getters.config.maxSubjectNameLength;
  const maxCustomLabelLength = store.getters.config.maxCustomLabelLength;
  const maxSkillVersion = store.getters.config.maxSkillVersion
  const maxSkillNameLength = store.getters.config.maxSkillNameLength
  const maxPointIncrement = toNumOr0(store.getters.config.maxPointIncrement)
  const maxNumPerformToCompletion = toNumOr0(store.getters.config.maxNumPerformToCompletion)
  const maxNumPointIncrementMaxOccurrences = toNumOr0(store.getters.config.maxNumPointIncrementMaxOccurrences)
  const maxTimeWindowInMinutes = toNumOr0(store.getters.config.maxTimeWindowInMinutes)
  const maxTimeWindowInHrs = maxTimeWindowInMinutes / 60
  return {
    rankingAndProgressViewsEnabled,
    docsHost,
    minNameLength,
    maxProjectNameLength,
    maxQuizNameLength,
    nameValidationRegex,
    minIdLength,
    maxIdLength,
    descriptionMaxLength,
    formFieldDebounceInMs,
    paragraphValidationRegex,
    maxSubjectNameLength,
    maxCustomLabelLength,
    maxSkillVersion,
    maxSkillNameLength,
    maxPointIncrement,
    maxNumPerformToCompletion,
    maxNumPointIncrementMaxOccurrences,
    maxTimeWindowInHrs,
  }
}
