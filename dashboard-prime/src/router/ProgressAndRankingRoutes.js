import MyProgress from '@/components/myProgress/MyProgress.vue'
import MyProgressPage from '@/components/myProgress/MyProgressPage.vue'
import MyProjectSkillsPage from '@/components/myProgress/MyProjectSkillsPage.vue'

const createProgressAndRankingRoutes = () => {
  return {
    path: '/progress-and-rankings',
    component: MyProgress,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'My Progress'
      }
    },
    children: [
      {
        name: 'MyProgressPage',
        path: '',
        component: MyProgressPage,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'My Progress'
          }
        }
      }, {
        name: 'MyProjectSkills',
        path: 'projects/:projectId',
        component: MyProjectSkillsPage,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'My Progress Project',
          },
        },
      },
    ]
  }
}

export default createProgressAndRankingRoutes
