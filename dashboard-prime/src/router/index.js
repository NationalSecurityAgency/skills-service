import { createRouter, createWebHistory } from 'vue-router'
import Login from '@/components/access/Login.vue'
import MyProgress from '@/components/myProgress/MyProgress.vue'
import createAdminRoutes from './AdminRoutes.js'
import createProgressAndRankingRoutes from './ProgressAndRankingRoutes.js'
import createQuizRoutes from '@/router/QuizRoutes.js'
import GlobalSettings from '@/components/settings/GlobalSettings.vue'
import GeneralSettings from '@/components/settings/GeneralSettings.vue'
import Preferences from '@/components/settings/Preferences.vue'
import SecuritySettings from '@/components/settings/SecuritySettings.vue'
import EmailSettings from '@/components/settings/EmailSettings.vue'
import SystemSettings from '@/components/settings/SystemSettings.vue'
import ProjectPage from '@/components/projects/ProjectPage.vue'
import Subjects from '@/components/subjects/Subjects.vue'
import Badges from '@/components/badges/Badges.vue'
import Levels from '@/components/levels/Levels.vue'
import FullDependencyGraph from '@/components/skills/dependencies/FullDependencyGraph.vue'
import Users from '@/components/users/Users.vue'
import SelfReportPageNav from '@/components/skills/selfReport/SelfReportPageNav.vue'
import SelfReportStatusPage from '@/components/skills/selfReport/SelfReportStatusPage.vue'
import SelfReportConfigurePage from '@/components/skills/selfReport/SelfReportConfigurePage.vue'
import AccessSettings from '@/components/access/AccessSettings.vue'
import ProjectSettings from '@/components/settings/ProjectSettings.vue'
import UserActionsPage from '@/components/userActions/UserActionsPage.vue'
import ExpirationHistory from '@/components/expiration/ExpirationHistory.vue'
import EmailUsers from '@/components/projects/EmailUsers.vue'
import ProjectErrorsPage from '@/components/projects/ProjectErrors.vue'
import MetricsPageNav from '@/components/metrics/MetricsPageNav.vue'
import ProjectMetrics from '@/components/metrics/ProjectMetrics.vue'
import UsersAchievementsMetricPage from '@/components/metrics/projectAchievements/UsersAchievementsMetricPage.vue'
import SubjectMetricsPage from '@/components/metrics/projectSubjects/SubjectMetricsPage.vue'
import SkillMetricsPage from '@/components/metrics/skill/SkillMetricsPage.vue'
import SkillsMetricsPage from '@/components/metrics/projectSkills/SkillsMetricsPage.vue'
import UserTagMetrics from '@/components/metrics/userTags/UserTagMetrics.vue'
import SkillsCatalog from '@/components/skills/catalog/SkillsCatalog.vue'
import AddSkillEvent from '@/components/skills/AddSkillEvent.vue'
import VideoConfigPage from '@/components/video/VideoConfigPage.vue'
import ExpirationConfigPage from '@/components/expiration/ExpirationConfigPage.vue'
import SkillPage from '@/components/skills/SkillPage.vue'
import SkillOverview from '@//components/skills/SkillOverview.vue'
import SubjectPage from '@/components/subjects/SubjectPage.vue'
import Skills from '@/components/skills/Skills.vue'
import MetricsOnSubjectPage from '@/components/metrics/subject/MetricsOnSubjectPage.vue'
import BadgePage from '@/components/badges/BadgePage.vue'
import BadgeSkills from '@/components/badges/BadgeSkills.vue'
import ErrorPage from '@/components/utils/errors/ErrorPage.vue'
import createSkillsClientRoutes from '@/router/SkillsDisplaySkillsClientRoutes.js'
import createSkillsDisplayChildRoutes from '@/router/SkillsDisplayChildRoutes.js'
import TestSkillsClient from '@/skills-display/components/test/TestSkillsClient.vue'
import TestSkillsDisplay from '@/skills-display/components/test/TestSkillsDisplay.vue'
import PathAppendValues from '@/router/SkillsDisplayPathAppendValues.js'
import InceptionSkills from '@/components/inception /InceptionSkills.vue'
import UserPage from '@/components/users/UserPage.vue'
import SkillsDisplayPreview from '@/components/users/SkillsDisplayPreview.vue'
import UserSkillsPerformed from '@/components/users/UserSkillsPerformed.vue'
import createSkillsDisplayPreviewRoutes from '@/router/SkillsDisplayPreviewRoutes.js'

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
    path: '/error',
    name: 'ErrorPage',
    component: ErrorPage,
    props: true,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Error Page',
      },
    },
  },
  {
    path: '/administrator/skills/:projectId',
    name: 'InceptionSkills',
    component: InceptionSkills,
    meta: {
      requiresAuth: true,
      reportSkillId: 'VisitDashboardSkills',
      announcer: {
        message: 'Dashboard Skills',
      },
    },
    children: createSkillsDisplayChildRoutes(PathAppendValues.Inception)
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
      },{
        name: 'Preferences',
        path: 'preferences',
        component: Preferences,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          reportSkillId: 'VisitMyPreferences',
          announcer: {
            message: 'My Preferences',
          },
        },
      }, {
        name: 'SecuritySettings',
        path: 'security',
        component: SecuritySettings,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'Security Settings',
          },
        },
      }, {
        name: 'EmailSettings',
        path: 'email',
        component: EmailSettings,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'Email Configuration Settings',
          },
        },
      }, {
        name: 'SystemSettings',
        path: 'system',
        component: SystemSettings,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'System Settings',
          },
        },
      }
    ]
  },
  {
    path: '/administrator/projects/:projectId',
    component: ProjectPage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Manage Project',
      },
    },
    children: [{
      name: 'Subjects',
      path: '',
      component: Subjects,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitSubjects',
        announcer: {
          message: 'Project Subjects',
        },
      },
    }, {
      name: 'Badges',
      path: 'badges',
      component: Badges,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitBadges',
        announcer: {
          message: 'Project Badges',
        },
      },
    }, {
      name: 'ProjectLevels',
      path: 'levels',
      component: Levels,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitProjectLevels',
        announcer: {
          message: 'Project Levels',
        },
      },
    },  {
      name: 'FullDependencyGraph',
      path: 'learning-path',
      component: FullDependencyGraph,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitProjectDependencies',
        announcer: {
          message: 'Project Learning Path',
        },
      },
    }, {
      name: 'ProjectUsers',
      path: 'users',
      component: Users,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitProjectUsers',
        announcer: {
          message: 'Project Users',
        },
      },
    }, {
      path: '/administrator/projects/:projectId/self-report',
      component: SelfReportPageNav,
      meta: { requiresAuth: true },
      children: [{
        component: SelfReportStatusPage,
        name: 'SelfReport',
        path: '',
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitSelfReport',
          announcer: {
            message: 'Manage Project Skill Approval Requests',
          },
        },
      }, {
        component: SelfReportConfigurePage,
        name: 'SelfReportConfigure',
        path: 'configure',
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Self Approval Configuration Page',
          },
        },
      }],
    }, {
      name: 'SkillsCatalog',
      path: '/administrator/projects/:projectId/skills-catalog',
      component: SkillsCatalog,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Project Skills Catalog',
        },
      },
    }, {
      name: 'ProjectAccess',
      path: 'access',
      component: AccessSettings,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitProjectAccessManagement',
        announcer: {
          message: 'Manage Project Access',
        },
      },
    }, {
      name: 'ProjectSettings',
      path: 'settings',
      component: ProjectSettings,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitProjectSettings',
        announcer: {
          message: 'Project Settings',
        },
      },
    }, {
      name: 'ProjectActivityHistory',
      path: 'activityHistory',
      component: UserActionsPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'User Activity History',
        },
      },
    }, {
      name: 'ExpirationHistory',
      path: 'expirationHistory',
      component: ExpirationHistory,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Skill Expiration History',
        },
      },
    }, {
      name: 'EmailUsers',
      path: 'contact-users',
      component: EmailUsers,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitContactUsers',
        announcer: {
          message: 'Contact Project Users',
        },
      },
    }, {
      name: 'ProjectErrorsPage',
      path: 'issues',
      component: ProjectErrorsPage,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitProjectErrors',
        announcer: {
          message: 'Project Errors',
        },
      },
    }, {
      path: '/administrator/projects/:projectId/metrics',
      component: MetricsPageNav,
      meta: { requiresAuth: true },
      children: [{
        name: 'ProjectMetrics',
        path: '',
        component: ProjectMetrics,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitProjectStats',
          announcer: {
            message: 'Project Metrics',
          },
        },
      }, {
        name: 'UsersAndLevelsMetrics',
        path: 'achievements',
        component: UsersAchievementsMetricPage,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitProjectUserAchievementMetrics',
          announcer: {
            message: 'Project User And Level Metrics',
          },
        },
      }, {
        name: 'SubjectMetricsPage',
        path: 'subjects',
        component: SubjectMetricsPage,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitProjectSubjectMetrics',
          announcer: {
            message: 'Project Subject Metrics',
          },
        },
      }, {
        name: 'SkillsMetricsPage',
        path: 'skills',
        component: SkillsMetricsPage,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitProjectSkillMetrics',
          announcer: {
            message: 'Project Skill Metrics',
          },
        },
      }, {
        name: 'UserTagMetrics',
        path: 'userTag/:tagKey/:tagFilter',
        component: UserTagMetrics,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'User Tag Metrics',
          },
        },
      }],
    },]
  },{
    path: '/administrator/projects/:projectId/users/:userId',
    component: UserPage,
    meta: { requiresAuth: true },
    children: [{
      name: 'ClientDisplayPreview',
      path: '',
      component: SkillsDisplayPreview,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitClientDisplay',
        announcer: {
          message: 'Client Display Preview for user',
        },
      },
      children: createSkillsDisplayChildRoutes(PathAppendValues.SkillsDisplayPreview)
    }, {
      name: 'UserSkillEvents',
      path: 'skillEvents',
      component: UserSkillsPerformed,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitUserPerformedSkills',
        announcer: {
          message: 'User\'s Skill Events',
        },
      },
    }],
  },
  {
    path: '/administrator/projects/:projectId/subjects/:subjectId/skills/:skillId',
    component: SkillPage,
    meta: { requiresAuth: true },
    children: [{
      name: 'SkillOverview',
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
      name: 'SkillUsers',
      path: 'users',
      component: Users,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitSkillUsers',
        announcer: {
          message: 'Skill Users',
        },
      },
    }, {
      name: 'ConfigureVideo',
      path: 'config-video',
      component: VideoConfigPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Configure Video',
        },
      },
      props: true,
    }, {
      name: 'ConfigureExpiration',
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
      name: 'AddSkillEvent',
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
      name: 'SkillMetrics',
      path: 'metrics',
      component: SkillMetricsPage,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitSkillStats',
        announcer: {
          message: 'Skill Metrics',
        },
      }
    }]
  },
  {
    path: '/administrator/projects/:projectId/subjects/:subjectId',
    component: SubjectPage,
    meta: {requiresAuth: true},
    children: [{
      name: 'SubjectSkills',
      path: '',
      component: Skills,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitSkillsForASubject',
        announcer: {
          message: 'Subject Skills',
        },
      },
    }, {
      name: 'SubjectLevels',
      path: 'levels',
      component: Levels,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitSubjectLevels',
        announcer: {
          message: 'Subject Levels',
        },
      },
    }, {
      name: 'SubjectUsers',
      path: 'users',
      component: Users,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitSubjectUsers',
        announcer: {
          message: 'Subject Users',
        },
      },
    }, {
      name: 'SubjectMetrics',
      path: 'metrics',
      component: MetricsOnSubjectPage,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitSubjectMetrics',
        announcer: {
          message: 'Subject Metrics',
        },
      },
    }],
  },
  {
    path: '/administrator/projects/:projectId/badges/:badgeId',
    component: BadgePage,
    meta: { requiresAuth: true },
    children: [{
      name: 'BadgeSkills',
      path: '',
      component: BadgeSkills,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitSingleBadgePage',
        announcer: {
          message: 'Badge Skills',
        },
      },
    }, {
      name: 'BadgeUsers',
      path: 'users',
      component: Users,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitBadgeUsers',
        announcer: {
          message: 'Badge Users',
        },
      },
    }],
  }
]

routes.push(createAdminRoutes())
routes.push(createQuizRoutes())
// skills display routes support local components and skills-client apps

routes.push(createProgressAndRankingRoutes(createSkillsDisplayChildRoutes(PathAppendValues.Local)))
routes.push(createSkillsClientRoutes(createSkillsDisplayChildRoutes(PathAppendValues.SkillsClient)))


routes.push({
  path: '/test-skills-client',
  component: ErrorPage,
  name: 'TestSkillsClientHomeNoPage',
  meta: {
    requiresAuth: true,
    nonAdmin: true,
  },
})
routes.push({
  path: '/test-skills-client/:projectId',
  component: TestSkillsClient,
  name: 'TestSkillsClient',
  meta: {
    requiresAuth: true,
    nonAdmin: true,
  },
})

routes.push({
  path: '/test-skills-display',
  component: ErrorPage,
  name: 'TestSkillsDisplayHomeNoPage',
  meta: {
    requiresAuth: true,
    nonAdmin: true,
  },
})
routes.push({
  path: '/test-skills-display/:projectId',
  component: TestSkillsDisplay,
  name: 'TestSkillsDisplay',
  meta: {
    requiresAuth: true,
    nonAdmin: true,
  },
  children: createSkillsDisplayChildRoutes(PathAppendValues.LocalTest)
})

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

export default router
