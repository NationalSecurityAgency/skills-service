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
  return {
    rankingAndProgressViewsEnabled,
    docsHost
  }
}
