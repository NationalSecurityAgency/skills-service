import Vue from 'vue';
import Router from 'vue-router';
import HomePage from '@/components/HomePage';
import LoginForm from '@/components/access/Login';
import RequestAccountForm from '@/components/access/RequestAccess';
import ProjectPage from '@/components/projects/ProjectPage';
import ErrorPage from '@/components/utils/ErrorPage';
import SubjectPage from '@/components/subjects/SubjectPage';
import BadgePage from '@/components/badges/BadgePage';
import SkillPage from '@/components/skills/SkillPage';
import UserPage from '@/components/users/UserPage';
import store from '@/store/store';
import GlobalSettings from '@/components/settings/GlobalSettings';
import GFMDescription from '@//components/utils/GFMDescription';

Vue.use(Router);

const router = new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'HomePage',
      component: HomePage,
      meta: {
        breadcrumb: {
          label: 'Home',
          utils: {
            iconClass: 'fas fa-home',
          },
        },
        requiresAuth: true,
      },
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
        breadcrumb: {
          label: 'Home',
          utils: {
            iconClass: 'fas fa-home',
          },
        },
        requiresAuth: false,
      },
    },
    {
      path: '/projects/:projectId',
      name: 'ProjectPage',
      component: ProjectPage,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId/subjects/:subjectId',
      name: 'SubjectPage',
      component: SubjectPage,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId/badges/:badgeId',
      name: 'BadgePage',
      component: BadgePage,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId/subjects/:subjectId/skills/:skillId',
      name: 'SkillPage',
      component: SkillPage,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId/user/:userId',
      name: 'UserPage',
      component: UserPage,
      meta: { requiresAuth: true },
    },
    {
      path: '/settings/:settingsCategory',
      name: 'Settings',
      component: GlobalSettings,
      meta: {
        breadcrumb: {
          label: 'Settings',
          utils: {
            iconClass: 'fas fa-home',
          },
        },
        requiresAuth: true,
      },
    },
    {
      path: '/markdown',
      name: 'MarkDownSupport',
      component: GFMDescription,
      meta: { requiresAuth: true },
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
      next({
        path: '/skills-login',
        query: { redirect: to.fullPath },
      });
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
