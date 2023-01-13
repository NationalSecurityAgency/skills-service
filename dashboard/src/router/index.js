/*
 * Copyright 2020 SkillTree
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
import Vue from 'vue';
import Router from 'vue-router';
import AdminHomePage from '@/components/AdminHomePage';
import MyProjects from '@/components/projects/MyProjects';
import QuizDefinitionsPage from '@/components/quiz/QuizDefinitionsPage';
import LoginForm from '@/components/access/Login';
import RequestAccountForm from '@/components/access/RequestAccess';
import ProjectPage from '@/components/projects/ProjectPage';
import ErrorPage from '@/components/utils/ErrorPage';
import DbUpgradeInProgressPage from '@/components/utils/DbUpgradeInProgressPage';
import NotAuthorizedPage from '@/components/utils/NotAuthorizedPage';
import PrivateProjectAccessRequestPage from '@/components/utils/PrivateProjectAccessRequestPage';
import NotFoundPage from '@/components/utils/NotFoundPage';
import SubjectPage from '@/components/subjects/SubjectPage';
import BadgePage from '@/components/badges/BadgePage';
import SkillPage from '@/components/skills/SkillPage';
import UserPage from '@/components/users/UserPage';
import GlobalSettings from '@/components/settings/GlobalSettings';
import Subjects from '@//components/subjects/Subjects';
import Badges from '@//components/badges/Badges';
import Levels from '@//components/levels/Levels';
import Users from '@//components/users/Users';
import AccessSettings from '@//components/access/AccessSettings';
import ProjectSettings from '@//components/settings/ProjectSettings';
import Skills from '@//components/skills/Skills';
import BadgeSkills from '@//components/badges/BadgeSkills';
import SkillOverview from '@//components/skills/SkillOverview';
import UserSkillsPerformed from '@//components/users/UserSkillsPerformed';
import GeneralSettings from '@//components/settings/GeneralSettings';
import Preferences from '@//components/settings/Preferences';
import ResetPassword from '@//components/access/ResetPassword';
import RequestPasswordReset from '@//components/access/RequestPasswordReset';
import RequestResetConfirmation from '@//components/access/RequestResetConfirmation';
import ResetConfirmation from '@//components/access/ResetConfirmation';
import EmailVerificationSent from '@//components/access/EmailVerificationSent';
import EmailVerifiedConfirmation from '@//components/access/EmailVerifiedConfirmation';
import RequestEmailVerification from '@//components/access/RequestEmailVerification';
import ResetNotSupportedPage from '@//components/access/ResetNotSupportedPage';
import SelfReportStatusPage from '@//components/skills/selfReport/SelfReportStatusPage';
import UserAgreement from '@//components/access/UserAgreement';
import EmailUsers from '@//components/projects/EmailUsers';
import EmaillProjectAdmins from '@//components/projects/EmailProjectAdmins';
import MyBadges from '@//components/myProgress/badges/MyBadges';
import SkillsCatalog from '@//components/skills/catalog/SkillsCatalog';
import JoinProject from '@/components/access/JoinProject';
import SubjectMetricsPage from '@/components/metrics/projectSubjects/SubjectMetricsPage';
import MetricsOnSubjectPage from '@/components/metrics/subject/MetricsOnSubjectPage';
import MetricsPageNav from '@/components/metrics/MetricsPageNav';
import SkillMetricsPage from '@/components/metrics/skill/SkillMetricsPage';
import MultipleProjectsMetricsPage from '@//components/metrics/multipleProjects/MultipleProjectsMetricsPage';
import SkillsMetricsPage from '@/components/metrics/projectSkills/SkillsMetricsPage';
import UsersAchievementsMetricPage from '@/components/metrics/projectAchievements/UsersAchievementsMetricPage';
import ProjectMetrics from '@/components/metrics/ProjectMetrics';
import UserTagMetrics from '@/components/metrics/userTags/UserTagMetrics';
import GlobalBadgePage from '@/components/badges/global/GlobalBadgePage';
import GlobalBadgeSkills from '@/components/badges/global/GlobalBadgeSkills';
import GlobalBadgeLevels from '@/components/levels/global/GlobalBadgeLevels';
import GlobalBadges from '@/components/badges/global/GlobalBadges';
import FullDependencyGraph from '@//components/skills/dependencies/FullDependencyGraph';
import SecuritySettings from '@/components/settings/SecuritySettings';
import EmailSettings from '@/components/settings/EmailSettings';
import SystemSettings from '@/components/settings/SystemSettings';
import SkillDependencies from '@/components/skills/dependencies/SkillDependencies';
import AddSkillEvent from '@/components/skills/AddSkillEvent';
import InceptionSkills from '@/components/inception/InceptionSkills';
import ClientDisplayPreview from '@/components/users/ClientDisplayPreview';
import MyProgressPage from '@/components/myProgress/MyProgressPage';
import MyProgress from '@/components/myProgress/MyProgress';
import MyUsagePage from '@/components/myProgress/usage/MyUsagePage';
import DiscoverProjectsPage from '@/components/myProgress/discover/DiscoverProjectsPage';
import MyProjectSkillsPage from '@/components/myProgress/MyProjectSkillsPage';
import ProjectErrorsPage from '@/components/projects/ProjectErrors';
import SelfReportPageNav from '@/components/skills/selfReport/SelfReportPageNav';
import SelfReportConfigurePage from '@/components/skills/selfReport/SelfReportConfigurePage';
import QuizPage from '@/components/quiz/QuizPage';
import Questions from '@/components/quiz/testCreation/Questions';
import QuizMetrics from '@/components/quiz/metrics/QuizMetrics';
import QuizRun from '@/components/quiz/QuizRunInDashboard';

Vue.use(Router);

const router = new Router({
  mode: 'history',
  routes: [
    {
      path: '/administrator',
      component: AdminHomePage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Project Administrator',
        },
      },
      children: [{
        name: 'AdminHomePage',
        path: '',
        component: MyProjects,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Project Administrator',
          },
        },
      }, {
        name: 'TestAndSurveys',
        path: 'tests-and-surveys',
        component: QuizDefinitionsPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Tests and Surveys',
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
      }],
    },
    {
      path: '/skills-login',
      name: 'Login',
      component: LoginForm,
      meta: {
        requiresAuth: false,
        announcer: {
          message: 'Login',
        },
      },
    },
    {
      path: '/request-account',
      name: 'RequestAccount',
      component: RequestAccountForm,
      meta: {
        requiresAuth: false,
        announcer: {
          message: 'Request New Account',
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
      path: '/forgot-password-confirmation',
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
      path: '/request-root-account',
      name: 'RequestRootAccount',
      component: RequestAccountForm,
      props: { isRootAccount: true },
      meta: {
        requiresAuth: false,
        announcer: {
          message: 'Request Root Account',
        },
      },
    },
    {
      path: '/error',
      name: 'ErrorPage',
      component: ErrorPage,
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
      component: DbUpgradeInProgressPage,
      meta: {
        requiresAuth: true,
      },
    },
    {
      path: '/not-authorized',
      name: 'NotAuthorizedPage',
      component: NotAuthorizedPage,
      props: true,
      meta: {
        requiresAuth: false,
        announcer: {
          message: 'Not Authorized',
        },
      },
    },
    {
      path: '/request-access',
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
    {
      path: '/join-project/:pid/:inviteToken',
      name: 'JoinProject',
      component: JoinProject,
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
      path: '/progress-and-rankings',
      component: MyProgress,
      meta: {
        requiresAuth: true,
        nonAdmin: true,
        announcer: {
          message: 'My Progress',
        },
      },
      children: [{
        name: 'MyProgressPage',
        path: '',
        component: MyProgressPage,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          announcer: {
            message: 'My Progress',
          },
        },
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
      }, {
        name: 'QuizRun',
        path: 'tests/:quizId',
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
      }],
    },
    {
      path: '/',
      component: MyProgress,
      meta: {
        requiresAuth: true,
        nonAdmin: true,
        announcer: {
          message: 'My Progress',
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
      }, {
        name: 'FullDependencyGraph',
        path: 'dependencies',
        component: FullDependencyGraph,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitProjectDependencies',
          announcer: {
            message: 'All Project Dependencies',
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
      }],
    },
    {
      path: '/administrator/projects/:projectId/subjects/:subjectId',
      component: SubjectPage,
      meta: { requiresAuth: true },
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
        name: 'SkillDependencies',
        path: 'dependencies',
        component: SkillDependencies,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitSkillDependencies',
          announcer: {
            message: 'Skill Dependencies',
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
        },
      }],
    },
    {
      path: '/administrator/projects/:projectId/users/:userId',
      component: UserPage,
      meta: { requiresAuth: true },
      children: [{
        name: 'ClientDisplayPreview',
        path: '',
        component: ClientDisplayPreview,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitClientDisplay',
          announcer: {
            message: 'Client Display Preview for user',
          },
        },
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
      path: '/administrator/projects/:projectId/subjects/:subjectId/users/:userId',
      component: UserPage,
      meta: { requiresAuth: true },
      children: [{
        name: 'ClientDisplayPreviewSubject',
        path: '',
        component: ClientDisplayPreview,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitClientDisplay',
          announcer: {
            message: 'Client Display Preview for user',
          },
        },
      }, {
        name: 'UserSkillEventsSubject',
        path: 'skillEvents',
        component: UserSkillsPerformed,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitUserPerformedSkills',
          announcer: {
            message: 'User\'s Skill Events for Subject',
          },
        },
      }],
    },
    {
      path: '/administrator/projects/:projectId/subjects/:subjectId/skills/:skillId/users/:userId',
      component: UserPage,
      meta: { requiresAuth: true },
      children: [{
        name: 'ClientDisplayPreviewSkill',
        path: '',
        component: ClientDisplayPreview,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitClientDisplay',
          announcer: {
            message: 'Client Display Preview for user',
          },
        },
      }, {
        name: 'UserSkillEventsSkill',
        path: 'skillEvents',
        component: UserSkillsPerformed,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitUserPerformedSkills',
          announcer: {
            message: 'User\'s Performed Skill Events',
          },
        },
      }],
    },
    {
      path: '/administrator/projects/:projectId/badges/:badgeId/users/:userId',
      component: UserPage,
      meta: { requiresAuth: true },
      children: [{
        name: 'ClientDisplayPreviewBadge',
        path: '',
        component: ClientDisplayPreview,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitClientDisplay',
          announcer: {
            message: 'User\'s Badge Client Display Preview',
          },
        },
      }, {
        name: 'UserSkillEventsBadge',
        path: 'skillEvents',
        component: UserSkillsPerformed,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitUserPerformedSkills',
          announcer: {
            message: 'User\'s Performed Skill EVents for Badge',
          },
        },
      }],
    },
    {
      path: '/administrator/tests-and-surveys/:quizId',
      component: QuizPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Manage Test',
        },
      },
      children: [{
        name: 'Questions',
        path: '',
        component: Questions,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Test or Survey',
          },
        },
      }, {
        name: 'QuizMetrics',
        path: 'metrics',
        component: QuizMetrics,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Test Metrics',
          },
        },
      }],
    },
    {
      path: '/settings',
      component: GlobalSettings,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Skill Tree Settings',
        },
      },
      children: [{
        name: 'GeneralSettings',
        path: '',
        component: GeneralSettings,
        meta: {
          requiresAuth: true,
          nonAdmin: true,
          reportSkillId: 'VisitUserSettings',
          announcer: {
            message: 'General Settings',
          },
        },
      }, {
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
      }],
    },
    {
      path: '/administrator/skills',
      name: 'InceptionSkills',
      component: InceptionSkills,
      meta: {
        requiresAuth: true,
        breadcrumb: 'Dashboard Skills',
        reportSkillId: 'VisitDashboardSkills',
        announcer: {
          message: 'Dashboard Skills',
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
      path: '*',
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
  ],
});

export default router;
