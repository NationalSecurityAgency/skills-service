import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/components/access/Login.vue'
import MyProgress from '@/components/myProgress/MyProgress.vue'
import createAdminRoutes from './AdminRoutes.js'
import createProgressAndRankingRoutes from './ProgressAndRankingRoutes.js'
import GlobalSettings from '@/components/settings/GlobalSettings.vue'
import GeneralSettings from '@/components/settings/GeneralSettings.vue'

const routes = [
  {
    path: '/',
    component: MyProgress,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'My Progress'
      }
    }
  },
  {
    path: '/skills-login',
    name: 'Login',
    component: Login,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Login'
      }
    }
  },
  {
    path: '/settings',
    component: GlobalSettings,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Skill Tree Settings'
      }
    },
    children: [
      {
        name: 'GeneralSettings',
        path: '',
        component: GeneralSettings,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          reportSkillId: 'VisitUserSettings',
          announcer: {
            message: 'General Settings'
          }
        }
      }
    ]
  }
]

routes.push(createAdminRoutes())
routes.push(createProgressAndRankingRoutes())

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
