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
import EmaillProjectAdmins from '@/components/projects/EmaillProjectAdmins.vue'
import GlobalBadges from '@/components/badges/global/GlobalBadges.vue'
import AdminGroupsPage from '@/components/access/groups/AdminGroupsPage.vue'
import UsersOverallProgressPage from "@/components/users/UsersOverallProgressPage.vue";
import OverallMetricsPage from "@/components/metrics/OverallMetricsPage.vue";
import GlobalQuizRunsHistoryPage from "@/components/quiz/runsHistory/GlobalQuizRunsHistoryPage.vue";
import SkillOverview from '@//components/skills/SkillOverview.vue'
import Users from '@/components/users/Users.vue'
import UsersTablePage from '@/components/users/UsersTablePage.vue'
import SlidesConfigPage from '@/components/slides/SlidesConfigPage.vue'
import VideoConfigPage from '@/components/video/VideoConfigPage.vue'
import ExpirationConfigPage from '@/components/expiration/ExpirationConfigPage.vue'
import AddSkillEvent from '@/components/skills/AddSkillEvent.vue'
import SkillMetricsPage from '@/components/metrics/skill/SkillMetricsPage.vue'

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
        name: 'UsersOverallProgressPage',
        path: 'users-progress',
        component: UsersOverallProgressPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Cross-projects users',
          },
        },
      }, {
        name: 'GlobalQuizRunsHistoryPage',
        path: 'quiz-runs',
        component: GlobalQuizRunsHistoryPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Quiz and Survey Runs',
          },
        },
      }, {
        name: 'OverallMetricsPage',
        path: 'overall-metrics',
        component: OverallMetricsPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Overall Metrics',
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
        name: 'AdminGroups',
        path: 'adminGroups',
        component: AdminGroupsPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Administrator Groups',
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

const createAdminSkillChildRoutes = (baseName) => [
  {
    name: baseName,
    path: '',
    component: SkillOverview,
    meta: {
      requiresAuth: true,
      reportSkillId: 'VisitSkillOverview',
      announcer: {
        message: 'Skill Overview',
      },
    },
  }, {
    path: 'users',
    component: Users,
    meta: { requiresAuth: true },
    children: [{
      component: UsersTablePage,
      name: `SkillUsers${baseName}`,
      path: '',
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitSkillUsers',
        announcer: {
          message: 'Skill Users',
        },
      },
    }],
  }, {
    name: `ConfigureSlides${baseName}`,
    path: 'config-slides',
    component: SlidesConfigPage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Configure Slides',
      },
    },
    props: true,
  }, {
    name: `ConfigureVideo${baseName}`,
    path: 'config-video',
    component: VideoConfigPage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Configure Audio/Video',
      },
    },
    props: true,
  }, {
    name: `ConfigureExpiration${baseName}`,
    path: 'config-expiration',
    component: ExpirationConfigPage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Configure Expiration',
      },
    },
    props: true,
  }, {
    name: `AddSkillEvent${baseName}`,
    path: 'addSkillEvent',
    component: AddSkillEvent,
    meta: {
      requiresAuth: true,
      breadcrumb: 'Add Skill Event',
      announcer: {
        message: 'Add Skill Event',
      },
    },
    props: true,
  }, {
    name: `SkillMetrics${baseName}`,
    path: 'metrics',
    component: SkillMetricsPage,
    meta: {
      requiresAuth: true,
      reportSkillId: 'VisitSkillStats',
      announcer: {
        message: 'Skill Metrics',
      },
    }
  }];

export { createAdminRoutes, createAdminSkillChildRoutes }