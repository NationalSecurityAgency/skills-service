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
import HomePage from '@/components/HomePage';
import MyProjects from '@/components/projects/MyProjects';
import LoginForm from '@/components/access/Login';
import RequestAccountForm from '@/components/access/RequestAccess';
import ProjectPage from '@/components/projects/ProjectPage';
import ErrorPage from '@/components/utils/ErrorPage';
import NotAuthorizedPage from '@/components/utils/NotAuthorizedPage';
import SubjectPage from '@/components/subjects/SubjectPage';
import BadgePage from '@/components/badges/BadgePage';
import GlobalBadgePage from '@/components/badges/global/GlobalBadgePage';
import SkillPage from '@/components/skills/SkillPage';
import UserPage from '@/components/users/UserPage';
import GlobalSettings from '@/components/settings/GlobalSettings';
import GFMDescription from '@//components/utils/GFMDescription';
import InceptionSkills from '@//components/inception/InceptionSkills';
import Subjects from '@//components/subjects/Subjects';
import Badges from '@//components/badges/Badges';
import GlobalBadges from '@//components/badges/global/GlobalBadges';
import Levels from '@//components/levels/Levels';
import FullDependencyGraph from '@//components/skills/dependencies/FullDependencyGraph';
import CrossProjectsSkills from '@//components/skills/crossProjects/CrossProjectsSkills';
import Users from '@//components/users/Users';
import AccessSettings from '@//components/access/AccessSettings';
import ProjectSettings from '@//components/settings/ProjectSettings';
import SectionMetrics from '@//components/metrics/SectionMetrics';
import Skills from '@//components/skills/Skills';
import BadgeSkills from '@//components/badges/BadgeSkills';
import GlobalBadgeSkills from '@//components/badges/global/GlobalBadgeSkills';
import GlobalBadgeLevels from '@//components/levels/global/GlobalBadgeLevels';
import SkillOverview from '@//components/skills/SkillOverview';
import SkillDependencies from '@//components/skills/dependencies/SkillDependencies';
import AddSkillEvent from '@//components/skills/AddSkillEvent';
import ClientDisplayPreview from '@//components/users/ClientDisplayPreview';
import UserSkillsPerformed from '@//components/users/UserSkillsPerformed';
import GeneralSettings from '@//components/settings/GeneralSettings';
import SecuritySettings from '@//components/settings/SecuritySettings';
import EmailSettings from '@//components/settings/EmailSettings';
import SystemSettings from '@//components/settings/SystemSettings';
import { SECTION } from '@//components/metrics/SectionHelper';
import ResetPassword from '@//components/access/ResetPassword';
import RequestPasswordReset from '@//components/access/RequestPasswordReset';

Vue.use(Router);

const router = new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      component: HomePage,
      meta: { requiresAuth: false },
      children: [{
        name: 'HomePage',
        path: '',
        component: MyProjects,
        meta: { requiresAuth: true },
      }, {
        name: 'GlobalBadges',
        path: 'globalBadges',
        component: GlobalBadges,
        meta: { requiresAuth: true },
      }, {
        name: 'GlobalMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true, metricsSection: SECTION.GLOBAL },
      }],
    },
    {
      path: '/skills-login',
      name: 'Login',
      component: LoginForm,
      meta: {
        requiresAuth: false,
      },
    },
    {
      path: '/request-account',
      name: 'RequestAccount',
      component: RequestAccountForm,
      meta: {
        requiresAuth: false,
      },
    },
    {
      path: '/forgot-password',
      name: 'ForgotPassword',
      component: RequestPasswordReset,
      meta: {
        requiresAuth: false,
      },
    },
    {
      path: '/request-root-account',
      name: 'RequestRootAccount',
      component: RequestAccountForm,
      props: { isRootAccount: true },
      meta: {
        requiresAuth: false,
      },
    },
    {
      path: '/error',
      name: 'ErrorPage',
      component: ErrorPage,
      meta: {
        requiresAuth: false,
      },
    },
    {
      path: '/not-authorized',
      name: 'NotAuthorizedPage',
      component: NotAuthorizedPage,
      meta: {
        requiresAuth: false,
      },
    },
    {
      path: '/projects/:projectId',
      component: ProjectPage,
      meta: { requiresAuth: true },
      children: [{
        name: 'Subjects',
        path: '',
        component: Subjects,
        meta: {
          requiresAuth: true,
          reportSkillId: 'VisitSubjects',
        },
      }, {
        name: 'Badges',
        path: 'badges',
        component: Badges,
        meta: { requiresAuth: true, reportSkillId: 'VisitBadges' },
      }, {
        name: 'ProjectLevels',
        path: 'levels',
        component: Levels,
        meta: { requiresAuth: true, reportSkillId: 'VisitProjectLevels' },
      }, {
        name: 'FullDependencyGraph',
        path: 'dependencies',
        component: FullDependencyGraph,
        meta: { requiresAuth: true, reportSkillId: 'VisitProjectDependencies' },
      }, {
        name: 'CrossProjectsSkills',
        path: 'cross Project',
        component: CrossProjectsSkills,
        meta: { requiresAuth: true, reportSkillId: 'VisitProjectCrossProjectSkills' },
      }, {
        name: 'ProjectUsers',
        path: 'users',
        component: Users,
        meta: { requiresAuth: true, reportSkillId: 'VisitProjectUsers' },
      }, {
        name: 'ProjectAccess',
        path: 'access',
        component: AccessSettings,
        meta: { requiresAuth: true, reportSkillId: 'VisitProjectAccessManagement' },
      }, {
        name: 'ProjectSettings',
        path: 'settings',
        component: ProjectSettings,
        meta: { requiresAuth: true, reportSkillId: 'VisitProjectSettings' },
      }, {
        name: 'ProjectMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true, reportSkillId: 'VisitProjectStats', metricsSection: SECTION.PROJECTS },
      }],
    },
    {
      path: '/projects/:projectId/subjects/:subjectId',
      component: SubjectPage,
      meta: { requiresAuth: true },
      children: [{
        name: 'SubjectSkills',
        path: '',
        component: Skills,
        meta: { requiresAuth: true, reportSkillId: 'VisitSkillsForASubject' },
      }, {
        name: 'SubjectLevels',
        path: 'levels',
        component: Levels,
        meta: { requiresAuth: true, reportSkillId: 'VisitSubjectLevels' },
      }, {
        name: 'SubjectUsers',
        path: 'users',
        component: Users,
        meta: { requiresAuth: true, reportSkillId: 'VisitSubjectUsers' },
      }, {
        name: 'SubjectMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true, reportSkillId: 'VisitSubjectMetrics', metricsSection: SECTION.SUBJECTS },
      }],
    },
    {
      path: '/projects/:projectId/badges/:badgeId',
      component: BadgePage,
      meta: { requiresAuth: true },
      children: [{
        name: 'BadgeSkills',
        path: '',
        component: BadgeSkills,
        meta: { requiresAuth: true, reportSkillId: 'VisitSingleBadgePage' },
      }, {
        name: 'BadgeUsers',
        path: 'users',
        component: Users,
        meta: { requiresAuth: true, reportSkillId: 'VisitBadgeUsers' },
      }, {
        name: 'BadgeMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true, reportSkillId: 'VisitBadgeStats', metricsSection: SECTION.BADGES },
      }],
    },
    {
      path: '/projects/:projectId/subjects/:subjectId/skills/:skillId',
      component: SkillPage,
      meta: { requiresAuth: true },
      children: [{
        name: 'SkillOverview',
        path: '',
        component: SkillOverview,
        meta: { requiresAuth: true, reportSkillId: 'VisitSkillOverview' },
      }, {
        name: 'SkillDependencies',
        path: 'dependencies',
        component: SkillDependencies,
        meta: { requiresAuth: true, reportSkillId: 'VisitSkillDependencies' },
      }, {
        name: 'SkillUsers',
        path: 'users',
        component: Users,
        meta: { requiresAuth: true, reportSkillId: 'VisitSkillUsers' },
      }, {
        name: 'AddSkillEvent',
        path: 'addSkillEvent',
        component: AddSkillEvent,
        meta: { requiresAuth: true, breadcrumb: 'Add Skill Event' },
        props: true,
      }, {
        name: 'SkillMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true, reportSkillId: 'VisitSkillStats', metricsSection: SECTION.SKILLS },
      }],
    },
    {
      path: '/projects/:projectId/users/:userId',
      component: UserPage,
      meta: { requiresAuth: true },
      children: [{
        name: 'ClientDisplayPreview',
        path: '',
        component: ClientDisplayPreview,
        meta: { requiresAuth: true, reportSkillId: 'VisitClientDisplay' },
      }, {
        name: 'UserSkillEvents',
        path: 'skillEvents',
        component: UserSkillsPerformed,
        meta: { requiresAuth: true, reportSkillId: 'VisitUserPerformedSkills' },
      }, {
        name: 'UserMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true, reportSkillId: 'VisitUserStats', metricsSection: SECTION.USERS },
      }],
    },
    {
      path: '/settings',
      component: GlobalSettings,
      meta: {
        requiresAuth: true,
      },
      children: [{
        name: 'GeneralSettings',
        path: '',
        component: GeneralSettings,
        meta: { requiresAuth: true, reportSkillId: 'VisitUserSettings' },
      }, {
        name: 'SecuritySettings',
        path: 'security',
        component: SecuritySettings,
        meta: { requiresAuth: true },
      }, {
        name: 'EmailSettings',
        path: 'email',
        component: EmailSettings,
        meta: { requiresAuth: true },
      }, {
        name: 'SystemSettings',
        path: 'system',
        component: SystemSettings,
        meta: { requiresAuth: true },
      }],
    },
    {
      path: '/markdown',
      name: 'MarkDownSupport',
      component: GFMDescription,
      meta: { requiresAuth: true, reportSkillId: 'VisitMarkdownDocs' },
    },
    {
      path: '/skills',
      name: 'InceptionSkills',
      component: InceptionSkills,
      meta: { requiresAuth: true, breadcrumb: 'Dashboard Skills', reportSkillId: 'VisitDashboardSkills' },
    },
    {
      path: '/globalBadges/:badgeId',
      component: GlobalBadgePage,
      meta: { requiresAuth: true },
      children: [{
        name: 'GlobalBadgeSkills',
        path: '',
        component: GlobalBadgeSkills,
        meta: { requiresAuth: true },
      }, {
        name: 'GlobalBadgeLevels',
        path: 'levels',
        component: GlobalBadgeLevels,
        meta: { requiresAuth: true },
      // }, {
      //   name: 'GlobalBadgeUsers',
      //   path: 'users',
      //   component: Users,
      //   meta: { requiresAuth: true },
      // }, {
      //   name: 'GlobalBadgeMetrics',
      //   path: 'metrics',
      //   component: SectionMetrics,
      //   meta: { requiresAuth: true },
      }],
    },
    {
      path: '*',
      name: '404',
      redirect: {
        name: 'ErrorPage',
        query: { errorMessage: '404 - Page Not Found' },
      },
      meta: { requiresAuth: false },
    },
  ],
});

export default router;
