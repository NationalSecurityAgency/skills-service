import Vue from 'vue';
import Router from 'vue-router';
import HomePage from '@/components/HomePage';
import MyProjects from '@/components/projects/MyProjects';
import LoginForm from '@/components/access/Login';
import RequestAccountForm from '@/components/access/RequestAccess';
import ProjectPage from '@/components/projects/ProjectPage';
import ErrorPage from '@/components/utils/ErrorPage';
import SubjectPage from '@/components/subjects/SubjectPage';
import BadgePage from '@/components/badges/BadgePage';
import GlobalBadgePage from '@/components/badges/global/GlobalBadgePage';
import SkillPage from '@/components/skills/SkillPage';
import UserPage from '@/components/users/UserPage';
import store from '@/store/store';
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
import { SkillsReporter } from '@skills/skills-client-vue';

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
        meta: { requiresAuth: true },
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
      path: '/error',
      name: 'ErrorPage',
      component: ErrorPage,
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
          // reportSkillId: 'ViewSubjects',
        },
      }, {
        name: 'Badges',
        path: 'badges',
        component: Badges,
        meta: { requiresAuth: true },
      }, {
        name: 'ProjectLevels',
        path: 'levels',
        component: Levels,
        meta: { requiresAuth: true },
      }, {
        name: 'FullDependencyGraph',
        path: 'dependencies',
        component: FullDependencyGraph,
        meta: { requiresAuth: true },
      }, {
        name: 'CrossProjectsSkills',
        path: 'crossProject',
        component: CrossProjectsSkills,
        meta: { requiresAuth: true },
      }, {
        name: 'ProjectUsers',
        path: 'users',
        component: Users,
        meta: { requiresAuth: true },
      }, {
        name: 'ProjectAccess',
        path: 'access',
        component: AccessSettings,
        meta: { requiresAuth: true },
      }, {
        name: 'ProjectSettings',
        path: 'settings',
        component: ProjectSettings,
        meta: { requiresAuth: true },
      }, {
        name: 'ProjectMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true },
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
        meta: { requiresAuth: true },
      }, {
        name: 'SubjectLevels',
        path: 'levels',
        component: Levels,
        meta: { requiresAuth: true },
      }, {
        name: 'SubjectUsers',
        path: 'users',
        component: Users,
        meta: { requiresAuth: true },
      }, {
        name: 'SubjectMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true },
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
        meta: { requiresAuth: true },
      }, {
        name: 'BadgeUsers',
        path: 'users',
        component: Users,
        meta: { requiresAuth: true },
      }, {
        name: 'BadgeMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true },
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
        meta: { requiresAuth: true },
      }, {
        name: 'SkillDependencies',
        path: 'dependencies',
        component: SkillDependencies,
        meta: { requiresAuth: true },
      }, {
        name: 'SkillUsers',
        path: 'users',
        component: Users,
        meta: { requiresAuth: true },
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
        meta: { requiresAuth: true },
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
        meta: { requiresAuth: true },
      }, {
        name: 'UserSkillEvents',
        path: 'skillEvents',
        component: UserSkillsPerformed,
        meta: { requiresAuth: true },
      }, {
        name: 'UserMetrics',
        path: 'metrics',
        component: SectionMetrics,
        meta: { requiresAuth: true },
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
        meta: { requiresAuth: true },
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
      }],
    },
    {
      path: '/markdown',
      name: 'MarkDownSupport',
      component: GFMDescription,
      meta: { requiresAuth: true },
    },
    {
      path: '/skills',
      name: 'InceptionSkills',
      component: InceptionSkills,
      meta: { requiresAuth: true, breadcrumb: 'Dashboard Skills' },
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

const isActiveProjectIdChange = (to, from) => to.params.projectId !== from.params.projectId;
const isLoggedIn = () => store.getters.isAuthenticated;

router.beforeEach((to, from, next) => {
  if (from.path !== '/error') {
    store.commit('previousUrl', from.fullPath);
  }
  if (isActiveProjectIdChange(to, from)) {
    store.commit('currentProjectId', to.params.projectId);
  }
  if (to.matched.some(record => record.meta.requiresAuth)) {
    // this route requires auth, check if logged in if not, redirect to login page.
    if (!isLoggedIn()) {
      const newRoute = { query: { redirect: to.fullPath } };
      if (store.getters.isPkiAuthenticated) {
        newRoute.name = 'HomePage';
      } else {
        newRoute.name = 'Login';
      }
      next(newRoute);
    } else {
      next();
    }
  } else {
    next();
  }
});

router.afterEach((to) => {
  if (to.meta.reportSkillId) {
    SkillsReporter.reportSkill(to.meta.reportSkillId);
  }
});

export default router;
