import { computed } from 'vue'
import { useRoute } from 'vue-router'

export const usePagePath = () => {
  const route = useRoute()

  const adminHomePage = '/administrator'
  const isAdminPage = computed(() => {
    return route.fullPath.toLowerCase().startsWith(adminHomePage)
  })

  const progressAndRankingHomePage = '/progress-and-rankings'
  const isProgressAndRankingPage = computed(() => {
    return route.fullPath.toLowerCase().startsWith(progressAndRankingHomePage)
  })

  const settingsHomePage = '/settings'
  const isSettingsPage = computed(() => {
    return route.fullPath.toLowerCase().startsWith(settingsHomePage)
  })

  return {
    adminHomePage,
    isAdminPage,
    progressAndRankingHomePage,
    isProgressAndRankingPage,
    settingsHomePage,
    isSettingsPage
  }
}
