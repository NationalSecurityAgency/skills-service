import MyProgress from '@/components/myProgress/MyProgress.vue'
import MyProgressPage from '@/components/myProgress/MyProgressPage.vue'
import MyProjectSkillsPage from '@/components/myProgress/MyProjectSkillsPage.vue'
import QuizRun from '@/components/quiz/QuizRunInDashboard.vue';
import DiscoverProjectsPage from '@/components/myProgress/discover/DiscoverProjectsPage.vue'
import MyUsagePage from '@/components/myProgress/usage/MyUsagePage.vue'

const createProgressAndRankingRoutes = (skillsDisplayChildRoutes) => {
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
        path: 'projects/:projectId',
        component: MyProjectSkillsPage,
        name: 'MyProjectSkillsPage',
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'Skills Display'
          }
        },
        children: skillsDisplayChildRoutes
      },{
        name: 'QuizRun',
        path: 'quizzes/:quizId',
        component: QuizRun,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'My Test Run',
          },
        },
      }, {
        name: 'MyUsagePage',
        path: 'my-usage',
        component: MyUsagePage,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'My Usage',
          },
        },
      }, {
        name: 'DiscoverProjectsPage',
        path: 'manage-my-projects',
        component: DiscoverProjectsPage,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'Discover Projects',
          },
        },
      }
    ]
  }
}

export default createProgressAndRankingRoutes
