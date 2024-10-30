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
import MyProgress from '@/components/myProgress/MyProgress.vue'
import MyProgressPage from '@/components/myProgress/MyProgressPage.vue'
import MyProjectSkillsPage from '@/components/myProgress/MyProjectSkillsPage.vue'
import QuizRun from '@/components/quiz/QuizRunInDashboard.vue';
import DiscoverProjectsPage from '@/components/myProgress/discover/DiscoverProjectsPage.vue'
import MyUsagePage from '@/components/myProgress/usage/MyUsagePage.vue'
import MyBadges from '@/components/myProgress/badges/MyBadges.vue'
import MyQuizAttemptsPage   from "@/components/myProgress/quiz/MyQuizAttemptsPage.vue";
import MySingleQuizAttemptPage from "@/components/myProgress/quiz/MySingleQuizAttemptPage.vue";

const createProgressAndRankingRoutes = (skillsDisplayChildRoutes) => {

  const skillPlaceholder = '##SKILL##'

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
        name: 'MyQuizAttemptsPage',
        path: 'my-quiz-attempts',
        component: MyQuizAttemptsPage,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'My Quiz and Survey Run',
          },
        },
      }, {
        name: 'MySingleQuizAttemptPage',
        path: 'my-quiz-attempts/:attemptId',
        component: MySingleQuizAttemptPage,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'My Quiz or Survey Attempt',
          },
        }
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
      }, {
        name: 'MyBadges',
        path: 'my-badges',
        component: MyBadges,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'My Badges',
          },
        },
      },
    ]
  }
}

export default createProgressAndRankingRoutes
