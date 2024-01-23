import { computed } from 'vue'
import { useStore } from 'vuex'

export const useAppConfig = () => {
  const store = useStore()

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
  const nameValidationRegex = store.getters.config.nameValidationRegex;
  const minIdLength = store.getters.config.minIdLength;
  const maxIdLength = store.getters.config.maxIdLength;
  return {
    rankingAndProgressViewsEnabled,
    docsHost,
    minNameLength,
    maxProjectNameLength,
    nameValidationRegex,
    minIdLength,
    maxIdLength
  }
}
