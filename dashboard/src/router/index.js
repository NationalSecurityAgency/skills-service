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
import {createMemoryHistory, createRouter, createWebHistory} from 'vue-router'
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
import PrivateProjectAccessRequestPage from '@/components/access/invite-only/PrivateProjectAccessRequestPage.vue'
import JoinProjectPage from '@/components/access/invite-only/JoinProjectPage.vue'
import NotFoundPage from '@/components/utils/NotFoundPage.vue';
import GlobalBadgePage from "@/components/badges/global/GlobalBadgePage.vue";
import GlobalBadgeSkills from "@/components/badges/global/GlobalBadgeSkills.vue";
import GlobalBadgeLevels from "@/components/levels/global/GlobalBadgeLevels.vue";
import RequestPasswordReset from '@/components/access/RequestPasswordReset.vue';
import ResetPassword from '@/components/access/ResetPassword.vue';
import RequestResetConfirmation from '@/components/access/RequestResetConfirmation.vue';
import ResetConfirmation from '@/components/access/ResetConfirmation.vue';
import ResetNotSupportedPage from '@/components/access/ResetNotSupportedPage.vue';
import RequestAccount from '@/components/access/RequestAccount.vue';
import UserAgreement from '@/components/access/UserAgreement.vue'
import EmailVerificationSent from "@/components/access/EmailVerificationSent.vue";
import EmailVerifiedConfirmation from "@/components/access/EmailVerifiedConfirmation.vue";
import RequestEmailVerification from "@/components/access/RequestEmailVerification.vue";
import AdminGroupPage from '@/components/access/groups/AdminGroupPage.vue';
import AdminGroupMembers from '@/components/access/groups/AdminGroupMembers.vue';
import AdminGroupProjects from '@/components/access/groups/AdminGroupProjects.vue';
import AdminGroupQuizzes from '@/components/access/groups/AdminGroupQuizzes.vue';
import RedirectPage from "@/components/utils/RedirectPage.vue";
import UpgradeInProgressPage from '@/components/utils/errors/UpgradeInProgressPage.vue'
import SkillsClientPath from '@/router/SkillsClientPath.js'
import log from 'loglevel'
import UserArchivePage from '@/components/users/UserArchivePage.vue';
import UsersTablePage from '@/components/users/UsersTablePage.vue';
import UserCommentsPage from "@/components/userComments/UserCommentsPage.vue";

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
    path: '/request-account',
    name: 'RequestAccount',
    component: RequestAccount,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Request New Account',
      },
    },
  },
  {
    path: '/request-root-account',
    name: 'RequestRootAccount',
    component: RequestAccount,
    meta: {
      requiresAuth: false,
      isRootAccount: true,
      announcer: {
        message: 'Request Root Account',
      },
    },
  },
  {
    path: '/forgot-password',
    name: 'ForgotPassword',
    component: RequestPasswordReset,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Forgot Password',
      },
    },
  },
  {
    path: '/reset-password/:resetToken',
    name: 'ResetPassword',
    component: ResetPassword,
    props: true,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Reset Password',
      },
    },
  },
  {
    path: '/forgot-password-confirmation/:email',
    name: 'RequestResetConfirmation',
    component: RequestResetConfirmation,
    props: true,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Request Password Reset',
      },
    },
  },
  {
    path: '/reset-password-confirmation',
    name: 'ResetConfirmation',
    component: ResetConfirmation,
    props: true,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Password Reset Confirmation',
      },
    },
  },
  {
    path: '/reset-not-supported',
    name: 'ResetNotSupportedPage',
    component: ResetNotSupportedPage,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Reset Not Supported',
      },
    },
  },
  {
    path: '/email-verification-sent',
    name: 'EmailVerificationSent',
    component: EmailVerificationSent,
    props: true,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Email Verification Sent',
      },
    },
  },
  {
    path: '/verify-email/:token/:email',
    name: 'EmailVerifiedConfirmation',
    component: EmailVerifiedConfirmation,
    props: true,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Email Verification Confirmation',
      },
    },
  },
  {
    path: '/request-email-verification',
    name: 'RequestEmailVerification',
    component: RequestEmailVerification,
    props: true,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Request Email Verification',
      },
    },
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
    path: '/upgrade-in-progress',
    name: 'DbUpgradeInProgressPage',
    component: UpgradeInProgressPage,
    props: true,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Dashboard upgrade in progress',
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
      path: 'users',
      component: Users,
      meta: { requiresAuth: true },
      children: [{
        component: UsersTablePage,
        name: 'ProjectUsers',
        path: '',
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitProjectUsers',
          announcer: {
            message: 'Project Users',
          },
        },
      }, {
        component: UserArchivePage,
        name: 'UserArchivePage',
        path: 'user-archive',
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'User Archive Page',
          },
        },
      }],
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
      name: 'UserComments',
      path: '/administrator/projects/:projectId/user-comments',
      component: UserCommentsPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Project Skills Catalog',
        },
      },
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
      path: 'users',
      component: Users,
      meta: { requiresAuth: true },
      children: [{
        component: UsersTablePage,
        name: 'SkillUsers',
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
      name: 'ConfigureVideo',
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
      path: 'users',
      component: Users,
      meta: { requiresAuth: true },
      children: [{
        component: UsersTablePage,
        name: 'SubjectUsers',
        path: '',
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitSubjectUsers',
          announcer: {
            message: 'Subject Users',
          },
        },
      }],
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
      path: 'users',
      component: Users,
      meta: { requiresAuth: true },
      children: [{
        component: UsersTablePage,
        name: 'BadgeUsers',
        path: '',
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitBadgeUsers',
          announcer: {
            message: 'Badge Users',
          },
        },
      }],
    }],
  }, {
    path: '/join-project/:pid/:inviteToken',
    name: 'JoinProject',
    component: JoinProjectPage,
    props: (route) => ({ ...route.params, projectName: route.query.pn }),
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      breadcrumb: (route) => [{
        value: `Join Project ${route.query.pn}`,
        url: `/join-project/${route.params.pid}/${route.params.inviteToken}?pn=${route.query.pn}`,
      }],
      announcer: {
        message: 'Join Project',
      },
    },
  },
  {
    path: '/request-access/:projectId',
    name: 'PrivateProjectAccessRequestPage',
    component: PrivateProjectAccessRequestPage,
    props: true,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Request Access to Private Project',
      },
    },
  },
  {
    path: '/administrator/globalBadges/:badgeId',
    component: GlobalBadgePage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Global Badge Overview',
      },
    },
    children: [{
      name: 'GlobalBadgeSkills',
      path: '',
      component: GlobalBadgeSkills,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Global Badge Skills',
        },
      },
    }, {
      name: 'GlobalBadgeLevels',
      path: 'levels',
      component: GlobalBadgeLevels,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Global Badge Levels',
        },
      },
    }],
  },
  {
    path: '/administrator/adminGroups/:adminGroupId',
    component: AdminGroupPage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Manage Admin Group',
      },
    },
    children: [{
      name: 'AdminGroupMembers',
      path: '',
      component: AdminGroupMembers,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Admin Group Members',
        },
      },
    }, {
      name: 'AdminGroupProjects',
      path: 'group-projects',
      component: AdminGroupProjects,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Admin Group Projects',
        },
      },
    }, {
      name: 'AdminGroupQuizzes',
      path: 'group-quizzes',
      component: AdminGroupQuizzes,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Admin Group Quizzes and Surveys',
        },
      },
    }],
  },
  {
    path: '/redirect',
    name: 'Redirect',
    component: RedirectPage,
    props: true,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Redirecting',
      },
    },
  },
  {
    path: '/not-found',
    name: 'NotFoundPage',
    component: NotFoundPage,
    props: true,
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Not Found',
      },
    },
  },
  { path: '/:pathMatch(.*)*',
    name: '404',
    redirect: {
      name: 'NotFoundPage',
    },
    meta: {
      requiresAuth: false,
      announcer: {
        message: 'Page Not Found',
      },
    },
  }, 
  {
    path: '/user-agreement',
    component: UserAgreement,
    name: 'UserAgreement',
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'User Agreement',
      },
    },
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

const isSkillsClient = SkillsClientPath.isSkillsClientIframePath()
const history = isSkillsClient ? createMemoryHistory(import.meta.env.BASE_URL) : createWebHistory(import.meta.env.BASE_URL)
const actualRoutes = isSkillsClient ? [createSkillsClientRoutes(createSkillsDisplayChildRoutes(PathAppendValues.SkillsClient, true))] : routes
const constructRouter = () => {
  const router =  createRouter({
    history,
    routes: actualRoutes
  })

  if (isSkillsClient) {
    router.push('/')
  }

  log.trace(`Constructed router for path [${window?.location?.pathname}] isSkillsClient: [${isSkillsClient}]`)

  return router
}

export default constructRouter
