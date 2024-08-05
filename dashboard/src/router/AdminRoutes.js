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
import AdminHomePage from '@/components/AdminHomePage.vue'
import MyProjects from '@/components/projects/MyProjects.vue'
import QuizDefinitionsPage from '@/components/quiz/QuizDefinitionsPage.vue'
import UserActionsPage from '@/components/userActions/UserActionsPage.vue'
import MultipleProjectsMetricsPage from '@/components/metrics/multipleProjects/MultipleProjectsMetricsPage.vue'
import EmaillProjectAdmins from '@/components/projects/EmaillProjectAdmins.vue'
import GlobalBadges from '@/components/badges/global/GlobalBadges.vue'

const createAdminRoutes = () => {
  return {
    path: '/administrator',
    component: AdminHomePage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Project Administrator'
      }
    },
    children: [
      {
        name: 'AdminHomePage',
        path: '',
        component: MyProjects,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Project Administrator'
          }
        }
      }, {
        name: 'QuizzesAndSurveys',
        path: 'quizzes',
        component: QuizDefinitionsPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Quizzes and Surveys',
          },
        },
      }, {
        name: 'GlobalBadges',
        path: 'globalBadges',
        component: GlobalBadges,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Global Badges',
          },
        },
      }, {
        name: 'UserActions',
        path: 'userActions',
        component: UserActionsPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'User Actions History',
          },
        },
      }, {
        name: 'MultipleProjectsMetricsPage',
        path: 'metrics',
        component: MultipleProjectsMetricsPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'All Projects Metrics',
          },
        },
      }, {
        name: 'ContactAdmins',
        path: 'contactAdmins',
        component: EmaillProjectAdmins,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Contact Project Admins',
          },
        },
      },
    ]
  }
}

export default createAdminRoutes
